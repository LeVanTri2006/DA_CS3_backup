package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.network.RetrofitClient
import com.example.da_cuoiky.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    orderId: String,
    onPaymentComplete: (PaymentMethod) -> Unit,
    onBack: () -> Unit
) {
    // ── Data States ───────────────────────────────────────────────────────────
    var order by remember { mutableStateOf<Order?>(null) }
    var isOrderLoading by remember { mutableStateOf(true) }
    var orderFetchError by remember { mutableStateOf<String?>(null) }
    
    var showPaymentSheet by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var showReceipt by remember { mutableStateOf(false) }
    var showCashConfirmDialog by remember { mutableStateOf(false) }

    var paymentInfo by remember { mutableStateOf<PaymentApiInfo?>(null) }
    var isLoadingPayment by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var isCheckoutLoading by remember { mutableStateOf(false) }
    var checkoutError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Logic: Fetch Order ───────────────────────────────────────────────────
    LaunchedEffect(orderId) {
        isOrderLoading = true
        orderFetchError = null
        try {
            val response = RetrofitClient.instance.getOrderById(orderId)
            if (response.isSuccessful) {
                order = response.body()?.data
                if (order == null) orderFetchError = "Không tìm thấy hóa đơn #$orderId"
            } else {
                orderFetchError = "Lỗi tải hóa đơn (${response.code()})"
            }
        } catch (e: Exception) {
            orderFetchError = "Lỗi kết nối: ${e.message}"
        } finally {
            isOrderLoading = false
        }
    }

    // ── Calculations ─────────────────────────────────────────────────────────
    val subtotal = order?.subtotal ?: 0
    val taxAmount = (subtotal * 0.08).toInt()
    val grandTotal = subtotal + taxAmount

    // ── Logic: Fetch QR ──────────────────────────────────────────────────────
    fun fetchQR() {
        val currentOrder = order ?: return
        Log.d("QR_DEBUG", "Bắt đầu fetchQR cho Order ID: ${currentOrder.id}")
        scope.launch {
            isLoadingPayment = true
            paymentError = null
            try {
                val response = RetrofitClient.instance.getPaymentInfo(currentOrder.id)
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    if (data != null) {
                        paymentInfo = data
                    } else {
                        paymentError = "Không tìm thấy thông tin thanh toán"
                    }
                } else {
                    paymentError = "Không thể tải thông tin QR"
                }
            } catch (e: Exception) {
                paymentError = "Lỗi kết nối máy chủ"
            } finally {
                isLoadingPayment = false
            }
        }
    }
    
    // ── Tự động lắng nghe Webhook ────────────────────────────────────────────
    val listenerStartTime = remember { System.currentTimeMillis() }
    DisposableEffect(orderId, selectedMethod) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (selectedMethod == PaymentMethod.QR) {
            listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("PaymentStatus")
                .document(orderId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w("POS_DEBUG", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status")
                        val timestampStr = snapshot.getString("timestamp")
                        val timestamp = timestampStr?.toLongOrNull() ?: 0L
                        
                        // Bỏ qua nếu dữ liệu Firebase là dữ liệu cũ (cache) từ các lần test trước (trừ hao lệch giờ 5s)
                        if (status == "da_thanh_toan" && (timestamp == 0L || timestamp > listenerStartTime - 5000)) {
                            Log.d("POS_DEBUG", "Webhook received! Auto confirming.")
                            // Webhook đã xử lý xong, không gọi API confirmCheckout nữa
                            showPaymentSheet = false
                            showReceipt = true
                        }
                    }
                }
        }
        onDispose { listener?.remove() }
    }

    // ── Logic: Confirm Checkout ──────────────────────────────────────────────
    fun confirmCheckout(method: PaymentMethod) {
        val currentOrder = order ?: return
        scope.launch {
            isCheckoutLoading = true
            checkoutError = null
            try {
                val methodStr = if (method == PaymentMethod.QR) "qr" else "tien_mat"
                val request = mapOf(
                    "id_hoa_don" to currentOrder.id,
                    "phuong_thuc_thanh_toan" to methodStr
                )
                Log.d("PAYMENT_DEBUG", "Request: $request")
                
                val response = RetrofitClient.instance.confirmCheckout(request)
                Log.d("PAYMENT_DEBUG", "Response code: ${response.code()}")
                val responseBody = response.body()
                Log.d("PAYMENT_DEBUG", "Response body: $responseBody")
                
                // Vì API có thể trả về chuỗi JSON hoặc chuỗi thuần túy "success"
                val isSuccess = response.isSuccessful && (
                    responseBody?.contains("\"status\":\"success\"") == true || 
                    responseBody?.trim() == "success"
                )

                if (isSuccess) {
                    showPaymentSheet = false
                    showReceipt = true
                } else {
                    // Cố gắng extract message nếu là JSON, nếu không dùng raw body
                    val message = if (responseBody?.startsWith("{") == true) {
                        try {
                            com.google.gson.Gson().fromJson(responseBody, GenericApiResponse::class.java).message
                        } catch (e: Exception) { null }
                    } else null
                    
                    checkoutError = message ?: responseBody ?: "Thanh toán thất bại"
                }
            } catch (e: Exception) {
                Log.e("PAYMENT_DEBUG", "Exception: ${e.message}", e)
                checkoutError = "Lỗi mạng: Không thể xác nhận"
            } finally {
                isCheckoutLoading = false
            }
        }
    }

    if (isOrderLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (orderFetchError != null || order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(orderFetchError ?: "Lỗi không xác định", color = Color.Red)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Quay lại") }
            }
        }
        return
    }

    val currentOrder = order!!

    // ── Dialog: Receipt ──────────────────────────────────────────────────────
    if (showReceipt) {
        ReceiptDialog(
            order = currentOrder,
            method = selectedMethod ?: PaymentMethod.CASH,
            tax = taxAmount, 
            grandTotal = grandTotal,
            onDismiss = {
                showReceipt = false
                onPaymentComplete(selectedMethod ?: PaymentMethod.CASH)
            }
        )
    }

    // ── Dialog: Cash Confirmation ────────────────────────────────────────────
    if (showCashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCashConfirmDialog = false },
            title = { Text("Xác nhận tiền mặt") },
            text = { Text("Bạn đã nhận đủ %,d ₫ từ khách hàng?".format(grandTotal)) },
            confirmButton = {
                Button(
                    onClick = { 
                        showCashConfirmDialog = false
                        confirmCheckout(PaymentMethod.CASH)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                ) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCashConfirmDialog = false
                    selectedMethod = null
                }) { Text("Hủy") }
            }
        )
    }

    // ── Main Layout ──────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Thu Ngân — ${currentOrder.tableName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Đơn hàng #${currentOrder.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                if (currentOrder.paymentStatus == "da_thanh_toan") {
                    Button(
                        onClick = { showReceipt = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Text("ĐƠN HÀNG ĐÃ THANH TOÁN", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                } else {
                    Button(
                        onClick = { showPaymentSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("THANH TOÁN — %,d ₫".format(grandTotal), fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("DANH SÁCH MÓN", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }

            items(currentOrder.items) { item ->
                OrderItemRow(item)
            }

            item {
                Spacer(Modifier.height(8.dp))
                OrderTotalCard(currentOrder.subtotal, taxAmount, grandTotal)
            }
        }
    }

    // ── Bottom Sheet: Payment ────────────────────────────────────────────────
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showPaymentSheet = false 
                selectedMethod = null
                paymentInfo = null
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (selectedMethod == null) "CHỌN PHƯƠNG THỨC" else "THANH TOÁN QR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                if (selectedMethod == null) {
                    // Selection View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PaymentMethodItem(
                            title = "Tiền mặt",
                            icon = Icons.Default.Payments,
                            color = SuccessColor,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                selectedMethod = PaymentMethod.CASH
                                showCashConfirmDialog = true
                            }
                        )
                        PaymentMethodItem(
                            title = "Chuyển khoản",
                            icon = Icons.Default.QrCodeScanner,
                            color = PrimaryColor,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                Log.d("QR_DEBUG", "User chọn phương thức QR")
                                selectedMethod = PaymentMethod.QR
                                fetchQR()
                            }
                        )
                    }
                } else if (selectedMethod == PaymentMethod.QR) {
                    // QR View
                    QRSection(
                        isLoading = isLoadingPayment,
                        error = paymentError,
                        info = paymentInfo,
                        onConfirm = { confirmCheckout(PaymentMethod.QR) },
                        isConfirming = isCheckoutLoading,
                        confirmError = checkoutError
                    )
                }
            }
        }
    }
}

