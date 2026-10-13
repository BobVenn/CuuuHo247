package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedVehicle
import com.example.data.model.UserProfile
import com.example.ui.theme.*

/**
 * Reusable User Profile Component that manages:
 * 1. Contact Information (Driver Name, Primary Phone, ICE Emergency Relative, Home Address)
 * 2. Vehicle Details (Vehicle Type, Make/Model, License Plate, Saved Garage vehicles)
 * 3. Emergency Dispatch Readiness (Readiness score 0-100%, Medical/Blood Type, Rescue notes)
 *
 * Designed to accelerate emergency dispatch (< 15s) by pre-populating critical telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileEmergencyManagerComponent(
    userProfile: UserProfile?,
    savedVehicles: List<SavedVehicle> = emptyList(),
    onUpdateEmergencyProfile: (
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
    ) -> Unit,
    onAddSavedVehicle: ((name: String, vType: String, plate: String, inspection: String, insurance: String) -> Unit)? = null,
    onRemoveSavedVehicle: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isInitiallyExpanded: Boolean = false
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    // Tính toán điểm số sẵn sàng điều phối cứu hộ (Dispatch Readiness Score)
    val hasDriverName = !userProfile?.displayName.isNullOrBlank()
    val hasDriverPhone = !userProfile?.phone.isNullOrBlank() && userProfile?.phone != "0987654321"
    val hasVehicle = !userProfile?.licensePlate.isNullOrBlank()
    val hasEmergencyContact = !userProfile?.emergencyContactPhone.isNullOrBlank()
    val hasMedicalOrNotes = !userProfile?.bloodType.isNullOrBlank() || !userProfile?.emergencyNotes.isNullOrBlank()

    var readinessScore = 0
    if (hasDriverName) readinessScore += 15
    if (hasDriverPhone) readinessScore += 30
    if (hasVehicle) readinessScore += 25
    if (hasEmergencyContact) readinessScore += 20
    if (hasMedicalOrNotes) readinessScore += 10
    if (readinessScore > 100) readinessScore = 100

    val isFullyReady = readinessScore >= 85
    val animatedProgress by animateFloatAsState(
        targetValue = readinessScore / 100f,
        label = "readiness_score_anim"
    )

    fun dialPhone(phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(context, "Chưa thiết lập số điện thoại", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_profile_emergency_manager_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, if (isFullyReady) SafeGreen.copy(alpha = 0.5f) else BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP HEADER: Title, Readiness Badge & Quick Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFullyReady) SafeGreen.copy(alpha = 0.15f) else AlertRedLight,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFullyReady) Icons.Filled.VerifiedUser else Icons.Filled.Emergency,
                                contentDescription = null,
                                tint = if (isFullyReady) SafeGreen else AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "HỒ SƠ CỨU HỘ & PHƯƠNG TIỆN",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = OnSurfaceLight
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isFullyReady) SafeGreen else AlertRed
                            ) {
                                Text(
                                    text = "$readinessScore%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isFullyReady) "Đã sẵn sàng điều phối tức thì (< 15s)" else "Cần bổ sung thông tin để hỗ trợ nhanh nhất",
                            fontSize = 11.sp,
                            color = if (isFullyReady) SafeGreen else AlertRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Edit Button
                FilledTonalButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = RescuePrimary.copy(alpha = 0.12f),
                        contentColor = RescuePrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_edit_emergency_profile")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cập nhật", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. READINESS PROGRESS BAR & TELEMETRY CHECKLIST
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isFullyReady) SafeGreen else AlertRed,
                    trackColor = Color(0xFFF1F5F9)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniChecklistBadge(title = "SĐT Tài xế", isChecked = hasDriverPhone)
                    MiniChecklistBadge(title = "Biển số xe", isChecked = hasVehicle)
                    MiniChecklistBadge(title = "ICE Khẩn cấp", isChecked = hasEmergencyContact)
                    MiniChecklistBadge(title = "Y tế/Ghi chú", isChecked = hasMedicalOrNotes)
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 3. CONTACT INFORMATION SECTION (Primary Driver & ICE Emergency Relative)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "THÔNG TIN LIÊN HỆ ĐIỀU PHỐI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariantLight
                    )
                    Text(
                        text = "Tổng đài & KTV gọi tới",
                        fontSize = 10.5.sp,
                        color = RescuePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Driver Primary Contact
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RescuePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = RescuePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = userProfile?.displayName?.ifBlank { "Chưa cập nhật tên tài xế" } ?: "Khách hàng",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userProfile?.phone?.ifBlank { "Chưa có SĐT chính" } ?: "Chưa có SĐT",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariantLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!userProfile?.phone.isNullOrBlank()) {
                            IconButton(
                                onClick = { dialPhone(userProfile?.phone ?: "") },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Gọi tài xế", tint = RescuePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // In Case of Emergency (ICE) Relative Contact
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEA580C),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Emergency,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Người thân khẩn cấp (ICE)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412)
                                    )
                                    if (!userProfile?.emergencyRelationship.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFDBA74)
                                        ) {
                                            Text(
                                                text = userProfile!!.emergencyRelationship,
                                                color = Color(0xFF7C2D12),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                val iceName = userProfile?.emergencyContactName?.ifBlank { "Chưa cập nhật tên" } ?: "Chưa cập nhật"
                                val icePhone = userProfile?.emergencyContactPhone?.ifBlank { "Chưa có SĐT" } ?: "Chưa có SĐT"
                                Text(
                                    text = "$iceName: $icePhone",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF431407)
                                )
                            }
                        }

                        if (!userProfile?.emergencyContactPhone.isNullOrBlank()) {
                            Button(
                                onClick = { dialPhone(userProfile?.emergencyContactPhone ?: "") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gọi ICE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // 4. VEHICLE DETAILS SECTION (Active Vehicle & Quick Garage Switcher)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PHƯƠNG TIỆN CỨU HỘ HIỆN TẠI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariantLight
                    )
                    Text(
                        text = "Biển số gửi tới đội cứu hộ",
                        fontSize = 10.5.sp,
                        color = RescuePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Active Vehicle Display
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RescuePrimary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when {
                                            userProfile?.vehicleType?.contains("xe máy", ignoreCase = true) == true -> Icons.Filled.TwoWheeler
                                            userProfile?.vehicleType?.contains("tải", ignoreCase = true) == true -> Icons.Filled.LocalShipping
                                            else -> Icons.Filled.DirectionsCar
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = userProfile?.vehicleName?.ifBlank { userProfile?.vehicleType } ?: "Chưa đặt tên xe",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${userProfile?.vehicleType ?: "Ô tô"} • ${userProfile?.licensePlate?.ifBlank { "Chưa có biển số" } ?: "Chưa có biển số"}",
                                    fontSize = 11.5.sp,
                                    color = OnSurfaceVariantLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // License plate badge
                        val plate = userProfile?.licensePlate?.ifBlank { "---" } ?: "---"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, Color.Black)
                        ) {
                            Text(
                                text = plate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Garage quick selector chips if saved vehicles exist
                if (savedVehicles.isNotEmpty()) {
                    Text(
                        text = "Chọn nhanh từ Gara cá nhân:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariantLight
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedVehicles) { veh ->
                            val isSelected = veh.licensePlate == userProfile?.licensePlate
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onUpdateEmergencyProfile(
                                        userProfile?.displayName ?: "",
                                        userProfile?.phone ?: "",
                                        veh.vehicleType,
                                        veh.name,
                                        veh.licensePlate,
                                        userProfile?.emergencyContactName ?: "",
                                        userProfile?.emergencyContactPhone ?: "",
                                        userProfile?.emergencyRelationship ?: "Người thân",
                                        userProfile?.bloodType ?: "",
                                        userProfile?.emergencyNotes ?: "",
                                        userProfile?.homeAddress ?: ""
                                    )
                                    Toast.makeText(context, "Đã đổi xe cứu hộ chính sang: ${veh.name} (${veh.licensePlate})", Toast.LENGTH_SHORT).show()
                                },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(veh.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(veh.licensePlate, fontSize = 9.5.sp)
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RescuePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = RescuePrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) RescuePrimary else BorderLight
                                ),
                                modifier = Modifier.testTag("garage_chip_${veh.id}")
                            )
                        }

                        if (onAddSavedVehicle != null) {
                            item {
                                OutlinedButton(
                                    onClick = { showAddVehicleDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    border = BorderStroke(1.dp, RescuePrimary),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thêm xe", fontSize = 11.sp, color = RescuePrimary)
                                }
                            }
                        }
                    }
                }
            }

            // 5. MEDICAL & RESCUE SPECIAL NOTES (Expandable)
            AnimatedVisibility(visible = isExpanded || !userProfile?.bloodType.isNullOrBlank() || !userProfile?.emergencyNotes.isNullOrBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Y TẾ & LƯU Ý KỸ THUẬT CỨU HỘ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariantLight
                        )
                        if (!userProfile?.bloodType.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "Nhóm máu: ${userProfile!!.bloodType}",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!userProfile?.emergencyNotes.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                Text(
                                    text = userProfile!!.emergencyNotes,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF7F1D1D),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    if (!userProfile?.homeAddress.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = OnSurfaceVariantLight, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Địa chỉ thường trú: ${userProfile!!.homeAddress}",
                                fontSize = 11.sp,
                                color = OnSurfaceVariantLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Expand / Collapse details trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Thu gọn chi tiết" else "Xem thêm ghi chú y tế & địa chỉ",
                    fontSize = 11.sp,
                    color = RescuePrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = RescuePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // DIALOG: EDIT EMERGENCY PROFILE
    if (showEditDialog) {
        EditEmergencyProfileDialog(
            userProfile = userProfile,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, vType, vName, plate, iceName, icePhone, iceRel, blood, notes, addr ->
                onUpdateEmergencyProfile(name, phone, vType, vName, plate, iceName, icePhone, iceRel, blood, notes, addr)
                showEditDialog = false
                Toast.makeText(context, "Đã cập nhật hồ sơ cứu hộ thành công", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // DIALOG: ADD VEHICLE TO GARAGE
    if (showAddVehicleDialog && onAddSavedVehicle != null) {
        AddGarageVehicleDialog(
            onDismiss = { showAddVehicleDialog = false },
            onAdd = { name, vType, plate, insp, insu ->
                onAddSavedVehicle(name, vType, plate, insp, insu)
                showAddVehicleDialog = false
            }
        )
    }
}

@Composable
private fun MiniChecklistBadge(title: String, isChecked: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isChecked) SafeGreen.copy(alpha = 0.12f) else Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isChecked) SafeGreen else Color(0xFF94A3B8),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                color = if (isChecked) SafeGreen else Color(0xFF64748B)
            )
        }
    }
}

/**
 * Interactive dialog allowing users to manage their contact and vehicle details across 3 organized tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEmergencyProfileDialog(
    userProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Tab 1: Driver & ICE Contacts
    var nameInput by remember { mutableStateOf(userProfile?.displayName ?: "") }
    var phoneInput by remember { mutableStateOf(userProfile?.phone ?: "") }
    var iceNameInput by remember { mutableStateOf(userProfile?.emergencyContactName ?: "") }
    var icePhoneInput by remember { mutableStateOf(userProfile?.emergencyContactPhone ?: "") }
    var iceRelationshipInput by remember { mutableStateOf(userProfile?.emergencyRelationship ?: "Người thân") }
    var homeAddressInput by remember { mutableStateOf(userProfile?.homeAddress ?: "") }

    // Tab 2: Vehicle details
    var vehicleTypeInput by remember { mutableStateOf(userProfile?.vehicleType ?: "Ô tô 4-7 chỗ") }
    var vehicleNameInput by remember { mutableStateOf(userProfile?.vehicleName ?: "") }
    var licensePlateInput by remember { mutableStateOf(userProfile?.licensePlate ?: "") }

    // Tab 3: Medical & Special Notes
    var bloodTypeInput by remember { mutableStateOf(userProfile?.bloodType ?: "") }
    var notesInput by remember { mutableStateOf(userProfile?.emergencyNotes ?: "") }

    val vehicleTypes = listOf(
        "Ô tô 4-7 chỗ",
        "Bán tải / SUV 7 chỗ",
        "Xe điện VinFast",
        "Xe tải nhẹ / Van",
        "Xe máy tay ga / Xe số"
    )

    val relationships = listOf("Người thân", "Vợ/Chồng", "Bố/Mẹ", "Con cái", "Anh/Em", "Đồng nghiệp", "Bạn bè")
    val bloodTypes = listOf("O+", "A+", "B+", "AB+", "O-", "A-", "Chưa rõ")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = RescuePrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text("Cập Nhật Hồ Sơ Cứu Hộ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Tối ưu tốc độ tiếp cận cứu hộ < 15 giây", fontSize = 10.5.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF8FAFC)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("1. Liên hệ & ICE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("2. Phương tiện", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("3. Y tế & Lưu ý", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // TAB 1: LIÊN HỆ TÀI XẾ & NGƯỜI THÂN ICE
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Họ và tên tài xế") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_driver_name")
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Số điện thoại tài xế (Gọi chính)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_driver_phone")
                        )

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        Text("Người thân khẩn cấp (In Case of Emergency - ICE):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))

                        OutlinedTextField(
                            value = iceNameInput,
                            onValueChange = { iceNameInput = it },
                            label = { Text("Họ tên người thân ICE") },
                            leadingIcon = { Icon(Icons.Default.ContactEmergency, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_ice_name")
                        )

                        OutlinedTextField(
                            value = icePhoneInput,
                            onValueChange = { icePhoneInput = it },
                            label = { Text("Số điện thoại người thân ICE") },
                            leadingIcon = { Icon(Icons.Default.PhoneInTalk, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_ice_phone")
                        )

                        Text("Mối quan hệ:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(relationships) { rel ->
                                FilterChip(
                                    selected = iceRelationshipInput == rel,
                                    onClick = { iceRelationshipInput = rel },
                                    label = { Text(rel, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = homeAddressInput,
                            onValueChange = { homeAddressInput = it },
                            label = { Text("Địa chỉ thường trú / Nhà riêng (Tùy chọn)") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    1 -> {
                        // TAB 2: PHƯƠNG TIỆN & BIỂN SỐ XE
                        Text("Loại phương tiện ưu tiên:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            vehicleTypes.forEach { type ->
                                val isSelected = vehicleTypeInput == type
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) RescuePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) RescuePrimary else BorderLight),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vehicleTypeInput = type }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(type, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = vehicleNameInput,
                            onValueChange = { vehicleNameInput = it },
                            label = { Text("Hãng xe / Dòng xe (Ví dụ: VinFast VF8, CX-5)") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_vehicle_name")
                        )

                        OutlinedTextField(
                            value = licensePlateInput,
                            onValueChange = { licensePlateInput = it.uppercase() },
                            label = { Text("Biển số xe (Ví dụ: 30K-999.88)") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_license_plate")
                        )
                    }

                    2 -> {
                        // TAB 3: Y TẾ & GHI CHÚ ĐIỀU PHỐI CỨU HỘ
                        Text("Nhóm máu tài xế (Phục vụ cấp cứu khẩn cấp):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(bloodTypes) { bType ->
                                FilterChip(
                                    selected = bloodTypeInput == bType,
                                    onClick = { bloodTypeInput = bType },
                                    label = { Text(bType, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AlertRedLight,
                                        selectedLabelColor = AlertRed
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Ghi chú cứu hộ đặc biệt") },
                            placeholder = { Text("Ví dụ: Xe số tự động 2 cầu AWD cần xe sàn trượt, không cẩu kéo trục; dị ứng kháng sinh...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth().testTag("input_emergency_notes")
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Thông tin này được chuyển thẳng tới đội xe cứu hộ và kỹ thuật viên ngay khi nhận đơn để chuẩn bị công cụ chuyên dụng phù hợp.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        nameInput.trim(),
                        phoneInput.trim(),
                        vehicleTypeInput.trim(),
                        vehicleNameInput.trim(),
                        licensePlateInput.trim(),
                        iceNameInput.trim(),
                        icePhoneInput.trim(),
                        iceRelationshipInput.trim(),
                        bloodTypeInput.trim(),
                        notesInput.trim(),
                        homeAddressInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_emergency_profile")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LƯU & ĐỒNG BỘ CLOUD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = OnSurfaceVariantLight)
            }
        }
    )
}

/**
 * Dialog to add a new vehicle to the personal garage.
 */
