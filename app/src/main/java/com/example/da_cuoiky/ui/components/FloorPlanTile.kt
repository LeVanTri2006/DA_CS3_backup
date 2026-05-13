package com.example.da_cuoiky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.da_cuoiky.model.TableModel
import com.example.da_cuoiky.model.TableStatus

@Composable
fun FloorPlanTile(
    table: TableModel,
    onClick: (TableModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (table.status) {
        TableStatus.EMPTY -> Color(0xFFBDBDBD)
        TableStatus.RESERVED -> Color(0xFF1976D2)
        TableStatus.OCCUPIED -> Color(0xFFFF6F00)
        TableStatus.PAID -> Color(0xFF2E7D32)
        TableStatus.LOCKED -> Color(0xFFD32F2F)
        else -> Color.LightGray
    }

    Card(
        modifier = modifier
            .size(120.dp)
            .padding(8.dp)
            .pointerInput(table.status) {
                detectTapGestures(
                    onDoubleTap = { /* Mở order khẩn */ },
                    onLongPress = { /* Kéo thả bàn */ },
                    onTap = { onClick(table) }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = table.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sức chứa tối đa ${table.capacity}",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${table.capacity} pax",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}
