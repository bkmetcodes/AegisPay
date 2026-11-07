package com.bekacodes.aegispay.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bekacodes.aegispay.ui.theme.Border
import com.bekacodes.aegispay.ui.theme.Card
import com.bekacodes.aegispay.ui.theme.Cyan
import com.bekacodes.aegispay.ui.theme.Obsidian
import com.bekacodes.aegispay.ui.theme.TextPri
import com.bekacodes.aegispay.ui.theme.TextSec

@Composable
fun CreateBurnerCardDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Double) -> Unit
) {
    var merchant by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        titleContentColor = TextPri,
        textContentColor = TextSec,
        title = {
            Text(
                "Mint Single-Merchant Burner Card",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = { Text("Merchant (e.g. Gym, Cloud Trial)", color = TextSec) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Border
                    )
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    placeholder = { Text("Monthly Cap ($)", color = TextSec) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPri,
                        unfocusedTextColor = TextPri,
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Border
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = limitText.toDoubleOrNull() ?: 50.0
                    if (merchant.isNotBlank()) onCreate(merchant.trim(), cap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Obsidian)
            ) {
                Text("Create Card", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSec)
            }
        }
    )
}