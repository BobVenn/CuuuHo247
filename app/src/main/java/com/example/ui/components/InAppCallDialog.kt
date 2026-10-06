package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen
import kotlinx.coroutines.delay

data class InAppCallInfo(
    val recipientName: String,
    val recipientRole: String, // "Kỹ thuật viên cứu hộ" or "Khách hàng gặp sự cố"
    val recipientPhone: String,
    val issueOrVehicle: String = "",
    val isIncoming: Boolean = false
)

@Composable
fun InAppCallDialog(
    callInfo: InAppCallInfo,
    onDismiss: () -> Unit,
    onFallbackPhoneCall: (String) -> Unit
) {
    val context = LocalContext.current

    // Call states: RINGING -> CONNECTED -> ENDED
    var callState by remember { mutableStateOf(if (callInfo.isIncoming) "INCOMING" else "RINGING") }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    // Pulsing circle animation during ringing
    val infiniteTransition = rememberInfiniteTransition(label = "call_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Call duration timer - only counts when actually CONNECTED
    LaunchedEffect(callState) {
        if (callState == "CONNECTED") {
            while (true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .testTag("in_app_call_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: In-App Voice Call status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (callState == "CONNECTED") SafeGreen else Color(0xFFF59E0B),
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = "Cuộc Gọi Cứu Hộ Trực Tiếp 24/7",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = callInfo.recipientName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RescuePrimary.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = callInfo.recipientRole,
                            color = RescuePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (callInfo.issueOrVehicle.isNotBlank()) {
                        Text(
                            text = callInfo.issueOrVehicle,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Call status or duration
                    Text(
                        text = when (callState) {
                            "CONNECTED" -> {
                                val minutes = callDurationSeconds / 60
                                val seconds = callDurationSeconds % 60
                                String.format(java.util.Locale.US, "%02d:%02d • Đang đàm thoại trực tiếp", minutes, seconds)
                            }
                            "INCOMING" -> "Cuộc gọi đến từ ${callInfo.recipientRole}..."
                            else -> "Đang đổ chuông... Đợi người nghe nhấc máy"
                        },
                        color = if (callState == "CONNECTED") SafeGreen else Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Center: Animated Avatar with voice waves
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    if (callState == "RINGING" || callState == "INCOMING" || callState == "CONNECTED") {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (callState == "CONNECTED") SafeGreen.copy(alpha = 0.15f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                )
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (callState == "CONNECTED") SafeGreen else RescuePrimary,
                        modifier = Modifier.size(110.dp),
                        shadowElevation = 12.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (callInfo.recipientRole.contains("Kỹ thuật", ignoreCase = true)) Icons.Default.Engineering else Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                // Middle: Call status banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (callState != "CONNECTED") {
                            Text(
                                text = "🔔 Đang chờ ${callInfo.recipientName} bắt máy để bắt đầu đàm thoại...",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )

                            // Action button to answer/connect the call
                            Button(
                                onClick = { callState = "CONNECTED" },
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (callInfo.isIncoming) "Nhấc Máy Trả Lời" else "Bắt Máy / Kết Nối Đàm Thoại",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Đang kết nối âm thanh hai chiều trực tiếp qua mạng",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Footer Actions: Mute, Speaker, Fallback to GSM, End Call
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Secondary Controls: Mute & Speaker & Direct Phone Call
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Bật/Tắt Micro",
                                    tint = if (isMuted) Color.Black else Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isMuted) "Bật Mic" else "Tắt Mic",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Speaker button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { isSpeakerOn = !isSpeakerOn },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerOn) SafeGreen else Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                    contentDescription = "Loa ngoài",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSpeakerOn) "Loa ngoài" else "Loa trong",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Fallback to traditional GSM phone call
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onFallbackPhoneCall(callInfo.recipientPhone)
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneForwarded,
                                    contentDescription = "Gọi qua SIM",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gọi SIM",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Big Red End Call Button
                    FilledIconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AlertRed),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("btn_end_in_app_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Gác máy",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "Bấm nút đỏ để gác máy kết thúc cuộc gọi",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
