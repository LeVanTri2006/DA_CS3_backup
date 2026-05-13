package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*
import com.example.da_cuoiky.ui.viewmodel.MenuViewModel

@Composable
fun MenuListScreen(
    viewModel: MenuViewModel = viewModel(),
    onProductClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onAddToCart: (OrderItem) -> Unit,
    cartCount: Int,
    onBack: () -> Unit
) {
    val menuItems by viewModel.menuItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = menuItems.filter { item ->
        val matchCategory = selectedCategory == "Tất cả" || item.category == selectedCategory
        val matchSearch = (item.name ?: "").contains(searchQuery, ignoreCase = true)
        matchCategory && matchSearch
    }

    Scaffold(
        topBar = {
            CustomerTopBar(
                title = "Thực Đơn",
                onBack = onBack,
                cartCount = cartCount,
                onCartClick = onCartClick
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Lỗi: $error", color = Color.Red)
                    Button(onClick = { viewModel.fetchMenu() }) { Text("Thử lại") }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm kiếm món ăn...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            shape = RoundedCornerShape(14.dp), singleLine = true
                        )
                    }
                    item {
                        val categories = listOf("Tất cả") + menuItems.mapNotNull { it.category }.distinct()
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                    items(filteredItems.chunked(2)) { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { item ->
                                MenuGridCard(
                                    modifier = Modifier.weight(1f),
                                    item = item,
                                    onClick = { onProductClick(item.id ?: "") },
                                    onAdd = {
                                        onAddToCart(OrderItem(
                                            menuItemId = item.id ?: "",
                                            name = item.name ?: "",
                                            qty = 1,
                                            price = item.price ?: 0
                                        ))
                                    }
                                )
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    itemId: String,
    viewModel: MenuViewModel = viewModel(),
    onAddToCart: (OrderItem) -> Unit,
    onBack: () -> Unit
) {
    val menuItems by viewModel.menuItems.collectAsState()
    val item = menuItems.find { it.id == itemId }
    var qty by remember { mutableIntStateOf(1) }

    if (item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFEF6C00))
        }
    } else {
        Scaffold(
            bottomBar = {
                // Thanh toán dưới cùng được thiết kế tinh tế hơn
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onAddToCart(OrderItem(
                                    menuItemId = item.id ?: "",
                                    name = item.name ?: "",
                                    qty = qty,
                                    price = item.price ?: 0
                                ))
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Thêm vào giỏ — %,d ₫".format((item.price ?: 0) * qty),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    // Ảnh sản phẩm với hiệu ứng Parallax nhẹ (nếu muốn)
                    item {
                        val fullImageUrl = if (item.imageUrl?.startsWith("http") == true) item.imageUrl
                        else "http://10.0.2.2/WEB_ADMIN/view/uploads/${item.imageUrl}"
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Phần chi tiết bọc trong một bề mặt trắng đè lên ảnh
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp) // Đè lên ảnh một chút
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                )
                                .padding(24.dp)
                        ) {
                            // Tên và Giá
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = item.name ?: "Không tên",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%,d ₫".format(item.price ?: 0),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFFEF6C00),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Mô tả
                            Text("Mô tả món ăn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.description ?: "Chưa có mô tả chi tiết cho món ăn này.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Điều khiển số lượng
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .background(Color(0xFFF5F5F5), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (qty > 1) qty-- }) {
                                        Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                                    }

                                    Text(
                                        text = "$qty",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    IconButton(onClick = { qty++ }) {
                                        Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFFEF6C00), modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Nút quay lại (Floating)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
                }
            }
        }
    }
}
@Composable
private fun MenuGridCard(modifier: Modifier = Modifier, item: MenuItem, onClick: () -> Unit, onAdd: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column {
            val fullImageUrl = if (item.imageUrl?.startsWith("http") == true) item.imageUrl else "http://10.0.2.2/WEB_ADMIN/view/uploads/${item.imageUrl}"
            AsyncImage(model = fullImageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.padding(10.dp)) {
                Text(item.name ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("%,d ₫".format(item.price ?: 0), color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onAdd, modifier = Modifier.size(32.dp).background(Color(0xFFEF6C00), CircleShape)) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTopBar(title: String, onBack: (() -> Unit)? = null, cartCount: Int = 0, onCartClick: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
        actions = {
            if (onCartClick != null) {
                BadgedBox(badge = { if (cartCount > 0) Badge { Text("$cartCount") } }) {
                    IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingCart, null) }
                }
            }
        }
    )
}
