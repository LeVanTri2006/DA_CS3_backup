package com.example.da_cuoiky.fiebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class AuthRepoesitory {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Đăng nhập ─────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String): Result<String> {
        android.util.Log.d("LOGIN_TRACE", "Repository login called")
        
        // Bước 1: Thử loginStaff trước (Cho nhân viên)
        val staffResult = loginStaff(email, password)
        
        // Nếu lỗi mạng thật sự → dừng lại ngay, không thử Firebase
        if (staffResult.exceptionOrNull()?.message == "NETWORK_ERROR") {
            return Result.failure(Exception("Lỗi kết nối mạng, vui lòng thử lại!"))
        }

        if (staffResult.isSuccess) {
            return staffResult
        }
        
        // Bước 2: Nếu không phải staff (hoặc staff API trả về thất bại), thử loginFirebase (Cho khách hàng)
        return loginFirebase(email, password)
    }

    private suspend fun loginStaff(email: String, password: String): Result<String> {
        return try {
            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
            val request = com.example.da_cuoiky.model.StaffLoginRequest(email, password)
            val response = apiService.loginStaff(request)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                val role = response.body()?.data?.role ?: "staff"
                Result.success("role:$role")
            } else {
                // Đánh dấu rõ: Đây không phải là tài khoản nhân viên
                Result.failure(Exception("NOT_STAFF"))
            }
        } catch (e: java.net.UnknownHostException) {
            // Lỗi mạng: Mất kết nối, DNS...
            Result.failure(Exception("NETWORK_ERROR"))
        } catch (e: Exception) {
            // Các lỗi khác (timeout, server error...)
            Result.failure(Exception("NOT_STAFF"))
        }
    }

    private suspend fun loginFirebase(email: String, password: String): Result<String> {
        return try {
            android.util.Log.d("LOGIN_TRACE", "Firebase signIn called")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User not found")
            syncWithPHP(user)
            Result.success("role:customer")
        } catch (e: Exception) {
            Result.failure(Exception("Sai tài khoản hoặc mật khẩu!"))
        }
    }

    // ── Đăng ký ───────────────────────────────────────────────────────────────
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String
    ): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: throw Exception("UID not found")

            val userDoc = hashMapOf(
                "uid"       to uid,
                "fullName"  to fullName,
                "email"     to email,
                "phone"     to phone,
                "role"      to "customer",
                "createdAt" to Timestamp.now()
            )
            db.collection("users").document(uid).set(userDoc).await()
            syncWithPHP(result.user!!)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Lấy thông tin profile từ Firestore (Thêm Timeout 5s) ────────────────────
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Chưa đăng nhập")
            
            // ✅ FIX: Thêm timeout để không bị treo nếu Firestore không phản hồi
            val doc = withTimeout(5000) {
                db.collection("users").document(uid).get().await()
            }
            
            val profile = UserProfile(
                uid      = uid,
                fullName = doc.getString("fullName") ?: auth.currentUser?.displayName ?: "Người dùng",
                email    = doc.getString("email")    ?: auth.currentUser?.email ?: "",
                phone    = doc.getString("phone")    ?: auth.currentUser?.phoneNumber ?: "",
                role     = doc.getString("role")     ?: "customer"
            )
            Result.success(profile)
        } catch (e: Exception) {
            // Nếu lỗi/timeout, trả về thông tin tối thiểu từ Firebase Auth
            val user = auth.currentUser
            if (user != null) {
                Result.success(UserProfile(uid = user.uid, fullName = user.displayName ?: "Khách hàng", email = user.email ?: ""))
            } else {
                Result.failure(e)
            }
        }
    }

    fun logout() = auth.signOut()
    fun getCurrenUser(): FirebaseUser? = auth.currentUser

    // ── Đăng nhập / đăng ký bằng Google ─────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user   = result.user ?: throw Exception("User not found")

            val isNewUser = result.additionalUserInfo?.isNewUser == true
            if (isNewUser) {
                val userDoc = hashMapOf(
                    "uid"       to user.uid,
                    "fullName"  to (user.displayName ?: ""),
                    "email"     to (user.email ?: ""),
                    "phone"     to (user.phoneNumber ?: ""),
                    "role"      to "customer",
                    "provider"  to "google",
                    "createdAt" to Timestamp.now()
                )
                db.collection("users").document(user.uid).set(userDoc).await()
            }
            syncWithPHP(user)
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Gửi email đặt lại mật khẩu ─────────────────────────────────────────────
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Đăng nhập bằng Facebook ────────────────────────────────────────
    suspend fun signInWithFacebook(accessToken: String): Result<String> {
        return try {
            val credential = FacebookAuthProvider.getCredential(accessToken)
            val result = auth.signInWithCredential(credential).await()
            val user   = result.user ?: throw Exception("User not found")

            val isNewUser = result.additionalUserInfo?.isNewUser == true
            if (isNewUser) {
                val userDoc = hashMapOf(
                    "uid"       to user.uid,
                    "fullName"  to (user.displayName ?: ""),
                    "email"     to (user.email ?: ""),
                    "phone"     to (user.phoneNumber ?: ""),
                    "role"      to "customer",
                    "provider"  to "facebook",
                    "createdAt" to Timestamp.now()
                )
                db.collection("users").document(user.uid).set(userDoc).await()
            }
            syncWithPHP(user)
            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncWithPHP(user: FirebaseUser) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val syncRequest = com.example.da_cuoiky.model.SyncUserRequest(
                    uid = user.uid,
                    name = user.displayName ?: "Khách hàng",
                    email = user.email ?: "",
                    phone = user.phoneNumber
                )
                val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
                apiService.syncUserToMySQL(syncRequest)
            } catch (e: Exception) {
                android.util.Log.e("SYNC", "Lỗi đồng bộ: ${e.message}")
            }
        }
    }
}

data class UserProfile(
    val uid: String      = "",
    val fullName: String = "",
    val email: String    = "",
    val phone: String    = "",
    val role: String     = "customer"
)
