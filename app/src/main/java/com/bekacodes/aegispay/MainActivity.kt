package com.bekacodes.aegispay

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bekacodes.aegispay.security.biometric.BiometricAuthManager
import com.bekacodes.aegispay.ui.AegisPayIntent
import com.bekacodes.aegispay.ui.AegisPayScreen
import com.bekacodes.aegispay.ui.AegisPayViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: AegisPayViewModel by viewModels()
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        biometricAuthManager = BiometricAuthManager(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AegisPayScreen(
                    uiState = uiState,
                    onIntent = { intent ->
                        handleIntent(intent)
                    },
                )
            }
        }
    }

    private fun handleIntent(intent: AegisPayIntent) {
        val isUnlockIntent = intent is AegisPayIntent.ToggleVaultLock && !viewModel.uiState.value.isVaultUnlocked
        if (isUnlockIntent) {
            biometricAuthManager.authenticate(
                onSuccess = {
                    viewModel.onIntent(AegisPayIntent.ToggleVaultLock)
                },
            )
        } else {
            viewModel.onIntent(intent)
        }
    }
}
