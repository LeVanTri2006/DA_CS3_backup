package com.example.da_cuoiky.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.da_cuoiky.fiebase.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Màu sắc đồng bộ với LoginScreen ──────────────────────────────────────────────────
private val RegGradientStart  = Color(0xFFFF7043)   // cam đậm
private val RegGradientMid    = Color(0xFFE53935)   // đỏ tươi
private val RegGradientEnd    = Color(0xFF8B0000)   // đỏ đậm
private val RegCardBg         = Color(0xFFFFFBFF)
private val RegTextPrimary    = Color(0xFF1A1A2E)
private val RegTextSecondary  = Color(0xFF6B7280)
private val RegFieldBorder    = Color(0xFFE5E7EB)
private val RegSuccessGreen   = Color(0xFF059669)
private val RegErrorRed       = Color(0xFFDC2626)

// ── Password strength colors ──────────────────────────────────────────────────
private val StrengthWeak      = Color(0xFFDC2626)
private val StrengthMedium    = Color(0xFFF59E0B)
private val StrengthStrong    = Color(0xFF059669)

private fun evaluateStrength(pwd: String): Int {
    var score = 0
    if (pwd.length >= 8)                         score++
    if (pwd.any { it.isUpperCase() })            score++
    if (pwd.any { it.isDigit() })                score++
    if (pwd.any { "!@#\$%^&*()_+-=[]{}|;':\",./<>?".contains(it) }) score++
    return score // 0-4
}

private fun strengthLabel(score: Int) = when {
    score <= 1 -> "Yếu"
    score <= 2 -> "Trung bình"
    score <= 3 -> "Khá mạnh"
    else       -> "Mạnh"
}

