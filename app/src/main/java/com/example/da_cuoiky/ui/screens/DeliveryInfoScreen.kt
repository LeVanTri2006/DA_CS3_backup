package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DeliveryInfo(
    val fullName: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryInfoScreen(
    onContinue: (DeliveryInfo) -> Unit,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isPickup by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông Tin Giao Hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Vui lòng nhập thông tin giao hàng",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { 
                        fullName = it
                        fullNameError = if (it.isBlank()) "Không được để trống" else null
                    },
                    label = { Text("Họ tên *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = fullNameError != null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                if (fullNameError != null) {
                    Text(
                        fullNameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        phone = it
                        phoneError = when {
                            it.isBlank() -> "Không được để trống"
                            it.length < 10 -> "Số điện thoại không hợp lệ"
                            else -> null
                        }
                    },
                    label = { Text("Số điện thoại *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = phoneError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp)
                )
                if (phoneError != null) {
                    Text(
                        phoneError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
                
                // Delivery Type Selection
                Text("Hình thức nhận hàng", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            isPickup = false 
                            if (address == "Nhận tại cửa hàng") address = ""
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!isPickup) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isPickup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Giao tận nơi", color = if (!isPickup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = { 
                            isPickup = true 
                            address = "Nhận tại cửa hàng"
                            addressError = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isPickup) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPickup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text("Tự đến lấy", color = if (isPickup) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                // Address
                if (!isPickup) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { 
                            address = it
                            addressError = if (it.isBlank()) "Không được để trống" else null
                        },
                        label = { Text("Địa chỉ nhận hàng *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = addressError != null,
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (addressError != null) {
                        Text(
                            addressError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                
                // Note (optional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú (tùy chọn)") },
                    placeholder = { Text("Ví dụ: Giao trước cửa, gọi số điện thoại...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Button(
                onClick = {
                    // Validate
                    var hasError = false
                    if (fullName.isBlank()) {
                        fullNameError = "Không được để trống"
                        hasError = true
                    }
                    if (phone.isBlank() || phone.length < 10) {
                        phoneError = if (phone.isBlank()) "Không được để trống" else "Số điện thoại không hợp lệ"
                        hasError = true
                    }
                    if (address.isBlank()) {
                        addressError = "Không được để trống"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        onContinue(DeliveryInfo(fullName, phone, address, note))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Tiếp tục thanh toán",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
