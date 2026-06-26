package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateDarkBackground)
                ) { innerPadding ->
                    MainEcosystemApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainEcosystemApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Room Database & Repository
    val database = remember {
        AppDatabase.getDatabase(context, coroutineScope)
    }
    val repository = remember {
        AppRepository(database.appDao())
    }
    
    // Obtain Ecosystem ViewModel
    val factory = remember {
        EcosystemViewModelFactory(context.applicationContext as Application, repository)
    }
    val viewModel: EcosystemViewModel = viewModel(factory = factory)
    
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val riderProfile by viewModel.riderProfile.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SlateDarkBackground)
        ) {
            // --- Premium Header Bar ---
            EcosystemHeader(
                activeRole = activeRole,
                activeOrdersCount = orders.count { it.status != OrderStatus.DELIVERED.name },
                currentUser = currentUser,
                onLogout = { viewModel.logout() },
                onRoleSelected = { viewModel.setRole(it) }
            )

            // --- Role Screen Container ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeRole) {
                    Role.CUSTOMER -> CustomerView(viewModel = viewModel)
                    Role.RIDER -> RiderView(viewModel = viewModel)
                    Role.ADMIN -> AdminView(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// --- PREMIUM GLOBAL HEADER ---
// ==========================================
@Composable
fun EcosystemHeader(
    activeRole: Role,
    activeOrdersCount: Int,
    currentUser: UserEntity?,
    onLogout: () -> Unit,
    onRoleSelected: (Role) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Brand & Global Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(OrangeAccent, OrangeLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Delivo logo",
                            tint = DeepPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "DELIVO",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = OrangeAccent,
                        letterSpacing = 1.5.sp
                    )
                }

                // Profile and Logout Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Global Status Badge
                    Surface(
                        color = SlateSurfaceLight,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activeOrdersCount > 0) OrangeLight else TealSecondary)
                            )
                            Text(
                                text = if (activeOrdersCount > 0) "$activeOrdersCount Active" else "Online",
                                fontSize = 11.sp,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (currentUser != null) {
                        // User initials avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangeAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.avatarUrl ?: currentUser.name.take(2).uppercase(),
                                color = DeepPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Logout Icon button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.size(32.dp).testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Persona Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurfaceLight)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RoleTabButton(
                    roleName = "Customer",
                    icon = Icons.Default.ShoppingBag,
                    isActive = activeRole == Role.CUSTOMER,
                    onClick = { onRoleSelected(Role.CUSTOMER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_customer_tab")
                )
                RoleTabButton(
                    roleName = "Rider",
                    icon = Icons.Default.DeliveryDining,
                    isActive = activeRole == Role.RIDER,
                    onClick = { onRoleSelected(Role.RIDER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_rider_tab")
                )
                RoleTabButton(
                    roleName = "Admin Store",
                    icon = Icons.Default.AdminPanelSettings,
                    isActive = activeRole == Role.ADMIN,
                    onClick = { onRoleSelected(Role.ADMIN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_admin_tab")
                )
            }
        }
    }
}

@Composable
fun RoleTabButton(
    roleName: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) OrangeAccent else Color.Transparent,
        animationSpec = tween(200, easing = LinearEasing),
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) DeepPurple else TextMuted,
        label = "tab_content"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = roleName,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = roleName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ==========================================
// --- CUSTOMER PERSPECTIVE ---
// ==========================================
@Composable
fun CustomerView(viewModel: EcosystemViewModel) {
    val screen by viewModel.customerScreen.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "customer_screen"
    ) { currentScreen ->
        when (currentScreen) {
            CustomerScreen.MERCHANT_LIST -> CustomerMerchantListScreen(viewModel)
            CustomerScreen.MERCHANT_DETAIL -> CustomerMerchantDetailScreen(viewModel)
            CustomerScreen.CART -> CustomerCartScreen(viewModel)
            CustomerScreen.TRACKING -> CustomerTrackingScreen(viewModel)
            CustomerScreen.ORDER_HISTORY -> CustomerOrderHistoryScreen(viewModel)
        }
    }
}

@Composable
fun CustomerMerchantListScreen(viewModel: EcosystemViewModel) {
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    val filteredMerchants = merchants.filter { merchant ->
        val matchesCategory = selectedCategory == "All" || merchant.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = merchant.name.contains(searchQuery, ignoreCase = true) || 
                            merchant.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
    ) {
        // Search & Shortcuts Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search burger, grocery, pharmacy...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = SlateSurface,
                        unfocusedContainerColor = SlateSurface,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Navigation Shortcuts Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explore Categories",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                    
                    if (orders.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.navigateCustomer(CustomerScreen.ORDER_HISTORY) },
                            colors = ButtonDefaults.textButtonColors(contentColor = OrangeAccent)
                        ) {
                            Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Order History", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Category Chips List
                val categories = listOf("All", "Food", "Grocery", "Pharmacy")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        val chipBg = if (isSelected) OrangeAccent else SlateSurface
                        val chipText = if (isSelected) TextWhite else TextMuted
                        val chipBorder = if (isSelected) OrangeAccent else CardBorder
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when(category) {
                                    "Food" -> "🍔 Food"
                                    "Grocery" -> "🥦 Grocery"
                                    "Pharmacy" -> "💊 Pharmacy"
                                    else -> "🌐 All"
                                },
                                color = chipText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Promo Banner Section styled as Hero Promotion
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(DeepPurple, MediumPurple)))
                    .clickable { viewModel.applyPromoCode("DELIVOFREE") }
                    .padding(20.dp)
            ) {
                // Top Right Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(OfferPink)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEW USER OFFER",
                        color = OfferPinkText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Left bottom content
                Column(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "First order\n50% OFF",
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "On all local restaurants today (Promo: DELIVOFREE)",
                        color = OrangeAccent, // #D0BCFF
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Active Orders Floating Notice (if any tracking) - Sophisticated Dark Layout
        val activeOrderList = orders.filter { it.status != OrderStatus.DELIVERED.name }
        if (activeOrderList.isNotEmpty()) {
            item {
                Surface(
                    color = LightPurpleContainer,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.viewOrderDetails(activeOrderList.first().id) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DeepPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = "Active Delivery",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Arriving in 12 min",
                                    color = DeepDarkPurpleText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Order #${activeOrderList.first().id} is ${activeOrderList.first().status.lowercase()}",
                                    color = DeepDarkPurpleText.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Track",
                            tint = DeepDarkPurpleText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Merchants Grid
        item {
            Text(
                text = "Nearby Merchants",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (filteredMerchants.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Empty",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No merchants found matching \"$searchQuery\"",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(filteredMerchants) { merchant ->
                MerchantCard(
                    merchant = merchant,
                    onClick = { viewModel.selectMerchant(merchant.id) },
                    modifier = Modifier.testTag("merchant_card_${merchant.id}")
                )
            }
        }
    }
}

@Composable
fun MerchantCard(
    merchant: MerchantEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = SlateSurface,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Simulated Merchant Cover Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = when (merchant.category) {
                                "Food" -> listOf(Color(0xFFE65100), Color(0xFFFFB300))
                                "Grocery" -> listOf(Color(0xFF1B5E20), Color(0xFF81C784))
                                "Pharmacy" -> listOf(Color(0xFF006064), Color(0xFF4DD0E1))
                                else -> listOf(Color(0xFF37474F), Color(0xFF90A4AE))
                            }
                        )
                    )
                    .padding(12.dp)
            ) {
                // Category Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = merchant.category.uppercase(),
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Merchant Info
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = merchant.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = GoldStar,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = merchant.rating.toString(),
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = merchant.promoText,
                    color = OrangeLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Divider(color = CardBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Delivery Time",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${merchant.deliveryTimeMinutes} mins",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = "Delivery Fee",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (merchant.deliveryFee == 0.0) "FREE Delivery" else "$${merchant.deliveryFee} Delivery",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerMerchantDetailScreen(viewModel: EcosystemViewModel) {
    val merchant by viewModel.selectedMerchant.collectAsStateWithLifecycle()
    val products by viewModel.productsForSelectedMerchant.collectAsStateWithLifecycle()
    val cart by viewModel.cartItems.collectAsStateWithLifecycle()

    if (merchant == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Top Detail Header
        Surface(
            color = SlateSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.selectMerchant(null) },
                        modifier = Modifier.testTag("merchant_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchant!!.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextWhite
                        )
                        Text(
                            text = merchant!!.category,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Surface(
                        color = OrangeAccent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, OrangeAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Star", tint = GoldStar, modifier = Modifier.size(14.dp))
                            Text(merchant!!.rating.toString(), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ ${merchant!!.deliveryTimeMinutes} mins",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "💰 Delivery: $${merchant!!.deliveryFee}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "🏷️ ${merchant!!.promoText}",
                        color = OrangeLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Product Catalog
        Box(modifier = Modifier.weight(1f)) {
            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangeAccent)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "Catalog Menu",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(products) { product ->
                        ProductItemRow(
                            product = product,
                            quantityInCart = cart[product] ?: 0,
                            onAdd = { viewModel.addToCart(product) },
                            onRemove = { viewModel.removeFromCart(product) },
                            modifier = Modifier.testTag("product_item_${product.id}")
                        )
                    }
                }
            }

            // Bottom Floating Cart Bar
            if (cart.isNotEmpty()) {
                val totalItems = cart.values.sum()
                val totalPrice = cart.entries.sumOf { it.key.price * it.value }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("floating_cart_bar"),
                    color = OrangeAccent,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { viewModel.navigateCustomer(CustomerScreen.CART) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(TextWhite.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = totalItems.toString(),
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "View Active Cart",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalPrice)}",
                                color = TextWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = TextWhite, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(
    product: ProductEntity,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Content: Name & Description & Price
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    if (!product.isAvailable) {
                        Surface(
                            color = SoftRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, SoftRed)
                        ) {
                            Text(
                                text = "SOLD OUT",
                                color = SoftRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = product.description,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "$${product.price}",
                    color = OrangeAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Right Content: Add/Remove Button or Sold Out indicator
            if (!product.isAvailable) {
                Surface(
                    color = SlateSurfaceLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "Out of Stock",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            } else if (quantityInCart > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurfaceLight)
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = OrangeAccent, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = quantityInCart.toString(),
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = OrangeAccent, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = DeepPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", color = DeepPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CustomerCartScreen(viewModel: EcosystemViewModel) {
    val cart by viewModel.cartItems.collectAsStateWithLifecycle()
    val merchant by viewModel.selectedMerchant.collectAsStateWithLifecycle()
    val promoCode by viewModel.promoCode.collectAsStateWithLifecycle()
    val promoDiscount by viewModel.promoDiscount.collectAsStateWithLifecycle()
    val promoError by viewModel.promoError.collectAsStateWithLifecycle()
    val address by viewModel.deliveryAddress.collectAsStateWithLifecycle()
    val selectedPayment by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()

    var promoInput by remember { mutableStateOf("") }

    if (merchant == null) return

    val subtotal = cart.entries.sumOf { it.key.price * it.value }
    val deliveryFee = merchant!!.deliveryFee
    val total = (subtotal + deliveryFee - promoDiscount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Header
        Surface(color = SlateSurface, border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = { viewModel.navigateCustomer(CustomerScreen.MERCHANT_DETAIL) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Text("Review Your Cart", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhite)
            }
        }

        if (cart.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.RemoveShoppingCart, contentDescription = "Empty Cart", tint = TextMuted, modifier = Modifier.size(64.dp))
                    Text("Your cart is empty!", color = TextWhite, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.navigateCustomer(CustomerScreen.MERCHANT_DETAIL) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("Add products", color = DeepPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Cart Items List
            item {
                Text("Items from ${merchant!!.name}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            items(cart.entries.toList()) { (product, qty) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$${product.price} each", color = TextMuted, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = { viewModel.removeFromCart(product) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Dec", tint = OrangeAccent)
                            }
                            Text(qty.toString(), color = TextWhite, fontWeight = FontWeight.Black)
                            IconButton(onClick = { viewModel.addToCart(product) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Inc", tint = OrangeAccent)
                            }
                        }
                        Text(
                            "$${String.format(Locale.US, "%.2f", product.price * qty)}",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // Delivery Address Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Address", tint = OrangeAccent, modifier = Modifier.size(18.dp))
                            Text("Delivery Destination", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { viewModel.setDeliveryAddress(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cart_address_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = CardBorder
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Coupon Code Application
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Have a promo coupon?", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = { promoInput = it },
                                placeholder = { Text("e.g. DELIVOFREE", color = TextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cart_promo_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder
                                ),
                                singleLine = true
                            )
                            Button(
                                onClick = { viewModel.applyPromoCode(promoInput) },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("APPLY", color = DeepPurple, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (promoError != null) {
                            Text(promoError!!, color = SoftRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (promoCode != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TealSecondary.copy(alpha = 0.1f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Code $promoCode Applied (-$${String.format(Locale.US, "%.2f", promoDiscount)})",
                                    color = TealSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.removePromoCode() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Code", tint = SoftRed)
                                }
                            }
                        }
                    }
                }
            }

            // Payment Option Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Method", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Card", "Wallet", "Cash").forEach { method ->
                            val isSelected = selectedPayment == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) OrangeAccent.copy(alpha = 0.15f) else SlateSurface)
                                    .border(1.dp, if (isSelected) OrangeAccent else CardBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPaymentMethod(method) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = when(method) {
                                            "Card" -> Icons.Default.CreditCard
                                            "Wallet" -> Icons.Default.AccountBalanceWallet
                                            else -> Icons.Default.Payments
                                        },
                                        contentDescription = method,
                                        tint = if (isSelected) OrangeAccent else TextMuted
                                    )
                                    Text(method, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Cost Summary Ledger Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Basket Subtotal", color = TextMuted, fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", subtotal)}", color = TextWhite, fontSize = 13.sp)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Delivery Fee", color = TextMuted, fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", deliveryFee)}", color = TextWhite, fontSize = 13.sp)
                        }
                        if (promoDiscount > 0.0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Discount Promo Applied", color = TealSecondary, fontSize = 13.sp)
                                Text("-$${String.format(Locale.US, "%.2f", promoDiscount)}", color = TealSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(color = CardBorder, thickness = 1.dp)
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Total to Pay", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("$${String.format(Locale.US, "%.2f", total)}", color = OrangeAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Big Action Button
            item {
                Button(
                    onClick = { viewModel.placeOrder() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("checkout_place_order_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Secure Payment", tint = DeepPurple)
                        Text(
                            text = "SECURELY CHECKOUT - $${String.format(Locale.US, "%.2f", total)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurple
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerTrackingScreen(viewModel: EcosystemViewModel) {
    val order by viewModel.activeOrder.collectAsStateWithLifecycle()
    val transitState by viewModel.transitState.collectAsStateWithLifecycle()
    val gpsProgress by viewModel.gpsProgress.collectAsStateWithLifecycle()

    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.HelpOutline, contentDescription = "No order", tint = TextMuted, modifier = Modifier.size(48.dp))
                Text("No active order currently being tracked", color = TextMuted)
                Button(onClick = { viewModel.navigateCustomer(CustomerScreen.MERCHANT_LIST) }, colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)) {
                    Text("Return to shop", color = DeepPurple, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Tracker Top Info Block
        Surface(
            color = SlateSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateCustomer(CustomerScreen.MERCHANT_LIST) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }

                    Text(
                        text = "ORDER ID: ${order!!.id}",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )

                    Surface(
                        color = when (order!!.status) {
                            "PLACED" -> OrangeAccent.copy(alpha = 0.15f)
                            "PREPARING" -> SoftOrange.copy(alpha = 0.15f)
                            "READY_FOR_PICKUP" -> SoftOrange.copy(alpha = 0.15f)
                            "OUT_FOR_DELIVERY" -> TealSecondary.copy(alpha = 0.15f)
                            else -> SlateSurfaceLight
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = order!!.status,
                            color = when (order!!.status) {
                                "PLACED" -> OrangeAccent
                                "PREPARING" -> SoftOrange
                                "READY_FOR_PICKUP" -> SoftOrange
                                "OUT_FOR_DELIVERY" -> TealSecondary
                                else -> TextWhite
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = order!!.merchantName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite
                )

                Text(
                    text = "Delivering to: ${order!!.customerAddress}",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Text(
                    text = "Items Ordered: ${order!!.itemsSummary}",
                    fontSize = 12.sp,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Stepper Tracker visual
        TrackingStepper(currentStatus = order!!.status)

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated live GPS Map
        Text(
            text = "Simulated Live GPS Dispatch Map",
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        SimulatedMap(
            riderLat = order!!.riderLatitude,
            riderLng = order!!.riderLongitude,
            riderOnline = order!!.riderId != null,
            transitState = transitState,
            progress = gpsProgress,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Delivery feedback if completed
        if (order!!.status == "DELIVERED") {
            RatingFeedbackForm(
                order = order!!,
                onSubmit = { rating, comment ->
                    viewModel.submitRating(order!!.id, rating, comment)
                }
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OrangeAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Agent", tint = OrangeAccent)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (order!!.riderName != null) "Rider: ${order!!.riderName}" else "Finding Rider...",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = when(order!!.status) {
                                "PLACED" -> "Waiting for store to confirm and dispatch your rider"
                                "PREPARING" -> "Merchant is preparing items"
                                "READY_FOR_PICKUP" -> "Rider is picking up items"
                                "OUT_FOR_DELIVERY" -> "Rider is heading your way with active GPS coordinate stream!"
                                else -> "Arrived"
                            },
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrackingStepper(currentStatus: String) {
    val stages = listOf("PLACED", "PREPARING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED")
    val currentIndex = stages.indexOf(currentStatus)

    Surface(
        color = SlateSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ecosystem Delivery Tracker", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
            
            stages.forEachIndexed { index, stage ->
                val isCompleted = index < currentIndex
                val isActive = index == currentIndex
                val isUpcoming = index > currentIndex

                val color = when {
                    isCompleted -> TealSecondary
                    isActive -> OrangeAccent
                    else -> TextMuted
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Bullet
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f))
                            .border(2.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = TealSecondary, modifier = Modifier.size(12.dp))
                        } else if (isActive) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(OrangeAccent))
                        }
                    }

                    Column {
                        Text(
                            text = when(stage) {
                                "PLACED" -> "Order Placed"
                                "PREPARING" -> "Preparing & Cooking"
                                "READY_FOR_PICKUP" -> "Ready for Rider Pick up"
                                "OUT_FOR_DELIVERY" -> "Out for Delivery"
                                else -> "Delivered successfully"
                            },
                            color = if (isUpcoming) TextMuted else TextWhite,
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingFeedbackForm(
    order: OrderEntity,
    onSubmit: (Int, String) -> Unit
) {
    var ratingSelected by remember { mutableIntStateOf(order.rating) }
    var comment by remember { mutableStateOf(order.ratingComment ?: "") }
    var submitted by remember { mutableStateOf(order.rating > 0) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.5.dp, if (submitted) TealSecondary else OrangeAccent),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (submitted) "Thank You for Your Rating!" else "Rate Your Experience",
                color = TextWhite,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )

            // Star Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    val isFilled = star <= ratingSelected
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $star",
                        tint = if (isFilled) GoldStar else TextMuted,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = !submitted) { ratingSelected = star }
                    )
                }
            }

            if (!submitted) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("What made this experience outstanding or needs improvement?", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = CardBorder
                    ),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        if (ratingSelected > 0) {
                            onSubmit(ratingSelected, comment)
                            submitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Feedback", fontWeight = FontWeight.Bold, color = DeepPurple)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealSecondary.copy(alpha = 0.1f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = "Submitted", tint = TealSecondary)
                    Column {
                        Text("Rating submitted: $ratingSelected / 5 stars", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (comment.isNotEmpty()) {
                            Text("\"$comment\"", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOrderHistoryScreen(viewModel: EcosystemViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
    ) {
        // Header
        Surface(color = SlateSurface, border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = { viewModel.navigateCustomer(CustomerScreen.MERCHANT_LIST) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Text("Order History", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextWhite)
            }
        }

        if (orders.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Empty list", tint = TextMuted, modifier = Modifier.size(48.dp))
                    Text("No orders placed yet", color = TextMuted)
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            items(orders) { order ->
                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (order.status != "DELIVERED") OrangeAccent else CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.viewOrderDetails(order.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(order.id, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                text = order.status,
                                color = if (order.status == "DELIVERED") TealSecondary else OrangeAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Text(order.merchantName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(order.itemsSummary, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Paid: $${order.totalPrice}",
                                color = OrangeLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            if (order.rating > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Star", tint = GoldStar, modifier = Modifier.size(14.dp))
                                    Text("${order.rating}/5", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// --- LIVE 2D INTERACTIVE GPS MAP ---
// ==========================================
@Composable
fun SimulatedMap(
    riderLat: Double,
    riderLng: Double,
    riderOnline: Boolean,
    transitState: TransitState,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color(0xFF0F1722))) {
        val width = size.width
        val height = size.height

        // 1. Draw Grid Lines (Streets layout)
        val streetGridLines = 8
        for (i in 1..streetGridLines) {
            val offsetVertical = i * (width / (streetGridLines + 1))
            val offsetHorizontal = i * (height / (streetGridLines + 1))

            // Draw streets grid lines
            drawLine(
                color = CardBorder.copy(alpha = 0.3f),
                start = Offset(offsetVertical, 0f),
                end = Offset(offsetVertical, height),
                strokeWidth = 3f
            )
            drawLine(
                color = CardBorder.copy(alpha = 0.3f),
                start = Offset(0f, offsetHorizontal),
                end = Offset(width, offsetHorizontal),
                strokeWidth = 3f
            )
        }

        // Pin Points Coordinates on Screen Space
        // Merchant Start Hub: Lat 0.1, Lng 0.1
        // Store Point: Lat 0.4, Lng 0.5
        // Customer Point: Lat 0.85, Lng 0.75
        val hubPoint = Offset(0.1f * width, 0.1f * height)
        val merchantPoint = Offset(0.4f * width, 0.5f * height)
        val customerPoint = Offset(0.85f * width, 0.75f * height)

        val activeRiderPoint = Offset(riderLat.toFloat() * width, riderLng.toFloat() * height)

        // 2. Draw Route Paths
        // From Hub to Merchant
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = SoftOrange.copy(alpha = 0.5f),
            start = hubPoint,
            end = merchantPoint,
            strokeWidth = 4f,
            pathEffect = pathEffect
        )

        // From Merchant to Customer
        drawLine(
            color = TealSecondary.copy(alpha = 0.5f),
            start = merchantPoint,
            end = customerPoint,
            strokeWidth = 4f,
            pathEffect = pathEffect
        )

        // 3. Draw Store Building Landmark (Merchant Pin)
        drawCircle(
            color = Color(0xFFE65100),
            center = merchantPoint,
            radius = 16f
        )
        drawCircle(
            color = TextWhite,
            center = merchantPoint,
            radius = 6f
        )

        // 4. Draw Customer Landmark (Home Pin)
        drawCircle(
            color = Color(0xFF00C853),
            center = customerPoint,
            radius = 16f
        )
        drawCircle(
            color = TextWhite,
            center = customerPoint,
            radius = 6f
        )

        // 5. Draw active Rider dot if assigned
        if (riderOnline) {
            // Pulse circle
            drawCircle(
                color = OrangeAccent.copy(alpha = 0.3f),
                center = activeRiderPoint,
                radius = 24f + (5f * java.lang.Math.sin(progress * java.lang.Math.PI * 8).toFloat())
            )

            // Core rider pin
            drawCircle(
                color = OrangeAccent,
                center = activeRiderPoint,
                radius = 12f
            )
            drawCircle(
                color = TextWhite,
                center = activeRiderPoint,
                radius = 4f
            )
        }
    }
}


// ==========================================
// --- RIDER / COURIER PERSPECTIVE ---
// ==========================================
@Composable
fun RiderView(viewModel: EcosystemViewModel) {
    val profile by viewModel.riderProfile.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val transitState by viewModel.transitState.collectAsStateWithLifecycle()
    val gpsProgress by viewModel.gpsProgress.collectAsStateWithLifecycle()

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangeAccent)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Rider Earnings Profile Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.5.dp, if (profile!!.isOnline) TealSecondary else CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(profile!!.name, color = TextWhite, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Professional Delivo Courier", color = TextMuted, fontSize = 12.sp)
                        }

                        // Online/Offline Switch
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (profile!!.isOnline) "ONLINE" else "OFFLINE",
                                color = if (profile!!.isOnline) TealSecondary else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Switch(
                                checked = profile!!.isOnline,
                                onCheckedChange = { viewModel.setRiderOnline(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TealSecondary,
                                    checkedTrackColor = TealSecondary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("rider_online_switch")
                            )
                        }
                    }

                    Divider(color = CardBorder)

                    // Financial metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("LIFETIME EARNINGS", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "$${String.format(Locale.US, "%.2f", profile!!.totalEarnings)}",
                                color = TealSecondary,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SIMULATED STATUS", color = TextMuted, fontSize = 10.sp)
                            Surface(
                                color = if (profile!!.activeOrderId != null) OrangeAccent.copy(alpha = 0.15f) else TealSecondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (profile!!.activeOrderId != null) "ON TRIP" else "AVAILABLE",
                                    color = if (profile!!.activeOrderId != null) OrangeAccent else TealSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!profile!!.isOnline) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PowerOff, contentDescription = "Offline", tint = TextMuted, modifier = Modifier.size(48.dp))
                        Text("You are offline. Go online to accept delivery jobs!", color = TextMuted)
                    }
                }
            }
            return@LazyColumn
        }

        // Active Assigned Trip Card
        if (profile!!.activeOrderId != null) {
            val activeOrder = orders.find { it.id == profile!!.activeOrderId }
            
            if (activeOrder != null) {
                item {
                    Text("ACTIVE DELIVERY JOB", color = OrangeLight, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.5.dp, OrangeAccent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("TRIP ID: ${activeOrder.id}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "EST. PAYOUT: $${String.format(Locale.US, "%.2f", activeOrder.totalPrice * 0.15 + 3.0)}",
                                    color = TealSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("STORE PICKUP:", color = TextMuted, fontSize = 10.sp)
                                Text(activeOrder.merchantName, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text("CUSTOMER DROP OFF:", color = TextMuted, fontSize = 10.sp)
                                Text(activeOrder.customerAddress, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text("DELIVERY ITEMS:", color = TextMuted, fontSize = 10.sp)
                                Text(activeOrder.itemsSummary, color = TextWhite, fontSize = 12.sp)
                            }

                            Divider(color = CardBorder)

                            // Simulated GPS preview
                            Text("Simulated Rider GPS Progress: ${(gpsProgress * 100).toInt()}%", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { gpsProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = OrangeAccent,
                                trackColor = SlateSurfaceLight
                            )

                            // Transit stage alert description
                            Text(
                                text = "GPS Mode: " + when(transitState) {
                                    TransitState.EN_ROUTE_TO_MERCHANT -> "En Route to Merchant. Speeding on grid coordinate streams."
                                    TransitState.ARRIVED_AT_MERCHANT -> "Arrived at Merchant store! Pick up items."
                                    TransitState.DELIVERING_TO_CUSTOMER -> "En route to Customer home address. Live coordinate feed streaming."
                                    TransitState.ARRIVED_AT_CUSTOMER -> "Arrived at Customer address! Handover pack."
                                    else -> "Idle"
                                },
                                color = OrangeLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Action button depending on status
                            when (activeOrder.status) {
                                "PREPARING", "READY_FOR_PICKUP" -> {
                                    Button(
                                        onClick = { viewModel.pickupOrderByRider() },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = transitState == TransitState.ARRIVED_AT_MERCHANT || activeOrder.status == "READY_FOR_PICKUP"
                                    ) {
                                        Text("PICK UP & DEPART FROM STORE", fontWeight = FontWeight.Bold, color = DeepPurple)
                                    }
                                }
                                "OUT_FOR_DELIVERY" -> {
                                    Button(
                                        onClick = { viewModel.completeDeliveryByRider() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = transitState == TransitState.ARRIVED_AT_CUSTOMER
                                    ) {
                                        Text("CONFIRM DROP-OFF & COLLECT PAYOUT", fontWeight = FontWeight.Bold, color = SlateDarkBackground)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Job Board
            val availableJobs = orders.filter { it.status == "PLACED" || it.status == "READY_FOR_PICKUP" }

            item {
                Text("AVAILABLE DELIVERY JOBS (${availableJobs.size})", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }

            if (availableJobs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = "Waiting", tint = TextMuted, modifier = Modifier.size(48.dp))
                            Text("Waiting for new customer orders in your area...", color = TextMuted)
                        }
                    }
                }
            } else {
                items(availableJobs) { job ->
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("JOB ID: ${job.id}", color = TextWhite, fontWeight = FontWeight.Bold)
                                Text(
                                    "PAY: $${String.format(Locale.US, "%.2f", job.totalPrice * 0.15 + 3.0)}",
                                    color = TealSecondary,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Pickup: ${job.merchantName}", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Deliver: ${job.customerAddress}", color = TextMuted, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.acceptOrderByRider(job.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ACCEPT DELIVERY TRIP", fontWeight = FontWeight.Bold, color = DeepPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// --- ECOSYSTEM ADMIN / STORE OWNER ---
// ==========================================
@Composable
fun AdminView(viewModel: EcosystemViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()
    val riderProfile by viewModel.riderProfile.collectAsStateWithLifecycle()
    val selectedAdminOrder by viewModel.adminSelectedOrder.collectAsStateWithLifecycle()

    var activeAdminTab by remember { mutableStateOf("Orders") }

    // Analytics Calculation
    val totalRevenue = orders.filter { it.status == "DELIVERED" }.sumOf { it.totalPrice }
    val totalOrdersCount = orders.size
    val completedOrders = orders.filter { it.status == "DELIVERED" }
    val ratedOrders = completedOrders.filter { it.rating > 0 }
    val avgRating = if (ratedOrders.isNotEmpty()) ratedOrders.sumOf { it.rating }.toDouble() / ratedOrders.size else 4.8
    val activeCouriersOnline = if (riderProfile?.isOnline == true) 1 else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Core KPIs Metrics Section
        item {
            Text("ECOSYSTEM ANALYTICS KPI", color = TextWhite, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Revenue card
                item {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("REVENUE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("$${String.format(Locale.US, "%.2f", totalRevenue)}", color = TealSecondary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("From completed deliveries", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Total Orders card
                item {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TOTAL ORDERS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(totalOrdersCount.toString(), color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Active & Historic", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Average Rating Card
                item {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("CUSTOMER RATING", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Star, contentDescription = "Star", tint = GoldStar, modifier = Modifier.size(16.dp))
                                Text(String.format(Locale.US, "%.1f", avgRating), color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Text("Feedback rating avg", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }

                // Active Rider Card
                item {
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("COURIERS ONLINE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(activeCouriersOnline.toString(), color = OrangeAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("Alex Rider", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        // Tab Selector Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceLight)
                    .padding(4.dp)
            ) {
                listOf("Orders", "Merchants Control").forEach { tab ->
                    val isSelected = activeAdminTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SlateSurface else Color.Transparent)
                            .clickable { activeAdminTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tab, color = if (isSelected) OrangeLight else TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tab Render
        if (activeAdminTab == "Orders") {
            // Orders Tab
            if (orders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Inbox, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(48.dp))
                            Text("No customer orders placed yet", color = TextMuted)
                        }
                    }
                }
            } else {
                items(orders) { order ->
                    val isSelected = selectedAdminOrder?.id == order.id
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) OrangeAccent else CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectAdminOrder(if (isSelected) null else order) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(order.id, color = TextWhite, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = when(order.status) {
                                        "DELIVERED" -> TealSecondary.copy(alpha = 0.15f)
                                        else -> OrangeAccent.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = order.status,
                                        color = if (order.status == "DELIVERED") TealSecondary else OrangeAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text("Store: ${order.merchantName}", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Value: $${order.totalPrice}", color = OrangeLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                            if (isSelected) {
                                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                                Text("Items: ${order.itemsSummary}", color = TextWhite, fontSize = 12.sp)
                                Text("Destination: ${order.customerAddress}", color = TextMuted, fontSize = 12.sp)
                                if (order.riderName != null) {
                                    Text("Courier: ${order.riderName}", color = TealSecondary, fontSize = 12.sp)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (order.status == "PLACED") {
                                        Button(
                                            onClick = { viewModel.updateOrderStatusByAdmin(order.id, OrderStatus.PREPARING) },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("COOK & PREPARE", color = DeepPurple, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (order.status == "PREPARING") {
                                        Button(
                                            onClick = { viewModel.updateOrderStatusByAdmin(order.id, OrderStatus.READY_FOR_PICKUP) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("MARK READY FOR RIDER", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Merchants Control Tab
            items(merchants) { merchant ->
                MerchantControlRow(merchant = merchant, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MerchantControlRow(
    merchant: MerchantEntity,
    viewModel: EcosystemViewModel
) {
    val products by viewModel.productsForSelectedMerchant.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            viewModel.selectMerchant(merchant.id)
        }
    }

    Surface(
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isExpanded) OrangeAccent else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(merchant.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Category: ${merchant.category}", color = TextMuted, fontSize = 11.sp)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = TextMuted
                )
            }

            if (isExpanded) {
                Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                Text("TOGGLE MENU PRODUCT STOCK:", color = OrangeLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                if (products.isEmpty()) {
                    Text("Loading store products...", color = TextMuted, fontSize = 11.sp)
                } else {
                    products.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("$${product.price}", color = TextMuted, fontSize = 11.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (product.isAvailable) "IN STOCK" else "OUT",
                                    color = if (product.isAvailable) TealSecondary else SoftRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = product.isAvailable,
                                    onCheckedChange = { viewModel.toggleProductAvailabilityByAdmin(product.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TealSecondary,
                                        checkedTrackColor = TealSecondary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = SoftRed,
                                        uncheckedTrackColor = SoftRed.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to scale standard switch size down slightly
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.drawBehind { } // Placeholder implementation
)

// ==========================================
// --- HIGH FIDELITY AUTHENTICATION SCREEN ---
// ==========================================
@Composable
fun AuthScreen(viewModel: EcosystemViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var loginWithEmailTab by remember { mutableStateOf(true) } // true = email, false = phone

    // Input States
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var smsOtpCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // State Tracking from ViewModel
    val phoneCodeSent by viewModel.phoneCodeSent.collectAsStateWithLifecycle()
    val isFbAvailable = viewModel.isFirebaseAvailable

    // Feedback states
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessFeedback by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Banner
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(DeepPurple, MediumPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Delivo logo",
                    tint = OrangeAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DELIVO",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = OrangeAccent,
                letterSpacing = 2.sp
            )
            Text(
                text = "Fast. Fresh. Secure.",
                fontSize = 14.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase Connection Status Chip
            Surface(
                color = if (isFbAvailable) TealSecondary.copy(alpha = 0.12f) else OrangeAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isFbAvailable) TealSecondary else OrangeAccent),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isFbAvailable) TealSecondary else OrangeAccent)
                    )
                    Text(
                        text = if (isFbAvailable) "Firebase Cloud Connected" else "Firebase Local Emulation Active",
                        fontSize = 11.sp,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Auth Tab Switcher (Don't show when waiting for OTP code to keep focus clean)
            if (!phoneCodeSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isRegisterMode) OrangeAccent else Color.Transparent)
                            .clickable {
                                isRegisterMode = false
                                feedbackMessage = null
                            }
                            .padding(vertical = 12.dp)
                            .testTag("auth_tab_login"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = if (!isRegisterMode) DeepPurple else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isRegisterMode) OrangeAccent else Color.Transparent)
                            .clickable {
                                isRegisterMode = true
                                feedbackMessage = null
                            }
                            .padding(vertical = 12.dp)
                            .testTag("auth_tab_register"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Register",
                            color = if (isRegisterMode) DeepPurple else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Interactive Feedback Card
            feedbackMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccessFeedback) TealSecondary.copy(alpha = 0.15f) else SoftRed.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSuccessFeedback) TealSecondary else SoftRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccessFeedback) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = "Feedback Status",
                            tint = if (isSuccessFeedback) TealSecondary else SoftRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = msg,
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Sub-Auth tabs for Login only (Email vs Phone option)
            if (!isRegisterMode && !phoneCodeSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { loginWithEmailTab = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (loginWithEmailTab) SlateSurfaceLight else SlateSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (loginWithEmailTab) OrangeAccent else CardBorder)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = if (loginWithEmailTab) OrangeAccent else TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Email Signin", fontSize = 12.sp, color = if (loginWithEmailTab) TextWhite else TextMuted)
                    }
                    Button(
                        onClick = { loginWithEmailTab = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!loginWithEmailTab) SlateSurfaceLight else SlateSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!loginWithEmailTab) OrangeAccent else CardBorder)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = if (!loginWithEmailTab) OrangeAccent else TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phone OTP", fontSize = 12.sp, color = if (!loginWithEmailTab) TextWhite else TextMuted)
                    }
                }
            }

            // Inputs card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (phoneCodeSent) {
                        // Phone OTP SMS Verification view
                        Text(
                            text = "Enter OTP Verification Code",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A 6-digit verification code was sent to $phone. Please enter it below to securely authenticate.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        OutlinedTextField(
                            value = smsOtpCode,
                            onValueChange = { if (it.length <= 6) smsOtpCode = it },
                            label = { Text("6-Digit Verification Code", color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeAccent) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("auth_input_otp"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = OrangeAccent,
                                unfocusedBorderColor = CardBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (smsOtpCode.length != 6) {
                                    isSuccessFeedback = false
                                    feedbackMessage = "Please enter a valid 6-digit OTP code"
                                    return@Button
                                }
                                viewModel.verifyPhoneSmsCode(smsOtpCode, name.ifBlank { null }, email.ifBlank { null }) { success, msg ->
                                    isSuccessFeedback = success
                                    feedbackMessage = msg
                                    if (success) {
                                        smsOtpCode = ""
                                        phone = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("VERIFY & SIGN IN", color = DeepPurple, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                // Cancel OTP code entry and go back
                                viewModel.verifyPhoneSmsCode("", null, null) { _, _ -> } // clears state
                                smsOtpCode = ""
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Change Phone Number / Go Back", color = OrangeAccent, fontSize = 12.sp)
                        }

                    } else {
                        // Regular Inputs flow (Email and standard Phone fields)
                        if (isRegisterMode) {
                            // Full Name input for registration
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = OrangeAccent) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_input_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Email input (Always shown in Register, or shown in Email Login)
                        if (isRegisterMode || loginWithEmailTab) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = OrangeAccent) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth().testTag("auth_input_email"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Phone input (Always shown in Register, or shown in Phone Login Tab)
                        if (isRegisterMode || !loginWithEmailTab) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(if (isRegisterMode) "Phone Number (Optional)" else "Phone Number (with Country Code)", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = OrangeAccent) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("auth_input_phone"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Password input (Only shown when not using Phone OTP Login)
                        if (isRegisterMode || loginWithEmailTab) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password", color = TextMuted) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeAccent) },
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(icon, contentDescription = "Toggle password visibility", tint = TextMuted)
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("auth_input_password"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = OrangeAccent,
                                    unfocusedBorderColor = CardBorder
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Secure Password handling / Cloud sync indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = TealSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFbAvailable) "Fully secured & cloud sync by Firebase" else "Locally encrypted & hashed with SHA-256",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Primary Action Button (Sign In or Register)
                        Button(
                            onClick = {
                                feedbackMessage = null
                                if (isRegisterMode) {
                                    val onComplete: (Boolean, String) -> Unit = { success, msg ->
                                        isSuccessFeedback = success
                                        feedbackMessage = msg
                                        if (success) {
                                            // Reset fields
                                            name = ""
                                            email = ""
                                            phone = ""
                                            password = ""
                                        }
                                    }
                                    if (loginWithEmailTab) {
                                        viewModel.registerWithEmail(name, email, phone.ifBlank { null }, password, onComplete)
                                    } else {
                                        viewModel.registerWithPhone(name, phone, email.ifBlank { null }, password, onComplete)
                                    }
                                } else {
                                    val onComplete: (Boolean, String) -> Unit = { success, msg ->
                                        isSuccessFeedback = success
                                        feedbackMessage = msg
                                    }
                                    if (loginWithEmailTab) {
                                        viewModel.loginWithEmail(email, password, onComplete)
                                    } else {
                                        // Phone OTP Flow Initiation
                                        if (phone.isBlank()) {
                                            isSuccessFeedback = false
                                            feedbackMessage = "Please enter your phone number"
                                            return@Button
                                        }
                                        if (activity != null) {
                                            viewModel.sendPhoneVerificationCode(phone, activity, onComplete)
                                        } else {
                                            // Safe fallback if activity is missing (e.g., in previews)
                                            viewModel.sendPhoneVerificationCode(phone, android.app.Activity(), onComplete)
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_submit_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isRegisterMode) Icons.Default.AppRegistration else if (!loginWithEmailTab) Icons.Default.Sms else Icons.Default.Login,
                                    contentDescription = null,
                                    tint = DeepPurple
                                )
                                Text(
                                    text = if (isRegisterMode) "CREATE ACCOUNT" else if (!loginWithEmailTab) "SEND VERIFICATION CODE" else "SECURE SIGN IN",
                                    color = DeepPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }

            // Social Integration Section (only show when not waiting for OTP code)
            if (!phoneCodeSent) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Or continue instantly with",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Sign In Button
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                feedbackMessage = null
                                // Pass a simulated client Google token, or use high fidelity callback
                                viewModel.signInWithSocialToken("Google", "google_token_mock_12345") { success, msg ->
                                    isSuccessFeedback = success
                                    feedbackMessage = msg
                                }
                            }
                            .testTag("auth_social_google")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color(0xFFEA4335),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Facebook Sign In Button
                    Surface(
                        color = Color(0xFF1877F2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                feedbackMessage = null
                                // Pass a simulated client Facebook token, or use high fidelity callback
                                viewModel.signInWithSocialToken("Facebook", "facebook_token_mock_12345") { success, msg ->
                                    isSuccessFeedback = success
                                    feedbackMessage = msg
                                }
                            }
                            .testTag("auth_social_facebook")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "f",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Facebook",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Helpful Evaluation Hint Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💡 QUICK TEST CREDENTIALS",
                        color = OrangeAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Email: user@example.com",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Password: password123",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Phone Demo: Use any number + verification code: 123456",
                        color = TextWhite,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

