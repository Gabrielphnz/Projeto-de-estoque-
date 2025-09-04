package com.seuapp.estoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable

/**
 * Main entry point for the application. This activity simply instantiates the
 * [EstoqueViewModel] and sets the top‑level Compose content. A custom
 * system UI controller is used so the status bar colour matches the app theme.
 */
class MainActivity : ComponentActivity() {
    /**
     * Lazily instantiate the shared [EstoqueViewModel] scoped to this activity.  All
     * screens in the app will share a single instance of the view model for
     * state management and persistence.
     */
    private val viewModel: EstoqueViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            /*
             * For simplicity we do not customise the system UI colours.  Using the
             * default Material 3 theme ensures the system bars pick an appropriate
             * colour automatically.  Should you wish to customise the status bar
             * colours you can introduce a rememberSystemUiController dependency
             * later on, but that requires adding the accompanist library.
             */
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppScaffold(viewModel)
                }
            }
        }
    }
}