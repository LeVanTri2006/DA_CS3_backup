package com.example.da_cuoiky.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*
import com.example.da_cuoiky.ui.viewmodel.KitchenViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun KitchenScreen(
    onBack: () -> Unit,
    viewModel: KitchenViewModel = viewModel()
) {
    val tickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var filter by remember { mutableStateOf("ALL") } // ALL, PENDING, PREPARING, READY
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Simulate time ticking
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val pendingCount = tickets.count { it.status == "PENDING" }
    val preparingCount = tickets.count { it.status == "PREPARING" }

    val filteredTickets = (when (filter) {
        "PENDING" -> tickets.filter { it.status == "PENDING" }
        "PREPARING" -> tickets.filter { it.status == "PREPARING" }
        else -> tickets
    }).distinctBy { it.orderId } // ✅ Tránh crash nếu trùng mã đơn hàng

    Scaffold(
        topBar = {
            StaffTopBar(
                title = "Bếp KDS",
                subtitle = "Đang chờ: $pendingCount • Đang nấu: $preparingCount"
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Status filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KitchenFilterChip("Tất cả", "ALL", filter, tickets.size) { filter = it }
                KitchenFilterChip("Chờ", "PENDING", filter, pendingCount, Color(0xFFFFA000)) { filter = it }
                KitchenFilterChip("Đang nấu", "PREPARING", filter, preparingCount, Color(0xFF1976D2)) { filter = it }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = Color.Red)
                        Button(onClick = { viewModel.fetchKitchenOrders() }) { Text("Thử lại") }
                    }
                }
            } else if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(64.dp), tint = SuccessColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Không có ticket!", style = MaterialTheme.typography.titleMedium,
                            color = SuccessColor, fontWeight = FontWeight.Bold)
                        Text("Tất cả đơn đã hoàn thành 🎉",
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTickets, key = { it.orderId }) { ticket ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            KitchenTicketCard(
                                ticket = ticket,
                                currentTick = elapsedSeconds,
                                onStart = { viewModel.updateStatus(ticket.orderId, "PREPARING") },
                                onDone = { viewModel.updateStatus(ticket.orderId, "READY") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KitchenFilterChip(
    label: String,
    value: String,
    current: String,
    count: Int,
    selectedColor: Color = PrimaryColor,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onClick(value) },
        label = {
            Text("$label ($count)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (current == value) FontWeight.Bold else FontWeight.Normal)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.15f),
            selectedLabelColor = selectedColor
        )
    )
}

@Composable
fun KitchenTicketCard(
    ticket: KitchenOrder,
    currentTick: Int,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    val statusColor = when (ticket.status) {
        "PENDING" -> Color(0xFFFFA000)
        "PREPARING" -> Color(0xFF1976D2)
        else -> Color.Gray
    }

    // Read currentTick to force recomposition every second
    val _tick = currentTick

    // Dynamic Time Calculation triggered by currentTick
    val targetTimeMs = ticket.targetTimeMinutes * 60 * 1000L
    val elapsedMs = System.currentTimeMillis() - ticket.createdAt
    val isOverdue = elapsedMs > targetTimeMs

    // Calculate time strings
    val elapsedSecs = (elapsedMs / 1000).coerceAtLeast(0)
    val eMins = elapsedSecs / 60
    val eSecs = elapsedSecs % 60
    val elapsedStr = String.format("%02d:%02d", eMins, eSecs)
    
    val borderColor = if (isOverdue) MaterialTheme.colorScheme.error else Color.Transparent

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isOverdue) 2.dp else 0.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "# ${ticket.orderId}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor, fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = ticket.tableName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // Timer badge
                Surface(
                    color = if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFF37474F),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Color.White,
                            modifier = Modifier.size(14.dp))
                        Text("$elapsedStr / ${ticket.targetTimeMinutes}:00", color = Color.White,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        if (isOverdue)
                            Text("⚠ TRỄ", color = Color.White,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Items
            ticket.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(PrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${item.qty}", fontWeight = FontWeight.ExtraBold,
                            color = PrimaryColor, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium)
                        if (item.note.isNotBlank()) {
                            Text("⚠ ${item.note}", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            when (ticket.status) {
                "PENDING" -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Icon(Icons.Default.OutdoorGrill, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BẮT ĐẦU NẤU", fontWeight = FontWeight.Bold)
                    }
                }
                "PREPARING" -> {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HOÀN THÀNH", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}