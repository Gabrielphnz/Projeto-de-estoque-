package com.seuapp.estoque

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
            // Maintain login state.  Show a login screen until the user authenticates.
            var isLoggedIn by remember { mutableStateOf(viewModel.currentUser != null) }
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (!isLoggedIn) {
                        LoginScreen(viewModel) { isLoggedIn = true }
                    } else {
                        AppScaffold(viewModel)
                    }
                }
            }
        }
    }

    /**
     * Simple login screen.  Prompts the user for a username and password.
     * When valid credentials are entered [onLoginSuccess] is invoked.
     */
    @Composable
    fun LoginScreen(viewModel: EstoqueViewModel, onLoginSuccess: () -> Unit) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Login", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                if (viewModel.authenticate(username, password)) {
                    message = ""
                    onLoginSuccess()
                } else {
                    message = "Credenciais inválidas"
                }
            }) {
                Text("Entrar")
            }
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, color = Color.Red)
            }
        }
    }
}