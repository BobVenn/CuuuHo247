package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.ServiceAvailabilityItem
import com.example.ui.theme.*

/**
 * Helper mapping service icon keys to distinctive Material 3 ImageVectors and themed colors.
 */
object ServiceVisualTheme {
    fun getIcon(iconKey: String): ImageVector = when (iconKey.lowercase()) {
        "towing", "tow_truck" -> Icons.Filled.LocalShipping
        "jump_start", "battery", "battery_jump" -> Icons.Filled.Bolt
        "tire_change", "tire", "tire_puncture" -> Icons.Filled.Settings
        "fuel_delivery", "fuel", "gas" -> Icons.Filled.LocalGasStation
        "locksmith", "key", "lockout" -> Icons.Filled.Key
        "mobile_repair", "repair", "mechanic" -> Icons.Filled.Build
        else -> Icons.Filled.BuildCircle
    }

    fun getBgColor(iconKey: String): Color = when (iconKey.lowercase()) {
        "towing", "tow_truck" -> Color(0xFFEFF6FF) // Blue 50
        "jump_start", "battery", "battery_jump" -> Color(0xFFFFFBEB) // Amber 50
        "tire_change", "tire", "tire_puncture" -> Color(0xFFECFDF5) // Emerald 50
        "fuel_delivery", "fuel", "gas" -> Color(0xFFFEF2F2) // Red 50
        "locksmith", "key", "lockout" -> Color(0xFFFAF5FF) // Purple 50
        "mobile_repair", "repair", "mechanic" -> Color(0xFFFFF7ED) // Orange 50
        else -> Color(0xFFF1F5F9)
    }

    fun getAccentColor(iconKey: String): Color = when (iconKey.lowercase()) {
        "towing", "tow_truck" -> Color(0xFF1D4ED8) // Blue 700
        "jump_start", "battery", "battery_jump" -> Color(0xFFB45309) // Amber 700
        "tire_change", "tire", "tire_puncture" -> Color(0xFF047857) // Emerald 700
        "fuel_delivery", "fuel", "gas" -> Color(0xFFB91C1C) // Red 700
        "locksmith", "key", "lockout" -> Color(0xFF7E22CE) // Purple 700
        "mobile_repair", "repair", "mechanic" -> Color(0xFFC2410C) // Orange 700
        else -> RescuePrimary
    }
}

/**
 * Dashboard UI hiển thị danh sách tình trạng sẵn sàng phục vụ của các dịch vụ cứu hộ
 * (Cẩu kéo xe, Kích bình, Vá lốp, Tiếp xăng, Mở khóa, Sửa lưu động)
 * Dữ liệu được fetch và đồng bộ thời gian thực từ Firebase Realtime Database.
 */
