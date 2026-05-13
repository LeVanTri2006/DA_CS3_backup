package com.example.da_cuoiky.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*

@Composable
fun CustomerHomeScreen(
    user: User?, 
    menuViewModel: com.example.da_cuoiky.ui.viewmodel.MenuViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onMenuClick: () -> Unit,
    onBookingClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val menuItems by menuViewModel.menuItems.collectAsState()
    
    val categories = remember(menuItems) {
        menuItems.mapNotNull { it.category }.distinct()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ... (Hero Header and Search Bar same as before)
            // ── Hero Header ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF6F00), Color(0xFFE65100))
                        )
                    )
            ) {
                // Decorative circles
                Box(modifier = Modifier.size(200.dp).offset((-40).dp, (-60).dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
                Box(modifier = Modifier.size(150.dp).align(Alignment.TopEnd).offset(30.dp, (-20).dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // XỬ LÝ LỜI CHÀO KHI USER NULL
                            val displayName = user?.name?.split(" ")?.last() ?: "bạn"
                            Text("Xin chào, $displayName! 👋",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f))
                            Text("Gourmet Hub",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White)
                        }
                        // Avatar
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onProfileClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, "Hồ sơ cá nhân", tint = Color.White,
                                modifier = Modifier.size(28.dp))
                        }
                    }

                    // Loyalty Points Badge (CHỈ HIỆN KHI ĐÃ ĐĂNG NHẬP)
                    if (user != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Stars, "Điểm tích lũy", tint = WarningColor,
                                    modifier = Modifier.size(20.dp))
                                Text("${user.loyaltyPoints} điểm tích lũy",
                                    color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("• Cấp Vàng 🥇",
                                    color = WarningColor, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        // Nếu chưa đăng nhập, có thể hiện một câu mời gọi nhẹ nhàng
                        Text(
                            "Đăng nhập để nhận ưu đãi ngay! ✨",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Search Bar ───────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-20).dp)) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Tìm kiếm món ăn, danh mục...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            // ── Quick Actions ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TableBar,
                    label = "Đặt Bàn",
                    color = Color(0xFF1976D2),
                    onClick = onBookingClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DeliveryDining,
                    label = "Đặt Giao",
                    color = Color(0xFF2E7D32),
                    onClick = onMenuClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingBag,
                    label = "Tự Lấy",
                    color = Color(0xFF7B1FA2),
                    onClick = onMenuClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    label = "Đơn Cũ",
                    color = Color(0xFFE65100),
                    onClick = onOrdersClick
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Promo Banner ──────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎉", fontSize = 36.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ưu đãi hôm nay!",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE65100))
                        Text("Giảm 20% tất cả combo — dùng mã: GOURMET20",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = PrimaryColor, shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dùng ngay",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Popular Items ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥 Món Nổi Bật",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold)
                TextButton(onClick = onMenuClick) {
                    Text("Xem tất cả", color = PrimaryColor)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val popularItems = menuItems.sortedByDescending { it.soldCount }.take(5)
                items(popularItems) { item ->
                    CustomerMenuCard(item = item, onClick = { onProductClick(item.id ?: "") })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Categories ────────────────────────────────────────
            Text(
                "📋 Danh Mục",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val catIcons = mapOf(
                "Món chính" to "🍜", "Khai vị" to "🥗",
                "Đồ uống" to "🧋", "Tráng miệng" to "🍮",
                "Món nướng" to "🥩", "Món lẩu" to "🍲"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { cat ->
                    CategoryChipCard(
                        emoji = catIcons[cat] ?: "🍽",
                        label = cat,
                        onClick = onMenuClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Branch Info ───────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 Nhà Hàng Gourmet Hub",
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("12 Lê Lợi, Q.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                    Text("⏰ 10:00 - 22:00",
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessColor, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CustomerQuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CustomerMenuCard(item: MenuItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                if (item.isPopular) {
                    Surface(
                        color = PrimaryColor,
                        shape = RoundedCornerShape(bottomEnd = 10.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text("🔥 Hot",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text("⏱ ${item.prepTime} phút",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("%,d ₫".format(item.price),
                        color = PrimaryColor, fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.bodyMedium)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, "Thêm ${item.name}",
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipCard(emoji: String, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(80.dp)
            .width(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 28.sp)
            Text(label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun CustomerBottomBar(
    currentRoute: String,
    onMenuClick: () -> Unit,
    onBookingClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = {},
            icon = { Icon(Icons.Default.Home, "Trang chủ") },
            label = { Text("Trang chủ") }
        )
        NavigationBarItem(
            selected = currentRoute == "menu",
            onClick = onMenuClick,
            icon = { Icon(Icons.Default.RestaurantMenu, "Thực đơn") },
            label = { Text("Thực đơn") }
        )
        NavigationBarItem(
            selected = currentRoute == "booking",
            onClick = onBookingClick,
            icon = { Icon(Icons.Default.TableBar, "Đặt bàn") },
            label = { Text("Đặt bàn") }
        )
        NavigationBarItem(
            selected = currentRoute == "orders",
            onClick = onOrdersClick,
            icon = { Icon(Icons.Default.ReceiptLong, "Đơn hàng") },
            label = { Text("Đơn hàng") }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.AccountCircle, "Hồ sơ") },
            label = { Text("Hồ sơ") }
        )
    }
}
