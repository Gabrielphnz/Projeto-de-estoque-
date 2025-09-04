package com.seuapp.estoque.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: EstoqueViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gerenciador de Estoque") })
        }
    ) { innerPadding ->
        Text(
            text = "Quantidade: ${vm.quantidade}",
            modifier = Modifier.padding(innerPadding)
        )
    }
}
