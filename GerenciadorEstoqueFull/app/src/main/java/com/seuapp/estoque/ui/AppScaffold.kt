package com.seuapp.estoque.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: EstoqueViewModel) {
    var tab by remember { mutableStateOf(1) } // 0=cadastro,1=estoque,2=relatorios,3=gerenciar
    val tabs = listOf("Cadastro de Produtos", "Estoque", "Relatórios", "Gerenciar")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Gerenciador de Estoque") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab==i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            // Filter buttons like HTML
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Açougue","Hortifruti","Outros").forEach { setor ->
                    val selected = vm.currentFilter == setor
                    AssistChip(
                        onClick = { vm.setFilter(setor) },
                        label = { Text(setor) },
                        leadingIcon = {},
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            when (tab) {
                0 -> CadastroTab(vm)
                1 -> EstoqueTab(vm)
                2 -> RelatoriosTab(vm)
                3 -> GerenciarTab(vm)
            }
        }
    }
}

@Composable
private fun CadastroTab(vm: EstoqueViewModel) {
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var setor by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cadastro de Produtos", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código do Item") })
        OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição do Item") })
        ExposedDropdownMenuBox(expanded = setor.isNotEmpty(), onExpandedChange = { }) {}
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Açougue","Hortifruti","Outros").forEach { s ->
                FilterChip(selected = setor==s, onClick = { setor = s },
                    label = { Text(s) },
                    modifier = Modifier.padding(end=8.dp))
            }
        }
        Button(onClick = {
            val ok = vm.salvarProduto(codigo.trim(), descricao.trim(), setor.trim())
            msg = if (ok) "Produto salvo com sucesso!" else "Preencha todos os campos."
            if (ok) { codigo=""; descricao=""; setor="" }
        }) { Text("Salvar") }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun EstoqueTab(vm: EstoqueViewModel) {
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("0") }
    var msg by remember { mutableStateOf<String?>(null) }

    fun preencherPorCodigo(cod: String) {
        val p = vm.obterProduto(cod)
        descricao = p?.descricao ?: ""
        total = if (p!=null) {
            val inv = vm.itensFiltradosComEstoque().find { it.getString("codigo")==cod }?.optDouble("total", 0.0) ?: 0.0
            if (vm.products.has(cod) && !vm.inventory.has(cod)) "0" else inv.toString()
        } else "0"
    }

    Column(Modifier.padding(16.dp)) {
        Text("Gerenciamento de Estoque", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = codigo, onValueChange = { codigo = it; preencherPorCodigo(it) }, label = { Text("Código do Item") })
        OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição do Item") })
        OutlinedTextField(value = quantidade, onValueChange = { quantidade = it }, label = { Text("Quantidade a Adicionar/Subtrair") })
        OutlinedTextField(value = total, onValueChange = { }, enabled = false, label = { Text("Total Atual") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val q = quantidade.toDoubleOrNull()
                if (codigo.isBlank() || descricao.isBlank() || q==null) {
                    msg = "Preencha todos os campos com valores válidos."
                } else {
                    val ok = vm.atualizarEstoque(codigo.trim(), descricao.trim(), (kotlin.math.round(q*10.0)/10.0))
                    msg = if (ok) "Estoque atualizado!" else "Produto não cadastrado ou pertence a outro setor."
                    if (ok) { codigo=""; descricao=""; quantidade=""; total="0" }
                }
            }) { Text("Salvar") }
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        Text("Itens em Estoque", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(vm.itensFiltradosComEstoque()) { item ->
                EstoqueListItem(item) { selected ->
                    codigo = selected.getString("codigo")
                    descricao = selected.getString("descricao")
                    total = selected.getDouble("total").toString()
                }
            }
        }
    }
}

@Composable
private fun EstoqueListItem(obj: JSONObject, onClick: (JSONObject)->Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(obj) }.padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${obj.getString("codigo")}: ${obj.getString("descricao")}", fontWeight = FontWeight.Medium)
        val total = obj.getDouble("total")
        val color = if (total < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Text("Total: $total", color = color)
    }
    Divider()
}

@Composable
private fun RelatoriosTab(vm: EstoqueViewModel) {
    val lista = vm.itensFiltradosComEstoque()
    Column(Modifier.padding(16.dp)) {
        Text("Relatórios", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(lista) { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.getString("codigo")} - ${item.getString("descricao")} (${item.getString("setor")})")
                    Text(item.getDouble("total").toString(), fontWeight = FontWeight.Bold)
                }
                Divider()
            }
        }
    }
}

@Composable
private fun GerenciarTab(vm: EstoqueViewModel) {
    var confirmText by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Gerenciar Estoque", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = confirmText, onValueChange = { confirmText = it }, label = { Text("Digite APAGAR para confirmar") })
        Button(
            onClick = {
                val ok = vm.limparTudo(confirmText.uppercase()=="APAGAR")
                msg = if (ok) "Estoque e histórico apagados." else "Digite APAGAR para confirmar."
                if (ok) confirmText = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Apagar todos os itens do estoque")
        }
        Button(
            onClick = {
                val count = vm.limparProdutosDoSetor(confirmText.uppercase()=="APAGAR")
                msg = if (count > 0) "$count produto(s) do setor ${vm.currentFilter} apagados." else "Nada apagado. Confirme digitando APAGAR."
                if (count>0) confirmText = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text("Apagar todos itens cadastrados do setor atual")
        }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
