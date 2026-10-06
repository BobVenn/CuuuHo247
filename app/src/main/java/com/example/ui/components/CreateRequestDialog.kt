package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.config.AppConfig
import com.example.data.model.SavedVehicle
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import com.example.util.UserLocationInfo
import java.util.Locale

/**
 * Form yêu cầu cứu hộ giao thông:
 * Cho phép người dùng chọn:
 * 1. Loại phương tiện (Ô tô / Xe máy - Car / Bike)
 * 2. Loại sự cố gặp phải (Thủng lốp - Flat tire, Hết bình ắc quy - Dead battery, Hết xăng, Hỏng máy, Khóa xe...)
 * 3. Vị trí GPS hiện tại (Kinh độ, Vĩ độ, Địa chỉ và nút Làm mới GPS)
 * và gửi trực tiếp dữ liệu lên Firebase Realtime Database.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestDialog(
    initialIssueType: String? = null,
    initialVehicleCategory: String? = null,
    initialDescription: String? = null,
    userProfile: UserProfile,
    currentLocation: UserLocationInfo,
    savedVehicles: List<SavedVehicle> = emptyList(),
    onDismiss: () -> Unit,
    onRefreshLocation: (() -> Unit)? = null,
    onSubmit: (
        issueType: String,
        description: String,
        vehicleType: String,
        licensePlate: String,
        imageUrl: String?
    ) -> Unit
) {
    // 1. Vehicle Type: Car or Bike (Primary choices)
    var selectedVehicleCategory by remember {
        mutableStateOf(
            initialVehicleCategory ?: if (userProfile.vehicleType.contains("xe máy", ignoreCase = true) ||
                userProfile.vehicleType.contains("moped", ignoreCase = true) ||
                userProfile.vehicleType.contains("bike", ignoreCase = true)
            ) "BIKE" else "CAR"
        )
    }

    var customVehicleDetail by remember {
        mutableStateOf(
            if (userProfile.vehicleType.isNotBlank()) userProfile.vehicleType
            else if (selectedVehicleCategory == "BIKE") "Xe máy (Bike)"
            else "Ô tô con (4-7 chỗ)"
        )
    }

    // 2. Issue Type: Flat tire, Dead battery, Out of gas, etc.
    var selectedIssue by remember {
        mutableStateOf(initialIssueType ?: "Thủng lốp")
    }

    var licensePlate by remember {
        mutableStateOf(userProfile.licensePlate.ifBlank { if (selectedVehicleCategory == "CAR") "30H-888.68" else "29B1-123.45" })
    }

    var description by remember { mutableStateOf(initialDescription ?: "") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPresetPhotoUrl by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            selectedPresetPhotoUrl = null
        }
    }

    val finalPhoto = selectedPhotoUri?.toString() ?: selectedPresetPhotoUrl

    val finalVehicleType = when (selectedVehicleCategory) {
        "CAR" -> if (customVehicleDetail.contains("Ô tô", ignoreCase = true)) customVehicleDetail else "Ô tô (Car)"
        "BIKE" -> "Xe máy (Bike)"
        else -> customVehicleDetail
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("create_request_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                            color = RescuePrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Sos,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Gửi Yêu Cầu Cứu Hộ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Kết nối cứu hộ 24/7 • Điều phối kỹ thuật viên gần nhất",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider()

                // SECTION 1: GPS LOCATION (VỊ TRÍ GPS HIỆN TẠI)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Vị trí cứu hộ (GPS hiện tại):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (onRefreshLocation != null) {
                            TextButton(
                                onClick = onRefreshLocation,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = RescuePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Làm mới GPS", fontSize = 11.5.sp, color = RescuePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(SafeGreen, CircleShape)
                                        )
                                        Text(
                                            text = "Tọa độ GPS Trực tiếp",
                                            color = SafeGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "%.5f, %.5f", currentLocation.latitude, currentLocation.longitude),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = currentLocation.address.ifBlank { "Đang lấy địa chỉ thực tế từ cảm biến GPS thiết bị..." },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // SECTION 2: VEHICLE TYPE SELECTION (CAR / BIKE)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedVehicles.isNotEmpty()) {
                        Text(
                            text = "Xe đã lưu của bạn (Bấm để chọn nhanh):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RescuePrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savedVehicles.take(3).forEach { v ->
                                val isCar = !v.vehicleType.contains("máy", ignoreCase = true) && !v.vehicleType.contains("bike", ignoreCase = true)
                                val isSelected = licensePlate == v.licensePlate
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedVehicleCategory = if (isCar) "CAR" else "BIKE"
                                            customVehicleDetail = "${v.name.ifBlank { v.vehicleType }} (${v.vehicleType})"
                                            licensePlate = v.licensePlate
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) RescuePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) RescuePrimary else BorderLight)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = v.name.ifBlank { if (isCar) "Ô tô" else "Xe máy" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = v.licensePlate,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) RescuePrimary else OnSurfaceVariantLight
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "1. Chọn loại phương tiện (Vehicle Type):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Car (Ô tô)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedVehicleCategory = "CAR"
                                    customVehicleDetail = "Ô tô con (4-7 chỗ)"
                                    if (licensePlate.startsWith("29B")) licensePlate = "30H-888.68"
                                }
                                .testTag("select_vehicle_car"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedVehicleCategory == "CAR") RescuePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (selectedVehicleCategory == "CAR") 2.dp else 1.dp,
                                color = if (selectedVehicleCategory == "CAR") RescuePrimary else BorderLight
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = "Car",
                                    tint = if (selectedVehicleCategory == "CAR") RescuePrimary else OnSurfaceVariantLight,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Ô tô (Car)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicleCategory == "CAR") RescuePrimary else OnSurfaceLight
                                )
                                Text(
                                    text = "4-7 chỗ, Sedan, SUV",
                                    fontSize = 10.sp,
                                    color = OnSurfaceVariantLight
                                )
                            }
                        }

                        // Option 2: Bike (Xe máy)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedVehicleCategory = "BIKE"
                                    customVehicleDetail = "Xe máy (Bike)"
                                    if (licensePlate.startsWith("30H")) licensePlate = "29B1-123.45"
                                }
                                .testTag("select_vehicle_bike"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedVehicleCategory == "BIKE") RescuePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (selectedVehicleCategory == "BIKE") 2.dp else 1.dp,
                                color = if (selectedVehicleCategory == "BIKE") RescuePrimary else BorderLight
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.TwoWheeler,
                                    contentDescription = "Bike",
                                    tint = if (selectedVehicleCategory == "BIKE") RescuePrimary else OnSurfaceVariantLight,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Xe máy (Bike)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedVehicleCategory == "BIKE") RescuePrimary else OnSurfaceLight
                                )
                                Text(
                                    text = "Xe ga, số, tay côn, PKL",
                                    fontSize = 10.sp,
                                    color = OnSurfaceVariantLight
                                )
                            }
                        }
                    }
                }

                // SECTION 3: ISSUE ENCOUNTERED (FLAT TIRE, DEAD BATTERY, ETC.)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. Sự cố đang gặp phải (Issue Encountered):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val issuesList = listOf(
                        Triple("Thủng lốp", "Flat tire", Icons.Default.Settings),
                        Triple("Hết bình", "Dead battery", Icons.Default.Bolt),
                        Triple("Xe hết xăng", "Out of fuel", Icons.Default.LocalGasStation),
                        Triple("Hỏng xe", "Breakdown", Icons.Default.Build),
                        Triple("Khóa xe", "Lockout", Icons.Default.Key),
                        Triple("Tai nạn", "Accident", Icons.Default.Warning)
                    )

                    issuesList.chunked(2).forEach { rowIssues ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowIssues.forEach { (issueVi, issueEn, icon) ->
                                val isSelected = selectedIssue == issueVi
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedIssue = issueVi }
                                        .testTag("issue_chip_${issueVi}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) RescuePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) RescuePrimary else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) RescuePrimary else OnSurfaceVariantLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = issueVi,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RescuePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = issueEn,
                                                fontSize = 9.5.sp,
                                                color = OnSurfaceVariantLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: LICENSE PLATE & DETAILS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = { Text("Biển số xe") },
                        leadingIcon = {
                            Icon(
                                if (selectedVehicleCategory == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_request_license_plate"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Ghi chú thêm (Tùy chọn)") },
                        placeholder = { Text("Gần ngã tư...") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_request_description"),
                        singleLine = true
                    )
                }

                // Optional Photo attachment
                if (finalPhoto != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, RescuePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Box {
                            AsyncImage(
                                model = finalPhoto,
                                contentDescription = "Ảnh sự cố",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    selectedPhotoUri = null
                                    selectedPresetPhotoUrl = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ảnh hiện trường sự cố:",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariantLight
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp).testTag("btn_pick_incident_photo")
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chụp / Chọn ảnh", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // SUBMIT BUTTON (XÁC NHẬN ĐẶT CỨU HỘ)
                Button(
                    onClick = {
                        isSubmitting = true
                        onSubmit(
                            selectedIssue,
                            description.ifBlank { "Yêu cầu cứu hộ: $selectedIssue cho $finalVehicleType" },
                            finalVehicleType,
                            licensePlate.ifBlank { userProfile.licensePlate.ifBlank { "Chưa rõ biển số" } },
                            finalPhoto
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_submit_request"),
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ĐANG ĐẶT CỨU HỘ...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "XÁC NHẬN ĐẶT CỨU HỘ NGAY",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
