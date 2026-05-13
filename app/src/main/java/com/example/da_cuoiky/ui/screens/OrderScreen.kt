package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OrderScreen(
    tableId: String = "T01",
    tableName: String = "Bàn 01",
    onSendToKitchen: (Order) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var searchQuery by remember { mutableStateOf("") }
    var cartItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var showNoteDialog by remember { mutableStateOf<String?>(null) } // menuItemId

    var menuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(listOf("Tất cả")) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
            val items = apiService.getMenuItems()
            menuItems = items
            
            val cats = items.mapNotNull { it.category }.distinct().toMutableList()
            if (!cats.contains("Tất cả")) cats.add(0, "Tất cả")
            categories = cats
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredMenu = menuItems.filter { item ->
        val matchCategory = selectedCategory == "Tất cả" || item.category == selectedCategory
        val matchSearch = item.name.contains(searchQuery, ignoreCase = true)
        // Nếu có isAvailable thì check, nếu API ko trả về thì coi như True
        matchCategory && matchSearch
    }

    val subtotal = cartItems.sumOf { it.totalPrice }

    Scaffold(
        topBar = {
            StaffTopBar(
                title = "Order — $tableName",
                subtitle = "${cartItems.sumOf { it.qty }} món • %,d ₫".format(subtotal),
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // ── Top: Menu Panel ──────────────────────────────────
            Column(modifier = Modifier.weight(1.3f).fillMaxWidth()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm món...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty())
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Category chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Menu Items
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMenu, key = { it.id }) { item ->
                        StaffMenuRow(
                            item = item,
                            qtyInCart = cartItems.find { it.menuItemId == item.id }?.qty ?: 0,
                            onAdd = {
                                cartItems = cartItems.toMutableList().also { list ->
                                    val idx = list.indexOfFirst { it.menuItemId == item.id }
                                    if (idx >= 0) list[idx] = list[idx].copy(qty = list[idx].qty + 1)
                                    else list.add(OrderItem(item.id, item.name, 1, item.price))
                                }
                            },
                            onRemove = {
                                cartItems = cartItems.toMutableList().also { list ->
                                    val idx = list.indexOfFirst { it.menuItemId == item.id }
                                    if (idx >= 0) {
                                        if (list[idx].qty > 1) list[idx] = list[idx].copy(qty = list[idx].qty - 1)
                                        else list.removeAt(idx)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp)

            // ── Bottom: Cart Panel ─────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Đơn hàng",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCartCheckout, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Chưa có món nào", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        items(cartItems, key = { it.menuItemId }) { item ->
                            CartItemRow(item = item)
                        }
                    }
                }

                // Cart Summary & Send Button
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tạm tính", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "%,d ₫".format(subtotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (isSubmitting) return@Button
                                isSubmitting = true
                                
                                val orderRequest = CreateOrderRequest(
                                    tableId = tableId,
                                    staffId = "S01",
                                    totalPrice = subtotal,
                                    items = cartItems.map { 
                                        OrderItemRequest(it.menuItemId, it.name, it.qty, it.price, it.note)
                                    }
                                )

                                coroutineScope.launch {
                                    try {
                                        val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                                        val response = apiService.createOrder(orderRequest)
                                        if (response.isSuccessful && response.body()?.status == "success") {
                                            android.widget.Toast.makeText(context, "Đã gửi order!", android.widget.Toast.LENGTH_SHORT).show()
                                            
                                            // Notify kitchen via Firebase Firestore
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("kitchen_updates").document("latest")
                                                .set(mapOf("timestamp" to System.currentTimeMillis()))
                                                .addOnSuccessListener {
                                                    android.util.Log.d("ORDER_DEBUG", "Kitchen notified via Firebase")
                                                }
                                                .addOnFailureListener { e ->
                                                    android.util.Log.w("ORDER_DEBUG", "Failed to notify kitchen via Firebase", e)
                                                }

                                            // Tạo object Order thật từ kết quả API
                                            val realOrder = Order(
                                                id = response.body()?.orderId ?: "0",
                                                tableId = tableId,
                                                staffId = "S01",
                                                statusStr = "PENDING",
                                                totalPriceFromApi = subtotal,
                                                createdAt = "",
                                                items = cartItems // Gắn món thật vào order
                                            )
                                            onSendToKitchen(realOrder)
                                        } else {
                                            val errMsg = response.body()?.message ?: "Lỗi không xác định"
                                            android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Lỗi kết nối: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = cartItems.isNotEmpty() && !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessColor)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.OutdoorGrill, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GỬI BẾP", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffMenuRow(
    item: MenuItem,
    qtyInCart: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("%,d ₫".format(item.price), color = PrimaryColor,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                if (item.prepTime > 0) {
                    Text("⏱ ${item.prepTime} phút", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            // Qty control
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (qtyInCart > 0) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.RemoveCircle, "Giảm", tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "$qtyInCart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 24.dp),
                        color = PrimaryColor
                    )
                }
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AddCircle, "Thêm", tint = PrimaryColor,
                        modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(item: OrderItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.qty}× ${item.name}",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (item.note.isNotBlank())
                Text("* ${item.note}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error)
        }
        Text(
            "%,d ₫".format(item.totalPrice),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(thickness = 0.5.dp)
}