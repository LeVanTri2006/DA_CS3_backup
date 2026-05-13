package com.example.da_cuoiky.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.example.da_cuoiky.fiebase.AuthViewModel
import com.example.da_cuoiky.fiebase.ProfileUiState
import com.example.da_cuoiky.fiebase.UserProfile
import com.example.da_cuoiky.model.*
import com.example.da_cuoiky.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────
// BOOKING / RESERVATION SCREEN
// ─────────────────────────────────

@Composable
fun BookingScreen(
    authViewModel: AuthViewModel,
    onConfirm: (Reservation) -> Unit,
    onBack: () -> Unit
) {
    val profileState by authViewModel.profileState.collectAsState()
    
    // Luôn đảm bảo tải profile khi vào màn hình
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile()
    }

    // Lấy thông tin user hiện tại từ Auth để làm fallback
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    when (val state = profileState) {
        is ProfileUiState.Loading -> {
            // Nếu đang loading nhưng đã có firebaseUser thì hiện form luôn cho nhanh
            if (firebaseUser != null) {
                val fallbackProfile = UserProfile(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Khách hàng",
                    email = firebaseUser.email ?: "",
                    phone = firebaseUser.phoneNumber ?: ""
                )
                BookingContent(fallbackProfile, onConfirm, onBack)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Đang tải thông tin...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        is ProfileUiState.Error -> {
            // Nếu lỗi Firestore nhưng đã đăng nhập Auth thì vẫn cho đặt bàn
            if (firebaseUser != null) {
                val fallbackProfile = UserProfile(
                    uid = firebaseUser.uid,
                    fullName = firebaseUser.displayName ?: "Khách hàng",
                    email = firebaseUser.email ?: ""
                )
                BookingContent(fallbackProfile, onConfirm, onBack)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Lỗi: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = { authViewModel.loadUserProfile() }) { Text("Thử lại") }
                        TextButton(onClick = onBack) { Text("Quay lại") }
                    }
                }
            }
        }
        is ProfileUiState.Success -> {
            BookingContent(state.profile, onConfirm, onBack)
        }
    }
}

