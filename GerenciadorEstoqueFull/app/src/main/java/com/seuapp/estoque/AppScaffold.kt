@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.seuapp.estoque

import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
// Removed ExposedDropdownMenu import because this API may not be available
// in the current Compose version. We use DropdownMenu inside
// ExposedDropdownMenuBox instead.  No separate icon imports are needed.
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
// Use foundation keyboard types for compatibility with earlier Compose versions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
// Import Product type for suggestions list
import com.seuapp.estoque.Product

/**
 * Top level scaffold for the stock manager application.  Presents four
 * sections—product registration, inventory management, reports and
 * general management—via a tab interface.  Each section is implemented
 * as its own composable function below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: EstoqueViewModel) {
    /*
     * Determine which tabs to display based on the current user's permissions.
     * By default we always show the product, inventory, reports and general
     * management tabs.  An additional "Usuários" tab is shown for
     * administrators or users with the canManageUsers permission.  This
     * dynamic list drives both the TabRow and the body content below.
     */
    var selectedTab by remember { mutableStateOf(0) }
    val baseTabs = mutableListOf("Cadastro de Produtos", "Estoque", "Relatórios", "Gerenciar Estoque")
    val current = viewModel.currentUser
    val showUsersTab = current?.canManageUsers == true || current?.isAdmin == true
    if (showUsersTab) {
        baseTabs.add("Usuários")
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gerenciador de Estoque") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                baseTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTab) {
                0 -> CadastroProdutosScreen(viewModel)
                1 -> EstoqueScreen(viewModel)
                2 -> RelatoriosScreen(viewModel)
                3 -> GerenciarScreen(viewModel)
                // If users tab is enabled, it will be the last index
                4 -> UsuariosScreen(viewModel)
            }
        }
    }
}

/**
 * Screen for registering and managing products.  Allows the user to
 * create, update or remove products and import a list from a CSV file.
 */