@Composable
fun AddGarageVehicleDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, vType: String, plate: String, inspection: String, insurance: String) -> Unit
) {
    var vName by remember { mutableStateOf("") }
    var vType by remember { mutableStateOf("Ô tô 4-7 chỗ") }
    var vPlate by remember { mutableStateOf("") }
    var vInspection by remember { mutableStateOf("15/12/2026") }
    var vInsurance by remember { mutableStateOf("28/11/2026") }

    val vehicleTypeOptions = listOf("Ô tô 4-7 chỗ", "Bán tải / SUV", "Xe máy", "Xe tải nhẹ", "Xe điện VinFast")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary)
                Text("Thêm Xe Vào Gara", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = vName,
                    onValueChange = { vName = it },
                    label = { Text("Tên xe / Dòng xe (VD: Mazda CX-5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vPlate,
                    onValueChange = { vPlate = it.uppercase() },
                    label = { Text("Biển số xe (VD: 30K-123.45)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Loại xe:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(vehicleTypeOptions) { opt ->
                        FilterChip(
                            selected = vType == opt,
                            onClick = { vType = opt },
                            label = { Text(opt, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = vInspection,
                    onValueChange = { vInspection = it },
                    label = { Text("Hạn đăng kiểm (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = vInsurance,
                    onValueChange = { vInsurance = it },
                    label = { Text("Hạn bảo hiểm TNDS (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (vName.isNotBlank() && vPlate.isNotBlank()) {
                        onAdd(vName.trim(), vType, vPlate.trim(), vInspection.trim(), vInsurance.trim())
                    }
                },
                enabled = vName.isNotBlank() && vPlate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Thêm vào Gara")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
