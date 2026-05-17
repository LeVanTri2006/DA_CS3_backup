package com.example.da_cuoiky.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.da_cuoiky.R
import com.example.da_cuoiky.fiebase.AuthViewModel
import com.example.da_cuoiky.model.UserRole
import com.example.da_cuoiky.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

// ── Màu sắc chủ đạo ──────────────────────────────────────────────────────────
private val GradientStart   = Color(0xFFFF7043)   // cam đậm
private val GradientMid     = Color(0xFFE53935)   // đỏ tươi
private val GradientEnd     = Color(0xFF8B0000)   // đỏ đậm
private val CardBg          = Color(0xFFFFFBFF)
private val TextPrimary     = Color(0xFF1A1A2E)
private val TextSecondary   = Color(0xFF6B7280)
private val FieldBorder     = Color(0xFFE5E7EB)
private val SuccessGreen    = Color(0xFF059669)
private val ErrorRed        = Color(0xFFDC2626)

@Composable
fun LoginScreen(
    navController : NavController,
    viewModel : AuthViewModel,
    onRoleSelected: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit = {}
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError         by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // ── Google Sign-In setup ─────────────────────────────────────────
    val context = LocalContext.current
    var googleLoading by remember { mutableStateOf(false) }

    // Web Client ID lấy từ Firebase Console → Project Settings → Your apps → Web
    val webClientId = context.getString(R.string.default_web_client_id)

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken  = account?.idToken
            if (idToken != null) {
                googleLoading = true
                viewModel.signInWithGoogle(idToken) { success, errorMsg ->
                    googleLoading = false
                    if (success) {
                        navController.navigate(Screen.CustomerMain.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        feedbackMessage = errorMsg ?: "Đăng nhập Google thất bại!"
                        isError = true
                    }
                }
            } else {
                feedbackMessage = "Không lấy được token từ Google."
                isError = true
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) { // 12501 = user đóng dialog, không hiện lỗi
                feedbackMessage = "Đăng nhập Google thất bại (mã ${e.statusCode})"
                isError = true
            }
        }
    }

    // ── Facebook Sign-In setup ────────────────────────────────────────────────
    var facebookLoading by remember { mutableStateOf(false) }
    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(Unit) {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    facebookLoading = true
                    val token = loginResult.accessToken.token
                    viewModel.signInWithFacebook(token) { success, errorMsg ->
                        facebookLoading = false
                        if (success) {
                            navController.navigate(Screen.CustomerMain.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            feedbackMessage = errorMsg ?: "Đăng nhập Facebook thất bại!"
                            isError = true
                        }
                    }
                }
                override fun onCancel() { 
                    facebookLoading = false
                }
                override fun onError(error: FacebookException) {
                    facebookLoading = false
                    feedbackMessage = "Đăng nhập Facebook thất bại: ${error.message}"
                    isError = true
                }
            }
        )
        onDispose { LoginManager.getInstance().unregisterCallback(callbackManager) }
    }

    // ── Animate logo scale on entry ────────────────────────────────────────
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )

    // ── Pulse animation for the logo ring ─────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val config = LocalConfiguration.current
    val isSmall = config.screenWidthDp < 380

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
    ) {
        // ── Nút quay lại (cho khách vãng lai) ─────────────────────────────
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quay lại",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp)
            )
        }

        // ── Decorative blurred circles ─────────────────────────────────────
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .alpha(0.25f)
                .blur(60.dp)
                .background(Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .alpha(0.20f)
                .blur(50.dp)
                .background(Color(0xFFFFD700), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isSmall) 16.dp else 20.dp, vertical = if (isSmall) 24.dp else 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo / Icon area ─────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(if (isSmall) 90.dp else 110.dp)
            ) {
                // Pulse ring
                Box(
                    modifier = Modifier
                        .size(if (isSmall) 90.dp else 110.dp)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .background(Color.White, CircleShape)
                )
                // Solid white circle
                Box(
                    modifier = Modifier
                        .size(if (isSmall) 70.dp else 90.dp)
                        .scale(logoScale)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(if (isSmall) 36.dp else 44.dp),
                        tint = GradientMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isSmall) 12.dp else 20.dp))

            // ── App name ──────────────────────────────────────────────────
            Text(
                text = "🍽️ Gourmet Hub",
                style = TextStyle(
                    fontSize = if (isSmall) 24.sp else 30.sp,
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
                text = "Hệ thống quản lý nhà hàng",
                fontSize = if (isSmall) 12.sp else 14.sp,
                color = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(if (isSmall) 24.dp else 36.dp))

            // ── Glassmorphism Card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isSmall) 16.dp else 24.dp, vertical = if (isSmall) 24.dp else 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đăng Nhập",
                        fontSize = if (isSmall) 20.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Chào mừng trở lại! Vui lòng đăng nhập.",
                        fontSize = if (isSmall) 12.sp else 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = if (isSmall) 16.dp else 24.dp)
                    )

                    // ── Email field ──────────────────────────────────────
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("staff@test.com hoặc user@test.com", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (email.isNotEmpty()) GradientMid else TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GradientMid,
                            unfocusedBorderColor = FieldBorder,
                            focusedLabelColor    = GradientMid,
                            cursorColor          = GradientMid
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // ── Password field ───────────────────────────────────
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (password.isNotEmpty()) GradientMid else TextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GradientMid,
                            unfocusedBorderColor = FieldBorder,
                            focusedLabelColor    = GradientMid,
                            cursorColor          = GradientMid
                        )
                    )

                    // ── Quên mật khẩu ────────────────────────────────────
                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Quên mật khẩu?",
                            color = GradientMid,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // ── Dialog quên mật khẩu ─────────────────────────────
                    if (showForgotDialog) {
                        ForgotPasswordDialog(
                            prefillEmail = email,   // tự điền nếu user đã gõ email
                            viewModel    = viewModel,
                            onDismiss    = { showForgotDialog = false }
                        )
                    }

                    // ── Feedback message ─────────────────────────────────
                    AnimatedVisibility(
                        visible = feedbackMessage != null,
                        enter   = fadeIn() + slideInVertically(),
                        exit    = fadeOut()
                    ) {
                        feedbackMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isError) ErrorRed.copy(alpha = 0.10f)
                                        else SuccessGreen.copy(alpha = 0.10f)
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
                                    color = if (isError) ErrorRed else SuccessGreen,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Nút Đăng Nhập ────────────────────────────────────
                    Button(
                        onClick = {
                            android.util.Log.d("LOGIN_TRACE", "LoginScreen button clicked - Email: $email")
                            if (email.isBlank() || password.isBlank()) {
                                feedbackMessage = "Vui lòng nhập đầy đủ email và mật khẩu"
                                isError = true
                                return@Button
                            }
                            isLoading = true
                            feedbackMessage = null
                            isError = false
                            
                            viewModel.login(email, password) { success, role ->
                                if (success) {
                                    // Không set isLoading = false để tránh user bấm lại trong khi đang chuyển màn
                                    android.util.Log.d("LOGIN_TRACE", "Login success, navigating to: $role")
                                    
                                    val destination = when (role) {
                                        "staff", "admin", "manager" -> Screen.StaffFloorPlan.route
                                        "kitchen" -> Screen.StaffKitchen.route
                                        else -> Screen.CustomerMain.route
                                    }
                                    
                                    navController.navigate(destination) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    android.util.Log.d("LOGIN_TRACE", "Login failed: $role")
                                    isLoading = false
                                    feedbackMessage = role
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSmall) 48.dp else 54.dp),
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
                                        Brush.horizontalGradient(listOf(GradientStart, GradientMid, GradientEnd))
                                    else
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Đang đăng nhập...", color = Color.White, fontSize = 15.sp)
                                }
                            } else {
                                Text(
                                    text = "Đăng Nhập",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Divider ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                        Text(
                            text = "  hoặc tiếp tục với  ",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Social Buttons ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Xóa session cũ (tránh auto-sign-in lại account cũ)
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isSmall) 46.dp else 50.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading && !googleLoading,
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF9FAFB)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.google_logo),
                                    contentDescription = "Google Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Google", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                (context as? ActivityResultRegistryOwner)?.let { registryOwner ->
                                    facebookLoading = true
                                    LoginManager.getInstance().logInWithReadPermissions(
                                        registryOwner,
                                        callbackManager,
                                        listOf("email", "public_profile")
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isSmall) 46.dp else 50.dp),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading && !googleLoading && !facebookLoading,
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF9FAFB)
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.facebook_logo),
                                    contentDescription = "Facebook Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Facebook", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Đăng ký ───────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Chưa có tài khoản?",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Đăng ký ngay",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
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

