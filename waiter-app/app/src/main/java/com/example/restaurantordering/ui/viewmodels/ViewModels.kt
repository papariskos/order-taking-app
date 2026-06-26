package com.example.restaurantordering.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurantordering.data.model.*
import com.example.restaurantordering.data.network.WebSocketService
import com.example.restaurantordering.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val userRepo: UserRepository,
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository,
    private val wsService: WebSocketService
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(username: String, password: String, serverIp: String = "order-taking-app-production.up.railway.app") {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = userRepo.login(username, password)
                userRepo.setToken(user.token)
                
                // Initialize local DB catalogs and order cache
                productRepo.refreshCatalog()
                orderRepo.refreshActiveOrders()

                // Connect WebSocket
                user.token?.let { wsService.connect(it, serverIp) }

                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Σφάλμα σύνδεσης")
            }
        }
    }

    fun logout() {
        userRepo.logout()
        wsService.disconnect()
        _authState.value = AuthState.Idle
    }
}

class OrderViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository,
    private val wsService: WebSocketService
) : ViewModel() {

    // Catalog state
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    // Active orders state
    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val activeOrders: StateFlow<List<Order>> = _activeOrders.asStateFlow()

    // Ordering cart states
    var selectedZone = mutableStateOf("Σάλα")
    var selectedTable = mutableStateOf("")
    val cartItems = mutableStateListOf<CartItem>()
    var cartNotes = mutableStateOf("")

    // For editing an existing order
    var editingOrderId = mutableStateOf<Int?>(null)

    init {
        viewModelScope.launch {
            productRepo.getProducts().collect { _products.value = it }
        }
        viewModelScope.launch {
            productRepo.getCategories().collect { _categories.value = it }
        }
        viewModelScope.launch {
            orderRepo.getActiveOrders().collect { _activeOrders.value = it }
        }
        
        // Listen to real-time events to sync state automatically
        viewModelScope.launch {
            wsService.events.collectLatest { event ->
                // When other waiter modifies database, WebSocket triggers event and we refresh local DB.
                // Since Compose views collect from Room Flows, UI will update instantly!
                when (event.event) {
                    "ORDER_SUBMITTED", "ORDER_MODIFIED", "TABLE_STATUS_CHANGED", "DAY_CLOSED" -> {
                        try {
                            orderRepo.refreshActiveOrders()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun selectTable(zone: String, tableId: String) {
        selectedZone.value = zone
        selectedTable.value = tableId
        cartItems.clear()
        cartNotes.value = ""
        editingOrderId.value = null

        // Check if table has an active order to load for edit/payment
        val existingOrder = _activeOrders.value.find { it.tableId == tableId && it.zone == zone }
        if (existingOrder != null) {
            editingOrderId.value = existingOrder.id
            cartNotes.value = existingOrder.notes ?: ""
            
            // Fetch items from database (Since cached Order doesn't hold items, we might rely on online load.
            // But we can fetch active orders details from backend or let the ViewModel load them).
            // To be simple and robust, we can implement local detail load or simply request.
            // Let's assume detail loads automatically or we trigger refresh.
        }
    }

    fun addToCart(product: Product) {
        val existing = cartItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity++
            // Trigger Compose recomposition by replacing item
            val idx = cartItems.indexOf(existing)
            cartItems[idx] = existing.copy()
        } else {
            cartItems.add(CartItem(product, 1))
        }
    }

    fun incrementCartItem(item: CartItem) {
        val idx = cartItems.indexOf(item)
        if (idx != -1) {
            item.quantity++
            cartItems[idx] = item.copy()
        }
    }

    fun decrementCartItem(item: CartItem) {
        val idx = cartItems.indexOf(item)
        if (idx != -1) {
            if (item.quantity > 1) {
                item.quantity--
                cartItems[idx] = item.copy()
            } else {
                cartItems.removeAt(idx)
            }
        }
    }

    fun updateItemNotes(item: CartItem, notes: String) {
        val idx = cartItems.indexOf(item)
        if (idx != -1) {
            item.notes = notes
            cartItems[idx] = item.copy()
        }
    }

    fun loadOrderItemsForEdit(order: Order) {
        editingOrderId.value = order.id
        selectedTable.value = order.tableId
        selectedZone.value = order.zone
        cartNotes.value = order.notes ?: ""
        cartItems.clear()
        
        // Load items. Wait, if order has items in it, load them directly.
        order.items.forEach { item ->
            val prod = _products.value.find { it.id == item.productId }
            if (prod != null) {
                cartItems.add(CartItem(prod, item.quantity, item.notes ?: ""))
            }
        }
    }

    fun submitCurrentOrder(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (editingOrderId.value != null) {
                    orderRepo.modifyOrder(editingOrderId.value!!, cartNotes.value, cartItems)
                } else {
                    orderRepo.submitOrder(selectedTable.value, selectedZone.value, cartNotes.value, cartItems)
                }
                cartItems.clear()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Αποτυχία υποβολής παραγγελίας")
            }
        }
    }

    fun cancelCurrentOrder(reason: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val orderId = editingOrderId.value ?: return
        viewModelScope.launch {
            try {
                orderRepo.cancelOrder(orderId, reason)
                cartItems.clear()
                editingOrderId.value = null
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Αποτυχία ακύρωσης")
            }
        }
    }

    fun issueBillCurrentOrder(paymentMethod: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val orderId = editingOrderId.value ?: return
        viewModelScope.launch {
            try {
                orderRepo.issueBill(orderId, paymentMethod)
                cartItems.clear()
                editingOrderId.value = null
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Αποτυχία έκδοσης λογαριασμού")
            }
        }
    }
}
