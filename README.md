# AegisPay ✦ — Biometric Smart Card Vault, NFC HCE & AI Fraud Shield

A security-first Android FinTech application built with **Kotlin**, **Jetpack Compose**, **Clean Architecture (MVI)**, **Android Keystore (`AES/GCM/NoPadding`)**, **Biometric Vault Authentication**, **NFC Host Card Emulation (HCE)**, **Room**, and an **AI Burner Card & Fraud Shield Engine**.

<p align="center">
  <!-- Paste your GitHub issue-uploaded GIF or screenshot URL here -->
  <img width="277" height="600" alt="Image" src="https://github.com/user-attachments/assets/f980afd9-bd87-469f-9ff9-f5b2ad7b7b2b" />
</p>

---

## ✨ Key Engineering Highlights

1. **Zero-Plaintext Storage with Android Keystore (`AES-256-GCM`)**
   - Card numbers (PAN) and CVV codes are **never stored in plaintext** inside SQLite (`AegisPayDatabase`).
   - `KeystoreVaultCipher` generates a hardware-backed `AES-256` master key inside the `"AndroidKeyStore"` provider using `KeyProperties.BLOCK_MODE_GCM` and `ENCRYPTION_PADDING_NONE`.
   - Every credential payload is encrypted with a fresh 12-byte Initialization Vector (IV) and 128-bit GCM authentication tag (`[IV_Length | IV | Ciphertext]`) before persistence.

2. **On-Demand Biometric Decryption & Automatic Memory Zeroing**
   - Tapping **`Unlock Vault (Biometric)`** decrypts the active card's PAN and CVV in memory and starts a **20-second coroutine countdown**.
   - Switching cards or reaching the 20-second timeout automatically cancels the reveal job and wipes the plaintext strings from `AegisPayUiState`.

3. **NFC Host Card Emulation (HCE) Radar & Tokenized APDU Ledger**
   - Features a multi-layered holographic virtual card with staggered `InfiniteTransition` NFC electromagnetic radar rings.
   - Simulates contactless ISO/IEC 7816-4 `SELECT PPSE` APDU handshakes, generating one-time cryptographic cryptograms (`ARQC`) and recording verified transactions in the reactive Room ledger.

4. **AI Fraud Shield & Natural-Language Burner Card Engine (`AiSecurityAgent`)**
   - Intercepts anomalous duplicate subscription charges (`FraudShieldCard`) with one-tap **Freeze Card** (revoking NFC tokens) or **Approve Once** overrides.
   - Bottom-docked AI Command Bar parses natural-language requests such as:
     - *"Create a $50 burner card for gym membership"*
     - *"Freeze card"* / *"Unfreeze card"*
     - *"Tap $12.50 at Blue Bottle"*

---

## 🏗️ Security & MVI Architecture Overview

```mermaid
flowchart LR
    UI["AegisPayScreen\n(Jetpack Compose + NFC Radar)"] -->|AegisPayIntent| VM["AegisPayViewModel\n(MVI StateFlow + 20s Auto-Lock)"]
    VM -->|Natural Language Prompt| AI["AiSecurityAgent\n(Burner & Security Parser)"]
    VM <-->|Encrypt / Decrypt PAN & CVV| KS["KeystoreVaultCipher\n(AndroidKeyStore AES-256-GCM)"]
    VM <-->|Base64 Ciphertext Only| Room["AegisPayDao\n(Encrypted Room SQLite Vault)"]
    Room -->|Flow.combine()| VM
    VM -->|Immutable AegisPayUiState| UI
```

---

## 🚀 Getting Started

1. **Clone & Run:**
   Open the project in Android Studio and run on any Android Emulator or physical device.
2. **Test the Core Security Workflows:**
   - Tap **`Unlock Vault (Biometric)`** on the virtual card to decrypt the 16-digit PAN and CVV via `AES/GCM/NoPadding` and observe the 20-second auto-relock timer.
   - Type **`Create a $50 burner card for gym membership`** in the bottom command bar to mint a new AES-256-GCM encrypted single-merchant card.
   - Tap **`NFC ACTIVE · TAP`** on the virtual card to trigger a tokenized APDU transaction, or tap **`Freeze Card`** on the AI Fraud Shield alert to lock the card and block NFC taps.
3. **Run the Unit Test Suite:**
   ```bash
   ./gradlew testDebugUnitTest
   ```
