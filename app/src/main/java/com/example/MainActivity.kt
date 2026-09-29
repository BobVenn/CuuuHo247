package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest
import com.example.ui.screens.*
import com.example.ui.theme.AlertRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RescuePrimary
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.RescueRealtimeViewModel

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
    HISTORY("Lịch sử"),
    STAFF("Điều phối"),
    REPORTS("Báo cáo"),
    PROFILE("Cá nhân")
}

sealed class AppScreen {
    object MainTabs : AppScreen()
    data class RequestDetail(val request: RescueRequest) : AppScreen()
    data class Chat(val request: RescueRequest) : AppScreen()
}

@Composable
fun MainAppHost(
    authViewModel: AuthViewModel = viewModel(),
    rescueViewModel: RescueRealtimeViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    val currentLocation by rescueViewModel.currentLocation.collectAsStateWithLifecycle()
    val isLocating by rescueViewModel.isLocating.collectAsStateWithLifecycle()

    val userRequests by rescueViewModel.userRequests.collectAsStateWithLifecycle()
    val allRequests by rescueViewModel.allRequests.collectAsStateWithLifecycle()
    val activeRequest by rescueViewModel.activeRequest.collectAsStateWithLifecycle()
    val chatMessages by rescueViewModel.chatMessages.collectAsStateWithLifecycle()
    val userRatings by rescueViewModel.userRatings.collectAsStateWithLifecycle()
    val userReports by rescueViewModel.userReports.collectAsStateWithLifecycle()
    val statusMessage by rescueViewModel.statusMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainNavTab.HOME) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            rescueViewModel.clearStatusMessage()
        }
    }

    // Permission launcher for Location and Phone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            rescueViewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    // If not authenticated, show AuthScreen (Login / Register)
    if (currentUser == null) {
        AuthScreen(
            authViewModel = authViewModel,
            onAuthSuccess = {
                currentTab = MainNavTab.HOME
                currentScreen = AppScreen.MainTabs
            }
        )
        return
    }

    // Handle Sub-screens (Detail & Chat)
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
                }
            )
            return
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
                }
            )
            return
        }

        AppScreen.MainTabs -> {
            // Main Bottom Navigation Flow
            val isStaffOrAdmin = userProfile?.role == AppConfig.UserRole.STAFF ||
                    userProfile?.role == AppConfig.UserRole.ADMIN

            if (currentTab != MainNavTab.HOME) {
                BackHandler {
                    currentTab = MainNavTab.HOME
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_bottom_nav"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        // Home Tab
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

                        // History Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.HISTORY,
                            onClick = { currentTab = MainNavTab.HISTORY },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "Lịch sử"
                                )
                            },
                            label = {
                                Text(
                                    MainNavTab.HISTORY.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        // Staff Dispatch Tab (Only for Staff or Admin)
                        if (isStaffOrAdmin) {
                            val pendingCount = allRequests.count { it.status == AppConfig.RequestStatus.PENDING }
                            NavigationBarItem(
                                selected = currentTab == MainNavTab.STAFF,
                                onClick = { currentTab = MainNavTab.STAFF },
                                icon = {
                                    BadgedBox(badge = {
                                        if (pendingCount > 0) {
                                            Badge(containerColor = AlertRed) {
                                                Text("$pendingCount")
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (currentTab == MainNavTab.STAFF) Icons.Filled.Build else Icons.Outlined.Build,
                                            contentDescription = "Điều phối"
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        MainNavTab.STAFF.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (currentTab == MainNavTab.STAFF) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }

                        // Reports Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.REPORTS,
                            onClick = { currentTab = MainNavTab.REPORTS },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.REPORTS) Icons.Filled.Feedback else Icons.Outlined.Feedback,
                                    contentDescription = "Báo cáo"
                                )
                            },
                            label = {
                                Text(
                                    MainNavTab.REPORTS.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == MainNavTab.REPORTS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )

                        // Profile Tab
                        NavigationBarItem(
                            selected = currentTab == MainNavTab.PROFILE,
                            onClick = { currentTab = MainNavTab.PROFILE },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Cá nhân"
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
                                activeRequest = activeRequest,
                                recentRequests = userRequests,
                                onRefreshLocation = { rescueViewModel.refreshLocation() },
                                onEmergencySosCall = {
                                    rescueViewModel.triggerEmergencySosDialer(context, AppConfig.DEFAULT_RESCUE_HOTLINE)
                                },
                                onCreateRequest = { issue, desc, vType, plate ->
                                    userProfile?.let { prof ->
                                        rescueViewModel.createRescueRequest(
                                            userProfile = prof,
                                            issueType = issue,
                                            description = desc,
                                            vehicleType = vType,
                                            licensePlate = plate,
                                            onSuccess = { reqId ->
                                                val createdReq = userRequests.find { it.id == reqId }
                                                if (createdReq != null) {
                                                    currentScreen = AppScreen.RequestDetail(createdReq)
                                                }
                                            }
                                        )
                                    }
                                },
                                onRequestClick = { req ->
                                    currentScreen = AppScreen.RequestDetail(req)
                                },
                                onNavigateToHistory = { currentTab = MainNavTab.HISTORY },
                                onNavigateToReports = { currentTab = MainNavTab.REPORTS },
                                onShareLocation = {
                                    rescueViewModel.shareLocation(
                                        context,
                                        currentLocation.address,
                                        currentLocation.latitude,
                                        currentLocation.longitude
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

                        MainNavTab.STAFF -> {
                            StaffDashboardScreen(
                                userProfile = userProfile,
                                allRequests = allRequests,
                                onRequestClick = { req ->
                                    currentScreen = AppScreen.RequestDetail(req)
                                },
                                onAcceptRequest = { id ->
                                    userProfile?.let { rescueViewModel.acceptRequestByStaff(id, it) }
                                }
                            )
                        }

                        MainNavTab.REPORTS -> {
                            ReportsScreen(
                                userProfile = userProfile,
                                reports = userReports,
                                onBack = { currentTab = MainNavTab.HOME },
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

                        MainNavTab.PROFILE -> {
                            ProfileScreen(
                                userProfile = userProfile,
                                myRatings = userRatings,
                                onUpdateProfile = { name, phone, vType, vName, plate, role ->
                                    authViewModel.updateProfile(name, phone, vType, vName, plate, role)
                                },
                                onNavigateToHistory = { currentTab = MainNavTab.HISTORY },
                                onNavigateToReports = { currentTab = MainNavTab.REPORTS },
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
}
