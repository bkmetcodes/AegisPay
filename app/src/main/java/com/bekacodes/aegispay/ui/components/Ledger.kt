package com.bekacodes.aegispay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.LedgerAccentType
import com.bekacodes.aegispay.ui.LedgerRowUiModel
import com.bekacodes.aegispay.ui.theme.Border
import com.bekacodes.aegispay.ui.theme.Card
import com.bekacodes.aegispay.ui.theme.Coral
import com.bekacodes.aegispay.ui.theme.Cyan
import com.bekacodes.aegispay.ui.theme.Emerald
import com.bekacodes.aegispay.ui.theme.Obsidian
import com.bekacodes.aegispay.ui.theme.TextPri
import com.bekacodes.aegispay.ui.theme.TextSec

@Composable
fun Ledger(rows: List<LedgerRowUiModel>) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Ledger",
                color = TextPri,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Encrypted telemetry", color = Cyan, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Card)
                .border(1.dp, Border, RoundedCornerShape(16.dp))
        ) {
            rows.forEachIndexed { i, r ->
                val accentColor = when (r.accentType) {
                    LedgerAccentType.CORAL -> Coral
                    LedgerAccentType.EMERALD -> Emerald
                    LedgerAccentType.CYAN -> Cyan
                    LedgerAccentType.NEUTRAL -> TextSec
                }
                if (i > 0) HorizontalDivider(color = Border)
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Obsidian)
                            .border(1.dp, Border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = r.name.firstOrNull()?.toString() ?: "•",
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = r.name,
                            color = TextPri,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = r.sub,
                            color = if (r.pill) accentColor else TextSec,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (r.pill) {
                        Text(
                            text = r.amount,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(accentColor.copy(.12f), CircleShape)
                                .border(1.dp, accentColor.copy(.35f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    } else {
                        Text(
                            text = r.amount,
                            color = TextPri,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}