package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.data.model.SavedVehicle
import com.example.data.model.ServiceAvailabilityItem
import com.example.data.model.UserProfile
import com.example.ui.components.CreateRequestDialog
import com.example.ui.components.EmergencyHandbookDialog
import com.example.ui.components.EmergencyHazardStrobeDialog
import com.example.ui.components.LeafletMapMarker
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RealtimeStatusDashboard
import com.example.ui.components.ServiceAvailabilityDashboard
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
    serviceAvailability: List<ServiceAvailabilityItem> = emptyList(),
    isServicesLoading: Boolean = false,
    onRefreshServices: () -> Unit = {},
    onToggleServiceAvailability: ((serviceId: String, isAvailable: Boolean) -> Unit)? = null,
    savedVehicles: List<SavedVehicle> = emptyList(),
    onUpdateEmergencyProfile: ((
        displayName: String,
        phone: String,
        vehicleType: String,
        vehicleName: String,
        licensePlate: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        emergencyRelationship: String,
        bloodType: String,
        emergencyNotes: String,
        homeAddress: String
    ) -> Unit)? = null,
    onAddSavedVehicle: ((name: String, vType: String, plate: String, inspection: String, insurance: String) -> Unit)? = null,
    onRemoveSavedVehicle: ((String) -> Unit)? = null,
    onRefreshLocation: () -> Unit,
    onEmergencySosCall: () -> Unit,
    onCreateRequest: (issueType: String, description: String, vehicleType: String, licensePlate: String, imageUrl: String?) -> Unit,
    onRequestClick: (RescueRequest) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onShareLocation: () -> Unit,
    onNavigateToGoogleMapsFinder: () -> Unit = {},
    onUpdateStatus: ((requestId: String, status: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedIssueForDialog by remember { mutableStateOf<String?>(null) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }
    var showHandbookDialog by remember { mutableStateOf(false) }
    var showStrobeDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("vi", "VN")) }

    // Tính năng quay số nhanh (Quick-Dial) trực tiếp tới số hotline cứu hộ
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
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 8 DỊCH VỤ CỨU HỘ CHÍNH - BỐ CỤC LƯỚI GỌN GÀNG HIỂN THỊ NGAY KHI MỞ APP
    val quickServices = remember {
        listOf(
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
                id = "MOTO",
                title = "Xe máy",
                priceDisplay = "Từ 70k",
                icon = Icons.Default.TwoWheeler,
                bgColor = CategoryOrangeBg,
                tintColor = CategoryOrangeTint,
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
                title = "Cẩu kéo",
                priceDisplay = "Từ 450k",
                icon = Icons.Default.LocalShipping,
                bgColor = CategoryBlueBg,
                tintColor = CategoryBlueTint,
                issueType = "Tai nạn"
            ),
            QuickServiceItem(
                id = "REPAIR",
                title = "Sửa nhanh",
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
            .padding(bottom = 90.dp)
    ) {
        // =====================================================================
        // 1. COMPACT TOP HEADER & INTEGRATED ADDRESS PILL (CHỈ ~70DP CAO)
        // =====================================================================
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Greetings & Emergency Hotline Chip
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
                            color = RescuePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Xin chào 👋",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariantLight,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (userProfile?.displayName.isNullOrBlank()) "Khách hàng" else userProfile!!.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 1-Tap Hotline Chip
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AlertRedLight,
                        border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clickable { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) }
                            .testTag("btn_top_hotline")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Gọi cứu hộ",
                                tint = AlertRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = AppConfig.RESCUE_HOTLINE_DISPLAY,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = AlertRed
                            )
                        }
                    }
                }

                // Compact Location Pill (Tối giản diện tích, hiển thị trực quan)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_info_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isLocating) "Đang định vị..." else currentLocation.address,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!hasFineLocationPermission) {
                                TextButton(
                                    onClick = onRequestLocationPermission,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Bật GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.clickable { onToggleRealtimeTracking() }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isRealtimeTrackingActive) SafeGreen else EmergencyGold)
                                    )
                                    Text(
                                        text = if (isRealtimeTrackingActive) "Live" else "Sẵn sàng",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRealtimeTrackingActive) SafeGreen else OnSurfaceVariantLight
                                    )
                                }
                            }

                            IconButton(
                                onClick = onRefreshLocation,
                                modifier = Modifier.size(22.dp)
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        color = RescuePrimary,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Làm mới vị trí",
                                        tint = OnSurfaceVariantLight,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =====================================================================
        // 2. ULTRA-COMPACT HERO EMERGENCY BANNER (TINH GỌN, CAO CHỈ ~90DP)
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("banner_emergency_sos"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = RescuePrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "CỨU HỘ 24/7",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "Có mặt sau 15p",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Gặp sự cố trên đường?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "KTV túc trực toàn quốc",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Hotline Call Button with pulsating scale animation
                    Button(
                        onClick = { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                        modifier = Modifier
                            .scale(pulseScale)
                            .testTag("btn_hero_sos_call"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = AlertRed
                        ),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppConfig.RESCUE_HOTLINE_DISPLAY,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                            color = AlertRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =====================================================================
        // 3. ESSENTIAL RESCUE SERVICES GRID (HIỂN THỊ 100% TRÊN MÀN HÌNH ĐẦU TIÊN)
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CarRepair, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(17.dp))
                        Text(
                            text = "DỊCH VỤ CỨU HỘ KHẨN CẤP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = OnSurfaceLight,
                            fontSize = 13.5.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CategoryGreenBg
                    ) {
                        Text(
                            text = "Giá niêm yết",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                // 2 Hàng x 4 Cột: Gọn gàng, vừa vặn trên mọi kích cỡ màn hình
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
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = item.bgColor,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            tint = item.tintColor,
                                            modifier = Modifier.size(23.dp)
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
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RescuePrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Quick Request & Share GPS Action Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedIssueForDialog = null
                            showCreateDialog = true
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(36.dp)
                            .testTag("btn_open_create_request"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đặt Cứu Hộ Ngay", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShareLocation,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("btn_share_location"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurfaceLight),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chia Sẻ GPS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =====================================================================
        // 4. TIẾN ĐỘ CỨU HỘ ĐANG DIỄN RA (NẾU CÓ ĐƠN HÀNG HOẠT ĐỘNG)
        // =====================================================================
        if (activeRequest != null) {
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                RealtimeStatusDashboard(
                    request = activeRequest,
                    isEmbeddedCard = true,
                    onOpenFullDetail = { onRequestClick(activeRequest) },
                    onCallHotline = { phone -> quickDial(phone) },
                    onCallTechnician = { phone -> quickDial(phone) },
                    onOpenChat = { onRequestClick(activeRequest) },
                    onSimulateStep = null
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // =====================================================================
        // 5. TIỆN ÍCH HỖ TRỢ DẠNG LƯỚI 2x2 (TIẾT KIỆM 65% DIỆN TÍCH CUỘN TRANG)
        // =====================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hàng 1: Tra cứu Gara & Bảng Giá Cẩm Nang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Tra cứu Gara lân cận
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToGoogleMapsFinder() }
                        .testTag("card_google_maps_grounding"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SafeGreen.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("Tìm Gara Gần", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = OnSurfaceLight)
                        }
                        Text(
                            text = "Tra cứu gara & tiệm vá xe quanh bạn",
                            fontSize = 10.sp,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card 2: Bảng giá & cẩm nang
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showHandbookDialog = true }
                        .testTag("card_handbook_pricing_guide"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, EmergencyGold.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("Bảng Giá & Mẹo", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = OnSurfaceLight)
                        }
                        Text(
                            text = "Giá chuẩn & mẹo nổ lốp, hết bình",
                            fontSize = 10.sp,
                            color = OnSurfaceVariantLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Hàng 2: Đèn chớp SOS Hazard & Xem Bản Đồ Xe Trực
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 3: Đèn chớp hazard SOS
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStrobeDialog = true }
                        .testTag("btn_open_emergency_strobe"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AlertRed,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                            Text("Đèn Chớp Hazard", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF991B1B))
                        }
                        Text(
                            text = "Cảnh báo ban đêm & còi hú SOS",
                            fontSize = 10.sp,
                            color = Color(0xFFB91C1C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Card 4: Tổng đài ứng trực 24/7
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { quickDial(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SafeGreen,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("Tổng Đài 24/7", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF166534))
                        }
                        Text(
                            text = "Điều phối xe kéo & thợ máy nhanh",
                            fontSize = 10.sp,
                            color = Color(0xFF15803D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =====================================================================
        // 6. BẢN ĐỒ CỨU HỘ TRỰC TIẾP (LEAFLET.JS - CHIỀU CAO 155DP TINH GỌN)
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("home_live_rescue_map_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            val isCurrentRescueActive = activeRequest != null && activeRequest.status in listOf(
                AppConfig.RequestStatus.ACCEPTED,
                AppConfig.RequestStatus.EN_ROUTE,
                AppConfig.RequestStatus.ARRIVED,
                AppConfig.RequestStatus.IN_PROGRESS
            )

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = RescuePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "Bản Đồ Xe Cứu Hộ Trực Tiếp",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrentRescueActive) Color(0xFFE0F2FE) else CategoryGreenBg
                    ) {
                        Text(
                            text = if (isCurrentRescueActive) "🚚 Xe đang tới bạn" else "3 xe trực gần bạn",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentRescueActive) Color(0xFF0284C7) else SafeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val baseLat = if (currentLocation.latitude != 0.0) currentLocation.latitude else (activeRequest?.latitude?.takeIf { it != 0.0 } ?: 21.0285)
                val baseLng = if (currentLocation.longitude != 0.0) currentLocation.longitude else (activeRequest?.longitude?.takeIf { it != 0.0 } ?: 105.8542)
                val active = if (isCurrentRescueActive) activeRequest else null

                val mapMarkers = remember(active?.id, active?.status, baseLat, baseLng) {
                    if (active != null) {
                        val reqLat = if (active.latitude != 0.0) active.latitude else baseLat
                        val reqLng = if (active.longitude != 0.0) active.longitude else baseLng

                        val (techLat, techLng, distStr, etaStr) = when (active.status) {
                            AppConfig.RequestStatus.ACCEPTED -> listOf(reqLat + 0.0165, reqLng + 0.0135, "2.8 km", "15-20 phút")
                            AppConfig.RequestStatus.EN_ROUTE -> listOf(reqLat + 0.0075, reqLng + 0.0062, "1.2 km", "8-12 phút")
                            AppConfig.RequestStatus.ARRIVED -> listOf(reqLat + 0.0003, reqLng + 0.0003, "50 m", "Đã đến nơi")
                            else -> listOf(reqLat + 0.0002, reqLng + 0.0002, "Tại chỗ", "Đang xử lý")
                        }

                        listOf(
                            LeafletMapMarker(
                                id = "home_incident_${active.id}",
                                title = "Vị trí sự cố (${active.issueType})",
                                snippet = active.address.ifBlank { "Vị trí của bạn" },
                                latitude = reqLat,
                                longitude = reqLng,
                                type = "REQUEST",
                                status = active.status
                            ),
                            LeafletMapMarker(
                                id = "home_tech_${active.id}",
                                title = "Xe Cứu Hộ: ${active.staffName ?: "KTV Cứu Hộ"}",
                                snippet = "Biển số: 29C-888.12 • Cách $distStr • ETA: $etaStr",
                                latitude = techLat as Double,
                                longitude = techLng as Double,
                                type = "TECHNICIAN",
                                status = active.status
                            )
                        )
                    } else {
                        listOf(
                            LeafletMapMarker(
                                id = "patrol_1",
                                title = "Đội Cứu Hộ 24/7 (Xe Sàn Trượt)",
                                snippet = "Cách bạn 1.2 km • Túc trực sẵn sàng",
                                latitude = baseLat + 0.0072,
                                longitude = baseLng + 0.0061,
                                type = "PATROL"
                            ),
                            LeafletMapMarker(
                                id = "patrol_2",
                                title = "Xe Cứu Hộ Kích Bình & Vá Lốp",
                                snippet = "Cách bạn 1.8 km • Túc trực sẵn sàng",
                                latitude = baseLat - 0.0068,
                                longitude = baseLng - 0.0054,
                                type = "PATROL"
                            ),
                            LeafletMapMarker(
                                id = "patrol_3",
                                title = "Trạm Cẩu Kéo Lưu Động Gara",
                                snippet = "Cách bạn 2.4 km • Túc trực sẵn sàng",
                                latitude = baseLat + 0.0112,
                                longitude = baseLng - 0.0075,
                                type = "PATROL"
                            )
                        )
                    }
                }

                val destLat = if (active != null && active.latitude != 0.0) active.latitude else null
                val destLng = if (active != null && active.longitude != 0.0) active.longitude else null

                LeafletMapView(
                    centerLat = currentLocation.latitude.takeIf { it != 0.0 } ?: baseLat,
                    centerLng = currentLocation.longitude.takeIf { it != 0.0 } ?: baseLng,
                    centerAddress = currentLocation.address,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp),
                    markers = mapMarkers,
                    routeDestinationLat = destLat,
                    routeDestinationLng = destLng,
                    routeDestinationLabel = activeRequest?.address ?: "Vị trí cứu hộ",
                    onMarkerClick = { marker ->
                        if (activeRequest != null && (marker.id.startsWith("home_") || marker.type == "TECHNICIAN" || marker.type == "REQUEST")) {
                            onRequestClick(activeRequest)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =====================================================================
        // 7. YÊU CẦU CỨU HỘ GẦN ĐÂY
        // =====================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HOẠT ĐỘNG GẦN ĐÂY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceLight,
                        fontSize = 12.5.sp
                    )
                    TextButton(
                        onClick = onNavigateToHistory,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Xem tất cả", color = RescuePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(14.dp))
                    }
                }

                if (recentRequests.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Chưa có sự cố nào gần đây",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = "Hệ thống sẵn sàng hỗ trợ bạn 24/7 khi cần",
                            fontSize = 10.5.sp,
                            color = OnSurfaceVariantLight
                        )
                    }
                } else {
                    recentRequests.take(3).forEach { req ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRequestClick(req) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
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
                                            fontSize = 12.sp,
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
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${req.vehicleType} • ${dateFormat.format(Date(req.timestamp))}",
                                        fontSize = 10.sp,
                                        color = OnSurfaceVariantLight
                                    )
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = OnSurfaceVariantLight,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 8. BẢNG ĐIỀU PHỐI DÀNH RIÊNG CHO KTV / QUẢN TRỊ VIÊN (STAFF ONLY)
        // =====================================================================
        if (userProfile?.role == AppConfig.UserRole.STAFF) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                ServiceAvailabilityDashboard(
                    services = serviceAvailability,
                    isLoading = isServicesLoading,
                    onRefresh = onRefreshServices,
                    onRequestService = { item ->
                        selectedIssueForDialog = item.issueType
                        showCreateDialog = true
                    },
                    onToggleAvailability = onToggleServiceAvailability,
                    onCallEmergencyHotline = { phone -> quickDial(phone) },
                    isStaffUser = true
                )
            }
        }
    }

    // =========================================================================
    // DIALOGS & OVERLAYS
    // =========================================================================

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
