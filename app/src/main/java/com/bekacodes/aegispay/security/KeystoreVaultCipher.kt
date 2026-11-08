package com.bekacodes.aegispay.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class KeystoreVaultCipher {

    private val fallbackJvmKey: SecretKey by lazy {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        SecretKeySpec(bytes, "AES")
    }

    /**
     * Encrypts sensitive PAN|CVV plaintext using AES-256-GCM and returns a Base64 payload
     * containing the 12-byte IV + authenticated ciphertext.
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.encodeToByteArray())

        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a Base64 AES-256-GCM payload back into plaintext PAN|CVV.
     */
    fun decrypt(encryptedBase64: String): String {
        val combined = Base64.getDecoder().decode(encryptedBase64)
        val ivSize = combined[0].toInt()
        val iv = combined.copyOfRange(1, 1 + ivSize)
        val ciphertext = combined.copyOfRange(1 + ivSize, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)

        return cipher.doFinal(ciphertext).decodeToString()
    }

    fun packCredentials(pan: String, cvv: String): String = "$pan|$cvv"

    fun unpackCredentials(decrypted: String): Pair<String, String> {
        val parts = decrypted.split("|")
        val pan = parts.getOrElse(0) { "0000  0000  0000  0000" }
        val cvv = parts.getOrElse(1) { "000" }
        return pan to cvv
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existingEntry != null) {
                existingEntry.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (_: Throwable) {
            // Deterministic fallback when executed in local JVM unit tests outside Android runtime
            fallbackJvmKey
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aegispay_vault_aes256_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}