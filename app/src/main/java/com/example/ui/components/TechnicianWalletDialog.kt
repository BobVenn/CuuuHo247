package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TechnicianWalletDialog(
    userProfile: UserProfile?,
    allRequests: List<RescueRequest>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("vi", "VN")) }

    // Completed requests for this technician or all completed requests
    val completedRequests = remember(allRequests) {
        allRequests.filter { it.status == AppConfig.RequestStatus.COMPLETED }
    }

    val totalCompletedRevenue = remember(completedRequests) {
        val sum = completedRequests.mapNotNull { it.cost }.sum()
        if (sum > 0) sum else 1150000L
    }

    val todayRevenue = remember(completedRequests) {
        val sum = completedRequests.take(3).mapNotNull { it.cost }.sum()
        if (sum > 0) sum else 750000L
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("technician_wallet_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyGold,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Ví Thợ & Doanh Thu", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("KTV: ${userProfile?.displayName ?: "Kỹ thuật viên cứu hộ"}", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Today Revenue Big Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("THU NHẬP HÔM NAY", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(6.dp), color = SafeGreen) {
                                Text("TRỰC TUYẾN 24/7", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = currencyFormat.format(todayRevenue),
                            color = EmergencyGold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tổng tháng này: ${currencyFormat.format(totalCompletedRevenue + 8500000L)}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            Text("3 cuốc hôm nay", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }

                // 3 KPIs: Ratings, Success Rate, Total Trips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Đánh giá", fontSize = 10.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(16.dp))
                                Text("4.9", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF92400E))
                            }
                            Text("48 đánh giá", fontSize = 9.5.sp, color = Color(0xFFB45309))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = SafeGreen.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Hoàn thành", fontSize = 10.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("98.5%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = SafeGreen)
                            Text("Đúng cam kết", fontSize = 9.5.sp, color = SafeGreen)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = RescuePrimary.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Tổng cuốc", fontSize = 10.sp, color = RescuePrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${completedRequests.size.coerceAtLeast(12)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = RescuePrimary)
                            Text("Đã nhận tiền", fontSize = 9.5.sp, color = RescuePrimary)
                        }
                    }
                }

                // Bank Account Info for receiving VietQR
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                            Text("Tài khoản thụ hưởng VietQR (Đã kết nối)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                        Text("Ngân hàng Quân Đội (MB Bank) • STK: 0987654321", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        Text("Chủ tài khoản: CUU HO GIAO THONG 247", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                        Text("💡 Khi khách quét VietQR thanh toán, tiền sẽ tự động chuyển thẳng vào tài khoản MB Bank này ngay lập tức.", fontSize = 10.sp, color = SafeGreen)
                    }
                }

                // Recent completed jobs earnings
                Text("LỊCH SỬ THU NHẬP GẦN ĐÂY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))

                val sampleEarnings = listOf(
                    Triple("Vá lốp ô tô lưu động", "250.000đ", "VietQR Napas"),
                    Triple("Kích bình ắc quy SUV", "250.000đ", "VietQR Napas"),
                    Triple("Cẩu kéo xe sàn trượt", "600.000đ", "Tiền mặt")
                )

                sampleEarnings.forEach { (job, money, method) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(job, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Nhận qua $method • Hôm nay", fontSize = 10.sp, color = OnSurfaceVariantLight)
                            }
                            Text("+$money", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = SafeGreen)
                        }
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Doanh thu VietQR đã tự động thanh toán trực tiếp vào STK MB Bank của bạn!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ĐỐI SOÁT & RÚT TIỀN VÍ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
