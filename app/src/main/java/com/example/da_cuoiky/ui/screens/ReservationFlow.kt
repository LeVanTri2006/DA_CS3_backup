package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReservationFlow(onConfirm: (String, Int) -> Unit) {
    var pax by remember { mutableIntStateOf(2) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Khởi tạo Lịch Đặt bàn", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Ngày & Giờ Nhận bàn", style = MaterialTheme.typography.labelLarge)
        val selectedDate = "18:00, 20/12/2026"
        OutlinedTextField(
            value = selectedDate,
            onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.CalendarToday, null) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Số lượng khách dự kiến", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = { if (pax > 1) pax-- }) { Icon(Icons.Default.RemoveCircleOutline, "Giảm") }
            Text(
                text = pax.toString(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            IconButton(onClick = { pax++ }) { Icon(Icons.Default.AddCircleOutline, "Tăng") }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { onConfirm(selectedDate, pax) },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Xác nhận & Chuyển tới Đặt Tiền Cọc (Deposit)")
        }
    }
}
