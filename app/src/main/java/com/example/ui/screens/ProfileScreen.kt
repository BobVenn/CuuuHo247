package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RatingItem
import com.example.data.model.SavedVehicle
import com.example.data.model.UserProfile
import com.example.ui.components.UserProfileEmergencyManagerComponent
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser

/**
 * Màn hình Hồ Sơ Người Dùng & Điều Phối Khẩn Cấp (Emergency Dispatch User Profile)
 * - Tích hợp Firebase Auth quản lý phiên đăng nhập và định danh tài xế.
 * - Lưu trữ thông tin liên hệ khẩn cấp (Primary Phone, ICE Emergency Relative, Thẻ Y Tế, Gara xe)
 *   giúp tổng đài 24/7 và kỹ thuật viên cứu hộ nhận diện, điều phối xe cứu hộ tức thì (< 15s).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    currentUser: FirebaseUser? = null,
    myRatings: List<RatingItem> = emptyList(),
    savedVehicles: List<SavedVehicle> = emptyList(),
    onAddVehicle: ((name: String, vType: String, plate: String, inspection: String, insurance: String) -> Unit)? = null,
    onRemoveVehicle: ((String) -> Unit)? = null,
    onUpdateProfile: (name: String, phone: String, vehicleType: String, vehicleName: String, licensePlate: String, role: String?) -> Unit,
    onUpdateEmergencyProfile: ((
        displayName: String,
        phone: String,
        vehicleType: String,
        vehicleName: String,
        licensePlate: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        emergencyRelationship: String,
        bloodType: String,
        emergencyNotes: String,
        homeAddress: String
    ) -> Unit)? = null,
    onSwitchRole: ((String) -> Unit)? = null,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showRatingsSheet by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }

    // Tính toán độ hoàn thiện hồ sơ cứu hộ khẩn cấp (0% - 100%)
    val hasDriverName = !userProfile?.displayName.isNullOrBlank()
    val hasDriverPhone = !userProfile?.phone.isNullOrBlank() && userProfile?.phone != "0987654321"
    val hasVehicle = !userProfile?.licensePlate.isNullOrBlank()
    val hasEmergencyContact = !userProfile?.emergencyContactPhone.isNullOrBlank()
    val hasMedicalOrNotes = !userProfile?.bloodType.isNullOrBlank() || !userProfile?.emergencyNotes.isNullOrBlank()

    var readinessScore = 0
    if (hasDriverName) readinessScore += 20
    if (hasDriverPhone) readinessScore += 30
    if (hasVehicle) readinessScore += 20
    if (hasEmergencyContact) readinessScore += 20
    if (hasMedicalOrNotes) readinessScore += 10
    if (readinessScore > 100) readinessScore = 100

    val isFullyReady = readinessScore >= 90
    val animatedProgress by animateFloatAsState(
        targetValue = readinessScore / 100f,
        label = "readiness_progress"
    )

    // Form thêm xe vào gara
    var newVehicleName by remember { mutableStateOf("") }
    var newVehicleType by remember { mutableStateOf("Ô tô 4-7 chỗ") }
    var newVehiclePlate by remember { mutableStateOf("") }
    var newVehicleInspection by remember { mutableStateOf("15/12/2026") }
    var newVehicleInsurance by remember { mutableStateOf("28/11/2026") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hồ Sơ Cứu Hộ & Tài Khoản",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dữ liệu liên hệ điều phối khẩn cấp 24/7",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cập nhật", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. FIREBASE AUTH USER CARD (Thông tin tài khoản & Xác thực)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_firebase_auth_user"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RescuePrimary,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = userProfile?.displayName?.ifBlank { "Tài Xế Cứu Hộ" } ?: "Khách hàng",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = currentUser?.email ?: userProfile?.email?.ifBlank { "Chưa liên kết email" } ?: "Chưa có email",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SafeGreen.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(SafeGreen, CircleShape))
                                            Text(
                                                text = "Đã xác thực",
                                                color = SafeGreen,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = RescuePrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (userProfile?.role == AppConfig.UserRole.STAFF) "Kỹ Thuật Viên" else "Tài Xế Khách Hàng",
                                            color = RescuePrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = "Sửa hồ sơ", tint = RescuePrimary, modifier = Modifier.size(26.dp))
                        }
                    }

                    // Mã định danh tài khoản với nút Copy
                    val displayUid = currentUser?.uid ?: userProfile?.uid ?: "chua_co_uid"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(displayUid))
                                Toast.makeText(context, "Đã sao chép mã tài khoản: $displayUid", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = OnSurfaceVariantLight, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Mã tài khoản: ${displayUid.take(16)}...",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariantLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Sao chép", fontSize = 10.sp, color = RescuePrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. USER PROFILE EMERGENCY MANAGER COMPONENT
            // =================================================================
            UserProfileEmergencyManagerComponent(
                userProfile = userProfile,
                savedVehicles = savedVehicles,
                onUpdateEmergencyProfile = { name, phone, vType, vName, plate, iceName, icePhone, iceRel, blood, notes, addr ->
                    onUpdateEmergencyProfile?.invoke(name, phone, vType, vName, plate, iceName, icePhone, iceRel, blood, notes, addr)
                        ?: onUpdateProfile(name, phone, vType, vName, plate, userProfile?.role)
                },
                onAddSavedVehicle = onAddVehicle,
                onRemoveSavedVehicle = onRemoveVehicle,
                isInitiallyExpanded = true
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_emergency_dispatch_readiness"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFullyReady) SafeGreen.copy(alpha = 0.08f) else AlertRedLight
                ),
                border = BorderStroke(
                    1.dp,
                    if (isFullyReady) SafeGreen.copy(alpha = 0.4f) else AlertRed.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isFullyReady) SafeGreen else AlertRed,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isFullyReady) Icons.Default.CheckCircle else Icons.Default.Emergency,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (isFullyReady) "SẴN SÀNG ĐIỀU PHỐI KHẨN CẤP" else "HỒ SƠ CỨU HỘ CHƯA ĐẦY ĐỦ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isFullyReady) SafeGreen else AlertRed
                                )
                                Text(
                                    text = if (isFullyReady) "Kỹ thuật viên kết nối ngay trong < 15 giây" else "Bổ sung thông tin để điều phối thợ nhanh nhất",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariantLight
                                )
                            }
                        }

                        Text(
                            text = "$readinessScore%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isFullyReady) SafeGreen else AlertRed
                        )
                    }

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isFullyReady) SafeGreen else AlertRed,
                        trackColor = Color.White.copy(alpha = 0.7f)
                    )

                    // Checklist chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ChecklistChip(title = "SĐT Tài Xế", isChecked = hasDriverPhone)
                        ChecklistChip(title = "Biển Số Xe", isChecked = hasVehicle)
                        ChecklistChip(title = "Người Thân ICE", isChecked = hasEmergencyContact)
                        ChecklistChip(title = "Y Tế / Ghi Chú", isChecked = hasMedicalOrNotes)
                    }

                    if (!isFullyReady) {
                        Button(
                            onClick = { showEditProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BỔ SUNG THÔNG TIN CỨU HỘ NGAY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // =================================================================
            // 3. THÔNG TIN LIÊN HỆ TÀI XẾ & ĐỊA CHỈ ƯU TIÊN (PRIMARY DRIVER INFO)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_primary_driver_info"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ContactPhone, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                            Text("Thông Tin Liên Hệ Tài Xế", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = { showEditProfileDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Thay đổi", fontSize = 11.5.sp, color = RescuePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))

                    // Họ tên & SĐT
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Họ và tên tài xế",
                        value = userProfile?.displayName?.ifBlank { "Chưa cập nhật tên" } ?: "Chưa cập nhật"
                    )

                    val phoneDisplay = userProfile?.phone?.ifBlank { "Chưa có số điện thoại" } ?: "Chưa cập nhật"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoRow(
                            icon = Icons.Default.Phone,
                            label = "Số điện thoại chính",
                            value = phoneDisplay,
                            modifier = Modifier.weight(1f)
                        )

                        if (!userProfile?.phone.isNullOrBlank()) {
                            FilledTonalIconButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${userProfile?.phone}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(dialIntent)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Gọi thử", tint = RescuePrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    InfoRow(
                        icon = Icons.Default.Home,
                        label = "Địa chỉ ưu tiên / Nhà riêng",
                        value = userProfile?.homeAddress?.ifBlank { "Chưa lưu địa chỉ thường trú" } ?: "Chưa có địa chỉ"
                    )
                }
            }

            // =================================================================
            // 4. NGƯỜI THÂN KHẨN CẤP (IN CASE OF EMERGENCY - ICE CONTACT)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_ice_emergency_contact"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AlertRed.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Người Thân Khẩn Cấp (ICE)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = AlertRed) {
                                        Text("KHI GẶP NẠN", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                                Text("Liên hệ khi tài xế bất tỉnh hoặc sự cố nghiêm trọng", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa ICE", tint = AlertRed, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))

                    if (!userProfile?.emergencyContactName.isNullOrBlank() || !userProfile?.emergencyContactPhone.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = userProfile?.emergencyContactName ?: "Chưa rõ họ tên",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Surface(shape = RoundedCornerShape(4.dp), color = CategoryBlueBg) {
                                        Text(
                                            text = userProfile?.emergencyRelationship?.ifBlank { "Người thân" } ?: "Người thân",
                                            color = RescuePrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = userProfile?.emergencyContactPhone ?: "Chưa có SĐT",
                                    fontSize = 13.sp,
                                    color = OnSurfaceLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!userProfile?.emergencyContactPhone.isNullOrBlank()) {
                                Button(
                                    onClick = {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${userProfile?.emergencyContactPhone}")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(dialIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gọi ICE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AlertRedLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Chưa cài đặt số điện thoại người thân", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AlertRed)
                                    Text("Thêm số liên hệ của vợ/chồng hoặc người nhà để cứu hộ 24/7 gọi khi khẩn cấp.", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                                }
                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Thêm ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 5. THẺ Y TẾ & GHI CHÚ AN TOÀN LÁI XE (MEDICAL & DRIVER SAFETY)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_medical_safety_notes"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                            Text("Thẻ Y Tế & Ghi Chú Cứu Hộ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = { showEditProfileDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Chỉnh sửa", fontSize = 11.5.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Nhóm máu:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (!userProfile?.bloodType.isNullOrBlank()) SafeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = userProfile?.bloodType?.ifBlank { "Chưa rõ" } ?: "Chưa rõ",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = if (!userProfile?.bloodType.isNullOrBlank()) SafeGreen else OnSurfaceVariantLight,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ghi chú y tế & cứu hộ xe:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                            Text(
                                text = userProfile?.emergencyNotes?.ifBlank { "Không có bệnh nền, dị ứng hoặc yêu cầu đặc biệt." } ?: "Không có ghi chú.",
                                fontSize = 12.sp,
                                color = OnSurfaceLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 6. PHƯƠNG TIỆN CỨU HỘ MẶC ĐỊNH & GARA XE
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_default_vehicle_info"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                            Text("Phương Tiện Cứu Hộ Mặc Định", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmergencyGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TỰ ĐỘNG CHỌN",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = EmergencyGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${userProfile?.vehicleType} • ${userProfile?.vehicleName?.ifBlank { "Chưa đặt tên xe" }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Biển số lưu hồ sơ: ${userProfile?.licensePlate?.ifBlank { "Chưa cập nhật" }}",
                                fontSize = 12.sp,
                                color = OnSurfaceVariantLight
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, BorderLight)
                        ) {
                            Text(
                                text = userProfile?.licensePlate?.ifBlank { "---" } ?: "---",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = OnSurfaceLight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // GARA XE CỦA TÔI & NHẮC HẠN ĐĂNG KIỂM / BẢO HIỂM
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_my_garage_vehicles"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Garage, contentDescription = null, tint = RescueSecondary, modifier = Modifier.size(20.dp))
                            Text("Gara Xe Cá Nhân (${savedVehicles.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        TextButton(
                            onClick = { showAddVehicleDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = RescuePrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Thêm xe", fontSize = 12.sp, color = RescuePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (savedVehicles.isEmpty()) {
                        Text(
                            text = "Chưa có xe phụ trong gara. Nhấn 'Thêm xe' để lưu thêm xe gia đình và tự động nhắc hạn đăng kiểm.",
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight
                        )
                    } else {
                        savedVehicles.forEach { vehicle ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(vehicle.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${vehicle.vehicleType} • ${vehicle.licensePlate}", fontSize = 11.5.sp, color = OnSurfaceVariantLight)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Hạn ĐK: ${vehicle.inspectionExpiry}", fontSize = 10.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                                            Text("Hạn BH: ${vehicle.insuranceExpiry}", fontSize = 10.sp, color = EmergencyGold, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveVehicle?.invoke(vehicle.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = AlertRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 7. TỔNG ĐÀI ĐIỀU PHỐI CỨU HỘ 24/7 (ĐƯỜNG DÂY NÓNG)
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_profile_hotline"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AlertRed,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text("Tổng Đài Điều Phối Cứu Hộ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(AppConfig.RESCUE_HOTLINE_DISPLAY, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AlertRed)
                            Text("Trực tiếp 24/7 toàn quốc", fontSize = 10.sp, color = OnSurfaceVariantLight)
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${AppConfig.DEFAULT_RESCUE_HOTLINE}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GỌI 24/7", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // =================================================================
            // 8. CHUYỂN ĐỔI CHẾ ĐỘ KỸ THUẬT VIÊN CỨU HỘ
            // =================================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_switch_to_ktv_mode"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, EmergencyGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Chế Độ Kỹ Thuật Viên Cứu Hộ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Dành cho thợ sửa xe, tài xế xe kéo nhận đơn", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                        }
                    }

                    Button(
                        onClick = { onSwitchRole?.invoke(AppConfig.UserRole.STAFF) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Vào KTV", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                    }
                }
            }

            // =================================================================
            // 9. MENU TIỆN ÍCH (LỊCH SỬ, ĐÁNH GIÁ, BÁO CÁO)
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text("Lịch sử các lần cứu hộ", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Xem lại các yêu cầu cứu hộ đã thực hiện") },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null, tint = RescuePrimary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Đánh giá của tôi (${myRatings.size})", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Các đánh giá chất lượng dịch vụ của bạn") },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showRatingsSheet = true }
                    )

                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Báo cáo & Phản ánh", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Gửi báo cáo sự cố hoặc góp ý chất lượng") },
                        leadingContent = { Icon(Icons.Default.Feedback, contentDescription = null, tint = RescuePrimary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToReports() }
                    )
                }
            }

            // =================================================================
            // 10. ĐĂNG XUẤT TÀI KHOẢN
            // =================================================================
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ĐĂNG XUẤT TÀI KHOẢN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // =========================================================================
    // DIALOG: CHỈNH SỬA HỒ SƠ CỨU HỘ & THÔNG TIN LIÊN HỆ ĐIỀU PHỐI KHẨN CẤP
    // =========================================================================
    if (showEditProfileDialog && userProfile != null) {
        var name by remember { mutableStateOf(userProfile.displayName) }
        var phone by remember { mutableStateOf(userProfile.phone) }
        var vType by remember { mutableStateOf(userProfile.vehicleType) }
        var vName by remember { mutableStateOf(userProfile.vehicleName) }
        var plate by remember { mutableStateOf(userProfile.licensePlate) }
        var homeAddr by remember { mutableStateOf(userProfile.homeAddress) }

        var iceName by remember { mutableStateOf(userProfile.emergencyContactName) }
        var icePhone by remember { mutableStateOf(userProfile.emergencyContactPhone) }
        var iceRel by remember { mutableStateOf(userProfile.emergencyRelationship.ifBlank { "Vợ/Chồng" }) }
        var blood by remember { mutableStateOf(userProfile.bloodType) }
        var notes by remember { mutableStateOf(userProfile.emergencyNotes) }

        val relationshipOptions = listOf("Vợ/Chồng", "Bố/Mẹ", "Con", "Anh/Chị/Em", "Bạn bè", "Đồng nghiệp")
        val bloodTypeOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = RescuePrimary)
                    Text("Cập Nhật Hồ Sơ Cứu Hộ Khẩn Cấp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Thông tin này được bảo mật an toàn để điều phối xe cứu hộ và kỹ thuật viên nhanh nhất khi bạn gặp sự cố trên đường.",
                        fontSize = 11.5.sp,
                        color = OnSurfaceVariantLight
                    )

                    // 1. TÀI XẾ
                    Text("1. THÔNG TIN TÀI XẾ", fontWeight = FontWeight.Black, fontSize = 12.sp, color = RescuePrimary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên tài xế *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại nhận cuộc gọi cứu hộ *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = homeAddr,
                        onValueChange = { homeAddr = it },
                        label = { Text("Địa chỉ thường trú / Điểm đỗ xe chính") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 2. PHƯƠNG TIỆN
                    Text("2. PHƯƠNG TIỆN MẶC ĐỊNH", fontWeight = FontWeight.Black, fontSize = 12.sp, color = RescuePrimary)
                    OutlinedTextField(
                        value = vName,
                        onValueChange = { vName = it },
                        label = { Text("Tên xe (ví dụ: Toyota Vios, Mazda CX-5)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("Biển số xe (ví dụ: 30K - 888.99) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. NGƯỜI THÂN KHẨN CẤP (ICE)
                    Text("3. LIÊN HỆ NGƯỜI THÂN KHẨN CẤP (ICE)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = AlertRed)
                    OutlinedTextField(
                        value = iceName,
                        onValueChange = { iceName = it },
                        label = { Text("Họ tên người thân (ICE)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = icePhone,
                        onValueChange = { icePhone = it },
                        label = { Text("Số điện thoại người thân khi khẩn cấp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Mối quan hệ:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        relationshipOptions.take(4).forEach { rel ->
                            FilterChip(
                                selected = iceRel == rel,
                                onClick = { iceRel = rel },
                                label = { Text(rel, fontSize = 10.5.sp) }
                            )
                        }
                    }

                    // 4. Y TẾ & GHI CHÚ
                    Text("4. THẺ Y TẾ & AN TOÀN", fontWeight = FontWeight.Black, fontSize = 12.sp, color = SafeGreen)
                    Text("Nhóm máu:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bloodTypeOptions.take(4).forEach { b ->
                            FilterChip(
                                selected = blood == b,
                                onClick = { blood = if (blood == b) "" else b },
                                label = { Text(b, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Ghi chú y tế / Xe đặc biệt (Dị ứng, móc kéo...)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditProfileDialog = false
                        if (onUpdateEmergencyProfile != null) {
                            onUpdateEmergencyProfile(
                                name, phone, vType, vName, plate,
                                iceName, icePhone, iceRel, blood, notes, homeAddr
                            )
                        } else {
                            onUpdateProfile(name, phone, vType, vName, plate, null)
                        }
                        Toast.makeText(context, "Đã lưu hồ sơ cứu hộ khẩn cấp thành công!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Text("LƯU THÔNG TIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // =========================================================================
    // DIALOG: THÊM XE VÀO GARA
    // =========================================================================
    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Thêm Xe Vào Gara Cá Nhân") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newVehicleName,
                        onValueChange = { newVehicleName = it },
                        label = { Text("Tên xe (ví dụ: Mazda CX-5)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehiclePlate,
                        onValueChange = { newVehiclePlate = it },
                        label = { Text("Biển số xe") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehicleInspection,
                        onValueChange = { newVehicleInspection = it },
                        label = { Text("Hạn đăng kiểm (DD/MM/YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVehicleInsurance,
                        onValueChange = { newVehicleInsurance = it },
                        label = { Text("Hạn bảo hiểm TNDS (DD/MM/YYYY)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVehicleName.isNotBlank() && newVehiclePlate.isNotBlank()) {
                            onAddVehicle?.invoke(newVehicleName, newVehicleType, newVehiclePlate, newVehicleInspection, newVehicleInsurance)
                            showAddVehicleDialog = false
                            newVehicleName = ""
                            newVehiclePlate = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Text("Thêm Xe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) { Text("Hủy") }
            }
        )
    }

    // =========================================================================
    // DIALOG: XÁC NHẬN ĐĂNG XUẤT
    // =========================================================================
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Đăng Xuất Tài Khoản?") },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng Cứu Hộ Xe 24/7?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Đăng Xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) { Text("Hủy") }
            }
        )
    }

    // =========================================================================
    // DIALOG: ĐÁNH GIÁ CỦA TÔI
    // =========================================================================
    if (showRatingsSheet) {
        AlertDialog(
            onDismissRequest = { showRatingsSheet = false },
            title = { Text("Đánh Giá Của Bạn (${myRatings.size})") },
            text = {
                if (myRatings.isEmpty()) {
                    Text("Bạn chưa gửi đánh giá nào.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        myRatings.take(5).forEach { r ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(r.rating) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    if (r.comment.isNotBlank()) {
                                        Text("\"${r.comment}\"", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRatingsSheet = false }) { Text("Đóng") }
            }
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, fontSize = 11.sp, color = OnSurfaceVariantLight)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
        }
    }
}

@Composable
private fun ChecklistChip(
    title: String,
    isChecked: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked) SafeGreen.copy(alpha = 0.12f) else AlertRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isChecked) SafeGreen else AlertRed,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) SafeGreen else AlertRed
            )
        }
    }
}
