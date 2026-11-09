package com.bekacodes.aegispay

import com.bekacodes.aegispay.data.AegisPayDao
import com.bekacodes.aegispay.data.CardVaultEntity
import com.bekacodes.aegispay.data.FraudAnomalyEntity
import com.bekacodes.aegispay.data.LedgerRowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeAegisPayDao : AegisPayDao {
    private val cardsState = MutableStateFlow<List<CardVaultEntity>>(emptyList())
    private val anomaliesState = MutableStateFlow<List<FraudAnomalyEntity>>(emptyList())
    private val ledgerState = MutableStateFlow<List<LedgerRowEntity>>(emptyList())

    override fun observeCards(): Flow<List<CardVaultEntity>> = cardsState.asStateFlow()

    override suspend fun getCardsSnapshot(): List<CardVaultEntity> = cardsState.value

    override suspend fun insertCard(card: CardVaultEntity) {
        val current = cardsState.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == card.id }
        if (existingIndex >= 0) {
            current[existingIndex] = card
        } else {
            current.add(card)
        }
        cardsState.value = current
    }

    override suspend fun updateCardFreezeState(cardId: String, frozen: Boolean) {
        val current = cardsState.value.toMutableList()
        val index = current.indexOfFirst { it.id == cardId }
        if (index >= 0) {
            current[index] = current[index].copy(isFrozen = frozen)
            cardsState.value = current
        }
    }

    override suspend fun updateCardBalance(cardId: String, newBalance: Double) {
        val current = cardsState.value.toMutableList()
        val index = current.indexOfFirst { it.id == cardId }
        if (index >= 0) {
            current[index] = current[index].copy(availableBalanceUsd = newBalance)
            cardsState.value = current
        }
    }

    override fun observeActiveAnomaly(): Flow<FraudAnomalyEntity?> {
        return anomaliesState.map { list -> list.firstOrNull { !it.isResolved } }
    }

    override suspend fun insertAnomaly(anomaly: FraudAnomalyEntity) {
        val current = anomaliesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == anomaly.id }
        if (index >= 0) {
            current[index] = anomaly
        } else {
            current.add(anomaly)
        }
        anomaliesState.value = current
    }

    override suspend fun markAnomalyResolved(anomalyId: String) {
        val current = anomaliesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == anomalyId }
        if (index >= 0) {
            current[index] = current[index].copy(isResolved = true)
            anomaliesState.value = current
        }
    }

    override fun observeLedger(): Flow<List<LedgerRowEntity>> = ledgerState.asStateFlow()

    override suspend fun insertLedgerRow(row: LedgerRowEntity) {
        val current = ledgerState.value.toMutableList()
        val index = current.indexOfFirst { it.id == row.id }
        if (index >= 0) {
            current[index] = row
        } else {
            current.add(0, row)
        }
        ledgerState.value = current
    }
}
