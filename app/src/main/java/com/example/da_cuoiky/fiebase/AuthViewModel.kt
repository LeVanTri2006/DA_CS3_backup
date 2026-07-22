package com.example.da_cuoiky.fiebase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repo = AuthRepoesitory()

    // ── State cho profile
    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState

    // ── Đăng nhập
    private var loginJob: kotlinx.coroutines.Job? = null

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        if (loginJob?.isActive == true) {
            android.util.Log.d("LOGIN_TRACE", "Login already in progress, ignoring duplicate call")
            return
        }
        
        android.util.Log.d("LOGIN_TRACE", "ViewModel login started")
        loginJob = viewModelScope.launch {
            try {
                val result = repo.login(email, password)
                if (result.isSuccess) {
                    val roleStr = result.getOrNull() ?: "role:customer"
                    val role = roleStr.removePrefix("role:")
                    
                    if (role == "customer") {
                        loadUserProfile()
                    }
                    onResult(true, role)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Email hoặc mật khẩu không đúng!"
                    onResult(false, errorMsg)
                }
            } finally {
                android.util.Log.d("LOGIN_TRACE", "ViewModel login finished")
            }
        }
    }

    // ── Đăng ký
    fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repo.register(email, password, fullName, phone)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val msg = when {
                    result.exceptionOrNull()?.message
                        ?.contains("email address is already in use") == true ->
                        "Email này đã được đăng ký rồi!"
                    result.exceptionOrNull()?.message
                        ?.contains("badly formatted") == true ->
                        "Email không đúng định dạng!"
                    result.exceptionOrNull()?.message
                        ?.contains("Password should be at least") == true ->
                        "Mật khẩu phải có ít nhất 6 ký tự!"
                    else -> "Đăng ký thất bại: ${result.exceptionOrNull()?.message}"
                }
                onResult(false, msg)
            }
        }
    }

    // ── Tải thông tin profile từ Firestore
    fun loadUserProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            val result = repo.getUserProfile()
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                // Fetch số đơn hàng để xác định hạng thành viên
                val totalOrders = try {
                    val uid = profile.uid
                    val api = com.example.da_cuoiky.network.RetrofitClient.instance
                    val response = api.getOrders(uid)
                    if (response.isSuccessful) response.body()?.data?.size ?: 0 else 0
                } catch (e: Exception) { 0 }
                _profileState.value = ProfileUiState.Success(profile, totalOrders)
            } else {
                _profileState.value = ProfileUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi không xác định")
            }
        }
    }

    // ── Đăng nhập / đăng ký bằng Google
    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.signInWithGoogle(idToken)
            if (result.isSuccess) {
                loadUserProfile()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Đăng nhập Google thất bại!")
            }
        }
    }

    // ── Đăng nhập / đăng ký bằng Facebook
    fun signInWithFacebook(accessToken: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.signInWithFacebook(accessToken)
            if (result.isSuccess) {
                loadUserProfile()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Đăng nhập Facebook thất bại!")
            }
        }
    }

    // ── Gửi email đặt lại mật khẩu
    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repo.resetPassword(email)
            if (result.isSuccess) {
                onResult(true, null)
            } else {
                val msg = when {
                    result.exceptionOrNull()?.message
                        ?.contains("no user record") == true ||
                    result.exceptionOrNull()?.message
                        ?.contains("There is no user") == true ->
                        "Email này chưa được đăng ký!"
                    result.exceptionOrNull()?.message
                        ?.contains("badly formatted") == true ->
                        "Email không đúng định dạng!"
                    else -> "Gửi email thất bại. Vui lòng thử lại!"
                }
                onResult(false, msg)
            }
        }
    }

    fun logout() = repo.logout()
    fun isLoggedIn() = repo.getCurrenUser() != null
}

// ── UI State cho màn Profile
sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile, val totalOrders: Int = 0) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}