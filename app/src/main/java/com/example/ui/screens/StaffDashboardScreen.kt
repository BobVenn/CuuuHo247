package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    userProfile: UserProfile?,
    allRequests: List<RescueRequest>,
    onRequestClick: (RescueRequest) -> Unit,
    onAcceptRequest: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("PENDING") }
    val dateFormat = remember { SimpleDateFormat("HH:mm • dd/MM", Locale("vi", "VN")) }

    val filteredList = remember(allRequests, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> allRequests.filter { it.status == AppConfig.RequestStatus.PENDING }
            "IN_PROGRESS" -> allRequests.filter {
                it.status == AppConfig.RequestStatus.ACCEPTED ||
                it.status == AppConfig.RequestStatus.EN_ROUTE ||
                it.status == AppConfig.RequestStatus.ARRIVED ||
                it.status == AppConfig.RequestStatus.IN_PROGRESS
            }
            "COMPLETED" -> allRequests.filter { it.status == AppConfig.RequestStatus.COMPLETED }
            else -> allRequests
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bàn Điều Phối Cứu Hộ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${userProfile?.role ?: "Staff"}: ${userProfile?.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RescuePrimary
                        )
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
            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" },
                    label = { Text("Chờ nhận (${allRequests.count { it.status == AppConfig.RequestStatus.PENDING }})", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == "IN_PROGRESS",
                    onClick = { selectedFilter = "IN_PROGRESS" },
                    label = { Text("Đang làm", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Xong", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Tất cả", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (filteredList.isEmpty()) {
                // Real Empty State
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
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Không có đơn cứu hộ nào trong mục này",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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
                                .testTag("staff_request_item_${req.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
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
                                    Text(
                                        text = "${req.issueType} • ${req.userName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (req.status) {
                                            AppConfig.RequestStatus.PENDING -> AlertRed
                                            AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                            else -> RescuePrimary
                                        }
                                    ) {
                                        Text(
                                            text = req.status,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "🚗 ${req.vehicleType} • ${req.licensePlate}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "📍 ${req.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "🕒 ${dateFormat.format(Date(req.timestamp))} • SĐT: ${req.userPhone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (req.status == AppConfig.RequestStatus.PENDING) {
                                    Button(
                                        onClick = { onAcceptRequest(req.id) },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                                    ) {
                                        Text("TIẾP NHẬN ĐƠN NÀY", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
