package com.bekacodes.aegispay.ui

import androidx.compose.runtime.Immutable

@Immutable
data class AegisPayUiState(
    val cards: List<CardVaultItem> = listOf(
        CardVaultItem(
            id = "card_obsidian",
            tabLabel = "Obsidian Metal",
            maskedPan = "••••  ••••  ••••  4829",
            encryptedPanPayload = "",
            decryptedPanPreview = "4532  8910  3321  4829",
            expiry = "08/29",
            decryptedCvv = "417",
            availableBalanceUsd = 12450.00,
            spendingLimitUsd = 15000.00,
            isBurner = false
        ),
        CardVaultItem(
            id = "card_netflix",
            tabLabel = "Netflix Burner ($25/mo)",
            maskedPan = "••••  ••••  ••••  9104",
            encryptedPanPayload = "",
            decryptedPanPreview = "4111  5520  8831  9104",
            expiry = "11/28",
            decryptedCvv = "809",
            availableBalanceUsd = 5.01,
            spendingLimitUsd = 25.00,
            isBurner = true
        ),
        CardVaultItem(
            id = "card_travel",
            tabLabel = "Travel Virtual",
            maskedPan = "••••  ••••  ••••  6312",
            encryptedPanPayload = "",
            decryptedPanPreview = "4916  7740  1290  6312",
            expiry = "04/30",
            decryptedCvv = "294",
            availableBalanceUsd = 3800.00,
            spendingLimitUsd = 5000.00,
            isBurner = false
        )
    ),
    val selectedCardIndex: Int = 0,
    val isVaultUnlocked: Boolean = false,
    val unlockedPanPlaintext: String? = null,
    val unlockedCvvPlaintext: String? = null,
    val activeAnomaly: FraudAnomalyUiModel? = FraudAnomalyUiModel(
        id = "anom_1",
        cardId = "card_obsidian",
        merchantName = "CloudStream Pro",
        amountUsd = 19.99,
        description = "Duplicate recurring charge attempt ($19.99) from 'CloudStream Pro' blocked 14m ago."
    ),
    val ledgerRows: List<LedgerRowUiModel> = listOf(
        LedgerRowUiModel(
            id = "tx_1",
            cardId = "card_obsidian",
            name = "CloudStream Pro",
            sub = "Blocked by AI Shield",
            amount = "$19.99",
            accentType = LedgerAccentType.CORAL,
            pill = true
        ),
        LedgerRowUiModel(
            id = "tx_2",
            cardId = "card_obsidian",
            name = "Apple Pay / NFC Terminal #402",
            sub = "Tokenized APDU · Verified",
            amount = "-$6.50",
            accentType = LedgerAccentType.EMERALD,
            pill = true
        ),
        LedgerRowUiModel(
            id = "tx_3",
            cardId = "card_netflix",
            name = "AWS EMEA",
            sub = "Burner Card ••9104",
            amount = "-$42.10",
            accentType = LedgerAccentType.NEUTRAL,
            pill = false
        )
    ),
    val commandPrompt: String = "",
    val statusBanner: String? = null,
    val isNewBurnerDialogOpen: Boolean = false
) {
    val activeCard: CardVaultItem
        get() = cards.getOrElse(selectedCardIndex) { cards.first() }
}