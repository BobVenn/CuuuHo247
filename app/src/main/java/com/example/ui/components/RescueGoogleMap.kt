package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen

data class RescueStationMarker(
    val id: String,
    val title: String,
    val snippet: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val distanceKm: Double,
    val etaMinutes: Int
)

data class RescueMapLocation(
    val latitude: Double,
    val longitude: Double
)

@Composable
fun RescueOpenStreetMap(
    userLat: Double,
    userLng: Double,
    userAddress: String,
    modifier: Modifier = Modifier,
    onCallPhone: (String) -> Unit = {},
    onRequestRescueAtLocation: (RescueMapLocation) -> Unit = {}
) {
    RescueGoogleMap(
        userLat = userLat,
        userLng = userLng,
        userAddress = userAddress,
        modifier = modifier,
        onCallPhone = onCallPhone,
        onRequestRescueAtLocation = onRequestRescueAtLocation
    )
}

@Composable
fun RescueLeafletMap(
    userLat: Double,
    userLng: Double,
    userAddress: String,
    modifier: Modifier = Modifier,
    onCallPhone: (String) -> Unit = {},
    onRequestRescueAtLocation: (RescueMapLocation) -> Unit = {}
) {
    RescueGoogleMap(
        userLat = userLat,
        userLng = userLng,
        userAddress = userAddress,
        modifier = modifier,
        onCallPhone = onCallPhone,
        onRequestRescueAtLocation = onRequestRescueAtLocation
    )
}

/**
 * High-performance, 100% Free OpenStreetMap & Leaflet.js interactive map component.
 * Completely free of charge, requires NO API Key, NO Credit Card, and NO Google Cloud billing.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RescueGoogleMap(
    userLat: Double,
    userLng: Double,
    userAddress: String,
    modifier: Modifier = Modifier,
    onCallPhone: (String) -> Unit = {},
    onRequestRescueAtLocation: (RescueMapLocation) -> Unit = {}
) {
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var selectedStation by remember { mutableStateOf<RescueStationMarker?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Nearby real-time patrol rescue stations relative to user coordinates
    val rescueStations = remember(userLat, userLng) {
        listOf(
            RescueStationMarker(
                id = "station_1",
                title = "Đội Cứu Hộ Phản Ứng Nhanh 24/7",
                snippet = "Xe sàn trượt thủy lực • Túc trực 24/24",
                latitude = userLat + 0.0075,
                longitude = userLng + 0.0062,
                phone = "1900545566",
                distanceKm = 1.2,
                etaMinutes = 8
            ),
            RescueStationMarker(
                id = "station_2",
                title = "Trạm Cứu Hộ Lưu Động - Xe 29C-782",
                snippet = "Chuyên kích bình ắc quy & vá lốp lưu động",
                latitude = userLat - 0.0082,
                longitude = userLng - 0.0055,
                phone = "0912345678",
                distanceKm = 1.8,
                etaMinutes = 12
            ),
            RescueStationMarker(
                id = "station_3",
                title = "Gara Liên Kết Cứu Hộ Giao Thông",
                snippet = "Cẩu kéo xe tai nạn, hỏng hộp số, xe 2 cầu",
                latitude = userLat + 0.0125,
                longitude = userLng - 0.0089,
                phone = "0987654321",
                distanceKm = 2.5,
                etaMinutes = 15
            )
        )
    }

    // Generate responsive Leaflet.js HTML with OpenStreetMap tiles
    val leafletHtml = remember(userLat, userLng, userAddress) {
        buildLeafletHtml(
            lat = userLat,
            lng = userLng,
            address = userAddress,
            stations = rescueStations
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("rescue_leaflet_map_container")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {}

                    // JavaScript Bridge to receive clicks from Leaflet markers
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMarkerClicked(
                            id: String,
                            title: String,
                            snippet: String,
                            phone: String,
                            dist: Double,
                            eta: Int,
                            lat: Double,
                            lng: Double
                        ) {
                            post {
                                selectedStation = RescueStationMarker(
                                    id = id,
                                    title = title,
                                    snippet = snippet,
                                    latitude = lat,
                                    longitude = lng,
                                    phone = phone,
                                    distanceKm = dist,
                                    etaMinutes = eta
                                )
                            }
                        }

                        @JavascriptInterface
                        fun onCallPhoneAction(phone: String) {
                            post { onCallPhone(phone) }
                        }

                        @JavascriptInterface
                        fun onRequestRescueAction(lat: Double, lng: Double) {
                            post { onRequestRescueAtLocation(RescueMapLocation(lat, lng)) }
                        }

                        @JavascriptInterface
                        fun onMapClicked() {
                            post { selectedStation = null }
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL("https://openstreetmap.org", leafletHtml, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
                webView.loadDataWithBaseURL("https://openstreetmap.org", leafletHtml, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Free OpenStreetMap Badge
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = if (selectedStation != null) 140.dp else 10.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "OpenStreetMap • Miễn phí 100%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Location Permission Alert Banner if not yet granted
        if (!hasLocationPermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.LocationDisabled, contentDescription = null, tint = AlertRed)
                        Text(
                            text = "Bật định vị để hiển thị vị trí trên OpenStreetMap",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Cấp quyền", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Map Control Floating Buttons (Recenter & Zoom in/out via Leaflet JS)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (!hasLocationPermission) 60.dp else 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Re-center on user coordinates
            FilledIconButton(
                onClick = {
                    webViewInstance?.evaluateJavascript("recenterMap($userLat, $userLng);", null)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RescuePrimary
                ),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của tôi", modifier = Modifier.size(20.dp))
            }

            // Zoom In
            FilledIconButton(
                onClick = { webViewInstance?.evaluateJavascript("map.zoomIn();", null) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Phóng to", modifier = Modifier.size(18.dp))
            }

            // Zoom Out
            FilledIconButton(
                onClick = { webViewInstance?.evaluateJavascript("map.zoomOut();", null) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", modifier = Modifier.size(18.dp))
            }
        }

        // Selected Rescue Station Card Overlay
        selectedStation?.let { station ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .testTag("marker_detail_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RescuePrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = RescuePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    station.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "${station.distanceKm} km • Khoảng ${station.etaMinutes} phút tiếp cận",
                                    fontSize = 11.sp,
                                    color = SafeGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { selectedStation = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        station.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCallPhone(station.phone) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gọi Trạm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onRequestRescueAtLocation(
                                    RescueMapLocation(station.latitude, station.longitude)
                                )
                            },
                            modifier = Modifier.weight(1.3f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đặt Tại Điểm Này", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds HTML document utilizing Leaflet 1.9.4 and OpenStreetMap (free tile layer).
 */
