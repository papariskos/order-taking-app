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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Database
        val database = AppDatabase.getDatabase(applicationContext)
        val categoryDao = database.categoryDao()
        val productDao = database.productDao()
        val orderDao = database.orderDao()

        // Setup Retrofit Client
        // Note: 10.0.2.2 is the special IP in the Android emulator to access the host's localhost (3000)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)

        // Setup Services
        val wsService = WebSocketService()

        // Setup Repositories
        val userRepo = UserRepositoryImpl(apiService)
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
                            }
                        )
                    }

                    composable("ordering") {
                        OrderScreen(
                            orderViewModel = orderViewModel,
                            onBack = {
                                navController.popBackStack()
                            },
                            onCheckout = {
                                navController.navigate("checkout")
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
