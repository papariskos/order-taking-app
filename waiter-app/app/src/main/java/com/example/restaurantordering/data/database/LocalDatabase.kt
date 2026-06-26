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

@Entity(
    tableName = "cached_order_items",
    foreignKeys = [
        ForeignKey(
            entity = CachedOrder::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class CachedOrderItem(
    @PrimaryKey val id: Int,
    val orderId: Int,
    val productId: Int,
    val productName: String?,
    val quantity: Int,
    val price: Double,
    val notes: String?
)

data class CachedOrderWithItems(
    @Embedded val order: CachedOrder,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<CachedOrderItem>
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
    @Transaction
    @Query("SELECT * FROM cached_orders WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveOrdersWithItemsFlow(): Flow<List<CachedOrderWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<CachedOrder>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<CachedOrderItem>)

    @Query("DELETE FROM cached_order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: Int)

    @Transaction
    suspend fun insertOrdersWithItems(orders: List<CachedOrder>, items: List<CachedOrderItem>) {
        insertOrders(orders)
        insertOrderItems(items)
    }

    @Query("DELETE FROM cached_orders WHERE id = :id")
    suspend fun deleteOrder(id: Int)

    @Query("DELETE FROM cached_order_items")
    suspend fun clearAllOrderItems()

    @Query("DELETE FROM cached_orders")
    suspend fun clearAllOrders()

    @Transaction
    suspend fun clearAll() {
        clearAllOrderItems()
        clearAllOrders()
    }
}

// --- Database Class ---

@Database(
    entities = [CachedCategory::class, CachedProduct::class, CachedOrder::class, CachedOrderItem::class],
    version = 2,
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
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
