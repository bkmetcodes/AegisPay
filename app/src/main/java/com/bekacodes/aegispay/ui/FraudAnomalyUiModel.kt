package com.bekacodes.aegispay.ui

import androidx.compose.runtime.Immutable

@Immutable
data class FraudAnomalyUiModel(
    val id: String,
    val cardId: String,
    val merchantName: String,
    val amountUsd: Double,
    val description: String,
    val isResolved: Boolean = false
)