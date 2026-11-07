package com.bekacodes.aegispay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.CardVaultItem
import com.bekacodes.aegispay.ui.theme.Coral
import com.bekacodes.aegispay.ui.theme.Cyan
import com.bekacodes.aegispay.ui.theme.Emerald
import com.bekacodes.aegispay.ui.theme.Indigo
import com.bekacodes.aegispay.ui.theme.Obsidian
import com.bekacodes.aegispay.ui.theme.TextPri
import com.bekacodes.aegispay.ui.theme.TextSec

@Composable
fun VirtualCard(
    card: CardVaultItem,
    unlocked: Boolean,
    decryptedPan: String,
    decryptedCvv: String,
    onToggleUnlock: () -> Unit,
    onSimulateNfcTap: () -> Unit
) {
    val radarColor = if (card.isFrozen) Coral else Emerald

    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(205.dp)
    ) {
        RadarRing(0, radarColor)
        RadarRing(1000, radarColor)
        RadarRing(2000, radarColor)

        Box(
            Modifier
                .fillMaxSize()
                .shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = Indigo, spotColor = Indigo)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1B2233), Color(0xFF0B0F1A), Color(0xFF151B2E))
                    )
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            Indigo.copy(.22f),
                            Cyan.copy(.18f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (card.isFrozen) Coral.copy(.6f) else Color.White.copy(.1f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            // Top Row: Metallic EMV Chip + Tap-to-Pay Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp, 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE5D7A8), Color(0xFF8A7A4A), Color(0xFFD9C98F))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !card.isFrozen, onClick = onSimulateNfcTap)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (card.isFrozen) "CARD FROZEN" else "NFC ACTIVE · TAP",
                        color = if (card.isFrozen) Coral else Emerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (card.isFrozen) "❄" else ")))",
                        color = if (card.isFrozen) Coral else TextPri,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Center Row: Masked vs Decrypted PAN & CVV
            Column(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = if (unlocked) decryptedPan else card.maskedPan,
                    color = if (unlocked) Cyan else TextPri,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 19.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "EXP ${card.expiry}     CVV ${if (unlocked) "$decryptedCvv (Decrypted)" else "••• (Biometric Locked)"}",
                    color = if (unlocked) Emerald else TextSec,
                    fontSize = 11.sp
                )
            }

            // Bottom Row: Available Balance + Biometric Vault Button
            Row(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$%,.2f".format(card.availableBalanceUsd),
                        color = TextPri,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Available", color = TextSec, fontSize = 11.sp)
                }
                Button(
                    onClick = onToggleUnlock,
                    shape = CircleShape,
                    modifier = Modifier
                        .height(32.dp)
                        .shadow(14.dp, CircleShape, ambientColor = Cyan, spotColor = Cyan),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan,
                        contentColor = Obsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (unlocked) "Lock Vault" else "Unlock Vault (Biometric)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}