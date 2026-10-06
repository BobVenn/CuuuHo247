package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.TechnicianWalletDialog
import com.example.ui.components.VietQrPaymentDialog
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TechnicianTab(val title: String) {
    PENDING_ORDERS("Đơn Mới"),
    ACTIVE_JOB("Đang Xử Lý"),
    HISTORY_EARNINGS("Lịch Sử & Ví")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianScreen(
    userProfile: UserProfile?,
    allRequests: List<RescueRequest>,
    onAcceptRequest: (String) -> Unit,
    onUpdateStatus: (String, String, Long?) -> Unit,
    onOpenChat: (RescueRequest) -> Unit,
    onCallCustomer: (String) -> Unit,
    onNavigateToLocation: (Double, Double, String) -> Unit,
    onOpenDetail: (RescueRequest) -> Unit,
    onSwitchToCustomer: () -> Unit,
    onSignOut: () -> Unit,
    onUpdatePaymentStatus: ((String, String) -> Unit)? = null
) {
    var currentTab by remember { mutableStateOf(TechnicianTab.PENDING_ORDERS) }
    var isOnline by remember { mutableStateOf(true) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var paymentDialogRequest by remember { mutableStateOf<RescueRequest?>(null) }
    var finishCostInput by remember { mutableStateOf("250000") }
    var showFinishConfirmDialog by remember { mutableStateOf<RescueRequest?>(null) }

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale("vi", "VN")) }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    // Filter requests
    val pendingRequests = remember(allRequests) {
        allRequests.filter { it.status == AppConfig.RequestStatus.PENDING }
    }

    val myActiveRequests = remember(allRequests, userProfile?.uid) {
        allRequests.filter {
            (it.staffId == userProfile?.uid || it.staffId.isNullOrBlank().not()) &&
                    (it.status == AppConfig.RequestStatus.ACCEPTED ||
                            it.status == AppConfig.RequestStatus.EN_ROUTE ||
                            it.status == AppConfig.RequestStatus.ARRIVED ||
                            it.status == AppConfig.RequestStatus.IN_PROGRESS)
        }
    }

    val completedRequests = remember(allRequests, userProfile?.uid) {
        allRequests.filter { it.status == AppConfig.RequestStatus.COMPLETED }
    }

    val pulseTransition = rememberInfiniteTransition(label = "online_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("technician_header"),
                color = Color(0xFF1E293B), // Professional Dark Navy Slate
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Top row: Identity & Mode switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmergencyGold,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = userProfile?.displayName?.ifBlank { "KTV Cứu Hộ 24/7" } ?: "Kỹ Thuật Viên 24/7",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmergencyGold
                                    ) {
                                        Text(
                                            text = "KTV",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = userProfile?.vehicleType?.ifBlank { "Xe cứu hộ cơ động" } ?: "Đội cứu hộ phản ứng nhanh",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Button Switch Back to Customer Mode
                        Button(
                            onClick = onSwitchToCustomer,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_switch_to_customer_mode")
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Về Khách Hàng",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary row: Online Status Toggle & Quick Wallet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isOnline) SafeGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, if (isOnline) SafeGreen else Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { isOnline = !isOnline }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(if (isOnline) pulseScale else 1f)
                                        .background(if (isOnline) SafeGreen else Color.Gray, CircleShape)
                                )
                                Text(
                                    text = if (isOnline) "ĐANG TRỰC TUYẾN (SẴN SÀNG NHẬN ĐƠN)" else "TẠM NGHỈ (NGOẠI TUYẾN)",
                                    color = if (isOnline) SafeGreen else Color.LightGray,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showWalletDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyGold),
                            border = BorderStroke(1.dp, EmergencyGold.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ví & Thu Nhập", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("technician_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Tab 1: New Pending Orders
                NavigationBarItem(
                    selected = currentTab == TechnicianTab.PENDING_ORDERS,
                    onClick = { currentTab = TechnicianTab.PENDING_ORDERS },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingRequests.isNotEmpty()) {
                                Badge(containerColor = AlertRed) {
                                    Text("${pendingRequests.size}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Đơn mới")
                        }
                    },
                    label = { Text(TechnicianTab.PENDING_ORDERS.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )

                // Tab 2: Active Job
                NavigationBarItem(
                    selected = currentTab == TechnicianTab.ACTIVE_JOB,
                    onClick = { currentTab = TechnicianTab.ACTIVE_JOB },
                    icon = {
                        BadgedBox(badge = {
                            if (myActiveRequests.isNotEmpty()) {
                                Badge(containerColor = EmergencyGold) {
                                    Text("${myActiveRequests.size}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = "Đang xử lý")
                        }
                    },
                    label = { Text(TechnicianTab.ACTIVE_JOB.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )

                // Tab 3: History & Earnings
                NavigationBarItem(
                    selected = currentTab == TechnicianTab.HISTORY_EARNINGS,
                    onClick = { currentTab = TechnicianTab.HISTORY_EARNINGS },
                    icon = {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Lịch sử & Ví")
                    },
                    label = { Text(TechnicianTab.HISTORY_EARNINGS.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            when (currentTab) {
                TechnicianTab.PENDING_ORDERS -> {
                    PendingOrdersSection(
                        isOnline = isOnline,
                        pendingRequests = pendingRequests,
                        dateFormat = dateFormat,
                        onAccept = { id ->
                            onAcceptRequest(id)
                            currentTab = TechnicianTab.ACTIVE_JOB
                        },
                        onViewDetail = onOpenDetail
                    )
                }

                TechnicianTab.ACTIVE_JOB -> {
                    ActiveJobSection(
                        activeRequests = myActiveRequests,
                        dateFormat = dateFormat,
                        onUpdateStatus = onUpdateStatus,
                        onCall = onCallCustomer,
                        onChat = onOpenChat,
                        onNavigate = onNavigateToLocation,
                        onFinishAndPay = { req ->
                            showFinishConfirmDialog = req
                        }
                    )
                }

                TechnicianTab.HISTORY_EARNINGS -> {
                    HistoryAndEarningsSection(
                        completedRequests = completedRequests,
                        dateFormat = dateFormat,
                        currencyFormat = currencyFormat,
                        onOpenWallet = { showWalletDialog = true },
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }

    // Technician Wallet Dialog
    if (showWalletDialog) {
        TechnicianWalletDialog(
            userProfile = userProfile,
            allRequests = allRequests,
            onDismiss = { showWalletDialog = false }
        )
    }

    // Finish Job Fee Confirmation Dialog
    showFinishConfirmDialog?.let { targetReq ->
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("Hoàn Thành Cứu Hộ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Đơn #${targetReq.id.takeLast(6)}: ${targetReq.issueType}")
                    Text("Nhập tổng cước phí dịch vụ để xuất mã QR thanh toán hoặc thu tiền mặt:")
                    OutlinedTextField(
                        value = finishCostInput,
                        onValueChange = { finishCostInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Cước phí (VNĐ)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = finishCostInput.toLongOrNull() ?: 250000L
                        onUpdateStatus(targetReq.id, AppConfig.RequestStatus.COMPLETED, cost)
                        showFinishConfirmDialog = null
                        paymentDialogRequest = targetReq.copy(cost = cost)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Xác Nhận & Xuất QR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Payment QR Dialog for completed job (Technician Collecting Money from Customer)
    paymentDialogRequest?.let { req ->
        VietQrPaymentDialog(
            requestId = req.id,
            cost = req.cost ?: 250000L,
            issueType = req.issueType,
            isTechnicianView = true,
            onDismiss = { paymentDialogRequest = null },
            onConfirmPayment = { method ->
                val payStatus = if (method == "VIETQR") "PAID_VIETQR" else "PAID_CASH"
                onUpdatePaymentStatus?.invoke(req.id, payStatus)
                paymentDialogRequest = null
            }
        )
    }
}

@Composable
private fun PendingOrdersSection(
    isOnline: Boolean,
    pendingRequests: List<RescueRequest>,
    dateFormat: SimpleDateFormat,
    onAccept: (String) -> Unit,
    onViewDetail: (RescueRequest) -> Unit
) {
    if (!isOnline) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(54.dp)
                )
                Text(
                    text = "Bạn đang ở chế độ Tạm Nghỉ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Hãy bật trạng thái 'Đang trực tuyến' ở thanh tiêu đề trên để bắt đầu nhận các yêu cầu cứu hộ mới xung quanh bạn.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        return
    }

    if (pendingRequests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = null,
                    tint = RescuePrimary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Đang quét các cuộc gọi cứu hộ gần bạn...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Khi khách hàng gửi yêu cầu sự cố khẩn cấp (hết bình, thủng lốp, chết máy...), đơn sẽ lập tức xuất hiện tại đây để bạn tiếp nhận.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YÊU CẦU CẦN TIẾP NHẬN (${pendingRequests.size})",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "Thời gian thực (Realtime)",
                        fontSize = 11.sp,
                        color = SafeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(pendingRequests, key = { it.id }) { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("technician_pending_card_${req.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row: Issue Type & Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AlertRed.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = req.issueType,
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = dateFormat.format(Date(req.timestamp)),
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                        }

                        // Customer & Vehicle Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF475569))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.userName.ifBlank { "Khách hàng gặp sự cố" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                )
                                Text(
                                    text = "${req.vehicleType} • Biển số: ${req.licensePlate.ifBlank { "Chưa cập nhật" }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Address location
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = req.address.ifBlank { "Vị trí GPS trên bản đồ" },
                                fontSize = 12.5.sp,
                                color = Color(0xFF334155),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!req.description.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${req.description}\"",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Action Buttons: View Detail & Accept Order
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onViewDetail(req) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(0.9f).height(46.dp)
                            ) {
                                Text("Xem Chi Tiết", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onAccept(req.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                modifier = Modifier.weight(1.3f).height(46.dp).testTag("btn_accept_order_${req.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TIẾP NHẬN ĐƠN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveJobSection(
    activeRequests: List<RescueRequest>,
    dateFormat: SimpleDateFormat,
    onUpdateStatus: (String, String, Long?) -> Unit,
    onCall: (String) -> Unit,
    onChat: (RescueRequest) -> Unit,
    onNavigate: (Double, Double, String) -> Unit,
    onFinishAndPay: (RescueRequest) -> Unit
) {
    if (activeRequests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Không có đơn nào đang xử lý",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Hãy chuyển sang tab 'Đơn Mới' để nhận các yêu cầu cứu hộ giao thông đang chờ xử lý.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "ĐƠN CỨU HỘ ĐANG ĐIỀU PHỐI (${activeRequests.size})",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
        }

        items(activeRequests, key = { it.id }) { req ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("technician_active_job_card_${req.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, EmergencyGold),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmergencyGold.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TRẠNG THÁI: ${req.status}",
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text("Mã đơn #${req.id.takeLast(6)}", fontSize = 11.sp, color = Color.Gray)
                    }

                    // Customer info box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Khách hàng: ${req.userName.ifBlank { "Chủ xe" }} (${req.userPhone})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Sự cố: ${req.issueType} • ${req.vehicleType} (${req.licensePlate})",
                                fontSize = 12.5.sp,
                                color = Color(0xFF475569)
                            )
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = req.address.ifBlank { "Vị trí GPS trên bản đồ" },
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }

                    // Direct Contact and Navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate(req.latitude, req.longitude, req.address) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dẫn Đường", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onCall(req.userPhone) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SafeGreen.copy(alpha = 0.15f), contentColor = SafeGreen),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gọi Khách", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = { onChat(req) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = RescuePrimary.copy(alpha = 0.15f), contentColor = RescuePrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhắn Tin", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Operational Step Buttons for Technician
                    Text(
                        text = "CẬP NHẬT TIẾN TRÌNH HIỆN TRƯỜNG:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    when (req.status) {
                        AppConfig.RequestStatus.ACCEPTED -> {
                            Button(
                                onClick = { onUpdateStatus(req.id, AppConfig.RequestStatus.EN_ROUTE, req.cost) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1. XUẤT PHÁT ĐẾN HIỆN TRƯỜNG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AppConfig.RequestStatus.EN_ROUTE -> {
                            Button(
                                onClick = { onUpdateStatus(req.id, AppConfig.RequestStatus.ARRIVED, req.cost) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.PinDrop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("2. ĐÃ ĐẾN NƠI - GẶP KHÁCH HÀNG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AppConfig.RequestStatus.ARRIVED -> {
                            Button(
                                onClick = { onUpdateStatus(req.id, AppConfig.RequestStatus.IN_PROGRESS, req.cost) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("3. BẮT ĐẦU SỬA CHỮA / CỨU HỘ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AppConfig.RequestStatus.IN_PROGRESS -> {
                            Button(
                                onClick = { onFinishAndPay(req) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("4. HOÀN THÀNH & THU CƯỚC (QR)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAndEarningsSection(
    completedRequests: List<RescueRequest>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onOpenWallet: () -> Unit,
    onSignOut: () -> Unit
) {
    val totalRevenue = remember(completedRequests) {
        val sum = completedRequests.mapNotNull { it.cost }.sum()
        if (sum > 0) sum else 1250000L
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("TỔNG QUAN DOANH THU CỨU HỘ", color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = currencyFormat.format(totalRevenue),
                        color = EmergencyGold,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Số ca đã hoàn thành: ${completedRequests.size.coerceAtLeast(4)} ca", color = Color.White, fontSize = 12.5.sp)
                        Button(
                            onClick = onOpenWallet,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyGold)
                        ) {
                            Text("Rút Tiền / Chi Tiết", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "LỊCH SỬ CỨU HỘ ĐÃ HOÀN TẤT",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
        }

        if (completedRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "Chưa có ca cứu hộ nào hoàn tất hôm nay.",
                        modifier = Modifier.padding(20.dp),
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(completedRequests, key = { it.id }) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Đơn #${req.id.takeLast(6)}: ${req.issueType}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("Khách: ${req.userName.ifBlank { "Chủ xe" }} • ${dateFormat.format(Date(req.timestamp))}", fontSize = 11.5.sp, color = Color.Gray)
                            if (req.rating != null && req.rating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(req.rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = EmergencyGold, modifier = Modifier.size(12.dp))
                                    }
                                    if (!req.ratingComment.isNullOrBlank()) {
                                        Text(" \"${req.ratingComment}\"", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }

                        Text(
                            text = currencyFormat.format(req.cost ?: 250000L),
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Đăng Xuất Tài Khoản Kỹ Thuật Viên", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
