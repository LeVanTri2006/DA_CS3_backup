package com.example.da_cuoiky.navigation

// ─────────────────────────────────
// Route Definitions
// ─────────────────────────────────

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")

    // Staff Flow
    object StaffMain : Screen("staff/main")
    object KitchenMain : Screen("kitchen/main")
    
    object StaffFloorPlan : Screen("staff/floor_plan")
    object StaffOrder : Screen("staff/order/{tableId}") {
        fun buildRoute(tableId: String) = "staff/order/$tableId"
    }
    object StaffKitchen : Screen("staff/kitchen")
    object StaffPOS : Screen("staff/pos/{orderId}") {
        fun buildRoute(orderId: String) = "staff/pos/$orderId"
    }
    object StaffProfile : Screen("staff/profile")

    // Customer Flow
    object CustomerHome    : Screen("customer/home")
    object CustomerMain    : Screen("customer/main?tab={tab}") {
        fun buildRoute(tab: String = "home") = "customer/main?tab=$tab"
    }
    object CustomerMenu    : Screen("customer/menu")
    object ProductDetail : Screen("customer/product/{itemId}") {
        fun buildRoute(itemId: String) = "customer/product/$itemId"
    }
    object CustomerCart : Screen("customer/cart")
    object ConfirmOrder : Screen("customer/confirm")
    object DeliveryInfo : Screen("customer/delivery")
    object CustomerPayment : Screen("customer/payment/{orderId}") {
        fun buildRoute(orderId: String) = "customer/payment/$orderId"
    }
    object CustomerCheckout : Screen("customer/checkout")
    object CustomerBooking : Screen("customer/booking")
    object OrderTracking : Screen("customer/tracking/{orderId}") {
        fun buildRoute(orderId: String) = "customer/tracking/$orderId"
    }
    object CustomerProfile : Screen("customer/profile")
    object AiAssistant : Screen("customer/ai_assistant")
}
