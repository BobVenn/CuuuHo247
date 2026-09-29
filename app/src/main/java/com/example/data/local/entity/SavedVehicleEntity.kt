package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_vehicles")
data class SavedVehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,             // e.g. "Mazda CX-5", "Honda SH 150i"
    val vehicleType: String,      // "Ô tô", "Xe máy", "Xe bán tải", "Xe tải"
    val licensePlate: String,     // e.g. "30H - 688.99"
    val brand: String,            // e.g. "Mazda"
    val color: String,            // e.g. "Đỏ pha lê"
    val isDefault: Boolean = false
)
