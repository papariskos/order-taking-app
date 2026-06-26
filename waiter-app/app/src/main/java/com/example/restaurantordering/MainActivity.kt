package com.example.restaurantordering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.restaurantordering.data.database.AppDatabase
import com.example.restaurantordering.data.network.ApiService
import com.example.restaurantordering.data.network.WebSocketService
import com.example.restaurantordering.data.repository.OrderRepositoryImpl
import com.example.restaurantordering.data.repository.ProductRepositoryImpl
import com.example.restaurantordering.data.repository.UserRepositoryImpl
import com.example.restaurantordering.ui.screens.*
import com.example.restaurantordering.ui.viewmodels.AuthViewModel
import com.example.restaurantordering.ui.viewmodels.OrderViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import com.example.restaurantordering.data.network.DynamicHostInterceptor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Database
        val database = AppDatabase.getDatabase(applicationContext)
        val categoryDao = database.categoryDao()
        val productDao = database.productDao()
        val orderDao = database.orderDao()

        // Setup Dynamic Host Interceptor & OkHttpClient
        val dynamicHostInterceptor = DynamicHostInterceptor()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(dynamicHostInterceptor)
            .build()

        // Setup Retrofit Client using okHttpClient
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        // Setup Services
        val wsService = WebSocketService()

        // Setup Repositories
        val userRepo = UserRepositoryImpl(apiService, dynamicHostInterceptor)
        val productRepo = ProductRepositoryImpl(apiService, categoryDao, productDao, userRepo)
        val orderRepo = OrderRepositoryImpl(apiService, orderDao, userRepo)

        // Setup ViewModels
        val authViewModel = AuthViewModel(userRepo, productRepo, orderRepo, wsService)
        val orderViewModel = OrderViewModel(productRepo, orderRepo, wsService)

        setContent {
            Surface(color = PrimaryDark) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("area_selection") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("area_selection") {
                        AreaSelectionScreen(
                            orderViewModel = orderViewModel,
                            authViewModel = authViewModel,
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo("area_selection") { inclusive = true }
                                }
                            },
                            onTableSelected = {
                                navController.navigate("ordering")
                            },
                            onEditOrder = { order ->
                                orderViewModel.loadOrderItemsForEdit(order)
                                navController.navigate("ordering")
                            }
                        )
                    }

                    composable("ordering") {
                        OrderScreen(
                            orderViewModel = orderViewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onGoToCart = {
                                navController.navigate("cart")
                            }
                        )
                    }

                    composable("cart") {
                        CartScreen(
                            orderViewModel = orderViewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onCheckout = {
                                navController.navigate("checkout")
                            },
                            onSubmitSuccess = {
                                navController.navigate("area_selection") {
                                    popUpTo("area_selection") { inclusive = false }
                                }
                            }
                        )
                    }

                    composable("checkout") {
                        BillScreen(
                            orderViewModel = orderViewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onSuccess = {
                                navController.navigate("area_selection") {
                                    popUpTo("area_selection") { inclusive = false }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
