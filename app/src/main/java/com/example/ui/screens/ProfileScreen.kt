package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RatingItem
import com.example.data.model.SavedVehicle
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    myRatings: List<RatingItem> = emptyList(),
    savedVehicles: List<SavedVehicle> = emptyList(),
    onAddVehicle: ((name: String, vType: String, plate: String, inspection: String, insurance: String) -> Unit)? = null,
    onRemoveVehicle: ((String) -> Unit)? = null,
    onUpdateProfile: (name: String, phone: String, vehicleType: String, vehicleName: String, licensePlate: String, role: String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showRatingsSheet by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var newVehicleName by remember { mutableStateOf("") }
    var newVehicleType by remember { mutableStateOf("Ô tô 4-7 chỗ") }
    var newVehiclePlate by remember { mutableStateOf("") }
    var newVehicleInspection by remember { mutableStateOf("15/12/2026") }
    var newVehicleInsurance by remember { mutableStateOf("28/11/2026") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tài Khoản & Hồ Sơ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
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
                .padding(bottom = 95.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. TỔNG ĐÀI ĐIỀU PHỐI CỨU HỘ 24/7 (ĐƯỜNG DÂY NÓNG KHẨN CẤP)
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
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Tổng Đài Cứu Hộ 24/7",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = OnSurfaceLight
                                )
                                Surface(shape = RoundedCornerShape(4.dp), color = AlertRed) {
                                    Text("TRỰC 24/7", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text(
                                text = AppConfig.RESCUE_HOTLINE_DISPLAY,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = AlertRed
                            )
                            Text(
                                text = "Điều phối xe cứu hộ khẩn cấp toàn quốc",
                                fontSize = 10.5.sp,
                                color = OnSurfaceVariantLight
                            )
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_call_hotline_from_profile")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GỌI NGAY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 1. User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RescuePrimary,
                                modifier = Modifier.size(56.dp)
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
                            Column {
                                Text(
                                    text = userProfile?.displayName?.ifBlank { "Người dùng" } ?: "Khách hàng",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SafeGreen
                                ) {
                                    Text(
                                        text = "Khách Hàng (Tài Xế)",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa thông tin", tint = RescuePrimary)
                        }
                    }

                    HorizontalDivider()

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                        Text("Số điện thoại: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(userProfile?.phone?.ifBlank { "Chưa cập nhật" } ?: "Chưa cập nhật", fontSize = 13.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                        Text("Phương tiện: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "${userProfile?.vehicleType} • ${userProfile?.vehicleName?.ifBlank { "Chưa có tên xe" }}",
                            fontSize = 13.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Numbers, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                        Text("Biển số xe: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(userProfile?.licensePlate?.ifBlank { "Chưa cập nhật" } ?: "Chưa cập nhật", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // GARA XE CỦA TÔI & NHẮC HẠN ĐĂNG KIỂM / BẢO HIỂM TNDS
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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = CategoryBlueBg,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = CategoryBlueTint, modifier = Modifier.size(18.dp))
                                }
                            }
                            Column {
                                Text("Gara Xe Của Tôi", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text("Nhắc hạn đăng kiểm & bảo hiểm TNDS", fontSize = 11.sp, color = OnSurfaceVariantLight)
                            }
                        }

                        Button(
                            onClick = {
                                newVehicleName = ""
                                newVehiclePlate = ""
                                showAddVehicleDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            modifier = Modifier.height(32.dp).testTag("btn_add_vehicle_to_garage")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm xe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (savedVehicles.isEmpty()) {
                        Text(
                            text = "Chưa có xe nào trong gara. Hãy thêm xe để gọi cứu hộ 1 chạm và nhận thông báo nhắc hạn đăng kiểm.",
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight
                        )
                    } else {
                        savedVehicles.forEach { veh ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(veh.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = RescuePrimary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = veh.licensePlate,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = RescuePrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text("Loại: ${veh.vehicleType}", fontSize = 11.sp, color = OnSurfaceVariantLight)

                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Event, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(13.dp))
                                                Text("Đăng kiểm: ${veh.inspectionExpiry}", fontSize = 10.5.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(13.dp))
                                                Text("Bảo hiểm: ${veh.insuranceExpiry}", fontSize = 10.5.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onRemoveVehicle?.invoke(veh.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa xe", tint = AlertRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. CHÍNH SÁCH CAM KẾT DỊCH VỤ CỨU HỘ KHẨN CẤP
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_service_commitment"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                        Text("Cam Kết Chất Lượng Cứu Hộ 24/7", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CategoryGreenBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ứng Trực Cấp Tốc 15 - 30 Phút", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Đội ngũ kỹ thuật viên được điều phối qua GPS tới ngay vị trí xe bạn", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CategoryBlueBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Minh Bạch Giá & Hóa Đơn Bảo Hiểm", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Báo giá trước khi thực hiện, xuất biên lai mộc đỏ yêu cầu bồi thường bảo hiểm", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CategoryOrangeBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = RescueSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Thanh Toán VietQR Chuẩn Napas", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Quét mã thanh toán an toàn chuyển khoản đúng số tiền thực tế", fontSize = 10.5.sp, color = OnSurfaceVariantLight)
                        }
                    }
                }
            }

            // 4. Quick Menu Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // History
                    ListItem(
                        headlineContent = { Text("Lịch sử các lần cứu hộ", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Xem lại các yêu cầu cứu hộ đã thực hiện") },
                        leadingContent = { Icon(Icons.Default.History, contentDescription = null, tint = RescuePrimary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )

                    HorizontalDivider()

                    // Ratings
                    ListItem(
                        headlineContent = { Text("Đánh giá của tôi (${myRatings.size})", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Các đánh giá chất lượng đã gửi lên Firebase") },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showRatingsSheet = true }
                    )

                    HorizontalDivider()

                    // Reports
                    ListItem(
                        headlineContent = { Text("Báo cáo & Phản ánh", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Gửi báo cáo sự cố hoặc góp ý chất lượng") },
                        leadingContent = { Icon(Icons.Default.Feedback, contentDescription = null, tint = RescuePrimary) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { onNavigateToReports() }
                    )
                }
            }

            // 6. Logout Button
            Button(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_logout"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ĐĂNG XUẤT", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog && userProfile != null) {
        var name by remember { mutableStateOf(userProfile.displayName) }
        var phone by remember { mutableStateOf(userProfile.phone) }
        var vType by remember { mutableStateOf(userProfile.vehicleType) }
        var vName by remember { mutableStateOf(userProfile.vehicleName) }
        var plate by remember { mutableStateOf(userProfile.licensePlate) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Chỉnh Sửa Thông Tin Cá Nhân") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = vName, onValueChange = { vName = it }, label = { Text("Tên xe") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Biển số xe") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditProfileDialog = false
                        onUpdateProfile(name, phone, vType, vName, plate, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Text("Lưu Thay Đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Logout Confirmation Dialog
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

    // My Ratings Sheet / Dialog
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