@Composable
fun ServiceAvailabilityDashboard(
    services: List<ServiceAvailabilityItem>,
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onRequestService: (ServiceAvailabilityItem) -> Unit = {},
    onToggleAvailability: ((serviceId: String, isAvailable: Boolean) -> Unit)? = null,
    onCallEmergencyHotline: (String) -> Unit = {},
    isStaffUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var detailDialogItem by remember { mutableStateOf<ServiceAvailabilityItem?>(null) }
    var showStaffControls by remember { mutableStateOf(false) }

    // Pulsing animation cho trạng thái trực tiếp của Firebase
    val infiniteTransition = rememberInfiniteTransition(label = "availability_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    // Lọc dịch vụ theo danh mục được chọn
    val filteredServices = remember(services, selectedCategoryFilter) {
        when (selectedCategoryFilter) {
            "ALL" -> services
            "TOWING" -> services.filter { it.id == "towing" || it.category == "TOWING" }
            "BATTERY" -> services.filter { it.id == "jump_start" || it.category == "BATTERY" }
            "TIRE" -> services.filter { it.id == "tire_change" || it.category == "TIRE" }
            "OTHER" -> services.filter { it.id !in listOf("towing", "jump_start", "tire_change") }
            else -> services
        }
    }

    // Thống kê tổng quan từ Firebase
    val availableCount = services.count { it.isAvailable }
    val totalCount = services.size.coerceAtLeast(1)
    val totalTechnicians = services.sumOf { if (it.isAvailable) it.activeTechnicians else 0 }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_availability_dashboard"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER: Title + Firebase Live Status Indicator + Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(SafeGreen.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "BẢNG ĐIỀU PHỐI DỊCH VỤ 24/7",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = OnSurfaceLight,
                            fontSize = 14.5.sp
                        )
                    }
                    Text(
                        text = "Trạng thái trực ban thời gian thực từ Firebase",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantLight,
                        fontSize = 11.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onToggleAvailability != null) {
                        IconButton(
                            onClick = { showStaffControls = !showStaffControls },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_toggle_staff_controls")
                        ) {
                            Icon(
                                imageVector = if (showStaffControls) Icons.Filled.Tune else Icons.Outlined.Tune,
                                contentDescription = "Bật điều chỉnh trực ca",
                                tint = if (showStaffControls) RescuePrimary else OnSurfaceVariantLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_refresh_services_dashboard")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = RescuePrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới dữ liệu",
                                tint = RescuePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. QUICK METRICS OVERVIEW STRIP (3 Chỉ số điều phối quan trọng)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metric 1: Sẵn sàng phục vụ
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$availableCount/$totalCount",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = SafeGreen
                            )
                        }
                        Text(
                            text = "Đang sẵn sàng",
                            fontSize = 10.5.sp,
                            color = OnSurfaceVariantLight,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFCBD5E1))
                    )

                    // Metric 2: Thời gian có mặt trung bình
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Timer,
                                contentDescription = null,
                                tint = RescuePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "~15 phút",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = RescuePrimary
                            )
                        }
                        Text(
                            text = "Tốc độ tiếp cận",
                            fontSize = 10.5.sp,
                            color = OnSurfaceVariantLight,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color(0xFFCBD5E1))
                    )

                    // Metric 3: Lực lượng trực ban
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                tint = EmergencyGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$totalTechnicians xe",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = "Đang túc trực",
                            fontSize = 10.5.sp,
                            color = OnSurfaceVariantLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Optional Staff Control Banner (Toggle switch simulation)
            AnimatedVisibility(visible = showStaffControls) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Chế độ điều phối: Bạn có thể bật/tắt trạng thái Sẵn sàng của từng dịch vụ bên dưới để kiểm tra đồng bộ Firebase Realtime.",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // 3. CATEGORY FILTER CHIPS
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "ALL" to "Tất cả dịch vụ (${services.size})",
                    "TOWING" to "Cẩu kéo xe",
                    "BATTERY" to "Kích bình",
                    "TIRE" to "Vá lốp",
                    "OTHER" to "Khác & Tiếp xăng"
                )
                items(chips) { (id, label) ->
                    val isSelected = selectedCategoryFilter == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = id },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RescuePrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) RescuePrimary else BorderLight
                        ),
                        modifier = Modifier.testTag("filter_chip_$id")
                    )
                }
            }

            // 4. MAIN SERVICE CARDS LIST (Featuring Towing, Jump-start, Tire change prominently)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (filteredServices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Không tìm thấy dịch vụ nào trong danh mục này",
                                fontSize = 12.5.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                } else {
                    filteredServices.forEach { item ->
                        ServiceAvailabilityCard(
                            item = item,
                            showStaffToggle = showStaffControls,
                            onToggle = { isAvail ->
                                onToggleAvailability?.invoke(item.id, isAvail)
                            },
                            onCardClick = {
                                detailDialogItem = item
                            },
                            onRequestClick = {
                                onRequestService(item)
                            },
                            onCallHotline = {
                                onCallEmergencyHotline(AppConfig.DEFAULT_RESCUE_HOTLINE)
                            }
                        )
                    }
                }
            }
        }
    }

    // 5. DETAIL MODAL DIALOG
    detailDialogItem?.let { item ->
        ServiceDetailDialog(
            item = item,
            onDismiss = { detailDialogItem = null },
            onRequestService = {
                detailDialogItem = null
                onRequestService(item)
            },
            onCallHotline = {
                detailDialogItem = null
                onCallEmergencyHotline(AppConfig.DEFAULT_RESCUE_HOTLINE)
            }
        )
    }
}

/**
 * Thẻ hiển thị từng dịch vụ cứu hộ kèm icon chuyên nghiệp, badge trạng thái trực quan và nút hành động.
 */
