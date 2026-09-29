package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.AppConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.model.*
import com.example.util.LocationHelper
import com.example.util.UserLocationInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RescueRealtimeViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseManager = FirebaseManager.getInstance()

    // Location State
    private val _currentLocation = MutableStateFlow(
        UserLocationInfo(
            latitude = 21.0285,
            longitude = 105.8542,
            address = "Đang xác định vị trí GPS..."
        )
    )
    val currentLocation: StateFlow<UserLocationInfo> = _currentLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

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

    // Status / Feedback
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refreshLocation()
        observeCurrentUserRequests()
        observeAllRequestsForStaff()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
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

    private fun observeCurrentUserRequests() {
        viewModelScope.launch {
            firebaseManager.getAuthStateFlow().collect { user ->
                if (user != null) {
                    // Listen to requests of the current user
                    firebaseManager.getUserRequestsFlow(user.uid).collect { list ->
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
                    firebaseManager.getUserRatingsFlow(user.uid).collect { ratings ->
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
                    firebaseManager.getUserReportsFlow(user.uid).collect { reports ->
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
            firebaseManager.getAllRequestsFlow().collect { list ->
                _allRequests.value = list
                // If a request is currently selected, update its reference
                val currentSelected = _selectedRequest.value
                if (currentSelected != null) {
                    val updated = list.find { it.id == currentSelected.id }
                    if (updated != null) {
                        _selectedRequest.value = updated
                    }
                }
            }
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
            firebaseManager.getChatMessagesFlow(requestId).collect { messages ->
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
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val loc = _currentLocation.value

            val request = RescueRequest(
                userId = userProfile.uid,
                userName = userProfile.displayName.ifBlank { "Khách hàng" },
                userPhone = userProfile.phone,
                issueType = issueType,
                description = description,
                address = loc.address,
                latitude = loc.latitude,
                longitude = loc.longitude,
                vehicleType = vehicleType.ifBlank { userProfile.vehicleType },
                licensePlate = licensePlate.ifBlank { userProfile.licensePlate },
                status = AppConfig.RequestStatus.PENDING,
                timestamp = System.currentTimeMillis()
            )

            val result = firebaseManager.createRescueRequest(request)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Yêu cầu cứu hộ đã được gửi tới hệ thống"
                val key = result.getOrThrow()
                onSuccess(key)
            } else {
                _statusMessage.value = "Lỗi khi gửi yêu cầu: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun acceptRequestByStaff(requestId: String, staffProfile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseManager.updateRequestStatus(
                requestId = requestId,
                status = AppConfig.RequestStatus.ACCEPTED,
                staffId = staffProfile.uid,
                staffName = staffProfile.displayName,
                staffPhone = staffProfile.phone
            )
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Đã tiếp nhận yêu cầu cứu hộ"
            } else {
                _statusMessage.value = "Lỗi tiếp nhận: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateRequestStatus(requestId: String, newStatus: String, cost: Long? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseManager.updateRequestStatus(
                requestId = requestId,
                status = newStatus,
                cost = cost
            )
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Đã cập nhật trạng thái: $newStatus"
            } else {
                _statusMessage.value = "Cập nhật thất bại: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseManager.cancelRequest(requestId)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Đã hủy yêu cầu cứu hộ"
            } else {
                _statusMessage.value = "Hủy thất bại: ${result.exceptionOrNull()?.localizedMessage}"
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
                requestId = requestId,
                senderId = userProfile.uid,
                senderName = userProfile.displayName.ifBlank { "Người dùng" },
                senderRole = userProfile.role,
                receiverId = receiverId,
                message = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            firebaseManager.sendChatMessage(msg)
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
                requestId = requestId,
                userId = userProfile.uid,
                userName = userProfile.displayName,
                staffId = staffId,
                staffName = staffName,
                rating = rating,
                comment = comment.trim(),
                timestamp = System.currentTimeMillis()
            )
            val result = firebaseManager.createRating(item)
            _isLoading.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Cảm ơn bạn đã gửi đánh giá dịch vụ!"
                onSuccess()
            } else {
                _statusMessage.value = "Gửi đánh giá thất bại: ${result.exceptionOrNull()?.localizedMessage}"
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
}
