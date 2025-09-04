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
import androidx.compose.material3.ExposedDropdownMenuBox
// Removed ExposedDropdownMenu import because this API may not be available
// in the current Compose version. We use DropdownMenu inside
// ExposedDropdownMenuBox instead.  No separate icon imports are needed.
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

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

    // ActivityResult launcher for CSV import.  Accept both comma and
    // semicolon separated files with a text MIME type.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val content = reader.readText()
                    viewModel.importProductsFromCsv(content)
                    mensagem = "CSV importado com sucesso"
                }
            } catch (e: Exception) {
                mensagem = "Erro ao importar CSV"
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Cadastro de Produtos", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        // Código do Produto
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Selecção de setor via dropdown usando ExposedDropdownMenuBox para melhor acessibilidade
        var setorExpanded by remember { mutableStateOf(false) }
        // Use ExposedDropdownMenuBox paired with a DropdownMenu.  We avoid
        // ExposedDropdownMenu because it may not be available in the current
        // Compose version.  The TextField is read-only and clicking the
        // trailing arrow toggles the menu visibility.
        ExposedDropdownMenuBox(
            expanded = setorExpanded,
            onExpandedChange = { setorExpanded = !setorExpanded }
        ) {
            OutlinedTextField(
                value = setor,
                onValueChange = {},
                label = { Text("Setor") },
                readOnly = true,
                // No explicit trailing icon; ExposedDropdownMenuBox provides the
                // appropriate affordance automatically when paired with
                // DropdownMenu.
                modifier = Modifier
                    .fillMaxWidth()
            )
            DropdownMenu(
                expanded = setorExpanded,
                onDismissRequest = { setorExpanded = false }
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
    // Toggle to display non‑counted products instead of current inventory.
    var showNotCounted by remember { mutableStateOf(false) }

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
        Spacer(modifier = Modifier.height(12.dp))
        // Toggle to display products that have not been counted in this sector
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showNotCounted, onCheckedChange = { showNotCounted = it })
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Mostrar produtos não contados")
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Código field
        OutlinedTextField(
            value = codigo,
            onValueChange = { codigo = it },
            label = { Text("Código") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Descrição field
        OutlinedTextField(
            value = descricao,
            onValueChange = { descricao = it },
            label = { Text("Descrição") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Quantidade field
        OutlinedTextField(
            value = quantidadeText,
            onValueChange = { quantidadeText = it },
            label = { Text("Quantidade a Adicionar/Subtrair") },
            modifier = Modifier.fillMaxWidth()
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
                    mensagem = "Estoque atualizado"
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
        Text(text = if (showNotCounted) "Produtos não contados" else "Itens em Estoque", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (!showNotCounted && lista.isEmpty()) {
            Text(text = "Nenhum item em estoque.")
        } else if (showNotCounted && listaNaoContados.isEmpty()) {
            Text(text = "Todos os produtos foram contados.")
        } else {
            LazyColumn {
                if (showNotCounted) {
                    // Show list of products that have not yet been counted
                    items(listaNaoContados) { product ->
                        androidx.compose.material3.Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // Populate form when clicked and switch back to inventory mode
                                    codigo = product.codigo
                                    descricao = product.descricao
                                    showNotCounted = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = product.codigo, fontWeight = FontWeight.Bold)
                                    Text(text = product.descricao, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                } else {
                    // Show current inventory items
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