// ── Sub-Components ───────────────────────────────────────────────────────────

@Composable
private fun OrderItemRow(item: OrderItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryColor.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${item.qty}", fontWeight = FontWeight.Bold, color = PrimaryColor)
            }
            Spacer(Modifier.width(12.dp))
            Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text("%,d ₫".format(item.totalPrice), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OrderTotalCard(subtotal: Int, tax: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TotalLine("Tạm tính", "%,d ₫".format(subtotal))
            TotalLine("Thuế (8%)", "%,d ₫".format(tax))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            TotalLine(
                "TỔNG CỘNG", "%,d ₫".format(total),
                isMain = true, color = PrimaryColor
            )
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, isMain: Boolean = false, color: Color = Color.Black) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (isMain) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, fontWeight = if (isMain) FontWeight.Black else FontWeight.Normal)
        Text(value, style = if (isMain) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun PaymentMethodItem(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun QRSection(
    isLoading: Boolean,
    error: String?,
    info: PaymentApiInfo?,
    onConfirm: () -> Unit,
    isConfirming: Boolean,
    confirmError: String?
) {
    // Xóa bỏ logic tự generate Bitmap vì info.qrUrl là link ảnh (không phải chuỗi VietQR)
    // Sẽ dùng AsyncImage để tải ảnh QR trực tiếp từ API.

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(40.dp))
        } else if (error != null) {
            Text(error, color = Color.Red, modifier = Modifier.padding(20.dp))
        } else if (info != null) {
            if (info.qrUrl.isNotBlank()) {
                Log.d("QR_DEBUG", "Đang render UI bằng AsyncImage với URL: ${info.qrUrl}")
                AsyncImage(
                    model = info.qrUrl,
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Tự động fallback sang link mặc định nếu API không trả về link ảnh
                AsyncImage(
                    model = "https://img.vietqr.io/image/TPBANK-0775109883-compact2.png?amount=${info.amount}&addInfo=${info.content}",
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Vui lòng chuyển khoản đúng số tiền:", fontSize = 12.sp, color = Color.Gray)
            Text("%,d ₫".format(info.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = PrimaryColor)
            Spacer(Modifier.height(8.dp))
            Text("Nội dung: ${info.content}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            if (confirmError != null) {
                Text(confirmError, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            // Spinner tự động check
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(color = PrimaryColor, modifier = Modifier.size(24.dp))
                } else {
                    CircularProgressIndicator(color = PrimaryColor, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Hệ thống đang tự động kiểm tra...", color = PrimaryColor, fontWeight = FontWeight.Bold)
                }
            }

            // Nút dự phòng thủ công
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.padding(top = 16.dp),
                enabled = !isConfirming
            ) {
                Text("Xác nhận thủ công (nếu lỗi mạng)", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// (Đã gỡ bỏ hàm generateQRCode do sử dụng AsyncImage)

@Composable
private fun ReceiptDialog(order: Order, method: PaymentMethod, tax: Int, grandTotal: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(64.dp), tint = SuccessColor)
                Spacer(Modifier.height(16.dp))
                Text("THANH TOÁN THÀNH CÔNG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(24.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mã đơn:", color = Color.Gray)
                    Text("#${order.id}", fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Phương thức:", color = Color.Gray)
                    Text(method.displayName, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng cộng:", color = Color.Gray)
                    Text("%,d ₫".format(grandTotal), fontWeight = FontWeight.Bold, color = PrimaryColor)
                }
                
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("HOÀN TẤT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}