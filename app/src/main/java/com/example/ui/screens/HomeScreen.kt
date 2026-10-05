package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import android.content.Intent
import android.net.Uri
import com.example.ui.components.CreateRequestDialog
import com.example.ui.components.EmergencyHandbookDialog
import com.example.ui.components.EmergencyHazardStrobeDialog
import com.example.ui.components.LeafletMapMarker
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RealtimeStatusDashboard
import com.example.ui.theme.*
import com.example.util.UserLocationInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Item biểu tượng dịch vụ nhanh theo phong cách ứng dụng Grab / Gojek.
 */
data class QuickServiceItem(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val icon: ImageVector,
    val bgColor: Color,
    val tintColor: Color,
    val issueType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    currentLocation: UserLocationInfo,
    isLocating: Boolean,
    hasFineLocationPermission: Boolean = false,
    isRealtimeTrackingActive: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    onToggleRealtimeTracking: () -> Unit = {},
    activeRequest: RescueRequest?,
    recentRequests: List<RescueRequest>,
    onRefreshLocation: () -> Unit,
    onEmergencySosCall: () -> Unit,
    onCreateRequest: (issueType: String, description: String, vehicleType: String, licensePlate: String, imageUrl: String?) -> Unit,
    onRequestClick: (RescueRequest) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onShareLocation: () -> Unit,
    onUpdateStatus: ((requestId: String, status: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedIssueForDialog by remember { mutableStateOf<String?>(null) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }
    var showHandbookDialog by remember { mutableStateOf(false) }
    var showStrobeDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("vi", "VN")) }

    // Tính năng quay số nhanh (Quick-Dial) trực tiếp tới số hotline cài sẵn 0898212031
    fun quickDial(phoneNumber: String = AppConfig.DEFAULT_RESCUE_HOTLINE) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            onEmergencySosCall()
        }
    }

    // Pulsating animation cho nút gọi khẩn cấp
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Danh mục dịch vụ chuẩn phong cách Grab/Gojek: icon hóa, ngắn gọn, có giá nổi bật
    val quickServices = remember {
        listOf(
            QuickServiceItem(
                id = "MOTO",
                title = "Xe máy",
                priceDisplay = "Từ 70k",
                icon = Icons.Default.TwoWheeler,
                bgColor = CategoryOrangeBg,
                tintColor = CategoryOrangeTint,
                issueType = "Hỏng xe"
            ),
            QuickServiceItem(
                id = "CAR",
                title = "Ô tô",
                priceDisplay = "Từ 200k",
                icon = Icons.Default.DirectionsCar,
                bgColor = CategoryBlueBg,
                tintColor = CategoryBlueTint,
                issueType = "Hỏng xe"
            ),
            QuickServiceItem(
                id = "BATTERY",
                title = "Kích bình",
                priceDisplay = "200k",
                icon = Icons.Default.Bolt,
                bgColor = CategoryAmberBg,
                tintColor = CategoryAmberTint,
                issueType = "Hết bình"
            ),
            QuickServiceItem(
                id = "TIRE",
                title = "Vá lốp",
                priceDisplay = "120k",
                icon = Icons.Default.Settings,
                bgColor = CategoryGreenBg,
                tintColor = CategoryGreenTint,
                issueType = "Thủng lốp"
            ),
            QuickServiceItem(
                id = "GAS",
                title = "Tiếp xăng",
                priceDisplay = "100k",
                icon = Icons.Default.LocalGasStation,
                bgColor = CategoryRedBg,
                tintColor = CategoryRedTint,
                issueType = "Xe hết xăng"
            ),
            QuickServiceItem(
                id = "KEY",
                title = "Mở khóa",
                priceDisplay = "150k",
                icon = Icons.Default.Key,
                bgColor = CategoryPurpleBg,
                tintColor = CategoryPurpleTint,
                issueType = "Khóa xe"
            ),
            QuickServiceItem(
                id = "TOW",
                title = "Cẩu kéo xe",
                priceDisplay = "Từ 450k",
                icon = Icons.Default.LocalShipping,
                bgColor = CategoryBlueBg,
                tintColor = CategoryBlueTint,
                issueType = "Tai nạn"
            ),
            QuickServiceItem(
                id = "REPAIR",
                title = "Sửa lưu động",
                priceDisplay = "Từ 150k",
                icon = Icons.Default.Build,
                bgColor = CategoryOrangeBg,
                tintColor = CategoryOrangeTint,
                issueType = "Khác"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 95.dp)
    ) {
        // TOP HEADER: Nền trắng tinh tế, logo + lời chào + nút gọi hotline nhỏ gọn
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile & Greetings
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RescuePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Xin chào 👋",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight
                            )
                            Text(
                                text = if (userProfile?.displayName.isNullOrBlank()) "Khách hàng" else userProfile!!.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight
                            )
                        }
                    }

                    // Hotline Dialer Quick Chip (Quick-Dial 0898212031)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AlertRedLight,
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) }
                            .testTag("btn_top_hotline")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Gọi cứu hộ",
                                tint = AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Hotline: ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = AlertRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // HERO SOS EMERGENCY BANNER (Phong cách Grab / Gojek: Thẻ bo góc hiện đại, cực kỳ nổi bật, không chữ thừa)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("banner_emergency_sos"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = RescuePrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                RescuePrimary,
                                Color(0xFFFF6E40),
                                AlertRed
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "KHẨN CẤP 24/7",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Gặp sự cố trên đường?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = "Kỹ thuật viên có mặt sau 15 phút",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Quick-Dial Call Button
                    Button(
                        onClick = { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                        modifier = Modifier
                            .scale(pulseScale)
                            .testTag("btn_hero_sos_call"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = AlertRed
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "0898 212 031",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = AlertRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QUICK-DIAL EMERGENCY SERVICE HOTLINE (0898 212 031)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) }
                .testTag("card_quick_dial_hotline"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AlertRed,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhoneForwarded,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "QUICK-DIAL HOTLINE 24/7",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AlertRed
                            ) {
                                Text(
                                    text = "1-CHẠM",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = AppConfig.RESCUE_HOTLINE_DISPLAY,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = AlertRed
                        )
                        Text(
                            text = "Bấm gọi ngay lập tức tới đội ứng trực cứu hộ",
                            fontSize = 10.5.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }

                Button(
                    onClick = { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_quick_dial_now")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GỌI NGAY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // LOCATION & GPS TỌA ĐỘ MINI CARD (Gọn gàng, tinh tế, chia nhóm rõ ràng)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("location_info_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Address Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CategoryGreenBg,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isLocating) "Đang định vị vị trí..." else currentLocation.address,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onRefreshLocation,
                        modifier = Modifier.size(28.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = RescuePrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Làm mới",
                                tint = OnSurfaceVariantLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Coordinates & Accuracy Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.5f", currentLocation.latitude)}, ${String.format(Locale.US, "%.5f", currentLocation.longitude)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CategoryGreenBg
                        ) {
                            Text(
                                text = currentLocation.formattedAccuracy,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Realtime status or permission button
                    if (!hasFineLocationPermission) {
                        TextButton(
                            onClick = onRequestLocationPermission,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bật GPS chính xác", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { onToggleRealtimeTracking() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRealtimeTrackingActive) SafeGreen else EmergencyGold)
                            )
                            Text(
                                text = if (isRealtimeTrackingActive) "Live GPS" else "GPS Sẵn sàng",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRealtimeTrackingActive) SafeGreen else OnSurfaceVariantLight
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BẢN ĐỒ CỨU HỘ TRỰC TIẾP (Leaflet.js & OpenStreetMap - 100% Free, Hiển thị trực quan)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("home_live_rescue_map_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RescuePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Bản Đồ Cứu Hộ Trực Tiếp",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Định vị vệ tinh GPS & Xe trực 24/7",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariantLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CategoryGreenBg
                    ) {
                        Text(
                            text = "3 xe trực gần bạn",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                val nearbyPatrols = remember(currentLocation.latitude, currentLocation.longitude) {
                    val lat = if (currentLocation.latitude != 0.0) currentLocation.latitude else 21.0285
                    val lng = if (currentLocation.longitude != 0.0) currentLocation.longitude else 105.8542
                    listOf(
                        LeafletMapMarker(
                            id = "patrol_1",
                            title = "Đội Cứu Hộ 24/7 (Xe Sàn Trượt)",
                            snippet = "Cách bạn 1.2 km • Túc trực sẵn sàng",
                            latitude = lat + 0.0072,
                            longitude = lng + 0.0061,
                            type = "PATROL"
                        ),
                        LeafletMapMarker(
                            id = "patrol_2",
                            title = "Xe Cứu Hộ Kích Bình & Vá Lốp",
                            snippet = "Cách bạn 1.8 km • Túc trực sẵn sàng",
                            latitude = lat - 0.0068,
                            longitude = lng - 0.0054,
                            type = "PATROL"
                        ),
                        LeafletMapMarker(
                            id = "patrol_3",
                            title = "Trạm Cẩu Kéo Lưu Động Gara",
                            snippet = "Cách bạn 2.4 km • Túc trực sẵn sàng",
                            latitude = lat + 0.0112,
                            longitude = lng - 0.0075,
                            type = "PATROL"
                        )
                    )
                }

                LeafletMapView(
                    centerLat = currentLocation.latitude,
                    centerLng = currentLocation.longitude,
                    centerAddress = currentLocation.address,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    markers = nearbyPatrols
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REALTIME EMERGENCY STATUS DASHBOARD (Firebase Realtime Database Live Observer)
        if (activeRequest != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                RealtimeStatusDashboard(
                    request = activeRequest,
                    isEmbeddedCard = true,
                    onOpenFullDetail = { onRequestClick(activeRequest) },
                    onCallHotline = { phone -> quickDial(phone) },
                    onCallTechnician = { phone -> quickDial(phone) },
                    onOpenChat = { onRequestClick(activeRequest) },
                    onSimulateStep = if (onUpdateStatus != null) { { newSt -> onUpdateStatus(activeRequest.id, newSt) } } else null
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // DỊCH VỤ CỨU HỘ: 8 ICON THEO LƯỚI 4 CỘT PHONG CÁCH GRAB/GOJEK
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DỊCH VỤ CỨU HỘ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = OnSurfaceLight
                    )
                    Text(
                        text = "Giá niêm yết",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 2 hàng x 4 cột dịch vụ dạng icon + tên ngắn + giá nổi bật
                quickServices.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedIssueForDialog = item.issueType
                                        showCreateDialog = true
                                    }
                                    .testTag("quick_service_${item.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = item.bgColor,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.tintColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceLight,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )

                                Text(
                                    text = item.priceDisplay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RescuePrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // BẢNG GIÁ NIÊM YẾT & CẨM NANG XỬ LÝ SỰ CỐ KHẨN CẤP (Tra cứu chuẩn & an toàn)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showHandbookDialog = true }
                .testTag("card_handbook_pricing_guide"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.2.dp, EmergencyGold.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Bảng Giá & Cẩm Nang Sự Cố",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = OnSurfaceLight
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafeGreen
                            ) {
                                Text(
                                    text = "NIÊM YẾT",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Vá lốp 120k • Kích bình 200k • Cẩu kéo • Cẩm nang nổ lốp cao tốc",
                            fontSize = 11.sp,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Xem",
                        color = Color(0xFFB45309),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // NÚT HÀNH ĐỘNG NHANH: GỬI YÊU CẦU & CHIA SẺ VỊ TRÍ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    selectedIssueForDialog = null
                    showCreateDialog = true
                },
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("btn_open_create_request"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Đặt Cứu Hộ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onShareLocation,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_share_location"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceLight)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chia Sẻ GPS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ĐÈN CHỚP CẢNH BÁO SOS & CÒI CỨU NẠN (BAN ĐÊM / CAO TỐC)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showStrobeDialog = true }
                .testTag("btn_open_emergency_strobe"),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFEF2F2),
            border = BorderStroke(1.2.dp, Color(0xFFFECACA)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AlertRed,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Đèn Chớp Hazard & Còi SOS Ban Đêm", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF991B1B))
                            Surface(shape = RoundedCornerShape(4.dp), color = AlertRed) {
                                Text("CỨU NẠN", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text("Phát đèn nhấp nháy báo hiệu & còi hú cứu nạn khi xe hỏng đêm trên cao tốc", fontSize = 10.5.sp, color = Color(0xFFB91C1C), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRed
                ) {
                    Text("BẬT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // YÊU CẦU CỨU HỘ GẦN ĐÂY (Tinh gọn, loại bỏ mô tả rườm rà)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOẠT ĐỘNG GẦN ĐÂY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = OnSurfaceLight
                    )
                    TextButton(
                        onClick = onNavigateToHistory,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Xem tất cả", color = RescuePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                    }
                }

                if (recentRequests.isEmpty()) {
                    // Empty state gọn gàng, không tràn chữ
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Chưa có sự cố nào gần đây",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = "Hệ thống sẵn sàng hỗ trợ bạn 24/7",
                            fontSize = 11.sp,
                            color = OnSurfaceVariantLight
                        )
                    }
                } else {
                    recentRequests.take(3).forEach { req ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestClick(req) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = req.issueType,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = OnSurfaceLight
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (req.status) {
                                                AppConfig.RequestStatus.COMPLETED -> CategoryGreenBg
                                                AppConfig.RequestStatus.CANCELLED -> CategoryRedBg
                                                else -> CategoryOrangeBg
                                            }
                                        ) {
                                            Text(
                                                text = req.status,
                                                color = when (req.status) {
                                                    AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                                    AppConfig.RequestStatus.CANCELLED -> AlertRed
                                                    else -> RescuePrimary
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${req.vehicleType} • ${dateFormat.format(Date(req.timestamp))}",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // SOS CONFIRMATION DIALOG
    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "GỌI CỨU HỘ KHẨN CẤP",
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kết nối trực tiếp tới tổng đài cứu hộ giao thông toàn quốc:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📞 ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = AlertRed
                            )
                            Text(
                                text = "Trực ban 24/7 • Toàn quốc",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_confirm_call_dialer")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GỌI ${AppConfig.RESCUE_HOTLINE_DISPLAY}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSosConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hủy")
                }
            }
        )
    }

    // CREATE REQUEST DIALOG
    if (showCreateDialog) {
        CreateRequestDialog(
            initialIssueType = selectedIssueForDialog,
            userProfile = userProfile ?: UserProfile(displayName = "Khách hàng"),
            currentLocation = currentLocation,
            onRefreshLocation = onRefreshLocation,
            onDismiss = { showCreateDialog = false },
            onSubmit = { issue, desc, vType, plate, img ->
                showCreateDialog = false
                onCreateRequest(issue, desc, vType, plate, img)
            }
        )
    }

    // EMERGENCY HANDBOOK & PRICING DIALOG
    if (showHandbookDialog) {
        EmergencyHandbookDialog(
            onDismiss = { showHandbookDialog = false },
            onDirectCallSos = onEmergencySosCall
        )
    }

    // EMERGENCY HAZARD STROBE & AUDIBLE SIREN DIALOG
    if (showStrobeDialog) {
        EmergencyHazardStrobeDialog(
            currentLocation = currentLocation,
            onDismiss = { showStrobeDialog = false },
            onCallSos = { phone ->
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    onEmergencySosCall()
                }
            }
        )
    }
}
