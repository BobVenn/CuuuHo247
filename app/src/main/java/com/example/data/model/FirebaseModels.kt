package com.example.data.model

import com.example.data.config.AppConfig

/**
 * Model người dùng lưu tại node "users/{uid}"
 */
data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String = "",
    val role: String = AppConfig.UserRole.USER, // "User", "Rescue Staff", "Admin"
    val vehicleType: String = "Ô tô 4-7 chỗ",
    val vehicleName: String = "",
    val licensePlate: String = "",
    val partnerStatus: String = AppConfig.PartnerApplicationStatus.NONE, // "NONE", "PENDING", "APPROVED", "REJECTED"
    val partnerRejectionReason: String? = null,
    // Thông tin liên hệ khẩn cấp (In Case of Emergency - ICE) phục vụ điều phối nhanh
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyRelationship: String = "Người thân", // Vợ/Chồng, Bố/Mẹ, Anh/Em, Đồng nghiệp...
    val bloodType: String = "", // O+, A+, B+, AB+, Chưa rõ
    val emergencyNotes: String = "", // Dị ứng, tiền sử bệnh hoặc ghi chú cứu hộ đặc biệt
    val homeAddress: String = "", // Địa chỉ cứu hộ thường trú / Nhà riêng
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "phone" to phone,
        "role" to role,
        "vehicleType" to vehicleType,
        "vehicleName" to vehicleName,
        "licensePlate" to licensePlate,
        "partnerStatus" to partnerStatus,
        "partnerRejectionReason" to partnerRejectionReason,
        "emergencyContactName" to emergencyContactName,
        "emergencyContactPhone" to emergencyContactPhone,
        "emergencyRelationship" to emergencyRelationship,
        "bloodType" to bloodType,
        "emergencyNotes" to emergencyNotes,
        "homeAddress" to homeAddress,
        "createdAt" to createdAt
    )
}

/**
 * Model xe lưu trong Gara cá nhân & nhắc hạn đăng kiểm/bảo hiểm
 */
data class SavedVehicle(
    val id: String = "",
    val name: String = "",
    val vehicleType: String = "Ô tô 4-7 chỗ",
    val licensePlate: String = "",
    val inspectionExpiry: String = "15/12/2026", // Hạn đăng kiểm
    val insuranceExpiry: String = "20/11/2026",  // Hạn bảo hiểm TNDS
    val notes: String = ""
)

/**
 * Model yêu cầu cứu hộ lưu tại node "requests/{requestId}"
 */
data class RescueRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val issueType: String = "Hỏng xe",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val vehicleType: String = "",
    val licensePlate: String = "",
    val status: String = AppConfig.RequestStatus.PENDING,
    val staffId: String? = null,
    val staffName: String? = null,
    val staffPhone: String? = null,
    val cost: Long? = null,
    val paymentStatus: String = "UNPAID", // "UNPAID", "PAID_CASH", "PAID_VIETQR"
    val imageUrl: String? = null,
    val emergencyContact: String? = null,
    val emergencyContactPhone: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int? = null,
    val ratingComment: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userPhone" to userPhone,
        "issueType" to issueType,
        "description" to description,
        "address" to address,
        "latitude" to latitude,
        "longitude" to longitude,
        "vehicleType" to vehicleType,
        "licensePlate" to licensePlate,
        "status" to status,
        "staffId" to staffId,
        "staffName" to staffName,
        "staffPhone" to staffPhone,
        "cost" to cost,
        "paymentStatus" to paymentStatus,
        "imageUrl" to imageUrl,
        "emergencyContact" to emergencyContact,
        "emergencyContactPhone" to emergencyContactPhone,
        "timestamp" to timestamp,
        "rating" to rating,
        "ratingComment" to ratingComment
    )
}

/**
 * Model tin nhắn cứu hộ lưu tại node "chats/{requestId}/{messageId}"
 */
data class ChatMessage(
    val id: String = "",
    val requestId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "User",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "requestId" to requestId,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderRole" to senderRole,
        "receiverId" to receiverId,
        "message" to message,
        "timestamp" to timestamp,
        "read" to read
    )
}

/**
 * Model đánh giá chất lượng lưu tại node "ratings/{ratingId}"
 */
data class RatingItem(
    val id: String = "",
    val requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val staffId: String = "",
    val staffName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "requestId" to requestId,
        "userId" to userId,
        "userName" to userName,
        "staffId" to staffId,
        "staffName" to staffName,
        "rating" to rating,
        "comment" to comment,
        "timestamp" to timestamp
    )
}

/**
 * Model báo cáo phản hồi lưu tại node "reports/{reportId}"
 */
