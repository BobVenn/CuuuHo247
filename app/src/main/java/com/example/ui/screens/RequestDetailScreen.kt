package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: RescueRequest,
    currentUserProfile: UserProfile?,
    onBack: () -> Unit,
    onCallPhone: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptRequestByStaff: (String) -> Unit,
    onUpdateStatusByStaff: (String, String, Long?) -> Unit,
    onSubmitRating: (rating: Int, comment: String) -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showUpdateCostDialog by remember { mutableStateOf(false) }
    var costInput by remember { mutableStateOf(request.cost?.toString() ?: "250000") }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale("vi", "VN")) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val isStaffOrAdmin = currentUserProfile?.role == AppConfig.UserRole.STAFF ||
            currentUserProfile?.role == AppConfig.UserRole.ADMIN

    val isUserOwner = currentUserProfile?.uid == request.userId

    // Status Stepper Index
    val statusSteps = listOf(
        AppConfig.RequestStatus.PENDING,
        AppConfig.RequestStatus.ACCEPTED,
        AppConfig.RequestStatus.EN_ROUTE,
        AppConfig.RequestStatus.ARRIVED,
        AppConfig.RequestStatus.IN_PROGRESS,
        AppConfig.RequestStatus.COMPLETED
    )
    val currentStepIndex = statusSteps.indexOf(request.status).takeIf { it >= 0 } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chi Tiết Yêu Cầu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Mã đơn: ${request.id.takeLast(8)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiến trình xử lý sự cố", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (request.status) {
                                AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                AppConfig.RequestStatus.CANCELLED -> AlertRed
                                else -> RescuePrimary
                            }
                        ) {
                            Text(
                                text = request.status,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (request.status != AppConfig.RequestStatus.CANCELLED) {
                        statusSteps.forEachIndexed { index, step ->
                            val isDone = index < currentStepIndex
                            val isCurrent = index == currentStepIndex

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isDone -> SafeGreen
                                        isCurrent -> RescuePrimary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        } else if (isCurrent) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("${index + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Yêu cầu cứu hộ này đã bị hủy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlertRed
                        )
                    }
                }
            }

            // Incident Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Thông Tin Sự Cố", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                        Text("Sự cố: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(request.issueType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RescuePrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                        Text("Phương tiện: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${request.vehicleType} • ${request.licensePlate}", fontSize = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column {
                            Text("Vị trí: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(request.address, fontSize = 14.sp)
                            if (request.latitude != 0.0 && request.longitude != 0.0) {
                                Text("Tọa độ GPS: ${request.latitude}, ${request.longitude}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (request.description.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                            Column {
                                Text("Mô tả của người dùng: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(request.description, fontSize = 14.sp)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text("Thời gian tạo: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(dateFormat.format(Date(request.timestamp)), fontSize = 14.sp)
                    }

                    if (request.cost != null && request.cost > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                            Text("Chi phí cứu hộ: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(currencyFormat.format(request.cost), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        }
                    }
                }
            }

            // Customer / Technician Card
            if (!isStaffOrAdmin) {
                // User sees Technician info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kỹ Thuật Viên Phụ Trách", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (!request.staffName.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = RescuePrimary, modifier = Modifier.size(46.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                    Column {
                                        Text(request.staffName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(request.staffPhone ?: "Chưa có SĐT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!request.staffPhone.isNullOrBlank()) {
                                        FilledIconButton(
                                            onClick = { onCallPhone(request.staffPhone) },
                                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeGreen)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = "Gọi KTV", tint = Color.White)
                                        }
                                    }
                                    FilledIconButton(
                                        onClick = { onOpenChat(request.id) },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = RescuePrimary)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Hệ thống đang điều phối nhân viên cứu hộ gần nhất...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Staff / Admin sees Customer info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Thông Tin Khách Hàng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(request.userName.ifBlank { "Khách hàng" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(request.userPhone.ifBlank { "Chưa cập nhật SĐT" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (request.userPhone.isNotBlank()) {
                                    FilledIconButton(
                                        onClick = { onCallPhone(request.userPhone) },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeGreen)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Gọi khách", tint = Color.White)
                                    }
                                }
                                FilledIconButton(
                                    onClick = { onOpenChat(request.id) },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = RescuePrimary)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Rating display if already rated
            if (request.rating != null && request.rating > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Đánh Giá Từ Khách Hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(request.rating) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (!request.ratingComment.isNullOrBlank()) {
                            Text("\"${request.ratingComment}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // USER ACTION BUTTONS
            if (isUserOwner) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Chat button
                    Button(
                        onClick = { onOpenChat(request.id) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhắn Tin")
                    }

                    // Rating button if completed
                    if (request.status == AppConfig.RequestStatus.COMPLETED && request.rating == null) {
                        Button(
                            onClick = { showRatingDialog = true },
                            modifier = Modifier.weight(1.2f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đánh Giá 5★")
                        }
                    }

                    // Cancel button if not completed
                    if (request.status == AppConfig.RequestStatus.PENDING || request.status == AppConfig.RequestStatus.ACCEPTED) {
                        OutlinedButton(
                            onClick = { showCancelConfirmDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                        ) {
                            Text("Hủy Đơn")
                        }
                    }
                }
            }

            // RESCUE STAFF / ADMIN ACTIONS
            if (isStaffOrAdmin) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bảng Điều Khiển Nhân Viên", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (request.status == AppConfig.RequestStatus.PENDING) {
                            Button(
                                onClick = { onAcceptRequestByStaff(request.id) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TIẾP NHẬN ĐƠN NÀY", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (request.status == AppConfig.RequestStatus.ACCEPTED) {
                            Button(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.EN_ROUTE, request.cost) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                            ) {
                                Text("CẬP NHẬT: ĐANG ĐẾN")
                            }
                        }

                        if (request.status == AppConfig.RequestStatus.EN_ROUTE) {
                            Button(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.ARRIVED, request.cost) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                            ) {
                                Text("CẬP NHẬT: ĐÃ ĐẾN NƠI")
                            }
                        }

                        if (request.status == AppConfig.RequestStatus.ARRIVED) {
                            Button(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.IN_PROGRESS, request.cost) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                            ) {
                                Text("CẬP NHẬT: ĐANG XỬ LÝ")
                            }
                        }

                        if (request.status == AppConfig.RequestStatus.IN_PROGRESS) {
                            Button(
                                onClick = { showUpdateCostDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HOÀN THÀNH & NHẬP CHI PHÍ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirm Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Hủy Yêu Cầu Cứu Hộ?") },
            text = { Text("Bạn có chắc chắn muốn hủy đơn cứu hộ này? Thông tin sẽ được cập nhật trực tiếp lên hệ thống.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelRequest(request.id)
                    }
                ) {
                    Text("Xác nhận hủy", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Đóng") }
            }
        )
    }

    // Rating Dialog
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Đánh Giá Dịch Vụ Cứu Hộ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Vui lòng đánh giá chất lượng phục vụ của kỹ thuật viên:")
                    Row(horizontalArrangement = Arrangement.Center) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { ratingStars = star }) {
                                Icon(
                                    imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star sao",
                                    tint = EmergencyGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = ratingComment,
                        onValueChange = { ratingComment = it },
                        label = { Text("Nhận xét (Tùy chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRatingDialog = false
                        onSubmitRating(ratingStars, ratingComment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Gửi Đánh Giá")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) { Text("Hủy") }
            }
        )
    }

    // Update Cost & Complete Dialog (Staff)
    if (showUpdateCostDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateCostDialog = false },
            title = { Text("Xác Nhận Hoàn Thành") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhập chi phí thực tế thu của khách hàng (VND):")
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Chi phí thực tế (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateCostDialog = false
                        val cost = costInput.toLongOrNull() ?: 0L
                        onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.COMPLETED, cost)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Xác Nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateCostDialog = false }) { Text("Hủy") }
            }
        )
    }
}
