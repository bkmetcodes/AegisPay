package com.bekacodes.aegispay.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.FraudAnomalyUiModel
import com.bekacodes.aegispay.ui.theme.*

@Composable
fun FraudShieldCard(
    anomaly: FraudAnomalyUiModel,
    isCardFrozen: Boolean,
    onFreezeCard: () -> Unit,
    onApproveOnce: () -> Unit
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .border(1.dp, Coral, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Indigo.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡", fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "✦ AI Fraud Shield · ",
                color = TextPri,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "1 Anomaly Intercepted",
                color = Coral,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = anomaly.description,
            color = TextSec,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onFreezeCard,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Coral,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isCardFrozen) "Unfreeze Card" else "Freeze Card",
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = onApproveOnce,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, TextSec.copy(.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPri)
            ) {
                Text("Approve Once", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}