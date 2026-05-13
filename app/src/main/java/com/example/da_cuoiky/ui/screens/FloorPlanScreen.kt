package com.example.da_cuoiky.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*

@Composable
fun FloorPlanScreen(
    onTableClick: (TableModel) -> Unit,
    onNavigateToOrder: (String) -> Unit,
    refreshKey: Int = 0  // ✅ Thêm refreshKey để trigger reload
) {
    var tables by remember { mutableStateOf<List<TableModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedZone by remember { mutableStateOf<TableZone?>(null) }
    var showLegend by remember { mutableStateOf(false) }

    var firebaseTrigger by remember { mutableStateOf(0) }

    // Lắng nghe Firebase realtime cho bàn ăn
    DisposableEffect(Unit) {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("table_updates")
            .document("latest")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.w("TABLE_DEBUG", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    firebaseTrigger++ // Tăng giá trị để ép load lại API
                }
            }
        onDispose { listener.remove() }
    }

    // ✅ Load tables khi refreshKey HOẶC firebaseTrigger thay đổi
    LaunchedEffect(refreshKey, firebaseTrigger) {
        try {
            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
            val response = apiService.getTables()
            if (response.isSuccessful && response.body()?.status == "success") {
                tables = response.body()?.data ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredTables = (if (selectedZone == null) tables
                         else tables.filter { it.zone == selectedZone })
                         .distinctBy { it.id } // ✅ Tránh crash nếu API trả trùng mã bàn (Key "..." was already used)

    val occupiedCount = tables.count { it.status == TableStatus.OCCUPIED }
    val totalCount = tables.count { it.status != TableStatus.LOCKED }

    Scaffold(
        topBar = {
            StaffTopBar(
                title = "Sơ Đồ Bàn",
                subtitle = "$occupiedCount/$totalCount bàn đang có khách",
                actions = {
                    IconButton(onClick = { showLegend = !showLegend }) {
                        Icon(Icons.Default.Info, "Chú thích màu sắc", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Zone Filter Chips
            ScrollableZoneFilter(
                selectedZone = selectedZone,
                onZoneSelected = { selectedZone = it }
            )

            // Legend
            if (showLegend) StatusLegendBar()

            // Table Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredTables, key = { it.id }) { table ->
                    TableTile(
                        table = table,
                        onTap = {
                            if (table.status == TableStatus.OCCUPIED) {
                                onTableClick(table)
                            } else {
                                onNavigateToOrder(table.id)
                            }
                        },
                        onDoubleTap = { }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffTopBar(
    title: String,
    subtitle: String = "",
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                if (subtitle.isNotEmpty())
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryColor
        ),
        actions = actions
    )
}

@Composable
fun ScrollableZoneFilter(
    selectedZone: TableZone?,
    onZoneSelected: (TableZone?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedZone == null,
            onClick = { onZoneSelected(null) },
            label = { Text("Tất cả") }
        )
        TableZone.entries.forEach { zone ->
            FilterChip(
                selected = selectedZone == zone,
                onClick = { onZoneSelected(if (selectedZone == zone) null else zone) },
                label = { Text(zone.displayName) }
            )
        }
    }
}

@Composable
fun StatusLegendBar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendDot(Color(0xFFBDBDBD), "Trống")
            LegendDot(Color(0xFF1976D2), "Đặt trước")
            LegendDot(Color(0xFFFF6F00), "Có khách")
            LegendDot(Color(0xFF2E7D32), "Đã thanh toán")
            LegendDot(Color(0xFFD32F2F), "Khóa")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TableTile(
    table: TableModel,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = when (table.status) {
            TableStatus.EMPTY -> Color(0xFFBDBDBD)
            TableStatus.RESERVED -> Color(0xFF1976D2)
            TableStatus.OCCUPIED -> Color(0xFFFF6F00)
            TableStatus.PAID -> Color(0xFF2E7D32)
            TableStatus.LOCKED -> Color(0xFFD32F2F)
        },
        animationSpec = spring(),
        label = "table_color"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(table.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Zone badge
            Text(
                text = when (table.zone) {
                    TableZone.VIP -> "⭐"
                    TableZone.OUTDOOR -> "🌿"
                    TableZone.INDOOR -> ""
                },
                fontSize = 14.sp
            )
            Text(
                text = table.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, "Sức chứa", tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp))
                Text(" ${table.capacity}", style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (table.status) {
                    TableStatus.EMPTY -> "Trống"
                    TableStatus.RESERVED -> "Đặt trước"
                    TableStatus.OCCUPIED -> "Có khách"
                    TableStatus.PAID -> "Đã thanh toán"
                    TableStatus.LOCKED -> "Khóa"
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            if (table.status == TableStatus.OCCUPIED) {
                Spacer(modifier = Modifier.height(2.dp))
                Text("Nhấn đôi → Order",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp)
                // ✅ Hiển thị số món và tổng tiền
                if (table.itemCount > 0) {
                    Text("${table.itemCount} món",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp)
                }
                if (table.orderTotal > 0) {
                    Text("%,d₫".format(table.orderTotal),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp)
                }
            }
        }
    }
}
