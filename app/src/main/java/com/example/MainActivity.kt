package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.data.model.UserProfile
import com.example.data.model.SavedVehicle
import com.example.ui.components.InAppCallDialog
import com.example.ui.components.InAppCallInfo
import com.example.ui.components.LocalizedStatusBanner
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.RescueRealtimeViewModel
import com.example.util.RescueNotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

enum class MainNavTab(val title: String) {
    HOME("Trang chủ"),
    SERVICES("Dịch vụ"),
    HISTORY("Lịch sử"),
    PROFILE("Cá nhân")
}

sealed class AppScreen {
    object MainTabs : AppScreen()
    data class RequestDetail(val request: RescueRequest) : AppScreen()
    data class Chat(val request: RescueRequest) : AppScreen()
    object Reports : AppScreen()
}

@Composable
fun MainAppHost(
    authViewModel: AuthViewModel = viewModel(),
    rescueViewModel: RescueRealtimeViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLocalSessionActive by authViewModel.isLocalSessionActive.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    val currentLocation by rescueViewModel.currentLocation.collectAsStateWithLifecycle()
    val isLocating by rescueViewModel.isLocating.collectAsStateWithLifecycle()
    val hasFineLocationPermission by rescueViewModel.hasFineLocationPermission.collectAsStateWithLifecycle()
    val isRealtimeTrackingActive by rescueViewModel.isRealtimeTrackingActive.collectAsStateWithLifecycle()

    val userRequests by rescueViewModel.userRequests.collectAsStateWithLifecycle()
    val allRequests by rescueViewModel.allRequests.collectAsStateWithLifecycle()
    val activeRequest by rescueViewModel.activeRequest.collectAsStateWithLifecycle()
    val chatMessages by rescueViewModel.chatMessages.collectAsStateWithLifecycle()
    val userRatings by rescueViewModel.userRatings.collectAsStateWithLifecycle()
    val userReports by rescueViewModel.userReports.collectAsStateWithLifecycle()
    val savedVehicles by rescueViewModel.savedVehicles.collectAsStateWithLifecycle()
    val statusMessage by rescueViewModel.statusMessage.collectAsStateWithLifecycle()
    val newRescueAlert by rescueViewModel.newRescueAlertEvent.collectAsStateWithLifecycle()
    val statusChangeEvent by rescueViewModel.statusChangeEvent.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var activeInAppCall by remember { mutableStateOf<InAppCallInfo?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            rescueViewModel.clearStatusMessage()
        }
    }

    // Check if app was opened from notification
    LaunchedEffect(Unit) {
        val notifReqId = (context as? ComponentActivity)?.intent?.getStringExtra(RescueNotificationHelper.EXTRA_REQUEST_ID)
        if (!notifReqId.isNullOrBlank()) {
            val matchedReq = userRequests.find { it.id == notifReqId }
                ?: allRequests.find { it.id == notifReqId }
            if (matchedReq != null) {
                currentScreen = AppScreen.RequestDetail(matchedReq)
            }
        }
    }

    // Permission launcher for Fine Location, Coarse Location, Phone, and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            rescueViewModel.updateLocationPermissionStatus(fineGranted)
        }
    }

    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CALL_PHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    // If not authenticated, show AuthScreen (Login / Register)
    if (currentUser == null && !isLocalSessionActive && userProfile == null) {
        AuthScreen(
            authViewModel = authViewModel,
            onAuthSuccess = {
                currentTab = MainNavTab.HOME
                currentScreen = AppScreen.MainTabs
            }
        )
        return
    }

    // Wrap whole app in Box to allow floating global overlays (Status Banner & In-App Call)
    Box(modifier = Modifier.fillMaxSize()) {
        // Handle Sub-screens (Detail, Chat, Reports) and MainTabs
        when (val screen = currentScreen) {
            is AppScreen.RequestDetail -> {
                BackHandler {
                    currentScreen = AppScreen.MainTabs
                }
                RequestDetailScreen(
                    request = screen.request,
                    currentUserProfile = userProfile,
                    onBack = { currentScreen = AppScreen.MainTabs },
                    onCallPhone = { phone -> rescueViewModel.triggerEmergencySosDialer(context, phone) },
                    onOpenChat = {
                        rescueViewModel.observeChat(screen.request.id)
                        currentScreen = AppScreen.Chat(screen.request)
                    },
                    onCancelRequest = { id ->
                        rescueViewModel.cancelRequest(id)
                        currentScreen = AppScreen.MainTabs
                    },
                    onAcceptRequestByStaff = { id ->
                        userProfile?.let { rescueViewModel.acceptRequestByStaff(id, it) }
                    },
                    onUpdateStatusByStaff = { id, status, cost ->
                        rescueViewModel.updateRequestStatus(id, status, cost)
                    },
                    onSubmitRating = { rating, comment ->
                        userProfile?.let {
                            rescueViewModel.submitRating(
                                requestId = screen.request.id,
                                staffId = screen.request.staffId ?: "",
                                staffName = screen.request.staffName ?: "Kỹ thuật viên",
                                rating = rating,
                                comment = comment,
                                userProfile = it
                            )
                        }
                    },
                    onSimulateRescueFlow = { id ->
                        rescueViewModel.simulateFullRescueWorkflow(id)
                    },
                    onInAppCall = { name, role, phone, issue ->
                        activeInAppCall = InAppCallInfo(
                            recipientName = name,
                            recipientRole = role,
                            recipientPhone = phone,
                            issueOrVehicle = issue
                        )
                    },
                    onNavigateToCoordinates = { lat, lng, label ->
                        rescueViewModel.launchNavigation(context, lat, lng, label)
                    },
                    onUpdatePaymentStatus = { id, paymentStatus ->
                        rescueViewModel.updatePaymentStatus(id, paymentStatus)
                    }
                )
            }

            is AppScreen.Chat -> {
                BackHandler {
                    currentScreen = AppScreen.RequestDetail(screen.request)
                }
                ChatScreen(
                    request = screen.request,
                    currentUserProfile = userProfile,
                    chatMessages = chatMessages,
                    onBack = { currentScreen = AppScreen.RequestDetail(screen.request) },
                    onSendMessage = { text ->
                        userProfile?.let {
                            val receiverId = if (it.uid == screen.request.userId) {
                                screen.request.staffId ?: ""
                            } else {
                                screen.request.userId
                            }
                            rescueViewModel.sendChatMessage(screen.request.id, text, it, receiverId)
                        }
                    },
                    onCallUser = {
                        val isCust = userProfile?.uid == screen.request.userId
                        val targetName = if (isCust) screen.request.staffName ?: "Kỹ thuật viên cứu hộ" else screen.request.userName.ifBlank { "Khách hàng" }
                        val targetRole = if (isCust) "Kỹ thuật viên cứu hộ" else "Khách hàng gặp sự cố"
                        val targetPhone = if (isCust) screen.request.staffPhone ?: "" else screen.request.userPhone
                        activeInAppCall = InAppCallInfo(
                            recipientName = targetName,
                            recipientRole = targetRole,
                            recipientPhone = targetPhone,
                            issueOrVehicle = "Đơn cứu hộ #${screen.request.id.takeLast(6)}: ${screen.request.issueType}"
                        )
                    }
                )
            }

            is AppScreen.Reports -> {
                BackHandler {
                    currentScreen = AppScreen.MainTabs
                }
                ReportsScreen(
                    userProfile = userProfile,
                    reports = userReports,
                    onBack = { currentScreen = AppScreen.MainTabs },
                    onSubmitReport = { type, content ->
                        userProfile?.let {
                            rescueViewModel.submitReport(
                                requestId = null,
                                reportType = type,
                                content = content,
                                userProfile = it
                            )
                        }
                    }
                )
            }

        AppScreen.MainTabs -> {
            if (currentTab != MainNavTab.HOME) {
                BackHandler {
                    currentTab = MainNavTab.HOME
                }
            }

            val hotlinePulseTransition = rememberInfiniteTransition(label = "hotline_pulse")
            val hotlineScale by hotlinePulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "hotline_scale"
            )

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                topBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_app_header"),
                        color = Color(0xFF0A2540),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RescuePrimary,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.DirectionsCar,
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
                                            text = "CỨU HỘ GIAO THÔNG 24/7",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SafeGreen
                                        ) {
                                            Text(
                                                text = "24/7",
                                                color = Color.White,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Tổng đài hỗ trợ: ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Quick-Dial Emergency Hotline (0898 212 031) with Pulse Animation
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${AppConfig.DEFAULT_RESCUE_HOTLINE}")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .scale(hotlineScale)
                                    .testTag("btn_top_quick_dial_hotline")
                            ) {
                                Icon(
                                    Icons.Default.PhoneInTalk,
                                    contentDescription = "Gọi Hotline ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GỌI SOS",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        // 1. Home Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.HOME,
                            onClick = { currentTab = MainNavTab.HOME },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Trang chủ",
                                    tint = if (currentTab == MainNavTab.HOME) RescuePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    MainNavTab.HOME.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.HOME) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        // 2. Services Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.SERVICES,
                            onClick = { currentTab = MainNavTab.SERVICES },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.SERVICES) Icons.Filled.Build else Icons.Outlined.Build,
                                    contentDescription = "Dịch vụ",
                                    tint = if (currentTab == MainNavTab.SERVICES) RescuePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    MainNavTab.SERVICES.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.SERVICES) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        // 3. History Tab
                        val activeOrdersCount = userRequests.count {
                            it.status != AppConfig.RequestStatus.COMPLETED && it.status != AppConfig.RequestStatus.CANCELLED
                        }
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.HISTORY,
                            onClick = { currentTab = MainNavTab.HISTORY },
                            icon = {
                                BadgedBox(badge = {
                                    if (activeOrdersCount > 0) {
                                        Badge(containerColor = AlertRed) {
                                            Text("$activeOrdersCount")
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (currentTab == MainNavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "Lịch sử",
                                        tint = if (currentTab == MainNavTab.HISTORY) RescuePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            label = {
                                Text(
                                    MainNavTab.HISTORY.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        // 4. Profile Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.PROFILE,
                            onClick = { currentTab = MainNavTab.PROFILE },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Cá nhân",
                                    tint = if (currentTab == MainNavTab.PROFILE) RescuePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    MainNavTab.PROFILE.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainNavTab.HOME -> {
                            HomeScreen(
                                userProfile = userProfile,
                                currentLocation = currentLocation,
                                isLocating = isLocating,
                                hasFineLocationPermission = hasFineLocationPermission,
                                isRealtimeTrackingActive = isRealtimeTrackingActive,
                                onRequestLocationPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onToggleRealtimeTracking = { rescueViewModel.toggleRealtimeTracking() },
                                activeRequest = activeRequest,
                                recentRequests = userRequests,
                                onRefreshLocation = { rescueViewModel.refreshLocation() },
                                onEmergencySosCall = {
                                    activeInAppCall = InAppCallInfo(
                                        recipientName = "Tổng Đài Cứu Hộ 24/7",
                                        recipientRole = "Đội Phản Ứng Nhanh Khẩn Cấp",
                                        recipientPhone = AppConfig.DEFAULT_RESCUE_HOTLINE,
                                        issueOrVehicle = "Cứu hộ khẩn cấp trên đường"
                                    )
                                },
                                onCreateRequest = { issue, desc, vType, plate, img ->
                                    val prof = userProfile ?: UserProfile(displayName = "Khách hàng")
                                    rescueViewModel.createRescueRequest(
                                        userProfile = prof,
                                        issueType = issue,
                                        description = desc,
                                        vehicleType = vType,
                                        licensePlate = plate,
                                        imageUrl = img,
                                        onSuccess = { createdReq ->
                                            currentScreen = AppScreen.RequestDetail(createdReq)
                                        }
                                    )
                                },
                                onRequestClick = { req ->
                                    currentScreen = AppScreen.RequestDetail(req)
                                },
                                onNavigateToHistory = { currentTab = MainNavTab.HISTORY },
                                onNavigateToReports = { currentTab = MainNavTab.PROFILE },
                                onShareLocation = {
                                    rescueViewModel.shareLocation(
                                        context,
                                        currentLocation.address,
                                        currentLocation.latitude,
                                        currentLocation.longitude
                                    )
                                },
                                onUpdateStatus = { id, st ->
                                    rescueViewModel.updateRequestStatus(id, st)
                                }
                            )
                        }

                        MainNavTab.SERVICES -> {
                            ServicesScreen(
                                userProfile = userProfile,
                                currentLocation = currentLocation,
                                savedVehicles = savedVehicles,
                                onRefreshLocation = { rescueViewModel.refreshLocation() },
                                onCallPhone = { phone -> rescueViewModel.triggerEmergencySosDialer(context, phone) },
                                onSubmitRequest = { issue, desc, vType, plate, img ->
                                    val prof = userProfile ?: UserProfile(displayName = "Khách hàng")
                                    rescueViewModel.createRescueRequest(
                                        userProfile = prof,
                                        issueType = issue,
                                        description = desc,
                                        vehicleType = vType,
                                        licensePlate = plate,
                                        imageUrl = img,
                                        onSuccess = { createdReq ->
                                            currentScreen = AppScreen.RequestDetail(createdReq)
                                        }
                                    )
                                }
                            )
                        }

                        MainNavTab.HISTORY -> {
                            HistoryScreen(
                                requests = userRequests,
                                onRequestClick = { req ->
                                    currentScreen = AppScreen.RequestDetail(req)
                                }
                            )
                        }

                        MainNavTab.PROFILE -> {
                            ProfileScreen(
                                userProfile = userProfile,
                                myRatings = userRatings,
                                savedVehicles = savedVehicles,
                                onAddVehicle = { name, vType, plate, insp, insu ->
                                    rescueViewModel.addSavedVehicle(name, vType, plate, insp, insu)
                                },
                                onRemoveVehicle = { id ->
                                    rescueViewModel.removeSavedVehicle(id)
                                },
                                onUpdateProfile = { name, phone, vType, vName, plate, role ->
                                    authViewModel.updateProfile(name, phone, vType, vName, plate, role)
                                },
                                onNavigateToHistory = { currentTab = MainNavTab.HISTORY },
                                onNavigateToReports = { currentScreen = AppScreen.Reports },
                                onSignOut = {
                                    authViewModel.signOut()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Active In-App Call Dialog (Single source of truth across all screens)
        activeInAppCall?.let { callInfo ->
            InAppCallDialog(
                callInfo = callInfo,
                onDismiss = { activeInAppCall = null },
                onFallbackPhoneCall = { phone ->
                    rescueViewModel.triggerEmergencySosDialer(context, phone)
                }
            )
        }

        // Localized In-App Heads-Up Status Banner (Floats above ALL screens)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 4.dp)
                .align(Alignment.TopCenter)
        ) {
            LocalizedStatusBanner(
                event = statusChangeEvent,
                onDismiss = { rescueViewModel.clearStatusChangeEvent() },
                onViewDetail = {
                    statusChangeEvent?.let { evt ->
                        currentScreen = AppScreen.RequestDetail(evt.request)
                        rescueViewModel.clearStatusChangeEvent()
                    }
                },
                onCallTechnician = { phone ->
                    rescueViewModel.triggerEmergencySosDialer(context, phone)
                }
            )
        }
    }
}
