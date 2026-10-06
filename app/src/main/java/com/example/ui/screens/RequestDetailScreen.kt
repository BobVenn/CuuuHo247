package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.firebase.FirebaseManager
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.components.LeafletMapMarker
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RatingReviewDialog
import com.example.ui.components.RealtimeStatusDashboard
import com.example.ui.components.RescueReceiptDialog
import com.example.ui.components.VietQrPaymentDialog
import com.example.ui.components.openGoogleMapsNavigation
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    request: RescueRequest,
    currentUserProfile: UserProfile?,
    onBack: () -> Unit,
    onCallPhone: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptRequestByStaff: (String) -> Unit,
    onUpdateStatusByStaff: (String, String, Long?) -> Unit,
    onSubmitRating: (rating: Int, comment: String) -> Unit,
    onSimulateRescueFlow: ((String) -> Unit)? = null,
    onInAppCall: ((name: String, role: String, phone: String, issue: String) -> Unit)? = null,
    onNavigateToCoordinates: ((lat: Double, lng: Double, label: String) -> Unit)? = null,
    onUpdatePaymentStatus: ((requestId: String, paymentStatus: String) -> Unit)? = null,
    onShareTrackingWithFamily: ((RescueRequest) -> Unit)? = null
) {
    val context = LocalContext.current
    var showRatingDialog by remember { mutableStateOf(false) }
    var showVietQrDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showUpdateCostDialog by remember { mutableStateOf(false) }
    var costInput by remember { mutableStateOf(request.cost?.toString() ?: "250000") }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale("vi", "VN")) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    val isStaffOrAdmin = currentUserProfile?.role == AppConfig.UserRole.STAFF ||
            currentUserProfile?.role == AppConfig.UserRole.ADMIN

    val isUserOwner = currentUserProfile?.uid == request.userId

    // Status Stepper Index
    val statusSteps = listOf(
        AppConfig.RequestStatus.PENDING,
        AppConfig.RequestStatus.ACCEPTED,
        AppConfig.RequestStatus.EN_ROUTE,
        AppConfig.RequestStatus.ARRIVED,
        AppConfig.RequestStatus.IN_PROGRESS,
        AppConfig.RequestStatus.COMPLETED
    )
    val currentStepIndex = statusSteps.indexOf(request.status).takeIf { it >= 0 } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Chi Tiết Yêu Cầu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Mã đơn: ${request.id.takeLast(8)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiến trình xử lý sự cố", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (request.status) {
                                AppConfig.RequestStatus.COMPLETED -> SafeGreen
                                AppConfig.RequestStatus.CANCELLED -> AlertRed
                                else -> RescuePrimary
                            }
                        ) {
                            Text(
                                text = request.status,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (request.status != AppConfig.RequestStatus.CANCELLED) {
                        statusSteps.forEachIndexed { index, step ->
                            val isDone = index < currentStepIndex
                            val isCurrent = index == currentStepIndex

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        isDone -> SafeGreen
                                        isCurrent -> RescuePrimary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        } else if (isCurrent) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Text("${index + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Yêu cầu cứu hộ này đã bị hủy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlertRed
                        )
                    }
                }
            }

            // Incident Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Thông Tin Sự Cố", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    HorizontalDivider()

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                        Text("Sự cố: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(request.issueType, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RescuePrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                        Text("Phương tiện: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("${request.vehicleType} • ${request.licensePlate}", fontSize = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Column {
                            Text("Vị trí: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(request.address, fontSize = 14.sp)
                            if (request.latitude != 0.0 && request.longitude != 0.0) {
                                Text("Tọa độ GPS: ${request.latitude}, ${request.longitude}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (request.description.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                            Column {
                                Text("Mô tả của người dùng: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(request.description, fontSize = 14.sp)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text("Thời gian tạo: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(dateFormat.format(Date(request.timestamp)), fontSize = 14.sp)
                    }

                    // Incident photo if provided by customer
                    if (!request.imageUrl.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                Text("Ảnh hiện trường sự cố:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                AsyncImage(
                                    model = request.imageUrl,
                                    contentDescription = "Ảnh hiện trường xe hỏng",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (request.cost != null && request.cost > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                            Text("Chi phí cứu hộ: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(currencyFormat.format(request.cost), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        }
                    }
                }
            }

            // THANH TOÁN VIETQR & TIỀN MẶT CARD
            if (request.cost != null && request.cost > 0) {
                val isPaid = request.paymentStatus.startsWith("PAID")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_info_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, if (isPaid) SafeGreen else EmergencyGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = if (isPaid) SafeGreen else EmergencyGold)
                                Text("THANH TOÁN CHI PHÍ", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPaid) SafeGreen else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = when (request.paymentStatus) {
                                        "PAID_VIETQR" -> "ĐÃ TT VIETQR ✓"
                                        "PAID_CASH" -> "ĐÃ TRẢ TIỀN MẶT ✓"
                                        else -> "CHƯA THANH TOÁN"
                                    },
                                    color = if (isPaid) Color.White else Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tổng tiền thanh toán:", fontSize = 11.5.sp, color = OnSurfaceVariantLight)
                                Text(
                                    text = currencyFormat.format(request.cost),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPaid) SafeGreen else AlertRed
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isPaid) {
                                    Button(
                                        onClick = { showVietQrDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("btn_open_vietqr_dialog")
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("QUÉT VIETQR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { showReceiptDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("btn_view_insurance_receipt")
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BIÊN LAI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Share tracking to relatives / friends
                        OutlinedButton(
                            onClick = {
                                if (onShareTrackingWithFamily != null) {
                                    onShareTrackingWithFamily(request)
                                } else {
                                    val trackingText = "🚨 Mình đang gặp sự cố xe cần cứu hộ!\n📍 Vị trí: ${request.address}\n🗺️ Bản đồ: https://maps.google.com/?q=${request.latitude},${request.longitude}\n🔧 KTV: ${request.staffName ?: "Đội Cứu Hộ 24/7"} (SĐT: ${request.staffPhone ?: AppConfig.DEFAULT_RESCUE_HOTLINE})\n🚗 Sự cố: ${request.issueType} (${request.licensePlate})"
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        putExtra(android.content.Intent.EXTRA_TEXT, trackingText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ hành trình cứu hộ cho người thân qua:"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderLight)
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chia Sẻ Hành Trình Cho Người Thân (Zalo/SMS)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceLight)
                        }
                    }
                }
            }

            // BẢN ĐỒ VỊ TRÍ SỰ CỐ & CHỈ ĐƯỜNG LÁI XE (Leaflet.js & OpenStreetMap)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("request_detail_map_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Bản Đồ Vị Trí Sự Cố",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RescuePrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.4f, %.4f", request.latitude, request.longitude)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RescuePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val detailMarker = remember(request.id, request.latitude, request.longitude) {
                        listOf(
                            LeafletMapMarker(
                                id = request.id,
                                title = "${request.issueType} • ${request.userName}",
                                snippet = request.address,
                                latitude = request.latitude,
                                longitude = request.longitude,
                                type = "REQUEST",
                                status = request.status
                            )
                        )
                    }

                    LeafletMapView(
                        centerLat = request.latitude,
                        centerLng = request.longitude,
                        centerAddress = request.address,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        markers = detailMarker,
                        onNavigateClick = { lat, lng, label ->
                            if (onNavigateToCoordinates != null) {
                                onNavigateToCoordinates(lat, lng, label)
                            } else {
                                openGoogleMapsNavigation(context, lat, lng, label)
                            }
                        }
                    )

                    // Big Navigation Button to drive directly to the user
                    Button(
                        onClick = {
                            if (onNavigateToCoordinates != null) {
                                onNavigateToCoordinates(request.latitude, request.longitude, request.address)
                            } else {
                                openGoogleMapsNavigation(context, request.latitude, request.longitude, request.address)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_navigate_to_requester"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHỈ ĐƯỜNG LÁI XE ĐẾN NƠI (GOOGLE MAPS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Customer / Technician Card
            if (!isStaffOrAdmin) {
                // User sees Technician info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kỹ Thuật Viên Phụ Trách", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (!request.staffName.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = RescuePrimary, modifier = Modifier.size(46.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                    Column {
                                        Text(request.staffName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(request.staffPhone ?: "Chưa có SĐT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // In-App Call Button
                                    FilledIconButton(
                                        onClick = {
                                            if (onInAppCall != null) {
                                                onInAppCall(
                                                    request.staffName ?: "Kỹ thuật viên",
                                                    "Kỹ thuật viên cứu hộ",
                                                    request.staffPhone ?: "",
                                                    "Cứu hộ: ${request.issueType}"
                                                )
                                            } else {
                                                onCallPhone(request.staffPhone ?: "")
                                            }
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeGreen)
                                    ) {
                                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Gọi KTV qua App", tint = Color.White)
                                    }

                                    // Chat Button
                                    FilledIconButton(
                                        onClick = { onOpenChat(request.id) },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = RescuePrimary)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Hệ thống đang điều phối nhân viên cứu hộ gần nhất...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Staff / Admin sees Customer info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Thông Tin Khách Hàng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(request.userName.ifBlank { "Khách hàng" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(request.userPhone.ifBlank { "Chưa cập nhật SĐT" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // In-App Call Button
                                FilledIconButton(
                                    onClick = {
                                        if (onInAppCall != null) {
                                            onInAppCall(
                                                request.userName.ifBlank { "Khách hàng" },
                                                "Khách hàng gặp sự cố",
                                                request.userPhone,
                                                "${request.issueType} • ${request.vehicleType}"
                                            )
                                        } else {
                                            onCallPhone(request.userPhone)
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = SafeGreen)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = "Gọi khách qua App", tint = Color.White)
                                }

                                // Chat Button
                                FilledIconButton(
                                    onClick = { onOpenChat(request.id) },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = RescuePrimary)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Nhắn tin", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Rating display if already rated
            if (request.rating != null && request.rating > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Đánh Giá Từ Khách Hàng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(request.rating) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (!request.ratingComment.isNullOrBlank()) {
                            Text("\"${request.ratingComment}\"", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // USER ACTION BUTTONS
            if (isUserOwner) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Chat button
                    Button(
                        onClick = { onOpenChat(request.id) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nhắn Tin")
                    }

                    // Rating button if completed
                    if (request.status == AppConfig.RequestStatus.COMPLETED && request.rating == null) {
                        Button(
                            onClick = { showRatingDialog = true },
                            modifier = Modifier.weight(1.2f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đánh Giá 5★")
                        }
                    }

                    // Cancel button if not completed
                    if (request.status == AppConfig.RequestStatus.PENDING || request.status == AppConfig.RequestStatus.ACCEPTED) {
                        OutlinedButton(
                            onClick = { showCancelConfirmDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                        ) {
                            Text("Hủy Đơn")
                        }
                    }
                }
            }

            // BẢNG ĐIỀU PHỐI TIẾN ĐỘ DÀNH RIÊNG CHO KỸ THUẬT VIÊN
            if (isStaffOrAdmin) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_interactive_simulation"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, EmergencyGold.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(18.dp))
                                Text("Bảng Điều Phối Cứu Hộ (KTV)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmergencyGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Dành Cho KTV",
                                    color = Color(0xFFB45309),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Cập nhật tiến độ xử lý hiện trường cho đơn hàng này:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAcceptRequestByStaff(request.id) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = request.status == AppConfig.RequestStatus.PENDING
                            ) {
                                Text("1. Nhận Đơn", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.EN_ROUTE, request.cost) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = request.status == AppConfig.RequestStatus.ACCEPTED
                            ) {
                                Text("2. Đang Đến", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.ARRIVED, request.cost) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = request.status == AppConfig.RequestStatus.EN_ROUTE
                            ) {
                                Text("3. Đến Nơi", fontSize = 10.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.IN_PROGRESS, request.cost) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = request.status == AppConfig.RequestStatus.ARRIVED
                            ) {
                                Text("4. Đang Xử Lý", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.COMPLETED, 250000L) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = request.status != AppConfig.RequestStatus.COMPLETED && request.status != AppConfig.RequestStatus.CANCELLED
                            ) {
                                Text("5. Hoàn Tất", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirm Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Hủy Yêu Cầu Cứu Hộ?") },
            text = { Text("Bạn có chắc chắn muốn hủy đơn cứu hộ này? Thông tin sẽ được cập nhật trực tiếp lên hệ thống.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        onCancelRequest(request.id)
                    }
                ) {
                    Text("Xác nhận hủy", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) { Text("Đóng") }
            }
        )
    }

    // Rating Dialog with 5-Stars & Quick Review Tags
    if (showRatingDialog) {
        RatingReviewDialog(
            staffName = request.staffName,
            issueType = request.issueType,
            onDismiss = { showRatingDialog = false },
            onSubmit = { rating, comment ->
                showRatingDialog = false
                onSubmitRating(rating, comment)
            }
        )
    }

    // Update Cost & Complete Dialog (Staff)
    if (showUpdateCostDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateCostDialog = false },
            title = { Text("Xác Nhận Hoàn Thành & Báo Giá") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhập chi phí thực tế thu của khách hàng (VND):")
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Chi phí thực tế (VND)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "💡 Hệ thống sẽ tự động tạo mã VietQR Napas để khách hàng quét trả tiền tức thì qua Banking hoặc trả tiền mặt.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateCostDialog = false
                        val cost = costInput.toLongOrNull() ?: 250000L
                        onUpdateStatusByStaff(request.id, AppConfig.RequestStatus.COMPLETED, cost)
                        showVietQrDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Xác Nhận & Tạo VietQR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateCostDialog = false }) { Text("Hủy") }
            }
        )
    }

    // VietQR Payment Dialog (Napas standard QR code generation)
    if (showVietQrDialog && (request.cost ?: 0L) > 0) {
        VietQrPaymentDialog(
            requestId = request.id,
            cost = request.cost ?: 250000L,
            issueType = request.issueType,
            isTechnicianView = isStaffOrAdmin,
            onDismiss = { showVietQrDialog = false },
            onConfirmPayment = { method ->
                showVietQrDialog = false
                val newStatus = if (method == "VIETQR") "PAID_VIETQR" else "PAID_CASH"
                onUpdatePaymentStatus?.invoke(request.id, newStatus)
            }
        )
    }

    // Rescue Receipt & Insurance Reimbursement Dialog
    if (showReceiptDialog) {
        RescueReceiptDialog(
            request = request,
            onDismiss = { showReceiptDialog = false }
        )
    }
}
