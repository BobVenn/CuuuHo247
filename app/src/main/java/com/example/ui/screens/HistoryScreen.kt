package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    requests: List<RescueRequest>,
    onRequestClick: (RescueRequest) -> Unit,
    onSeedSampleRequest: (() -> Unit)? = null
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val dateFormat = remember { SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale("vi", "VN")) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val filteredList = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "ACTIVE" -> requests.filter { it.status != AppConfig.RequestStatus.COMPLETED && it.status != AppConfig.RequestStatus.CANCELLED }
            "COMPLETED" -> requests.filter { it.status == AppConfig.RequestStatus.COMPLETED }
            "CANCELLED" -> requests.filter { it.status == AppConfig.RequestStatus.CANCELLED }
            else -> requests
        }
    }

    val totalCompleted = remember(requests) { requests.count { it.status == AppConfig.RequestStatus.COMPLETED } }
    val totalCost = remember(requests) { requests.mapNotNull { it.cost }.sum() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lịch Sử Cứu Hộ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${requests.size} yêu cầu được đồng bộ từ Firebase Realtime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (onSeedSampleRequest != null) {
                        TextButton(
                            onClick = onSeedSampleRequest,
                            modifier = Modifier.testTag("btn_seed_sample_history")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm Mẫu", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            // SUMMARY STATS CARD
            if (requests.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("history_stats_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng Yêu Cầu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${requests.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = RescuePrimary)
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = BorderLight)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Đã Hoàn Thành", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalCompleted", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SafeGreen)
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = BorderLight)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng Chi Phí", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (totalCost > 0) currencyFormat.format(totalCost) else "0 ₫",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // FILTER CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Tất cả (${requests.size})", fontSize = 11.5.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Đang xử lý", fontSize = 11.5.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Hoàn thành ($totalCompleted)", fontSize = 11.5.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "CANCELLED",
                    onClick = { selectedFilter = "CANCELLED" },
                    label = { Text("Đã hủy", fontSize = 11.5.sp) }
                )
            }

            if (filteredList.isEmpty()) {
                // Empty State with informative description and Sample creation button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "Chưa có yêu cầu cứu hộ nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toàn bộ hồ sơ cứu hộ quá khứ bao gồm Ngày giờ, Dịch vụ, Chi phí thực tế và Nhận xét chẩn đoán từ Kỹ thuật viên sẽ được lưu trữ tự động trên Firebase Realtime Database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (onSeedSampleRequest != null) {
                            Button(
                                onClick = onSeedSampleRequest,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag("btn_seed_empty_history"),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tạo Yêu Cầu Cứu Hộ Mẫu (Firebase)")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                ) {
                    items(filteredList, key = { it.id }) { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestClick(req) }
                                .testTag("history_item_${req.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. HEADER ROW: SERVICE TYPE & STATUS BADGE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when {
                                                req.issueType.contains("Kéo", ignoreCase = true) -> Color(0xFFFEF3C7)
                                                req.issueType.contains("bình", ignoreCase = true) -> Color(0xFFEFF6FF)
                                                else -> Color(0xFFF3F4F6)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when {
                                                        req.issueType.contains("Kéo", ignoreCase = true) -> Icons.Default.LocalShipping
                                                        req.issueType.contains("bình", ignoreCase = true) -> Icons.Default.ElectricBolt
                                                        else -> Icons.Default.Build
                                                    },
                                                    contentDescription = null,
                                                    tint = when {
                                                        req.issueType.contains("Kéo", ignoreCase = true) -> Color(0xFFB45309)
                                                        req.issueType.contains("bình", ignoreCase = true) -> RescuePrimary
                                                        else -> Color(0xFF4B5563)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = req.issueType,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${req.vehicleType} • ${req.licensePlate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // STATUS BADGE
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (req.status) {
                                            AppConfig.RequestStatus.COMPLETED -> SafeGreen.copy(alpha = 0.15f)
                                            AppConfig.RequestStatus.CANCELLED -> AlertRed.copy(alpha = 0.15f)
                                            else -> RescuePrimary.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = when (req.status) {
                                                AppConfig.RequestStatus.COMPLETED -> "Hoàn thành ✓"
                                                AppConfig.RequestStatus.CANCELLED -> "Đã hủy"
                                                AppConfig.RequestStatus.PENDING -> "Chờ xử lý"
                                                AppConfig.RequestStatus.ACCEPTED -> "Đã tiếp nhận"
                                                AppConfig.RequestStatus.EN_ROUTE -> "Đang đến"
                                                AppConfig.RequestStatus.ARRIVED -> "Đã đến nơi"
                                                AppConfig.RequestStatus.IN_PROGRESS -> "Đang sửa chữa"
                                                else -> req.status
                                            },
                                            color = when (req.status) {
                                                AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                                AppConfig.RequestStatus.CANCELLED -> AlertRed
                                                else -> RescuePrimary
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 2. LOCATION ADDRESS
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = req.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 3. DATE & TIME + COST & PAYMENT
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = dateFormat.format(Date(req.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // COST & PAYMENT BADGE
                                    if (req.cost != null && req.cost > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = currencyFormat.format(req.cost),
                                                fontWeight = FontWeight.Black,
                                                color = SafeGreen,
                                                fontSize = 13.5.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (req.paymentStatus.startsWith("PAID")) SafeGreen.copy(alpha = 0.15f) else Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = when (req.paymentStatus) {
                                                        "PAID_VIETQR" -> "VietQR ✓"
                                                        "PAID_CASH" -> "Tiền mặt ✓"
                                                        else -> "Chưa TT"
                                                    },
                                                    color = if (req.paymentStatus.startsWith("PAID")) SafeGreen else Color(0xFFB45309),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. TECHNICIAN FEEDBACK SECTION (Crucial requirement stored in Firebase)
                                if (!req.technicianFeedback.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = SafeGreen,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "Nhận xét KTV (${req.staffName ?: "KTV Cứu Hộ"}):",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                            Text(
                                                text = "\"${req.technicianFeedback}\"",
                                                fontSize = 11.5.sp,
                                                lineHeight = 16.5.sp,
                                                color = Color(0xFF14532D)
                                            )
                                        }
                                    }
                                } else if (req.status == AppConfig.RequestStatus.COMPLETED && !req.staffName.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF9FAFB),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("👨‍🔧", fontSize = 12.sp)
                                            Text(
                                                text = "KTV: ${req.staffName} • Nghiệm thu bàn giao an toàn",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF4B5563)
                                            )
                                        }
                                    }
                                }

                                // 5. CUSTOMER RATING DISPLAY
                                if (req.rating != null && req.rating > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        repeat(req.rating) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = EmergencyGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        if (!req.ratingComment.isNullOrBlank()) {
                                            Text(
                                                text = "\"${req.ratingComment}\"",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // 6. FOOTER: DETAILS NAVIGATION AFFORDANCE
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Xem chi tiết & lộ trình cứu hộ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RescuePrimary
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = RescuePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
