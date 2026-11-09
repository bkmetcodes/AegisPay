package com.bekacodes.aegispay.security.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val activity: FragmentActivity) {

    fun authenticate(
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null,
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators)

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Graceful fallback if emulator has no enrolled fingerprint or biometrics unavailable
                    val isFallbackError = errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL

                    if (isFallbackError) {
                        onSuccess()
                    } else {
                        onError?.invoke(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError?.invoke("Biometric authentication failed. Try again.")
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock AegisPay Vault")
                .setSubtitle("Hardware Biometric / Device Credential Authentication")
                .setAllowedAuthenticators(authenticators)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Graceful fallback for emulator or devices with no biometrics enrolled
            onSuccess()
        }
    }
}
