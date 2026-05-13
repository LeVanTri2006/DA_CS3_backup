package com.example.da_cuoiky.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────
// CORE ENUMS
// ─────────────────────────────────

enum class UserRole { STAFF, CUSTOMER, KITCHEN, MANAGER }
enum class TableStatus { EMPTY, RESERVED, OCCUPIED, PAID, LOCKED }
enum class TableZone(val displayName: String) {
    INDOOR("Trong nhà"),
    OUTDOOR("Ngoài trời"),
    VIP("Phòng VIP")
}

enum class OrderStatus(val displayName: String) {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PREPARING("Đang chuẩn bị"),
    READY("Sẵn sàng"),
    DELIVERING("Đang giao"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy")
}

enum class DeliveryType(val displayName: String) {
    DINE_IN("Tại bàn"),
    PICKUP("Tự đến lấy"),
    DELIVERY("Giao tận nơi")
}

enum class PaymentMethod(val displayName: String, val icon: String) {
    CASH("Tiền mặt", "💵"),
    CARD("Thẻ tín dụng", "💳"),
    QR("Quét QR", "📱"),
    MOMO("MoMo", "🟣"),
    VNPAY("VNPay", "🔵")
}

enum class ReservationStatus { PENDING, CONFIRMED, CHECKED_IN, CANCELLED, NO_SHOW }

// ─────────────────────────────────
// USER & AUTH
// ─────────────────────────────────

data class User(
    @SerializedName("ma_nguoi_dung") val id: String,
    @SerializedName("ho_ten") val name: String,
    @SerializedName("so_dien_thoai") val phone: String,
    @SerializedName("email") val email: String = "",
    @SerializedName("vai_tro") val role: UserRole = UserRole.CUSTOMER,
    @SerializedName("anh_dai_dien") val avatarUrl: String = "",
    val loyaltyPoints: Int = 0,
    val defaultAddresses: List<String> = emptyList()
)

// ─────────────────────────────────
// BRANCH
// ─────────────────────────────────

data class Branch(
    @SerializedName("ma_chi_nhanh") val id: String,
    @SerializedName("ten_chi_nhanh") val name: String,
    @SerializedName("dia_chi") val address: String,
    @SerializedName("so_dien_thoai") val phone: String,
    @SerializedName("gio_mo_cua") val openHours: String,
    val deliveryFee: Int = 0,
    val serviceFee: Int = 0,
    val taxRate: Double = 0.08
)

// ─────────────────────────────────
// MENU
// ─────────────────────────────────

data class ModifierOption(
    val id: String,
    val name: String,
    val extraPrice: Int = 0
)

data class Modifier(
    val id: String,
    val name: String,
    val type: String, // "single" | "multi"
    val options: List<ModifierOption>
)

data class MenuItem(
    @SerializedName("ma_mon_an") val id: String,
    @SerializedName("ten_mon") val name: String,
    @SerializedName("gia_ban") val price: Int,
    @SerializedName("ma_danh_muc") val categoryId: Int,
    @SerializedName("duong_dan_anh") val imageUrl: String?,
    @SerializedName("mo_ta_ngan") val description: String? = "",
    @SerializedName("trang_thai") val status: String = "dang_ban",

    @SerializedName("ten_danh_muc") val category: String? = "Món ăn",
    @SerializedName("da_ban") val soldCount: Int = 0,
    val modifiers: List<Modifier> = emptyList(),
    val isAvailable: Boolean = true,
    val prepTime: Int = 10,
    val calories: Int = 0,
    val allergens: List<String> = emptyList(),
    val isPopular: Boolean = false,
    val isVegetarian: Boolean = false
)

// ─────────────────────────────────
// ORDER
// ─────────────────────────────────

data class OrderItem(
    val menuItemId: String,
    val name: String,
    val qty: Int,
    val price: Int,
    val note: String = "",
    val selectedModifiers: List<ModifierOption>? = emptyList()
) {
    val totalPrice: Int get() = (price + (selectedModifiers?.sumOf { it.extraPrice } ?: 0)) * qty
}

data class Order(
    @SerializedName("id_hoa_don")
    val id: String,

    @SerializedName("ma_ban")
    val tableId: String,

    @SerializedName("nhan_vien_id")
    val staffId: String? = null,

    @SerializedName("trang_thai_don")
    val statusStr: String = "PENDING",

    @SerializedName("tong_tien")
    val totalPriceFromApi: Int = 0, 

    @SerializedName("thoi_gian_tao")
    val createdAt: String = "",

    val items: List<OrderItem> = emptyList(),
    val discount: Int = 0,
    val eta: String = "25 phút",
    val tableName: String = "",
    val deliveryType: String = "",
    val paymentStatus: String = "chua_thanh_toan"
) {
    val status: OrderStatus get() = try {
        OrderStatus.valueOf(statusStr.uppercase())
    } catch (e: Exception) {
        OrderStatus.PENDING
    }

    val subtotal: Int get() = items.sumOf { it.price * it.qty }
    val total: Int get() = subtotal - discount
}

data class PaymentInfo(
    val method: PaymentMethod = PaymentMethod.CASH,
    val status: String = "PENDING",
    val transactionId: String = "",
    val paidAt: String = ""
)

// ─────────────────────────────────
// TABLE & OTHERS
// ─────────────────────────────────

data class TableModel(
    @SerializedName("ma_ban") val id: String,
    @SerializedName("ten_ban") val name: String,
    @SerializedName("khu_vuc") val zoneStr: String = "Trong nhà",
    @SerializedName("suc_chua") val capacity: Int,
    @SerializedName("trang_thai") val statusStr: String = "trong",
    @SerializedName("id_hoa_don") val currentOrderId: String? = null,
    @SerializedName("currentOrderId") val currentOrderIdAlt: String? = null,  // ✅ Alt field name from API
    @SerializedName("orderStatus") val orderStatus: String? = null,  // ✅ Order status from DB
    @SerializedName("orderTotal") val orderTotal: Int = 0,  // ✅ Order total
    @SerializedName("itemCount") val itemCount: Int = 0  // ✅ Number of items
) {
    val status: TableStatus get() = when(statusStr) {
        "co_khach" -> TableStatus.OCCUPIED
        "dat_truoc" -> TableStatus.RESERVED
        else -> TableStatus.EMPTY
    }
    val zone: TableZone get() = when(zoneStr) {
        "Phòng VIP" -> TableZone.VIP
        "Ngoài trời" -> TableZone.OUTDOOR
        else -> TableZone.INDOOR
    }
    // ✅ Use either field for current order ID
    val activeOrderId: String? get() = currentOrderId ?: currentOrderIdAlt
}

data class TableApiResponse(
    val status: String,
    val data: List<TableModel>?
)

data class GenericApiResponse(
    val status: String,
    val message: String?,
    @SerializedName("ma_dat_cho") val maDatCho: String? = null,
    @SerializedName("trang_thai") val trangThai: String? = null,
    val data: Map<String, Any>? = null
)

data class KitchenOrder(
    val orderId: String,
    val tableName: String,
    val items: List<OrderItem>,
    val createdAt: Long = 0L,
    val targetTimeMinutes: Int = 10,
    val status: String
)

data class KitchenListResponse(
    val status: String,
    val data: List<KitchenOrder>?
)

data class Reservation(
    @SerializedName("ma_dat_cho") val id: String,
    val userId: String,
    val userName: String,
    val branchId: String,
    val datetime: String,
    val pax: Int,
    val zone: TableZone = TableZone.INDOOR,
    val specialRequests: String = "",
    val status: ReservationStatus = ReservationStatus.PENDING,
    val deposit: Int = 0,
    val qrCode: String = ""
)

data class InventoryItem(
    @SerializedName("id") val id: Int,
    @SerializedName("ten_nguyen_lieu") val name: String,
    @SerializedName("don_vi_tinh") val unit: String,
    @SerializedName("so_luong_ton") val qtyOnHand: Double,
    @SerializedName("nguong_bao_dong") val lowStockThreshold: Double,
    @SerializedName("gia_von_nhap") val importPrice: Double,
    @SerializedName("ma_danh_muc") val categoryId: Int
) {
    val isLowStock: Boolean get() = qtyOnHand <= lowStockThreshold
}

data class SyncUserRequest(
    @SerializedName("uid_firebase") val uid: String,
    @SerializedName("ho_ten") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("so_dien_thoai") val phone: String? = null
)

data class SyncUserResponse(
    val success: Boolean,
    val message: String
)


// ─────────────────────────────────
// SAMPLE DATA REPOSITORY
// ─────────────────────────────────

object SampleData {
    val branch = Branch(
        id = "B01", name = "Nhà Hàng Gourmet Hub",
        address = "12 Lê Lợi, Q.1", phone = "028 3822 0000",
        openHours = "10:00 - 22:00"
    )

