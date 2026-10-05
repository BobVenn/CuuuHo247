package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

data class PriceItem(
    val serviceName: String,
    val priceRange: String,
    val description: String,
    val icon: ImageVector
)

data class SafetyTip(
    val title: String,
    val steps: List<String>,
    val warning: String,
    val icon: ImageVector,
    val badgeColor: Color
)

@Composable
fun EmergencyHandbookDialog(
    onDismiss: () -> Unit,
    onDirectCallSos: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bảng giá, 1: Cẩm nang an toàn

    val priceList = remember {
        listOf(
            PriceItem("Vá lốp ô tô lưu động", "100.000đ - 180.000đ", "Vá nấm, vá dùi, bơm hơi lốp tận nơi", Icons.Default.TireRepair),
            PriceItem("Vá lốp xe máy / Xe điện", "60.000đ - 120.000đ", "Vá săm, vá lốp không săm lưu động", Icons.Default.TwoWheeler),
            PriceItem("Kích nổ bình ắc quy ô tô", "150.000đ - 250.000đ", "Kích bình 12V/24V, kiểm tra máy phát điện", Icons.Default.BatteryChargingFull),
            PriceItem("Thay ắc quy chính hãng mới", "850.000đ - 2.500.000đ", "Ắc quy GS, Varta, Đồng Nai bảo hành 12 tháng", Icons.Default.ElectricalServices),
            PriceItem("Tiếp xăng dầu khẩn cấp", "Tiền xăng + 50.000đ ship", "Mang tối thiểu 5L xăng/dầu tận vị trí chết máy", Icons.Default.LocalGasStation),
            PriceItem("Cứu hộ xe kéo sàn trượt (4-7 chỗ)", "500.000đ mở cửa + 20k/km", "Cẩu kéo về gara chỉ định hoặc hãng", Icons.Default.LocalShipping),
            PriceItem("Cứu hộ xe tải / Xe khách", "800.000đ - 1.800.000đ", "Xe cứu hộ 3 chân kéo xe trọng tải lớn", Icons.Default.DirectionsBus),
            PriceItem("Rút xăng nhầm dầu / Xử lý bình", "350.000đ - 600.000đ", "Rút cạn bình, súc rửa đường ống nhiên liệu", Icons.Default.CleaningServices)
        )
    }

    val safetyTips = remember {
        listOf(
            SafetyTip(
                title = "Nổ lốp trên đường cao tốc",
                steps = listOf(
                    "Giữ thật chặt vô lăng bằng 2 tay theo hướng thẳng",
                    "TUYỆT ĐỐI KHÔNG đạp phanh gấp (dễ lật xe)",
                    "Nhả bàn đạp ga từ từ để xe tự hãm tốc độ",
                    "Bật đèn báo nguy hiểm (Hazard) và quan sát gương",
                    "Đánh lái từ từ vào làn dừng khẩn cấp bên phải đường"
                ),
                warning = "Sau khi dừng xe, tất cả mọi người bước ra khỏi xe và đứng sau dải hộ lan an toàn!",
                icon = Icons.Default.WarningAmber,
                badgeColor = AlertRed
            ),
            SafetyTip(
                title = "Đặt biển tam giác phản quang",
                steps = listOf(
                    "Bật đèn Hazard khẩn cấp ngay khi xe dừng",
                    "Lấy tam giác phản quang và áo phản quang trong cốp",
                    "Đường đô thị: Đặt cách đuôi xe 30m - 50m",
                    "Đường quốc lộ: Đặt cách đuôi xe 80m - 100m",
                    "Đường cao tốc: Đặt cách đuôi xe tối thiểu 150m - 200m"
                ),
                warning = "Không đứng ở phía sau đuôi xe khi chưa đặt cọc tiêu cảnh báo từ xa!",
                icon = Icons.Default.ReportProblem,
                badgeColor = EmergencyGold
            ),
            SafetyTip(
                title = "Xe quá nhiệt / Sôi nước làm mát",
                steps = listOf(
                    "Tắt điều hòa xe (A/C) ngay lập tức",
                    "Tấp xe vào lề an toàn và về số P (hoặc Mo), kéo phanh tay",
                    "Mở nắp ca-pô nhưng TUYỆT ĐỐI KHÔNG MỞ NẮP KÉT NƯỚC",
                    "Để máy nổ không tải hoặc tắt máy đợi nguội ít nhất 30 phút",
                    "Chỉ mở nắp bình nước phụ khi động cơ đã hoàn toàn nguội"
                ),
                warning = "Nước sôi áp lực cao trong két nước có thể bắn vào mặt gây bỏng cực kỳ nguy hiểm!",
                icon = Icons.Default.Thermostat,
                badgeColor = Color(0xFFEA580C)
            ),
            SafetyTip(
                title = "Xe chết máy khi đi vào vùng ngập nước",
                steps = listOf(
                    "TUYỆT ĐỐI KHÔNG cố gắng vặn chìa để đề máy lại",
                    "Tắt hoàn toàn khóa điện xe để ngắt nguồn điện",
                    "Tháo cọc âm (-) bình ắc quy nếu có thể",
                    "Chụp ảnh vị trí mực nước ngập đến đâu trên thân xe",
                    "Gọi xe cứu hộ sàn trượt kéo về gara kiểm tra buồng đốt"
                ),
                warning = "Đề máy khi nước đã lọt vào cổ hút sẽ gây thủy kích làm cong tay biên, vỡ lốc máy hàng trăm triệu!",
                icon = Icons.Default.WaterDrop,
                badgeColor = Color(0xFF0284C7)
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("emergency_handbook_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = RescuePrimary, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Bảng Giá & Cẩm Nang", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Minh bạch chi phí • Xử lý khẩn cấp", fontSize = 11.sp, color = OnSurfaceVariantLight)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = if (selectedTab == 0) RescuePrimary else OnSurfaceVariantLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bảng Giá Niêm Yết", fontSize = 11.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium, color = if (selectedTab == 0) RescuePrimary else OnSurfaceVariantLight)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) Color.White else Color.Transparent,
                        shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = if (selectedTab == 1) AlertRed else OnSurfaceVariantLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cẩm Nang An Toàn", fontSize = 11.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium, color = if (selectedTab == 1) AlertRed else OnSurfaceVariantLight)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                                Text("Cam kết giá đúng niêm yết, không chặt chém, có hóa đơn VAT khi yêu cầu.", fontSize = 11.sp, color = Color(0xFF166534))
                            }
                        }

                        priceList.forEach { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDirectCallSos() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(shape = CircleShape, color = CategoryOrangeBg, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(item.icon, contentDescription = null, tint = CategoryOrangeTint, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.serviceName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(item.description, fontSize = 11.sp, color = OnSurfaceVariantLight)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = item.priceRange,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = RescuePrimary
                                        )
                                        Text("Bấm để gọi", fontSize = 9.5.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        safetyTips.forEach { tip ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(tip.icon, contentDescription = null, tint = tip.badgeColor, modifier = Modifier.size(22.dp))
                                        Text(tip.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = tip.badgeColor)
                                    }

                                    tip.steps.forEachIndexed { idx, step ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = RescuePrimary)
                                            Text(step, fontSize = 11.5.sp, color = OnSurfaceLight, lineHeight = 15.sp)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = AlertRed, modifier = Modifier.size(16.dp))
                                            Text(tip.warning, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AlertRed, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Call SOS Button
                Button(
                    onClick = onDirectCallSos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GỌI TỔNG ĐÀI CỨU HỘ KHẨN CẤP 24/7", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
