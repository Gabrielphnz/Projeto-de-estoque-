package com.seuapp.estoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Main entry point for the application. This activity simply instantiates the
 * [EstoqueViewModel] and sets the top‑level Compose content. A custom
 * system UI controller is used so the status bar colour matches the app theme.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: EstoqueViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemUiController = rememberSystemUiController()
            val darkIcons = !isSystemInDarkTheme()
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = MaterialTheme.colorScheme.primary,
                    darkIcons = darkIcons
                )
            }
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppScaffold(viewModel)
                }
            }
        }
    }
}