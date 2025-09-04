package com.seuapp.estoque

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Overload conveniente: permite chamar AppScaffold() sem passar ViewModel explicitamente.
 */
@Composable
fun AppScaffold() {
    val vm: EstoqueViewModel = viewModel()
    AppScaffold(viewModel = vm)
}

/**
 * Assinatura que o seu MainActivity usa: AppScaffold(viewModel)
 * Material 3 (androidx.compose.material3.*)
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun AppScaffold(viewModel: EstoqueViewModel) {
    var showAllProductsDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gerenciador de Estoque",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        },
        content = { innerPadding: PaddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Bem-vindo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { selectedTab = 0 }) { Text("Estoque") }
                    Button(onClick = { selectedTab = 1 }) { Text("Cadastro") }
                    Button(onClick = { showAllProductsDialog = true }) { Text("Ver todos") }
                }
            }
        }
    )

    if (showAllProductsDialog) {
        AlertDialog(
            onDismissRequest = { showAllProductsDialog = false },
            title = { Text("Ver todos os produtos") },
            text = { Text("Confirme para abrir a lista completa de produtos.") },
            confirmButton = {
                TextButton(onClick = { showAllProductsDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllProductsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}