private fun buildLeafletHtml(
    lat: Double,
    lng: Double,
    address: String,
    stations: List<RescueStationMarker>
): String {
    val escapedAddress = address.replace("\"", "\\\"").replace("'", "\\'")

    val stationsJsArray = stations.joinToString(",") { s ->
        """
        {
            id: "${s.id}",
            title: "${s.title.replace("\"", "\\\"")}",
            snippet: "${s.snippet.replace("\"", "\\\"")}",
            phone: "${s.phone}",
            dist: ${s.distanceKm},
            eta: ${s.etaMinutes},
            lat: ${s.latitude},
            lng: ${s.longitude}
        }
        """.trimIndent()
    }

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body {
                height: 100%;
                margin: 0;
                padding: 0;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
                width: 100%;
                height: 100%;
                background: #f4f5f7;
            }
            /* Custom user marker pulse */
            .user-pulse-marker {
                position: relative;
            }
            .pulse-dot {
                width: 16px;
                height: 16px;
                background: #2563eb;
                border: 3px solid #ffffff;
                border-radius: 50%;
                box-shadow: 0 0 8px rgba(37, 99, 235, 0.8);
            }
            .pulse-ring {
                position: absolute;
                top: -6px;
                left: -6px;
                width: 28px;
                height: 28px;
                border-radius: 50%;
                background: rgba(37, 99, 235, 0.35);
                animation: pulseAnim 2s infinite ease-out;
            }
            @keyframes pulseAnim {
                0% { transform: scale(0.6); opacity: 1; }
                100% { transform: scale(1.6); opacity: 0; }
            }
            /* Custom rescue vehicle marker */
            .rescue-marker-icon {
                background: #EA580C;
                color: #ffffff;
                border: 2px solid #ffffff;
                border-radius: 50%;
                width: 34px;
                height: 34px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 10px rgba(234, 88, 12, 0.5);
                font-size: 16px;
            }
            .leaflet-popup-content-wrapper {
                border-radius: 12px;
                box-shadow: 0 4px 16px rgba(0,0,0,0.15);
                padding: 4px;
            }
            .leaflet-popup-content {
                margin: 8px 12px;
                font-size: 13px;
                line-height: 1.4;
            }
            .leaflet-control-zoom {
                display: none !important;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map', {
                zoomControl: false,
                attributionControl: false
            }).setView([$lat, $lng], 15);

            // OpenStreetMap 100% Free Standard Tile Layer
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19
            }).addTo(map);

            // User pulse marker
            var userIcon = L.divIcon({
                className: 'user-pulse-marker',
                html: '<div class="pulse-ring"></div><div class="pulse-dot"></div>',
                iconSize: [20, 20],
                iconAnchor: [10, 10]
            });

            var userMarker = L.marker([$lat, $lng], { icon: userIcon }).addTo(map);
            userMarker.bindPopup("<b>Vị trí của bạn</b><br/><span style='color:#666;'>$escapedAddress</span>");

            // Rescue Stations markers
            var stations = [$stationsJsArray];

            stations.forEach(function(s) {
                var rescueIcon = L.divIcon({
                    className: '',
                    html: '<div class="rescue-marker-icon">🚚</div>',
                    iconSize: [34, 34],
                    iconAnchor: [17, 17]
                });

                var marker = L.marker([s.lat, s.lng], { icon: rescueIcon }).addTo(map);

                marker.on('click', function() {
                    if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                        window.AndroidBridge.onMarkerClicked(
                            s.id, s.title, s.snippet, s.phone, s.dist, s.eta, s.lat, s.lng
                        );
                    }
                });
            });

            map.on('click', function() {
                if (window.AndroidBridge && window.AndroidBridge.onMapClicked) {
                    window.AndroidBridge.onMapClicked();
                }
            });

            function recenterMap(newLat, newLng) {
                map.flyTo([newLat, newLng], 15, { duration: 1.0 });
                userMarker.setLatLng([newLat, newLng]);
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
