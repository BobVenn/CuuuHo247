package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.UserProfile
import com.example.ui.components.CreateRequestDialog
import com.example.ui.components.RescueOpenStreetMap
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen
import com.example.util.UserLocationInfo
import java.text.NumberFormat
import java.util.Locale

data class StreamlinedServiceItem(
    val id: String,
    val title: String,
    val category: String, // "CAR", "MOTORBIKE", "TOW"
    val priceDisplay: String,
    val estimatedPrice: Long,
    val icon: ImageVector,
    val actionText: String = "Đặt ngay"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    userProfile: UserProfile?,
    currentLocation: UserLocationInfo,
    onCallPhone: (String) -> Unit,
    onSubmitRequest: (issueType: String, description: String, vehicleType: String, licensePlate: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedServiceForDialog by remember { mutableStateOf<StreamlinedServiceItem?>(null) }

    // Streamlined service catalog without wordy explanations
    val services = remember {
        listOf(
            StreamlinedServiceItem(
                id = "BATTERY_JUMP",
                title = "Kích bình ắc quy",
                category = "CAR",
                priceDisplay = "250.000đ",
                estimatedPrice = 250000,
                icon = Icons.Default.Bolt,
                actionText = "Gọi cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "TIRE_PUNCTURE",
                title = "Vá lốp & Thay bánh sơ cua",
                category = "CAR",
                priceDisplay = "250.000đ",
                estimatedPrice = 250000,
                icon = Icons.Default.Settings,
                actionText = "Gọi cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "TOW_TRUCK",
                title = "Cẩu kéo xe sàn trượt",
                category = "TOW",
                priceDisplay = "600.000đ",
                estimatedPrice = 600000,
                icon = Icons.Default.LocalShipping,
                actionText = "Đặt xe"
            ),
            StreamlinedServiceItem(
                id = "FUEL_DELIVERY",
                title = "Tiếp xăng khẩn cấp",
                category = "CAR",
                priceDisplay = "150.000đ",
                estimatedPrice = 150000,
                icon = Icons.Default.LocalGasStation,
                actionText = "Gọi cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "UNLOCK_DOOR",
                title = "Mở khóa xe quên chìa",
                category = "CAR",
                priceDisplay = "300.000đ",
                estimatedPrice = 300000,
                icon = Icons.Default.Key,
                actionText = "Gọi cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "FLOOD_RESCUE",
                title = "Cứu hộ ngập nước",
                category = "TOW",
                priceDisplay = "700.000đ",
                estimatedPrice = 700000,
                icon = Icons.Default.WaterDamage,
                actionText = "Đặt xe"
            ),
            StreamlinedServiceItem(
                id = "MOTO_PUNCTURE",
                title = "Vá lốp xe máy đêm",
                category = "MOTORBIKE",
                priceDisplay = "70.000đ",
                estimatedPrice = 70000,
                icon = Icons.Default.TwoWheeler,
                actionText = "Đặt ngay"
            ),
            StreamlinedServiceItem(
                id = "MOTO_TOW",
                title = "Chở xe máy về trạm",
                category = "MOTORBIKE",
                priceDisplay = "180.000đ",
                estimatedPrice = 180000,
                icon = Icons.Default.ElectricMoped,
                actionText = "Đặt xe"
            )
        )
    }

    val filteredServices = remember(selectedCategory) {
        when (selectedCategory) {
            "CAR" -> services.filter { it.category == "CAR" }
            "MOTORBIKE" -> services.filter { it.category == "MOTORBIKE" }
            "TOW" -> services.filter { it.category == "TOW" }
            else -> services
        }
    }

    // Tow distance calculator state
    var towDistanceKm by remember { mutableFloatStateOf(10f) }
    val towEstimatedPrice = 600000L + (towDistanceKm.toLong() * 20000L)
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dịch Vụ Cứu Hộ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bảng giá niêm yết • 1 Chạm đặt ngay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 95.dp)
        ) {
            // 1. OpenStreetMap & Leaflet.js Location & Rescue Markers View (100% Free, No API Key)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .testTag("leaflet_osm_map_card"),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    RescueOpenStreetMap(
                        userLat = currentLocation.latitude,
                        userLng = currentLocation.longitude,
                        userAddress = currentLocation.address,
                        onCallPhone = onCallPhone,
                        onRequestRescueAtLocation = {
                            selectedServiceForDialog = services.find { it.id == "TOW_TRUCK" }
                            showCreateDialog = true
                        }
                    )
                }
            }

            // 2. Streamlined Category Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("Tất cả", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == "CAR",
                        onClick = { selectedCategory = "CAR" },
                        label = { Text("Ô tô", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == "MOTORBIKE",
                        onClick = { selectedCategory = "MOTORBIKE" },
                        label = { Text("Xe máy", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == "TOW",
                        onClick = { selectedCategory = "TOW" },
                        label = { Text("Kéo xe", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Compact Tow Fare Estimator
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compact_tow_calculator"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                Text("Cước cẩu kéo dự tính: ${towDistanceKm.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = currencyFormat.format(towEstimatedPrice),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = RescuePrimary
                            )
                        }

                        Slider(
                            value = towDistanceKm,
                            onValueChange = { towDistanceKm = it },
                            valueRange = 2f..60f,
                            steps = 28,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // 4. Streamlined Services List (No Long Descriptions, Generous Padding, Sharp Icons, One-Tap Buttons)
            items(filteredServices) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedServiceForDialog = service
                            showCreateDialog = true
                        }
                        .testTag("service_item_${service.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Icon + Title & Price
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RescuePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = service.icon,
                                        contentDescription = null,
                                        tint = RescuePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = service.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = service.priceDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = RescuePrimary
                                )
                            }
                        }

                        // One-Tap Action Button
                        Button(
                            onClick = {
                                selectedServiceForDialog = service
                                showCreateDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_order_${service.id}")
                        ) {
                            Text(
                                text = service.actionText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Booking Dialog prefilled with selected service
    if (showCreateDialog && userProfile != null) {
        CreateRequestDialog(
            initialIssueType = when (selectedServiceForDialog?.id) {
                "BATTERY_JUMP" -> "Hết bình"
                "TIRE_PUNCTURE", "MOTO_PUNCTURE" -> "Thủng lốp"
                "FUEL_DELIVERY" -> "Xe hết xăng"
                "UNLOCK_DOOR" -> "Khóa xe"
                "FLOOD_RESCUE", "TOW_TRUCK", "MOTO_TOW" -> "Hỏng xe"
                else -> AppConfig.ISSUE_TYPES[1]
            },
            userProfile = userProfile,
            currentLocation = currentLocation,
            onDismiss = { showCreateDialog = false },
            onSubmit = { issue, desc, vType, plate ->
                showCreateDialog = false
                onSubmitRequest(issue, desc, vType, plate)
            }
        )
    }
}
