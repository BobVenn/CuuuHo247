package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rescue_requests")
data class RescueRequestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderCode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vehicleType: String,       // "Ô tô con (4-7 chỗ)", "Xe SUV / Bán tải", "Xe máy / Xe điện", "Xe tải nhẹ"
    val serviceType: String,       // "Kích bình ắc quy", "Vá vỏ / Thay bánh sơ cua", "Cẩu kéo về Gara", "Tiếp xăng khẩn cấp", "Mở khóa xe", "Cứu hộ ngập nước"
    val licensePlate: String,
    val contactName: String,
    val contactPhone: String,
    val locationAddress: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val status: String,            // "FINDING", "ASSIGNED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val technicianName: String,
    val technicianPhone: String,
    val technicianVehicle: String,
    val estimatedDistanceKm: Double,
    val estimatedMinutes: Int,
    val estimatedCost: Long,       // VND
    val actualCost: Long = 0,
    val rating: Int = 0,
    val ratingComment: String = ""
)