private fun strengthColor(score: Int) = when {
    score <= 1 -> StrengthWeak
    score <= 2 -> StrengthMedium
    else       -> StrengthStrong
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    var fullName         by remember { mutableStateOf("") }
    var email            by remember { mutableStateOf("") }
    var phone            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var confirmVisible   by remember { mutableStateOf(false) }
    var agreedToTerms    by remember { mutableStateOf(false) }
    var isLoading        by remember { mutableStateOf(false) }
    var feedbackMessage  by remember { mutableStateOf<String?>(null) }
    var isError          by remember { mutableStateOf(false) }

    val scope       = rememberCoroutineScope()
    val pwdStrength = evaluateStrength(password)

    // ── Pulse animation cho logo ───────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )

    // ── Validation helper ──────────────────────────────────────────────────
    fun validate(): String? {
        if (fullName.isBlank())              return "Vui lòng nhập họ tên."
        if (email.isBlank() || !email.contains("@")) return "Email không hợp lệ."
        if (phone.isBlank())                 return "Vui lòng nhập số điện thoại."
        if (password.length < 6)             return "Mật khẩu phải có ít nhất 6 ký tự."
        if (password != confirmPassword)     return "Mật khẩu xác nhận không khớp."
        if (!agreedToTerms)                  return "Bạn cần đồng ý với điều khoản sử dụng."
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(RegGradientStart, RegGradientMid, RegGradientEnd)
                )
            )
    ) {
        // ── Trang trí hình tròn mờ ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-70).dp, y = (-70).dp)
                .alpha(0.22f)
                .blur(60.dp)
                .background(Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .alpha(0.18f)
                .blur(55.dp)
                .background(Color(0xFFFFD700), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Nút quay lại ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToLogin,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Quay lại đăng nhập",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Logo ──────────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = RegGradientMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🍽️ Gourmet Hub",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(0f, 4f),
                        blurRadius = 8f
                    )
                )
            )
            Text(
                text = "Tạo tài khoản mới",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Main Card ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đăng Ký",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = RegTextPrimary
                    )
                    Text(
                        text = "Điền thông tin để tạo tài khoản",
                        fontSize = 13.sp,
                        color = RegTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
                    )

                    // ── Họ và tên ─────────────────────────────────────────
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Họ và tên") },
                        placeholder = { Text("Nguyễn Văn A", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person, null,
                                tint = if (fullName.isNotEmpty()) RegGradientMid else RegTextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Email ─────────────────────────────────────────────
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("example@email.com", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, null,
                                tint = if (email.isNotEmpty()) RegGradientMid else RegTextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Số điện thoại ─────────────────────────────────────
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại") },
                        placeholder = { Text("0901 234 567", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone, null,
                                tint = if (phone.isNotEmpty()) RegGradientMid else RegTextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Mật khẩu ─────────────────────────────────────────
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, null,
                                tint = if (password.isNotEmpty()) RegGradientMid else RegTextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = RegTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = fieldColors()
                    )

                    // ── Password strength indicator ────────────────────────
                    AnimatedVisibility(visible = password.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(4) { index ->
                                    val filled = index < pwdStrength
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (filled) strengthColor(pwdStrength)
                                                else RegFieldBorder
                                            )
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Độ mạnh mật khẩu:",
                                    fontSize = 11.sp,
                                    color = RegTextSecondary
                                )
                                Text(
                                    text = strengthLabel(pwdStrength),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = strengthColor(pwdStrength)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Xác nhận mật khẩu ────────────────────────────────
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LockReset, null,
                                tint = when {
                                    confirmPassword.isEmpty()          -> RegTextSecondary
                                    confirmPassword == password        -> RegSuccessGreen
                                    else                               -> RegErrorRed
                                }
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // match indicator
                                if (confirmPassword.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (confirmPassword == password) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (confirmPassword == password) RegSuccessGreen else RegErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                    Icon(
                                        imageVector = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = RegTextSecondary
                                    )
                                }
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Điều khoản ────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { agreedToTerms = it },
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor   = RegGradientMid,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = "Tôi đồng ý với ",
                            fontSize = 13.sp,
                            color = RegTextSecondary
                        )
                        TextButton(
                            onClick = { /* Mở trang điều khoản */ },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Điều khoản & Chính sách",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RegGradientMid
                            )
                        }
                    }

                    // ── Feedback message ──────────────────────────────────
                    AnimatedVisibility(
                        visible = feedbackMessage != null,
                        enter   = fadeIn() + slideInVertically(),
                        exit    = fadeOut()
                    ) {
                        feedbackMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isError) RegErrorRed.copy(alpha = 0.10f)
                                        else RegSuccessGreen.copy(alpha = 0.10f)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isError) "⚠️" else "✅",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = msg,
                                    color = if (isError) RegErrorRed else RegSuccessGreen,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Nút Đăng Ký ──────────────────────────────────────
                    Button(
                        onClick = {
                            val error = validate()
                            if (error != null) {
                                feedbackMessage = error
                                isError = true
                                return@Button
                            }
                            isLoading = true
                            feedbackMessage = null
                            isError = false

                            // ✅ Gọi Firebase Auth thật sự
                            viewModel.register(
                                email    = email,
                                password = password,
                                fullName = fullName,
                                phone    = phone
                            ) { success, errorMsg ->
                                isLoading = false
                                if (success) {
                                    feedbackMessage = "Đăng ký thành công! Chào mừng bạn đến với Gourmet Hub 🎉"
                                    isError = false
                                    // Chờ 1.5s để user đọc thông báo rồi mới chuyển màn
                                    scope.launch {
                                        delay(1500)
                                        onRegisterSuccess()
                                    }
                                } else {
                                    feedbackMessage = errorMsg
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (!isLoading)
                                        Brush.horizontalGradient(
                                            listOf(RegGradientStart, RegGradientMid, RegGradientEnd)
                                        )
                                    else
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier  = Modifier.size(22.dp),
                                        color     = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Đang tạo tài khoản...", color = Color.White, fontSize = 15.sp)
                                }
                            } else {
                                Text(
                                    text          = "Tạo Tài Khoản",
                                    color         = Color.White,
                                    fontSize      = 16.sp,
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Steps indicator ───────────────────────────────────
                    RegisterStepsHint()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Đã có tài khoản ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Đã có tài khoản?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                TextButton(onClick = onBackToLogin, enabled = !isLoading) {
                    Text(
                        text          = "Đăng nhập ngay",
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "© 2026 Gourmet Hub. All rights reserved.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Màu cho OutlinedTextField ─────────────────────────────────────────────────
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = RegGradientMid,
    unfocusedBorderColor = RegFieldBorder,
    focusedLabelColor    = RegGradientMid,
    cursorColor          = RegGradientMid
)

// ── Gợi ý bước đăng ký ───────────────────────────────────────────────────────
@Composable
private fun RegisterStepsHint() {
    HorizontalDivider(color = RegFieldBorder, modifier = Modifier.padding(vertical = 8.dp))

    Text(
        text = "Sau khi đăng ký, bạn có thể:",
        fontSize = 12.sp,
        color = RegTextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val tips = listOf(
        Icons.Default.RestaurantMenu to "Đặt bàn & xem thực đơn online",
        Icons.Default.Loyalty        to "Tích điểm và nhận ưu đãi thành viên",
        Icons.Default.Notifications  to "Nhận thông báo đơn hàng thời gian thực"
    )

    tips.forEach { (icon, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(RegGradientStart, RegGradientMid)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector     = icon,
                    contentDescription = null,
                    tint            = Color.White,
                    modifier        = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(text = label, fontSize = 12.sp, color = RegTextSecondary)
        }
    }
}
