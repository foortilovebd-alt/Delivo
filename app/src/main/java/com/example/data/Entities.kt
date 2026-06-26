package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Food", "Grocery", "Pharmacy", "Pets"
    val rating: Double,
    val deliveryTimeMinutes: Int,
    val deliveryFee: Double,
    val promoText: String,
    val isClosed: Boolean = false
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val isAvailable: Boolean = true
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val merchantName: String,
    val customerAddress: String,
    val status: String, // "PLACED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED"
    val totalPrice: Double,
    val itemsSummary: String, // e.g. "2x Cheeseburger, 1x Fries"
    val paymentMethod: String, // "Card", "Wallet", "Cash"
    val promoApplied: String?,
    val riderId: String?,
    val riderName: String?,
    val riderLatitude: Double, // Simulated GPS latitude (0.0 to 1.0)
    val riderLongitude: Double, // Simulated GPS longitude (0.0 to 1.0)
    val rating: Int = 0, // 1 to 5, 0 means unrated
    val ratingComment: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "rider_profile")
data class RiderProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isOnline: Boolean,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val totalEarnings: Double,
    val activeOrderId: String?
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // email, phone, or social-id
    val name: String,
    val email: String?,
    val phone: String?,
    val passwordHash: String?, // Secure SHA-256 hash of password
    val loginType: String, // "EMAIL", "PHONE", "GOOGLE", "FACEBOOK"
    val avatarUrl: String? = null,
    val registeredAt: Long = System.currentTimeMillis()
)

object PasswordHasher {
    fun hash(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            // Fallback (should not occur since SHA-256 is guaranteed in Android/Java)
            password
        }
    }
}

