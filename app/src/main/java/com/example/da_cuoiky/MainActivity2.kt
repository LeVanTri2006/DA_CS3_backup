package com.example.da_cuoiky

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity2 : ComponentActivity() {
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        val email = "ltri50647@gmail.com"
        val password = "Tri12345@" // Sửa lỗi chính tả từ passwork thành password

        // Đăng ký người dùng mới bằng Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Thành công: Cập nhật UI với thông tin người dùng
                    Log.d("Main", "createUserWithEmail:success")
                    val user: FirebaseUser? = mAuth.currentUser

                    Toast.makeText(
                        applicationContext, 
                        "Đăng ký thành công: ${user?.email}", 
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Thất bại: Hiển thị thông báo lỗi
                    Log.w("Main", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this@MainActivity2, 
                        "Xác thực thất bại: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}
