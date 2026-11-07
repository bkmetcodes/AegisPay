package com.bekacodes.aegispay.ui

sealed interface AegisPayIntent {
    data class SelectCardTab(val index: Int) : AegisPayIntent
    data object ToggleVaultLock : AegisPayIntent
    data object SimulateNfcTapToPay : AegisPayIntent
    data object FreezeActiveCard : AegisPayIntent
    data object ApproveAnomalyOnce : AegisPayIntent
    data class UpdateCommandPrompt(val text: String) : AegisPayIntent
    data object SubmitCommandPrompt : AegisPayIntent
    data object DismissStatusBanner : AegisPayIntent
    data class ToggleNewBurnerDialog(val open: Boolean) : AegisPayIntent
    data class CreateBurnerCard(val merchantName: String, val monthlyLimit: Double) : AegisPayIntent
}