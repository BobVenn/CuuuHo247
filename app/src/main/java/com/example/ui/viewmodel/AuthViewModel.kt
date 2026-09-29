package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseManager
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val firebaseManager = FirebaseManager.getInstance()

    val currentUser: StateFlow<FirebaseUser?> = firebaseManager.getAuthStateFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, firebaseManager.currentUser)

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    firebaseManager.getUserProfileFlow(user.uid).collect { profile ->
                        _userProfile.value = profile
                    }
                } else {
                    _userProfile.value = null
                }
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun signIn(email: String, pass: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Vui lòng nhập đầy đủ Email và Mật khẩu"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = firebaseManager.signIn(email, pass)
            _isLoading.value = false
            if (result.isSuccess) {
                _successMessage.value = "Đăng nhập thành công"
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Đăng nhập thất bại"
            }
        }
    }

    fun signUp(
        email: String,
        pass: String,
        confirmPass: String,
        displayName: String,
        phone: String,
        vehicleType: String,
        vehicleName: String,
        licensePlate: String,
        onSuccess: () -> Unit = {}
    ) {
        if (email.isBlank() || pass.isBlank() || displayName.isBlank() || phone.isBlank()) {
            _errorMessage.value = "Vui lòng điền đầy đủ các thông tin bắt buộc"
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Mật khẩu phải có tối thiểu 6 ký tự"
            return
        }
        if (pass != confirmPass) {
            _errorMessage.value = "Mật khẩu xác nhận không khớp"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = firebaseManager.signUp(
                email = email,
                pass = pass,
                displayName = displayName,
                phone = phone,
                vehicleType = vehicleType,
                vehicleName = vehicleName,
                licensePlate = licensePlate
            )
            _isLoading.value = false
            if (result.isSuccess) {
                _successMessage.value = "Tạo tài khoản thành công"
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Tạo tài khoản thất bại"
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank()) {
            _errorMessage.value = "Vui lòng nhập địa chỉ Email"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = firebaseManager.sendPasswordReset(email)
            _isLoading.value = false
            if (result.isSuccess) {
                _successMessage.value = "Đã gửi liên kết đặt lại mật khẩu đến $email. Vui lòng kiểm tra hộp thư."
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Gửi yêu cầu thất bại"
            }
        }
    }

    fun updateProfile(
        displayName: String,
        phone: String,
        vehicleType: String,
        vehicleName: String,
        licensePlate: String,
        role: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val current = _userProfile.value ?: return
        val updated = current.copy(
            displayName = displayName.ifBlank { current.displayName },
            phone = phone.ifBlank { current.phone },
            vehicleType = vehicleType.ifBlank { current.vehicleType },
            vehicleName = vehicleName.ifBlank { current.vehicleName },
            licensePlate = licensePlate.ifBlank { current.licensePlate },
            role = role ?: current.role
        )

        viewModelScope.launch {
            _isLoading.value = true
            val result = firebaseManager.updateUserProfile(updated)
            _isLoading.value = false
            if (result.isSuccess) {
                _userProfile.value = updated
                _successMessage.value = "Cập nhật thông tin thành công"
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Cập nhật thất bại"
            }
        }
    }

    fun signOut() {
        firebaseManager.signOut()
        _userProfile.value = null
    }
}
