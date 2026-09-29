package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.RescueRequestEntity
import com.example.data.local.entity.SavedVehicleEntity
import com.example.data.repository.RescueRepository
import com.example.util.LocationHelper
import com.example.util.UserLocationInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RescueViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RescueRepository(
        database.rescueDao(),
        database.vehicleDao(),
        database.contactDao()
    )

    val activeRequest: StateFlow<RescueRequestEntity?> = repository.activeRequest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allRequests: StateFlow<List<RescueRequestEntity>> = repository.allRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVehicles: StateFlow<List<SavedVehicleEntity>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allContacts: StateFlow<List<EmergencyContactEntity>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLocation = MutableStateFlow(
        UserLocationInfo(
            latitude = 21.0285,
            longitude = 105.8542,
            address = "Số 18 Tràng Tiền, Hoàn Kiếm, Hà Nội"
        )
    )
    val currentLocation: StateFlow<UserLocationInfo> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private var simulationJob: Job? = null

    init {
        refreshLocation()
        // Listen to active request to run tracking progression simulation
        viewModelScope.launch {
            activeRequest.collect { req ->
                if (req != null && req.status != "COMPLETED" && req.status != "CANCELLED") {
                    startTrackingSimulation(req.id, req.status)
                }
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            try {
                val loc = LocationHelper.getCurrentLocation(getApplication())
                _currentLocation.value = loc
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun updateManualLocation(address: String) {
        _currentLocation.value = _currentLocation.value.copy(address = address)
    }

    fun triggerOneTapSOS(vehicleType: String, licensePlate: String) {
        viewModelScope.launch {
            val loc = _currentLocation.value
            repository.createRescueRequest(
                vehicleType = vehicleType.ifBlank { "Ô tô con (4-7 chỗ)" },
                serviceType = "CỨU HỘ KHẨN CẤP SOS 24/7",
                licensePlate = licensePlate.ifBlank { "30H - 688.99" },
                contactName = "Chủ xe (Khẩn cấp)",
                contactPhone = "0987654321",
                locationAddress = loc.address,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = "Yêu cầu cứu hộ khẩn cấp 1 chạm SOS. Xe gặp sự cố đột xuất cần hỗ trợ ngay!",
                estimatedCost = 350000
            )
        }
    }

    fun submitCustomRequest(
        vehicleType: String,
        serviceType: String,
        licensePlate: String,
        contactName: String,
        contactPhone: String,
        description: String,
        estimatedCost: Long
    ) {
        viewModelScope.launch {
            val loc = _currentLocation.value
            repository.createRescueRequest(
                vehicleType = vehicleType,
                serviceType = serviceType,
                licensePlate = licensePlate,
                contactName = contactName,
                contactPhone = contactPhone,
                locationAddress = loc.address,
                latitude = loc.latitude,
                longitude = loc.longitude,
                description = description,
                estimatedCost = estimatedCost
            )
        }
    }

    private fun startTrackingSimulation(requestId: Long, currentStatus: String) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            when (currentStatus) {
                "FINDING" -> {
                    delay(3500)
                    repository.updateStatus(requestId, "ASSIGNED")
                    delay(2500)
                    repository.updateStatus(requestId, "EN_ROUTE")
                    repository.updateTracking(requestId, 3.2, 10)
                }
                "ASSIGNED" -> {
                    delay(2500)
                    repository.updateStatus(requestId, "EN_ROUTE")
                    repository.updateTracking(requestId, 3.2, 10)
                }
                "EN_ROUTE" -> {
                    // Simulate moving closer
                    delay(4000)
                    repository.updateTracking(requestId, 1.8, 6)
                    delay(4000)
                    repository.updateTracking(requestId, 0.6, 2)
                    delay(3500)
                    repository.updateStatus(requestId, "ARRIVED")
                    repository.updateTracking(requestId, 0.0, 0)
                }
                "ARRIVED" -> {
                    delay(3000)
                    repository.updateStatus(requestId, "IN_PROGRESS")
                }
            }
        }
    }

    fun cancelActiveRequest(requestId: Long) {
        simulationJob?.cancel()
        viewModelScope.launch {
            repository.cancelRequest(requestId)
        }
    }

    fun completeActiveRequest(requestId: Long, rating: Int, comment: String) {
        simulationJob?.cancel()
        viewModelScope.launch {
            val req = activeRequest.value
            val cost = req?.estimatedCost ?: 300000
            repository.completeRequest(requestId, cost, rating, comment)
        }
    }

    fun addNewVehicle(
        name: String,
        type: String,
        licensePlate: String,
        brand: String,
        color: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            repository.insertVehicle(
                SavedVehicleEntity(
                    name = name,
                    vehicleType = type,
                    licensePlate = licensePlate,
                    brand = brand,
                    color = color,
                    isDefault = isDefault
                )
            )
        }
    }

    fun setDefaultVehicle(id: Long) {
        viewModelScope.launch {
            repository.setDefaultVehicle(id)
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            repository.deleteVehicle(id)
        }
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            repository.insertContact(
                EmergencyContactEntity(
                    name = name,
                    phone = phone,
                    relationship = relationship
                )
            )
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
        }
    }

    companion object {
        const val PREDEFINED_RESCUE_HOTLINE = "1900545566"
        const val PREDEFINED_RESCUE_HOTLINE_DISPLAY = "1900 545566"
    }

    fun triggerEmergencySosDialer(context: Context, phoneNumber: String = PREDEFINED_RESCUE_HOTLINE) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        triggerEmergencySosDialer(context, phoneNumber)
    }

    fun shareLocation(context: Context, address: String, lat: Double, lng: Double) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
            val text = "Tôi đang gặp sự cố xe cần cứu hộ gấp tại:\n📍 $address\n🔗 Tọa độ Google Maps: $mapsUrl"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Chia sẻ vị trí cứu hộ khẩn cấp")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể chia sẻ vị trí", Toast.LENGTH_SHORT).show()
        }
    }
}
