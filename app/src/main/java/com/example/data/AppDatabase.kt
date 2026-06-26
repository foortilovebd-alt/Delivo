package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MerchantEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        RiderProfileEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "delivo_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.appDao())
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            // --- Seed Merchants ---
            val merchants = listOf(
                MerchantEntity(
                    id = "m1",
                    name = "Gourmet Burger Kitchen",
                    category = "Food",
                    rating = 4.8,
                    deliveryTimeMinutes = 20,
                    deliveryFee = 2.99,
                    promoText = "Free fries on orders over $15"
                ),
                MerchantEntity(
                    id = "m2",
                    name = "FreshMart Grocers",
                    category = "Grocery",
                    rating = 4.6,
                    deliveryTimeMinutes = 15,
                    deliveryFee = 1.49,
                    promoText = "Fresh organic products guaranteed"
                ),
                MerchantEntity(
                    id = "m3",
                    name = "MediLife Pharmacy",
                    category = "Pharmacy",
                    rating = 4.9,
                    deliveryTimeMinutes = 25,
                    deliveryFee = 3.49,
                    promoText = "Use code HEALTH10 for 10% off"
                ),
                MerchantEntity(
                    id = "m4",
                    name = "Pizza Bella",
                    category = "Food",
                    rating = 4.7,
                    deliveryTimeMinutes = 30,
                    deliveryFee = 2.49,
                    promoText = "Buy 1 Get 1 Medium Pepperoni on Tuesdays"
                )
            )
            dao.insertMerchants(merchants)

            // --- Seed Products ---
            val products = listOf(
                // Gourmet Burger Kitchen (m1)
                ProductEntity("p1", "m1", "Truffle Bacon Cheeseburger", "Smoked bacon, truffle aioli, aged cheddar, brioche bun", 14.99, "Burgers"),
                ProductEntity("p2", "m1", "Spicy Crispy Chicken Burger", "Crispy buttermilk chicken, jalapeno slaw, sriracha mayo", 12.99, "Burgers"),
                ProductEntity("p3", "m1", "Parmesan Truffle Fries", "Thin-cut rustic fries tossed in truffle oil and freshly grated parmesan", 5.99, "Sides"),
                ProductEntity("p4", "m1", "Craft Avocado Shake", "Creamy blended avocado with almond milk and premium honey", 4.99, "Drinks"),

                // FreshMart Grocers (m2)
                ProductEntity("p5", "m2", "Organic Avocado Pack (4pcs)", "Ripe organic Haas avocados, perfect for salads and guacamole", 6.49, "Fresh Produce"),
                ProductEntity("p6", "m2", "Whole Almond Milk 1L", "Unsweetened, rich dairy-free whole almond milk", 3.99, "Dairy & Alternatives"),
                ProductEntity("p7", "m2", "Artisan Sourdough Bread", "Freshly baked crusty rustic sourdough loaf", 4.50, "Bakery"),
                ProductEntity("p8", "m2", "Premium Strawberries 500g", "Sweet and juicy fresh organic strawberries", 5.25, "Fresh Produce"),

                // MediLife Pharmacy (m3)
                ProductEntity("p9", "m3", "Vitamin C 1000mg (90 Tabs)", "High-strength immune support chewable daily tablets", 11.99, "Supplements"),
                ProductEntity("p10", "m3", "Organic Herbal Sleep Tea", "Chamomile, lavender and valerian root blend for restful sleep", 6.49, "Wellness"),
                ProductEntity("p11", "m3", "Premium Hydrating Face Cream", "Rich hyaluronic acid moisturizing daily facial cream", 18.50, "Skincare"),

                // Pizza Bella (m4)
                ProductEntity("p12", "m4", "Quattro Formaggi Pizza", "Mozzarella, Gorgonzola, Parmesan, and Fontina cheese with fresh basil", 16.99, "Pizza"),
                ProductEntity("p13", "m4", "Fiery Pepperoni & Hot Honey", "Spicy Italian pepperoni, dynamic hot honey glaze, fresh chili", 17.49, "Pizza"),
                ProductEntity("p14", "m4", "Garlic Butter Dough Balls", "Oven-fresh soft bread bites drenched in garlic herb butter", 6.50, "Sides")
            )
            dao.insertProducts(products)

            // --- Seed Rider Profile ---
            val defaultRider = RiderProfileEntity(
                id = "rider1",
                name = "Alex Rider",
                isOnline = true,
                currentLatitude = 0.25,
                currentLongitude = 0.25,
                totalEarnings = 0.0,
                activeOrderId = null
            )
            dao.insertRiderProfile(defaultRider)
        }
    }
}
