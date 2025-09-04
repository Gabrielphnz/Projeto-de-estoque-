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
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card

/**
 * Top level scaffold for the stock manager application.  Presents four
 * sections—product registration, inventory management, reports and
 * general management—via a tab interface.  Each section is implemented
 * as its own composable function below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: EstoqueViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cadastro de Produtos", "Estoque", "Relatórios", "Gerenciar Estoque")
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gerenciador de Estoque") })
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
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
        if (filteredProducts.isEmpty()) {
            Text("Nenhum produto encontrado.")
        } else {
            LazyColumn {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
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
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        // Clicking product fills the form for editing
                                        codigo = product.codigo
                                        descricao = product.descricao
                                        setor = product.setor
                                        mensagem = ""
                                    }
                            ) {
                                Text(product.codigo, fontWeight = FontWeight.Bold)
                                Text(product.descricao, color = Color.DarkGray)
                                Text(product.setor, color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
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

    // FocusRequester for directing focus to the quantity field after selecting a product
    val quantityFocusRequester = remember { FocusRequester() }
    // Additional focus requesters for code and description fields to support IME navigation
    val codeFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.padding(16.dp)) {
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
        Text(text = "Relatórios", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        // Actions row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { csvLauncher.launch("relatorio.csv") }) {
                Text("Exportar CSV")
            }
            Button(onClick = { pdfLauncher.launch("relatorio.pdf") }) {
                Text("Exportar PDF")
            }
            Button(onClick = { showProducts = !showProducts }) {
                Text(if (showProducts) "Relatório de estoque" else "Relatório de produtos cadastrados")
            }
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