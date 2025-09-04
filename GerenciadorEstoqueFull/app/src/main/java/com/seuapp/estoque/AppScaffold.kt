package com.seuapp.estoque

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Additional imports used by the individual screens below. These are grouped
// together here so that all imports reside at the top of the file as
// required by Kotlin's import conventions.
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * Top‑level scaffold for the application. This composable displays a
 * [TopAppBar] and a [TabRow] for navigation between the four functional
 * sections of the stock manager. Each tab delegates rendering to its own
 * composable defined in this file.
 *
 * @param viewModel Shared [EstoqueViewModel] instance used by all screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: EstoqueViewModel) {
    // Index of the currently selected tab
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cadastro de Produtos", "Estoque", "Relatórios", "Gerenciar Estoque")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Gerenciador de Estoque") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> CadastroProdutosScreen(viewModel)
                1 -> EstoqueScreen(viewModel)
                2 -> RelatoriosScreen(viewModel)
                3 -> GerenciarScreen(viewModel)
            }
        }
    }
}

// Forward declarations so that functions can be referenced above
@Composable
private fun CadastroProdutosScreen(viewModel: EstoqueViewModel)

@Composable
private fun EstoqueScreen(viewModel: EstoqueViewModel)

@Composable
private fun RelatoriosScreen(viewModel: EstoqueViewModel)

@Composable
private fun GerenciarScreen(viewModel: EstoqueViewModel)

