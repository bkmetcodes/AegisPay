package com.bekacodes.aegispay.data

import android.content.Context
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "vault_cards")
data class CardVaultEntity(
    @PrimaryKey val id: String,
    val tabLabel: String,
    val maskedPan: String,
    val encryptedCredentialsPayload: String, // AES-256-GCM encrypted "PAN|CVV"
    val expiry: String,
    val availableBalanceUsd: Double,
    val spendingLimitUsd: Double,
    val isBurner: Boolean,
    val isFrozen: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fraud_anomalies")
data class FraudAnomalyEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val merchantName: String,
    val amountUsd: Double,
    val description: String,
    val isResolved: Boolean = false
)

@Entity(tableName = "vault_ledger")
data class LedgerRowEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val name: String,
    val sub: String,
    val amount: String,
    val accentTypeName: String,
    val pill: Boolean,
    val apduHex: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Dao
interface AegisPayDao {
    @Query("SELECT * FROM vault_cards ORDER BY createdAt ASC")
    fun observeCards(): Flow<List<CardVaultEntity>>

    @Query("SELECT * FROM vault_cards ORDER BY createdAt ASC")
    suspend fun getCardsSnapshot(): List<CardVaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardVaultEntity)

    @Query("UPDATE vault_cards SET isFrozen = :frozen WHERE id = :cardId")
    suspend fun updateCardFreezeState(cardId: String, frozen: Boolean)

    @Query("UPDATE vault_cards SET availableBalanceUsd = :newBalance WHERE id = :cardId")
    suspend fun updateCardBalance(cardId: String, newBalance: Double)

    @Query("SELECT * FROM fraud_anomalies WHERE isResolved = 0 LIMIT 1")
    fun observeActiveAnomaly(): Flow<FraudAnomalyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomaly(anomaly: FraudAnomalyEntity)

    @Query("UPDATE fraud_anomalies SET isResolved = 1 WHERE id = :anomalyId")
    suspend fun markAnomalyResolved(anomalyId: String)

    @Query("SELECT * FROM vault_ledger ORDER BY timestampMs DESC")
    fun observeLedger(): Flow<List<LedgerRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerRow(row: LedgerRowEntity)
}

@Database(
    entities = [
        CardVaultEntity::class,
        FraudAnomalyEntity::class,
        LedgerRowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AegisPayDatabase : RoomDatabase() {
    abstract fun dao(): AegisPayDao

    companion object {
        @Volatile
        private var INSTANCE: AegisPayDatabase? = null

        fun getInstance(context: Context): AegisPayDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AegisPayDatabase::class.java,
                    "aegispay_encrypted_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}