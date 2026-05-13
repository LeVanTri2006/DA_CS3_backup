package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.Order
import com.example.da_cuoiky.model.PaymentApiInfo
import com.example.da_cuoiky.network.RetrofitClient
import com.example.da_cuoiky.ui.components.OrderSummary
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CheckoutScreen(order: Order, onPaymentSubmit: (method: String) -> Unit) {
    var selectedMethod by remember { mutableStateOf("") } // "QR" hoặc "COD"
    var paymentInfo by remember { mutableStateOf<PaymentApiInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Lỗi 2: Generate QR locally thay vì dùng AsyncImage
    LaunchedEffect(paymentInfo) {
        paymentInfo?.let { info ->
            val qrContent = info.qrUrl.ifBlank { info.content }
            if (qrContent.isNotBlank()) {
                qrBitmap = withContext(Dispatchers.IO) {
                    generateQRCode(qrContent)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("Thanh Toán", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Thông tin đơn hàng
        OrderSummary(order = order, onCheckout = {})

        Spacer(modifier = Modifier.height(24.dp))
        Text("Chọn phương thức thanh toán", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // Lựa chọn 1: QR Payment
        PaymentOptionCard(
            title = "QR Payment (Chuyển khoản nhanh)",
            icon = Icons.Default.QrCodeScanner,
            isSelected = selectedMethod == "QR",
            enabled = !isLoading,
            onClick = {
                selectedMethod = "QR"
                // Chỉ gọi API 1 lần duy nhất
                if (paymentInfo == null && !isLoading) {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = RetrofitClient.instance.getPaymentInfo(order.id)
                            if (response.isSuccessful) {
                                paymentInfo = response.body()?.data
                                if (paymentInfo == null) errorMessage = "Không thể tải thông tin thanh toán"
                            } else {
                                errorMessage = "Không thể tải thông tin thanh toán"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Lỗi kết nối: Không thể tải thông tin"
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lựa chọn 2: Thanh toán khi nhận hàng
        PaymentOptionCard(
            title = "Thanh toán khi nhận hàng (COD)",
            icon = Icons.Default.LocalShipping,
            isSelected = selectedMethod == "COD",
            enabled = !isLoading,
            onClick = {
                selectedMethod = "COD"
                errorMessage = null
            }
        )

        // Hiển thị lỗi nếu có
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
        }

        // Hiển thị thông tin QR nếu đã chọn
        if (selectedMethod == "QR") {
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (paymentInfo != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Thông tin chuyển khoản", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Spacer(Modifier.height(12.dp))
                        
                        InfoRow("Ngân hàng", paymentInfo!!.bankName)
                        InfoRow("Chủ TK", paymentInfo!!.accountName)
                        InfoRow("Số TK", paymentInfo!!.accountNumber)
                        InfoRow("Số tiền", "${paymentInfo!!.amount}đ")
                        
                        Spacer(Modifier.height(20.dp))
                        
                        qrBitmap?.let { bmp ->
                            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.LightGray.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Đang tạo mã QR...", color = Color.Gray)
                        }
                        
                        Spacer(Modifier.height(16.dp))

                        // Hiển thị Nội dung chuyển khoản (Yêu cầu 5)
                        Text("Nội dung chuyển khoản:", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = paymentInfo!!.content, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 18.sp, 
                            color = Color(0xFFE65100)
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = { onPaymentSubmit("QR") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tôi đã chuyển khoản", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Nút xác nhận cho COD
        if (selectedMethod == "COD") {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onPaymentSubmit("COD") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Xác nhận đặt hàng", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PaymentOptionCard(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isSelected: Boolean, 
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = if (enabled) onClick else ({}),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        border = androidx.compose.foundation.BorderStroke(
            2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
        ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isSelected) MaterialTheme.colorScheme.primary else if (enabled) Color.Gray else Color.LightGray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                title, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) Color.Unspecified else Color.Gray
            )
            Spacer(modifier = Modifier.weight(1f))
            RadioButton(selected = isSelected, onClick = if (enabled) onClick else null, enabled = enabled)
        }
    }
}

// Hàm bổ trợ tạo QR
private fun generateQRCode(content: String): Bitmap? {
    if (content.isBlank()) return null
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        bmp
    } catch (e: Exception) {
        Log.e("QR_DEBUG", "generateQRCode: Lỗi", e)
        null
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            text = value, 
            fontWeight = FontWeight.Bold, 
            fontSize = 14.sp,
            softWrap = false,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
