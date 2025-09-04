package com.seuapp.estoque.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Produto(val codigo: String, val descricao: String, val setor: String)
data class Mov(val timestamp: String, val quantity: Double, val type: String, val newTotal: Double)

class LocalStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("estoque_store", Context.MODE_PRIVATE)

    fun save(key: String, data: JSONObject) {
        prefs.edit().putString(key, data.toString()).apply()
    }
    fun load(key: String): JSONObject {
        val str = prefs.getString(key, "{}") ?: "{}"
        return try { JSONObject(str) } catch (_: Exception) { JSONObject() }
    }
}

class EstoqueViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app.applicationContext)

    // products: {codigo: {codigo, descricao, setor}}
    // inventory: {codigo: {total: Double}}
    // history: {codigo: [{timestamp, quantity, type, newTotal}]}
    var products by mutableStateOf(store.load("products"))
        private set
    var inventory by mutableStateOf(store.load("inventory"))
        private set
    var history by mutableStateOf(store.load("inventoryHistory"))
        private set
    var currentFilter by mutableStateOf("Açougue")
        private set

    fun setFilter(sector: String) {
        currentFilter = sector
    }

    private fun now(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun salvarProduto(codigo: String, descricao: String, setor: String): Boolean {
        if (codigo.isBlank() || descricao.isBlank() || setor.isBlank()) return false
        val entry = JSONObject().apply {
            put("codigo", codigo)
            put("descricao", descricao)
            put("setor", setor)
        }
        products.put(codigo, entry)
        store.save("products", products)
        return true
    }

    fun excluirProduto(codigo: String): Boolean {
        if (!products.has(codigo)) return false
        products.remove(codigo)
        inventory.remove(codigo)
        history.remove(codigo)
        store.save("products", products)
        store.save("inventory", inventory)
        store.save("inventoryHistory", history)
        return true
    }

    fun obterProduto(codigo: String): Produto? {
        if (!products.has(codigo)) return null
        val o = products.getJSONObject(codigo)
        return Produto(o.getString("codigo"), o.getString("descricao"), o.getString("setor"))
    }

    fun atualizarEstoque(codigo: String, descricao: String, quantidade: Double): Boolean {
        val prod = obterProduto(codigo) ?: return false
        if (prod.setor != currentFilter) return false
        val totalAtual = if (inventory.has(codigo)) inventory.getJSONObject(codigo).optDouble("total", 0.0) else 0.0
        val novoTotal = ((totalAtual * 10).toInt() + (quantidade * 10).toInt()) / 10.0
        val inv = JSONObject().apply {
            put("codigo", codigo)
            put("descricao", descricao)
            put("total", novoTotal)
        }
        inventory.put(codigo, inv)

        val arr = if (history.has(codigo)) history.getJSONArray(codigo) else JSONArray()
        val mov = JSONObject().apply {
            put("timestamp", now())
            put("quantity", quantidade)
            put("type", if (quantidade > 0) "Entrada" else "Saída")
            put("newTotal", novoTotal)
        }
        arr.put(mov)
        history.put(codigo, arr)

        store.save("inventory", inventory)
        store.save("inventoryHistory", history)
        return true
    }

    fun itensFiltradosComEstoque(): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val names = products.keys()
        while (names.hasNext()) {
            val cod = names.next()
            val p = products.getJSONObject(cod)
            if (p.getString("setor") != currentFilter) continue
            val total = if (inventory.has(cod)) inventory.getJSONObject(cod).optDouble("total", 0.0) else 0.0
            if (total != 0.0) {
                val obj = JSONObject()
                obj.put("codigo", cod)
                obj.put("descricao", p.getString("descricao"))
                obj.put("setor", p.getString("setor"))
                obj.put("total", total)
                list.add(obj)
            }
        }
        return list.sortedBy { it.getString("descricao") }
    }

    fun itensZeroOuNaoInformados(): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val names = products.keys()
        while (names.hasNext()) {
            val cod = names.next()
            val p = products.getJSONObject(cod)
            if (p.getString("setor") != currentFilter) continue
            val total = if (inventory.has(cod)) inventory.getJSONObject(cod).optDouble("total", 0.0) else 0.0
            if (total == 0.0) {
                val obj = JSONObject()
                obj.put("codigo", cod)
                obj.put("descricao", p.getString("descricao"))
                list.add(obj)
            }
        }
        return list.sortedBy { it.getString("descricao") }
    }

    fun limparTudo(confirmado: Boolean): Boolean {
        if (!confirmado) return false
        inventory = JSONObject()
        history = JSONObject()
        store.save("inventory", inventory)
        store.save("inventoryHistory", history)
        return true
    }

    fun limparProdutosDoSetor(confirmado: Boolean): Int {
        if (!confirmado) return 0
        val toDelete = mutableListOf<String>()
        val names = products.keys()
        while (names.hasNext()) {
            val cod = names.next()
            val p = products.getJSONObject(cod)
            if (p.getString("setor") == currentFilter) toDelete.add(cod)
        }
        toDelete.forEach { cod ->
            products.remove(cod)
            inventory.remove(cod)
            history.remove(cod)
        }
        store.save("products", products)
        store.save("inventory", inventory)
        store.save("inventoryHistory", history)
        return toDelete.size
    }
}
