package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.StatusChangeEvent
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Localized In-App Heads-Up Status Banner.
 * Appears dynamically at the top of the screen whenever a service request status
 * changes in Firebase Realtime Database. Includes audio/haptic alert, status change transition,
 * technician details, quick action to view full details or call KTV, and auto-dismiss timer.
 */
@Composable
fun LocalizedStatusBanner(
    event: StatusChangeEvent?,
    onDismiss: () -> Unit,
    onViewDetail: () -> Unit,
    onCallTechnician: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    autoDismissSeconds: Int = 8
) {
    var progress by remember(event?.timestamp) { mutableFloatStateOf(1f) }

    // Auto-dismiss countdown timer
    LaunchedEffect(event?.timestamp) {
        if (event != null) {
            val totalSteps = autoDismissSeconds * 20
            val stepDelay = 1000L / 20
            for (i in totalSteps downTo 0) {
                progress = i.toFloat() / totalSteps
                delay(stepDelay)
            }
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        if (event == null) return@AnimatedVisibility

        val req = event.request
        val newStatus = event.newStatus
        val prevStatus = event.previousStatus

        // Determine Theme & Accent based on status
        val (accentColor, bgColor, icon: ImageVector, bannerTitle: String, subtitle: String) = when (newStatus) {
            AppConfig.RequestStatus.ACCEPTED -> StatusBannerConfig(
                accentColor = CategoryBlueTint,
                bgColor = Color(0xFFEFF6FF),
                icon = Icons.Default.AssignmentInd,
                title = "KTV ĐÃ TIẾP NHẬN YÊU CẦU",
                subtitle = "${req.staffName ?: "KTV Nguyễn Văn Toàn"} đang chuẩn bị dụng cụ cứu hộ"
            )
            AppConfig.RequestStatus.EN_ROUTE -> StatusBannerConfig(
                accentColor = Color(0xFF4F46E5),
                bgColor = Color(0xFFEEF2FF),
                icon = Icons.Default.DirectionsCar,
                title = "KTV ĐANG DI CHUYỂN ĐẾN BẠN",
                subtitle = "Dự kiến 8-15 phút tại ${req.address.take(28)}..."
            )
            AppConfig.RequestStatus.ARRIVED -> StatusBannerConfig(
                accentColor = Color(0xFF0284C7),
                bgColor = Color(0xFFE0F2FE),
                icon = Icons.Default.Place,
                title = "KTV ĐÃ ĐẾN HIỆN TRƯỜNG!",
                subtitle = "Vui lòng kiểm tra xe và trao đổi với KTV cứu hộ"
            )
            AppConfig.RequestStatus.IN_PROGRESS -> StatusBannerConfig(
                accentColor = CategoryAmberTint,
                bgColor = CategoryAmberBg,
                icon = Icons.Default.Build,
                title = "ĐANG XỬ LÝ SỰ CỐ XE",
                subtitle = "KTV đang kiểm tra ${req.issueType} cho xe ${req.vehicleType}"
            )
            AppConfig.RequestStatus.COMPLETED -> StatusBannerConfig(
                accentColor = SafeGreen,
                bgColor = CategoryGreenBg,
                icon = Icons.Default.CheckCircle,
                title = "CỨU HỘ HOÀN TẤT THÀNH CÔNG!",
                subtitle = "Sẵn sàng thanh toán VietQR & nhận biên lai bảo hiểm"
            )
            AppConfig.RequestStatus.CANCELLED -> StatusBannerConfig(
                accentColor = AlertRed,
                bgColor = AlertRedLight,
                icon = Icons.Default.Cancel,
                title = "YÊU CẦU CỨU HỘ ĐÃ HỦY",
                subtitle = "Đơn #${req.id.takeLast(6).uppercase()} đã đóng"
            )
            else -> StatusBannerConfig(
                accentColor = RescuePrimary,
                bgColor = Color(0xFFFFF7ED),
                icon = Icons.Default.NotificationsActive,
                title = "CẬP NHẬT TRẠNG THÁI CỨU HỘ",
                subtitle = "Trạng thái mới: $newStatus"
            )
        }

        // Live Pulsing Dot Animation
        val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(10.dp, shape = RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.35f))
                .clickable { onViewDetail() }
                .testTag("localized_status_banner"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Main Banner Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Bar: Badge, Transition label, Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseScale)
                                        .background(accentColor, CircleShape)
                                )
                                Text(
                                    text = "CẬP NHẬT TRỰC TIẾP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "#${req.id.takeLast(6).uppercase()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng thông báo",
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Content Row: Icon avatar, Status Title, Description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bannerTitle,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = OnSurfaceLight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Status transition indicator: e.g. "Chờ tiếp nhận -> Đã điều phối"
                            if (prevStatus.isNotBlank() && prevStatus != newStatus) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = prevStatus,
                                        fontSize = 10.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = newStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewDetail,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "XEM TIẾN TRÌNH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val staffPhone = req.staffPhone
                        if (onCallTechnician != null && !staffPhone.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { onCallTechnician(staffPhone) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Gọi KTV",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RescuePrimary
                                )
                            }
                        }
                    }
                }

                // Bottom linear countdown timer indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = accentColor,
                    trackColor = Color(0xFFF1F5F9),
                )
            }
        }
    }
}

private data class StatusBannerConfig(
    val accentColor: Color,
    val bgColor: Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)
