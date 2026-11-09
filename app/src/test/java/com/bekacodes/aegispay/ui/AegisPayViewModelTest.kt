package com.bekacodes.aegispay.ui

import com.bekacodes.aegispay.data.AegisPayDao
import com.bekacodes.aegispay.data.CardVaultEntity
import com.bekacodes.aegispay.data.FraudAnomalyEntity
import com.bekacodes.aegispay.data.LedgerRowEntity
import com.bekacodes.aegispay.domain.AiSecurityAgent
import com.bekacodes.aegispay.domain.ParsedSecurityCommand
import com.bekacodes.aegispay.security.KeystoreVaultCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AegisPayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeAegisPayDao
    private lateinit var cipher: KeystoreVaultCipher
    private lateinit var aiAgent: AiSecurityAgent

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAegisPayDao()
        cipher = KeystoreVaultCipher()
        aiAgent = AiSecurityAgent()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `KeystoreVaultCipher encrypts with randomized IV and decrypts PAN and CVV accurately`() {
        val rawPacked = cipher.packCredentials("4532  8910  3321  4829", "417")

        val encryptedFirst = cipher.encrypt(rawPacked)
        val encryptedSecond = cipher.encrypt(rawPacked)

        // Randomized 12-byte GCM IV must yield different Base64 ciphertexts for identical inputs
        assertNotEquals(encryptedFirst, encryptedSecond)
        assertFalse(encryptedFirst.contains("4829"))

        val (decryptedPan, decryptedCvv) = cipher.unpackCredentials(cipher.decrypt(encryptedFirst))
        assertEquals("4532  8910  3321  4829", decryptedPan)
        assertEquals("417", decryptedCvv)
    }

    @Test
    fun `unlocking vault decrypts active card credentials and auto-locks after 20 seconds`() =
        runTest(testDispatcher) {
            val viewModel = AegisPayViewModel(fakeDao, cipher, aiAgent)
            runCurrent()

            // Initially locked: plaintext fields in UI state must be null
            assertFalse(viewModel.uiState.value.isVaultUnlocked)
            assertNull(viewModel.uiState.value.unlockedPanPlaintext)
            assertNull(viewModel.uiState.value.unlockedCvvPlaintext)

            // 1. Trigger Biometric Unlock
            viewModel.onIntent(AegisPayIntent.ToggleVaultLock)
            runCurrent()

            val unlockedState = viewModel.uiState.value
            assertTrue(unlockedState.isVaultUnlocked)
            assertEquals("4532  8910  3321  4829", unlockedState.unlockedPanPlaintext)
            assertEquals("417", unlockedState.unlockedCvvPlaintext)

            // 2. Advance virtual clock past the 20,000ms security timeout
            advanceTimeBy(20_001L)
            runCurrent()

            val relockedState = viewModel.uiState.value
            assertFalse(relockedState.isVaultUnlocked)
            assertNull(relockedState.unlockedPanPlaintext)
            assertNull(relockedState.unlockedCvvPlaintext)
        }

    @Test
    fun `AI command mints encrypted burner card and freezing card blocks NFC tap-to-pay`() =
        runTest(testDispatcher) {
            val viewModel = AegisPayViewModel(fakeDao, cipher, aiAgent)
            runCurrent()

            val parsed = aiAgent.parseCommand("Create a $50 burner card for gym membership")
            assertTrue(parsed is ParsedSecurityCommand.MintBurnerCard)

            // 1. Submit AI command to mint the burner card
            viewModel.onIntent(AegisPayIntent.UpdateCommandPrompt("Create a $50 burner card for gym membership"))
            viewModel.onIntent(AegisPayIntent.SubmitCommandPrompt)
            runCurrent()

            val stateAfterMint = viewModel.uiState.value
            assertEquals(4, stateAfterMint.cards.size)
            assertEquals(3, stateAfterMint.selectedCardIndex) // Switched to newly minted 4th card
            assertTrue(stateAfterMint.activeCard.tabLabel.contains("Gym Membership Burner ($50/mo)"))
            assertEquals(50.0, stateAfterMint.activeCard.availableBalanceUsd, 0.01)

            // 2. Freeze the new burner card and attempt an NFC tap
            viewModel.onIntent(AegisPayIntent.FreezeActiveCard)
            runCurrent()
            assertTrue(viewModel.uiState.value.activeCard.isFrozen)

            viewModel.onIntent(AegisPayIntent.SimulateNfcTapToPay)
            runCurrent()

            // Balance must remain untouched at $50.00 because the card is frozen
            assertEquals(50.0, viewModel.uiState.value.activeCard.availableBalanceUsd, 0.01)
        }
}

private class FakeAegisPayDao : AegisPayDao {
    private val cardsFlow = MutableStateFlow<List<CardVaultEntity>>(emptyList())
    private val anomalyFlow = MutableStateFlow<FraudAnomalyEntity?>(null)
    private val ledgerFlow = MutableStateFlow<List<LedgerRowEntity>>(emptyList())

    override fun observeCards(): Flow<List<CardVaultEntity>> = cardsFlow
    override suspend fun getCardsSnapshot(): List<CardVaultEntity> = cardsFlow.value

    override suspend fun insertCard(card: CardVaultEntity) {
        cardsFlow.value = cardsFlow.value.filterNot { it.id == card.id } + card
    }

    override suspend fun updateCardFreezeState(cardId: String, frozen: Boolean) {
        cardsFlow.value = cardsFlow.value.map {
            if (it.id == cardId) it.copy(isFrozen = frozen) else it
        }
    }

    override suspend fun updateCardBalance(cardId: String, newBalance: Double) {
        cardsFlow.value = cardsFlow.value.map {
            if (it.id == cardId) it.copy(availableBalanceUsd = newBalance) else it
        }
    }

    override fun observeActiveAnomaly(): Flow<FraudAnomalyEntity?> = anomalyFlow

    override suspend fun insertAnomaly(anomaly: FraudAnomalyEntity) {
        anomalyFlow.value = anomaly
    }

    override suspend fun markAnomalyResolved(anomalyId: String) {
        anomalyFlow.value?.let {
            if (it.id == anomalyId) anomalyFlow.value = null
        }
    }

    override fun observeLedger(): Flow<List<LedgerRowEntity>> = ledgerFlow

    override suspend fun insertLedgerRow(row: LedgerRowEntity) {
        ledgerFlow.value = listOf(row) + ledgerFlow.value
    }
}