    val categories = listOf("Tất cả", "Món chính", "Khai vị", "Đồ uống", "Tráng miệng")

    val menuItems = listOf(
        MenuItem(id = "M01", name = "Phở Bò Đặc Biệt", price = 65000, categoryId = 1, imageUrl = "https://images.unsplash.com/photo-1569050467447-ce54b3bbc37d?w=400"),
        MenuItem(id = "M02", name = "Bún Bò Huế", price = 58000, categoryId = 1, imageUrl = "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=400"),
        MenuItem(id = "M03", name = "Cơm Tấm Sườn Nướng", price = 75000, categoryId = 1, imageUrl = "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=400")
    )
    
    val kitchenOrders = listOf(
        KitchenOrder(
            orderId = "O1001", tableName = "Bàn 01",
            items = listOf(OrderItem("M01", "Phở Bò", 2, 65000)),
            createdAt = System.currentTimeMillis(), targetTimeMinutes = 10, status = "PENDING"
        )
    )

    val tables = listOf(
        TableModel("T01", "Bàn 01", "Trong nhà", 4, "trong"),
        TableModel("T02", "Bàn 02", "Trong nhà", 4, "co_khach", "O1001"),
        TableModel("T03", "Bàn 03", "Ngoài trời", 2, "trong"),
        TableModel("T04", "Phòng VIP 1", "Phòng VIP", 10, "dat_truoc")
    )

