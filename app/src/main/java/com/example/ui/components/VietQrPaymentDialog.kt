package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VietQrPaymentDialog(
    requestId: String,
    cost: Long,
    issueType: String,
    isTechnicianView: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmPayment: (paymentMethod: String) -> Unit // "VIETQR" or "CASH"
) {
    val context = LocalContext.current
    var selectedMethod by remember { mutableStateOf("VIETQR") } // "VIETQR" or "CASH"
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val bankName = "MB Bank (Ngân Hàng Quân Đội)"
    val bankAccountNo = "0987654321"
    val bankAccountOwner = "CUU HO GIAO THONG 247"
    val transferContent = "CUUHO ${requestId.takeLast(6).uppercase()}"

    // URL chuẩn VietQR Napas tự động sinh mã QR với số tiền và nội dung
    val qrCodeUrl = "https://img.vietqr.io/image/MB-$bankAccountNo-compact2.png?amount=$cost&addInfo=${transferContent.replace(" ", "%20")}&accountName=${bankAccountOwner.replace(" ", "%20")}"

    fun copyToClipboard(label: String, value: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép $label", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("vietqr_payment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
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
                            color = SafeGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = if (isTechnicianView) "Thu Cước Cứu Hộ (KTV)" else "Thanh Toán Cứu Hộ",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isTechnicianView) "Đưa mã cho khách quét hoặc thu tiền mặt" else "Đơn #$transferContent",
                                fontSize = 11.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                // Amount Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTechnicianView) "TỔNG CƯỚC THU TỪ KHÁCH HÀNG" else "TỔNG CHI PHÍ DỊCH VỤ CẦN TRẢ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = currencyFormat.format(cost),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SafeGreen
                        )
                        Text(
                            text = "Sự cố: $issueType",
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight
                        )
                    }
                }

                // Payment Method Selector Tabs
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
                            .clickable { selectedMethod = "VIETQR" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMethod == "VIETQR") Color.White else Color.Transparent,
                        shadowElevation = if (selectedMethod == "VIETQR") 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = if (selectedMethod == "VIETQR") RescuePrimary else OnSurfaceVariantLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Chuyển Khoản QR",
                                fontSize = 12.sp,
                                fontWeight = if (selectedMethod == "VIETQR") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMethod == "VIETQR") RescuePrimary else OnSurfaceVariantLight
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedMethod = "CASH" },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedMethod == "CASH") Color.White else Color.Transparent,
                        shadowElevation = if (selectedMethod == "CASH") 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = if (selectedMethod == "CASH") SafeGreen else OnSurfaceVariantLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Tiền Mặt",
                                fontSize = 12.sp,
                                fontWeight = if (selectedMethod == "CASH") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMethod == "CASH") SafeGreen else OnSurfaceVariantLight
                            )
                        }
                    }
                }

                if (selectedMethod == "VIETQR") {
                    // QR Code Image
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        color = Color.White,
                        modifier = Modifier.size(190.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = qrCodeUrl,
                                contentDescription = "Mã QR Thanh Toán VietQR",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Bank Account Info Details
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ngân hàng:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                                Text(bankName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Số tài khoản:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { copyToClipboard("Số tài khoản", bankAccountNo) }
                                ) {
                                    Text(bankAccountNo, fontSize = 12.sp, fontWeight = FontWeight.Black, color = RescuePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = RescuePrimary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chủ tài khoản:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                                Text(bankAccountOwner, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Nội dung:", fontSize = 11.sp, color = OnSurfaceVariantLight)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { copyToClipboard("Nội dung chuyển khoản", transferContent) }
                                ) {
                                    Text(transferContent, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SafeGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Cash Option Info
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(40.dp))
                            Text(
                                text = if (isTechnicianView) "Thu Tiền Mặt Trực Tiếp" else "Thanh Toán Bằng Tiền Mặt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = if (isTechnicianView) {
                                    "Thu trực tiếp số tiền ${currencyFormat.format(cost)} tiền mặt từ khách hàng tại hiện trường sau khi hoàn tất cứu hộ."
                                } else {
                                    "Vui lòng gửi trực tiếp số tiền ${currencyFormat.format(cost)} cho Kỹ thuật viên sau khi hoàn thành công việc cứu hộ."
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF78350F),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Buttons - clearly distinguished for KTV vs Customer
                Button(
                    onClick = {
                        onConfirmPayment(selectedMethod)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_payment_complete"),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTechnicianView) {
                            if (selectedMethod == "VIETQR") "XÁC NHẬN KHÁCH ĐÃ CHUYỂN KHOẢN QR" else "XÁC NHẬN ĐÃ THU TIỀN MẶT TỪ KHÁCH"
                        } else {
                            if (selectedMethod == "VIETQR") "TÔI ĐÃ CHUYỂN KHOẢN QR THÀNH CÔNG" else "TÔI ĐÃ GỬI TIỀN MẶT CHO THỢ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}
