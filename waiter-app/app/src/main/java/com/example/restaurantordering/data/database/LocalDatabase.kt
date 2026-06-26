package com.example.restaurantordering.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "cached_categories")
data class CachedCategory(
    @PrimaryKey val id: Int,
    val name: String
)

@Entity(tableName = "cached_products")
data class CachedProduct(
    @PrimaryKey val id: Int,
    val name: String,
    val price: Double,
    val categoryId: Int,
    val isAvailable: Boolean,
    val categoryName: String?
)

@Entity(tableName = "cached_orders")
data class CachedOrder(
    @PrimaryKey val id: Int,
    val tableId: String,
    val zone: String,
    val status: String,
    val waiterId: Int,
    val waiterName: String?,
    val totalPrice: Double,
    val paymentMethod: String?,
    val notes: String?,
    val createdAt: String
)

// --- Room DAOs ---

@Dao
interface ProductDao {
    @Query("SELECT * FROM cached_products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<CachedProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<CachedProduct>)

    @Query("DELETE FROM cached_products")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM cached_categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CachedCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CachedCategory>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearAll()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM cached_orders WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveOrdersFlow(): Flow<List<CachedOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<CachedOrder>)

    @Query("DELETE FROM cached_orders WHERE id = :id")
    suspend fun deleteOrder(id: Int)

    @Query("DELETE FROM cached_orders")
    suspend fun clearAll()
}

// --- Database Class ---

@Database(
    entities = [CachedCategory::class, CachedProduct::class, CachedOrder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