@Composable
fun ServiceAvailabilityCard(
    item: ServiceAvailabilityItem,
    showStaffToggle: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    onCardClick: () -> Unit = {},
    onRequestClick: () -> Unit = {},
    onCallHotline: () -> Unit = {}
) {
    val icon = ServiceVisualTheme.getIcon(item.iconKey)
    val bgColor = ServiceVisualTheme.getBgColor(item.iconKey)
    val accentColor = ServiceVisualTheme.getAccentColor(item.iconKey)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("service_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (item.isAvailable) Color.White else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.2.dp,
            if (item.isAvailable) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
        ),
        shadowElevation = if (item.isAvailable) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Title + Real-time Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Service Icon in decorative shape
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = item.name,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.priceDisplay,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = RescuePrimary
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = OnSurfaceVariantLight
                            )
                            Text(
                                text = item.operatingHours,
                                fontSize = 11.sp,
                                color = OnSurfaceVariantLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Live Availability Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (item.isAvailable) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = BorderStroke(
                        1.dp,
                        if (item.isAvailable) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (item.isAvailable) SafeGreen else AlertRed)
                        )
                        Text(
                            text = if (item.isAvailable) "SẴN SÀNG" else "TẠM BẬN",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = if (item.isAvailable) SafeGreen else AlertRed
                        )
                    }
                }
            }

            // Row 2: Short description & key specs
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Dispatch Indicators (ETA & Active Technicians)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = OnSurfaceLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Có mặt ~${item.etaMinutes}p",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceLight
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Engineering,
                                contentDescription = null,
                                tint = OnSurfaceLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${item.activeTechnicians} xe trực",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceLight
                            )
                        }
                    }
                }

                // Action button: Đặt cứu hộ ngay
                Button(
                    onClick = onRequestClick,
                    enabled = item.isAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RescuePrimary,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_request_${item.id}")
                ) {
                    Text(
                        text = if (item.isAvailable) "ĐẶT CỨU HỘ" else "TẠM BẬN",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Staff Toggle Switch (Shown only when staff controls enabled)
            if (showStaffToggle) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = RescuePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Ghi đè trạng thái trực ban (Firebase):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight
                        )
                    }

                    Switch(
                        checked = item.isAvailable,
                        onCheckedChange = { onToggle(it) },
                        modifier = Modifier.scale(0.8f).testTag("switch_${item.id}")
                    )
                }
            }
        }
    }
}

/**
 * Hộp thoại chi tiết dịch vụ khi người dùng chạm vào thẻ.
 */
@Composable
fun ServiceDetailDialog(
    item: ServiceAvailabilityItem,
    onDismiss: () -> Unit,
    onRequestService: () -> Unit,
    onCallHotline: () -> Unit
) {
    val icon = ServiceVisualTheme.getIcon(item.iconKey)
    val bgColor = ServiceVisualTheme.getBgColor(item.iconKey)
    val accentColor = ServiceVisualTheme.getAccentColor(item.iconKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = "Trạng thái Firebase: ${if (item.isAvailable) "SẴN SÀNG" else "TẠM BẬN"}",
                        fontSize = 11.sp,
                        color = if (item.isAvailable) SafeGreen else AlertRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = item.description,
                    fontSize = 12.5.sp,
                    color = OnSurfaceLight,
                    lineHeight = 18.sp
                )

                // Chi tiết bảng giá & cam kết
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Giá niêm yết:", fontSize = 12.sp, color = OnSurfaceVariantLight)
                            Text(item.priceDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RescuePrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thời gian tiếp cận:", fontSize = 12.sp, color = OnSurfaceVariantLight)
                            Text("~${item.etaMinutes} phút", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Xe & KTV ứng trực:", fontSize = 12.sp, color = OnSurfaceVariantLight)
                            Text("${item.activeTechnicians} phương tiện", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurfaceLight)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Phạm vi hỗ trợ:", fontSize = 12.sp, color = OnSurfaceVariantLight)
                            Text(item.operatingHours, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        }
                    }
                }

                // Ghi chú cam kết an toàn
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Trang thiết bị đạt chuẩn an toàn giao thông, bảo hiểm đầy đủ.",
                        fontSize = 11.sp,
                        color = SafeGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRequestService,
                enabled = item.isAvailable,
                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Đặt cứu hộ ngay")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCallHotline,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Gọi hotline", color = AlertRed)
            }
        }
    )
}