@Composable
fun BookingContent(
    userProfile: UserProfile,
    onConfirm: (Reservation) -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var pax by remember { mutableIntStateOf(2) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Định dạng hiển thị cho User
    val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    // Định dạng gửi lên Server PHP (yyyy-MM-dd)
    val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var selectedDateDisplay by remember { mutableStateOf(displayDateFormat.format(Date())) }
    var selectedTime by remember { mutableStateOf("18:00") }
    var selectedZone by remember { mutableStateOf(TableZone.INDOOR) }
    var specialRequest by remember { mutableStateOf("") }
    var addDeposit by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf(userProfile.phone) }
    var currentReservationId by remember { mutableStateOf("") }

    val reservation = remember(userProfile.uid, userProfile.fullName, selectedTime, selectedDateDisplay, pax, selectedZone, specialRequest, currentReservationId) {
        Reservation(
            id = currentReservationId.ifEmpty { "RES${System.currentTimeMillis()}" },
            userId = userProfile.uid,
            userName = userProfile.fullName,
            branchId = "B01",
            datetime = "$selectedTime, $selectedDateDisplay", pax = pax,
            zone = selectedZone, specialRequests = specialRequest,
            status = ReservationStatus.CONFIRMED,
            deposit = 100000,
            qrCode = currentReservationId
        )
    }

    when (step) {
        0 -> BookingDetailsForm(
            pax = pax, onPaxChange = { pax = it },
            selectedDate = selectedDateDisplay, onDateChange = { selectedDateDisplay = it },
            selectedTime = selectedTime, onTimeChange = { selectedTime = it },
            selectedZone = selectedZone, onZoneChange = { selectedZone = it },
            specialRequest = specialRequest, onSpecialRequestChange = { specialRequest = it },
            addDeposit = addDeposit, onDepositChange = { addDeposit = it },
            phone = phoneNumber, onPhoneChange = { phoneNumber = it },
            onBack = onBack, onNext = { 
                if (phoneNumber.length < 9) {
                    android.widget.Toast.makeText(context, "Vui lòng nhập số điện thoại hợp lệ", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    if (isSubmitting) return@BookingDetailsForm
                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            val apiDate = try {
                                val dateObj = displayDateFormat.parse(selectedDateDisplay)
                                apiDateFormat.format(dateObj!!)
                            } catch (e: Exception) { selectedDateDisplay }

                            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                            val request = ReservationRequest(
                                userId = userProfile.uid,
                                userName = userProfile.fullName,
                                phone = phoneNumber,
                                tableId = null,
                                datetime = "$apiDate $selectedTime",
                                pax = pax,
                                zone = selectedZone.displayName,
                                note = specialRequest,
                                deposit = 100000
                            )
                            val response = withTimeoutOrNull(10000) { apiService.createReservation(request) }
                            if (response?.isSuccessful == true && response.body()?.status == "success") {
                                currentReservationId = response.body()?.maDatCho ?: "RES-${System.currentTimeMillis()}"
                                step = 1 
                            } else {
                                android.widget.Toast.makeText(context, "Lỗi: ${response?.body()?.message ?: "Server từ chối"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Lỗi kết nối: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            }
        )
        1 -> BookingConfirmation(
            reservation = reservation,
            isSubmitting = isSubmitting,
            onConfirm = {
                if (isSubmitting) return@BookingConfirmation
                isSubmitting = true
                coroutineScope.launch {
                    try {
                        val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                        val response = withTimeoutOrNull(5000) { apiService.checkReservationStatus(currentReservationId) }
                        if (response?.isSuccessful == true) {
                            val st = response.body()?.trangThai?.uppercase()
                            if (st == "CONFIRMED" || st == "DA_THANH_TOAN") {
                                step = 2
                            } else {
                                android.widget.Toast.makeText(context, "Chưa nhận được thanh toán. Vui lòng đợi trong giây lát hoặc quét lại mã QR.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Lỗi kiểm tra trạng thái", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Lỗi kết nối: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            onBack = { if (!isSubmitting) step = 0 }
        )
        2 -> BookingSuccessScreen(reservation = reservation, onDone = { onConfirm(reservation) })
    }
}

@Composable
private fun BookingDetailsForm(
    pax: Int, onPaxChange: (Int) -> Unit,
    selectedDate: String, onDateChange: (String) -> Unit,
    selectedTime: String, onTimeChange: (String) -> Unit,
    selectedZone: TableZone, onZoneChange: (TableZone) -> Unit,
    specialRequest: String, onSpecialRequestChange: (String) -> Unit,
    addDeposit: Boolean, onDepositChange: (Boolean) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    onBack: () -> Unit, onNext: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val displayDateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    
    Scaffold(
        topBar = { CustomerTopBar(title = "Đặt Bàn Trước", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Branch Card
                item {
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = PrimaryColor.copy(alpha = 0.08f))) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = PrimaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(SampleData.branch.name, fontWeight = FontWeight.Bold)
                                Text(SampleData.branch.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                // Số khách
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Số lượng khách", fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                IconButton(onClick = { if (pax > 1) onPaxChange(pax - 1) }) { Icon(Icons.Default.RemoveCircle, null, tint = PrimaryColor) }
                                Text("$pax người", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
                                IconButton(onClick = { if (pax < 20) onPaxChange(pax + 1) }) { Icon(Icons.Default.AddCircle, null, tint = PrimaryColor) }
                            }
                        }
                    }
                }

                // Ngày giờ
                item {
                    val calendar = Calendar.getInstance()
                    
                    // Date Picker Dialog
                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val cal = Calendar.getInstance()
                            cal.set(year, month, dayOfMonth)
                            onDateChange(displayDateFormat.format(cal.time))
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    )

                    // Time Picker Dialog
                    val timePickerDialog = android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            onTimeChange("%02d:%02d".format(hourOfDay, minute))
                        },
                        18, 0, true
                    )

                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Ngày & Giờ", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = selectedDate, 
                                    onValueChange = {}, 
                                    label = { Text("Ngày") }, 
                                    modifier = Modifier.weight(1f), 
                                    readOnly = true,
                                    trailingIcon = { 
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(Icons.Default.CalendarMonth, null, tint = PrimaryColor)
                                        }
                                    },
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        .also { interactionSource ->
                                            LaunchedEffect(interactionSource) {
                                                interactionSource.interactions.collect {
                                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                                        datePickerDialog.show()
                                                    }
                                                }
                                            }
                                        }
                                )
                                OutlinedTextField(
                                    value = selectedTime, 
                                    onValueChange = {}, 
                                    label = { Text("Giờ") }, 
                                    modifier = Modifier.weight(1f),
                                    readOnly = true,
                                    trailingIcon = { 
                                        IconButton(onClick = { timePickerDialog.show() }) {
                                            Icon(Icons.Default.AccessTime, null, tint = PrimaryColor)
                                        }
                                    },
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        .also { interactionSource ->
                                            LaunchedEffect(interactionSource) {
                                                interactionSource.interactions.collect {
                                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                                        timePickerDialog.show()
                                                    }
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                // Số điện thoại liên hệ
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Số điện thoại liên hệ", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phone, onValueChange = onPhoneChange,
                                placeholder = { Text("Nhập số điện thoại để nhà hàng liên hệ") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = PrimaryColor) },
                                singleLine = true
                            )
                        }
                    }
                }

                // Khu vực
                item {
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Khu vực ngồi", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TableZone.entries.forEach { zone ->
                                    FilterChip(
                                        selected = selectedZone == zone,
                                        onClick = { onZoneChange(zone) },
                                        label = { Text(zone.displayName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Ghi chú
                item {
                    OutlinedTextField(
                        value = specialRequest, onValueChange = onSpecialRequestChange,
                        label = { Text("Yêu cầu đặc biệt (không bắt buộc)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Tiếp theo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookingConfirmation(
    reservation: Reservation,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Tự động lắng nghe Webhook từ Firebase
    androidx.compose.runtime.DisposableEffect(reservation.id) {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("PaymentStatus")
            .document(reservation.id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "da_thanh_toan") {
                        android.widget.Toast.makeText(context, "Thanh toán thành công!", android.widget.Toast.LENGTH_SHORT).show()
                        onConfirm() // Trigger success screen navigation
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = { CustomerTopBar(title = "Xác Nhận Đặt Bàn", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Chi tiết đặt bàn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    BookingInfoRow(Icons.Default.Person, "Khách hàng", reservation.userName)
                    BookingInfoRow(Icons.Default.CalendarToday, "Thời gian", reservation.datetime)
                    BookingInfoRow(Icons.Default.Groups, "Số khách", "${reservation.pax} người")
                    BookingInfoRow(Icons.Default.LocationOn, "Khu vực", reservation.zone.displayName)
                    BookingInfoRow(Icons.Default.Payments, "Tiền cọc bắt buộc", "100,000 đ")
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mã QR Thanh Toán Tiền Cọc", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    Text("Vui lòng quét mã QR dưới đây để thanh toán 100,000 đ. Đơn đặt bàn sẽ được ghi nhận sau khi thanh toán.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    
                    val qrUrl = "https://img.vietqr.io/image/TPBANK-0775109883-compact2.png?amount=100000&addInfo=DATBAN%20${reservation.id}"
                    
                    Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)) {
                        coil.compose.AsyncImage(
                            model = qrUrl,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    Text("Chủ TK: LE VAN TRI\nNgân hàng: TP BANK", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), 
                        color = SuccessColor, 
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Hệ thống đang chờ nhận thanh toán...", 
                        fontWeight = FontWeight.Bold, 
                        color = SuccessColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = PrimaryColor, modifier = Modifier.size(20.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BookingSuccessScreen(reservation: Reservation, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(SuccessColor).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Đặt Bàn Thành Công!", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Cảm ơn bạn đã tin tưởng Gourmet Hub", color = Color.White.copy(0.8f))
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Về Trang Chủ", color = SuccessColor, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────
// CUSTOMER PROFILE SCREEN
// ─────────────────────────────────

@Composable
fun CustomerProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onLogout: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }
    var showReservations by remember { mutableStateOf(false) }

    if (showReservations) {
        MyReservationsScreen(onBack = { showReservations = false })
        return
    }

    Scaffold(
        topBar = { CustomerTopBar(title = "Hồ Sơ Cá Nhân", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PrimaryColor, PrimaryVariant))).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        when (val state = profileState) {
                            is ProfileUiState.Loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            is ProfileUiState.Success -> {
                                Text(state.profile.fullName, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(state.profile.email, color = Color.White.copy(0.8f))
                            }
                            is ProfileUiState.Error -> Text("Chào mừng bạn!", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profileState is ProfileUiState.Success) {
                        ProfileMenuItem(Icons.Default.History, "Lịch sử đơn hàng", "Xem các đơn hàng đã đặt", onClick = onNavigateToOrders)
                        ProfileMenuItem(Icons.Default.TableBar, "Đặt bàn của tôi", "Quản lý lịch hẹn", onClick = { showReservations = true })
                        ProfileMenuItem(Icons.Default.LocationOn, "Địa chỉ giao hàng", "Sửa địa chỉ mặc định")
                        ProfileMenuItem(Icons.Default.CreditCard, "Phương thức thanh toán", "Quản lý thẻ, ví")
                    }

                    ProfileMenuItem(Icons.Default.Notifications, "Thông báo", "Cập nhật khuyến mãi")
                    ProfileMenuItem(Icons.Default.Help, "Hỗ trợ & Góp ý", "Liên hệ với chúng tôi")

                    if (profileState is ProfileUiState.Success) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileMenuItem(
                            icon          = Icons.Default.Logout,
                            title         = "Đăng xuất",
                            subtitle      = "Thoát tài khoản hiện tại",
                            isDestructive = true,
                            onClick       = {
                                viewModel.logout()
                                onLogout()
                            }
                        )
                    } else if (profileState is ProfileUiState.Error) {
                        ProfileMenuItem(
                            icon    = Icons.Default.Login,
                            title   = "Đăng nhập",
                            subtitle = null,
                            onClick = onNavigateToLogin
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(onClick = onClick, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = if (isDestructive) Color.Red else PrimaryColor)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = if (isDestructive) Color.Red else Color.Unspecified, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable
fun MyReservationsScreen(onBack: () -> Unit) {
    var reservations by remember { mutableStateOf<List<ReservationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReservation by remember { mutableStateOf<ReservationItem?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val userId = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch(e: Exception) { null }

    // Firebase Realtime Listener
    androidx.compose.runtime.DisposableEffect(Unit) {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("kitchen_updates")
            .document("latest")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null && snapshot.exists()) {
                    refreshTrigger++
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(userId, refreshTrigger) {
        if (userId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
            val response = withTimeoutOrNull(10000) { apiService.getReservations(userId) }
            if (response?.isSuccessful == true) reservations = response.body()?.data ?: emptyList()
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    Scaffold(topBar = { CustomerTopBar(title = "Lịch Đặt Bàn", onBack = onBack) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryColor) }
        else {
            if (reservations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có lịch đặt bàn nào", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reservations) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { selectedReservation = item }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.datetime, fontWeight = FontWeight.Bold)
                                    val statusVN = when(item.statusStr.uppercase()) {
                                        "PENDING" -> "Chưa đặt cọc"
                                        "CONFIRMED", "DA_THANH_TOAN" -> "Đã đặt cọc"
                                        "DA_XEP_BAN" -> "Đã nhận bàn"
                                        "COMPLETED" -> "Hoàn thành"
                                        "CANCELLED" -> "Đã hủy"
                                        else -> item.statusStr
                                    }
                                    val statusColor = when(item.statusStr.uppercase()) {
                                        "PENDING" -> Color(0xFFE6A23C)
                                        "CONFIRMED", "DA_THANH_TOAN" -> Color(0xFF409EFF)
                                        "DA_XEP_BAN" -> Color(0xFF9C27B0)
                                        "COMPLETED" -> SuccessColor
                                        "CANCELLED" -> Color.Red
                                        else -> PrimaryColor
                                    }
                                    Text(statusVN, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("${item.pax} người - ${item.zone}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedReservation != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedReservation = null },
            confirmButton = {
                TextButton(onClick = { selectedReservation = null }) { Text("Đóng", fontWeight = FontWeight.Bold) }
            },
            title = { Text("Chi Tiết Đặt Bàn", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Mã đặt bàn: ${selectedReservation!!.id}", fontWeight = FontWeight.Bold, color = PrimaryColor)
                    
                    if (selectedReservation!!.statusStr.uppercase() == "CONFIRMED" || selectedReservation!!.statusStr.uppercase() == "DA_THANH_TOAN") {
                        Card(
                            modifier = Modifier.padding(16.dp).size(200.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                coil.compose.AsyncImage(
                                    model = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${selectedReservation!!.id}",
                                    contentDescription = "Reservation QR",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Text("Vui lòng đưa mã này cho nhân viên để xác nhận", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    } else {
                        Text("Trạng thái: ${
                            when(selectedReservation!!.statusStr.uppercase()) {
                                "PENDING" -> "Chưa đặt cọc"
                                "CANCELLED" -> "Đã hủy"
                                else -> selectedReservation!!.statusStr
                            }
                        }", color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Chưa hoàn tất thanh toán cọc hoặc đang xử lý.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }

                    HorizontalDivider()
                    BookingInfoRow(Icons.Default.Person, "Khách hàng", selectedReservation!!.userName)
                    BookingInfoRow(Icons.Default.CalendarToday, "Thời gian", selectedReservation!!.datetime)
                    BookingInfoRow(Icons.Default.Groups, "Số khách", "${selectedReservation!!.pax} người")
                    BookingInfoRow(Icons.Default.LocationOn, "Khu vực", selectedReservation!!.zone)
                }
            }
        )
    }
}
