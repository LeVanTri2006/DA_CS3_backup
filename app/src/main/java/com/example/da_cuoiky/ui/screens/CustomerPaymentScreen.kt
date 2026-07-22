package com.example.da_cuoiky.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import kotlinx.coroutines.launch

// Payment Constants
object PaymentConstants {
    const val BANK_NAME = "TP BANK"
    const val BANK_CODE = "TPBANK"
    const val ACCOUNT_NUMBER = "0775109883"
    const val ACCOUNT_NAME = "LE VAN TRI"
}

enum class CustomerPaymentMethod { COD, QR, PAYPAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentScreen(
    totalAmount: Int,
    deliveryInfo: DeliveryInfo,
    cartItems: List<OrderItem>,
    onPaymentComplete: (String) -> Unit,
    onBack: () -> Unit,
    navigateToTracking: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf<CustomerPaymentMethod?>(null) }
    var isOnlineExpanded by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var paymentSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentPayPalOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var showQRModal by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    
    val scope = rememberCoroutineScope()
    val activity = context as? androidx.fragment.app.FragmentActivity
    
    val payPalClient = remember(activity) {
        if (activity != null) {
            val config = com.paypal.android.corepayments.CoreConfig(
                "AbXnimJcBjtYtH5Rq87kb6J5lraw9aGCvXH-AmXrCyK4-wWyF4qMzj_Ap-c0JVh4PTss4_8T8OBKg0ix",
                environment = com.paypal.android.corepayments.Environment.LIVE
            )
            val client = com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient(
                activity, config, "dacuoikypaypal"
            )
            client.listener = object : com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener {
                override fun onPayPalWebSuccess(result: com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult) {
                    scope.launch {
                        isProcessing = true
                        try {
                            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                            val request = HashMap<String, Any>()
                            request["paypal_order_id"] = result.orderId ?: ""
                            request["id_hoa_don"] = currentOrderId ?: ""
                            val response = apiService.capturePayPalOrder(request)
                            if (response.isSuccessful && response.body()?.get("status") == "success") {
                                paymentSuccess = true
                            } else {
                                errorMessage = "Lỗi capture: ${response.body()?.get("message")}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Lỗi mạng: ${e.message}"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
                override fun onPayPalWebFailure(error: com.paypal.android.corepayments.PayPalSDKError) {
                    errorMessage = "PayPal Error: ${error.errorDescription}"
                    isProcessing = false
                }
                override fun onPayPalWebCanceled() {
                    // Cố gắng capture trong trường hợp trình duyệt trả về Cancel (do mất State)
                    // nhưng khách hàng thực ra đã ấn Approve trên Web!
                    val localPId = currentPayPalOrderId
                    val localOId = currentOrderId
                    if (localPId != null && localOId != null) {
                        scope.launch {
                            isProcessing = true
                            try {
                                val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                                val request = HashMap<String, Any>()
                                request["paypal_order_id"] = localPId
                                request["id_hoa_don"] = localOId
                                val response = apiService.capturePayPalOrder(request)
                                if (response.isSuccessful && response.body()?.get("status") == "success") {
                                    paymentSuccess = true
                                    errorMessage = null
                                } else {
                                    errorMessage = "Đã hủy thanh toán PayPal"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Đã hủy thanh toán PayPal"
                            } finally {
                                isProcessing = false
                            }
                        }
                    } else {
                        errorMessage = "Đã hủy thanh toán PayPal"
                        isProcessing = false
                    }
                }
            }
            client
        } else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh Toán", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (paymentSuccess) {
                // Success Dialog
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Thanh Toán Thành Công!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Mã đơn: ${currentOrderId}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { onPaymentComplete(currentOrderId ?: "") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Xem Đơn Hàng", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Order Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tóm Tắt Đơn Hàng", fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            InfoRow("Mã đơn", "Đang tạo...")
                            InfoRow("Tổng tiền", "%,d ₫".format(totalAmount))
                            InfoRow("Người nhận", deliveryInfo.fullName)
                            InfoRow("SĐT", deliveryInfo.phone)
                            InfoRow("Địa chỉ", deliveryInfo.address)
                        }
                    }

                    // Payment Method Selection
                    Text("Chọn Phương Thức Thanh Toán", fontWeight = FontWeight.Bold)
                    
                    PaymentMethodCard(
                        title = "Thanh Toán Khi Nhận Hàng (COD)",
                        description = "Thanh toán tiền mặt khi nhận đơn hàng",
                        icon = Icons.Default.CheckCircle,
                        isSelected = selectedMethod == CustomerPaymentMethod.COD,
                        onClick = { 
                            selectedMethod = CustomerPaymentMethod.COD 
                            isOnlineExpanded = false
                        }
                    )
                    
                    PaymentMethodCard(
                        title = "Thanh Toán Online",
                        description = "Chuyển khoản QR hoặc thanh toán PayPal",
                        icon = Icons.Default.CheckCircle, // Main icon
                        isSelected = isOnlineExpanded || selectedMethod == CustomerPaymentMethod.QR || selectedMethod == CustomerPaymentMethod.PAYPAL,
                        onClick = { 
                            isOnlineExpanded = true 
                            if (selectedMethod == CustomerPaymentMethod.COD) selectedMethod = null
                        }
                    )

                    if (isOnlineExpanded) {
                        Column(modifier = Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PaymentMethodCard(
                                title = "VietQR",
                                description = "Mở app ngân hàng quét mã",
                                imageUrl = "https://img.vietqr.io/image/vietqr.png",
                                isSelected = selectedMethod == CustomerPaymentMethod.QR,
                                onClick = { selectedMethod = CustomerPaymentMethod.QR }
                            )
                            
                            PaymentMethodCard(
                                title = "PayPal",
                                description = "Thanh toán bằng thẻ quốc tế",
                                imageUrl = "https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_111x69.jpg",
                                isSelected = selectedMethod == CustomerPaymentMethod.PAYPAL,
                                onClick = { selectedMethod = CustomerPaymentMethod.PAYPAL }
                            )
                        }
                    }


                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Confirm Button
                Button(
                    onClick = {
                        selectedMethod?.let { method ->
                            when (method) {
                                CustomerPaymentMethod.COD -> {
                                    isProcessing = true
                                    scope.launch {
                                        try {
                                            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                                            val userId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
                                            val request = mutableMapOf<String, String>(
                                                "phuong_thuc_thanh_toan" to "tien_mat",
                                                "ho_ten" to deliveryInfo.fullName,
                                                "so_dien_thoai" to deliveryInfo.phone,
                                                "dia_chi" to deliveryInfo.address,
                                                "ghi_chu" to deliveryInfo.note,
                                                "tong_tien" to totalAmount.toString()
                                            )
                                            if (userId != null) {
                                                request["ma_nguoi_dung"] = userId
                                            }
                                            val itemsJson = cartItems.map {
                                                """{"ma_mon_an":"${it.menuItemId}","so_luong":${it.qty},"don_gia":${it.price}}"""
                                            }.joinToString(",")
                                            request["items"] = "[$itemsJson]"

                                            Log.d("PAYMENT_DEBUG", "COD Request: $request")

                                            val response = apiService.createCustomerOrder(request)

                                            if (response.isSuccessful && response.body()?.status == "success") {
                                                // Notify kitchen via Firebase Firestore
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    .collection("kitchen_updates").document("latest")
                                                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                                                    .addOnSuccessListener { Log.d("PAYMENT_DEBUG", "Kitchen notified") }
                                                    
                                                val responseBody = response.body()
                                                val serverOrderId = responseBody?.data?.get("id_hoa_don")?.toString()
                                                if (serverOrderId != null && serverOrderId.isNotEmpty()) {
                                                    paymentSuccess = true
                                                    currentOrderId = serverOrderId
                                                } else {
                                                    currentOrderId = "ORD" + System.currentTimeMillis()
                                                    paymentSuccess = true
                                                }
                                            } else {
                                                errorMessage = response.body()?.message ?: "Thanh toán thất bại"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Lỗi mạng: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                                CustomerPaymentMethod.QR -> {
                                    isProcessing = true
                                    scope.launch {
                                        try {
                                            if (currentOrderId != null) {
                                                showQRModal = true
                                                isProcessing = false
                                                return@launch
                                            }
                                            
                                            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                                            val userId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
                                            val request = mutableMapOf<String, String>(
                                                "phuong_thuc_thanh_toan" to "qr",
                                                "ho_ten" to deliveryInfo.fullName,
                                                "so_dien_thoai" to deliveryInfo.phone,
                                                "dia_chi" to deliveryInfo.address,
                                                "ghi_chu" to deliveryInfo.note,
                                                "tong_tien" to totalAmount.toString()
                                            )
                                            if (userId != null) {
                                                request["ma_nguoi_dung"] = userId
                                            }
                                            val itemsJson = cartItems.map {
                                                """{"ma_mon_an":"${it.menuItemId}","so_luong":${it.qty},"don_gia":${it.price}}"""
                                            }.joinToString(",")
                                            request["items"] = "[$itemsJson]"

                                            val response = apiService.createCustomerOrder(request)

                                            if (response.isSuccessful && response.body()?.status == "success") {
                                                // Notify kitchen via Firebase Firestore
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    .collection("kitchen_updates").document("latest")
                                                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                                                    
                                                val responseBody = response.body()
                                                val serverOrderId = responseBody?.data?.get("id_hoa_don")?.toString()
                                                if (serverOrderId != null && serverOrderId.isNotEmpty()) {
                                                    currentOrderId = serverOrderId
                                                    showQRModal = true
                                                } else {
                                                    errorMessage = "Lỗi không nhận được mã đơn"
                                                }
                                            } else {
                                                errorMessage = response.body()?.message ?: "Khởi tạo đơn thất bại"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Lỗi mạng: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                                CustomerPaymentMethod.PAYPAL -> {
                                    isProcessing = true
                                    scope.launch {
                                        try {
                                            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                                            val userId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null }
                                            val request = mutableMapOf<String, String>(
                                                "phuong_thuc_thanh_toan" to "paypal",
                                                "ho_ten" to deliveryInfo.fullName,
                                                "so_dien_thoai" to deliveryInfo.phone,
                                                "dia_chi" to deliveryInfo.address,
                                                "ghi_chu" to deliveryInfo.note,
                                                "tong_tien" to totalAmount.toString()
                                            )
                                            if (userId != null) {
                                                request["ma_nguoi_dung"] = userId
                                            }
                                            val itemsJson = cartItems.map {
                                                """{"ma_mon_an":"${it.menuItemId}","so_luong":${it.qty},"don_gia":${it.price}}"""
                                            }.joinToString(",")
                                            request["items"] = "[$itemsJson]"

                                            if (currentOrderId == null) {
                                                if (cartItems.isEmpty()) {
                                                    errorMessage = "Giỏ hàng trống hoặc đã hết hạn, vui lòng quay lại giỏ hàng."
                                                    isProcessing = false
                                                    return@launch
                                                }
                                                // 1. Tạo đơn hàng vào DB lần đầu
                                                val response = apiService.createCustomerOrder(request)
                                                if (response.isSuccessful && response.body()?.status == "success") {
                                                    currentOrderId = response.body()?.data?.get("id_hoa_don")?.toString() ?: ""
                                                } else {
                                                    errorMessage = response.body()?.message ?: "Khởi tạo đơn thất bại"
                                                    isProcessing = false
                                                    return@launch
                                                }
                                            }

                                            // Đã có currentOrderId, tiếp tục tạo/mở lại PayPal Order
                                            val serverOrderId = currentOrderId ?: ""
                                            val paypalReq = java.util.HashMap<String, Any>()
                                            paypalReq["id_hoa_don"] = serverOrderId
                                            paypalReq["tong_tien"] = totalAmount
                                            val paypalRes = apiService.createPayPalOrder(paypalReq)
                                            if (paypalRes.isSuccessful && paypalRes.body()?.get("status") == "success") {
                                                val paypalOrderIdStr = paypalRes.body()?.get("paypal_order_id")?.toString() ?: ""
                                                currentPayPalOrderId = paypalOrderIdStr
                                                
                                                // 3. Khởi động Web Checkout
                                                val payPalReq = com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest(paypalOrderIdStr)
                                                payPalClient?.start(payPalReq)
                                            } else {
                                                errorMessage = "Khởi tạo PayPal thất bại"
                                                isProcessing = false
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Lỗi mạng: ${e.message}"
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = selectedMethod != null && !isProcessing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (selectedMethod == CustomerPaymentMethod.QR) "Xác Nhận Chuyển Khoản" else "Xác Nhận Đặt Hàng",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // QR Payment ModalBottomSheet
    val qrOrderId = currentOrderId
    if (showQRModal && qrOrderId != null) {
        QRPaymentModalBottomSheet(
            totalAmount = totalAmount,
            orderId = qrOrderId,
            onDismiss = { showQRModal = false },
            onConfirmPayment = { orderId ->
                showQRModal = false
                navigateToTracking(orderId)
            }
        )
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    imageUrl: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (icon != null) {
                    Icon(
                        icon,
                        null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.outline)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRPaymentModalBottomSheet(
    totalAmount: Int,
    orderId: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (String) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }

    // Tự động lắng nghe Webhook từ Firebase
    DisposableEffect(orderId) {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("PaymentStatus")
            .document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("PAYMENT_DEBUG", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "da_thanh_toan") {
                        // Nhận được webhook -> Tự động chuyển trang ngay lập tức
                        onConfirmPayment(orderId)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    // SePay QR URL Construction
    val qrUrl = remember(totalAmount, orderId) {
        "https://qr.sepay.vn/img" +
                "?bank=${PaymentConstants.BANK_CODE}" +
                "&acc=${PaymentConstants.ACCOUNT_NUMBER}" +
                "&amount=$totalAmount" +
                "&des=$orderId"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Thanh Toán QR",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // QR Code Display using Coil AsyncImage
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = qrUrl,
                        contentDescription = "VietQR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        PaymentConstants.BANK_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        "STK: ${PaymentConstants.ACCOUNT_NUMBER}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "CHỦ TK: ${PaymentConstants.ACCOUNT_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                    
                    Text(
                        "Số tiền: %,d ₫".format(totalAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Nội dung: $orderId",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hiển thị trạng thái đang chờ thanh toán
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Đang chờ xác nhận thanh toán...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
