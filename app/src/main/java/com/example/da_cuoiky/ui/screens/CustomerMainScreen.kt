package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.da_cuoiky.fiebase.AuthViewModel
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.navigation.Screen

// ── Tab enum ──────────────────────────────────────────────────────────────────
enum class CustomerTab { HOME, MENU, BOOKING, ORDERS, PROFILE }

// ── Màn hình chính sau khi đăng nhập ─────────────────────────────────────────
@Composable
fun CustomerMainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    initialTab: CustomerTab = CustomerTab.HOME,
    cartItems: List<OrderItem>,
    onAddToCart: (OrderItem) -> Unit,
    lastOrderId: String? = null,
    targetTab: String? = null
) {
    val selectedTabState = remember {
        mutableStateOf(
            when (targetTab) {
                "orders" -> CustomerTab.ORDERS
                "menu" -> CustomerTab.MENU
                "booking" -> CustomerTab.BOOKING
                "profile" -> CustomerTab.PROFILE
                else -> initialTab
            }
        )
    }
    var selectedTab by selectedTabState

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected  = selectedTab == CustomerTab.HOME,
                    onClick   = { selectedTab = CustomerTab.HOME },
                    icon      = { Icon(Icons.Default.Home, "Trang chủ") },
                    label     = { Text("Trang chủ") }
                )
                NavigationBarItem(
                    selected  = selectedTab == CustomerTab.MENU,
                    onClick   = { selectedTab = CustomerTab.MENU },
                    icon      = { Icon(Icons.Default.RestaurantMenu, "Thực đơn") },
                    label     = { Text("Thực đơn") }
                )
                NavigationBarItem(
                    selected  = selectedTab == CustomerTab.BOOKING,
                    onClick   = { selectedTab = CustomerTab.BOOKING },
                    icon      = { Icon(Icons.Default.TableBar, "Đặt bàn") },
                    label     = { Text("Đặt bàn") }
                )
                NavigationBarItem(
                    selected  = selectedTab == CustomerTab.ORDERS,
                    onClick   = { selectedTab = CustomerTab.ORDERS },
                    icon      = { Icon(Icons.Default.ReceiptLong, "Đơn hàng") },
                    label     = { Text("Đơn hàng") }
                )
                NavigationBarItem(
                    selected  = selectedTab == CustomerTab.PROFILE,
                    onClick   = { selectedTab = CustomerTab.PROFILE },
                    icon      = { Icon(Icons.Default.AccountCircle, "Hồ sơ") },
                    label     = { Text("Hồ sơ") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                CustomerTab.HOME -> CustomerHomeScreen(
                    user           = null,
                    onMenuClick    = { selectedTab = CustomerTab.MENU },
                    onBookingClick = { selectedTab = CustomerTab.BOOKING },
                    onOrdersClick  = { selectedTab = CustomerTab.ORDERS },
                    onProfileClick = { selectedTab = CustomerTab.PROFILE },
                    onProductClick = { itemId ->
                        navController.navigate(Screen.ProductDetail.buildRoute(itemId))
                    }
                )

                CustomerTab.MENU -> MenuListScreen(
                    onProductClick = { itemId ->
                        navController.navigate(Screen.ProductDetail.buildRoute(itemId))
                    },
                    onCartClick = {
                        navController.navigate(Screen.CustomerCart.route)
                    },
                    onAddToCart = onAddToCart,
                    cartCount = cartItems.size,
                    onBack = { selectedTab = CustomerTab.HOME }
                )

                CustomerTab.BOOKING -> {
                    if (authViewModel.isLoggedIn()) {
                        BookingScreen(
                            authViewModel = authViewModel,
                            onConfirm     = { _ -> selectedTab = CustomerTab.HOME },
                            onBack        = { selectedTab = CustomerTab.HOME }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Login.route)
                            selectedTab = CustomerTab.HOME 
                        }
                    }
                }

                CustomerTab.ORDERS -> {
                    // ✅ Thay đổi: Hiển thị danh sách đơn hàng thay vì chỉ 1 đơn lẻ
                    OrderListScreen(
                        authViewModel = authViewModel,
                        onOrderClick = { id ->
                            navController.navigate(Screen.OrderTracking.buildRoute(id))
                        },
                        onBack = { selectedTab = CustomerTab.HOME }
                    )
                }

                CustomerTab.PROFILE -> {
                    if (authViewModel.isLoggedIn()) {
                        CustomerProfileScreen(
                            navController     = navController,
                            viewModel         = authViewModel,
                            onBack            = { selectedTab = CustomerTab.HOME },
                            onNavigateToLogin = {
                                navController.navigate(Screen.Login.route)
                            },
                            onNavigateToOrders = {
                                selectedTab = CustomerTab.ORDERS
                            },
                            onLogout = {
                                navController.navigate(Screen.CustomerMain.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        navController.navigate(Screen.Login.route)
                    }
                }
            }
        }
    }
}
