package com.seuapp.estoque

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Data model classes used by the app. A [Product] represents an item that can
 * be stocked. An [InventoryItem] represents the current quantity on hand for
 * a product. An [InventoryMovement] records a change to the inventory. All
 * three types are serialised to and from JSON via [Gson] for persistence.
 */
data class Product(
    val codigo: String,
    val descricao: String,
    val setor: String
)

data class InventoryItem(
    val codigo: String,
    val descricao: String,
    val setor: String,
    var total: Float
)

data class InventoryMovement(
    val codigo: String,
    val descricao: String,
    val setor: String,
    val quantidade: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel responsible for managing products, current inventory and inventory
 * history. State is maintained in [mutableStateListOf] so Compose recomposes
 * when lists change. The lists are persisted in [SharedPreferences] as JSON.
 */
class EstoqueViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("estoque_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Mutable lists backing Compose state. When items are added or removed
    // Compose will update the UI accordingly.
    val produtos = mutableStateListOf<Product>()
    val estoque = mutableStateListOf<InventoryItem>()
    val historico = mutableStateListOf<InventoryMovement>()

    init {
        loadData()
    }

    /**
     * Load saved data from SharedPreferences. If no saved data is present
     * the lists remain empty. Any corrupt JSON is ignored to avoid crashes.
     */
    private fun loadData() {
        // Load products
        val productsJson = prefs.getString("products", null)
        if (!productsJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<Product>>() {}.type
                produtos.clear()
                produtos.addAll(gson.fromJson(productsJson, type))
            } catch (e: Exception) {
                // ignore corrupt data
            }
        }
        // Load inventory
        val invJson = prefs.getString("inventory", null)
        if (!invJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<InventoryItem>>() {}.type
                estoque.clear()
                estoque.addAll(gson.fromJson(invJson, type))
            } catch (e: Exception) {
                // ignore corrupt data
            }
        }
        // Load history
        val histJson = prefs.getString("history", null)
        if (!histJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<InventoryMovement>>() {}.type
                historico.clear()
                historico.addAll(gson.fromJson(histJson, type))
            } catch (e: Exception) {
                // ignore corrupt data
            }
        }
    }

    /**
     * Persist current state to SharedPreferences. Each list is
     * serialised separately to its own key.
     */
    private fun persistData() {
        prefs.edit().apply {
            putString("products", gson.toJson(produtos))
            putString("inventory", gson.toJson(estoque))
            putString("history", gson.toJson(historico))
            apply()
        }
    }

    /**
     * Add a new product or update an existing one. The product is uniquely
     * identified by its [codigo]. If a product with the same code already
     * exists it will be replaced; otherwise it will be appended. After
     * modification the data is persisted.
     */
    fun upsertProduct(product: Product) {
        val existingIndex = produtos.indexOfFirst { it.codigo == product.codigo }
        if (existingIndex >= 0) {
            produtos[existingIndex] = product
        } else {
            produtos.add(product)
        }
        persistData()
    }

    /**
     * Remove a product by its code. This also removes any inventory entries
     * associated with that code. If the code does not exist nothing happens.
     */
    fun deleteProduct(codigo: String) {
        produtos.removeAll { it.codigo == codigo }
        estoque.removeAll { it.codigo == codigo }
        persistData()
    }

    /**
     * Update the inventory for a given product. If the product does not yet
     * exist in the inventory it is created. Quantities can be positive or
     * negative. An entry in the history is also recorded.
     *
     * @param codigo Code of the product being updated
     * @param descricao Product description (used if a new entry is created)
     * @param setor Sector (used if a new entry is created)
     * @param quantidade Quantity to add (or subtract if negative)
     */
    fun updateInventory(codigo: String, descricao: String, setor: String, quantidade: Float) {
        if (codigo.isBlank()) return
        // Find existing inventory entry by code
        val index = estoque.indexOfFirst { it.codigo == codigo }
        if (index >= 0) {
            val item = estoque[index]
            item.total += quantidade
            // Update description or sector if not empty
            if (descricao.isNotBlank()) item.descricao = descricao
            if (setor.isNotBlank()) item.setor = setor
            estoque[index] = item
        } else {
            // Create new entry
            val newItem = InventoryItem(
                codigo = codigo,
                descricao = descricao,
                setor = setor,
                total = quantidade
            )
            estoque.add(newItem)
        }
        // Record movement
        if (quantidade != 0f) {
            historico.add(InventoryMovement(codigo, descricao, setor, quantidade))
        }
        persistData()
    }

    /**
     * Clear all inventory and its history. Products remain untouched.
     */
    fun clearInventory() {
        estoque.clear()
        historico.clear()
        persistData()
    }

    /**
     * Clear all products and their inventory/history. Use with caution.
     */
    fun clearProducts() {
        produtos.clear()
        estoque.clear()
        historico.clear()
        persistData()
    }
}