package com.bekacodes.aegispay.ui

import androidx.compose.runtime.Immutable

@Immutable
data class LedgerRowUiModel(
    val id: String,
    val cardId: String,
    val name: String,
    val sub: String,
    val amount: String,
    val accentType: LedgerAccentType,
    val pill: Boolean,
    val apduHex: String = "90 00 OK"
)