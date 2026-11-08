package com.bekacodes.aegispay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bekacodes.aegispay.data.AegisPayDatabase
import com.bekacodes.aegispay.ui.AegisPayScreen
import com.bekacodes.aegispay.ui.AegisPayViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy { AegisPayDatabase.getInstance(applicationContext) }
    private val viewModel: AegisPayViewModel by viewModels {
        AegisPayViewModel.provideFactory(database.dao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AegisPayScreen(
                    uiState = uiState,
                    onIntent = viewModel::onIntent
                )
            }
        }
    }
}