@Composable
fun CadastroProdutosScreen(viewModel: EstoqueViewModel) {
    val context = LocalContext.current
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var setor by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    // Search term used to filter the products list on-the-fly
    var searchTerm by remember { mutableStateOf("") }
    // Multi-select state: holds the codes of currently selected products
    val selectedCodes = remember { mutableStateListOf<String>() }
    // Track selected sector for batch reassignment
    var batchSector by remember { mutableStateOf("") }
    var showBatchAssignDialog by remember { mutableStateOf(false) }

    // Show a full-screen dialog listing all products for easier management.
    var showAllProductsDialog by remember { mutableStateOf(false) }

    // Dropdown state for selecting a sector in the product form.  Defined at
    // this scope so that keyboard actions can modify it before the dropdown
    // component is declared.
    var setorExpanded by remember { mutableStateOf(false) }

    // ActivityResult launcher for CSV import.  Accept both comma and
    // semicolon separated files with a text MIME type.
    // Track whether to show the sector assignment dialog after importing CSVs with missing sector
    var showAssignSectorDialog by remember { mutableStateOf(false) }
    var selectedAssignSector by remember { mutableStateOf("") }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Attempt to persist read permission so that the file can be read
                // immediately.  Some document providers require this.
                try {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {
                    // silently ignore if we cannot persist the permission
                }
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val content = reader.readText()
                    viewModel.importProductsFromCsv(content)
                    // If any products were imported without a sector, prompt user to assign
                    if (viewModel.pendingImported.isNotEmpty()) {
                        showAssignSectorDialog = true
                        selectedAssignSector = ""
                    }
                    mensagem = "CSV importado com sucesso"
                }
            } catch (e: Exception) {
                mensagem = "Erro ao importar CSV"
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // If the current user does not have permission to edit products, show an informative message and return
        val canEditProducts = viewModel.currentUser?.canEditProducts ?: true
        if (!canEditProducts) {
            Text("Você não tem permissão para editar produtos.", color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            // Still show the list of products for reference
            Text(text = "Produtos Cadastrados", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(viewModel.produtos) { product ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Text(product.codigo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(product.descricao, modifier = Modifier.weight(2f))
                        Text(product.setor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            return
        }
        // Title
        Text(text = "Cadastro de Produtos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // Search bar to filter products by code or description
        OutlinedTextField(
            value = searchTerm,
            onValueChange = { searchTerm = it },
            label = { Text("Pesquisar produto") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* nothing */ })
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Código do Produto
        // Define focus requesters to support moving focus between fields via IME actions
        val descricaoFocusRequester = remember { FocusRequester() }

        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código") },
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { descricaoFocusRequester.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(descricaoFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = {
                    // When pressing next on description, open sector picker
                    setorExpanded = true
                }
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Seleção de setor via um botão que abre um menu suspenso.
        Box {
            Button(
                onClick = { setorExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (setor.isNotBlank()) setor else "Selecione o setor")
            }
            DropdownMenu(
                expanded = setorExpanded,
                onDismissRequest = { setorExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.setores.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s) },
                        onClick = {
                            setor = s
                            setorExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Action buttons for saving and deleting
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (codigo.isBlank() || descricao.isBlank() || setor.isBlank()) {
                    mensagem = "Preencha todos os campos"
                } else {
                    viewModel.upsertProduct(Product(codigo, descricao, setor))
                    mensagem = "Produto salvo/atualizado"
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
        if (mensagem.isNotEmpty()) {
            Text(text = mensagem, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        // CSV import button
        Button(onClick = {
            // Launch open document picker for CSV files
            importLauncher.launch(arrayOf("text/*", "text/csv", "text/plain"))
        }) {
            Text("Importar CSV")
        }

        // Button to open full list dialog for easier browsing and batch actions
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showAllProductsDialog = true }) {
            Text("Ver todos os produtos")
        }

        // Batch actions: if there are selected products, provide options to delete or move them
        if (selectedCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    // Delete all selected products
                    selectedCodes.forEach { code -> viewModel.deleteProduct(code) }
                    selectedCodes.clear()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Excluir selecionados")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    // Show dialog to assign selected products to a sector
                    showBatchAssignDialog = true
                    batchSector = ""
                }) {
                    Text("Mover setor")
                }
            }
        }

        // If products were imported without a sector, prompt the user to assign a sector
        if (showAssignSectorDialog) {
            AlertDialog(
                onDismissRequest = { showAssignSectorDialog = false },
                title = { Text("Atribuir setor aos produtos importados") },
                text = {
                    Column {
                        Text("Selecione um setor para os produtos importados sem setor.")
                        Spacer(modifier = Modifier.height(8.dp))
                        // Dropdown for choosing the sector
                        var expandAssign by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { expandAssign = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (selectedAssignSector.isNotBlank()) selectedAssignSector else "Selecione o setor")
                            }
                            DropdownMenu(
                                expanded = expandAssign,
                                onDismissRequest = { expandAssign = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.setores.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = {
                                            selectedAssignSector = s
                                            expandAssign = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (selectedAssignSector.isNotBlank()) {
                            viewModel.assignSectorToPending(selectedAssignSector)
                            showAssignSectorDialog = false
                            mensagem = "Produtos importados atribuídos ao setor " + selectedAssignSector
                        }
                    }) {
                        Text("Atribuir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAssignSectorDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Dialog for batch assigning selected products to a sector
        if (showBatchAssignDialog) {
            AlertDialog(
                onDismissRequest = { showBatchAssignDialog = false },
                title = { Text("Reatribuir setor") },
                text = {
                    Column {
                        Text("Escolha o setor para os produtos selecionados:")
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandBatchAssign by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { expandBatchAssign = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (batchSector.isNotBlank()) batchSector else "Selecione o setor")
                            }
                            DropdownMenu(
                                expanded = expandBatchAssign,
                                onDismissRequest = { expandBatchAssign = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                viewModel.setores.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = {
                                            batchSector = s
                                            expandBatchAssign = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (batchSector.isNotBlank()) {
                            viewModel.assignSectorToProducts(selectedCodes.toList(), batchSector)
                            selectedCodes.clear()
                            showBatchAssignDialog = false
                        }
                    }) { Text("Salvar") }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchAssignDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Section listing existing products with search and multi-select
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Produtos Cadastrados", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val filteredProducts = viewModel.produtos.filter { p ->
            p.codigo.contains(searchTerm, ignoreCase = true) ||
                p.descricao.contains(searchTerm, ignoreCase = true)
        }
        // Provide a larger scrollable area using weight so that the list can grow
        if (filteredProducts.isEmpty()) {
            Text("Nenhum produto encontrado.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox for multi-select
                            val isChecked = selectedCodes.contains(product.codigo)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selectedCodes.contains(product.codigo)) {
                                            selectedCodes.add(product.codigo)
                                        }
                                    } else {
                                        selectedCodes.remove(product.codigo)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Row with code, description and sector side by side
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        codigo = product.codigo
                                        descricao = product.descricao
                                        setor = product.setor
                                        mensagem = ""
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(product.codigo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(product.descricao, color = Color.DarkGray, modifier = Modifier.weight(2f))
                                Text(product.setor, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Full screen dialog listing all products for bulk management
    if (showAllProductsDialog) {
        AlertDialog(
            onDismissRequest = { showAllProductsDialog = false },
            title = { Text("Lista completa de produtos") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Scrollable list of all products (filtered by searchTerm)
                    val allProducts = if (searchTerm.isBlank()) viewModel.produtos else filteredProducts
                    LazyColumn {
                        items(allProducts) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        codigo = product.codigo
                                        descricao = product.descricao
                                        setor = product.setor
                                        showAllProductsDialog = false
                                        mensagem = ""
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val checked = selectedCodes.contains(product.codigo)
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedCodes.contains(product.codigo)) selectedCodes.add(product.codigo)
                                        } else {
                                            selectedCodes.remove(product.codigo)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(product.codigo, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(product.descricao, modifier = Modifier.weight(2f))
                                Text(product.setor, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                // Actions appear only when at least one product is selected
                Column {
                    if (selectedCodes.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {
                                // Delete selected
                                selectedCodes.forEach { code -> viewModel.deleteProduct(code) }
                                selectedCodes.clear()
                                showAllProductsDialog = false
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Excluir selecionados")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                showBatchAssignDialog = true
                                showAllProductsDialog = false
                            }) {
                                Text("Mover setor")
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllProductsDialog = false }) { Text("Fechar") }
            }
        )
    }
}

/**
 * Screen for managing stock levels.  Users can select a sector,
 * increment or decrement quantities and see the list of items currently
 * in stock.
 */
@Composable
fun EstoqueScreen(viewModel: EstoqueViewModel) {
    var setorAtual by remember { mutableStateOf(viewModel.setores.firstOrNull() ?: "") }
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var quantidadeText by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    // When updating the form, calculate the current total for the code if it exists in inventory.
    val totalAtual: Float? = viewModel.estoque.firstOrNull { it.codigo == codigo }?.total
    // Compute list of inventory items for the selected sector.
    val lista = viewModel.estoque.filter { it.setor == setorAtual }
    // Compute list of products not yet counted in the selected sector.
    val listaNaoContados = viewModel.produtos.filter { it.setor == setorAtual }
        .filter { p -> viewModel.estoque.none { it.codigo == p.codigo } }
    // Dialog toggle for selecting products not yet counted
    var showNaoContadosDialog by remember { mutableStateOf(false) }

    // Suggestion dialog state when searching by code or description.  When
    // the user presses enter on a code or description that does not match
    // exactly one product, this dialog appears with a list of possible
    // matches for the user to choose from.
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    val suggestionResults = remember { mutableStateListOf<Product>() }

    // FocusRequester for directing focus to the quantity field after selecting a product
    val quantityFocusRequester = remember { FocusRequester() }
    // Additional focus requesters for code and description fields to support IME navigation
    val codeFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }

    // Helper functions to handle searching when the user presses enter on the
    // code or description fields.  They update the description/code fields
    // if an exact match is found, otherwise populate the suggestion list.
    fun handleCodeEntered() {
        val text = codigo.trim()
        if (text.isNotEmpty()) {
            // Try exact match by code
            val exact = viewModel.produtos.firstOrNull { it.codigo.equals(text, ignoreCase = true) }
            if (exact != null) {
                descricao = exact.descricao
                // focus on quantity field
                quantityFocusRequester.requestFocus()
                return
            }
            // Try exact match by description
            val matches = viewModel.produtos.filter { it.codigo.contains(text, ignoreCase = true) || it.descricao.contains(text, ignoreCase = true) }
            if (matches.size == 1) {
                codigo = matches[0].codigo
                descricao = matches[0].descricao
                quantityFocusRequester.requestFocus()
                return
            }
            if (matches.isNotEmpty()) {
                suggestionResults.clear()
                suggestionResults.addAll(matches)
                showSuggestionsDialog = true
            } else {
                mensagem = "Produto não encontrado"
            }
        }
    }
    fun handleDescriptionEntered() {
        val text = descricao.trim()
        if (text.isNotEmpty()) {
            // Try exact match by description
            val exact = viewModel.produtos.firstOrNull { it.descricao.equals(text, ignoreCase = true) }
            if (exact != null) {
                codigo = exact.codigo
                descricao = exact.descricao
                quantityFocusRequester.requestFocus()
                return
            }
            val matches = viewModel.produtos.filter { it.descricao.contains(text, ignoreCase = true) || it.codigo.contains(text, ignoreCase = true) }
            if (matches.size == 1) {
                codigo = matches[0].codigo
                descricao = matches[0].descricao
                quantityFocusRequester.requestFocus()
                return
            }
            if (matches.isNotEmpty()) {
                suggestionResults.clear()
                suggestionResults.addAll(matches)
                showSuggestionsDialog = true
            } else {
                mensagem = "Produto não encontrado"
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // If user cannot edit inventory, show read-only message and return
        val canEditInventory = viewModel.currentUser?.canEditInventory ?: true
        if (!canEditInventory) {
            Text("Você não tem permissão para editar o estoque.", color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Itens em Estoque", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val listAll = viewModel.estoque.filter { it.setor == setorAtual }
            if (listAll.isEmpty()) {
                Text("Nenhum item em estoque.")
            } else {
                LazyColumn {
                    items(listAll) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.codigo, fontWeight = FontWeight.Bold)
                                Text(item.descricao, color = Color.DarkGray)
                            }
                            Text(
                                text = String.format("%.1f", item.total),
                                color = if (item.total < 0) Color.Red else Color.Black
                            )
                        }
                    }
                }
            }
            return
        }
        Text(text = "Gerenciamento de Estoque", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Sector buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.setores.forEach { s ->
                val selected = s == setorAtual
                Button(
                    onClick = { setorAtual = s },
                    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Text(s)
                }
            }
        }

        // Dialog listing products not yet counted.  When a product is selected
        // its code and description are filled into the form and focus moves
        // to the quantity field.
        if (showNaoContadosDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showNaoContadosDialog = false },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showNaoContadosDialog = false }) { Text("Fechar") }
                },
                title = { Text("Produtos não contados") },
                text = {
                    if (listaNaoContados.isEmpty()) {
                        Text("Todos os produtos já foram contados neste setor.")
                    } else {
                        LazyColumn {
                            items(listaNaoContados) { product ->
                                androidx.compose.material3.Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            codigo = product.codigo
                                            descricao = product.descricao
                                            quantidadeText = ""
                                            showNaoContadosDialog = false
                                            // request focus on quantity field
                                            quantityFocusRequester.requestFocus()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(product.codigo, fontWeight = FontWeight.Bold)
                                            Text(product.descricao, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Suggestion dialog for approximate searches.  When multiple products
        // match the entered code or description, this list allows the user
        // to choose the correct product.  Selecting an item fills the code
        // and description fields and focuses the quantity input.
        if (showSuggestionsDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSuggestionsDialog = false },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSuggestionsDialog = false }) { Text("Fechar") }
                },
                title = { Text("Selecione o produto") },
                text = {
                    if (suggestionResults.isEmpty()) {
                        Text("Nenhum produto encontrado.")
                    } else {
                        LazyColumn {
                            items(suggestionResults) { product ->
                                androidx.compose.material3.Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            codigo = product.codigo
                                            descricao = product.descricao
                                            showSuggestionsDialog = false
                                            // move focus to quantity field
                                            quantityFocusRequester.requestFocus()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(product.codigo, fontWeight = FontWeight.Bold)
                                            Text(product.descricao, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Botão para abrir a lista de produtos não contados no setor atual
        Button(onClick = {
            showNaoContadosDialog = true
        }) {
            Text("Produtos não contados")
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Código field
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(codeFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                // When pressing next, attempt to auto fill using the code
                handleCodeEntered()
                descFocusRequester.requestFocus()
            })
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição field
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(descFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                // When pressing next, attempt to auto fill using the description
                handleDescriptionEntered()
                quantityFocusRequester.requestFocus()
            })
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Quantidade field (focusable after selecting um produto não contado)
        OutlinedTextField(
            value = quantidadeText,
            onValueChange = { quantidadeText = it },
            label = { Text("Quantidade a Adicionar/Subtrair") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(quantityFocusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                // Trigger save logic on done action
                if (codigo.isBlank() || quantidadeText.isBlank()) {
                    mensagem = "Código e quantidade são obrigatórios"
                } else {
                    val qty = quantidadeText.replace(",", ".").toFloatOrNull()
                    if (qty == null) {
                        mensagem = "Quantidade inválida"
                    } else {
                        viewModel.updateInventory(codigo, descricao, setorAtual, qty)
                        mensagem = "Estoque atualizado"
                        codigo = ""
                        descricao = ""
                        quantidadeText = ""
                    }
                }
            })
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Total atual (read only)
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
                    // Clear the form fields after updating to make the workflow obvious for users
                    mensagem = "Estoque atualizado"
                    codigo = ""
                    descricao = ""
                    quantidadeText = ""
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
                    androidx.compose.material3.Card(
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

/**
 * Reports screen.  Displays either the list of products or inventory for
 * a selected sector and allows exporting the current report as CSV or PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatoriosScreen(viewModel: EstoqueViewModel) {
    val context = LocalContext.current
    var showProducts by remember { mutableStateOf(false) }
    var setorAtual by remember { mutableStateOf(viewModel.setores.firstOrNull() ?: "") }
    val inventoryList = viewModel.estoque.filter { it.setor == setorAtual }
    val productsList = viewModel.produtos.filter { it.setor == setorAtual }

    // CSV export launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    val csv = if (showProducts) {
                        viewModel.generateProductsCsv(setorAtual)
                    } else {
                        viewModel.generateInventoryCsv(setorAtual)
                    }
                    writer.write(csv)
                }
            } catch (_: Exception) {
                // ignore failures silently
            }
        }
    }
    // PDF export launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val pdfDocument = PdfDocument()
                val paint = android.graphics.Paint()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                var page = pdfDocument.startPage(pageInfo)
                var y = 40f
                paint.textSize = 12f
                if (showProducts) {
                    // header
                    page.canvas.drawText("Código    Descrição", 10f, y, paint)
                    y += 20f
                    productsList.forEach { item ->
                        val line = "${item.codigo}    ${item.descricao}"
                        page.canvas.drawText(line, 10f, y, paint)
                        y += 20f
                        if (y > 800f) {
                            pdfDocument.finishPage(page)
                            page = pdfDocument.startPage(pageInfo)
                            y = 40f
                            page.canvas.drawText("Código    Descrição", 10f, y, paint)
                            y += 20f
                        }
                    }
                } else {
                    // header
                    page.canvas.drawText("Código    Descrição    Setor    Total", 10f, y, paint)
                    y += 20f
                    inventoryList.forEach { item ->
                        val totalStr = String.format("%.1f", item.total)
                        val line = "${item.codigo}    ${item.descricao}    ${item.setor}    $totalStr"
                        page.canvas.drawText(line, 10f, y, paint)
                        y += 20f
                        if (y > 800f) {
                            pdfDocument.finishPage(page)
                            page = pdfDocument.startPage(pageInfo)
                            y = 40f
                            page.canvas.drawText("Código    Descrição    Setor    Total", 10f, y, paint)
                            y += 20f
                        }
                    }
                }
                pdfDocument.finishPage(page)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()
            } catch (_: Exception) {
                // ignore failures silently
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Restrict access if user lacks permission
        val canViewReports = viewModel.currentUser?.canViewReports ?: true
        if (!canViewReports) {
            Text("Você não tem permissão para visualizar relatórios.", color = Color.Red)
            return
        }
        Text(text = "Relatórios", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Actions row: export buttons on the first row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { csvLauncher.launch("relatorio.csv") }) {
                Text("Exportar CSV")
            }
            Button(onClick = { pdfLauncher.launch("relatorio.pdf") }) {
                Text("Exportar PDF")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Toggle button placed below in a full width rectangular style
        Button(onClick = { showProducts = !showProducts }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showProducts) "Relatório de estoque" else "Relatório de produtos cadastrados")
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Sector filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.setores.forEach { s ->
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
        // Table header with light grey background and bold text for readability
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            Text(
                text = "Código",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = "Descrição",
                modifier = Modifier.weight(2f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (!showProducts) {
                Text(
                    text = "Setor",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Total",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val itemsToShow = if (showProducts) productsList else inventoryList
        if (itemsToShow.isEmpty()) {
            Text(text = "Nenhum dado disponível.")
        } else {
            LazyColumn {
                if (showProducts) {
                    itemsIndexed(productsList) { index, item ->
                        val backgroundColor = if (index % 2 == 0) Color(0xFFF7F7F7) else Color.White
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Text(text = item.codigo, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            Text(text = item.descricao, modifier = Modifier.weight(2f), fontSize = 13.sp)
                        }
                    }
                } else {
                    itemsIndexed(inventoryList) { index, item ->
                        val backgroundColor = if (index % 2 == 0) Color(0xFFF7F7F7) else Color.White
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Text(text = item.codigo, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            Text(text = item.descricao, modifier = Modifier.weight(2f), fontSize = 13.sp)
                            Text(text = item.setor, modifier = Modifier.weight(1f), fontSize = 13.sp)
                            Text(
                                text = String.format("%.1f", item.total),
                                modifier = Modifier.weight(1f),
                                color = if (item.total < 0) Color.Red else Color.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * General management screen.  Provides options to clear inventory and
 * products and to manage the list of sectors (add, rename or remove).
 */
@Composable
fun GerenciarScreen(viewModel: EstoqueViewModel) {
    var showClearInventoryDialog by remember { mutableStateOf(false) }
    var showClearProductsDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSectorName by remember { mutableStateOf("") }
    var sectorToEdit by remember { mutableStateOf<String?>(null) }
    var editSectorName by remember { mutableStateOf("") }
    var sectorToDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        val user = viewModel.currentUser
        val allowed = user?.isAdmin == true || user?.canManageUsers == true
        if (!allowed) {
            Text("Você não tem permissão para acessar esta área.", color = Color.Red)
        } else {
            Text(text = "Gerenciar Estoque", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Use as ferramentas abaixo para gerenciar seu estoque de forma geral. Cuidado, estas ações não podem ser desfeitas.")
            Spacer(modifier = Modifier.height(16.dp))
            // Danger actions
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Gerenciar Setores", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // List of sectors with edit/delete buttons
            viewModel.setores.forEach { setorName ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(setorName, modifier = Modifier.weight(1f))
                    Button(onClick = {
                        sectorToEdit = setorName
                        editSectorName = setorName
                    }, modifier = Modifier.padding(horizontal = 2.dp)) {
                        Text("Editar")
                    }
                    Button(onClick = { sectorToDelete = setorName }, modifier = Modifier.padding(horizontal = 2.dp)) {
                        Text("Excluir")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showAddDialog = true }) {
                Text("Adicionar Setor")
            }
        }
    }

    // Note: confirmation dialogs are implemented below using local state variables.
    if (showClearInventoryDialog) {
        var confirmationText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClearInventoryDialog = false },
            title = { Text("Apagar Itens do Estoque") },
            text = {
                Column {
                    Text("Digite APAGAR para confirmar esta ação irreversível.")
                    OutlinedTextField(value = confirmationText, onValueChange = { confirmationText = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmationText == "APAGAR") {
                        viewModel.clearInventory()
                        showClearInventoryDialog = false
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearInventoryDialog = false }) { Text("Cancelar") }
            }
        )
    }
    if (showClearProductsDialog) {
        var confirmationText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClearProductsDialog = false },
            title = { Text("Apagar Produtos e Estoque") },
            text = {
                Column {
                    Text("Digite APAGAR para confirmar esta ação irreversível.")
                    OutlinedTextField(value = confirmationText, onValueChange = { confirmationText = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (confirmationText == "APAGAR") {
                        viewModel.clearProducts()
                        showClearProductsDialog = false
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showClearProductsDialog = false }) { Text("Cancelar") }
            }
        )
    }
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar novo setor") },
            text = {
                OutlinedTextField(value = newSectorName, onValueChange = { newSectorName = it }, label = { Text("Nome do setor") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addSector(newSectorName)
                    newSectorName = ""
                    showAddDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
    if (sectorToEdit != null) {
        AlertDialog(
            onDismissRequest = { sectorToEdit = null },
            title = { Text("Editar setor") },
            text = {
                OutlinedTextField(value = editSectorName, onValueChange = { editSectorName = it }, label = { Text("Nome do setor") })
            },
            confirmButton = {
                TextButton(onClick = {
                    sectorToEdit?.let { oldName -> viewModel.editSector(oldName, editSectorName) }
                    sectorToEdit = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { sectorToEdit = null }) { Text("Cancelar") }
            }
        )
    }
    if (sectorToDelete != null) {
        AlertDialog(
            onDismissRequest = { sectorToDelete = null },
            title = { Text("Excluir setor") },
            text = {
                Text("Tem certeza que deseja excluir o setor \"${sectorToDelete}\"? Todos os produtos e itens desse setor serão removidos.")
            },
            confirmButton = {
                TextButton(onClick = {
                    sectorToDelete?.let { name -> viewModel.deleteSector(name) }
                    sectorToDelete = null
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { sectorToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Screen for managing application users and their permissions.  Visible only
 * to administrators or users with the canManageUsers permission.  Allows
 * adding new users with a username, password and a set of permissions, as
 * well as editing or deleting existing users.  The admin user cannot be
 * deleted nor can its permissions be modified.
 */
@Composable
fun UsuariosScreen(viewModel: EstoqueViewModel) {
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newCanEditProducts by remember { mutableStateOf(true) }
    var newCanEditInventory by remember { mutableStateOf(true) }
    var newCanViewReports by remember { mutableStateOf(true) }
    var newCanManageUsers by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Gerenciar Usuários", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Adicione novos usuários e atribua permissões. O usuário 'admin' possui acesso total e não pode ser removido.")
        Spacer(modifier = Modifier.height(12.dp))
        // New user form
        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it },
            label = { Text("Nome de usuário") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Permissions checkboxes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = newCanEditProducts, onCheckedChange = { newCanEditProducts = it })
            Text("Pode editar produtos", modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = newCanEditInventory, onCheckedChange = { newCanEditInventory = it })
            Text("Pode editar estoque", modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = newCanViewReports, onCheckedChange = { newCanViewReports = it })
            Text("Pode ver relatórios", modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = newCanManageUsers, onCheckedChange = { newCanManageUsers = it })
            Text("Pode gerenciar usuários", modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (newUsername.isBlank() || newPassword.isBlank()) {
                message = "Usuário e senha são obrigatórios"
            } else {
                // Only add if username not taken
                if (viewModel.users.any { it.username.equals(newUsername, ignoreCase = true) }) {
                    message = "Usuário já existe"
                } else {
                    viewModel.addUser(
                        EstoqueViewModel.User(
                            username = newUsername,
                            password = newPassword,
                            canEditProducts = newCanEditProducts,
                            canEditInventory = newCanEditInventory,
                            canViewReports = newCanViewReports,
                            canManageUsers = newCanManageUsers,
                            isAdmin = false
                        )
                    )
                    newUsername = ""
                    newPassword = ""
                    newCanEditProducts = true
                    newCanEditInventory = true
                    newCanViewReports = true
                    newCanManageUsers = false
                    message = "Usuário cadastrado"
                }
            }
        }) {
            Text("Adicionar Usuário")
        }
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Usuários Cadastrados", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // List existing users
        LazyColumn {
            items(viewModel.users) { user ->
                // Row for each user with permission checkboxes and delete button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.username, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            // Delete button for non-admin users
                            if (!user.isAdmin) {
                                Button(onClick = { viewModel.deleteUser(user.username) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                    Text("Excluir")
                                }
                            } else {
                                Text("(Administrador)", color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Only allow editing permissions for non-admin users
                        if (!user.isAdmin) {
                            var editProd by remember { mutableStateOf(user.canEditProducts) }
                            var editInv by remember { mutableStateOf(user.canEditInventory) }
                            var viewRep by remember { mutableStateOf(user.canViewReports) }
                            var manage by remember { mutableStateOf(user.canManageUsers) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editProd, onCheckedChange = { checked ->
                                    editProd = checked
                                    viewModel.updateUserPermissions(user.username, editProd, editInv, viewRep, manage)
                                })
                                Text("Editar Produtos", modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = editInv, onCheckedChange = { checked ->
                                    editInv = checked
                                    viewModel.updateUserPermissions(user.username, editProd, editInv, viewRep, manage)
                                })
                                Text("Editar Estoque", modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = viewRep, onCheckedChange = { checked ->
                                    viewRep = checked
                                    viewModel.updateUserPermissions(user.username, editProd, editInv, viewRep, manage)
                                })
                                Text("Ver Relatórios", modifier = Modifier.weight(1f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = manage, onCheckedChange = { checked ->
                                    manage = checked
                                    viewModel.updateUserPermissions(user.username, editProd, editInv, viewRep, manage)
                                })
                                Text("Gerenciar Usuários", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * User management screen.  Only available to admin users.  Allows adding
 * new users and editing permissions for existing users.  The admin user
 * cannot be deleted.
 */
@Composable
fun UsuariosScreen(viewModel: EstoqueViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var canEditProducts by remember { mutableStateOf(true) }
    var canEditInventory by remember { mutableStateOf(true) }
    var canViewReports by remember { mutableStateOf(true) }
    var canManageUsers by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Cadastro de Usuários", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Nome de usuário") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = canEditProducts, onCheckedChange = { canEditProducts = it })
            Text("Pode editar produtos")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = canEditInventory, onCheckedChange = { canEditInventory = it })
            Text("Pode editar estoque")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = canViewReports, onCheckedChange = { canViewReports = it })
            Text("Pode ver relatórios")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = canManageUsers, onCheckedChange = { canManageUsers = it })
            Text("Pode gerenciar usuários")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (username.isBlank() || password.isBlank()) {
                message = "Nome de usuário e senha são obrigatórios"
            } else {
                viewModel.addUser(EstoqueViewModel.User(username, password, canEditProducts, canEditInventory, canViewReports, canManageUsers, isAdmin = false))
                message = "Usuário adicionado"
                username = ""
                password = ""
                canEditProducts = true
                canEditInventory = true
                canViewReports = true
                canManageUsers = false
            }
        }) {
            Text("Adicionar Usuário")
        }
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(message, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Usuários Cadastrados", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn {
            items(viewModel.users) { user ->
                if (!user.isAdmin) {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.username, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Button(onClick = { viewModel.deleteUser(user.username) }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                                    Text("Excluir")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = user.canEditProducts, onCheckedChange = {
                                    viewModel.updateUserPermissions(user.username, it, user.canEditInventory, user.canViewReports, user.canManageUsers)
                                })
                                Text("Produtos")
                                Spacer(modifier = Modifier.width(8.dp))
                                Checkbox(checked = user.canEditInventory, onCheckedChange = {
                                    viewModel.updateUserPermissions(user.username, user.canEditProducts, it, user.canViewReports, user.canManageUsers)
                                })
                                Text("Estoque")
                                Spacer(modifier = Modifier.width(8.dp))
                                Checkbox(checked = user.canViewReports, onCheckedChange = {
                                    viewModel.updateUserPermissions(user.username, user.canEditProducts, user.canEditInventory, it, user.canManageUsers)
                                })
                                Text("Relatórios")
                                Spacer(modifier = Modifier.width(8.dp))
                                Checkbox(checked = user.canManageUsers, onCheckedChange = {
                                    viewModel.updateUserPermissions(user.username, user.canEditProducts, user.canEditInventory, user.canViewReports, it)
                                })
                                Text("Usuários")
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(user.username + " (admin)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}