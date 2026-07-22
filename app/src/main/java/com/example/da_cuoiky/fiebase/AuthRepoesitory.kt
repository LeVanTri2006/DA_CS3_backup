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

    // ── Đăng nhập
    suspend fun login(email: String, password: String): Result<String> {
        android.util.Log.d("LOGIN_TRACE", "Repository login called")
        
        //  Thử loginStaff trước (Cho nhân viên)
        val staffResult = loginStaff(email, password)
        
        // Nếu lỗi mạng thật sự → dừng lại ngay, không thử Firebase
        if (staffResult.exceptionOrNull()?.message == "NETWORK_ERROR") {
            return Result.failure(Exception("Lỗi kết nối mạng, vui lòng thử lại!"))
        }

        if (staffResult.isSuccess) {
            return staffResult
        }
        
        // Nếu không phải staff (hoặc staff API trả về thất bại), thử loginFirebase (Cho khách hàng)
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

    // ── Đăng ký
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

    // ── Lấy thông tin profile từ API PHP (Thay vì chỉ Firestore)
    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Chưa đăng nhập")
            
            val apiService = com.example.da_cuoiky.network.RetrofitClient.instance
            
            //  Gọi API get_profile từ server PHP để lấy thông tin + hạng
            val response = withTimeout(5000) {
                apiService.getProfile(uid)
            }
            
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    val profile = UserProfile(
                        uid      = data.uid,
                        fullName = data.name,
                        email    = data.email,
                        phone    = data.phone ?: "",
                        role     = "customer",
                        rankData = data.rankData
                    )
                    return Result.success(profile)
                }
            }
            
            // Fallback nếu API PHP lỗi: lấy từ Firebase
            val user = auth.currentUser
            if (user != null) {
                Result.success(UserProfile(uid = user.uid, fullName = user.displayName ?: "Khách hàng", email = user.email ?: ""))
            } else {
                Result.failure(Exception("Lỗi lấy thông tin tài khoản"))
            }
        } catch (e: Exception) {
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

    // ── Đăng nhập / đăng ký bằng Google
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

    // ── Gửi email đặt lại mật khẩu
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Đăng nhập bằng Facebook
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
                // Đọc thông tin từ Firestore để có đầy đủ tên và sđt (vì FirebaseUser có thể bị thiếu)
                val doc = db.collection("users").document(user.uid).get().await()
                val fullName = doc.getString("fullName") ?: user.displayName ?: "Khách hàng"
                val phone = doc.getString("phone") ?: user.phoneNumber
                
                val syncRequest = com.example.da_cuoiky.model.SyncUserRequest(
                    uid = user.uid,
                    name = fullName,
                    email = user.email ?: "",
                    phone = phone
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
    val role: String     = "customer",
    val rankData: com.example.da_cuoiky.model.RankData? = null
)
