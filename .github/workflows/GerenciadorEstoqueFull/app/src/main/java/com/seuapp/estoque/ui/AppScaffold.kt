package com.seuapp.estoque.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppScaffold(vm: EstoqueViewModel) {
    Scaffold(topBar = { TopAppBar(title = { Text("Gerenciador de Estoque") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("App pronto. Funcionalidades de CSV, PDF e Preview serão aqui.")
        }
    }
}
