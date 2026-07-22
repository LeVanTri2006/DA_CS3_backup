package com.example.da_cuoiky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.da_cuoiky.fiebase.AuthViewModel
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.navigation.Screen
import com.example.da_cuoiky.ui.screens.*
import com.example.da_cuoiky.ui.theme.DA_CuoiKyTheme

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DA_CuoiKyTheme {
                RestaurantApp()
            }
        }
    }

    override fun onNewIntent(newIntent: android.content.Intent) {
        super.onNewIntent(newIntent)
        intent = newIntent
    }
}

@Composable
fun RestaurantApp() {
    val authViewModel: AuthViewModel = viewModel()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("restaurant_prefs", android.content.Context.MODE_PRIVATE) }
    val gson = remember { com.google.gson.Gson() }

    var cartItems by remember {
        val savedCart = sharedPrefs.getString("cart_items", null)
        val initialList = if (savedCart != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<OrderItem>>() {}.type
            try {
                gson.fromJson<List<OrderItem>>(savedCart, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        mutableStateOf(initialList)
    }

    LaunchedEffect(cartItems) {
        sharedPrefs.edit().putString("cart_items", gson.toJson(cartItems)).apply()
    }

    var lastOrderId by remember { mutableStateOf<String?>(null) }
    var deliveryType by remember { mutableStateOf(DeliveryType.PICKUP) }
    var deliveryAddress by remember { mutableStateOf("") }
    var floorPlanRefreshKey by remember { mutableIntStateOf(0) }  // ✅ Refresh key for FloorPlan
    
    var customerDeliveryInfo by remember {
        val savedInfo = sharedPrefs.getString("delivery_info", null)
        val initialInfo = if (savedInfo != null) {
            try {
                gson.fromJson(savedInfo, DeliveryInfo::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        mutableStateOf(initialInfo)
    }

    LaunchedEffect(customerDeliveryInfo) {
        if (customerDeliveryInfo == null) {
            sharedPrefs.edit().remove("delivery_info").apply()
        } else {
            sharedPrefs.edit().putString("delivery_info", gson.toJson(customerDeliveryInfo)).apply()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.CustomerMain.route
    ) {
        // ── AUTH ──
        composable(Screen.Login.route) {
            LoginScreen(
                navController        = navController,
                viewModel            = authViewModel,
                onRoleSelected       = { },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // ── STAFF ──
        composable(Screen.StaffFloorPlan.route) {
            FloorPlanScreen(
                onTableClick = { table ->
                    val activeOrderId = table.activeOrderId
                    if (!activeOrderId.isNullOrEmpty()) {
                        // Nếu có mã hóa đơn, đi tới trang thanh toán
                        navController.navigate(Screen.StaffPOS.buildRoute(activeOrderId))
                    } else {
                        // Nếu chưa có (hoặc bàn trống), đi tới trang chọn món
                        navController.navigate(Screen.StaffOrder.buildRoute(table.id))
                    }
                },
                onNavigateToOrder = { tableId -> navController.navigate(Screen.StaffOrder.buildRoute(tableId)) },
                refreshKey = floorPlanRefreshKey
            )
        }

        composable(Screen.StaffOrder.route) { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId") ?: "T01"
            OrderScreen(
                tableId = tableId,
                tableName = "Bàn",
                onSendToKitchen = { 
                    // ✅ Increment refresh key to reload FloorPlan
                    floorPlanRefreshKey++
                    navController.popBackStack() 
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StaffKitchen.route) {
            KitchenScreen(
                onBack = { 
                    // ✅ Increment refresh key to reload FloorPlan
                    floorPlanRefreshKey++
                    navController.popBackStack() 
                }
            )
        }

        composable(Screen.StaffPOS.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            POSScreen(
                orderId = orderId,
                onPaymentComplete = { _ ->
                    // ✅ Increment refresh key to reload FloorPlan
                    floorPlanRefreshKey++
                    navController.navigate(Screen.StaffFloorPlan.route) { popUpTo(Screen.StaffFloorPlan.route) { inclusive = false } }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── CUSTOMER ──
        composable(Screen.CustomerMain.route) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab")
            CustomerMainScreen(
                navController  = navController,
                authViewModel  = authViewModel,
                initialTab     = CustomerTab.HOME,
                cartItems      = cartItems,
                onAddToCart    = { item -> 
                    val idx = cartItems.indexOfFirst { it.menuItemId == item.menuItemId }
                    if (idx >= 0) {
                        val newList = cartItems.toMutableList()
                        newList[idx] = newList[idx].copy(qty = newList[idx].qty + item.qty)
                        cartItems = newList
                    } else {
                        cartItems = cartItems + item 
                    }
                },
                lastOrderId    = lastOrderId,
                targetTab      = tab
            )
        }

        composable(Screen.ProductDetail.route) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: "M01"
            ProductDetailScreen(
                itemId = itemId,
                onAddToCart = { item -> 
                    val idx = cartItems.indexOfFirst { it.menuItemId == item.menuItemId }
                    if (idx >= 0) {
                        val newList = cartItems.toMutableList()
                        newList[idx] = newList[idx].copy(qty = newList[idx].qty + item.qty)
                        cartItems = newList
                    } else {
                        cartItems = cartItems + item 
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerCart.route) {
            CartScreen(
                cartItems = cartItems,
                authViewModel = authViewModel,
                deliveryType = deliveryType,
                onDeliveryTypeChange = { deliveryType = it },
                deliveryAddress = deliveryAddress,
                onAddressChange = { deliveryAddress = it },
                onQtyChange = { item, newQty ->
                    cartItems = if (newQty > 0) {
                        cartItems.map { if (it.menuItemId == item.menuItemId) it.copy(qty = newQty) else it }
                    } else {
                        cartItems.filter { it.menuItemId != item.menuItemId }
                    }
                },
                onCheckout = { navController.navigate(Screen.ConfirmOrder.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onBack = { navController.popBackStack() }
            )
        }

        //  Confirm Order
        composable(Screen.ConfirmOrder.route) {
            ConfirmOrderScreen(
                cartItems = cartItems,
                deliveryType = deliveryType,
                onContinue = { navController.navigate(Screen.DeliveryInfo.route) },
                onBack = { navController.popBackStack() }
            )
        }

        //  Delivery Info
        composable(Screen.DeliveryInfo.route) {
            val profileState by authViewModel.profileState.collectAsState()
            val profile = (profileState as? com.example.da_cuoiky.fiebase.ProfileUiState.Success)?.profile
            
            LaunchedEffect(Unit) {
                if (profileState is com.example.da_cuoiky.fiebase.ProfileUiState.Loading) {
                    authViewModel.loadUserProfile()
                }
            }
            
            DeliveryInfoScreen(
                initialName = profile?.fullName ?: "",
                initialPhone = profile?.phone ?: "",
                initialAddress = deliveryAddress,
                isPickup = deliveryType == DeliveryType.PICKUP,
                onContinue = { deliveryInfo ->

                    customerDeliveryInfo = deliveryInfo

                    navController.navigate(Screen.CustomerPayment.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        //  Payment
        composable(Screen.CustomerPayment.route) {
            val deliveryFee = if (deliveryType == com.example.da_cuoiky.model.DeliveryType.PICKUP) 0 else 15000
            val totalAmount = cartItems.sumOf { it.totalPrice } + deliveryFee
            CustomerPaymentScreen(
                totalAmount = totalAmount,
                deliveryInfo = customerDeliveryInfo ?: DeliveryInfo(),
                cartItems = cartItems,
                onPaymentComplete = { completedOrderId ->
                    lastOrderId = completedOrderId
                    cartItems = emptyList()
                    customerDeliveryInfo = null
                    navController.navigate(Screen.OrderTracking.buildRoute(completedOrderId)) {
                        popUpTo(Screen.CustomerMain.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                navigateToTracking = { orderId ->
                    lastOrderId = orderId
                    cartItems = emptyList()
                    customerDeliveryInfo = null
                    navController.navigate(Screen.OrderTracking.buildRoute(orderId)) {
                        popUpTo(Screen.CustomerMain.route) { inclusive = true }
                    }
                }
            )
        }

        // Old Customer Checkout (deprecated, kept for compatibility)
        composable(Screen.CustomerCheckout.route) {
            CustomerCheckoutScreen(
                cartItems = cartItems,
                authViewModel = authViewModel,
                deliveryType = deliveryType,
                deliveryAddress = deliveryAddress,
                onConfirm = { realOrderId ->
                    lastOrderId = realOrderId
                    cartItems = emptyList()
                    deliveryAddress = "" // Reset
                    navController.navigate(Screen.OrderTracking.buildRoute(realOrderId)) {
                        popUpTo(Screen.CustomerMain.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OrderTracking.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderTrackingScreen(
                orderId = orderId,
                onBack = {
                    navController.navigate(Screen.CustomerMain.buildRoute("orders")) {
                        popUpTo(Screen.CustomerMain.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CustomerBooking.route) {
            BookingScreen(
                authViewModel = authViewModel,
                onConfirm = { _ -> navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiAssistant.route) {
            AiAssistantScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
