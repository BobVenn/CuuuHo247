package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RescueRequest
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time Emergency Status Dashboard powered by Firebase Realtime Database observers.
 * Listens directly to requestsRef.child(requestId) via ValueEventListener (callbackFlow)
 * and animates state transitions dynamically:
 * - Chờ tiếp nhận (Pending)
 * - Đã điều phối (Dispatched / Accepted)
 * - Đang di chuyển (En Route)
 * - Đã đến nơi (Arrived)
 * - Đang xử lý (In Progress)
 * - Hoàn thành (Completed)
 */
@Composable
fun RealtimeStatusDashboard(
    request: RescueRequest,
    realtimeRequestFlow: Flow<RescueRequest?>? = null,
    isEmbeddedCard: Boolean = false,
    onCallHotline: (String) -> Unit = {},
    onCallTechnician: (String) -> Unit = {},
    onOpenChat: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onSimulateStep: ((String) -> Unit)? = null,
    onOpenFullDetail: (() -> Unit)? = null,
    onOpenPayment: (() -> Unit)? = null,
    onOpenReceipt: (() -> Unit)? = null,
    onRateService: (() -> Unit)? = null,
    onNavigateToLocation: (() -> Unit)? = null
) {
    // 1. Observe real-time changes directly from Firebase Realtime Database
    val firebaseManager = remember { FirebaseManager.getInstance() }
    val observedRequestFlow = remember(request.id) {
        realtimeRequestFlow ?: firebaseManager.getRequestByIdFlow(request.id)
    }
    val liveRequestState by observedRequestFlow.collectAsState(initial = request)
    val current = liveRequestState ?: request

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("vi", "VN")) }

    // Pulsing animation for active live observer
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Current State Mapping
    val statusStage = when (current.status) {
        AppConfig.RequestStatus.PENDING -> 1
        AppConfig.RequestStatus.ACCEPTED -> 2
        AppConfig.RequestStatus.EN_ROUTE -> 3
        AppConfig.RequestStatus.ARRIVED -> 4
        AppConfig.RequestStatus.IN_PROGRESS -> 4
        AppConfig.RequestStatus.COMPLETED -> 5
        AppConfig.RequestStatus.CANCELLED -> -1
        else -> 1
    }

    val stageColor by animateColorAsState(
        targetValue = when (statusStage) {
            1 -> CategoryAmberTint
            2 -> CategoryBlueTint
            3 -> Color(0xFF4F46E5) // Indigo
            4 -> Color(0xFF0284C7) // Sky / Cyan
            5 -> SafeGreen
            else -> AlertRed
        },
        label = "stage_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realtime_status_dashboard_${current.id}")
            .then(
                if (isEmbeddedCard && onOpenFullDetail != null) Modifier.clickable { onOpenFullDetail() }
                else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, stageColor.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEmbeddedCard) 3.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------------------------------------------------------
            // HEADER: REALTIME OBSERVER BADGE & ORDER CODE
            // ---------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Firebase Database indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SafeGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .background(SafeGreen, CircleShape)
                        )
                        Text(
                            text = "FIREBASE LIVE OBSERVER",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = SafeGreen
                        )
                    }
                }

                // Order Code Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "#${current.id.takeLast(7).uppercase()}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // ---------------------------------------------------------
            // ACTIVE STATE HERO BANNER
            // ---------------------------------------------------------
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (statusStage) {
                    1 -> CategoryAmberBg
                    2 -> CategoryBlueBg
                    3 -> Color(0xFFEEF2FF)
                    4 -> Color(0xFFE0F2FE)
                    5 -> CategoryGreenBg
                    else -> AlertRedLight
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated Icon Avatar
                    Surface(
                        shape = CircleShape,
                        color = stageColor,
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (statusStage in 1..4) pulseScale else 1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (statusStage) {
                                    1 -> Icons.Default.HourglassTop
                                    2 -> Icons.Default.AssignmentInd
                                    3 -> Icons.Default.DirectionsCar
                                    4 -> Icons.Default.Place
                                    5 -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Cancel
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = when (current.status) {
                                    AppConfig.RequestStatus.PENDING -> "Chờ tiếp nhận cứu hộ"
                                    AppConfig.RequestStatus.ACCEPTED -> "Đã điều phối KTV"
                                    AppConfig.RequestStatus.EN_ROUTE -> "KTV đang di chuyển đến"
                                    AppConfig.RequestStatus.ARRIVED -> "KTV đã đến hiện trường"
                                    AppConfig.RequestStatus.IN_PROGRESS -> "Đang xử lý kỹ thuật"
                                    AppConfig.RequestStatus.COMPLETED -> "Cứu hộ thành công!"
                                    AppConfig.RequestStatus.CANCELLED -> "Đơn yêu cầu đã hủy"
                                    else -> current.status
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = stageColor
                            )
                        }

                        Text(
                            text = when (statusStage) {
                                1 -> "Đang kết nối trạm & xe cứu hộ gần nhất trong 5km"
                                2 -> "KTV ${current.staffName ?: "Nguyễn Văn Toàn"} đã nhận đơn"
                                3 -> "Dự kiến có mặt sau ~8-12 phút (${current.address.take(28)}...)"
                                4 -> "KTV đang kiểm tra xe tại vị trí của bạn"
                                5 -> "Đã khắc phục sự cố, sẵn sàng thanh toán & xuất hóa đơn"
                                else -> "Yêu cầu đã được đóng lại"
                            },
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // 5-STAGE REALTIME STEPPER PROGRESS BAR
            // ---------------------------------------------------------
            if (current.status != AppConfig.RequestStatus.CANCELLED) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tiến độ điều phối cứu hộ thời gian thực:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariantLight
                    )

                    val steps = listOf(
                        Triple(1, "Chờ duyệt", Icons.Default.HourglassBottom),
                        Triple(2, "Điều phối", Icons.Default.Badge),
                        Triple(3, "Đang đến", Icons.Default.DirectionsCar),
                        Triple(4, "Đến nơi", Icons.Default.Place),
                        Triple(5, "Hoàn tất", Icons.Default.Check)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.forEachIndexed { idx, (stageNumber, label, icon) ->
                            val isCompleted = statusStage > stageNumber
                            val isCurrent = statusStage == stageNumber
                            val stepColor = when {
                                isCompleted -> SafeGreen
                                isCurrent -> stageColor
                                else -> Color(0xFFCBD5E1)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCompleted) SafeGreen else if (isCurrent) stageColor else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.5.dp, stepColor),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        } else {
                                            Text(
                                                text = "$stageNumber",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                    color = if (isCurrent) stageColor else if (isCompleted) SafeGreen else Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Divider connecting steps
                            if (idx < steps.size - 1) {
                                val nextStageCompleted = statusStage > stageNumber
                                Box(
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .height(2.5.dp)
                                        .background(if (nextStageCompleted) SafeGreen else Color(0xFFE2E8F0))
                                )
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // ASSIGNED TECHNICIAN CARD (KTV PHỤ TRÁCH)
            // ---------------------------------------------------------
            if (statusStage in 2..5) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    color = RescuePrimary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Engineering, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        text = current.staffName ?: "KTV Nguyễn Văn Toàn",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = "Xe sàn trượt • Biển số: 29C-888.12",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                }
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = CategoryGreenBg) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(12.dp))
                                    Text("4.9", fontSize = 11.sp, fontWeight = FontWeight.Black, color = CategoryAmberTint)
                                }
                            }
                        }

                        // Contact Actions Row: Gọi KTV, Nhắn tin chat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val phone = current.staffPhone?.ifBlank { "0901234567" } ?: "0901234567"
                                    onCallTechnician(phone)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gọi KTV", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenChat,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp), tint = RescuePrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nhắn Tin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            if (onNavigateToLocation != null) {
                                OutlinedIconButton(
                                    onClick = onNavigateToLocation,
                                    modifier = Modifier.size(38.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = "Chỉ đường", tint = SafeGreen)
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // INCIDENT SUMMARY & LOCATION
            // ---------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CategoryBlueBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = CategoryBlueTint, modifier = Modifier.size(18.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${current.issueType} • ${current.vehicleType}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = current.address.ifBlank { "Tọa độ: ${current.latitude}, ${current.longitude}" },
                        fontSize = 11.sp,
                        color = OnSurfaceVariantLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ---------------------------------------------------------
            // ACTION BUTTONS: HOTLINE, DETAIL, OR PAYMENT/RECEIPT
            // ---------------------------------------------------------
            if (isEmbeddedCard) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenFullDetail?.invoke() },
                        modifier = Modifier.weight(1.3f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("XEM TIẾN TRÌNH CHI TIẾT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onCallHotline(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp), tint = AlertRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hotline 24/7", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                    }
                }
            } else {
                // If Completed: Show VietQR payment, receipt & rating buttons
                if (statusStage == 5) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenPayment != null) {
                                Button(
                                    onClick = onOpenPayment,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quét VietQR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (onOpenReceipt != null) {
                                OutlinedButton(
                                    onClick = onOpenReceipt,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Biên Lai", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (onRateService != null) {
                            OutlinedButton(
                                onClick = onRateService,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Đánh Giá Dịch Vụ Cứu Hộ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
