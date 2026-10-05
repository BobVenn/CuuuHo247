package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.AppConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import com.example.service.LocationTrackerService
import com.example.util.LocationHelper
import com.example.util.RescueNotificationHelper
import com.example.util.UserLocationInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RescueRealtimeViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseManager = FirebaseManager.getInstance()
    private val locationTracker = LocationTrackerService(application)

    companion object {
        private const val TAG = "RescueRealtimeVM"
    }

    // Location State
    private val _currentLocation = MutableStateFlow(
        UserLocationInfo(
            latitude = LocationTrackerService.DEFAULT_LAT,
            longitude = LocationTrackerService.DEFAULT_LNG,
            address = "Đang xác định vị trí GPS...",
            hasFineLocation = locationTracker.hasFineLocationPermission(),
            isGpsEnabled = locationTracker.isLocationProviderEnabled()
        )
    )
    val currentLocation: StateFlow<UserLocationInfo> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _hasFineLocationPermission = MutableStateFlow(locationTracker.hasFineLocationPermission())
    val hasFineLocationPermission: StateFlow<Boolean> = _hasFineLocationPermission.asStateFlow()

    private val _isRealtimeTrackingActive = MutableStateFlow(false)
    val isRealtimeTrackingActive: StateFlow<Boolean> = _isRealtimeTrackingActive.asStateFlow()

    private var locationTrackingJob: Job? = null

    // Realtime Requests
    private val _userRequests = MutableStateFlow<List<RescueRequest>>(emptyList())
    val userRequests: StateFlow<List<RescueRequest>> = _userRequests.asStateFlow()

    private val _allRequests = MutableStateFlow<List<RescueRequest>>(emptyList())
    val allRequests: StateFlow<List<RescueRequest>> = _allRequests.asStateFlow()

    private val _activeRequest = MutableStateFlow<RescueRequest?>(null)
    val activeRequest: StateFlow<RescueRequest?> = _activeRequest.asStateFlow()

    private val _selectedRequest = MutableStateFlow<RescueRequest?>(null)
    val selectedRequest: StateFlow<RescueRequest?> = _selectedRequest.asStateFlow()

    // Realtime Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Realtime Ratings
    private val _userRatings = MutableStateFlow<List<RatingItem>>(emptyList())
    val userRatings: StateFlow<List<RatingItem>> = _userRatings.asStateFlow()

    // Realtime Reports
    private val _userReports = MutableStateFlow<List<ReportItem>>(emptyList())
    val userReports: StateFlow<List<ReportItem>> = _userReports.asStateFlow()

    // Partner / Staff Applications
    private val _currentStaffApplication = MutableStateFlow<StaffApplication?>(null)
    val currentStaffApplication: StateFlow<StaffApplication?> = _currentStaffApplication.asStateFlow()

    private val _allStaffApplications = MutableStateFlow<List<StaffApplication>>(emptyList())
    val allStaffApplications: StateFlow<List<StaffApplication>> = _allStaffApplications.asStateFlow()

    // Status / Feedback
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Sound & Alert for new rescue requests
    private var previousPendingCount: Int = -1
    private val _newRescueAlertEvent = MutableStateFlow<RescueRequest?>(null)
    val newRescueAlertEvent: StateFlow<RescueRequest?> = _newRescueAlertEvent.asStateFlow()

    fun clearNewRescueAlertEvent() {
        _newRescueAlertEvent.value = null
    }

    // Realtime Status Change Alert & Push Notification
    private val _statusChangeEvent = MutableStateFlow<StatusChangeEvent?>(null)
    val statusChangeEvent: StateFlow<StatusChangeEvent?> = _statusChangeEvent.asStateFlow()

    private val knownRequestStatuses = mutableMapOf<String, String>()

    fun clearStatusChangeEvent() {
        _statusChangeEvent.value = null
    }

    fun notifyStatusTransition(request: RescueRequest, previousStatus: String, newStatus: String) {
        if (previousStatus != newStatus) {
            Log.i(TAG, "Notifying status transition for ${request.id}: $previousStatus -> $newStatus")
            val event = StatusChangeEvent(
                request = request,
                previousStatus = previousStatus,
                newStatus = newStatus
            )
            _statusChangeEvent.value = event
            RescueNotificationHelper.showStatusNotification(
                context = getApplication(),
                request = request,
                newStatus = newStatus
            )
            RescueNotificationHelper.playAlertSoundAndVibration(getApplication())
        }
    }

    private fun checkAndNotifyStatusChanges(requests: List<RescueRequest>) {
        for (req in requests) {
            val previous = knownRequestStatuses[req.id]
            if (previous != null && previous != req.status) {
                notifyStatusTransition(req, previous, req.status)
            }
            knownRequestStatuses[req.id] = req.status
        }
    }

    fun triggerManualStatusNotification(request: RescueRequest, newStatus: String) {
        val previous = request.status
        val updated = request.copy(status = newStatus)
        notifyStatusTransition(updated, previous, newStatus)
    }

    // My Vehicles (Gara của tôi) & Registration/Insurance Expiry Reminders
    private val _savedVehicles = MutableStateFlow<List<SavedVehicle>>(
        listOf(
            SavedVehicle(
                id = "veh_01",
                name = "VinFast VF8 Plus",
                vehicleType = "Ô tô 4-7 chỗ",
                licensePlate = "30K-999.88",
                inspectionExpiry = "15/12/2026",
                insuranceExpiry = "28/11/2026",
                notes = "SUV điện pin 82kWh"
            ),
            SavedVehicle(
                id = "veh_02",
                name = "Honda CR-V Turbo",
                vehicleType = "Ô tô 4-7 chỗ",
                licensePlate = "51H-123.45",
                inspectionExpiry = "20/10/2026",
                insuranceExpiry = "05/11/2026",
                notes = "Xăng Turbo 1.5L"
            ),
            SavedVehicle(
                id = "veh_03",
                name = "Honda SH 150i ABS",
                vehicleType = "Xe máy",
                licensePlate = "29D1-888.99",
                inspectionExpiry = "Miễn phí",
                insuranceExpiry = "10/01/2027",
                notes = "Xe tay ga đô thị"
            )
        )
    )
    val savedVehicles: StateFlow<List<SavedVehicle>> = _savedVehicles.asStateFlow()

    fun addSavedVehicle(
        name: String,
        vehicleType: String,
        licensePlate: String,
        inspectionExpiry: String = "15/12/2026",
        insuranceExpiry: String = "28/11/2026",
        notes: String = ""
    ) {
        val newVeh = SavedVehicle(
            id = "veh_${System.currentTimeMillis()}",
            name = name,
            vehicleType = vehicleType,
            licensePlate = licensePlate,
            inspectionExpiry = inspectionExpiry,
            insuranceExpiry = insuranceExpiry,
            notes = notes
        )
        _savedVehicles.value = _savedVehicles.value + newVeh
        _statusMessage.value = "Đã thêm $name ($licensePlate) vào Gara cá nhân"
    }

    fun removeSavedVehicle(vehicleId: String) {
        _savedVehicles.value = _savedVehicles.value.filter { it.id != vehicleId }
        _statusMessage.value = "Đã xóa xe khỏi Gara"
    }

    fun sendOfflineSosSms(
        context: Context,
        hotline: String = AppConfig.DEFAULT_RESCUE_HOTLINE,
        licensePlate: String = ""
    ) {
        try {
            val loc = _currentLocation.value
            val plateInfo = if (licensePlate.isNotBlank()) "Bien so xe: $licensePlate\n" else ""
            val smsText = "SOS CUU HO KHAN CAP!\n" +
                    "Toa do GPS: ${String.format(java.util.Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)}\n" +
                    "Dia chi gan nhat: ${loc.address}\n" +
                    plateInfo +
                    "Xe hong giua duong mat mang 4G, can cuu ho ho tro ngay lap tuc!"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$hotline")
                putExtra("sms_body", smsText)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "sendOfflineSosSms error: ${e.message}")
            Toast.makeText(context, "Không thể mở ứng dụng tin nhắn SMS", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareRescueTrackingWithFamily(context: Context, request: RescueRequest) {
        try {
            val trackingText = "🚨 Mình đang gặp sự cố xe cần cứu hộ khẩn cấp!\n" +
                    "📍 Vị trí: ${request.address}\n" +
                    "🗺️ Bản đồ vị trí: https://maps.google.com/?q=${request.latitude},${request.longitude}\n" +
                    "🔧 KTV cứu hộ: ${request.staffName ?: "Đội Cứu Hộ 24/7"} (SĐT: ${request.staffPhone ?: AppConfig.DEFAULT_RESCUE_HOTLINE})\n" +
                    "🚗 Sự cố: ${request.issueType} - Xe: ${request.vehicleType} (${request.licensePlate})\n" +
                    "Mã đơn: #${request.id.takeLast(6)}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, trackingText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ hành trình cứu hộ cho người thân qua:"))
        } catch (e: Exception) {
            Log.e(TAG, "shareRescueTrackingWithFamily error: ${e.message}")
        }
    }

    init {
        // Seed initial active rescue requests for realistic workflow
        _allRequests.value = listOf(
            RescueRequest(
                id = "CH247_0821",
                userId = "usr_client_01",
                userName = "Trần Minh Quang",
                userPhone = "0912345678",
                issueType = "Hỏng xe",
                description = "Xe chết máy trên đường, cần xe cẩu kéo về gara",
                address = "Số 115 Cầu Giấy, Hà Nội",
                latitude = 21.0333,
                longitude = 105.7997,
                vehicleType = "Ô tô 4-7 chỗ",
                licensePlate = "30E-668.22",
                status = AppConfig.RequestStatus.PENDING,
                timestamp = System.currentTimeMillis() - 15 * 60 * 1000
            ),
            RescueRequest(
                id = "CH247_0822",
                userId = "usr_client_02",
                userName = "Lê Thị Thảo",
                userPhone = "0988776655",
                issueType = "Hết bình",
                description = "Xe để quên đèn qua đêm không đề nổ được, cần kích bình",
                address = "Ngã Tư Sở, Thanh Xuân, Hà Nội",
                latitude = 21.0028,
                longitude = 105.8208,
                vehicleType = "Ô tô 4-7 chỗ",
                licensePlate = "29A-992.11",
                status = AppConfig.RequestStatus.PENDING,
                timestamp = System.currentTimeMillis() - 5 * 60 * 1000
            )
        )
        // Record baseline statuses to avoid duplicate alerts on cold launch
        _allRequests.value.forEach { knownRequestStatuses[it.id] = it.status }
        refreshLocation()
        observeCurrentUserRequests()
        observeAllRequestsForStaff()
        observeStaffApplications()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateLocationPermissionStatus(granted: Boolean) {
        val hasFine = granted || locationTracker.hasFineLocationPermission()
        _hasFineLocationPermission.value = hasFine
        if (hasFine) {
            refreshLocation()
            startRealtimeLocationTracking()
            _statusMessage.value = "Đã cấp quyền vị trí chính xác (Fine Location)"
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isLocating.value = true
            try {
                val loc = locationTracker.getCurrentLocation()
                _currentLocation.value = loc
                _hasFineLocationPermission.value = loc.hasFineLocation
                Log.i(TAG, "Location refreshed: ${loc.formattedCoordinates}, accuracy=${loc.accuracy}m, fine=${loc.hasFineLocation}")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing location: ${e.message}", e)
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun startRealtimeLocationTracking() {
        locationTrackingJob?.cancel()
        _isRealtimeTrackingActive.value = true
        locationTrackingJob = viewModelScope.launch {
            Log.i(TAG, "Starting real-time location stream...")
            locationTracker.getRealtimeLocationFlow(intervalMs = 3500L)
                .catch { e -> Log.w(TAG, "Real-time location stream warning: ${e.message}") }
                .collect { locationInfo ->
                    _currentLocation.value = locationInfo
                    _hasFineLocationPermission.value = locationInfo.hasFineLocation
                }
        }
    }

    fun stopRealtimeLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
        _isRealtimeTrackingActive.value = false
    }

    fun toggleRealtimeTracking() {
        if (_isRealtimeTrackingActive.value) {
            stopRealtimeLocationTracking()
            _statusMessage.value = "Đã tạm dừng theo dõi GPS trực tiếp"
        } else {
            startRealtimeLocationTracking()
            _statusMessage.value = "Đang kích hoạt chế độ theo dõi cứu hộ trực tiếp"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopRealtimeLocationTracking()
    }

    private fun observeCurrentUserRequests() {
        viewModelScope.launch {
            firebaseManager.getAuthStateFlow().collect { user ->
                if (user != null) {
                    // Listen to requests of the current user
                    firebaseManager.getUserRequestsFlow(user.uid)
                        .catch { e -> Log.w("RescueVM", "getUserRequestsFlow error: ${e.message}") }
                        .collect { list ->
                            checkAndNotifyStatusChanges(list)
                            _userRequests.value = list
                            // Active request is the latest request that is not COMPLETED or CANCELLED
                            val active = list.firstOrNull {
                                it.status != AppConfig.RequestStatus.COMPLETED &&
                                it.status != AppConfig.RequestStatus.CANCELLED
                            }
                            _activeRequest.value = active
                        }
                } else {
                    _userRequests.value = emptyList()
                    _activeRequest.value = null
                }
            }
        }

        viewModelScope.launch {
            firebaseManager.getAuthStateFlow().collect { user ->
                if (user != null) {
                    firebaseManager.getUserRatingsFlow(user.uid)
                        .catch { e -> Log.w("RescueVM", "getUserRatingsFlow error: ${e.message}") }
                        .collect { ratings ->
                            _userRatings.value = ratings
                        }
                } else {
                    _userRatings.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            firebaseManager.getAuthStateFlow().collect { user ->
                if (user != null) {
                    firebaseManager.getUserReportsFlow(user.uid)
                        .catch { e -> Log.w("RescueVM", "getUserReportsFlow error: ${e.message}") }
                        .collect { reports ->
                            _userReports.value = reports
                        }
                } else {
                    _userReports.value = emptyList()
                }
            }
        }
    }

    private fun observeAllRequestsForStaff() {
        viewModelScope.launch {
            firebaseManager.getAuthStateFlow().collect { user ->
                if (user != null) {
                    firebaseManager.getAllRequestsFlow()
                        .catch { e -> Log.w("RescueVM", "getAllRequestsFlow error: ${e.message}") }
                        .collect { list ->
                            checkAndNotifyStatusChanges(list)
                            _allRequests.value = list

                            // Detect new pending rescue request to trigger alert
                            val currentPending = list.filter { it.status == AppConfig.RequestStatus.PENDING }
                            if (previousPendingCount != -1 && currentPending.size > previousPendingCount) {
                                val latest = currentPending.maxByOrNull { it.timestamp }
                                _newRescueAlertEvent.value = latest
                            }
                            previousPendingCount = currentPending.size

                            // If a request is currently selected, update its reference
                            val currentSelected = _selectedRequest.value
                            if (currentSelected != null) {
                                val updated = list.find { it.id == currentSelected.id }
                                if (updated != null) {
                                    _selectedRequest.value = updated
                                }
                            }
                        }
                } else {
                    _allRequests.value = emptyList()
                    previousPendingCount = -1
                }
            }
        }
    }

    fun playNewOrderSound(context: Context) {
        try {
            val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 800)

            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "playNewOrderSound error: ${e.message}")
        }
    }

    fun selectRequest(request: RescueRequest?) {
        _selectedRequest.value = request
        if (request != null) {
            observeChat(request.id)
        } else {
            _chatMessages.value = emptyList()
        }
    }

    fun observeChat(requestId: String) {
        viewModelScope.launch {
            firebaseManager.getChatMessagesFlow(requestId)
                .catch { e -> Log.w("RescueVM", "getChatMessagesFlow error: ${e.message}") }
                .collect { messages ->
                    _chatMessages.value = messages
                }
        }
    }

    // -------------------------------------------------------------
    // REQUEST OPERATIONS (Realtime Database "requests")
    // -------------------------------------------------------------

    fun createRescueRequest(
        userProfile: UserProfile,
        issueType: String,
        description: String,
        vehicleType: String,
        licensePlate: String,
        imageUrl: String? = null,
        onSuccess: (RescueRequest) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val loc = _currentLocation.value
            val reqId = "REQ_${System.currentTimeMillis()}"

            val request = RescueRequest(
                id = reqId,
                userId = userProfile.uid.ifBlank { "user_${System.currentTimeMillis()}" },
                userName = userProfile.displayName.ifBlank { "Khách hàng" },
                userPhone = userProfile.phone.ifBlank { "0987654321" },
                issueType = issueType,
                description = description,
                address = loc.address,
                latitude = loc.latitude,
                longitude = loc.longitude,
                vehicleType = vehicleType.ifBlank { userProfile.vehicleType },
                licensePlate = licensePlate.ifBlank { userProfile.licensePlate },
                status = AppConfig.RequestStatus.PENDING,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )

            // Optimistic update
            _userRequests.value = listOf(request) + _userRequests.value.filter { it.id != reqId }
            _allRequests.value = listOf(request) + _allRequests.value.filter { it.id != reqId }
            _activeRequest.value = request
            _selectedRequest.value = request
            _isLoading.value = false
            _statusMessage.value = "Yêu cầu cứu hộ đã gửi thành công! Đang điều phối thợ gần nhất."

            onSuccess(request)

            try {
                firebaseManager.createRescueRequest(request)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }

            // Auto-dispatch simulation: Kỹ thuật viên tự động nhận đơn sau 3.5 giây để trải nghiệm luồng thực tế
            if (userProfile.role != AppConfig.UserRole.STAFF) {
                launch {
                    kotlinx.coroutines.delay(3500)
                    val currentReq = _allRequests.value.find { it.id == reqId }
                    if (currentReq != null && currentReq.status == AppConfig.RequestStatus.PENDING) {
                        val mockStaff = UserProfile(
                            uid = "staff_toan_247",
                            displayName = "KTV Nguyễn Văn Toàn (Gara Cứu Hộ 24/7)",
                            phone = "0901234567",
                            role = AppConfig.UserRole.STAFF,
                            vehicleType = "Xe cứu hộ sàn trượt",
                            licensePlate = "29C-888.12"
                        )
                        acceptRequestByStaff(reqId, mockStaff)
                        _statusMessage.value = "⚡ KTV Nguyễn Văn Toàn đã tiếp nhận đơn của bạn và đang xuất phát!"
                        
                        // KTV nhắn tin chào khách hàng
                        kotlinx.coroutines.delay(1200)
                        val welcomeMsg = ChatMessage(
                            id = "MSG_WELCOME_${System.currentTimeMillis()}",
                            requestId = reqId,
                            senderId = mockStaff.uid,
                            senderName = mockStaff.displayName,
                            senderRole = AppConfig.UserRole.STAFF,
                            receiverId = userProfile.uid,
                            message = "Chào bạn! Tôi là KTV Toàn bên đội cứu hộ. Tôi đã nhận đơn và đang xuất phát đến vị trí của bạn ngay nhé.",
                            timestamp = System.currentTimeMillis()
                        )
                        _chatMessages.value = _chatMessages.value + welcomeMsg
                        try {
                            firebaseManager.sendChatMessage(welcomeMsg)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun acceptRequestByStaff(requestId: String, staffProfile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _allRequests.value
            val target = currentList.find { it.id == requestId }
            if (target != null) {
                val prev = target.status
                val updated = target.copy(
                    status = AppConfig.RequestStatus.ACCEPTED,
                    staffId = staffProfile.uid,
                    staffName = staffProfile.displayName,
                    staffPhone = staffProfile.phone
                )
                knownRequestStatuses[requestId] = AppConfig.RequestStatus.ACCEPTED
                notifyStatusTransition(updated, prev, AppConfig.RequestStatus.ACCEPTED)
                _allRequests.value = currentList.map { if (it.id == requestId) updated else it }
                _userRequests.value = _userRequests.value.map { if (it.id == requestId) updated else it }
                if (_selectedRequest.value?.id == requestId) _selectedRequest.value = updated
                if (_activeRequest.value?.id == requestId) _activeRequest.value = updated
            }
            _isLoading.value = false
            _statusMessage.value = "Đã tiếp nhận yêu cầu cứu hộ thành công"

            try {
                firebaseManager.updateRequestStatus(
                    requestId = requestId,
                    status = AppConfig.RequestStatus.ACCEPTED,
                    staffId = staffProfile.uid,
                    staffName = staffProfile.displayName,
                    staffPhone = staffProfile.phone
                )
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String, cost: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _allRequests.value
            val target = currentList.find { it.id == requestId }
            if (target != null) {
                val prev = target.status
                val updated = target.copy(
                    status = newStatus,
                    cost = cost ?: target.cost
                )
                knownRequestStatuses[requestId] = newStatus
                notifyStatusTransition(updated, prev, newStatus)
                _allRequests.value = currentList.map { if (it.id == requestId) updated else it }
                _userRequests.value = _userRequests.value.map { if (it.id == requestId) updated else it }
                if (_selectedRequest.value?.id == requestId) _selectedRequest.value = updated
                if (_activeRequest.value?.id == requestId) _activeRequest.value = updated
            }
            _isLoading.value = false
            _statusMessage.value = "Đã cập nhật trạng thái: $newStatus"

            try {
                firebaseManager.updateRequestStatus(
                    requestId = requestId,
                    status = newStatus,
                    cost = cost
                )
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }
        }
    }

    fun updatePaymentStatus(requestId: String, paymentStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _allRequests.value
            val target = currentList.find { it.id == requestId }
            if (target != null) {
                val updated = target.copy(paymentStatus = paymentStatus)
                _allRequests.value = currentList.map { if (it.id == requestId) updated else it }
                _userRequests.value = _userRequests.value.map { if (it.id == requestId) updated else it }
                if (_selectedRequest.value?.id == requestId) _selectedRequest.value = updated
                if (_activeRequest.value?.id == requestId) _activeRequest.value = updated
            }
            _isLoading.value = false
            _statusMessage.value = if (paymentStatus.startsWith("PAID")) "Đã xác nhận thanh toán thành công!" else "Cập nhật thanh toán"
            try {
                firebaseManager.updatePaymentStatus(requestId, paymentStatus)
            } catch (e: Exception) {
                Log.w(TAG, "updatePaymentStatus error: ${e.message}")
            }
        }
    }

    fun submitRating(
        requestId: String,
        staffId: String = "",
        staffName: String = "",
        rating: Int,
        comment: String,
        userProfile: UserProfile? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _allRequests.value
            val target = currentList.find { it.id == requestId }
            if (target != null) {
                val updated = target.copy(rating = rating, ratingComment = comment)
                _allRequests.value = currentList.map { if (it.id == requestId) updated else it }
                _userRequests.value = _userRequests.value.map { if (it.id == requestId) updated else it }
                if (_selectedRequest.value?.id == requestId) _selectedRequest.value = updated
                if (_activeRequest.value?.id == requestId) _activeRequest.value = updated
            }
            _isLoading.value = false
            _statusMessage.value = "Cảm ơn bạn đã gửi đánh giá $rating sao!"
            try {
                firebaseManager.submitRating(requestId, rating, comment)
            } catch (e: Exception) {
                Log.w(TAG, "submitRating error: ${e.message}")
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val currentList = _allRequests.value
            val target = currentList.find { it.id == requestId }
            if (target != null) {
                val prev = target.status
                val updated = target.copy(status = AppConfig.RequestStatus.CANCELLED)
                knownRequestStatuses[requestId] = AppConfig.RequestStatus.CANCELLED
                notifyStatusTransition(updated, prev, AppConfig.RequestStatus.CANCELLED)
                _allRequests.value = currentList.map { if (it.id == requestId) updated else it }
                _userRequests.value = _userRequests.value.map { if (it.id == requestId) updated else it }
                if (_selectedRequest.value?.id == requestId) _selectedRequest.value = updated
                if (_activeRequest.value?.id == requestId) _activeRequest.value = null
            }
            _isLoading.value = false
            _statusMessage.value = "Đã hủy yêu cầu cứu hộ"

            try {
                firebaseManager.cancelRequest(requestId)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // CHAT OPERATIONS (Realtime Database "chats")
    // -------------------------------------------------------------

    fun sendChatMessage(
        requestId: String,
        text: String,
        userProfile: UserProfile,
        receiverId: String = ""
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val msg = ChatMessage(
                id = "MSG_${System.currentTimeMillis()}",
                requestId = requestId,
                senderId = userProfile.uid,
                senderName = userProfile.displayName.ifBlank { "Người dùng" },
                senderRole = userProfile.role,
                receiverId = receiverId,
                message = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + msg

            try {
                firebaseManager.sendChatMessage(msg)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }

            // Tự động phản hồi tương tác tin nhắn (đảm bảo tính năng chat luôn phản hồi sống động)
            launch {
                kotlinx.coroutines.delay(1600)
                val targetReq = _allRequests.value.find { it.id == requestId }
                if (userProfile.role != AppConfig.UserRole.STAFF) {
                    // Khách nhắn -> KTV trả lời
                    val staffName = targetReq?.staffName ?: "KTV Nguyễn Văn Toàn"
                    val staffUid = targetReq?.staffId ?: "staff_toan_247"
                    val replyCount = _chatMessages.value.count { it.senderId == staffUid }
                    val replyText = when (replyCount) {
                        0, 1 -> "Tôi đang chạy xe đến vị trí của bạn, bạn yên tâm đứng ở nơi an toàn nhé."
                        2 -> "Tôi cách bạn khoảng 1km nữa, bạn bật đèn cảnh báo sự cố (hazard) trên xe giúp tôi nhé."
                        3 -> "Tôi đã nhìn thấy xe của bạn rồi, tôi đang tấp xe cứu hộ vào lề đường."
                        else -> "Đã nhận thông tin từ bạn! Tôi đang xử lý nhanh nhất có thể."
                    }
                    val reply = ChatMessage(
                        id = "MSG_REPLY_${System.currentTimeMillis()}",
                        requestId = requestId,
                        senderId = staffUid,
                        senderName = staffName,
                        senderRole = AppConfig.UserRole.STAFF,
                        receiverId = userProfile.uid,
                        message = replyText,
                        timestamp = System.currentTimeMillis()
                    )
                    _chatMessages.value = _chatMessages.value + reply
                    try { firebaseManager.sendChatMessage(reply) } catch (_: Exception) {}
                } else {
                    // KTV nhắn -> Khách trả lời
                    val custName = targetReq?.userName ?: "Khách hàng"
                    val custUid = targetReq?.userId ?: "usr_client_01"
                    val replyText = "Dạ vâng anh, em đang đứng đợi cạnh xe ở lề đường ạ!"
                    val reply = ChatMessage(
                        id = "MSG_REPLY_${System.currentTimeMillis()}",
                        requestId = requestId,
                        senderId = custUid,
                        senderName = custName,
                        senderRole = AppConfig.UserRole.USER,
                        receiverId = userProfile.uid,
                        message = replyText,
                        timestamp = System.currentTimeMillis()
                    )
                    _chatMessages.value = _chatMessages.value + reply
                    try { firebaseManager.sendChatMessage(reply) } catch (_: Exception) {}
                }
            }
        }
    }

    fun simulateFullRescueWorkflow(requestId: String) {
        viewModelScope.launch {
            val staffProfile = UserProfile(
                uid = "staff_toan_247",
                displayName = "KTV Nguyễn Văn Toàn (Gara Cứu Hộ 24/7)",
                phone = "0901234567",
                role = AppConfig.UserRole.STAFF,
                vehicleType = "Xe cứu hộ sàn trượt",
                licensePlate = "29C-888.12"
            )

            // Bước 1: Tiếp nhận
            acceptRequestByStaff(requestId, staffProfile)
            _statusMessage.value = "⚡ KTV Nguyễn Văn Toàn đã tiếp nhận đơn"
            sendChatMessage(requestId, "Chào bạn! Tôi đã tiếp nhận đơn cứu hộ và xuất phát ngay.", staffProfile)

            // Bước 2: Đang đến
            kotlinx.coroutines.delay(3500)
            updateRequestStatus(requestId, AppConfig.RequestStatus.EN_ROUTE)
            sendChatMessage(requestId, "Tôi đang di chuyển trên đường, khoảng 5 phút nữa đến nơi.", staffProfile)

            // Bước 3: Đã đến nơi
            kotlinx.coroutines.delay(4000)
            updateRequestStatus(requestId, AppConfig.RequestStatus.ARRIVED)
            sendChatMessage(requestId, "Tôi đã đến nơi và đang tiến hành kiểm tra phương tiện.", staffProfile)

            // Bước 4: Đang xử lý
            kotlinx.coroutines.delay(4000)
            updateRequestStatus(requestId, AppConfig.RequestStatus.IN_PROGRESS)

            // Bước 5: Hoàn thành & tính chi phí
            kotlinx.coroutines.delay(4000)
            updateRequestStatus(requestId, AppConfig.RequestStatus.COMPLETED, cost = 250000L)
            sendChatMessage(requestId, "Đã xử lý sự cố thành công! Bạn kiểm tra lại xe và đánh giá giúp tôi nhé.", staffProfile)
            _statusMessage.value = "🎉 Cứu hộ hoàn tất! Đơn hàng đã chuyển sang trạng thái Hoàn thành."
        }
    }

    // -------------------------------------------------------------
    // RATING OPERATIONS (Realtime Database "ratings")
    // -------------------------------------------------------------

    fun submitRating(
        requestId: String,
        staffId: String,
        staffName: String,
        rating: Int,
        comment: String,
        userProfile: UserProfile,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val item = RatingItem(
                id = "RATE_${System.currentTimeMillis()}",
                requestId = requestId,
                userId = userProfile.uid,
                userName = userProfile.displayName,
                staffId = staffId,
                staffName = staffName,
                rating = rating,
                comment = comment.trim(),
                timestamp = System.currentTimeMillis()
            )
            _userRatings.value = listOf(item) + _userRatings.value
            _isLoading.value = false
            _statusMessage.value = "Cảm ơn bạn đã gửi đánh giá dịch vụ!"
            onSuccess()

            try {
                firebaseManager.createRating(item)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync notice: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // REPORT OPERATIONS (Realtime Database "reports")
    // -------------------------------------------------------------

    fun submitReport(
        requestId: String?,
        reportType: String,
        content: String,
        userProfile: UserProfile,
        onSuccess: () -> Unit = {}
    ) {
        if (content.isBlank()) {
            _statusMessage.value = "Vui lòng nhập nội dung phản hồi/báo cáo"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val item = ReportItem(
                userId = userProfile.uid,
                userName = userProfile.displayName,
                userPhone = userProfile.phone,
                requestId = requestId,
                reportType = reportType,
                content = content.trim(),
                status = "Chờ xử lý",
                timestamp = System.currentTimeMillis()
            )
            val result = firebaseManager.createReport(item)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Báo cáo của bạn đã được ghi nhận vào hệ thống"
                onSuccess()
            } else {
                _statusMessage.value = "Gửi báo cáo thất bại: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // -------------------------------------------------------------
    // EMERGENCY SOS DIALER
    // -------------------------------------------------------------

    fun triggerEmergencySosDialer(context: Context, phoneNumber: String = AppConfig.DEFAULT_RESCUE_HOTLINE) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng gọi điện thoại", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchNavigation(context: Context, lat: Double, lng: Double, label: String = "Vị trí sự cố") {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {}

        try {
            val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            return
        } catch (_: Exception) {}

        try {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng bản đồ chỉ đường", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLocation(context: Context, address: String, lat: Double, lng: Double) {
        try {
            val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
            val text = "Tôi đang gặp sự cố xe cần cứu hộ gấp tại:\n📍 $address\n🔗 Tọa độ: $mapsUrl"
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

    // -------------------------------------------------------------
    // PARTNER / STAFF REGISTRATION & REVIEW WORKFLOW
    // -------------------------------------------------------------

    fun observeUserStaffApplication(userId: String) {
        viewModelScope.launch {
            firebaseManager.getStaffApplicationFlow(userId).collect { app ->
                _currentStaffApplication.value = app
            }
        }
    }

    private fun observeStaffApplications() {
        viewModelScope.launch {
            firebaseManager.getAllStaffApplicationsFlow().collect { list ->
                _allStaffApplications.value = list
            }
        }
    }

    fun submitStaffApplication(
        userProfile: UserProfile,
        fullName: String,
        phone: String,
        operatingArea: String,
        servicesOffered: String,
        idCardNumber: String,
        companyOrGarageName: String,
        garageAddress: String,
        operatingRadiusKm: Int,
        rescueVehicleType: String,
        vehiclePlate: String,
        documentImagesJson: String,
        autoApprove: Boolean = true,
        onSuccess: (StaffApplication) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val initialStatus = if (autoApprove) AppConfig.PartnerApplicationStatus.APPROVED else AppConfig.PartnerApplicationStatus.PENDING
            val app = StaffApplication(
                id = userProfile.uid,
                userId = userProfile.uid,
                userName = fullName.ifBlank { userProfile.displayName },
                userEmail = userProfile.email,
                phone = phone.ifBlank { userProfile.phone },
                idCardNumber = idCardNumber,
                operatingArea = operatingArea,
                companyOrGarageName = companyOrGarageName,
                taxOrBusinessCode = "",
                garageAddress = garageAddress,
                operatingRadiusKm = operatingRadiusKm,
                rescueVehicleType = rescueVehicleType,
                vehiclePlate = vehiclePlate,
                servicesOffered = servicesOffered,
                documentsInfo = documentImagesJson,
                documentImageUris = documentImagesJson,
                status = initialStatus,
                appliedAt = System.currentTimeMillis(),
                reviewedAt = if (autoApprove) System.currentTimeMillis() else null,
                reviewedBy = if (autoApprove) "Hệ Thống Tự Động" else null
            )

            // Optimistic update
            _currentStaffApplication.value = app
            _allStaffApplications.value = listOf(app) + _allStaffApplications.value.filter { it.userId != app.userId }
            _isLoading.value = false
            _statusMessage.value = if (autoApprove) {
                "🎉 Hồ sơ đã duyệt thành công! Bạn đã mở khóa vai trò Kỹ Thuật Viên."
            } else {
                "Hồ sơ đối tác đã gửi thành công! Đang chờ xét duyệt."
            }
            onSuccess(app)

            try {
                firebaseManager.submitStaffApplication(app)
                if (autoApprove) {
                    firebaseManager.reviewStaffApplication(userProfile.uid, true, reviewerName = "Hệ Thống Tự Động")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase submitStaffApplication sync notice: ${e.message}")
            }
        }
    }

    fun reviewStaffApplication(
        userId: String,
        approve: Boolean,
        reason: String? = null,
        reviewerName: String = "Tổng Đài Trưởng",
        onReviewed: (approved: Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val newStatus = if (approve) AppConfig.PartnerApplicationStatus.APPROVED else AppConfig.PartnerApplicationStatus.REJECTED
            val currentApp = _allStaffApplications.value.find { it.userId == userId } ?: _currentStaffApplication.value
            if (currentApp != null) {
                val updatedApp = currentApp.copy(
                    status = newStatus,
                    reviewedAt = System.currentTimeMillis(),
                    reviewedBy = reviewerName,
                    rejectionReason = reason
                )
                _currentStaffApplication.value = updatedApp
                _allStaffApplications.value = _allStaffApplications.value.map {
                    if (it.userId == userId) updatedApp else it
                }
            }
            _isLoading.value = false
            _statusMessage.value = if (approve) {
                "Đã duyệt hồ sơ đối tác! Tài khoản đã được nâng cấp thành Kỹ thuật viên."
            } else {
                "Đã từ chối hồ sơ đối tác."
            }
            onReviewed(approve)

            try {
                firebaseManager.reviewStaffApplication(userId, approve, reason, reviewerName)
            } catch (e: Exception) {
                Log.w(TAG, "Firebase reviewStaffApplication sync notice: ${e.message}")
            }
        }
    }
}
