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
        "createdAt" to createdAt
    )
}

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
