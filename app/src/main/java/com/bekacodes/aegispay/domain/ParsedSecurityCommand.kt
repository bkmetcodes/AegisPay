package com.bekacodes.aegispay.domain

sealed interface ParsedSecurityCommand {
    data class MintBurnerCard(
        val merchantLabel: String,
        val monthlyLimitUsd: Double
    ) : ParsedSecurityCommand

    data class SetFreezeState(val freeze: Boolean) : ParsedSecurityCommand

    data class SimulateNfcTap(
        val merchantName: String,
        val amountUsd: Double
    ) : ParsedSecurityCommand

    data object UnlockVaultCommand : ParsedSecurityCommand
    data class Unrecognized(val hint: String) : ParsedSecurityCommand
}

class AiSecurityAgent {

    /**
     * Parses natural-language security commands such as:
     * - "Create a $50 burner card for gym membership"
     * - "Mint a $20 burner for Cloud Trial"
     * - "Freeze card" / "Unfreeze card"
     * - "Tap $12.50 at Blue Bottle"
     */
    fun parseCommand(rawInput: String): ParsedSecurityCommand {
        val text = rawInput.trim()
        val lower = text.lowercase()

        // 1. Freeze / Unfreeze commands
        if (lower.contains("unfreeze") || lower.contains("unlock card")) {
            return ParsedSecurityCommand.SetFreezeState(freeze = false)
        }
        if (lower.startsWith("freeze") || lower.contains("freeze card") || lower.contains("block card")) {
            return ParsedSecurityCommand.SetFreezeState(freeze = true)
        }

        // 2. Biometric Vault Reveal command
        if (lower.contains("decrypt") || lower.contains("reveal cvv") || lower.contains("unlock vault")) {
            return ParsedSecurityCommand.UnlockVaultCommand
        }

        // 3. Create / Mint Burner Card command ("Create a $50 burner card for gym membership")
        if (lower.contains("burner") || lower.contains("virtual card") || lower.startsWith("create ") || lower.startsWith("mint ")) {
            val amount = Regex("""\$?\s*(\d+(?:\.\d{1,2})?)""")
                .find(text)
                ?.groupValues?.get(1)
                ?.toDoubleOrNull() ?: 50.0

            val merchantRaw = Regex("""(?:for|called|named)\s+(.+)""", RegexOption.IGNORE_CASE)
                .find(text)
                ?.groupValues?.get(1)
                ?.trim()
                ?: "Merchant Burner"

            val formattedMerchant = merchantRaw
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
                .take(22)

            return ParsedSecurityCommand.MintBurnerCard(
                merchantLabel = formattedMerchant,
                monthlyLimitUsd = amount.coerceAtLeast(5.0)
            )
        }

        // 4. Simulated NFC Tap command ("Tap $12.50 at Blue Bottle")
        if (lower.startsWith("tap ") || lower.contains("nfc") || lower.startsWith("pay ")) {
            val amount = Regex("""\$?\s*(\d+(?:\.\d{1,2})?)""")
                .find(text)
                ?.groupValues?.get(1)
                ?.toDoubleOrNull() ?: 6.50

            val merchant = Regex("""(?:at|to)\s+(.+)""", RegexOption.IGNORE_CASE)
                .find(text)
                ?.groupValues?.get(1)
                ?.trim()
                ?: "NFC Terminal #402"

            return ParsedSecurityCommand.SimulateNfcTap(
                merchantName = merchant,
                amountUsd = amount
            )
        }

        return ParsedSecurityCommand.Unrecognized(
            "Try: \"Create a $50 burner card for gym membership\", \"Freeze card\", or \"Tap $8.50 at Blue Bottle\""
        )
    }
}