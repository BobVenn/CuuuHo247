package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfig
import com.example.data.model.*
import com.example.ui.components.CreateRequestDialog
import com.example.ui.components.EmergencyHandbookDialog
import com.example.ui.components.RescueOpenStreetMap
import com.example.ui.theme.*
import com.example.util.UserLocationInfo
import java.text.NumberFormat
import java.util.Locale

data class StreamlinedServiceItem(
    val id: String,
    val title: String,
    val category: String, // "CAR", "MOTORBIKE", "TOW"
    val priceDisplay: String,
    val estimatedPrice: Long,
    val icon: ImageVector,
    val description: String,
    val actionText: String = "Đặt cứu hộ"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    userProfile: UserProfile?,
    currentLocation: UserLocationInfo,
    savedVehicles: List<SavedVehicle> = emptyList(),
    onRefreshLocation: (() -> Unit)? = null,
    onCallPhone: (String) -> Unit,
    onSubmitRequest: (issueType: String, description: String, vehicleType: String, licensePlate: String, imageUrl: String?) -> Unit
) {
    var selectedMainTab by remember { mutableIntStateOf(0) } // 0: Bảng giá dịch vụ, 1: Cẩm nang & Đèn táp-lô
    var handbookSubTab by remember { mutableIntStateOf(0) } // 0: Kỹ năng, 1: Đèn Taplo, 2: Hotline
    var expandedGuideId by remember { mutableStateOf<String?>(RescueDataCatalog.handbookGuides.firstOrNull()?.id) }

    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedServiceForDialog by remember { mutableStateOf<StreamlinedServiceItem?>(null) }
    var selectedServiceDetail by remember { mutableStateOf<StreamlinedServiceItem?>(null) }
    var showHandbookDialog by remember { mutableStateOf(false) }

    val services = remember {
        listOf(
            StreamlinedServiceItem(
                id = "BATTERY_JUMP",
                title = "Kích bình ắc quy ô tô",
                category = "CAR",
                priceDisplay = "250.000đ",
                estimatedPrice = 250000,
                icon = Icons.Default.Bolt,
                description = "Kích nổ bình điện khẩn cấp cho xe 4-7 chỗ, SUV, bán tải. Đo kiểm điện áp và tình trạng sạc máy phát miễn phí.",
                actionText = "Đặt cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "TIRE_PUNCTURE",
                title = "Vá lốp & Thay bánh sơ cua",
                category = "CAR",
                priceDisplay = "250.000đ",
                estimatedPrice = 250000,
                icon = Icons.Default.Settings,
                description = "Vá dùi, vá nấm hoặc thay bánh phụ sơ cua ô tô lưu động. Trang bị kích thủy lực an toàn không móp gầm xe.",
                actionText = "Đặt cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "TOW_TRUCK",
                title = "Cẩu kéo xe sàn trượt về Gara",
                category = "TOW",
                priceDisplay = "600.000đ (mở cửa)",
                estimatedPrice = 600000,
                icon = Icons.Default.LocalShipping,
                description = "Xe sàn trượt thủy lực hiện đại kéo xe số tự động, xe 2 cầu AWD, xe gầm thấp về gara theo yêu cầu.",
                actionText = "Đặt xe kéo"
            ),
            StreamlinedServiceItem(
                id = "FUEL_DELIVERY",
                title = "Tiếp xăng dầu tận nơi",
                category = "CAR",
                priceDisplay = "150.000đ + tiền nhiên liệu",
                estimatedPrice = 150000,
                icon = Icons.Default.LocalGasStation,
                description = "Cung cấp can xăng RON 95 hoặc dầu Diesel đạt chuẩn tận nơi khi xe hết nhiên liệu giữa đường.",
                actionText = "Gọi tiếp xăng"
            ),
            StreamlinedServiceItem(
                id = "UNLOCK_DOOR",
                title = "Mở khóa ô tô quên chìa",
                category = "CAR",
                priceDisplay = "300.000đ",
                estimatedPrice = 300000,
                icon = Icons.Default.Key,
                description = "Mở khóa chuyên dụng bóng hơi và que kéo chốt cửa an toàn, cam kết không trầy xước sơn hay hỏng gioăng cửa.",
                actionText = "Đặt cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "FLOOD_RESCUE",
                title = "Cứu hộ ngập nước / Thủy kích",
                category = "TOW",
                priceDisplay = "700.000đ",
                estimatedPrice = 700000,
                icon = Icons.Default.WaterDamage,
                description = "Kéo xe ra khỏi khu vực ngập lụt, kiểm tra lọc gió buồng đốt, chống gãy tay biên động cơ.",
                actionText = "Đặt xe kéo"
            ),
            StreamlinedServiceItem(
                id = "BATTERY_REPLACE",
                title = "Thay bình ắc quy ô tô tận nơi",
                category = "CAR",
                priceDisplay = "Từ 950.000đ (Bảo hành 12T)",
                estimatedPrice = 950000,
                icon = Icons.Default.ElectricalServices,
                description = "Thay ắc quy chính hãng GS, Varta, Đồng Nai mới 100%, bảo hành 1 đổi 1 trong 12 tháng, thu lại bình cũ giá cao.",
                actionText = "Đặt thay bình"
            ),
            StreamlinedServiceItem(
                id = "TIRE_INFLATE",
                title = "Bơm lốp & Đo áp suất chuẩn",
                category = "CAR",
                priceDisplay = "50.000đ",
                estimatedPrice = 50000,
                icon = Icons.Default.Speed,
                description = "Bơm hơi lưu động đồng hồ đo điện tử chuẩn PSI nhà sản xuất, kiểm tra cảm biến áp suất lốp TPMS.",
                actionText = "Đặt bơm lốp"
            ),
            StreamlinedServiceItem(
                id = "MOTO_PUNCTURE",
                title = "Vá lốp xe máy đêm lưu động",
                category = "MOTORBIKE",
                priceDisplay = "70.000đ",
                estimatedPrice = 70000,
                icon = Icons.Default.TwoWheeler,
                description = "Vá lốp không ruột, thay ruột xe máy, xe tay ga, xe số lưu động tận nơi 24/24.",
                actionText = "Đặt cứu hộ"
            ),
            StreamlinedServiceItem(
                id = "MOTO_TOW",
                title = "Chở xe máy về trạm cứu hộ",
                category = "MOTORBIKE",
                priceDisplay = "180.000đ",
                estimatedPrice = 180000,
                icon = Icons.Default.ElectricMoped,
                description = "Xe bán tải chuyên dụng chở xe máy chết máy, đứt dây curoa, hỏng động cơ về trạm sửa chữa an toàn.",
                actionText = "Đặt xe chở"
            )
        )
    }

    val filteredServices = remember(selectedCategory, searchQuery) {
        val catFiltered = when (selectedCategory) {
            "CAR" -> services.filter { it.category == "CAR" }
            "MOTORBIKE" -> services.filter { it.category == "MOTORBIKE" }
            "TOW" -> services.filter { it.category == "TOW" }
            else -> services
        }
        if (searchQuery.isBlank()) catFiltered
        else catFiltered.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    var towDistanceKm by remember { mutableFloatStateOf(10f) }
    val towEstimatedPrice = 600000L + (towDistanceKm.toLong() * 20000L)
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dịch Vụ Cứu Hộ 24/7",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bảng giá niêm yết • KTV ứng trực 15-20 phút",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick Call Hotline in TopBar
                    IconButton(
                        onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                        modifier = Modifier.testTag("btn_services_call_top")
                    ) {
                        Icon(
                            Icons.Default.PhoneInTalk,
                            contentDescription = "Gọi cứu hộ ${AppConfig.RESCUE_HOTLINE_DISPLAY}",
                            tint = AlertRed
                        )
                    }
                    IconButton(
                        onClick = { showHandbookDialog = true },
                        modifier = Modifier.testTag("btn_services_handbook")
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Cẩm nang sự cố",
                            tint = Color(0xFFB45309)
                        )
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
        ) {
            // Main Top Tabs: Bảng giá dịch vụ vs Sổ tay & Đèn Taplo
            PrimaryTabRow(
                selectedTabIndex = selectedMainTab,
                modifier = Modifier.fillMaxWidth().testTag("services_main_tab_row"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Bảng Giá & Đặt Dịch Vụ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Sổ Tay & Đèn Táp-Lô", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            if (selectedMainTab == 0) {
                // TAB 0: BẢNG GIÁ & ĐẶT DỊCH VỤ
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 95.dp)
                ) {
                    // 1. Hotline Emergency Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("card_services_hotline_banner"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AlertRed,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.PhoneInTalk,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Tổng Đài Cứu Hộ 24/7",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = OnSurfaceLight
                                        )
                                        Text(
                                            text = AppConfig.RESCUE_HOTLINE_DISPLAY,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = AlertRed
                                        )
                                        Text(
                                            text = "Điều phối KTV gần nhất trong 15 phút",
                                            fontSize = 10.5.sp,
                                            color = OnSurfaceVariantLight
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_services_call_hotline_banner")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GỌI NGAY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. OpenStreetMap Interactive Card with Quick Action Buttons
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leaflet_osm_map_card"),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = BorderStroke(1.dp, BorderLight)
                        ) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                    RescueOpenStreetMap(
                                        userLat = currentLocation.latitude,
                                        userLng = currentLocation.longitude,
                                        userAddress = currentLocation.address,
                                        onCallPhone = onCallPhone,
                                        onRequestRescueAtLocation = {
                                            selectedServiceForDialog = services.find { it.id == "TOW_TRUCK" }
                                            showCreateDialog = true
                                        }
                                    )
                                }

                                // Interactive action bar under map
                                Surface(
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedServiceForDialog = null
                                                showCreateDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = RescuePrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Đặt Cứu Hộ Tại Đây", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gọi 0898 212 031", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2.5 Quick Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm nhanh dịch vụ (ví dụ: vá lốp, kích bình, kéo xe...)") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RescuePrimary) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_services"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }

                    // 3. Category Filter Chips
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = { selectedCategory = "ALL" },
                                label = { Text("Tất cả", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "CAR",
                                onClick = { selectedCategory = "CAR" },
                                label = { Text("Ô tô", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "MOTORBIKE",
                                onClick = { selectedCategory = "MOTORBIKE" },
                                label = { Text("Xe máy", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCategory == "TOW",
                                onClick = { selectedCategory = "TOW" },
                                label = { Text("Kéo xe", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. Tow Fare Estimator with Direct Booking and Direct Call
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compact_tow_calculator"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(18.dp))
                                        Text("Tính cước cẩu kéo: ${towDistanceKm.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        text = currencyFormat.format(towEstimatedPrice),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = RescuePrimary
                                    )
                                }

                                Text(
                                    text = "Đơn giá: 600.000đ mở cửa + 20.000đ/km tiếp theo. Kéo xe sàn trượt an toàn.",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariantLight
                                )

                                Slider(
                                    value = towDistanceKm,
                                    onValueChange = { towDistanceKm = it },
                                    valueRange = 2f..60f,
                                    steps = 28,
                                    modifier = Modifier.height(24.dp)
                                )

                                // Action Buttons for Tow Calculator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedServiceForDialog = StreamlinedServiceItem(
                                                id = "TOW_TRUCK",
                                                title = "Cẩu kéo xe sàn trượt (${towDistanceKm.toInt()}km)",
                                                category = "TOW",
                                                priceDisplay = currencyFormat.format(towEstimatedPrice),
                                                estimatedPrice = towEstimatedPrice,
                                                icon = Icons.Default.LocalShipping,
                                                description = "Cần xe cứu hộ kéo sàn trượt cự ly ~${towDistanceKm.toInt()} km"
                                            )
                                            showCreateDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.3f).height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ĐẶT XE KÉO (${towDistanceKm.toInt()} KM)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = AlertRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("GỌI TỔNG ĐÀI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Service Items List with Two Functional Action Buttons
                    items(filteredServices) { service ->
                        val (iconBg, iconTint) = when (service.category) {
                            "MOTORBIKE" -> CategoryOrangeBg to CategoryOrangeTint
                            "CAR" -> CategoryBlueBg to CategoryBlueTint
                            "TOW" -> CategoryAmberBg to CategoryAmberTint
                            else -> CategoryGreenBg to CategoryGreenTint
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServiceDetail = service
                                }
                                .testTag("service_item_${service.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, BorderLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = iconBg,
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = service.icon,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = service.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = service.priceDisplay,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = RescuePrimary
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = CategoryGreenBg
                                    ) {
                                        Text(
                                            text = "15-20 phút",
                                            color = SafeGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = service.description,
                                    fontSize = 11.5.sp,
                                    color = OnSurfaceVariantLight,
                                    lineHeight = 16.sp
                                )

                                // Action Buttons Row: Đặt Cứu Hộ & Gọi Hotline
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            selectedServiceForDialog = service
                                            showCreateDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1.3f).height(40.dp).testTag("btn_order_${service.id}")
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = service.actionText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("btn_call_${service.id}")
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = AlertRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("0898 212 031", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                                    }
                                }
                            }
                        }
                    }

                    // Empty search state
                    if (filteredServices.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderLight)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = OnSurfaceVariantLight, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "Không tìm thấy dịch vụ cho \"$searchQuery\"",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = OnSurfaceLight
                                    )
                                    Text(
                                        text = "Gọi ngay đường dây nóng ${AppConfig.RESCUE_HOTLINE_DISPLAY} để tổng đài viên điều phối xe cứu hộ khẩn cấp.",
                                        fontSize = 11.5.sp,
                                        color = OnSurfaceVariantLight,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("GỌI ${AppConfig.RESCUE_HOTLINE_DISPLAY}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: SỔ TAY SỰ CỐ & TRA CỨU ĐÈN TÁP-LÔ
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = handbookSubTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("handbook_sub_tab_row")
                    ) {
                        Tab(
                            selected = handbookSubTab == 0,
                            onClick = { handbookSubTab = 0 },
                            text = { Text("Kỹ Năng", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = handbookSubTab == 1,
                            onClick = { handbookSubTab = 1 },
                            text = { Text("Đèn Táp-Lô", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = handbookSubTab == 2,
                            onClick = { handbookSubTab = 2 },
                            text = { Text("Hotline 24/7", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 95.dp)
                    ) {
                        when (handbookSubTab) {
                            0 -> {
                                items(RescueDataCatalog.handbookGuides) { guide ->
                                    GuideAccordionCard(
                                        guide = guide,
                                        isExpanded = expandedGuideId == guide.id,
                                        onToggle = {
                                            expandedGuideId = if (expandedGuideId == guide.id) null else guide.id
                                        }
                                    )
                                }
                            }
                            1 -> {
                                items(RescueDataCatalog.dashAlertLights) { light ->
                                    DashboardLightCard(light = light)
                                }
                            }
                            2 -> {
                                items(RescueDataCatalog.hotlines) { contact ->
                                    HotlineCard(hotline = contact, onCallPhone = onCallPhone)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SERVICE DETAIL DIALOG
    selectedServiceDetail?.let { s ->
        AlertDialog(
            onDismissRequest = { selectedServiceDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(s.icon, contentDescription = null, tint = RescuePrimary)
                    Text(s.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Giá niêm yết: ${s.priceDisplay}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = RescuePrimary)
                    Text(s.description, fontSize = 12.5.sp, lineHeight = 18.sp)
                    HorizontalDivider()
                    Text("✓ KTV có mặt sau 15-20 phút tại hiện trường", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.Medium)
                    Text("✓ Báo giá minh bạch trước khi làm, không phát sinh", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.Medium)
                    Text("✓ Hỗ trợ thanh toán VietQR chuẩn Napas & hóa đơn bảo hiểm", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = s
                        selectedServiceDetail = null
                        selectedServiceForDialog = chosen
                        showCreateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                ) {
                    Text("Đặt Cứu Hộ Ngay")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        selectedServiceDetail = null
                        onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE)
                    }
                ) {
                    Text("Gọi 0898 212 031", color = AlertRed)
                }
            }
        )
    }

    // BOOKING DIALOG (Fully functional with pre-filled service data and saved vehicles)
    if (showCreateDialog) {
        val effectiveProfile = userProfile ?: UserProfile(displayName = "Khách hàng")
        val chosenService = selectedServiceForDialog
        val initCategory = when (chosenService?.category) {
            "MOTORBIKE" -> "BIKE"
            "CAR", "TOW" -> "CAR"
            else -> null
        }
        val initIssue = when (chosenService?.id) {
            "BATTERY_JUMP", "BATTERY_REPLACE" -> "Hết bình"
            "TIRE_PUNCTURE", "MOTO_PUNCTURE", "TIRE_INFLATE" -> "Thủng lốp"
            "FUEL_DELIVERY" -> "Xe hết xăng"
            "UNLOCK_DOOR" -> "Khóa xe"
            "FLOOD_RESCUE", "TOW_TRUCK", "MOTO_TOW" -> "Hỏng xe"
            else -> "Hỏng xe"
        }
        val initDesc = chosenService?.let { "${it.title} (${it.priceDisplay}) - ${it.description}" }

        CreateRequestDialog(
            initialIssueType = initIssue,
            initialVehicleCategory = initCategory,
            initialDescription = initDesc,
            userProfile = effectiveProfile,
            currentLocation = currentLocation,
            savedVehicles = savedVehicles,
            onRefreshLocation = onRefreshLocation,
            onDismiss = {
                showCreateDialog = false
                selectedServiceForDialog = null
            },
            onSubmit = { issue, desc, vType, plate, img ->
                showCreateDialog = false
                val servicePrefix = selectedServiceForDialog?.let { "[${it.title}] " } ?: ""
                val fullDesc = if (desc.isNotBlank()) {
                    if (desc.startsWith("[")) desc else "$servicePrefix$desc"
                } else (selectedServiceForDialog?.description ?: "Yêu cầu cứu hộ khẩn cấp")
                selectedServiceForDialog = null
                onSubmitRequest(issue, fullDesc, vType, plate, img)
            }
        )
    }

    // Emergency Handbook Dialog
    if (showHandbookDialog) {
        EmergencyHandbookDialog(
            onDismiss = { showHandbookDialog = false },
            onDirectCallSos = { onCallPhone(AppConfig.DEFAULT_RESCUE_HOTLINE) }
        )
    }
}
