package com.example.da_cuoiky.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.da_cuoiky.model.KitchenOrder

@Composable
fun KitchenTicket(ticket: KitchenOrder, onStart: () -> Unit, onDone: () -> Unit) {
    // Dynamic Time Calculation
    val targetTimeMs = ticket.targetTimeMinutes * 60 * 1000L
    val elapsedMs = System.currentTimeMillis() - ticket.createdAt
    val isOverdue = elapsedMs > targetTimeMs

    val elapsedSecs = (elapsedMs / 1000).coerceAtLeast(0)
    val eMins = elapsedSecs / 60
    val eSecs = elapsedSecs % 60
    val timeElapsedStr = String.format("%02d:%02d", eMins, eSecs)

    val outlineBorder = if (isOverdue) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else BorderStroke(0.dp, Color.Transparent)

    Card(
        modifier = Modifier
            .width(320.dp)
            .padding(8.dp),
        border = outlineBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bàn: ${ticket.tableName}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Surface(
                    color = if (isOverdue) MaterialTheme.colorScheme.error else Color.DarkGray,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$timeElapsedStr / ${ticket.targetTimeMinutes}:00",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            ticket.items.forEach { item ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "[${item.qty}]",
                        modifier = Modifier.width(32.dp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (item.note.isNotBlank()) {
                            Text(
                                text = "Note: ${item.note}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (ticket.status == "PENDING") {
                    OutlinedButton(onClick = onStart, modifier = Modifier.padding(end = 8.dp)) {
                        Text("BẮT ĐẦU NẤU")
                    }
                } else if (ticket.status == "PREPARING") {
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HOÀN THÀNH")
                    }
                }
            }
        }
    }
}
