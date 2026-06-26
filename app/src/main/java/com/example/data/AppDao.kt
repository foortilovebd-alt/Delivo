package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Merchant Operations ---
    @Query("SELECT * FROM merchants")
    fun getAllMerchantsFlow(): Flow<List<MerchantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchants(merchants: List<MerchantEntity>)

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getMerchantById(id: String): MerchantEntity?


    // --- Product Operations ---
    @Query("SELECT * FROM products WHERE merchantId = :merchantId")
    fun getProductsForMerchantFlow(merchantId: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET isAvailable = :isAvailable WHERE id = :productId")
    suspend fun updateProductAvailability(productId: String, isAvailable: Boolean)


    // --- Order Operations ---
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET rating = :rating, ratingComment = :comment WHERE id = :orderId")
    suspend fun rateOrder(orderId: String, rating: Int, comment: String?)


    // --- Rider Operations ---
    @Query("SELECT * FROM rider_profile WHERE id = :riderId")
    fun getRiderProfileFlow(riderId: String): Flow<RiderProfileEntity?>

    @Query("SELECT * FROM rider_profile WHERE id = :riderId")
    suspend fun getRiderProfile(riderId: String): RiderProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiderProfile(profile: RiderProfileEntity)

    @Query("UPDATE rider_profile SET isOnline = :isOnline WHERE id = :riderId")
    suspend fun updateRiderOnlineStatus(riderId: String, isOnline: Boolean)

    @Query("UPDATE rider_profile SET activeOrderId = :activeOrderId WHERE id = :riderId")
    suspend fun updateRiderActiveOrder(riderId: String, activeOrderId: String?)

    @Query("UPDATE rider_profile SET currentLatitude = :lat, currentLongitude = :lng WHERE id = :riderId")
    suspend fun updateRiderLocation(riderId: String, lat: Double, lng: Double)

    @Query("UPDATE rider_profile SET totalEarnings = totalEarnings + :amount WHERE id = :riderId")
    suspend fun addRiderEarnings(riderId: String, amount: Double)

    // --- User/Auth Operations ---
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}
