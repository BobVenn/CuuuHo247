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
import com.example.data.config.AppConfig
import com.example.data.model.UserProfile
import com.example.ui.theme.RescuePrimary
import com.example.util.UserLocationInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestDialog(
    initialIssueType: String? = null,
    userProfile: UserProfile,
    currentLocation: UserLocationInfo,
    onDismiss: () -> Unit,
    onSubmit: (
        issueType: String,
        description: String,
        vehicleType: String,
        licensePlate: String
    ) -> Unit
) {
    var selectedIssue by remember {
        mutableStateOf(initialIssueType ?: AppConfig.ISSUE_TYPES[1])
    }
    var vehicleType by remember {
        mutableStateOf(userProfile.vehicleType.ifBlank { AppConfig.VEHICLE_TYPES[0] })
    }
    var licensePlate by remember {
        mutableStateOf(userProfile.licensePlate)
    }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("create_request_dialog"),
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
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Gửi trực tiếp lên hệ thống Firebase 24/7",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider()

                // Location Banner (Real Device GPS)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary)
                        Column {
                            Text(
                                text = "Vị trí cứu hộ (GPS hiện tại)",
                                style = MaterialTheme.typography.labelSmall,
                                color = RescuePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentLocation.address,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Issue Types
                Text(
                    text = "Chọn loại sự cố gặp phải:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppConfig.ISSUE_TYPES.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { issue ->
                                val isSelected = selectedIssue == issue
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedIssue = issue },
                                    label = { Text(issue, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = {
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Vehicle Type selector
                Text(
                    text = "Loại phương tiện:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConfig.VEHICLE_TYPES.take(2).forEach { type ->
                        FilterChip(
                            selected = vehicleType == type,
                            onClick = { vehicleType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppConfig.VEHICLE_TYPES.drop(2).forEach { type ->
                        FilterChip(
                            selected = vehicleType == type,
                            onClick = { vehicleType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // License Plate
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("Biển số xe *") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_request_license_plate"),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả chi tiết sự cố (Tùy chọn)") },
                    placeholder = { Text("Xe chết máy trong hầm, bánh xẹp hoàn toàn...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Submit Button
                Button(
                    onClick = {
                        onSubmit(
                            selectedIssue,
                            description.ifBlank { "Yêu cầu cứu hộ sự cố: $selectedIssue" },
                            vehicleType,
                            licensePlate.ifBlank { userProfile.licensePlate }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_submit_request"),
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GỬI YÊU CẦU CỨU HỘ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
