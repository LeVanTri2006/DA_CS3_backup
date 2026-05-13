package com.example.da_cuoiky.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*
import com.example.da_cuoiky.fiebase.AuthViewModel
import com.example.da_cuoiky.fiebase.ProfileUiState
import com.google.firebase.auth.FirebaseAuth
import com.example.da_cuoiky.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────
// CART SCREEN
// ─────────────────────────────────

@Composable
fun CartScreen(
    cartItems: List<OrderItem>,
    authViewModel: AuthViewModel,
    deliveryType: DeliveryType,
    onDeliveryTypeChange: (DeliveryType) -> Unit,
    deliveryAddress: String,
    onAddressChange: (String) -> Unit,
    onQtyChange: (OrderItem, Int) -> Unit,
    onCheckout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var couponCode by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf(0) }
    val total = cartItems.sumOf { it.totalPrice }
    val finalTotal = (total - discountAmount).coerceAtLeast(0)

    Scaffold(
        topBar = { CustomerTopBar(title = "Giỏ Hàng (${cartItems.size})", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Delivery Type ──────────────────────────────────────────
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Hình thức nhận", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DeliveryType.entries.filter { it != DeliveryType.DINE_IN }.forEach { type ->
                                    FilterChip(
                                        selected = deliveryType == type,
                                        onClick = { onDeliveryTypeChange(type) },
                                        label = { Text(type.displayName) }
                                    )
                                }
                            }

                            if (deliveryType == DeliveryType.DELIVERY) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Địa chỉ giao hàng", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = deliveryAddress,
                                    onValueChange = onAddressChange,
                                    placeholder = { Text("Nhập địa chỉ nhận hàng chi tiết") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = PrimaryColor) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                item { Text("Món đã chọn", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }

                // ── Cart Items ─────────────────────────────────────────────
                items(cartItems, key = { it.menuItemId }) { item ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                Text("%,d ₫".format(item.price), color = PrimaryColor)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onQtyChange(item, item.qty - 1) }) { Icon(Icons.Default.Remove, null) }
                                Text("${item.qty}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onQtyChange(item, item.qty + 1) }) { Icon(Icons.Default.Add, null, tint = PrimaryColor) }
                            }
                        }
                    }
                }

                // ── Coupon Code ────────────────────────────────────────────
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mã giảm giá", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = couponCode,
                                    onValueChange = { couponCode = it },
                                    placeholder = { Text("Nhập mã (VD: GOURMET20)", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = { Icon(Icons.Default.Redeem, null, tint = PrimaryColor) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        if (couponCode.uppercase() == "GOURMET20") {
                                            discountAmount = (total * 0.2).toInt()
                                            Toast.makeText(context, "Đã áp dụng mã giảm giá 20%!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            discountAmount = 0
                                            Toast.makeText(context, "Mã không hợp lệ", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text("Áp dụng")
                                }
                            }
                        }
                    }
                }

                // ── Price Summary ──────────────────────────────────────────
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tạm tính", color = Color.Gray)
                                Text("%,d ₫".format(total), fontWeight = FontWeight.Bold)
                            }
                            if (discountAmount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Giảm giá", color = Color.Gray)
                                    Text("- %,d ₫".format(discountAmount), color = PrimaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tổng cộng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("%,d ₫".format(finalTotal), color = PrimaryColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Surface(shadowElevation = 12.dp) {
                Button(
                    onClick = {
                        if (deliveryType == DeliveryType.DELIVERY && deliveryAddress.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (authViewModel.isLoggedIn()) onCheckout() else onNavigateToLogin()
                    },
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Icon(Icons.Default.CreditCard, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tiến hành thanh toán — %,d ₫".format(finalTotal), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────
// CUSTOMER CHECKOUT SCREEN
// ─────────────────────────────────

@Composable
fun CustomerCheckoutScreen(
    cartItems: List<OrderItem>,
    authViewModel: AuthViewModel,
    deliveryType: DeliveryType,
    deliveryAddress: String,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSubmitting by remember { mutableStateOf(false) }
    var confirmedOrderId by remember { mutableStateOf<String?>(null) }
    val total = cartItems.sumOf { it.totalPrice }

    // Lấy thông tin người dùng từ StateFlow của ViewModel
    val profileState by authViewModel.profileState.collectAsState()
    val userProfile = (profileState as? ProfileUiState.Success)?.profile

    // Đảm bảo thông tin profile được nạp
    LaunchedEffect(Unit) {
        if (profileState is ProfileUiState.Loading) {
            authViewModel.loadUserProfile()
        }
    }

    if (confirmedOrderId != null) {
        OrderConfirmedOverlay(orderId = confirmedOrderId!!, onDone = { onConfirm(confirmedOrderId!!) })
        return
    }

    Scaffold(topBar = { CustomerTopBar(title = "Xác Nhận Thanh Toán", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Tóm tắt đơn hàng", fontWeight = FontWeight.Bold) }
                    items(cartItems) { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.qty}x ${item.name}", style = MaterialTheme.typography.bodySmall)
                            Text("%,d ₫".format(item.totalPrice), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng cộng", fontWeight = FontWeight.Bold)
                            Text("%,d ₫".format(total), color = PrimaryColor, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (userProfile != null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("Thông tin khách hàng", fontWeight = FontWeight.Bold)
                            Text("Họ tên: ${userProfile.fullName}", style = MaterialTheme.typography.bodySmall)
                            Text("SĐT: ${userProfile.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isSubmitting) return@Button

                    val userId = try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
                    if (userId == null) {
                        Toast.makeText(context, "Vui lòng đăng nhập lại để tiếp tục", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            // Bổ sung đầy đủ thông tin để tránh lỗi Server PHP "Thiếu thông tin"
                            val request = CreateOrderRequest(
                                userId = userId,
                                tableId = "Online",
                                staffId = "", // Tránh gửi null
                                branchId = "B01",
                                totalPrice = total,
                                deliveryType = "PICKUP",
                                paymentMethod = "Tiền mặt",
                                fullName = userProfile?.fullName ?: "Khách hàng",
                                phone = userProfile?.phone ?: "",
                                items = cartItems.map { item ->
                                    OrderItemRequest(
                                        menuItemId = item.menuItemId,
                                        name = item.name,
                                        qty = item.qty,
                                        price = item.price,
                                        note = if (item.note.isBlank()) "Đơn hàng Online" else item.note
                                    )
                                }
                            )

                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.instance.createOrder(request)
                            }

                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body?.status == "success" && !body.orderId.isNullOrEmpty()) {
                                    // Notify kitchen via Firebase Firestore
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("kitchen_updates").document("latest")
                                        .set(mapOf("timestamp" to System.currentTimeMillis()))
                                        .addOnSuccessListener {
                                            android.util.Log.d("CHECKOUT_DEBUG", "Kitchen notified via Firebase")
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.w("CHECKOUT_DEBUG", "Failed to notify kitchen via Firebase", e)
                                        }
                                        
                                    // Chuyển hướng ngay sang theo dõi đơn hàng như yêu cầu
                                    onConfirm(body.orderId)
                                } else {
                                    val errorMsg = body?.message ?: "Lỗi thiếu thông tin đơn hàng"
                                    Toast.makeText(context, "Thất bại: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Lỗi Server (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Checkout", "Error: ${e.message}")
                            Toast.makeText(context, "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                enabled = !isSubmitting && cartItems.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Xác Nhận Đặt Hàng", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OrderConfirmedOverlay(orderId: String, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SuccessColor, Color(0xFF1B5E20)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(80.dp))
            Text("Đặt Hàng Thành Công!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Mã đơn: #$orderId", color = Color.White.copy(alpha = 0.9f))
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Text("Theo dõi đơn hàng", color = SuccessColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────
// ORDER LIST SCREEN (Tab Đơn hàng)
// ─────────────────────────────────

@Composable
fun OrderListScreen(
    authViewModel: AuthViewModel,
    onOrderClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val userId = remember { try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null } }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.instance.getOrders(userId)
            }
            if (response.isSuccessful) {
                orders = response.body()?.data ?: emptyList()
            } else {
                error = "Không thể tải danh sách đơn hàng"
            }
        } catch (e: Exception) {
            error = "Lỗi kết nối: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(topBar = { CustomerTopBar(title = "Đơn Hàng Của Tôi", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = PrimaryColor)
            } else if (userId == null) {
                Text("Vui lòng đăng nhập để xem đơn hàng", Modifier.align(Alignment.Center), color = Color.Gray)
            } else if (error != null) {
                Text(error!!, Modifier.align(Alignment.Center), color = Color.Red)
            } else if (orders.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, null, Modifier.size(64.dp), Color.LightGray)
                    Text("Chưa có đơn hàng nào", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(orders) { order ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onOrderClick(order.id) }, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Mã đơn: #${order.id}", fontWeight = FontWeight.Bold)
                                    Text(order.createdAt, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text("%,d ₫".format(order.totalPriceFromApi), color = PrimaryColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────
// ORDER TRACKING SCREEN
// ─────────────────────────────────

@Composable
fun OrderTrackingScreen(orderId: String, onBack: () -> Unit) {
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderId) {
        if (orderId.isBlank()) { isLoading = false; errorMessage = "Mã đơn hàng không hợp lệ"; return@LaunchedEffect }
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.instance.getOrderById(orderId)
            }
            if (response.isSuccessful) {
                order = response.body()?.data
                if (order == null) errorMessage = "Không tìm thấy đơn hàng #$orderId"
            } else {
                errorMessage = "Lỗi kết nối máy chủ (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Lỗi: ${e.message}"
        } finally { isLoading = false }
    }

    Scaffold(topBar = { CustomerTopBar(title = "Chi Tiết Đơn Hàng", onBack = onBack) }) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = PrimaryColor)
            } else if (errorMessage != null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), Color.LightGray)
                    Text(errorMessage!!, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Button(onClick = onBack) { Text("Quay lại") }
                }
            } else if (order != null) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 32.sp)
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(order?.status?.displayName ?: "Đang xử lý", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryColor)
                                Text("Cập nhật lúc: ${order?.createdAt}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // Timeline Card
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        order?.let { OrderTimeline(it) }
                    }

                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            item { Text("Danh sách món", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)) }
                            items(order?.items ?: emptyList()) { item ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.qty}x ${item.name}")
                                    Text("%,d ₫".format(item.totalPrice))
                                }
                            }
                            item {
                                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tổng thanh toán", fontWeight = FontWeight.ExtraBold)
                                    Text("%,d ₫".format(order?.totalPriceFromApi ?: 0), color = PrimaryColor, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderTimeline(order: Order) {
    val isPickup = order.deliveryType == "Đến quán lấy" || order.tableName != "Đơn hàng Online"
    
    val status = order.status
    
    // Determine active steps
    val step1Active = true // PENDING or higher
    val step2Active = status == OrderStatus.PREPARING || status == OrderStatus.READY || status == OrderStatus.COMPLETED
    val step3Active = status == OrderStatus.READY || status == OrderStatus.COMPLETED
    val step4Active = status == OrderStatus.COMPLETED
    
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Tiến độ đơn hàng", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        TimelineStep(
            title = "Chờ xác nhận", 
            subtitle = "Đơn hàng đã được hệ thống ghi nhận", 
            isActive = step1Active, 
            isLast = false
        )
        TimelineStep(
            title = "Bếp đang nấu", 
            subtitle = "Món ăn đang được nhà bếp chuẩn bị", 
            isActive = step2Active, 
            isLast = false
        )
        
        if (isPickup) {
            TimelineStep(
                title = "Vui lòng đến lấy", 
                subtitle = "Bếp nấu xong, đang chờ bạn đến lấy", 
                isActive = step3Active, 
                isLast = false
            )
            TimelineStep(
                title = "Đã hoàn thành", 
                subtitle = "Đã lấy món và thanh toán", 
                isActive = step4Active, 
                isLast = true
            )
        } else {
            TimelineStep(
                title = "Đang giao hàng", 
                subtitle = "Chờ shipper lấy và giao đến bạn", 
                isActive = step3Active, 
                isLast = false
            )
            TimelineStep(
                title = "Đã giao thành công", 
                subtitle = "Giao hàng và thanh toán thành công", 
                isActive = step4Active, 
                isLast = true
            )
        }
    }
}

@Composable
fun TimelineStep(title: String, subtitle: String, isActive: Boolean, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PrimaryColor else Color.LightGray)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isActive) PrimaryColor else Color.LightGray)
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp, bottom = if (isLast) 0.dp else 24.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (isActive) Color.Black else Color.Gray)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}