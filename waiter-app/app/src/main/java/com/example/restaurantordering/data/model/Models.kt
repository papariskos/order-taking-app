package com.example.restaurantordering.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String,
    val role: String,
    val token: String? = null
)

data class Category(
    val id: Int,
    val name: String
)

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("is_available") val isAvailable: Boolean,
    @SerializedName("category_name") val categoryName: String? = null
) {
    val available: Boolean get() = isAvailable
}

data class Order(
    val id: Int,
    @SerializedName("table_id") val tableId: String,
    val zone: String,
    val status: String, // "active", "paid", "cancelled"
    @SerializedName("waiter_id") val waiterId: Int,
    @SerializedName("waiter_name") val waiterName: String? = null,
    @SerializedName("total_price") val totalPrice: Double,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("closed_at") val closedAt: String? = null,
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val id: Int,
    @SerializedName("order_id") val orderId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String? = null,
    val quantity: Int,
    val price: Double,
    val notes: String? = null
)

data class CartItem(
    val product: Product,
    var quantity: Int,
    var notes: String = ""
)

// Request payloads
data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class OrderSubmitRequest(
    @SerializedName("table_id") val tableId: String,
    val zone: String,
    val notes: String?,
    val items: List<OrderItemSubmit>
)

data class OrderItemSubmit(
    @SerializedName("product_id") val productId: Int,
    val quantity: Int,
    @SerializedName("item_notes") val itemNotes: String?
)

data class OrderModifyRequest(
    val items: List<OrderItemSubmit>,
    val notes: String?
)

data class CancelRequest(
    val reason: String
)

data class BillRequest(
    @SerializedName("payment_method") val paymentMethod: String // "cash" or "card"
)
