package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entity.SavedVehicleEntity
import com.example.data.model.RescueDataCatalog
import com.example.data.model.RescueServiceItem
import com.example.ui.components.RequestRescueDialog
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.RescueSecondary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    currentAddress: String,
    savedVehicles: List<SavedVehicleEntity>,
    onSubmitRequest: (
        vehicleType: String,
        serviceType: String,
        licensePlate: String,
        contactName: String,
        contactPhone: String,
        description: String,
        estimatedCost: Long
    ) -> Unit,
    onNavigateToTracking: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedServiceForDialog by remember { mutableStateOf<RescueServiceItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Quick Tow Estimator state
    var selectedTowVehicleType by remember { mutableStateOf("Ô tô con 4-7 chỗ") }
    var towDistanceKm by remember { mutableStateOf(10f) }

    val towBaseFee = when (selectedTowVehicleType) {
        "Xe máy" -> 150000L
        "Ô tô con 4-7 chỗ" -> 600000L
        "Xe SUV / Bán tải" -> 700000L
        else -> 900000L
    }
    val towPerKmRate = when (selectedTowVehicleType) {
        "Xe máy" -> 15000L
        "Ô tô con 4-7 chỗ" -> 20000L
        "Xe SUV / Bán tải" -> 25000L
        else -> 30000L
    }
    val estimatedTowTotal = towBaseFee + (towDistanceKm.toLong() * towPerKmRate)
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val filteredServices = remember(selectedCategory) {
        when (selectedCategory) {
            "CAR" -> RescueDataCatalog.services.filter { it.category == "CAR" || it.category == "ALL" }
            "MOTORBIKE" -> RescueDataCatalog.services.filter { it.category == "MOTORBIKE" || it.category == "ALL" }
            else -> RescueDataCatalog.services
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bảng Giá Dịch Vụ Cứu Hộ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Niêm yết minh bạch • Không phí ẩn",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            // Category Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == "ALL",
                        onClick = { selectedCategory = "ALL" },
                        label = { Text("Tất cả (8)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == "CAR",
                        onClick = { selectedCategory = "CAR" },
                        label = { Text("Ô tô / SUV") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedCategory == "MOTORBIKE",
                        onClick = { selectedCategory = "MOTORBIKE" },
                        label = { Text("Xe máy") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Tow Calculator Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tow_calculator_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = RescuePrimary)
                            Text(
                                text = "Công Cụ Tính Cước Kéo Xe Cứu Hộ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Vehicle type choices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Xe máy", "Ô tô con 4-7 chỗ", "Xe SUV / Bán tải").forEach { type ->
                                val isSelected = selectedTowVehicleType == type
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) RescuePrimary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTowVehicleType = type }
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Distance Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Khoảng cách kéo xe:")
                                Text(
                                    text = "${towDistanceKm.toInt()} km",
                                    fontWeight = FontWeight.Bold,
                                    color = RescuePrimary
                                )
                            }
                            Slider(
                                value = towDistanceKm,
                                onValueChange = { towDistanceKm = it },
                                valueRange = 2f..80f,
                                steps = 38
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tổng cước dự kiến:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatter.format(estimatedTowTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = RescuePrimary
                                )
                            }

                            Button(
                                onClick = {
                                    val item = RescueDataCatalog.services.find { it.id == "TOW_TRUCK" }
                                    selectedServiceForDialog = item
                                    showDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Đặt xe kéo")
                            }
                        }
                    }
                }
            }

            // Services List
            items(filteredServices) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedServiceForDialog = service
                            showDialog = true
                        }
                        .testTag("service_item_${service.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = RescuePrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (service.id) {
                                                "BATTERY_JUMP" -> Icons.Default.Bolt
                                                "TIRE_PUNCTURE" -> Icons.Default.Settings
                                                "TOW_TRUCK" -> Icons.Default.LocalShipping
                                                "FUEL_DELIVERY" -> Icons.Default.LocalGasStation
                                                "UNLOCK_DOOR" -> Icons.Default.Key
                                                "FLOOD_RESCUE" -> Icons.Default.Water
                                                "MOTO_PUNCTURE" -> Icons.Default.TwoWheeler
                                                else -> Icons.Default.Build
                                            },
                                            contentDescription = null,
                                            tint = RescuePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = service.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = service.priceDisplay,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RescuePrimary
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedServiceForDialog = service
                                    showDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Gọi cứu hộ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = service.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        RequestRescueDialog(
            initialService = selectedServiceForDialog,
            savedVehicles = savedVehicles,
            currentAddress = currentAddress,
            onDismiss = { showDialog = false },
            onSubmit = { vehicleType, serviceType, plate, name, phone, desc, cost ->
                showDialog = false
                onSubmitRequest(vehicleType, serviceType, plate, name, phone, desc, cost)
                onNavigateToTracking()
            }
        )
    }
}
