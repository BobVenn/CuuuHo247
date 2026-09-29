package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.SavedVehicleEntity
import com.example.data.model.RescueDataCatalog
import com.example.data.model.RescueServiceItem
import com.example.ui.theme.RescuePrimary
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRescueDialog(
    initialService: RescueServiceItem? = null,
    savedVehicles: List<SavedVehicleEntity>,
    currentAddress: String,
    onDismiss: () -> Unit,
    onSubmit: (
        vehicleType: String,
        serviceType: String,
        licensePlate: String,
        contactName: String,
        contactPhone: String,
        description: String,
        estimatedCost: Long
    ) -> Unit
) {
    val vehicleTypes = listOf("Ô tô con (4-7 chỗ)", "Xe SUV / Bán tải", "Xe máy / Xe điện", "Xe tải nhẹ")
    val defaultVehicle = savedVehicles.find { it.isDefault } ?: savedVehicles.firstOrNull()

    var selectedVehicleType by remember {
        mutableStateOf(defaultVehicle?.vehicleType ?: vehicleTypes[0])
    }
    var selectedService by remember {
        mutableStateOf(initialService?.title ?: RescueDataCatalog.services[0].title)
    }
    var licensePlate by remember {
        mutableStateOf(defaultVehicle?.licensePlate ?: "30H - 688.99")
    }
    var contactName by remember { mutableStateOf("Chủ xe") }
    var contactPhone by remember { mutableStateOf("0987654321") }
    var description by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf(currentAddress) }

    val serviceItem = RescueDataCatalog.services.find { it.title == selectedService }
        ?: RescueDataCatalog.services[0]
    val estimatedCost = serviceItem.basePrice

    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("request_rescue_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Yêu Cầu Cứu Hộ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Đội cứu hộ phản ứng nhanh 24/7",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider()

                // Vehicle Type selector
                Text(
                    text = "Loại phương tiện gặp sự cố",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vehicleTypes.take(2).forEach { type ->
                        FilterChip(
                            selected = selectedVehicleType == type,
                            onClick = { selectedVehicleType = type },
                            label = { Text(type, maxLines = 1, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vehicleTypes.drop(2).forEach { type ->
                        FilterChip(
                            selected = selectedVehicleType == type,
                            onClick = { selectedVehicleType = type },
                            label = { Text(type, maxLines = 1, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Service Type
                Text(
                    text = "Dịch vụ cần hỗ trợ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        RescueDataCatalog.services.take(4).forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedService = s.title }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedService == s.title,
                                    onClick = { selectedService = s.title }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = s.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = s.priceDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RescuePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // License plate and phone
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("Biển số xe") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_license_plate"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Tên liên hệ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Số điện thoại") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_contact_phone"),
                        singleLine = true
                    )
                }

                // Location address
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Địa điểm gặp nạn / Điểm ghim") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_location_address"),
                    maxLines = 2
                )

                // Notes / Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả hiện trạng (Xe chết máy, thủng lốp, hầm sâu...)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )

                // Price Summary Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Phí dự kiến (Báo giá chuẩn)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Miễn phí công kiểm tra ban đầu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = formatter.format(estimatedCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RescuePrimary
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        onSubmit(
                            selectedVehicleType,
                            selectedService,
                            licensePlate,
                            contactName,
                            contactPhone,
                            description.ifBlank { "Yêu cầu cứu hộ cho xe $licensePlate tại $locationText" },
                            estimatedCost
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_rescue"),
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GỬI YÊU CẦU CỨU HỘ NGAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