// ── Dialog Quên Mật Khẩu ─────────────────────────────────────────────────────
@Composable
fun ForgotPasswordDialog(
    prefillEmail: String = "",
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var resetEmail   by remember { mutableStateOf(prefillEmail) }
    var isLoading    by remember { mutableStateOf(false) }
    var resultMsg    by remember { mutableStateOf<String?>(null) }
    var isSuccess    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape            = RoundedCornerShape(20.dp),
        icon             = {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint   = GradientMid,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text       = "Quên mật khẩu?",
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isSuccess) {
                    Text(
                        text  = "Nhập email đã đăng ký. Chúng tôi sẽ gửi link đặt lại mật khẩu ngay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value         = resetEmail,
                        onValueChange = { resetEmail = it; resultMsg = null },
                        label         = { Text("Email") },
                        leadingIcon   = { Icon(Icons.Default.Email, null, tint = GradientMid) },
                        singleLine    = true,
                        enabled       = !isLoading,
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GradientMid,
                            focusedLabelColor    = GradientMid,
                            cursorColor          = GradientMid
                        )
                    )
                }

                // Kết quả
                resultMsg?.let { msg ->
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.Check
                                          else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text  = msg,
                            color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Loading
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color    = GradientMid
                    )
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                // Đã gửi xong → chỉ hiện nút Đóng
                TextButton(onClick = onDismiss) {
                    Text("Đóng", fontWeight = FontWeight.Bold, color = GradientMid)
                }
            } else {
                TextButton(
                    enabled = !isLoading && resetEmail.isNotBlank(),
                    onClick = {
                        isLoading = true
                        resultMsg = null
                        viewModel.resetPassword(resetEmail.trim()) { success, errorMsg ->
                            isLoading = false
                            isSuccess = success
                            resultMsg = if (success)
                                "✅ Email đã được gửi! Kiểm tra hộp thư (kể cả Spam)."
                            else
                                errorMsg
                        }
                    }
                ) {
                    Text("Gửi link", fontWeight = FontWeight.Bold, color = GradientMid)
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(
                    onClick  = onDismiss,
                    enabled  = !isLoading
                ) {
                    Text("Huỷ", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    )
}