// Implementation of the four screen composables follows. These definitions
// are placed after the forward declarations to avoid forward reference
// compilation issues and to improve readability.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadastroProdutosScreen(viewModel: EstoqueViewModel) {
    // Local state for the form fields
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var setor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var expandSetor by remember { mutableStateOf(false) }
    val setores = listOf("Açougue", "Hortifruti", "Outros")

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Cadastro de Produtos", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        // Código do Item
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código do Item") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição do Item
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição do Item") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Setor dropdown
        Box {
            OutlinedTextField(
                value = setor,
                onValueChange = {},
                label = { Text("Setor") },
                modifier = Modifier.fillMaxWidth().clickable { expandSetor = true },
                readOnly = true
            )
            DropdownMenu(expanded = expandSetor, onDismissRequest = { expandSetor = false }) {
                setores.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s) },
                        onClick = {
                            setor = s
                            expandSetor = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (codigo.isBlank() || descricao.isBlank() || setor.isBlank()) {
                    mensagem = "Preencha todos os campos"
                } else {
                    viewModel.upsertProduct(Product(codigo, descricao, setor))
                    mensagem = "Produto salvo/atualizado"
                    // Clear fields
                    codigo = ""
                    descricao = ""
                    setor = ""
                }
            }) {
                Text("Salvar")
            }
            Button(onClick = {
                if (codigo.isNotBlank()) {
                    viewModel.deleteProduct(codigo)
                    mensagem = "Produto removido"
                    codigo = ""
                    descricao = ""
                    setor = ""
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                Text("Excluir")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Display message if present
        if (mensagem.isNotEmpty()) {
            Text(text = mensagem, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Import CSV placeholder
        Button(onClick = { /* TODO: implementar importação */ }, enabled = false) {
            Text("Importar CSV")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstoqueScreen(viewModel: EstoqueViewModel) {
    // Sector filter state
    var setorAtual by remember { mutableStateOf("Açougue") }
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var quantidadeText by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    // Compute current total for selected code
    val totalAtual: Float? = viewModel.estoque.firstOrNull { it.codigo == codigo }?.total
    // List of inventory items filtered by sector
    val lista = viewModel.estoque.filter { it.setor == setorAtual }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Gerenciamento de Estoque", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Sector selection buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Açougue", "Hortifruti", "Outros").forEach { s ->
                val selected = s == setorAtual
                Button(
                    onClick = { setorAtual = s },
                    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text(s)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Código
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código do Item") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição do Item") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Quantidade
        OutlinedTextField(
            value = quantidadeText,
            onValueChange = { quantidadeText = it },
            label = { Text("Quantidade a Adicionar/Subtrair") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Total atual
        OutlinedTextField(
            value = totalAtual?.toString() ?: "",
            onValueChange = {},
            label = { Text("Total Atual") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Save button
        Button(onClick = {
            if (codigo.isBlank() || quantidadeText.isBlank()) {
                mensagem = "Código e quantidade são obrigatórios"
            } else {
                val qty = quantidadeText.replace(",", ".").toFloatOrNull()
                if (qty == null) {
                    mensagem = "Quantidade inválida"
                } else {
                    viewModel.updateInventory(codigo, descricao, setorAtual, qty)
                    mensagem = "Estoque atualizado"
                    quantidadeText = ""
                    // Recalculate total - Compose will update automatically
                }
            }
        }) {
            Text("Salvar")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (mensagem.isNotEmpty()) {
            Text(text = mensagem, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Itens em Estoque", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (lista.isEmpty()) {
            Text(text = "Nenhum item em estoque.")
        } else {
            LazyColumn {
                items(lista) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Populate form when clicked
                                codigo = item.codigo
                                descricao = item.descricao
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = item.codigo, fontWeight = FontWeight.Bold)
                                Text(text = item.descricao, color = Color.DarkGray)
                            }
                            Text(
                                text = String.format("%.1f", item.total),
                                color = if (item.total < 0) Color.Red else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatoriosScreen(viewModel: EstoqueViewModel) {
    // Choose whether to show inventory report or products report
    var showProducts by remember { mutableStateOf(false) }
    var setorAtual by remember { mutableStateOf("Açougue") }
    val setores = listOf("Açougue", "Hortifruti", "Outros")
    // Inventory list filtered by sector
    val inventoryList = viewModel.estoque.filter { it.setor == setorAtual }
    val productsList = viewModel.produtos.filter { it.setor == setorAtual }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Relatórios", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Buttons row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { /* TODO: exportar CSV */ }, enabled = false) {
                Text("Exportar para CSV")
            }
            Button(onClick = { /* TODO: exportar PDF */ }, enabled = false) {
                Text("Exportar para PDF")
            }
            Button(onClick = { showProducts = !showProducts }) {
                Text(if (showProducts) "Relatório de estoque" else "Relatório de produtos cadastrados")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Sector filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            setores.forEach { s ->
                val selected = s == setorAtual
                Button(
                    onClick = { setorAtual = s },
                    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) { Text(s) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = "Código", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(text = "Descrição", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            if (!showProducts) {
                Text(text = "Setor", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(text = "Total", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val itemsToShow = if (showProducts) productsList else inventoryList
        if (itemsToShow.isEmpty()) {
            Text(text = "Nenhum dado disponível.")
        } else {
            LazyColumn {
                if (showProducts) {
                    items(productsList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = item.codigo, modifier = Modifier.weight(1f))
                            Text(text = item.descricao, modifier = Modifier.weight(2f))
                        }
                    }
                } else {
                    items(inventoryList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = item.codigo, modifier = Modifier.weight(1f))
                            Text(text = item.descricao, modifier = Modifier.weight(2f))
                            Text(text = item.setor, modifier = Modifier.weight(1f))
                            Text(
                                text = String.format("%.1f", item.total),
                                modifier = Modifier.weight(1f),
                                color = if (item.total < 0) Color.Red else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GerenciarScreen(viewModel: EstoqueViewModel) {
    // Dialog states
    var showClearInventoryDialog by remember { mutableStateOf(false) }
    var showClearProductsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Gerenciar Estoque", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Use as ferramentas abaixo para gerenciar seu estoque de forma geral. Cuidado, esta ação não pode ser desfeita.")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showClearInventoryDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Apagar todos os itens do estoque")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showClearProductsDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Apagar todos itens cadastrados")
        }

        if (showClearInventoryDialog) {
            ConfirmationDialog(
                title = "Apagar Itens do Estoque",
                onConfirm = {
                    viewModel.clearInventory()
                    showClearInventoryDialog = false
                },
                onDismiss = { showClearInventoryDialog = false }
            )
        }
        if (showClearProductsDialog) {
            ConfirmationDialog(
                title = "Apagar Produtos e Estoque",
                onConfirm = {
                    viewModel.clearProducts()
                    showClearProductsDialog = false
                },
                onDismiss = { showClearProductsDialog = false }
            )
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (text == "APAGAR") {
                        onConfirm()
                    }
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text(title) },
        text = {
            Column {
                Text("Digite APAGAR para confirmar esta ação irreversível.")
                OutlinedTextField(value = text, onValueChange = { text = it })
            }
        }
    )
}