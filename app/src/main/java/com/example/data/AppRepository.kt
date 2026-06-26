package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val dao: AppDao) {

    // --- Merchants ---
    fun getAllMerchantsFlow(): Flow<List<MerchantEntity>> = dao.getAllMerchantsFlow()

    suspend fun getMerchantById(id: String): MerchantEntity? = dao.getMerchantById(id)

    suspend fun insertMerchants(merchants: List<MerchantEntity>) = dao.insertMerchants(merchants)


    // --- Products ---
    fun getProductsForMerchantFlow(merchantId: String): Flow<List<ProductEntity>> = 
        dao.getProductsForMerchantFlow(merchantId)

    suspend fun updateProductAvailability(productId: String, isAvailable: Boolean) =
        dao.updateProductAvailability(productId, isAvailable)

    suspend fun insertProducts(products: List<ProductEntity>) = dao.insertProducts(products)


    // --- Orders ---
    fun getAllOrdersFlow(): Flow<List<OrderEntity>> = dao.getAllOrdersFlow()

    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?> = dao.getOrderByIdFlow(orderId)

    suspend fun getOrderById(orderId: String): OrderEntity? = dao.getOrderById(orderId)

    suspend fun insertOrder(order: OrderEntity) = dao.insertOrder(order)

    suspend fun updateOrder(order: OrderEntity) = dao.updateOrder(order)

    suspend fun updateOrderStatus(orderId: String, status: String) = dao.updateOrderStatus(orderId, status)

    suspend fun rateOrder(orderId: String, rating: Int, comment: String?) = dao.rateOrder(orderId, rating, comment)


    // --- Rider ---
    fun getRiderProfileFlow(riderId: String = "rider1"): Flow<RiderProfileEntity?> = 
        dao.getRiderProfileFlow(riderId)

    suspend fun getRiderProfile(riderId: String = "rider1"): RiderProfileEntity? = 
        dao.getRiderProfile(riderId)

    suspend fun updateRiderOnlineStatus(isOnline: Boolean, riderId: String = "rider1") =
        dao.updateRiderOnlineStatus(riderId, isOnline)

    suspend fun updateRiderActiveOrder(activeOrderId: String?, riderId: String = "rider1") =
        dao.updateRiderActiveOrder(riderId, activeOrderId)

    suspend fun updateRiderLocation(lat: Double, lng: Double, riderId: String = "rider1") =
        dao.updateRiderLocation(riderId, lat, lng)

    suspend fun addRiderEarnings(amount: Double, riderId: String = "rider1") =
        dao.addRiderEarnings(riderId, amount)

    suspend fun insertRiderProfile(profile: RiderProfileEntity) = dao.insertRiderProfile(profile)

    // --- User Auth ---
    suspend fun getUserById(id: String): UserEntity? = dao.getUserById(id)
    suspend fun getUserByEmail(email: String): UserEntity? = dao.getUserByEmail(email)
    suspend fun getUserByPhone(phone: String): UserEntity? = dao.getUserByPhone(phone)
    suspend fun insertUser(user: UserEntity) = dao.insertUser(user)

    // --- Manual Seeder Check ---
    suspend fun ensureSeeded() {
        val merchants = dao.getAllMerchantsFlow().firstOrNull()
        if (merchants.isNullOrEmpty()) {
            val defaultMerchants = listOf(
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
            dao.insertMerchants(defaultMerchants)

            val defaultProducts = listOf(
                ProductEntity("p1", "m1", "Truffle Bacon Cheeseburger", "Smoked bacon, truffle aioli, aged cheddar, brioche bun", 14.99, "Burgers"),
                ProductEntity("p2", "m1", "Spicy Crispy Chicken Burger", "Crispy buttermilk chicken, jalapeno slaw, sriracha mayo", 12.99, "Burgers"),
                ProductEntity("p3", "m1", "Parmesan Truffle Fries", "Thin-cut rustic fries tossed in truffle oil and freshly grated parmesan", 5.99, "Sides"),
                ProductEntity("p4", "m1", "Craft Avocado Shake", "Creamy blended avocado with almond milk and premium honey", 4.99, "Drinks"),

                ProductEntity("p5", "m2", "Organic Avocado Pack (4pcs)", "Ripe organic Haas avocados, perfect for salads and guacamole", 6.49, "Fresh Produce"),
                ProductEntity("p6", "m2", "Whole Almond Milk 1L", "Unsweetened, rich dairy-free whole almond milk", 3.99, "Dairy & Alternatives"),
                ProductEntity("p7", "m2", "Artisan Sourdough Bread", "Freshly baked crusty rustic sourdough loaf", 4.50, "Bakery"),
                ProductEntity("p8", "m2", "Premium Strawberries 500g", "Sweet and juicy fresh organic strawberries", 5.25, "Fresh Produce"),

                ProductEntity("p9", "m3", "Vitamin C 1000mg (90 Tabs)", "High-strength immune support chewable daily tablets", 11.99, "Supplements"),
                ProductEntity("p10", "m3", "Organic Herbal Sleep Tea", "Chamomile, lavender and valerian root blend for restful sleep", 6.49, "Wellness"),
                ProductEntity("p11", "m3", "Premium Hydrating Face Cream", "Rich hyaluronic acid moisturizing daily facial cream", 18.50, "Skincare"),

                ProductEntity("p12", "m4", "Quattro Formaggi Pizza", "Mozzarella, Gorgonzola, Parmesan, and Fontina cheese with fresh basil", 16.99, "Pizza"),
                ProductEntity("p13", "m4", "Fiery Pepperoni & Hot Honey", "Spicy Italian pepperoni, dynamic hot honey glaze, fresh chili", 17.49, "Pizza"),
                ProductEntity("p14", "m4", "Garlic Butter Dough Balls", "Oven-fresh soft bread bites drenched in garlic butter", 6.50, "Sides")
            )
            dao.insertProducts(defaultProducts)

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

            val defaultUser = UserEntity(
                id = "user@example.com",
                name = "John Doe",
                email = "user@example.com",
                phone = "555-0199",
                passwordHash = PasswordHasher.hash("password123"),
                loginType = "EMAIL",
                avatarUrl = "JD"
            )
            dao.insertUser(defaultUser)
        }
    }
}
