package com.example.da_cuoiky.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    // ✅ Responsive: Phát hiện màn hình nhỏ
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 380

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
                subtitle = "Chờ: $pendingCount • Đang nấu: $preparingCount"
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ✅ FIX: Filter chips dùng horizontalScroll thay vì Row cứng
            // → Không bị clip khi màn nhỏ hoặc thêm nhiều tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KitchenFilterChip(
                    label = "Tất cả",
                    value = "ALL",
                    current = filter,
                    count = tickets.size,
                    isSmallScreen = isSmallScreen
                ) { filter = it }
                KitchenFilterChip(
                    label = "Chờ nấu",
                    value = "PENDING",
                    current = filter,
                    count = pendingCount,
                    selectedColor = Color(0xFFFFA000),
                    isSmallScreen = isSmallScreen
                ) { filter = it }
                KitchenFilterChip(
                    label = "Đang nấu",
                    value = "PREPARING",
                    current = filter,
                    count = preparingCount,
                    selectedColor = Color(0xFF1976D2),
                    isSmallScreen = isSmallScreen
                ) { filter = it }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Đang tải đơn hàng...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.WifiOff,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 48.dp else 64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchKitchenOrders() }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thử lại")
                        }
                    }
                }
            } else if (filteredTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 56.dp else 72.dp),
                            tint = SuccessColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Không có ticket!",
                            style = if (isSmallScreen) MaterialTheme.typography.titleSmall
                                    else MaterialTheme.typography.titleMedium,
                            color = SuccessColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tất cả đơn đã hoàn thành 🎉",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    // ✅ FIX: Padding nhỏ hơn trên màn nhỏ
                    contentPadding = PaddingValues(if (isSmallScreen) 8.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 12.dp)
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
                                isSmallScreen = isSmallScreen,
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

// ─────────────────────────────────
// Filter Chip — responsive label size
// ─────────────────────────────────
@Composable
private fun KitchenFilterChip(
    label: String,
    value: String,
    current: String,
    count: Int,
    selectedColor: Color = PrimaryColor,
    isSmallScreen: Boolean = false,
    onClick: (String) -> Unit
) {
    FilterChip(
        selected = current == value,
        onClick = { onClick(value) },
        label = {
            Text(
                // ✅ FIX: Màn nhỏ chỉ hiện số, màn lớn hiện cả label
                text = if (isSmallScreen) "$label($count)" else "$label ($count)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (current == value) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.15f),
            selectedLabelColor = selectedColor
        )
    )
}

// ─────────────────────────────────
// Ticket Card — Responsive
// ─────────────────────────────────
@Composable
fun KitchenTicketCard(
    ticket: KitchenOrder,
    currentTick: Int,
    isSmallScreen: Boolean = false,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    val statusColor = when (ticket.status) {
        "PENDING" -> Color(0xFFFFA000)
        "PREPARING" -> Color(0xFF1976D2)
        else -> Color.Gray
    }

    // Read currentTick to force recomposition every second
    @Suppress("UNUSED_VARIABLE")
    val tick = currentTick

    // Dynamic Time Calculation
    val targetTimeMs = ticket.targetTimeMinutes * 60 * 1000L
    val elapsedMs = System.currentTimeMillis() - ticket.createdAt
    val isOverdue = elapsedMs > targetTimeMs

    val elapsedSecs = (elapsedMs / 1000).coerceAtLeast(0)
    val eMins = elapsedSecs / 60
    val eSecs = elapsedSecs % 60
    val elapsedStr = String.format("%02d:%02d", eMins, eSecs)

    // ✅ Responsive padding
    val cardPadding = if (isSmallScreen) 12.dp else 16.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isOverdue) 2.dp else 0.dp,
            color = if (isOverdue) MaterialTheme.colorScheme.error else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOverdue) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(cardPadding)) {

            // ── Header ────────────────────────────────────────────────
            // ✅ FIX: Màn nhỏ dùng Column xếp dọc, màn lớn dùng Row ngang
            if (isSmallScreen) {
                // Dòng 1: Mã đơn + Tên bàn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "#${ticket.orderId.takeLast(8)}",  // Rút gọn orderId
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = ticket.tableName,
                        style = MaterialTheme.typography.titleMedium,  // Nhỏ hơn trên màn nhỏ
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Dòng 2: Timer badge full width
                TimerBadge(
                    elapsedStr = elapsedStr,
                    targetMinutes = ticket.targetTimeMinutes,
                    isOverdue = isOverdue,
                    isSmallScreen = true
                )
            } else {
                // Màn lớn: Header ngang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "# ${ticket.orderId}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = ticket.tableName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TimerBadge(
                        elapsedStr = elapsedStr,
                        targetMinutes = ticket.targetTimeMinutes,
                        isOverdue = isOverdue,
                        isSmallScreen = false
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = if (isSmallScreen) 8.dp else 12.dp))

            // ── Items ─────────────────────────────────────────────────
            ticket.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (isSmallScreen) 3.dp else 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ✅ FIX: Box số lượng nhỏ hơn trên màn nhỏ
                    Box(
                        modifier = Modifier
                            .size(if (isSmallScreen) 26.dp else 30.dp)
                            .background(PrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${item.qty}",
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryColor,
                            fontSize = if (isSmallScreen) 12.sp else 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            // ✅ FIX: Font nhỏ hơn trên màn nhỏ, có Ellipsis
                            style = if (isSmallScreen) MaterialTheme.typography.bodyMedium
                                    else MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.note.isNotBlank()) {
                            Text(
                                "⚠ ${item.note}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))

            // ── Action Buttons ─────────────────────────────────────────
            when (ticket.status) {
                "PENDING" -> {
                    Button(
                        onClick = onStart,
                        // ✅ FIX: heightIn thay vì height cứng
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isSmallScreen) 44.dp else 48.dp),
                        shape = RoundedCornerShape(if (isSmallScreen) 8.dp else 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Icon(
                            Icons.Default.OutdoorGrill,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "BẮT ĐẦU NẤU",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallScreen) 14.sp else 15.sp
                        )
                    }
                }
                "PREPARING" -> {
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isSmallScreen) 44.dp else 48.dp),
                        shape = RoundedCornerShape(if (isSmallScreen) 8.dp else 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "HOÀN THÀNH",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallScreen) 14.sp else 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────
// Timer Badge — extracted component
// ─────────────────────────────────
@Composable
private fun TimerBadge(
    elapsedStr: String,
    targetMinutes: Int,
    isOverdue: Boolean,
    isSmallScreen: Boolean
) {
    Surface(
        color = if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFF37474F),
        shape = RoundedCornerShape(if (isSmallScreen) 6.dp else 8.dp),
        // ✅ Màn nhỏ: badge chiếm full width để text không bị wrap
        modifier = if (isSmallScreen) Modifier.fillMaxWidth() else Modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isSmallScreen) 8.dp else 10.dp,
                vertical = if (isSmallScreen) 4.dp else 6.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Timer,
                null,
                tint = Color.White,
                modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp)
            )
            // ✅ FIX: Gộp timer + overdue vào 1 Text, tránh wrap
            Text(
                text = if (isOverdue) "$elapsedStr / ${targetMinutes}:00 ⚠ TRỄ"
                       else "$elapsedStr / ${targetMinutes}:00",
                color = Color.White,
                style = if (isSmallScreen) MaterialTheme.typography.labelSmall
                        else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}