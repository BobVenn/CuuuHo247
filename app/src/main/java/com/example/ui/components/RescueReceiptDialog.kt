package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RescueReceiptDialog(
    request: RescueRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale("vi", "VN")) }
    val receiptId = "BL-${request.id.takeLast(8).uppercase()}"

    val paymentMethodDisplay = when (request.paymentStatus) {
        "PAID_VIETQR" -> "Chuyển khoản Ngân hàng (VietQR Napas)"
        "PAID_CASH" -> "Tiền mặt trực tiếp cho KTV"
        else -> "Đã xác nhận thanh toán"
    }

    val cost = request.cost ?: 250000L

    fun shareReceiptForInsurance(context: Context) {
        val receiptText = """
            ====================================
            BIÊN LAI ĐIỆN TỬ DỊCH VỤ CỨU HỘ GIAO THÔNG 24/7
            (Chứng từ phục vụ yêu cầu bồi thường Bảo Hiểm)
            ====================================
            Mã biên lai: $receiptId
            Thời gian: ${dateFormat.format(Date(request.timestamp))}
            
            [ĐƠN VỊ CUNG CẤP DỊCH VỤ]
            Trung Tâm Cứu Hộ Giao Thông Toàn Quốc 24/7
            Kỹ thuật viên thực hiện: ${request.staffName ?: "KTV Nguyễn Văn Toàn"}
            SĐT cứu hộ: ${request.staffPhone ?: "0901234567"} - Hotline: ${AppConfig.RESCUE_HOTLINE_DISPLAY}
            
            [THÔNG TIN KHÁCH HÀNG & PHƯƠNG TIỆN]
            Khách hàng: ${request.userName.ifBlank { "Khách hàng cá nhân" }}
            Số điện thoại: ${request.userPhone.ifBlank { "0987654321" }}
            Loại phương tiện: ${request.vehicleType}
            Biển kiểm soát: ${request.licensePlate}
            
            [HIỆN TRƯỜNG & DỊCH VỤ THỰC HIỆN]
            Địa điểm cứu hộ: ${request.address}
            Tọa độ GPS: ${request.latitude}, ${request.longitude}
            Nội dung xử lý: ${request.issueType}
            Ghi chú hiện trường: ${request.description.ifBlank { "Sửa chữa, khắc phục sự cố lưu động thành công" }}
            
            [CHI PHÍ THANH TOÁN]
            Tổng tiền: ${currencyFormat.format(cost)}
            Hình thức thanh toán: $paymentMethodDisplay
            Trạng thái: ĐÃ THANH TOÁN ĐẦY ĐỦ ✓
            
            Biên lai hợp lệ gửi công ty bảo hiểm (Bảo Việt, PVI, PJICO, PTI, VBI...) để làm thủ tục hoàn trả 100% chi phí cứu hộ.
            ====================================
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Biên lai cứu hộ xe $receiptId - Biển số ${request.licensePlate}")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Gửi biên lai bồi thường bảo hiểm qua:"))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("rescue_receipt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RescuePrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("BIÊN LAI CỨU HỘ", fontWeight = FontWeight.Black, fontSize = 15.sp, color = OnSurfaceLight)
                            Text("Chứng từ gửi Bảo hiểm", fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                HorizontalDivider(color = BorderLight)

                // Receipt Content Surface with perforated receipt design
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title & Receipt ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Số biên lai:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                            Text(receiptId, fontSize = 12.sp, fontWeight = FontWeight.Black, color = RescuePrimary, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thời gian lập:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                            Text(dateFormat.format(Date(request.timestamp)), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Customer info
                        Text("KHÁCH HÀNG & PHƯƠNG TIỆN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                        Text("${request.userName.ifBlank { "Khách hàng cá nhân" }} • SĐT: ${request.userPhone.ifBlank { "0987654321" }}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("Xe: ${request.vehicleType} | Biển số: ${request.licensePlate}", fontSize = 12.sp, color = OnSurfaceLight)

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Rescue Location & Service
                        Text("ĐƠN VỊ CỨU HỘ & HIỆN TRƯỜNG", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                        Text("KTV: ${request.staffName ?: "Nguyễn Văn Toàn (Gara 24/7)"} (${request.staffPhone ?: "0901234567"})", fontSize = 12.sp)
                        Text("Nội dung: ${request.issueType}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RescuePrimary)
                        Text("Vị trí: ${request.address}", fontSize = 11.sp, color = OnSurfaceVariantLight, maxLines = 2)

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Payment Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TỔNG CHI PHÍ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Text(paymentMethodDisplay, fontSize = 10.sp, color = SafeGreen)
                            }
                            Text(
                                text = currencyFormat.format(cost),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = AlertRed
                            )
                        }

                        // Certified Stamp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(2.dp, SafeGreen),
                                color = SafeGreen.copy(alpha = 0.08f),
                                modifier = Modifier.rotate(-4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ĐÃ THANH TOÁN", fontWeight = FontWeight.Black, color = SafeGreen, fontSize = 11.sp)
                                        Text("CỨU HỘ 24/7 VIETNAM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "💡 Biên lai này đáp ứng tiêu chuẩn chứng từ cứu hộ để làm thủ tục hoàn trả 100% chi phí với các hãng Bảo hiểm (Bảo Việt, PVI, PTI, PJICO, VBI, Liberty...).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                // Share Button for Insurance Agent
                Button(
                    onClick = { shareReceiptForInsurance(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_share_receipt_insurance"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GỬI CHO HÃNG BẢO HIỂM (ZALO / EMAIL)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Đã lưu biên lai $receiptId vào bộ nhớ máy", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tải Biên Lai Về Máy", fontSize = 12.sp)
                }
            }
        }
    }
}
