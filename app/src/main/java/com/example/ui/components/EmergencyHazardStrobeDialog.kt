package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.config.AppConfig
import com.example.ui.theme.*
import com.example.util.UserLocationInfo
import kotlinx.coroutines.delay
import java.util.Locale

enum class StrobeMode {
    HAZARD_AMBER, // Đèn chớp vàng cam cảnh báo nguy hiểm
    SOS_RED,      // Đèn chớp đỏ cứu nạn khẩn cấp
    TORCH_WHITE   // Đèn trắng soi sáng khoang máy / bánh xe
}

@Composable
fun EmergencyHazardStrobeDialog(
    currentLocation: UserLocationInfo,
    onDismiss: () -> Unit,
    onCallSos: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(StrobeMode.HAZARD_AMBER) }
    var isSirenActive by remember { mutableStateOf(false) }

    // Flashing Animation for Hazard/SOS
    val infiniteTransition = rememberInfiniteTransition(label = "strobe_transition")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (selectedMode == StrobeMode.SOS_RED) 400 else 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash_alpha"
    )

    // System Tone Generator for Audible Rescue Siren
    val toneGen = remember {
        try {
            ToneGenerator(AudioManager.STREAM_ALARM, 90)
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGen?.stopTone()
                toneGen?.release()
            } catch (_: Exception) {}
        }
    }

    // Siren loop
    LaunchedEffect(isSirenActive) {
        if (isSirenActive) {
            while (isSirenActive) {
                try {
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 350)
                } catch (_: Exception) {}
                delay(700)
            }
        } else {
            try {
                toneGen?.stopTone()
            } catch (_: Exception) {}
        }
    }

    val activeBackgroundColor by animateColorAsState(
        targetValue = when (selectedMode) {
            StrobeMode.HAZARD_AMBER -> Color(0xFFF59E0B).copy(alpha = flashAlpha)
            StrobeMode.SOS_RED -> Color(0xFFDC2626).copy(alpha = flashAlpha)
            StrobeMode.TORCH_WHITE -> Color.White
        },
        label = "bg_color"
    )

    val contentColor = when (selectedMode) {
        StrobeMode.TORCH_WHITE -> Color.Black
        else -> Color.White
    }

    fun copyCoordinates() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = "${currentLocation.latitude},${currentLocation.longitude} (${currentLocation.address})"
        val clip = ClipData.newPlainText("GPS", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép tọa độ & vị trí khẩn cấp", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = {
            isSirenActive = false
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(activeBackgroundColor)
                .testTag("emergency_strobe_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .systemBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Title & Close
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
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "ĐÈN CẢNH BÁO SỰ CỐ KHẨN CẤP",
                                color = contentColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Đặt điện thoại sau kính lái hoặc đuôi xe để báo hiệu",
                                color = contentColor.copy(alpha = 0.85f),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isSirenActive = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tắt", tint = Color.White)
                    }
                }

                // Center: Big Warning Graphic
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = BorderStroke(3.dp, if (selectedMode == StrobeMode.SOS_RED) Color.Red else Color(0xFFFBBF24)),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (selectedMode) {
                                    StrobeMode.HAZARD_AMBER -> Icons.Default.WarningAmber
                                    StrobeMode.SOS_RED -> Icons.Default.Sos
                                    StrobeMode.TORCH_WHITE -> Icons.Default.LightMode
                                },
                                contentDescription = null,
                                tint = when (selectedMode) {
                                    StrobeMode.HAZARD_AMBER -> Color(0xFFFBBF24)
                                    StrobeMode.SOS_RED -> Color(0xFFEF4444)
                                    StrobeMode.TORCH_WHITE -> Color.White
                                },
                                modifier = Modifier.size(72.dp)
                            )

                            Text(
                                text = when (selectedMode) {
                                    StrobeMode.HAZARD_AMBER -> "CẢNH BÁO XE DỪNG KHẨN CẤP"
                                    StrobeMode.SOS_RED -> "TÍN HIỆU CỨU NẠN SOS"
                                    StrobeMode.TORCH_WHITE -> "ĐÈN SOI SÁNG BAN ĐÊM"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = when (selectedMode) {
                                    StrobeMode.HAZARD_AMBER -> "Đèn hazard hổ phách cảnh báo khẩn cấp khi xe gặp sự cố điện"
                                    StrobeMode.SOS_RED -> "Nhấp nháy tần số cao thu hút sự chú ý của cứu hộ"
                                    StrobeMode.TORCH_WHITE -> "Ánh sáng trắng cực đại soi kiểm tra lốp và động cơ"
                                },
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Mode Selection Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clickable { selectedMode = StrobeMode.HAZARD_AMBER }
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (selectedMode == StrobeMode.HAZARD_AMBER) Color(0xFFF59E0B) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚠️ Đèn Hazard",
                                color = if (selectedMode == StrobeMode.HAZARD_AMBER) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clickable { selectedMode = StrobeMode.SOS_RED }
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (selectedMode == StrobeMode.SOS_RED) Color(0xFFDC2626) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🚨 SOS Đỏ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clickable { selectedMode = StrobeMode.TORCH_WHITE }
                                .clip(RoundedCornerShape(12.dp)),
                            color = if (selectedMode == StrobeMode.TORCH_WHITE) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "💡 Soi Sáng",
                                color = if (selectedMode == StrobeMode.TORCH_WHITE) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Siren Toggle Button
                    Button(
                        onClick = { isSirenActive = !isSirenActive },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSirenActive) Color(0xFFDC2626) else Color.Black.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_toggle_emergency_siren")
                    ) {
                        Icon(
                            if (isSirenActive) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSirenActive) "ĐANG PHÁT CÒI BÁO ĐỘNG HÚ (BẬT)" else "BẬT CÒI BÁO ĐỘNG SOS (ÂM THANH LỚN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    }
                }

                // Bottom Panel: GPS Coordinates & 1-Tap Emergency Dials
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // GPS Location Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { copyCoordinates() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "Vị trí của bạn: ${String.format(Locale.US, "%.5f, %.5f", currentLocation.latitude, currentLocation.longitude)}",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currentLocation.address.ifBlank { "Đang lấy địa chỉ chính xác..." },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("Chép GPS", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                        // Quick Hotline Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onCallSos("113") },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("113 (CSGT)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onCallSos("114") },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("114 (Cứu Nạn)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onCallSos("115") },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("115 (Cấp Cứu)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onCallSos(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                modifier = Modifier.weight(1.3f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Hotline 24/7", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
