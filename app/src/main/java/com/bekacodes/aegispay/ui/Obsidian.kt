package com.bekacodes.aegispay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.components.CommandBar
import com.bekacodes.aegispay.ui.components.CreateBurnerCardDialog
import com.bekacodes.aegispay.ui.components.FraudShieldCard
import com.bekacodes.aegispay.ui.components.Ledger
import com.bekacodes.aegispay.ui.components.SecurityHeader
import com.bekacodes.aegispay.ui.components.VirtualCard
import com.bekacodes.aegispay.ui.theme.Indigo
import com.bekacodes.aegispay.ui.theme.Obsidian
import com.bekacodes.aegispay.ui.theme.TextPri
import com.bekacodes.aegispay.ui.theme.TextSec

@Composable
fun AegisPayScreen(
    uiState: AegisPayUiState,
    onIntent: (AegisPayIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCard = uiState.activeCard

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Obsidian)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            SecurityHeader(
                cards = uiState.cards,
                selectedIndex = uiState.selectedCardIndex,
                isVaultUnlocked = uiState.isVaultUnlocked,
                onSelect = { onIntent(AegisPayIntent.SelectCardTab(it)) },
                onNewBurnerClick = { onIntent(AegisPayIntent.ToggleNewBurnerDialog(true)) }
            )

            // AI / Cryptographic Feedback Banner
            AnimatedVisibility(visible = uiState.statusBanner != null) {
                uiState.statusBanner?.let { message ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Indigo.copy(alpha = 0.16f))
                            .border(1.dp, Indigo, RoundedCornerShape(12.dp))
                            .clickable { onIntent(AegisPayIntent.DismissStatusBanner) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✦ $message",
                            color = TextPri,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text("✕", color = TextSec, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            VirtualCard(
                card = activeCard,
                unlocked = uiState.isVaultUnlocked,
                decryptedPan = uiState.unlockedPanPlaintext ?: activeCard.decryptedPanPreview,
                decryptedCvv = uiState.unlockedCvvPlaintext ?: activeCard.decryptedCvv,
                onToggleUnlock = { onIntent(AegisPayIntent.ToggleVaultLock) },
                onSimulateNfcTap = { onIntent(AegisPayIntent.SimulateNfcTapToPay) }
            )

            uiState.activeAnomaly?.let { anomaly ->
                if (!anomaly.isResolved) {
                    Spacer(Modifier.height(24.dp))
                    FraudShieldCard(
                        anomaly = anomaly,
                        isCardFrozen = activeCard.isFrozen,
                        onFreezeCard = { onIntent(AegisPayIntent.FreezeActiveCard) },
                        onApproveOnce = { onIntent(AegisPayIntent.ApproveAnomalyOnce) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Ledger(rows = uiState.ledgerRows)
        }

        CommandBar(
            value = uiState.commandPrompt,
            onChange = { onIntent(AegisPayIntent.UpdateCommandPrompt(it)) },
            onSend = { onIntent(AegisPayIntent.SubmitCommandPrompt) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
        )

        if (uiState.isNewBurnerDialogOpen) {
            CreateBurnerCardDialog(
                onDismiss = { onIntent(AegisPayIntent.ToggleNewBurnerDialog(false)) },
                onCreate = { merchant, limit ->
                    onIntent(AegisPayIntent.CreateBurnerCard(merchant, limit))
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun AegisPayScreenPreview() {
    MaterialTheme {
        AegisPayScreen(
            uiState = AegisPayUiState(),
            onIntent = {}
        )
    }
}