package com.example.data.repository

import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.RescueDao
import com.example.data.local.dao.VehicleDao
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.RescueRequestEntity
import com.example.data.local.entity.SavedVehicleEntity
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

class RescueRepository(
    private val rescueDao: RescueDao,
    private val vehicleDao: VehicleDao,
    private val contactDao: ContactDao
) {
    val allRequests: Flow<List<RescueRequestEntity>> = rescueDao.getAllRequests()
    val activeRequest: Flow<RescueRequestEntity?> = rescueDao.getActiveRequest()
    val allVehicles: Flow<List<SavedVehicleEntity>> = vehicleDao.getAllVehicles()
    val allContacts: Flow<List<EmergencyContactEntity>> = contactDao.getAllContacts()

    suspend fun createRescueRequest(
        vehicleType: String,
        serviceType: String,
        licensePlate: String,
        contactName: String,
        contactPhone: String,
        locationAddress: String,
        latitude: Double,
        longitude: Double,
        description: String,
        estimatedCost: Long
    ): Long {
        val randomSuffix = Random.nextInt(10000, 99999)
        val orderCode = "CH-$randomSuffix"

        val techNames = listOf("Nguyễn Văn Hùng", "Trần Đình Trọng", "Lê Hoàng Long", "Phạm Quốc Huy", "Vũ Minh Tuấn")
        val techVehicles = listOf(
            "Xe cứu hộ sàn trượt 29C-782.14",
            "Xe cơ động phản ứng nhanh 29C-415.82",
            "Xe cẩu kéo chuyên dụng 30G-671.29"
        )
        val techPhones = listOf("0912345678", "0987654321", "0903112233", "0934567890")

        val newRequest = RescueRequestEntity(
            orderCode = orderCode,
            timestamp = System.currentTimeMillis(),
            vehicleType = vehicleType,
            serviceType = serviceType,
            licensePlate = licensePlate,
            contactName = contactName,
            contactPhone = contactPhone,
            locationAddress = locationAddress,
            latitude = latitude,
            longitude = longitude,
            description = description,
            status = "FINDING", // Starts with FINDING
            technicianName = techNames.random(),
            technicianPhone = techPhones.random(),
            technicianVehicle = techVehicles.random(),
            estimatedDistanceKm = 3.8,
            estimatedMinutes = 12,
            estimatedCost = estimatedCost,
            actualCost = estimatedCost
        )

        return rescueDao.insertRequest(newRequest)
    }

    suspend fun updateStatus(id: Long, status: String) {
        rescueDao.updateStatus(id, status)
    }

    suspend fun updateTracking(id: Long, distanceKm: Double, minutes: Int) {
        rescueDao.updateTracking(id, distanceKm, minutes)
    }

    suspend fun cancelRequest(id: Long) {
        rescueDao.updateStatus(id, "CANCELLED")
    }

    suspend fun completeRequest(id: Long, actualCost: Long, rating: Int, comment: String) {
        val current = rescueDao.getRequestById(id)
        if (current != null) {
            val updated = current.copy(
                status = "COMPLETED",
                actualCost = actualCost,
                rating = rating,
                ratingComment = comment
            )
            rescueDao.updateRequest(updated)
        }
    }

    suspend fun insertVehicle(vehicle: SavedVehicleEntity) {
        if (vehicle.isDefault) {
            vehicleDao.clearDefaultFlags()
        }
        vehicleDao.insertVehicle(vehicle)
    }

    suspend fun setDefaultVehicle(id: Long) {
        vehicleDao.clearDefaultFlags()
        vehicleDao.setDefaultVehicle(id)
    }

    suspend fun deleteVehicle(id: Long) {
        vehicleDao.deleteVehicle(id)
    }

    suspend fun insertContact(contact: EmergencyContactEntity) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(id: Long) {
        contactDao.deleteContact(id)
    }
}