data class ReportItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val requestId: String? = null,
    val reportType: String = "Góp ý cải tiến dịch vụ",
    val content: String = "",
    val status: String = "Chờ xử lý",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userPhone" to userPhone,
        "requestId" to requestId,
        "reportType" to reportType,
        "content" to content,
        "status" to status,
        "timestamp" to timestamp
    )
}

/**
 * Model hồ sơ đăng ký trở thành Kỹ thuật viên / Gara đối tác cứu hộ
 * Lưu tại node "staff_applications/{userId}"
 */
data class StaffApplication(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val idCardNumber: String = "",            // Số CCCD / CMND
    val operatingArea: String = "",           // Khu vực hoạt động (Tỉnh/Thành phố, Quận/Huyện)
    val companyOrGarageName: String = "",     // Tên Gara / Doanh nghiệp / Đội cứu hộ
    val taxOrBusinessCode: String = "",       // Mã số thuế / Đăng ký kinh doanh
    val garageAddress: String = "",           // Địa chỉ trụ sở / trạm trực
    val operatingRadiusKm: Int = 20,          // Bán kính hoạt động (km)
    val rescueVehicleType: String = "Xe cẩu sàn trượt", // Loại phương tiện cứu hộ
    val vehiclePlate: String = "",            // Biển số xe cứu hộ
    val servicesOffered: String = "Kích bình, Vá lốp, Cẩu kéo xe", // Dịch vụ chuyên môn (ô tô/xe máy/kích bình/...)
    val documentsInfo: String = "Đã đính kèm CCCD & Giấy tờ xác minh", // Ghi chú giấy tờ xác minh
    val documentImageUris: String = "",       // Danh sách ảnh giấy tờ xác minh
    val status: String = AppConfig.PartnerApplicationStatus.PENDING, // "PENDING", "APPROVED", "REJECTED"
    val rejectionReason: String? = null,
    val appliedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userEmail" to userEmail,
        "phone" to phone,
        "idCardNumber" to idCardNumber,
        "operatingArea" to operatingArea,
        "companyOrGarageName" to companyOrGarageName,
        "taxOrBusinessCode" to taxOrBusinessCode,
        "garageAddress" to garageAddress,
        "operatingRadiusKm" to operatingRadiusKm,
        "rescueVehicleType" to rescueVehicleType,
        "vehiclePlate" to vehiclePlate,
        "servicesOffered" to servicesOffered,
        "documentsInfo" to documentsInfo,
        "documentImageUris" to documentImageUris,
        "status" to status,
        "rejectionReason" to rejectionReason,
        "appliedAt" to appliedAt,
        "reviewedAt" to reviewedAt,
        "reviewedBy" to reviewedBy
    )
}

/**
 * Event phát ra khi có sự thay đổi trạng thái của yêu cầu cứu hộ từ Firebase Realtime Database
 */
data class StatusChangeEvent(
    val request: RescueRequest,
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Model trạng thái sẵn sàng phục vụ của các dịch vụ cứu hộ
 * Lưu tại node "services_availability/{serviceId}" trong Firebase Realtime Database
 */
data class ServiceAvailabilityItem(
    val id: String = "",                       // "towing", "jump_start", "tire_change", "fuel_delivery", "lockout", "mobile_repair"
    val name: String = "",                     // Tên dịch vụ hiển thị
    val issueType: String = "",                // Liên kết với CreateRequestDialog: "Tai nạn", "Hết bình", "Thủng lốp", v.v.
    val category: String = "EMERGENCY",        // "TOWING", "BATTERY", "TIRE", "FUEL", "LOCKSMITH", "MECHANIC"
    val isAvailable: Boolean = true,           // Sẵn sàng (true) hoặc Tạm bận / bảo trì (false)
    val availabilityStatus: String = "AVAILABLE", // "AVAILABLE", "BUSY", "HIGH_DEMAND"
    val statusText: String = "Sẵn sàng phục vụ 24/7",
    val etaMinutes: Int = 15,                  // Thời gian kỹ thuật viên có mặt ước tính (phút)
    val activeTechnicians: Int = 4,            // Số lượng KTV / xe cứu hộ đang trực
    val priceDisplay: String = "Từ 450.000đ",   // Giá niêm yết công khai
    val basePrice: Long = 450000L,
    val description: String = "",
    val iconKey: String = "towing",            // towing, jump_start, tire_change, fuel_delivery, lockout, mobile_repair
    val operatingHours: String = "24/7 Toàn quốc",
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "issueType" to issueType,
        "category" to category,
        "isAvailable" to isAvailable,
        "availabilityStatus" to availabilityStatus,
        "statusText" to statusText,
        "etaMinutes" to etaMinutes,
        "activeTechnicians" to activeTechnicians,
        "priceDisplay" to priceDisplay,
        "basePrice" to basePrice,
        "description" to description,
        "iconKey" to iconKey,
        "operatingHours" to operatingHours,
        "updatedTimestamp" to updatedTimestamp
    )
}

