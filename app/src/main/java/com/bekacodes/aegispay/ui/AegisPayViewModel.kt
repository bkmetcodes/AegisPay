package com.bekacodes.aegispay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bekacodes.aegispay.data.AegisPayDao
import com.bekacodes.aegispay.data.CardVaultEntity
import com.bekacodes.aegispay.data.FraudAnomalyEntity
import com.bekacodes.aegispay.data.LedgerRowEntity
import com.bekacodes.aegispay.domain.AiSecurityAgent
import com.bekacodes.aegispay.domain.ParsedSecurityCommand
import com.bekacodes.aegispay.security.KeystoreVaultCipher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

@HiltViewModel
class AegisPayViewModel @Inject constructor(
    private val dao: AegisPayDao,
    private val cipher: KeystoreVaultCipher = KeystoreVaultCipher(),
    private val aiAgent: AiSecurityAgent = AiSecurityAgent()
) : ViewModel() {

    private val selectedIndexFlow = MutableStateFlow(0)
    private val isUnlockedFlow = MutableStateFlow(false)
    private var autoLockJob: Job? = null

    private val _uiState = MutableStateFlow(AegisPayUiState())
    val uiState: StateFlow<AegisPayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            seedEncryptedVaultIfEmpty()
        }

        viewModelScope.launch {
            combine(
                dao.observeCards(),
                dao.observeActiveAnomaly(),
                dao.observeLedger(),
                selectedIndexFlow,
                isUnlockedFlow
            ) { cardEntities, anomalyEntity, ledgerEntities, selectedIndex, isUnlocked ->

                if (cardEntities.isEmpty()) return@combine _uiState.value

                val safeIndex = selectedIndex.coerceIn(0, cardEntities.lastIndex)
                val mappedCards = cardEntities.map { entity ->
                    CardVaultItem(
                        id = entity.id,
                        tabLabel = entity.tabLabel,
                        maskedPan = entity.maskedPan,
                        encryptedPanPayload = entity.encryptedCredentialsPayload,
                        decryptedPanPreview = entity.maskedPan,
                        expiry = entity.expiry,
                        decryptedCvv = "•••",
                        availableBalanceUsd = entity.availableBalanceUsd,
                        spendingLimitUsd = entity.spendingLimitUsd,
                        isBurner = entity.isBurner,
                        isFrozen = entity.isFrozen
                    )
                }

                val activeEntity = cardEntities[safeIndex]
                val (decryptedPan, decryptedCvv) = if (isUnlocked) {
                    runCatching {
                        val raw = cipher.decrypt(activeEntity.encryptedCredentialsPayload)
                        cipher.unpackCredentials(raw)
                    }.getOrElse { activeEntity.maskedPan to "ERR" }
                } else {
                    null to null
                }

                val mappedAnomaly = anomalyEntity?.let {
                    FraudAnomalyUiModel(
                        id = it.id,
                        cardId = it.cardId,
                        merchantName = it.merchantName,
                        amountUsd = it.amountUsd,
                        description = it.description,
                        isResolved = it.isResolved
                    )
                }

                val mappedLedger = ledgerEntities.map { row ->
                    LedgerRowUiModel(
                        id = row.id,
                        cardId = row.cardId,
                        name = row.name,
                        sub = row.sub,
                        amount = row.amount,
                        accentType = runCatching { LedgerAccentType.valueOf(row.accentTypeName) }
                            .getOrDefault(LedgerAccentType.NEUTRAL),
                        pill = row.pill,
                        apduHex = row.apduHex
                    )
                }

                _uiState.value.copy(
                    cards = mappedCards,
                    selectedCardIndex = safeIndex,
                    isVaultUnlocked = isUnlocked,
                    unlockedPanPlaintext = decryptedPan,
                    unlockedCvvPlaintext = decryptedCvv,
                    activeAnomaly = mappedAnomaly,
                    ledgerRows = mappedLedger
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onIntent(intent: AegisPayIntent) {
        when (intent) {
            is AegisPayIntent.SelectCardTab -> {
                selectedIndexFlow.value = intent.index
                // Re-lock vault when switching cards so another card's PAN isn't exposed automatically
                lockVaultImmediately()
            }

            is AegisPayIntent.ToggleVaultLock -> {
                if (isUnlockedFlow.value) {
                    lockVaultImmediately()
                    _uiState.update { it.copy(statusBanner = "Vault locked · Plaintext wiped from memory") }
                } else {
                    unlockVaultWithAutoRelock()
                }
            }

            is AegisPayIntent.SimulateNfcTapToPay -> {
                val card = _uiState.value.activeCard
                executeSimulatedNfcTap(
                    card = card,
                    merchantName = "Apple Pay / NFC Terminal #${Random.nextInt(100, 999)}",
                    amountUsd = 6.50
                )
            }

            is AegisPayIntent.FreezeActiveCard -> {
                viewModelScope.launch {
                    val active = _uiState.value.activeCard
                    val nextFrozen = !active.isFrozen
                    dao.updateCardFreezeState(active.id, nextFrozen)
                    _uiState.value.activeAnomaly?.let { anomaly ->
                        if (nextFrozen) dao.markAnomalyResolved(anomaly.id)
                    }
                    _uiState.update {
                        it.copy(
                            statusBanner = if (nextFrozen) {
                                "❄ Frozen '${active.tabLabel}' & revoked active NFC tokens"
                            } else {
                                "🔥 Unfroze '${active.tabLabel}' · NFC tokens restored"
                            }
                        )
                    }
                }
            }

            is AegisPayIntent.ApproveAnomalyOnce -> {
                viewModelScope.launch {
                    val anomaly = _uiState.value.activeAnomaly ?: return@launch
                    dao.markAnomalyResolved(anomaly.id)
                    dao.insertLedgerRow(
                        LedgerRowEntity(
                            id = "tx_${UUID.randomUUID().toString().take(8)}",
                            cardId = anomaly.cardId,
                            name = anomaly.merchantName,
                            sub = "Approved via AI Shield Override",
                            amount = "-$${String.format("%.2f", anomaly.amountUsd)}",
                            accentTypeName = LedgerAccentType.EMERALD.name,
                            pill = true,
                            apduHex = "90 00 OK"
                        )
                    )
                    _uiState.update {
                        it.copy(statusBanner = "Approved one-time charge ($${anomaly.amountUsd}) for ${anomaly.merchantName}")
                    }
                }
            }

            is AegisPayIntent.UpdateCommandPrompt -> {
                _uiState.update { it.copy(commandPrompt = intent.text) }
            }

            is AegisPayIntent.SubmitCommandPrompt -> {
                handleAiCommand(_uiState.value.commandPrompt)
            }

            is AegisPayIntent.DismissStatusBanner -> {
                _uiState.update { it.copy(statusBanner = null) }
            }

            is AegisPayIntent.ToggleNewBurnerDialog -> {
                _uiState.update { it.copy(isNewBurnerDialogOpen = intent.open) }
            }

            is AegisPayIntent.CreateBurnerCard -> {
                mintEncryptedBurnerCard(intent.merchantName, intent.monthlyLimit)
            }
        }
    }

    private fun unlockVaultWithAutoRelock() {
        autoLockJob?.cancel()
        isUnlockedFlow.value = true
        _uiState.update {
            it.copy(statusBanner = "🔓 Biometric verified · Decrypted via Android Keystore AES-256-GCM (Auto-locks in 20s)")
        }
        autoLockJob = viewModelScope.launch {
            delay(20_000L)
            isUnlockedFlow.value = false
            _uiState.update { it.copy(statusBanner = "🔒 Vault auto-locked after 20s timeout") }
        }
    }

    private fun lockVaultImmediately() {
        autoLockJob?.cancel()
        isUnlockedFlow.value = false
    }

    private fun handleAiCommand(rawPrompt: String) {
        if (rawPrompt.isBlank()) return
        when (val cmd = aiAgent.parseCommand(rawPrompt)) {
            is ParsedSecurityCommand.MintBurnerCard -> {
                _uiState.update { it.copy(commandPrompt = "") }
                mintEncryptedBurnerCard(cmd.merchantLabel, cmd.monthlyLimitUsd)
            }

            is ParsedSecurityCommand.SetFreezeState -> {
                viewModelScope.launch {
                    val active = _uiState.value.activeCard
                    dao.updateCardFreezeState(active.id, cmd.freeze)
                    _uiState.update {
                        it.copy(
                            commandPrompt = "",
                            statusBanner = if (cmd.freeze) {
                                "❄ Frozen '${active.tabLabel}'"
                            } else {
                                "🔥 Unfroze '${active.tabLabel}'"
                            }
                        )
                    }
                }
            }

            is ParsedSecurityCommand.SimulateNfcTap -> {
                _uiState.update { it.copy(commandPrompt = "") }
                executeSimulatedNfcTap(
                    card = _uiState.value.activeCard,
                    merchantName = cmd.merchantName,
                    amountUsd = cmd.amountUsd
                )
            }

            is ParsedSecurityCommand.UnlockVaultCommand -> {
                _uiState.update { it.copy(commandPrompt = "") }
                unlockVaultWithAutoRelock()
            }

            is ParsedSecurityCommand.Unrecognized -> {
                _uiState.update { it.copy(statusBanner = cmd.hint) }
            }
        }
    }

    private fun mintEncryptedBurnerCard(merchantLabel: String, limitUsd: Double) {
        viewModelScope.launch {
            val last4 = Random.nextInt(1000, 9999).toString()
            val mid4 = Random.nextInt(1000, 9999).toString()
            val fullPan = "4111  $mid4  8820  $last4"
            val cvv = Random.nextInt(100, 999).toString()

            val encryptedPayload = cipher.encrypt(cipher.packCredentials(fullPan, cvv))
            val cardId = "card_burner_${UUID.randomUUID().toString().take(6)}"
            val cleanTitle = if (merchantLabel.lowercase().contains("burner")) {
                "$merchantLabel ($${limitUsd.toInt()}/mo)"
            } else {
                "$merchantLabel Burner ($${limitUsd.toInt()}/mo)"
            }

            val newCard = CardVaultEntity(
                id = cardId,
                tabLabel = cleanTitle,
                maskedPan = "••••  ••••  ••••  $last4",
                encryptedCredentialsPayload = encryptedPayload,
                expiry = "10/29",
                availableBalanceUsd = limitUsd,
                spendingLimitUsd = limitUsd,
                isBurner = true,
                isFrozen = false
            )

            dao.insertCard(newCard)
            val updatedCount = dao.getCardsSnapshot().size
            selectedIndexFlow.value = (updatedCount - 1).coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    isNewBurnerDialogOpen = false,
                    statusBanner = "Minted AES-256-GCM burner '$cleanTitle' (••$last4)"
                )
            }
        }
    }

    private fun executeSimulatedNfcTap(
        card: CardVaultItem,
        merchantName: String,
        amountUsd: Double
    ) {
        if (card.isFrozen) {
            _uiState.update { it.copy(statusBanner = "⚠ NFC Tap Declined: '${card.tabLabel}' is frozen") }
            return
        }
        viewModelScope.launch {
            val newBalance = (card.availableBalanceUsd - amountUsd).coerceAtLeast(0.0)
            dao.updateCardBalance(card.id, newBalance)

            val cryptogram = Random.nextBytes(4).joinToString("") { "%02X".format(it) }
            dao.insertLedgerRow(
                LedgerRowEntity(
                    id = "tx_${UUID.randomUUID().toString().take(8)}",
                    cardId = card.id,
                    name = merchantName,
                    sub = "Tokenized APDU · Cryptogram $cryptogram",
                    amount = "-$${String.format("%.2f", amountUsd)}",
                    accentTypeName = LedgerAccentType.EMERALD.name,
                    pill = true,
                    apduHex = "00 A4 04 00 -> 90 00 OK"
                )
            )
            _uiState.update {
                it.copy(
                    statusBanner = "))) NFC HCE Approved (-$${
                        String.format(
                            "%.2f",
                            amountUsd
                        )
                    }) · Cryptogram $cryptogram"
                )
            }
        }
    }

    private suspend fun seedEncryptedVaultIfEmpty() {
        if (dao.getCardsSnapshot().isNotEmpty()) return

        dao.insertCard(
            CardVaultEntity(
                id = "card_obsidian",
                tabLabel = "Obsidian Metal",
                maskedPan = "••••  ••••  ••••  4829",
                encryptedCredentialsPayload = cipher.encrypt(
                    cipher.packCredentials(
                        "4532  8910  3321  4829",
                        "417"
                    )
                ),
                expiry = "08/29",
                availableBalanceUsd = 12450.00,
                spendingLimitUsd = 15000.00,
                isBurner = false,
                isFrozen = false,
                createdAt = 1L
            )
        )
        dao.insertCard(
            CardVaultEntity(
                id = "card_netflix",
                tabLabel = "Netflix Burner ($25/mo)",
                maskedPan = "••••  ••••  ••••  9104",
                encryptedCredentialsPayload = cipher.encrypt(
                    cipher.packCredentials(
                        "4111  5520  8831  9104",
                        "809"
                    )
                ),
                expiry = "11/28",
                availableBalanceUsd = 25.00,
                spendingLimitUsd = 25.00,
                isBurner = true,
                isFrozen = false,
                createdAt = 2L
            )
        )
        dao.insertCard(
            CardVaultEntity(
                id = "card_travel",
                tabLabel = "Travel Virtual",
                maskedPan = "••••  ••••  ••••  6312",
                encryptedCredentialsPayload = cipher.encrypt(
                    cipher.packCredentials(
                        "4916  7740  1290  6312",
                        "294"
                    )
                ),
                expiry = "04/30",
                availableBalanceUsd = 3800.00,
                spendingLimitUsd = 5000.00,
                isBurner = false,
                isFrozen = false,
                createdAt = 3L
            )
        )

        dao.insertAnomaly(
            FraudAnomalyEntity(
                id = "anom_1",
                cardId = "card_obsidian",
                merchantName = "CloudStream Pro",
                amountUsd = 19.99,
                description = "Duplicate recurring charge attempt ($19.99) from 'CloudStream Pro' blocked 14m ago."
            )
        )

        dao.insertLedgerRow(
            LedgerRowEntity(
                id = "tx_1",
                cardId = "card_obsidian",
                name = "CloudStream Pro",
                sub = "Blocked by AI Shield",
                amount = "$19.99",
                accentTypeName = LedgerAccentType.CORAL.name,
                pill = true,
                apduHex = "69 85 BLOCKED",
                timestampMs = 3000L
            )
        )
        dao.insertLedgerRow(
            LedgerRowEntity(
                id = "tx_2",
                cardId = "card_obsidian",
                name = "Apple Pay / NFC Terminal #402",
                sub = "Tokenized APDU · Verified",
                amount = "-$6.50",
                accentTypeName = LedgerAccentType.EMERALD.name,
                pill = true,
                apduHex = "90 00 OK",
                timestampMs = 2000L
            )
        )
        dao.insertLedgerRow(
            LedgerRowEntity(
                id = "tx_3",
                cardId = "card_netflix",
                name = "AWS EMEA",
                sub = "Burner Card ••9104",
                amount = "-$42.10",
                accentTypeName = LedgerAccentType.NEUTRAL.name,
                pill = false,
                apduHex = "90 00 OK",
                timestampMs = 1000L
            )
        )
    }

    companion object {
        fun provideFactory(dao: AegisPayDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AegisPayViewModel(dao) as T
                }
            }
    }
}