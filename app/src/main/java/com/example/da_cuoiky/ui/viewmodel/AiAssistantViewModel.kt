package com.example.da_cuoiky.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.da_cuoiky.model.GeminiRequest
import com.example.da_cuoiky.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean
)

class AiAssistantViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Xin chào! Mình là trợ lý AI của nhà hàng.\nMình có thể gợi ý món ăn theo thời tiết hoặc trả lời các câu hỏi về dinh dưỡng (Ví dụ: 'Tôi ăn kiêng thì nên ăn gì?'). Bạn cần mình giúp gì nào?",
                isFromUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // 1. Thêm tin nhắn của User vào danh sách
        val userMsg = ChatMessage(text = query, isFromUser = true)
        _messages.value = _messages.value + userMsg

        // 2. Gọi API để lấy câu trả lời
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Phân tích từ khóa sơ bộ để tự động chọn 'type' (Gợi ý hoặc Dinh dưỡng)
                val qLower = query.lowercase()
                var type = "nutrition"
                if (qLower.contains("thời tiết") || qLower.contains("nóng") || qLower.contains("lạnh") || qLower.contains("mưa") || qLower.contains("gợi ý")) {
                    type = "recommendation"
                }

                val request = GeminiRequest(
                    type = type,
                    query = query,
                    weather = "Bình thường", // Nếu bạn có API thời tiết thì truyền vào đây
                    time = "Hiện tại"
                )
                
                val response = RetrofitClient.instance.askGeminiAssistant(request)
                
                if (response.isSuccessful) {
                    val aiReply = response.body()?.reply
                    val errorMessage = response.body()?.message
                    
                    val finalText = aiReply ?: errorMessage ?: "Xin lỗi, mình chưa hiểu ý bạn."
                    val aiMsg = ChatMessage(text = finalText, isFromUser = false)
                    _messages.value = _messages.value + aiMsg
                } else {
                    val errorMsg = ChatMessage(text = "Lỗi kết nối: Server trả về ${response.code()}", isFromUser = false)
                    _messages.value = _messages.value + errorMsg
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(text = "Có lỗi xảy ra: ${e.message}", isFromUser = false)
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }
}