    val sampleOrder = Order(
        id = "O1001",
        tableId = "T02",
        staffId = "S01",
        statusStr = "PREPARING",
        totalPriceFromApi = 130000,
        createdAt = "2024-05-20 18:00:00",
        items = listOf(
            OrderItem("M01", "Phở Bò Đặc Biệt", 2, 65000)
        )
    )

    val customerUser = User(id = "U002", name = "Lan", phone = "098", role = UserRole.CUSTOMER)

    val reservations = listOf(
        Reservation(
            id = "RES01", userId = "U002", userName = "Lan", branchId = "B01",
            datetime = "20:00, 20/04/2026", pax = 4, status = ReservationStatus.CONFIRMED
        )
    )

    val inventoryItems = listOf(
        InventoryItem(1, "Bò thăn loại 1", "kg", 15.0, 3.0, 280000.0, 6),
        InventoryItem(2, "Sườn heo non", "kg", 20.0, 5.0, 180000.0, 6),
        InventoryItem(3, "Gạo tấm thơm", "kg", 50.0, 10.0, 22000.0, 5),
        InventoryItem(4, "Bánh phở tươi", "kg", 25.0, 5.0, 15000.0, 6),
        InventoryItem(9, "Sữa đặc (Lon 380g)", "lon", 48.0, 12.0, 18500.0, 5)
    )
}

// ─────────────────────────────────
// STAFF API MODELS
// ─────────────────────────────────

data class StaffLoginRequest(val email: String, val password: String)

data class StaffData(
    @SerializedName("ma_nhan_vien")  val id: String,
    @SerializedName("ho_ten")        val name: String,
    @SerializedName("vai_tro")       val role: String,
    @SerializedName("chi_nhanh")     val branch: String = "",
    @SerializedName("email")         val email: String = "",
    @SerializedName("so_dien_thoai") val phone: String? = null,
    @SerializedName("trang_thai")    val status: String = "active"
)

data class StaffLoginResponse(
    val status: String,
    val message: String,
    val data: StaffData?
)

data class StaffListResponse(val status: String, val data: List<StaffData>?)

data class StaffRequest(
    @SerializedName("ma_nhan_vien")  val id: String = "",
    @SerializedName("ho_ten")        val name: String,
    @SerializedName("vai_tro")       val role: String,
    @SerializedName("email")         val email: String,
    @SerializedName("mat_khau")      val password: String = "",
    @SerializedName("so_dien_thoai") val phone: String? = null,
    @SerializedName("chi_nhanh")     val branch: String = "",
    @SerializedName("trang_thai")    val status: String = "active",
    @SerializedName("tuoi")          val age: Int = 0
)

// ─────────────────────────────────
// TABLE API MODELS
// ─────────────────────────────────

data class TableStatusRequest(
    @SerializedName("ma_ban")    val tableId: String,
    @SerializedName("trang_thai") val status: String  
)

// ─────────────────────────────────
// ORDER API MODELS
// ─────────────────────────────────

data class CreateOrderRequest(
    @SerializedName("ma_nguoi_dung") val userId: String? = null,
    @SerializedName("ma_ban")       val tableId: String,
    @SerializedName("nhan_vien_id") val staffId: String? = null,
    @SerializedName("ma_chi_nhanh")   val branchId: String = "B01",
    @SerializedName("tong_tien")    val totalPrice: Int,
    @SerializedName("hinh_thuc_nhan") val deliveryType: String = "PICKUP", // ✅ Bổ sung hình thức nhận
    @SerializedName("phuong_thuc_thanh_toan") val paymentMethod: String = "Tiền mặt", // ✅ Bổ sung PT thanh toán
    @SerializedName("ho_ten")       val fullName: String? = null, // ✅ Bổ sung tên KH
    @SerializedName("so_dien_thoai") val phone: String? = null,   // ✅ Bổ sung SĐT KH
    @SerializedName("chi_tiet")     val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    @SerializedName("ma_mon_an") val menuItemId: String,
    @SerializedName("ten_mon")   val name: String,
    @SerializedName("so_luong")  val qty: Int,
    @SerializedName("don_gia")   val price: Int,
    @SerializedName("ghi_chu")   val note: String = ""
)

data class CreateOrderResponse(
    val status: String,
    val message: String,
    @SerializedName("id_hoa_don") val orderId: String?
)

data class OrderListResponse(
    val status: String,
    val data: List<Order>?
)

data class OrderResponse(
    val status: String,
    val data: Order?
)

data class OrderStatusRequest(
    @SerializedName("id_hoa_don")    val orderId: String,
    @SerializedName("trang_thai_don") val status: String  
)

// ─────────────────────────────────
// RESERVATION API MODELS
// ─────────────────────────────────

data class ReservationRequest(
    @SerializedName("ma_nguoi_dung")  val userId: String,
    @SerializedName("ho_ten")         val userName: String,
    @SerializedName("so_dien_thoai")  val phone: String,
    @SerializedName("ma_ban")         val tableId: String? = null,
    @SerializedName("ma_chi_nhanh")   val branchId: String = "B01",
    @SerializedName("thoi_gian")      val datetime: String,    
    @SerializedName("so_nguoi")       val pax: Int,
    @SerializedName("khu_vuc")        val zone: String = "Trong nhà",
    @SerializedName("ghi_chu")        val note: String = "",
    @SerializedName("tien_dat_coc")   val deposit: Int = 0
)

data class ReservationListResponse(
    val status: String,
    val data: List<ReservationItem>?
)

data class ReservationItem(
    @SerializedName("ma_dat_cho")     val id: String,
    @SerializedName("ma_nguoi_dung")  val userId: String,
    @SerializedName("ho_ten")         val userName: String,
    @SerializedName("so_dien_thoai")  val phone: String = "",
    @SerializedName("ma_ban")         val tableId: String? = null,
    @SerializedName("ma_chi_nhanh")   val branchId: String = "B01",
    @SerializedName("thoi_gian")      val datetime: String,
    @SerializedName("so_nguoi")       val pax: Int,
    @SerializedName("khu_vuc")        val zone: String = "Trong nhà",
    @SerializedName("ghi_chu")        val note: String = "",
    @SerializedName("trang_thai")     val statusStr: String = "PENDING",
    @SerializedName("tien_dat_coc")   val deposit: Int = 0
) {
    val status: ReservationStatus get() = try {
        ReservationStatus.valueOf(statusStr.uppercase())
    } catch (e: Exception) { ReservationStatus.PENDING }
}

data class ReservationStatusRequest(
    @SerializedName("ma_dat_cho") val id: String,
    @SerializedName("trang_thai") val status: String
)

// ─────────────────────────────────
// PAYMENT API MODELS
// ─────────────────────────────────

data class PaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("du_lieu") val data: PaymentApiInfo?
)

data class PaymentApiInfo(
    @SerializedName("ma_giao_dich") val transactionId: String,
    @SerializedName("ten_ngan_hang") val bankName: String,
    @SerializedName("chu_tai_khoan") val accountName: String,
    @SerializedName("so_tai_khoan") val accountNumber: String,
    @SerializedName("so_tien") val amount: Int,
    @SerializedName("noi_dung_chuyen_khoan") val content: String,
    @SerializedName("duong_dan_qr") val qrUrl: String
)
