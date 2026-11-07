package com.bekacodes.aegispay.ui

import androidx.compose.runtime.Immutable

@Immutable
data class CardVaultItem(
    val id: String,
    val tabLabel: String,
    val cardHolder: String = "ALEXANDER VANCE",
    val maskedPan: String,
    val encryptedPanPayload: String, // Decrypted via Android Keystore AES-256-GCM in Milestone 2
    val decryptedPanPreview: String,
    val expiry: String,
    val decryptedCvv: String,
    val availableBalanceUsd: Double,
    val spendingLimitUsd: Double,
    val isBurner: Boolean = false,
    val isFrozen: Boolean = false
)