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

enum class CustomerPaymentMethod { COD, QR }

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
    var isProcessing by remember { mutableStateOf(false) }
    var paymentSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentOrderId by remember { mutableStateOf<String?>(null) }
    var showQRModal by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

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
                        onClick = { selectedMethod = CustomerPaymentMethod.COD }
                    )
                    
                    PaymentMethodCard(
                        title = "Chuyển Khoản QR",
                        description = "Quét mã QR để thanh toán ngay",
                        icon = Icons.Default.QrCodeScanner,
                        isSelected = selectedMethod == CustomerPaymentMethod.QR,
                        onClick = { selectedMethod = CustomerPaymentMethod.QR }
                    )


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
    if (showQRModal && currentOrderId != null) {
        QRPaymentModalBottomSheet(
            totalAmount = totalAmount,
            orderId = currentOrderId!!,
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
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
