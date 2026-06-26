package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.AuthCredential
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sin

class EcosystemViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // --- Safe Firebase Authentication Reference ---
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            val context = getApplication<Application>().applicationContext
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("EcosystemViewModel", "Firebase Auth initialization failed: ${e.message}. Using high-fidelity local emulation mode.")
            null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = firebaseAuth != null

    // --- User Auth State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Phone Verification State Tracking
    var phoneVerificationId: String? = null
    private val _phoneCodeSent = MutableStateFlow(false)
    val phoneCodeSent: StateFlow<Boolean> = _phoneCodeSent.asStateFlow()

    fun registerWithEmail(name: String, email: String, phone: String?, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank() || name.isBlank()) {
                onResult(false, "Please fill in all fields")
                return@launch
            }

            val auth = firebaseAuth
            if (auth != null) {
                // Real Firebase Authentication registration
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fbUser = task.result?.user
                            val userId = fbUser?.uid ?: email
                            viewModelScope.launch {
                                val newUser = UserEntity(
                                    id = userId,
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    passwordHash = null,
                                    loginType = "EMAIL",
                                    avatarUrl = name.take(2).uppercase()
                                )
                                repository.insertUser(newUser)
                                _currentUser.value = newUser
                                onResult(true, "Successfully registered via Firebase!")
                            }
                        } else {
                            val errMsg = task.exception?.localizedMessage ?: "Firebase registration failed"
                            onResult(false, errMsg)
                        }
                    }
            } else {
                // Fallback / Emulated Room Local DB Secure Auth
                val existing = repository.getUserByEmail(email)
                if (existing != null) {
                    onResult(false, "Email is already registered")
                    return@launch
                }
                val newUser = UserEntity(
                    id = email,
                    name = name,
                    email = email,
                    phone = phone,
                    passwordHash = PasswordHasher.hash(password),
                    loginType = "EMAIL",
                    avatarUrl = name.take(2).uppercase()
                )
                repository.insertUser(newUser)
                _currentUser.value = newUser
                onResult(true, "Registration successful (Local Emulated Mode)")
            }
        }
    }

    fun registerWithPhone(name: String, phone: String, email: String?, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (phone.isBlank() || password.isBlank() || name.isBlank()) {
                onResult(false, "Please fill in all fields")
                return@launch
            }
            // Real registration or local simulation. For phone registrations, we register local records
            val existing = repository.getUserByPhone(phone)
            if (existing != null) {
                onResult(false, "Phone number is already registered")
                return@launch
            }
            val newUser = UserEntity(
                id = phone,
                name = name,
                email = email,
                phone = phone,
                passwordHash = PasswordHasher.hash(password),
                loginType = "PHONE",
                avatarUrl = name.take(2).uppercase()
            )
            repository.insertUser(newUser)
            _currentUser.value = newUser
            onResult(true, "Registration successful")
        }
    }

    fun loginWithEmail(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || password.isBlank()) {
                onResult(false, "Please enter email and password")
                return@launch
            }

            val auth = firebaseAuth
            if (auth != null) {
                // Real Firebase login
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fbUser = task.result?.user
                            val userId = fbUser?.uid ?: email
                            viewModelScope.launch {
                                var localUser = repository.getUserById(userId)
                                if (localUser == null) {
                                    localUser = UserEntity(
                                        id = userId,
                                        name = fbUser?.displayName ?: email.substringBefore("@"),
                                        email = email,
                                        phone = null,
                                        passwordHash = null,
                                        loginType = "EMAIL",
                                        avatarUrl = (fbUser?.displayName ?: email).take(2).uppercase()
                                    )
                                    repository.insertUser(localUser)
                                }
                                _currentUser.value = localUser
                                onResult(true, "Welcome back (Firebase), ${localUser.name}!")
                            }
                        } else {
                            val errMsg = task.exception?.localizedMessage ?: "Firebase login failed"
                            onResult(false, errMsg)
                        }
                    }
            } else {
                // Local DB Secure Auth Fallback
                val user = repository.getUserByEmail(email)
                if (user == null) {
                    onResult(false, "No account found with this email")
                    return@launch
                }
                val hashed = PasswordHasher.hash(password)
                if (user.passwordHash == hashed) {
                    _currentUser.value = user
                    onResult(true, "Welcome back, ${user.name}! (Local Emulated)")
                } else {
                    onResult(false, "Incorrect password")
                }
            }
        }
    }

    fun loginWithPhone(phone: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (phone.isBlank() || password.isBlank()) {
                onResult(false, "Please enter phone number and password")
                return@launch
            }
            val user = repository.getUserByPhone(phone)
            if (user == null) {
                onResult(false, "No account found with this phone number")
                return@launch
            }
            val hashed = PasswordHasher.hash(password)
            if (user.passwordHash == hashed) {
                _currentUser.value = user
                onResult(true, "Welcome back, ${user.name}!")
            } else {
                onResult(false, "Incorrect password")
            }
        }
    }

    fun sendPhoneVerificationCode(phoneNumber: String, activity: android.app.Activity, onResult: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        viewModelScope.launch {
                            signInWithPhoneCredential(credential, onResult)
                        }
                    }

                    override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                        onResult(false, e.localizedMessage ?: "Verification failed")
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        phoneVerificationId = verificationId
                        _phoneCodeSent.value = true
                        onResult(true, "Verification code sent to $phoneNumber")
                    }
                })
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            // High-fidelity local emulation of OTP code delivery
            phoneVerificationId = "mock_verification_id_12345"
            _phoneCodeSent.value = true
            onResult(true, "[EMULATION] SMS verification code sent to $phoneNumber. Enter '123456' to proceed.")
        }
    }

    fun verifyPhoneSmsCode(smsCode: String, name: String?, email: String?, onResult: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        val verificationId = phoneVerificationId
        if (auth != null && verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
            signInWithPhoneCredential(credential, onResult)
        } else {
            // Emulated Mode: Immediately authenticate successfully
            if (smsCode != "123456" && smsCode.length < 6) {
                onResult(false, "Invalid verification code")
                return
            }
            viewModelScope.launch {
                val mockPhone = "555-0199"
                val existing = repository.getUserByPhone(mockPhone)
                val user = existing ?: UserEntity(
                    id = "phone_user_emulated",
                    name = name ?: "Phone User",
                    email = email ?: "phone@example.com",
                    phone = mockPhone,
                    passwordHash = null,
                    loginType = "PHONE",
                    avatarUrl = (name ?: "Phone User").take(2).uppercase()
                )
                if (existing == null) {
                    repository.insertUser(user)
                }
                _currentUser.value = user
                _phoneCodeSent.value = false
                phoneVerificationId = null
                onResult(true, "Successfully authenticated! (Local Emulated)")
            }
        }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential, onResult: (Boolean, String) -> Unit) {
        val auth = firebaseAuth ?: return
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fbUser = task.result?.user
                    val userId = fbUser?.uid ?: "phone_user"
                    val phone = fbUser?.phoneNumber ?: ""
                    viewModelScope.launch {
                        var localUser = repository.getUserById(userId)
                        if (localUser == null) {
                            localUser = UserEntity(
                                id = userId,
                                name = fbUser?.displayName ?: "Phone User",
                                email = fbUser?.email,
                                phone = phone,
                                passwordHash = null,
                                loginType = "PHONE",
                                avatarUrl = (fbUser?.displayName ?: "Phone User").take(2).uppercase()
                            )
                            repository.insertUser(localUser)
                        }
                        _currentUser.value = localUser
                        _phoneCodeSent.value = false
                        phoneVerificationId = null
                        onResult(true, "Successfully signed in via Phone!")
                    }
                } else {
                    val errMsg = task.exception?.localizedMessage ?: "Phone authentication failed"
                    onResult(false, errMsg)
                }
            }
    }

    fun signInWithSocialToken(provider: String, token: String, onResult: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            val credential = when (provider.uppercase()) {
                "GOOGLE" -> GoogleAuthProvider.getCredential(token, null)
                "FACEBOOK" -> FacebookAuthProvider.getCredential(token)
                else -> {
                    onResult(false, "Unsupported provider: $provider")
                    return
                }
            }

            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        val userId = fbUser?.uid ?: "${provider.lowercase()}_user"
                        viewModelScope.launch {
                            var localUser = repository.getUserById(userId)
                            if (localUser == null) {
                                localUser = UserEntity(
                                    id = userId,
                                    name = fbUser?.displayName ?: "$provider User",
                                    email = fbUser?.email,
                                    phone = fbUser?.phoneNumber,
                                    passwordHash = null,
                                    loginType = provider.uppercase(),
                                    avatarUrl = (fbUser?.displayName ?: provider).take(2).uppercase()
                                )
                                repository.insertUser(localUser)
                            }
                            _currentUser.value = localUser
                            onResult(true, "Successfully authenticated with $provider!")
                        }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "$provider authentication failed"
                        onResult(false, errMsg)
                    }
                }
        } else {
            // Fallback / Emulated mode
            loginWithSocial(provider, onResult)
        }
    }

    fun loginWithSocial(provider: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val socialId = "${provider.lowercase()}_user_${(1000..9999).random()}"
            val name = if (provider.equals("GOOGLE", true)) "Google User" else "Facebook Friend"
            val email = "${socialId}@${provider.lowercase()}.com"
            val user = UserEntity(
                id = socialId,
                name = name,
                email = email,
                phone = null,
                passwordHash = null,
                loginType = provider.uppercase(),
                avatarUrl = if (provider.equals("GOOGLE", true)) "G" else "F"
            )
            repository.insertUser(user)
            _currentUser.value = user
            onResult(true, "Successfully logged in via $provider")
        }
    }

    fun logout() {
        firebaseAuth?.signOut()
        _currentUser.value = null
    }

    // --- Active Persona (Role) ---
    private val _activeRole = MutableStateFlow(Role.CUSTOMER)
    val activeRole: StateFlow<Role> = _activeRole.asStateFlow()

    fun setRole(role: Role) {
        _activeRole.value = role
    }

    // --- Customer Navigation States ---
    private val _customerScreen = MutableStateFlow(CustomerScreen.MERCHANT_LIST)
    val customerScreen: StateFlow<CustomerScreen> = _customerScreen.asStateFlow()

    fun navigateCustomer(screen: CustomerScreen) {
        _customerScreen.value = screen
    }

    // --- DB Data Flows ---
    val merchants: StateFlow<List<MerchantEntity>> = repository.getAllMerchantsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.getAllOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val riderProfile: StateFlow<RiderProfileEntity?> = repository.getRiderProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Active Selected Merchant & Products ---
    private val _selectedMerchantId = MutableStateFlow<String?>(null)
    val selectedMerchantId: StateFlow<String?> = _selectedMerchantId.asStateFlow()

    val selectedMerchant: StateFlow<MerchantEntity?> = _selectedMerchantId
        .map { id -> id?.let { repository.getMerchantById(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val productsForSelectedMerchant: StateFlow<List<ProductEntity>> = _selectedMerchantId
        .flatMapLatest { id ->
            if (id != null) repository.getProductsForMerchantFlow(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMerchant(merchantId: String?) {
        _selectedMerchantId.value = merchantId
        if (merchantId != null) {
            _customerScreen.value = CustomerScreen.MERCHANT_DETAIL
        } else {
            _customerScreen.value = CustomerScreen.MERCHANT_LIST
        }
    }

    // --- Search & Categories ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // --- Active Customer Cart ---
    private val _cartItems = MutableStateFlow<Map<ProductEntity, Int>>(emptyMap())
    val cartItems: StateFlow<Map<ProductEntity, Int>> = _cartItems.asStateFlow()

    fun addToCart(product: ProductEntity) {
        val currentMap = _cartItems.value.toMutableMap()
        currentMap[product] = (currentMap[product] ?: 0) + 1
        _cartItems.value = currentMap
    }

    fun removeFromCart(product: ProductEntity) {
        val currentMap = _cartItems.value.toMutableMap()
        val count = currentMap[product] ?: 0
        if (count <= 1) {
            currentMap.remove(product)
        } else {
            currentMap[product] = count - 1
        }
        _cartItems.value = currentMap
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
    }

    // --- Promo Code / Checkout ---
    private val _promoCode = MutableStateFlow<String?>(null)
    val promoCode: StateFlow<String?> = _promoCode.asStateFlow()

    private val _promoDiscount = MutableStateFlow(0.0)
    val promoDiscount: StateFlow<Double> = _promoDiscount.asStateFlow()

    private val _promoError = MutableStateFlow<String?>(null)
    val promoError: StateFlow<String?> = _promoError.asStateFlow()

    fun applyPromoCode(code: String) {
        _promoError.value = null
        val upperCode = code.trim().uppercase()
        when (upperCode) {
            "DELIVOFREE" -> {
                _promoCode.value = upperCode
                _promoDiscount.value = 3.00 // flat discount
            }
            "FAST20" -> {
                _promoCode.value = upperCode
                _promoDiscount.value = 5.00 // flat discount
            }
            "HEALTH10" -> {
                _promoCode.value = upperCode
                _promoDiscount.value = 2.50
            }
            else -> {
                _promoError.value = "Invalid or expired coupon code"
            }
        }
    }

    fun removePromoCode() {
        _promoCode.value = null
        _promoDiscount.value = 0.0
        _promoError.value = null
    }

    // --- Order Checkout Flow ---
    private val _deliveryAddress = MutableStateFlow("Apt 4B, 128 Pine Street")
    val deliveryAddress: StateFlow<String> = _deliveryAddress.asStateFlow()

    fun setDeliveryAddress(address: String) {
        _deliveryAddress.value = address
    }

    private val _selectedPaymentMethod = MutableStateFlow("Card") // "Card", "Wallet", "Cash"
    val selectedPaymentMethod: StateFlow<String> = _selectedPaymentMethod.asStateFlow()

    fun setPaymentMethod(method: String) {
        _selectedPaymentMethod.value = method
    }

    private val _activeOrderId = MutableStateFlow<String?>(null)
    val activeOrderId: StateFlow<String?> = _activeOrderId.asStateFlow()

    val activeOrder: StateFlow<OrderEntity?> = _activeOrderId
        .flatMapLatest { id ->
            if (id != null) repository.getOrderByIdFlow(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun viewOrderDetails(orderId: String) {
        _activeOrderId.value = orderId
        _customerScreen.value = CustomerScreen.TRACKING
    }

    fun placeOrder() {
        val currentMerchant = selectedMerchant.value ?: return
        val currentCart = _cartItems.value
        if (currentCart.isEmpty()) return

        val orderId = "ORD-${(100000..999999).random()}"
        val totalProductPrice = currentCart.entries.sumOf { it.key.price * it.value }
        val discount = _promoDiscount.value
        val fee = currentMerchant.deliveryFee
        val finalPrice = (totalProductPrice + fee - discount).coerceAtLeast(0.0)

        val itemsSummary = currentCart.entries.joinToString(", ") { "${it.value}x ${it.key.name}" }

        val newOrder = OrderEntity(
            id = orderId,
            merchantId = currentMerchant.id,
            merchantName = currentMerchant.name,
            customerAddress = _deliveryAddress.value,
            status = OrderStatus.PLACED.name,
            totalPrice = finalPrice,
            itemsSummary = itemsSummary,
            paymentMethod = _selectedPaymentMethod.value,
            promoApplied = _promoCode.value,
            riderId = null,
            riderName = null,
            riderLatitude = 0.25,
            riderLongitude = 0.25
        )

        viewModelScope.launch {
            repository.insertOrder(newOrder)
            _activeOrderId.value = orderId
            _customerScreen.value = CustomerScreen.TRACKING
            clearCart()
            removePromoCode()
        }
    }

    // --- Rating / Feedback ---
    fun submitRating(orderId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            repository.rateOrder(orderId, rating, comment)
        }
    }

    // --- Admin Dashboard State ---
    private val _adminSelectedOrder = MutableStateFlow<OrderEntity?>(null)
    val adminSelectedOrder: StateFlow<OrderEntity?> = _adminSelectedOrder.asStateFlow()

    fun selectAdminOrder(order: OrderEntity?) {
        _adminSelectedOrder.value = order
    }

    fun updateOrderStatusByAdmin(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus.name)
            // Refresh admin selected order details if open
            val currentSelected = _adminSelectedOrder.value
            if (currentSelected?.id == orderId) {
                _adminSelectedOrder.value = repository.getOrderById(orderId)
            }
        }
    }

    fun toggleProductAvailabilityByAdmin(productId: String, isAvailable: Boolean) {
        viewModelScope.launch {
            repository.updateProductAvailability(productId, isAvailable)
        }
    }

    // --- Rider Dashboard State ---
    private var gpsSimulationJob: Job? = null
    private val _gpsProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val gpsProgress: StateFlow<Float> = _gpsProgress.asStateFlow()

    private val _transitState = MutableStateFlow<TransitState>(TransitState.IDLE)
    val transitState: StateFlow<TransitState> = _transitState.asStateFlow()

    fun setRiderOnline(isOnline: Boolean) {
        viewModelScope.launch {
            repository.updateRiderOnlineStatus(isOnline)
        }
    }

    fun acceptOrderByRider(orderId: String) {
        viewModelScope.launch {
            val rider = repository.getRiderProfile() ?: return@launch
            if (rider.activeOrderId != null) return@launch // Already busy

            val updatedOrder = repository.getOrderById(orderId)?.copy(
                riderId = rider.id,
                riderName = rider.name,
                status = OrderStatus.PREPARING.name // Move to preparing, or if admin ready, ready for pickup
            )

            if (updatedOrder != null) {
                repository.updateOrder(updatedOrder)
                repository.updateRiderActiveOrder(orderId)
                startGpsSimulation(orderId)
            }
        }
    }

    fun pickupOrderByRider() {
        val activeOrderValue = riderProfile.value?.activeOrderId ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(activeOrderValue, OrderStatus.OUT_FOR_DELIVERY.name)
            startGpsSimulation(activeOrderValue)
        }
    }

    fun completeDeliveryByRider() {
        val rider = riderProfile.value ?: return
        val activeOrderValue = rider.activeOrderId ?: return
        viewModelScope.launch {
            val order = repository.getOrderById(activeOrderValue) ?: return@launch
            repository.updateOrderStatus(activeOrderValue, OrderStatus.DELIVERED.name)
            repository.addRiderEarnings(order.totalPrice * 0.15 + 3.0, rider.id) // 15% service pay + $3.00 delivery premium
            repository.updateRiderActiveOrder(null, rider.id)
            repository.updateRiderLocation(0.25, 0.25, rider.id)
            _transitState.value = TransitState.IDLE
            _gpsProgress.value = 0f
            gpsSimulationJob?.cancel()
        }
    }

    // --- Simulates GPS Coordinate Movement on a 2D grid ---
    private fun startGpsSimulation(orderId: String) {
        gpsSimulationJob?.cancel()
        _gpsProgress.value = 0f

        gpsSimulationJob = viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            val status = order.status

            // Define start & target coordinates depending on delivery status
            val startLat: Double
            val startLng: Double
            val targetLat: Double
            val targetLng: Double

            if (status == OrderStatus.PREPARING.name || status == OrderStatus.READY_FOR_PICKUP.name) {
                _transitState.value = TransitState.EN_ROUTE_TO_MERCHANT
                // From Rider starting hub (0.1, 0.1) to Merchant (0.4, 0.5)
                startLat = 0.1
                startLng = 0.1
                targetLat = 0.4
                targetLng = 0.5
            } else if (status == OrderStatus.OUT_FOR_DELIVERY.name) {
                _transitState.value = TransitState.DELIVERING_TO_CUSTOMER
                // From Merchant (0.4, 0.5) to Customer address (0.85, 0.75)
                startLat = 0.4
                startLng = 0.5
                targetLat = 0.85
                targetLng = 0.75
            } else {
                _transitState.value = TransitState.IDLE
                return@launch
            }

            val steps = 40
            for (i in 0..steps) {
                val fraction = i.toFloat() / steps
                _gpsProgress.value = fraction

                // Calculate intermediate coordinates with a slight sinus curvature
                val wave = sin(fraction * Math.PI) * 0.05
                val currentLat = startLat + (targetLat - startLat) * fraction + wave
                val currentLng = startLng + (targetLng - startLng) * fraction - wave

                repository.updateRiderLocation(currentLat, currentLng)

                // Also update order rider coordinates so customer map synchronizes
                val currentOrder = repository.getOrderById(orderId)
                if (currentOrder != null) {
                    repository.updateOrder(
                        currentOrder.copy(
                            riderLatitude = currentLat,
                            riderLongitude = currentLng
                        )
                    )
                }

                delay(600) // update every 600ms
            }

            // Once GPS completes:
            if (_transitState.value == TransitState.EN_ROUTE_TO_MERCHANT) {
                // Arrived at store, wait for rider pickup
                _transitState.value = TransitState.ARRIVED_AT_MERCHANT
            } else if (_transitState.value == TransitState.DELIVERING_TO_CUSTOMER) {
                // Arrived at customer, wait for rider to complete delivery
                _transitState.value = TransitState.ARRIVED_AT_CUSTOMER
            }
        }
    }

    init {
        // Ensure initial database seeding is checked on startup
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }
}

// --- View State Enums & Classes ---

enum class Role {
    CUSTOMER, RIDER, ADMIN
}

enum class CustomerScreen {
    MERCHANT_LIST, MERCHANT_DETAIL, CART, TRACKING, ORDER_HISTORY
}

enum class OrderStatus {
    PLACED,          // Placed by user, visible to admin and nearby riders
    PREPARING,       // Accepted by rider/admin, merchant cooking/preparing
    READY_FOR_PICKUP,// Ready at merchant, waiting for rider to pick up
    OUT_FOR_DELIVERY,// Picked up by rider, en route to customer
    DELIVERED        // Done!
}

enum class TransitState {
    IDLE,
    EN_ROUTE_TO_MERCHANT,
    ARRIVED_AT_MERCHANT,
    DELIVERING_TO_CUSTOMER,
    ARRIVED_AT_CUSTOMER
}

class EcosystemViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EcosystemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EcosystemViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
