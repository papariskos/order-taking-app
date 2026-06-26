package com.example.restaurantordering.data.repository

import com.example.restaurantordering.data.database.*
import com.example.restaurantordering.data.model.*
import com.example.restaurantordering.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// --- Repository Interfaces ---

interface UserRepository {
    suspend fun login(username: String, password: String): User
    fun setToken(token: String?)
    fun getToken(): String?
    fun getCurrentUser(): User?
    fun logout()
}

interface ProductRepository {
    fun getCategories(): Flow<List<Category>>
    fun getProducts(): Flow<List<Product>>
    suspend fun refreshCatalog()
}

interface OrderRepository {
    fun getActiveOrders(): Flow<List<Order>>
    suspend fun refreshActiveOrders()
    suspend fun submitOrder(tableId: String, zone: String, notes: String?, items: List<CartItem>): Order
    suspend fun modifyOrder(orderId: Int, notes: String?, items: List<CartItem>): Order
    suspend fun cancelOrder(orderId: Int, reason: String): Order
    suspend fun issueBill(orderId: Int, paymentMethod: String): Order
}

// --- Repository Implementations ---

class UserRepositoryImpl(private val apiService: ApiService) : UserRepository {
    private var token: String? = null
    private var currentUser: User? = null

    override suspend fun login(username: String, password: String): User {
        val response = apiService.login(LoginRequest(username, password))
        token = response.token
        currentUser = response.user.copy(token = response.token)
        return currentUser!!
    }

    override fun setToken(token: String?) {
        this.token = token
    }

    override fun getToken(): String? = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }

    override fun getCurrentUser(): User? = currentUser

    override fun logout() {
        token = null
        currentUser = null
    }
}

class ProductRepositoryImpl(
    private val apiService: ApiService,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val userRepo: UserRepository
) : ProductRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { list ->
            list.map { Category(it.id, it.name) }
        }
    }

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { list ->
            list.map { Product(it.id, it.name, it.price, it.categoryId, it.isAvailable, it.categoryName) }
        }
    }

    override suspend fun refreshCatalog() {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        
        val apiCategories = apiService.getCategories(authToken)
        categoryDao.clearAll()
        categoryDao.insertCategories(apiCategories.map { CachedCategory(it.id, it.name) })

        val apiProducts = apiService.getProducts(authToken)
        productDao.clearAll()
        productDao.insertProducts(apiProducts.map { 
            CachedProduct(it.id, it.name, it.price, it.categoryId, it.isAvailable, it.categoryName)
        })
    }
}

class OrderRepositoryImpl(
    private val apiService: ApiService,
    private val orderDao: OrderDao,
    private val userRepo: UserRepository
) : OrderRepository {

    override fun getActiveOrders(): Flow<List<Order>> {
        return orderDao.getActiveOrdersFlow().map { list ->
            list.map { 
                Order(
                    id = it.id,
                    tableId = it.tableId,
                    zone = it.zone,
                    status = it.status,
                    waiterId = it.waiterId,
                    waiterName = it.waiterName,
                    totalPrice = it.totalPrice,
                    paymentMethod = it.paymentMethod,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }
        }
    }

    override suspend fun refreshActiveOrders() {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        val apiOrders = apiService.getActiveOrders(authToken)
        
        orderDao.clearAll()
        orderDao.insertOrders(apiOrders.map { 
            CachedOrder(
                id = it.id,
                tableId = it.tableId,
                zone = it.zone,
                status = it.status,
                waiterId = it.waiterId,
                waiterName = it.waiterName,
                totalPrice = it.totalPrice,
                paymentMethod = it.paymentMethod,
                notes = it.notes,
                createdAt = it.createdAt
            )
        })
    }

    override suspend fun submitOrder(
        tableId: String,
        zone: String,
        notes: String?,
        items: List<CartItem>
    ): Order {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        val submitItems = items.map { OrderItemSubmit(it.product.id, it.quantity, it.notes) }
        val response = apiService.submitOrder(authToken, OrderSubmitRequest(tableId, zone, notes, submitItems))
        
        refreshActiveOrders() // Sync cache
        return response
    }

    override suspend fun modifyOrder(
        orderId: Int,
        notes: String?,
        items: List<CartItem>
    ): Order {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        val submitItems = items.map { OrderItemSubmit(it.product.id, it.quantity, it.notes) }
        val response = apiService.modifyOrder(authToken, orderId, OrderModifyRequest(submitItems, notes))
        
        refreshActiveOrders()
        return response
    }

    override suspend fun cancelOrder(orderId: Int, reason: String): Order {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.cancelOrder(authToken, orderId, CancelRequest(reason))
        
        orderDao.deleteOrder(orderId) // Remove from cache
        return response
    }

    override suspend fun issueBill(orderId: Int, paymentMethod: String): Order {
        val authToken = userRepo.getToken() ?: throw Exception("Not authenticated")
        val response = apiService.issueBill(authToken, orderId, BillRequest(paymentMethod))
        
        orderDao.deleteOrder(orderId)
        return response
    }
}
