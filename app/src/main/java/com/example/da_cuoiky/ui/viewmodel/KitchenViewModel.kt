package com.example.da_cuoiky.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.da_cuoiky.model.KitchenOrder
import com.example.da_cuoiky.model.OrderStatusRequest
import com.example.da_cuoiky.network.RetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KitchenViewModel : ViewModel() {
    private val _tickets = MutableStateFlow<List<KitchenOrder>>(emptyList())
    val tickets: StateFlow<List<KitchenOrder>> = _tickets

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val db = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    init {
        fetchKitchenOrders()
        listenForKitchenUpdates()
    }

    private fun listenForKitchenUpdates() {
        listenerRegistration = db.collection("kitchen_updates").document("latest")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("KITCHEN_DEBUG", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d("KITCHEN_DEBUG", "New update from Firebase, refreshing orders...")
                    fetchKitchenOrders()
                }
            }
    }

    fun fetchKitchenOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.instance.getKitchenOrders()
                Log.d("KITCHEN_DEBUG", "Fetch response: ${response.body()}")
                if (response.isSuccessful && response.body()?.status == "success") {
                    _tickets.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = "Lỗi: ${response.body()?.status ?: "Không thể tải dữ liệu"}"
                }
            } catch (e: Exception) {
                Log.e("KITCHEN_DEBUG", "Error fetching kitchen orders", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.updateOrderStatus(
                    OrderStatusRequest(orderId, newStatus)
                )
                if (response.isSuccessful && response.body()?.status == "success") {
                    fetchKitchenOrders() // Refresh list after update
                }
            } catch (e: Exception) {
                Log.e("KITCHEN_DEBUG", "Error updating status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
