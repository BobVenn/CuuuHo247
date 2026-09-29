package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.ui.components.CreateRequestDialog
import com.example.ui.theme.*
import com.example.util.UserLocationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    currentLocation: UserLocationInfo,
    isLocating: Boolean,
    activeRequest: RescueRequest?,
    recentRequests: List<RescueRequest>,
    onRefreshLocation: () -> Unit,
    onEmergencySosCall: () -> Unit,
    onCreateRequest: (issueType: String, description: String, vehicleType: String, licensePlate: String) -> Unit,
    onRequestClick: (RescueRequest) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onShareLocation: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedIssueForDialog by remember { mutableStateOf<String?>(null) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale("vi", "VN")) }

    // Pulsating animation for SOS emergency button
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // TOP HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RescueSecondary,
                            RescueSecondary.copy(alpha = 0.95f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Title, User Name and Emergency Hotline
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
                            shape = RoundedCornerShape(12.dp),
                            color = RescuePrimary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CỨU HỘ XE 24/7",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SafeGreen
                                ) {
                                    Text(
                                        text = "ONLINE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (userProfile?.displayName.isNullOrBlank()) "Trực chiến toàn quốc 24/7" else "Xin chào, ${userProfile?.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Emergency Hotline Dialer Header Button
                    FilledTonalButton(
                        onClick = { showSosConfirmDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AlertRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("emergency_sos_header_button")
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Emergency SOS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SOS ${AppConfig.RESCUE_HOTLINE_DISPLAY}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Current GPS Location Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_info_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(RescuePrimary.copy(alpha = 0.4f), Color.Transparent)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = RescuePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "VỊ TRÍ THỰC TẾ (GPS)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RescuePrimary
                                )
                                Text(
                                    text = if (isLocating) "Đang tìm kiếm tọa độ GPS..." else currentLocation.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = RescuePrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Làm mới",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Request Ongoing Banner (from Firebase)
        if (activeRequest != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { onRequestClick(activeRequest) }
                    .testTag("banner_active_request"),
                colors = CardDefaults.cardColors(containerColor = RescuePrimary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(RescuePrimary, EmergencyGold)))
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
                            color = RescuePrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White)
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "YÊU CẦU ĐANG XỬ LÝ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RescuePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RescuePrimary
                                ) {
                                    Text(
                                        text = activeRequest.status,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${activeRequest.issueType} • ${activeRequest.licensePlate}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!activeRequest.staffName.isNullOrBlank()) {
                                Text(
                                    text = "Kỹ thuật viên: ${activeRequest.staffName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = "Xem chi tiết",
                        tint = RescuePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Center Emergency Call SOS Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TRƯỜNG HỢP KHẨN CẤP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsating SOS Trigger Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .testTag("sos_button_container")
            ) {
                // Pulse waves
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AlertRed.copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .scale(pulseScale * 0.94f)
                        .clip(CircleShape)
                        .background(AlertRed.copy(alpha = pulseAlpha + 0.1f))
                )

                // Core Button
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .clickable { showSosConfirmDialog = true }
                        .testTag("main_sos_button"),
                    shape = CircleShape,
                    color = AlertRed,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444),
                                        Color(0xFFDC2626),
                                        Color(0xFF991B1B)
                                    )
                                )
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "GỌI CỨU HỘ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "HOTLINE 24/7",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bấm để liên hệ tổng đài cứu hộ giao thông quốc gia ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        // Action Buttons: Tạo yêu cầu mới & Chia sẻ vị trí
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    selectedIssueForDialog = null
                    showCreateDialog = true
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("btn_open_create_request"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gửi Yêu Cầu Cứu Hộ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onShareLocation,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_share_location"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chia Sẻ Vị Trí", fontSize = 13.sp)
            }
        }

        // CÁC LOẠI SỰ CỐ PHỔ BIẾN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CÁC LOẠI SỰ CỐ PHỔ BIẾN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chọn để gửi nhanh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Grid of 6 common issues
            val issues = AppConfig.ISSUE_TYPES.take(6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                issues.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { issue ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedIssueForDialog = issue
                                        showCreateDialog = true
                                    }
                                    .testTag("issue_card_${issue.replace(" ", "_")}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (issue) {
                                            "Xe hết xăng" -> Icons.Default.LocalGasStation
                                            "Hỏng xe" -> Icons.Default.Build
                                            "Thủng lốp" -> Icons.Default.Settings
                                            "Hết bình" -> Icons.Default.Bolt
                                            "Tai nạn" -> Icons.Default.Warning
                                            "Khóa xe" -> Icons.Default.Key
                                            else -> Icons.Default.HelpOutline
                                        },
                                        contentDescription = null,
                                        tint = RescuePrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = issue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // YÊU CẦU CỨU HỘ GẦN ĐÂY TỪ FIREBASE (Hoặc Empty State)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YÊU CẦU GẦN ĐÂY CỦA BẠN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text("Tất cả", color = RescuePrimary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RescuePrimary)
                }
            }

            if (recentRequests.isEmpty()) {
                // Professional Empty State (No Mock Data)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_recent_requests_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Chưa có yêu cầu cứu hộ nào",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Khi bạn gửi yêu cầu cứu hộ, thông tin sẽ được cập nhật trực tiếp tại đây từ Firebase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Show real requests from Firebase
                recentRequests.take(3).forEach { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRequestClick(req) }
                            .testTag("recent_request_item_${req.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = req.issueType,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (req.status) {
                                            AppConfig.RequestStatus.COMPLETED -> SafeGreen.copy(alpha = 0.15f)
                                            AppConfig.RequestStatus.CANCELLED -> AlertRed.copy(alpha = 0.15f)
                                            else -> RescuePrimary.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = req.status,
                                            color = when (req.status) {
                                                AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                                AppConfig.RequestStatus.CANCELLED -> AlertRed
                                                else -> RescuePrimary
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${req.vehicleType} • ${req.licensePlate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = req.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = dateFormat.format(Date(req.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Support & Feedback Quick Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onNavigateToReports() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Feedback, contentDescription = null, tint = RescuePrimary)
                    Column {
                        Text(
                            text = "Trung Tâm Phản Hồi & Báo Cáo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Góp ý chất lượng phục vụ hoặc báo cáo sự cố",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = RescuePrimary)
            }
        }
    }

    // SOS CALL CONFIRMATION DIALOG (Mandatory confirmation before dialer)
    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "XÁC NHẬN GỌI CỨU HỘ KHẨN CẤP",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ứng dụng sẽ mở trình quay số thiết bị tới Hotline Cứu Hộ Giao Thông 24/7:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📞 ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = AlertRed
                            )
                            Text(
                                text = "Tổng đài Cứu hộ Giao thông Khẩn cấp Quốc gia (Trực ban 24/7)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Vị trí của bạn: ${currentLocation.address}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        onEmergencySosCall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    modifier = Modifier.testTag("btn_confirm_call_dialer")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GỌI NGAY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // CREATE REQUEST DIALOG
    if (showCreateDialog && userProfile != null) {
        CreateRequestDialog(
            initialIssueType = selectedIssueForDialog,
            userProfile = userProfile,
            currentLocation = currentLocation,
            onDismiss = { showCreateDialog = false },
            onSubmit = { issue, desc, vType, plate ->
                showCreateDialog = false
                onCreateRequest(issue, desc, vType, plate)
            }
        )
    }
}
