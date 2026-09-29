package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
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
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    requests: List<RescueRequest>,
    onRequestClick: (RescueRequest) -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lịch Sử Cứu Hộ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${requests.size} yêu cầu được lưu trên Firebase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Tất cả (${requests.size})", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Đang xử lý", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Hoàn thành", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "CANCELLED",
                    onClick = { selectedFilter = "CANCELLED" },
                    label = { Text("Đã hủy", fontSize = 11.sp) }
                )
            }

            if (filteredList.isEmpty()) {
                // Real Empty State (No Mock Data)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "Không có yêu cầu cứu hộ nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Danh sách yêu cầu cứu hộ của bạn từ Firebase Realtime Database sẽ xuất hiện tại đây.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                ) {
                    items(filteredList) { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestClick(req) }
                                .testTag("history_item_${req.id}"),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(req.issueType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                        text = "${req.vehicleType} • Biển số: ${req.licensePlate}",
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(req.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (req.cost != null && req.cost > 0) {
                                            Text(
                                                text = currencyFormat.format(req.cost),
                                                fontWeight = FontWeight.Bold,
                                                color = SafeGreen,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
