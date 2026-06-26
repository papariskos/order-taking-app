package com.example.restaurantordering.data.network

import com.example.restaurantordering.data.model.*
import retrofit2.http.*

interface ApiService {
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/categories")
    suspend fun getCategories(
        @Header("Authorization") token: String
    ): List<Category>

    @GET("api/products")
    suspend fun getProducts(
        @Header("Authorization") token: String
    ): List<Product>

    @POST("api/orders")
    suspend fun submitOrder(
        @Header("Authorization") token: String,
        @Body request: OrderSubmitRequest
    ): Order

    @PUT("api/orders/{id}")
    suspend fun modifyOrder(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: OrderModifyRequest
    ): Order

    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CancelRequest
    ): Order

    @POST("api/orders/{id}/bill")
    suspend fun issueBill(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: BillRequest
    ): Order

    @GET("api/orders/active")
    suspend fun getActiveOrders(
        @Header("Authorization") token: String
    ): List<Order>

    @GET("api/orders/history")
    suspend fun getOrderHistory(
        @Header("Authorization") token: String
    ): List<Order>
}
