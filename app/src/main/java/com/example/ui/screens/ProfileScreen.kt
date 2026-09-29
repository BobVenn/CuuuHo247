package com.example.ui.screens

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
import com.example.data.model.UserProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.EmergencyGold
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    myRatings: List<RatingItem>,
    onUpdateProfile: (name: String, phone: String, vehicleType: String, vehicleName: String, licensePlate: String, role: String?) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showRatingsSheet by remember { mutableStateOf(false) }

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
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                    color = when (userProfile?.role) {
                                        AppConfig.UserRole.ADMIN -> AlertRed
                                        AppConfig.UserRole.STAFF -> RescuePrimary
                                        else -> SafeGreen
                                    }
                                ) {
                                    Text(
                                        text = userProfile?.role ?: AppConfig.UserRole.USER,
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

            // Quick Menu Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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

            // Role Switcher (For testing User / Staff / Admin features)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chuyển Đổi Quyền Hạn (Kiểm Thử)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Cho phép bạn thử nghiệm giao diện của User, Rescue Staff hoặc Admin:", style = MaterialTheme.typography.bodySmall)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(AppConfig.UserRole.USER, AppConfig.UserRole.STAFF, AppConfig.UserRole.ADMIN).forEach { role ->
                            FilterChip(
                                selected = userProfile?.role == role,
                                onClick = {
                                    if (userProfile != null) {
                                        onUpdateProfile(
                                            userProfile.displayName,
                                            userProfile.phone,
                                            userProfile.vehicleType,
                                            userProfile.vehicleName,
                                            userProfile.licensePlate,
                                            role
                                        )
                                    }
                                },
                                label = { Text(role, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Logout Button
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
