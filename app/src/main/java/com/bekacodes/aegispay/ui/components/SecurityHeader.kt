package com.bekacodes.aegispay.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.CardVaultItem
import com.bekacodes.aegispay.ui.theme.Border
import com.bekacodes.aegispay.ui.theme.Card
import com.bekacodes.aegispay.ui.theme.Coral
import com.bekacodes.aegispay.ui.theme.Cyan
import com.bekacodes.aegispay.ui.theme.Emerald
import com.bekacodes.aegispay.ui.theme.TextPri
import com.bekacodes.aegispay.ui.theme.TextSec

@Composable
fun SecurityHeader(
    cards: List<CardVaultItem>,
    selectedIndex: Int,
    isVaultUnlocked: Boolean,
    onSelect: (Int) -> Unit,
    onNewBurnerClick: () -> Unit
) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 40.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AegisPay ✦",
                    color = TextPri,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                val badgeColor = if (isVaultUnlocked) Cyan else Emerald
                Row(
                    modifier = Modifier
                        .border(1.dp, badgeColor.copy(.4f), CircleShape)
                        .background(badgeColor.copy(.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isVaultUnlocked) "KEYSTORE AES-256 DECRYPTED" else "KEYSTORE AES-256 LOCKED",
                        color = badgeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            OutlinedButton(
                onClick = onNewBurnerClick,
                shape = CircleShape,
                border = BorderStroke(1.dp, Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Card,
                    contentColor = TextPri
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Filled.Add, null, tint = Cyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Burner Card", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cards.size) { i ->
                val item = cards[i]
                FilterChip(
                    selected = i == selectedIndex,
                    onClick = { onSelect(i) },
                    shape = CircleShape,
                    label = {
                        Text(
                            text = if (item.isFrozen) "❄ ${item.tabLabel}" else item.tabLabel,
                            fontSize = 12.5.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = TextSec,
                        selectedContainerColor = Cyan.copy(.1f),
                        selectedLabelColor = TextPri
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = i == selectedIndex,
                        borderColor = if (item.isFrozen) Coral else Border,
                        selectedBorderColor = if (item.isFrozen) Coral else Cyan
                    )
                )
            }
        }
    }
}
