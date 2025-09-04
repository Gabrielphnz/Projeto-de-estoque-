package com.seuapp.estoque

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model classes used by the app. A [Product] represents an item that can
 * be stocked. An [InventoryItem] represents the current quantity on hand for
 * a product. An [InventoryMovement] records a change to the inventory. All
 * three types are serialised to and from JSON via [Gson] for persistence.
 */
/**
 * Represents a product that can be stocked in the system.  The [setor]
 * (sector) identifies which logical group the product belongs to.  Both
 * [descricao] and [setor] are mutable so that products can be edited
 * after creation (for example when renaming a sector).
 */
data class Product(
    val codigo: String,
    var descricao: String,
    var setor: String
)

/**
 * Represents an item currently held in inventory.  It is uniquely
 * identified by [codigo].  The [descricao] and [setor] fields are
 * mutable so that updates can propagate when editing product or sector
 * information.  The [total] field tracks the current quantity on hand.
 */
data class InventoryItem(
    val codigo: String,
    var descricao: String,
    var setor: String,
    var total: Float
)

/**
 * Records a change to the inventory.  Each movement notes the product
 * [codigo], description and sector at the time of the update, the
 * [quantidade] delta applied (positive or negative) and the moment it
 * occurred.  Movements are immutable and provide a historical audit
 * trail.
 */
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

    /**
     * Mutable lists backing Compose state. When items are added or removed
     * Compose will update the UI accordingly.  In addition to products,
     * inventory and history we maintain a list of available sectors.  The
     * sectors list is persisted alongside the other collections so that
     * user‑defined sectors survive app restarts.
     */
    val produtos = mutableStateListOf<Product>()
    val estoque = mutableStateListOf<InventoryItem>()
    val historico = mutableStateListOf<InventoryMovement>()
    val setores = mutableStateListOf<String>()

    /**
     * Represents an application user.  Each user has a username and
     * password along with a set of permissions controlling access to various
     * app features.  The [isAdmin] flag grants full access including user
     * management.
     */
    data class User(
        val username: String,
        val password: String,
        var canEditProducts: Boolean = true,
        var canEditInventory: Boolean = true,
        var canViewReports: Boolean = true,
        var canManageUsers: Boolean = false,
        var isAdmin: Boolean = false
    )

    val users = mutableStateListOf<User>()
    var currentUser: User? = null

    /**
     * Holds products imported from CSV that did not specify a sector.  After
     * importing, the UI can prompt the user to assign these products to a
     * chosen sector.  This list is not persisted to disk and exists only
     * during the lifetime of the ViewModel.
     */
    val pendingImported = mutableStateListOf<Product>()

    init {
        loadData()
        // If no sectors were loaded, initialise with three default ones
        if (setores.isEmpty()) {
            setores.addAll(listOf("Açougue", "Hortifruti", "Outros"))
        }
        // Ensure at least one admin user exists
        if (users.isEmpty()) {
            users.add(User("admin", "admin", isAdmin = true, canManageUsers = true))
        }
    }

    /**
     * Load saved data from SharedPreferences.  Data is encoded in JSON
     * format using org.json arrays and objects.  Corrupt or missing
     * entries are ignored to avoid crashes.
     */
    private fun loadData() {
        produtos.clear()
        estoque.clear()
        historico.clear()
        setores.clear()
        prefs.getString("products", null)?.let { json ->
            try {
                produtos.addAll(decodeProducts(json))
            } catch (_: Exception) {
                // ignore corrupt product data
            }
        }
        prefs.getString("inventory", null)?.let { json ->
            try {
                estoque.addAll(decodeInventory(json))
            } catch (_: Exception) {
                // ignore corrupt inventory data
            }
        }
        prefs.getString("history", null)?.let { json ->
            try {
                historico.addAll(decodeHistory(json))
            } catch (_: Exception) {
                // ignore corrupt history data
            }
        }
        prefs.getString("sectors", null)?.let { json ->
            try {
                setores.addAll(decodeSectors(json))
            } catch (_: Exception) {
                // ignore corrupt sectors data
            }
        }
        // Load users list
        prefs.getString("users", null)?.let { json ->
            try {
                users.addAll(decodeUsers(json))
            } catch (_: Exception) {
                // ignore corrupt users data
            }
        }
    }

    /**
     * Persist current state to SharedPreferences by serialising each list
     * into JSON strings.  Using apply() writes asynchronously.
     */
    private fun persistData() {
        prefs.edit().apply {
            putString("products", encodeProducts(produtos))
            putString("inventory", encodeInventory(estoque))
            putString("history", encodeHistory(historico))
            putString("sectors", encodeSectors(setores))
            putString("users", encodeUsers(users))
            apply()
        }
    }

    /**
     * Encode and decode helper functions for each data type.  JSON is
     * constructed manually via org.json to avoid introducing an external
     * serialisation dependency like Gson.  See decode functions for the
     * inverse operations.
     */
    private fun encodeProducts(list: List<Product>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject()
            obj.put("codigo", p.codigo)
            obj.put("descricao", p.descricao)
            obj.put("setor", p.setor)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeProducts(json: String): List<Product> {
        val arr = JSONArray(json)
        val items = mutableListOf<Product>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val codigo = obj.optString("codigo", "")
            val descricao = obj.optString("descricao", "")
            val setor = obj.optString("setor", "")
            if (codigo.isNotBlank()) {
                items.add(Product(codigo, descricao, setor))
            }
        }
        return items
    }

    private fun encodeInventory(list: List<InventoryItem>): String {
        val arr = JSONArray()
        list.forEach { i ->
            val obj = JSONObject()
            obj.put("codigo", i.codigo)
            obj.put("descricao", i.descricao)
            obj.put("setor", i.setor)
            obj.put("total", i.total)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeInventory(json: String): List<InventoryItem> {
        val arr = JSONArray(json)
        val items = mutableListOf<InventoryItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val codigo = obj.optString("codigo", "")
            val descricao = obj.optString("descricao", "")
            val setor = obj.optString("setor", "")
            val total = obj.optDouble("total", 0.0).toFloat()
            if (codigo.isNotBlank()) {
                items.add(InventoryItem(codigo, descricao, setor, total))
            }
        }
        return items
    }

    private fun encodeHistory(list: List<InventoryMovement>): String {
        val arr = JSONArray()
        list.forEach { m ->
            val obj = JSONObject()
            obj.put("codigo", m.codigo)
            obj.put("descricao", m.descricao)
            obj.put("setor", m.setor)
            obj.put("quantidade", m.quantidade)
            obj.put("timestamp", m.timestamp)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeHistory(json: String): List<InventoryMovement> {
        val arr = JSONArray(json)
        val items = mutableListOf<InventoryMovement>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val codigo = obj.optString("codigo", "")
            val descricao = obj.optString("descricao", "")
            val setor = obj.optString("setor", "")
            val quantidade = obj.optDouble("quantidade", 0.0).toFloat()
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            if (codigo.isNotBlank()) {
                items.add(InventoryMovement(codigo, descricao, setor, quantidade, timestamp))
            }
        }
        return items
    }

    private fun encodeSectors(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { s -> arr.put(s) }
        return arr.toString()
    }

    private fun decodeSectors(json: String): List<String> {
        val arr = JSONArray(json)
        val items = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val name = arr.optString(i)
            if (name.isNotBlank()) items.add(name)
        }
        return items
    }

    // Encode users list into JSON
    private fun encodeUsers(list: List<User>): String {
        val arr = JSONArray()
        list.forEach { u ->
            val obj = JSONObject()
            obj.put("username", u.username)
            obj.put("password", u.password)
            obj.put("canEditProducts", u.canEditProducts)
            obj.put("canEditInventory", u.canEditInventory)
            obj.put("canViewReports", u.canViewReports)
            obj.put("canManageUsers", u.canManageUsers)
            obj.put("isAdmin", u.isAdmin)
            arr.put(obj)
        }
        return arr.toString()
    }

    // Decode users list from JSON
    private fun decodeUsers(json: String): List<User> {
        val arr = JSONArray(json)
        val items = mutableListOf<User>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val username = obj.optString("username", "")
            val password = obj.optString("password", "")
            val canEditProducts = obj.optBoolean("canEditProducts", true)
            val canEditInventory = obj.optBoolean("canEditInventory", true)
            val canViewReports = obj.optBoolean("canViewReports", true)
            val canManageUsers = obj.optBoolean("canManageUsers", false)
            val isAdmin = obj.optBoolean("isAdmin", false)
            if (username.isNotBlank()) {
                items.add(User(username, password, canEditProducts, canEditInventory, canViewReports, canManageUsers, isAdmin))
            }
        }
        return items
    }

    /**
     * Authenticate a user by username and password.  Returns true if
     * credentials match a stored user.  On success sets [currentUser] to
     * the authenticated user.  Fails silently if no match is found.
     */
    fun authenticate(username: String, password: String): Boolean {
        val user = users.firstOrNull { it.username.equals(username, ignoreCase = true) && it.password == password }
        return if (user != null) {
            currentUser = user
            true
        } else {
            false
        }
    }

    /** Add a new user.  Duplicate usernames are ignored. */
    fun addUser(user: User) {
        if (users.any { it.username.equals(user.username, ignoreCase = true) }) return
        users.add(user)
        persistData()
    }

    /** Update permissions for an existing user. */
    fun updateUserPermissions(username: String, canEditProducts: Boolean, canEditInventory: Boolean, canViewReports: Boolean, canManageUsers: Boolean) {
        val idx = users.indexOfFirst { it.username.equals(username, ignoreCase = true) }
        if (idx >= 0) {
            val u = users[idx]
            users[idx] = u.copy(
                canEditProducts = canEditProducts,
                canEditInventory = canEditInventory,
                canViewReports = canViewReports,
                canManageUsers = canManageUsers
            )
            persistData()
        }
    }

    /** Delete a user by username.  The admin user cannot be deleted. */
    fun deleteUser(username: String) {
        if (username.equals("admin", ignoreCase = true)) return
        users.removeAll { it.username.equals(username, ignoreCase = true) }
        persistData()
    }

    /**
     * Add a new product or update an existing one.  Products are uniquely
     * identified by their [Product.codigo].  When updating, only the
     * description and sector may change—new codes create new entries.  The
     * list of sectors is updated automatically if a product uses a new
     * sector name.  After mutation the data is persisted.
     */
    fun upsertProduct(product: Product) {
        val idx = produtos.indexOfFirst { it.codigo == product.codigo }
        if (idx >= 0) {
            produtos[idx] = product
        } else {
            produtos.add(product)
        }
        if (product.setor.isNotBlank() && setores.none { it.equals(product.setor, ignoreCase = true) }) {
            setores.add(product.setor)
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
        val idx = estoque.indexOfFirst { it.codigo == codigo }
        if (idx >= 0) {
            val item = estoque[idx]
            item.total += quantidade
            if (descricao.isNotBlank()) item.descricao = descricao
            if (setor.isNotBlank()) item.setor = setor
            estoque[idx] = item
        } else {
            val newItem = InventoryItem(codigo, descricao, setor, quantidade)
            estoque.add(newItem)
        }
        // update sectors list if needed
        if (setor.isNotBlank() && setores.none { it.equals(setor, ignoreCase = true) }) {
            setores.add(setor)
        }
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

    /**
     * Import a set of products from a CSV string.  Each line should
     * contain at least three fields separated by semicolons or commas:
     * código;descrição;setor.  Lines beginning with non‑numeric codes or
     * those missing required fields are skipped.  New sectors are added
     * automatically as necessary.  After import the data is persisted.
     */
    fun importProductsFromCsv(csvContent: String) {
        // Clear any previous pending imports
        pendingImported.clear()
        /*
         * Import products from a CSV string. Lines may be separated by either
         * semicolons or commas. We trim whitespace on each line and ignore
         * empty lines. A header row (where the first column is "codigo" or
         * contains alphabetic characters) is skipped. Valid lines must
         * provide at least three fields: codigo, descricao and setor. After
         * import, new sectors are added automatically via upsertProduct and
         * all data is persisted.
         */
        val lines = csvContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        lines.forEachIndexed { index, line ->
            val parts = line.split(';', ',')
            if (parts.size >= 2) {
                // Trim whitespace and surrounding quotes from each field
                val rawCodigo = parts[0].trim()
                val rawDescricao = parts[1].trim()
                val rawSetor: String? = if (parts.size >= 3) parts[2].trim() else null
                // Remove double quotes around fields if present
                val codigo = rawCodigo.trim('"')
                val descricao = rawDescricao.trim('"')
                val setor: String = rawSetor?.trim('"') ?: ""
                // skip potential header rows on the first line (codigo or non‑numeric code)
                if (index == 0 && codigo.equals("codigo", ignoreCase = true)) return@forEachIndexed
                if (codigo.isNotBlank() && descricao.isNotBlank()) {
                    if (setor.isNotBlank()) {
                        upsertProduct(Product(codigo, descricao, setor))
                    } else {
                        // Sector missing: add to list for later assignment
                        val product = Product(codigo, descricao, "")
                        upsertProduct(product)
                        pendingImported.add(product)
                    }
                }
            }
        }
        persistData()
    }

    /**
     * Assign a single sector to all products that were imported without one.  If
     * the sector name is blank nothing happens.  Newly assigned products will
     * be updated in the products list and persisted.  After assignment the
     * pending list is cleared.
     */
    fun assignSectorToPending(sector: String) {
        val cleaned = sector.trim()
        if (cleaned.isBlank()) return
        pendingImported.forEach { product ->
            // update sector of product in the products list
            val idx = produtos.indexOfFirst { it.codigo == product.codigo }
            if (idx >= 0) {
                produtos[idx].setor = cleaned
            }
        }
        // Ensure the sector exists in the sectors list
        if (setores.none { it.equals(cleaned, ignoreCase = true) }) {
            setores.add(cleaned)
        }
        pendingImported.clear()
        persistData()
    }

    /**
     * Assign a new sector to a list of products identified by their codes.  If
     * the sector name is blank nothing happens.  Products whose codes do
     * not exist in the current list are ignored.  If the sector does not
     * exist in the sectors list, it will be added.  All changes are
     * persisted.
     */
    fun assignSectorToProducts(codes: List<String>, sector: String) {
        val cleaned = sector.trim()
        if (cleaned.isBlank()) return
        codes.forEach { code ->
            val idx = produtos.indexOfFirst { it.codigo == code }
            if (idx >= 0) {
                produtos[idx].setor = cleaned
            }
        }
        if (setores.none { it.equals(cleaned, ignoreCase = true) }) {
            setores.add(cleaned)
        }
        persistData()
    }

    /**
     * Generate a CSV string representing the current inventory for a
     * specific sector.  The header row is included.  Quantities are
     * formatted with one decimal place.
     */
    fun generateInventoryCsv(setor: String): String {
        val sb = StringBuilder()
        sb.append("codigo;descricao;setor;total\n")
        estoque.filter { it.setor == setor }.forEach { item ->
            sb.append(item.codigo).append(';')
            sb.append(item.descricao).append(';')
            sb.append(item.setor).append(';')
            sb.append(String.format("%.1f", item.total)).append('\n')
        }
        return sb.toString()
    }

    /**
     * Generate a CSV string representing the list of products for a
     * specific sector.  The header row is included.
     */
    fun generateProductsCsv(setor: String): String {
        val sb = StringBuilder()
        sb.append("codigo;descricao;setor\n")
        produtos.filter { it.setor == setor }.forEach { item ->
            sb.append(item.codigo).append(';')
            sb.append(item.descricao).append(';')
            sb.append(item.setor).append('\n')
        }
        return sb.toString()
    }

    /**
     * Add a new sector to the list.  Empty names are ignored.  If a
     * sector with the same name (case insensitive) already exists it
     * will not be added again.  After modification the data is persisted.
     */
    fun addSector(name: String) {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return
        if (setores.any { it.equals(cleaned, ignoreCase = true) }) return
        setores.add(cleaned)
        persistData()
    }

    /**
     * Rename an existing sector.  All products, inventory items and
     * historical movements referencing the old name will be updated to the
     * new name.  If the old name is not found or the new name is blank,
     * nothing happens.
     */
    fun editSector(oldName: String, newName: String) {
        val cleanedNew = newName.trim()
        if (cleanedNew.isBlank()) return
        val index = setores.indexOfFirst { it == oldName }
        if (index >= 0) {
            setores[index] = cleanedNew
            // update products
            produtos.forEachIndexed { i, p ->
                if (p.setor == oldName) {
                    produtos[i] = p.copy(setor = cleanedNew)
                }
            }
            // update inventory
            estoque.forEachIndexed { i, item ->
                if (item.setor == oldName) {
                    estoque[i] = item.copy(setor = cleanedNew)
                }
            }
            // update history
            historico.forEachIndexed { i, m ->
                if (m.setor == oldName) {
                    historico[i] = m.copy(setor = cleanedNew)
                }
            }
            persistData()
        }
    }

    /**
     * Remove a sector from the list.  All products, inventory items and
     * historical movements associated with the removed sector are also
     * deleted.  If the sector name is not found, nothing happens.
     */
    fun deleteSector(name: String) {
        val removed = setores.remove(name)
        if (removed) {
            produtos.removeAll { it.setor == name }
            estoque.removeAll { it.setor == name }
            historico.removeAll { it.setor == name }
            persistData()
        }
    }
}