package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.UserLocationInfo
import com.example.data.service.GoogleMapsGroundingService
import com.example.data.service.GroundedPlace
import com.example.data.service.MapsGroundingResult
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Màn hình tra cứu Gara & Trạm Cứu Hộ sử dụng Google Maps Grounding
 * Model: gemini-3.5-flash với công cụ googleMaps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleMapsGroundingScreen(
    currentLocation: UserLocationInfo,
    onBack: () -> Unit,
    onDirectRequestWithLocation: ((placeTitle: String, placeAddress: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Gara sửa chữa ô tô gần nhất đang mở cửa") }
    var isLoading by remember { mutableStateOf(false) }
    var groundingResult by remember { mutableStateOf<MapsGroundingResult?>(null) }

    val quickCategories = listOf(
        "Gara ô tô đang mở cửa",
        "Vá lốp ô tô xe máy lưu động",
        "Trạm kích bình ắc quy tận nơi",
        "Cây xăng Petrolimex gần nhất",
        "Đội xe kéo cứu hộ 24/7"
    )

    fun executeSearch(query: String) {
        if (isLoading) return
        isLoading = true
        coroutineScope.launch {
            val result = GoogleMapsGroundingService.searchRescuePlacesWithGoogleMaps(
                query = query,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                currentAddress = currentLocation.address
            )
            groundingResult = result
            isLoading = false
        }
    }

    // Tự động tìm kiếm lần đầu khi vào màn hình với category mặc định
    LaunchedEffect(Unit) {
        executeSearch(selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Bản Đồ Cứu Hộ Google Maps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafeGreen
                            ) {
                                Text(
                                    text = "AI GROUNDING",
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "gemini-3.5-flash • googleMaps tool",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. BANNER VỊ TRÍ GPS HIỆN TẠI CỦA THIẾT BỊ
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RescuePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(22.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tọa độ của bạn:", fontSize = 11.sp, color = OnSurfaceVariantLight, fontWeight = FontWeight.Medium)
                            Text(
                                text = "%.4f, %.4f".format(currentLocation.latitude, currentLocation.longitude),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RescuePrimary
                            )
                        }
                        Text(
                            text = currentLocation.address.ifBlank { "Đang lấy địa chỉ thực tế từ cảm biến GPS..." },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceLight,
                            maxLines = 2
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:${currentLocation.latitude},${currentLocation.longitude}?q=gara+sua+xe")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            try {
                                context.startActivity(mapIntent)
                            } catch (_: Exception) {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/gara+sua+xe/@${currentLocation.latitude},${currentLocation.longitude},15z"))
                                context.startActivity(webIntent)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Mở Google Maps", tint = RescuePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // =================================================================
            // 2. THANH TÌM KIẾM TÙY CHỌN & CHIPS DANH MỤC NHANH
            // =================================================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Khám phá địa điểm cứu hộ gần bạn:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Quick Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(quickCategories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = category
                                executeSearch(category)
                            },
                            label = { Text(category, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = RescuePrimary)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RescuePrimary.copy(alpha = 0.15f),
                                selectedLabelColor = RescuePrimary
                            )
                        )
                    }
                }

                // Custom search input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Nhập địa chỉ hoặc dịch vụ cần tìm...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariantLight) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_maps_grounding_query"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val q = searchQuery.ifBlank { selectedCategory }
                            selectedCategory = q
                            executeSearch(q)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("btn_execute_maps_grounding"),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tìm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // =================================================================
            // 3. TRẠNG THÁI LOADING / ĐANG TRUY VẤN
            // =================================================================
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(color = RescuePrimary, strokeWidth = 3.dp)
                        Text(
                            text = "Đang truy vấn Google Maps dữ liệu thực tế...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = OnSurfaceLight
                        )
                        Text(
                            text = "Gemini 3.5 Flash đang kết nối công cụ googleMaps để định vị gara, trạm vá lốp và xe kéo cứu hộ gần bạn nhất.",
                            fontSize = 11.5.sp,
                            color = OnSurfaceVariantLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // =================================================================
            // 4. KẾT QUẢ GOOGLE MAPS GROUNDING
            // =================================================================
            if (!isLoading && groundingResult != null) {
                val res = groundingResult!!

                // Badge thông báo kết quả
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (res.isRealGrounding) SafeGreen.copy(alpha = 0.1f) else EmergencyGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (res.isRealGrounding) SafeGreen.copy(alpha = 0.3f) else EmergencyGold.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (res.isRealGrounding) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (res.isRealGrounding) SafeGreen else EmergencyGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (res.isRealGrounding) "Google Maps Grounding Đã Xác Thực" else "Dữ Liệu Trạm Cứu Hộ Tiêu Chuẩn",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = if (res.isRealGrounding) SafeGreen else EmergencyGold
                            )
                            Text(
                                text = "Nguồn: Google Maps Platform API & Gemini 3.5 Flash Tooling",
                                fontSize = 10.5.sp,
                                color = OnSurfaceVariantLight
                            )
                        }
                    }
                }

                // Danh sách địa điểm đã tìm thấy
                if (res.places.isNotEmpty()) {
                    Text(
                        text = "Các địa điểm cứu hộ tìm thấy (${res.places.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    res.places.forEachIndexed { index, place ->
                        PlaceItemCard(
                            place = place,
                            index = index + 1,
                            onOpenMap = {
                                val uriToOpen = if (place.uri.isNotBlank()) {
                                    place.uri
                                } else {
                                    "https://www.google.com/maps/search/" + Uri.encode("${place.title} ${place.address}")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriToOpen))
                                context.startActivity(intent)
                            },
                            onRequestHelp = {
                                onDirectRequestWithLocation?.invoke(place.title, place.address)
                            }
                        )
                    }
                }

                // Phân tích chi tiết từ Gemini với Maps Data
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = CategoryBlueBg,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "Tư Vấn & Chỉ Dẫn Cứu Hộ Thực Tế:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))

                        Text(
                            text = res.responseText,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = OnSurfaceLight
                        )

                        if (res.searchQueries.isNotEmpty()) {
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.7f))
                            Text("Từ khóa truy vấn Maps:", fontSize = 11.sp, color = OnSurfaceVariantLight, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                res.searchQueries.take(3).forEach { sq ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = sq,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 5. BANNER TỔNG ĐÀI CỨU HỘ KHẨN CẤP 24/7
            // =================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Không tìm thấy gara phù hợp?", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AlertRed)
                        Text("Gọi ngay tổng đài điều phối xe cứu hộ khẩn cấp toàn quốc 24/7.", fontSize = 11.sp, color = OnSurfaceVariantLight)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1900888247")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1900 888 247", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceItemCard(
    place: GroundedPlace,
    index: Int,
    onOpenMap: () -> Unit,
    onRequestHelp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = RescuePrimary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = index.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = place.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = OnSurfaceLight
                        )
                        if (place.address.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = RescuePrimary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = place.address,
                                    fontSize = 11.5.sp,
                                    color = OnSurfaceVariantLight,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SafeGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Google Maps",
                        color = SafeGreen,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(15.dp), tint = RescuePrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ đường Maps", fontSize = 11.5.sp, color = RescuePrimary, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onRequestHelp,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Sos, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gọi Cứu Hộ Này", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
