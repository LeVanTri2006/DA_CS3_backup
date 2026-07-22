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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*


internal data class AppTypoScale(
    val heroTitle: TextUnit,       // "Gourmet Hub"
    val heroSubtitle: TextUnit,    // "Xin chào..."
    val sectionTitle: TextUnit,    // "🔥 Món Nổi Bật"
    val cardName: TextUnit,        // Tên món trên card
    val cardPrice: TextUnit,       // Giá trên card
    val labelAction: TextUnit,     // Label quick action
    val labelCategory: TextUnit,   // Label danh mục
    val bodyPromo: TextUnit,       // Text banner khuyến mãi
    val prepTime: TextUnit,        // "⏱ 5 phút"
)

@Composable
internal fun rememberTypoScale(screenW: Int): AppTypoScale {
    return remember(screenW) {
        when {
            screenW < 360 -> AppTypoScale(      // Màn siêu nhỏ (< 360dp)
                heroTitle = 22.sp,
                heroSubtitle = 13.sp,
                sectionTitle = 16.sp,
                cardName = 13.sp,
                cardPrice = 13.sp,
                labelAction = 11.sp,
                labelCategory = 11.sp,
                bodyPromo = 12.sp,
                prepTime = 11.sp
            )
            screenW < 390 -> AppTypoScale(      // Màn nhỏ (360-389dp) — Samsung A, Redmi
                heroTitle = 24.sp,
                heroSubtitle = 14.sp,
                sectionTitle = 17.sp,
                cardName = 14.sp,
                cardPrice = 14.sp,
                labelAction = 12.sp,
                labelCategory = 12.sp,
                bodyPromo = 13.sp,
                prepTime = 12.sp
            )
            screenW < 430 -> AppTypoScale(      // Màn trung bình (390-429dp) — Pixel 7, iPhone 14
                heroTitle = 26.sp,
                heroSubtitle = 15.sp,
                sectionTitle = 18.sp,
                cardName = 15.sp,
                cardPrice = 15.sp,
                labelAction = 13.sp,
                labelCategory = 12.sp,
                bodyPromo = 13.sp,
                prepTime = 12.sp
            )
            else -> AppTypoScale(               // Màn lớn (≥ 430dp) — Tablet, foldable
                heroTitle = 28.sp,
                heroSubtitle = 16.sp,
                sectionTitle = 20.sp,
                cardName = 16.sp,
                cardPrice = 16.sp,
                labelAction = 14.sp,
                labelCategory = 13.sp,
                bodyPromo = 14.sp,
                prepTime = 12.sp
            )
        }
    }
}

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

    val screenW = LocalConfiguration.current.screenWidthDp
    val typo = rememberTypoScale(screenW)

    // Spacing scale — giảm nhẹ trên màn nhỏ
    val hPad: Dp  = if (screenW < 390) 14.dp else 16.dp
    val sectionGap: Dp = if (screenW < 390) 18.dp else 24.dp

    // Hero height — ShopeeFood style: không quá cao
    val heroHeight: Dp = when {
        screenW < 360 -> 185.dp
        screenW < 390 -> 195.dp
        screenW < 430 -> 205.dp
        else -> 220.dp
    }

    // Card width — tỉ lệ 40% màn, min 145dp
    val cardWidth: Dp = (screenW * 0.42f).coerceIn(145f, 180f).dp

    val categories = remember(menuItems) {
        menuItems.mapNotNull { it.category }.distinct()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // ── Hero Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF6F00), Color(0xFFE65100))
                        )
                    )
            ) {
                // Decorative circles
                Box(
                    modifier = Modifier.size(180.dp).offset((-30).dp, (-50).dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
                )
                Box(
                    modifier = Modifier.size(130.dp).align(Alignment.TopEnd).offset(25.dp, (-15).dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = hPad, vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = user?.name?.split(" ")?.last() ?: "bạn"
                            Text(
                                "Xin chào, $displayName! 👋",
                                fontSize = typo.heroSubtitle,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Gourmet Hub",
                                fontSize = typo.heroTitle,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        // Avatar — ShopeeFood style: luôn 44dp
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onProfileClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person, "Hồ sơ",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Loyalty / CTA badge
                    if (user != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Stars, null, tint = WarningColor, modifier = Modifier.size(18.dp))
                                Text(
                                    "${user.loyaltyPoints} điểm tích lũy • Cấp Vàng 🥇",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = typo.prepTime,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            "Đăng nhập để nhận ưu đãi! ✨",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = typo.prepTime
                        )
                    }
                }
            }

            // ── Search Bar (floating lên Hero) ───────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = hPad)
                    .offset(y = (-20).dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(10.dp)
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = {
                            Text(
                                "Tìm kiếm món ăn, danh mục...",
                                fontSize = typo.bodyPromo
                            )
                        },
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


            // 4 action icons luôn fit 1 hàng, dùng weight(1f) không cần lo overflow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TableBar,
                    label = "Đặt Bàn",
                    color = Color(0xFF1976D2),
                    labelFontSize = typo.labelAction,
                    onClick = onBookingClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DeliveryDining,
                    label = "Giao Hàng",
                    color = Color(0xFF2E7D32),
                    labelFontSize = typo.labelAction,
                    onClick = onMenuClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingBag,
                    label = "Tự Lấy",
                    color = Color(0xFF7B1FA2),
                    labelFontSize = typo.labelAction,
                    onClick = onMenuClick
                )
                CustomerQuickAction(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    label = "Đơn Cũ",
                    color = Color(0xFFE65100),
                    labelFontSize = typo.labelAction,
                    onClick = onOrdersClick
                )
            }

            Spacer(modifier = Modifier.height(sectionGap))


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎉", fontSize = 30.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ưu đãi hôm nay!",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE65100),
                            fontSize = typo.cardName
                        )
                        Text(
                            "Giảm 20% tất cả combo — mã: GOURMET20",
                            fontSize = typo.bodyPromo,
                            color = Color(0xFF5D4037),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = PrimaryColor, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "Dùng ngay",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = typo.prepTime,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // ── Popular Items ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🔥 Món Nổi Bật",
                    fontSize = typo.sectionTitle,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onMenuClick) {
                    Text("Xem tất cả", color = PrimaryColor, fontSize = typo.bodyPromo)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = hPad),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val popularItems = menuItems.sortedByDescending { it.soldCount }.take(6)
                items(popularItems) { item ->
                    CustomerMenuCard(
                        item = item,
                        cardWidth = cardWidth,
                        onClick = { onProductClick(item.id ?: "") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // ── Categories ───────────────────────────────────────────
            Text(
                "📋 Danh Mục",
                fontSize = typo.sectionTitle,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = hPad)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val catIcons = mapOf(
                "Món chính" to "🍜", "Khai vị" to "🥗",
                "Đồ uống" to "🧋", "Tráng miệng" to "🍮",
                "Món nướng" to "🥩", "Món lẩu" to "🍲"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = hPad),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    CategoryChipCard(
                        emoji = catIcons[cat] ?: "🍽",
                        label = cat,
                        labelFontSize = typo.labelCategory,
                        onClick = onMenuClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionGap))

            // ── Branch Info ──────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Store, null, tint = PrimaryColor, modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            "Nhà Hàng Gourmet Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = typo.cardName
                        )
                        Text(
                            "📍 12 Lê Lợi, Q.1  •  ⏰ 10:00 - 22:00",
                            fontSize = typo.prepTime,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────
// Quick Action Button
// Icon 52dp, label 12-14sp
// ─────────────────────────────────
@Composable
private fun CustomerQuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    labelFontSize: TextUnit = 12.sp,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            label,
            fontSize = labelFontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────
// Menu Card — tỉ lệ theo màn
// Style giá bold màu cam
// ─────────────────────────────────
@Composable
fun CustomerMenuCard(
    item: MenuItem,
    cardWidth: Dp = 160.dp,
    isSmall: Boolean = false,   // legacy compat
    onClick: () -> Unit
) {
    // Tính font size trực tiếp — không nhận AppTypoScale (tránh visibility error)
    val screenW = LocalConfiguration.current.screenWidthDp
    val nameFontSize = when {
        screenW < 360 -> 12.sp
        screenW < 390 -> 13.sp
        screenW < 430 -> 14.sp
        else -> 15.sp
    }
    val priceFontSize = nameFontSize
    val prepTimeFontSize = maxOf(nameFontSize.value - 2f, 10f).sp

    // Image height tỉ lệ 65% card width
    val imageHeight = (cardWidth.value * 0.65f).dp

    Card(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                    contentScale = ContentScale.Crop
                )
                if (item.isPopular) {
                    Surface(
                        color = PrimaryColor,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            "🔥 Hot",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    item.name,
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2, // Đảm bảo luôn chiếm 2 dòng để card bằng nhau
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (nameFontSize.value + 4f).sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "⏱ ${item.prepTime} phút",
                    fontSize = prepTimeFontSize,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )  {

                    Text(
                        "%,d ₫".format(item.price),
                        color = PrimaryColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = priceFontSize,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, "Thêm ${item.name}",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────
// Category Chip
// 80×88dp, emoji 24-26sp, label 11-13sp — giống ShopeeFood
// ─────────────────────────────────
@Composable
private fun CategoryChipCard(
    emoji: String,
    label: String,
    labelFontSize: TextUnit = 12.sp,
    isSmall: Boolean = false,  // legacy compat
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(84.dp)
            .width(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                label,
                fontSize = labelFontSize,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────
// Bottom Bar (stub)
// ─────────────────────────────────
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
