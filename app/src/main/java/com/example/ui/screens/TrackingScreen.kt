package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RescueRequestEntity
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    activeRequest: RescueRequestEntity?,
    onCallPhone: (String) -> Unit,
    onShareLocation: (address: String, lat: Double, lng: Double) -> Unit,
    onCancelRequest: (Long) -> Unit,
    onCompleteRequest: (Long, rating: Int, comment: String) -> Unit,
    onNavigateHome: () -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    // Radar scanning animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing)
        ),
        label = "sweep_angle"
    )
    val pingPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ping_pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Theo Dõi Cứu Hộ Trực Tiếp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (activeRequest == null || activeRequest.status in listOf("COMPLETED", "CANCELLED")) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_tracking_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RescuePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = "Không Có Yêu Cầu Đang Chạy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Phương tiện của bạn hiện an toàn. Khi gặp sự cố đột xuất (hết bình, xịt lốp, chết máy), hãy bấm nút SOS tại trang chủ để nhận hỗ trợ tức thì!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onNavigateHome,
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Về Trang Chủ")
                        }
                    }
                }
            }
        } else {
            // Active Request Live Tracking View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Radar Canvas Map Simulation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .testTag("radar_simulation_map"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RescueSecondary)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val maxRadius = minOf(centerX, centerY) * 0.85f

                            // Draw radar rings
                            for (i in 1..3) {
                                drawCircle(
                                    color = Color(0xFF334155),
                                    radius = maxRadius * (i / 3f),
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.5f)
                                )
                            }

                            // Crosshair lines
                            drawLine(
                                color = Color(0xFF334155),
                                start = Offset(centerX - maxRadius, centerY),
                                end = Offset(centerX + maxRadius, centerY),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF334155),
                                start = Offset(centerX, centerY - maxRadius),
                                end = Offset(centerX, centerY + maxRadius),
                                strokeWidth = 1f
                            )

                            // Radar sweep beam
                            val rad = Math.toRadians(sweepAngle.toDouble())
                            val sweepX = centerX + maxRadius * cos(rad).toFloat()
                            val sweepY = centerY + maxRadius * sin(rad).toFloat()
                            drawLine(
                                color = RescuePrimary.copy(alpha = 0.7f),
                                start = Offset(centerX, centerY),
                                end = Offset(sweepX, sweepY),
                                strokeWidth = 2.5f
                            )

                            // User car marker (Center)
                            drawCircle(
                                color = AlertRed.copy(alpha = pingPulse * 0.4f),
                                radius = 24f * pingPulse,
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = AlertRed,
                                radius = 9f,
                                center = Offset(centerX, centerY)
                            )

                            // Rescue vehicle marker (Approaching)
                            val distanceFactor = (activeRequest.estimatedDistanceKm / 4.0).coerceIn(0.15, 0.75)
                            val techX = centerX + (maxRadius * distanceFactor * 0.8f).toFloat()
                            val techY = centerY - (maxRadius * distanceFactor * 0.6f).toFloat()

                            drawCircle(
                                color = RescuePrimary.copy(alpha = 0.35f),
                                radius = 18f,
                                center = Offset(techX, techY)
                            )
                            drawCircle(
                                color = EmergencyGold,
                                radius = 10f,
                                center = Offset(techX, techY)
                            )
                        }

                        // Status Badge on Map
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (activeRequest.status == "ARRIVED" || activeRequest.status == "IN_PROGRESS") SafeGreen else RescuePrimary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = "Mã đơn: ${activeRequest.orderCode}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // ETA Pill on Map
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = RescuePrimary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (activeRequest.status in listOf("ARRIVED", "IN_PROGRESS")) "ĐÃ ĐẾN HIỆN TRƯỜNG" else "Dự kiến: ${activeRequest.estimatedMinutes} phút (${activeRequest.estimatedDistanceKm} km)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Step Progression Tracker Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tiến Trình Cứu Hộ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val steps = listOf(
                            "FINDING" to "Tìm kiếm đội cứu hộ gần nhất",
                            "ASSIGNED" to "Kỹ thuật viên đã nhận đơn",
                            "EN_ROUTE" to "Xe cứu hộ đang di chuyển đến",
                            "ARRIVED" to "KTV đã đến & đang xử lý sự cố"
                        )

                        val currentStepIdx = when (activeRequest.status) {
                            "FINDING" -> 0
                            "ASSIGNED" -> 1
                            "EN_ROUTE" -> 2
                            "ARRIVED", "IN_PROGRESS" -> 3
                            else -> 0
                        }

                        steps.forEachIndexed { index, pair ->
                            val isDone = index < currentStepIdx
                            val isCurrent = index == currentStepIdx
                            val isUpcoming = index > currentStepIdx

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
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("${index + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Text(
                                    text = pair.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Technician Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("technician_info_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RescuePrimary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        text = activeRequest.technicianName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(14.dp))
                                        Text(" 4.9 • Đội cứu hộ cơ động 24/7", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Quick Call Button
                            FilledIconButton(
                                onClick = { onCallPhone(activeRequest.technicianPhone) },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeGreen),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("btn_call_technician")
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Gọi KTV", tint = Color.White)
                            }
                        }

                        HorizontalDivider()

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                Text("Xe chuyên dụng: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(activeRequest.technicianVehicle, fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                Text("Xe cần cứu hộ: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${activeRequest.vehicleType} • ${activeRequest.licensePlate}", fontSize = 13.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Địa điểm: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(activeRequest.locationAddress, fontSize = 13.sp, maxLines = 1)
                                    }
                                    if (activeRequest.latitude != 0.0 && activeRequest.longitude != 0.0) {
                                        Text(
                                            text = "Tọa độ GPS: ${String.format(Locale.US, "%.6f, %.6f", activeRequest.latitude, activeRequest.longitude)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                                Text("Chi phí dự kiến: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(formatter.format(activeRequest.estimatedCost), fontWeight = FontWeight.Bold, color = SafeGreen, fontSize = 14.sp)
                            }
                        }

                        // Share location with technician
                        OutlinedButton(
                            onClick = {
                                onShareLocation(
                                    activeRequest.locationAddress,
                                    activeRequest.latitude,
                                    activeRequest.longitude
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gửi thêm vị trí chi tiết cho KTV")
                        }
                    }
                }

                // Action controls: Complete or Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCancelConfirmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                    ) {
                        Text("Hủy Đơn")
                    }

                    Button(
                        onClick = { showRatingDialog = true },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("btn_complete_rescue"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ĐÃ XỬ LÝ XONG", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Cancel Confirm Dialog
    if (showCancelConfirmDialog && activeRequest != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Hủy Yêu Cầu Cứu Hộ?") },
            text = { Text("Kỹ thuật viên đang chuẩn bị hoặc trên đường đến hỗ trợ. Bạn có chắc chắn muốn hủy yêu cầu cứu hộ này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelRequest(activeRequest.id)
                    }
                ) {
                    Text("Xác nhận hủy", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Rating & Completion Dialog
    if (showRatingDialog && activeRequest != null) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            icon = { Icon(Icons.Default.Stars, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(40.dp)) },
            title = {
                Text(
                    text = "Hoàn Tất & Đánh Giá Cứu Hộ",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cảm ơn bạn đã tin tưởng dịch vụ Cứu Hộ 24/7! Vui lòng đánh giá kỹ thuật viên:")

                    // Star Selector
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
                        label = { Text("Nhận xét về kỹ thuật viên (Tùy chọn)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Text(
                        text = "Số tiền thanh toán: ${formatter.format(activeRequest.estimatedCost)}",
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRatingDialog = false
                        onCompleteRequest(activeRequest.id, ratingStars, ratingComment)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("GỬI ĐÁNH GIÁ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false }) {
                    Text("Bỏ qua")
                }
            }
        )
    }
}
