package com.example.da_cuoiky.network

import com.example.da_cuoiky.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("view/api_menu.php")
    suspend fun getMenuItems(): List<MenuItem>

    @POST("view/api_sync_user.php")
    suspend fun syncUserToMySQL(@Body request: SyncUserRequest): Response<SyncUserResponse>

    @POST("view/api_login_staff.php")
    suspend fun loginStaff(@Body request: StaffLoginRequest): Response<StaffLoginResponse>

    @GET("view/api_get_tables.php")
    suspend fun getTables(): Response<TableApiResponse>

    @POST("view/api_create_order.php")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>

    @GET("view/api_get_order.php")
    suspend fun getOrderById(@Query("id") id: String): Response<OrderResponse>

    // ✅ Lấy danh sách đơn hàng cho nhà bếp
    @GET("view/api_get_kitchen_orders.php")
    suspend fun getKitchenOrders(): Response<KitchenListResponse>

    // ✅ Cập nhật trạng thái đơn hàng (Dùng cho Bếp)
    @POST("view/api_update_order_status.php")
    suspend fun updateOrderStatus(@Body request: OrderStatusRequest): Response<GenericApiResponse>

    // ✅ Lấy danh sách đơn hàng của người dùng
    @GET("view/api_get_orders.php")
    suspend fun getOrders(@Query("user_id") userId: String): Response<OrderListResponse>

    // ✅ Lấy thông tin tài khoản và hạng thành viên
    @GET("view/api_get_profile.php")
    suspend fun getProfile(@Query("uid") uid: String): Response<ProfileApiResponse>

    @POST("view/api_create_reservation.php")
    suspend fun createReservation(@Body request: ReservationRequest): Response<GenericApiResponse>

    @GET("view/api_get_reservations.php")
    suspend fun getReservations(@Query("user_id") userId: String? = null): Response<ReservationListResponse>
    
    @GET("view/api_check_reservation.php")
    suspend fun checkReservationStatus(@Query("id") id: String): Response<GenericApiResponse>

    @GET("view/api_get_thanh_toan.php")
    suspend fun getPaymentInfo(@Query("id_hoa_don") idHoaDon: String): Response<PaymentResponse>

    @POST("view/api_checkout.php")
    suspend fun confirmCheckout(@Body request: Map<String, String>): Response<String>

    @POST("view/api_create_customer_order.php")
    suspend fun createCustomerOrder(@Body request: Map<String, String>): Response<GenericApiResponse>
}