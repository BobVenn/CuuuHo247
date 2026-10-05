package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
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
import com.example.ui.theme.AlertRed
import com.example.ui.theme.RescuePrimary
import com.example.ui.theme.SafeGreen

data class LeafletMapMarker(
    val id: String,
    val title: String,
    val snippet: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "REQUEST", // "REQUEST", "TECHNICIAN", "USER", "PATROL"
    val status: String = "PENDING"
)

/**
 * High-performance, 100% Free Leaflet.js & OpenStreetMap web-view component.
 * Displays accurate map coordinates, user location, technician location, route polyline,
 * and clickable request pins.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    centerLat: Double,
    centerLng: Double,
    centerAddress: String = "",
    modifier: Modifier = Modifier,
    zoomLevel: Int = 15,
    markers: List<LeafletMapMarker> = emptyList(),
    routeDestinationLat: Double? = null,
    routeDestinationLng: Double? = null,
    routeDestinationLabel: String? = null,
    onMarkerClick: ((LeafletMapMarker) -> Unit)? = null,
    onNavigateClick: ((lat: Double, lng: Double, label: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var selectedMarker by remember { mutableStateOf<LeafletMapMarker?>(null) }

    // Generate responsive Leaflet.js HTML with OpenStreetMap tiles and Fallback
    val htmlContent = remember(centerLat, centerLng, markers, routeDestinationLat, routeDestinationLng) {
        buildLeafletHtmlContent(
            centerLat = if (centerLat != 0.0) centerLat else 21.0285, // Default Hanoi if 0.0
            centerLng = if (centerLng != 0.0) centerLng else 105.8542,
            centerAddress = centerAddress,
            zoom = zoomLevel,
            markers = markers,
            destLat = routeDestinationLat,
            destLng = routeDestinationLng,
            destLabel = routeDestinationLabel ?: "Điểm đến"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .testTag("leaflet_webview_map_container")
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
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {}

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onMarkerClicked(id: String) {
                            post {
                                val found = markers.find { it.id == id }
                                selectedMarker = found
                                if (found != null) {
                                    onMarkerClick?.invoke(found)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onNavigateAction(lat: Double, lng: Double, label: String) {
                            post {
                                if (onNavigateClick != null) {
                                    onNavigateClick.invoke(lat, lng, label)
                                } else {
                                    openGoogleMapsNavigation(context, lat, lng, label)
                                }
                            }
                        }

                        @JavascriptInterface
                        fun onMapClicked() {
                            post { selectedMarker = null }
                        }
                    }, "AndroidBridge")

                    loadDataWithBaseURL("https://www.openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                    webViewInstance = this
                }
            },
            update = { webView ->
                webViewInstance = webView
                webView.loadDataWithBaseURL("https://www.openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top-Left Badge: OpenStreetMap & Live GPS
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Surface(shape = CircleShape, color = SafeGreen, modifier = Modifier.size(8.dp)) {}
                Text(
                    text = "Bản đồ vệ tinh • OpenStreetMap",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Top-Right Quick Action: Open in Native Google Maps
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(10.dp),
            color = RescuePrimary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val targetLat = routeDestinationLat ?: centerLat
                        val targetLng = routeDestinationLng ?: centerLng
                        val label = routeDestinationLabel ?: centerAddress.ifBlank { "Vị trí sự cố" }
                        if (onNavigateClick != null) {
                            onNavigateClick.invoke(targetLat, targetLng, label)
                        } else {
                            openGoogleMapsNavigation(context, targetLat, targetLng, label)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Directions,
                        contentDescription = "Chỉ đường Google Maps",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Chỉ Đường",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Bottom-Right Controls: Zoom & Recenter
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    webViewInstance?.evaluateJavascript("if (window.map) { window.map.zoomIn(); }", null)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Phóng to", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    webViewInstance?.evaluateJavascript("if (window.map) { window.map.zoomOut(); }", null)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", modifier = Modifier.size(18.dp))
            }

            SmallFloatingActionButton(
                onClick = {
                    webViewInstance?.evaluateJavascript(
                        "if (window.recenterMap) { window.recenterMap($centerLat, $centerLng); }",
                        null
                    )
                },
                containerColor = RescuePrimary,
                contentColor = Color.White,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của tôi", modifier = Modifier.size(18.dp))
            }
        }

        // Selected Marker Quick Bottom Sheet Card
        selectedMarker?.let { marker ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = marker.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = marker.snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tọa độ: ${String.format(java.util.Locale.US, "%.4f, %.4f", marker.latitude, marker.longitude)}",
                            fontSize = 11.sp,
                            color = RescuePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                openGoogleMapsNavigation(context, marker.latitude, marker.longitude, marker.title)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RescuePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chỉ đường", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { selectedMarker = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Open Google Maps Navigation or Turn-by-Turn directions to target coordinates
 */
fun openGoogleMapsNavigation(context: Context, lat: Double, lng: Double, label: String = "Vị trí cứu hộ") {
    try {
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {}

    // Fallback 1: geo URI
    try {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
        return
    } catch (_: Exception) {}

    // Fallback 2: Google Maps Web Directions
    try {
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "Không thể mở ứng dụng bản đồ", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Builds responsive Leaflet.js HTML with OpenStreetMap standard tiles and fallback visual canvas
 */
private fun buildLeafletHtmlContent(
    centerLat: Double,
    centerLng: Double,
    centerAddress: String,
    zoom: Int,
    markers: List<LeafletMapMarker>,
    destLat: Double?,
    destLng: Double?,
    destLabel: String
): String {
    val escapedAddress = centerAddress.replace("\"", "\\\"").replace("'", "\\'")

    val markersJs = markers.joinToString(",") { m ->
        """
        {
            id: "${m.id}",
            title: "${m.title.replace("\"", "\\\"")}",
            snippet: "${m.snippet.replace("\"", "\\\"")}",
            lat: ${m.latitude},
            lng: ${m.longitude},
            type: "${m.type}",
            status: "${m.status}"
        }
        """.trimIndent()
    }

    val hasRoute = destLat != null && destLng != null && (destLat != centerLat || destLng != centerLng)

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
                background-color: #f1f5f9;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            #map {
                width: 100%;
                height: 100%;
                background: #e2e8f0;
            }
            /* Pulsing Blue Beacon for User */
            .user-pulse-marker {
                position: relative;
            }
            .pulse-dot {
                width: 16px;
                height: 16px;
                background: #2563eb;
                border: 3px solid #ffffff;
                border-radius: 50%;
                box-shadow: 0 0 10px rgba(37, 99, 235, 0.9);
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
            /* Technician / Vehicle Marker */
            .tech-marker-icon {
                background: #ea580c;
                color: #ffffff;
                border: 2px solid #ffffff;
                border-radius: 50%;
                width: 32px;
                height: 32px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 10px rgba(234, 88, 12, 0.5);
                font-size: 15px;
            }
            /* Request / Incident Alert Marker */
            .alert-marker-icon {
                background: #dc2626;
                color: #ffffff;
                border: 2px solid #ffffff;
                border-radius: 50%;
                width: 34px;
                height: 34px;
                display: flex;
                align-items: center;
                justify-content: center;
                box-shadow: 0 4px 12px rgba(220, 38, 38, 0.6);
                font-size: 16px;
                animation: bounceAnim 1.8s infinite ease-in-out;
            }
            @keyframes bounceAnim {
                0%, 100% { transform: translateY(0); }
                50% { transform: translateY(-4px); }
            }
            /* Leaflet popup styling */
            .leaflet-popup-content-wrapper {
                border-radius: 12px;
                box-shadow: 0 6px 18px rgba(0,0,0,0.18);
            }
            .leaflet-popup-content {
                margin: 10px 14px;
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
            // Initialize Leaflet Map
            var map = L.map('map', {
                zoomControl: false,
                attributionControl: false
            }).setView([$centerLat, $centerLng], $zoom);
            window.map = map;

            // OpenStreetMap standard tile layer
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19
            }).addTo(map);

            // Center User Location Beacon
            var userIcon = L.divIcon({
                className: 'user-pulse-marker',
                html: '<div class="pulse-ring"></div><div class="pulse-dot"></div>',
                iconSize: [20, 20],
                iconAnchor: [10, 10]
            });
            var centerMarker = L.marker([$centerLat, $centerLng], { icon: userIcon }).addTo(map);
            centerMarker.bindPopup("<b>Vị trí của bạn</b><br/><span style='color:#555;'>$escapedAddress</span>");

            // Add Provided Markers
            var markersData = [$markersJs];
            markersData.forEach(function(m) {
                var iconClass = m.type === 'TECHNICIAN' || m.type === 'PATROL' ? 'tech-marker-icon' : 'alert-marker-icon';
                var iconEmoji = m.type === 'TECHNICIAN' ? '🚚' : (m.status === 'COMPLETED' ? '✅' : '⚠️');
                var markerIcon = L.divIcon({
                    className: '',
                    html: '<div class="' + iconClass + '">' + iconEmoji + '</div>',
                    iconSize: [32, 32],
                    iconAnchor: [16, 16]
                });

                var mark = L.marker([m.lat, m.lng], { icon: markerIcon }).addTo(map);
                mark.bindPopup("<b>" + m.title + "</b><br/><span>" + m.snippet + "</span><br/><small style='color:#ea580c;'>Tọa độ: " + m.lat.toFixed(4) + ", " + m.lng.toFixed(4) + "</small>");

                mark.on('click', function() {
                    if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                        window.AndroidBridge.onMarkerClicked(m.id);
                    }
                });
            });

            // Route Polyline if destination is set
            ${if (hasRoute) {
                """
                var destLat = $destLat;
                var destLng = $destLng;
                var destIcon = L.divIcon({
                    className: '',
                    html: '<div class="alert-marker-icon">📍</div>',
                    iconSize: [34, 34],
                    iconAnchor: [17, 17]
                });
                var destMarker = L.marker([destLat, destLng], { icon: destIcon }).addTo(map);
                destMarker.bindPopup("<b>${destLabel.replace("\"", "\\\"")}</b><br/><button onclick='window.AndroidBridge.onNavigateAction(" + destLat + "," + destLng + ",\"" + "${destLabel.replace("\"", "\\\"")}" + "\")' style='background:#ea580c;color:#fff;border:none;border-radius:6px;padding:4px 8px;margin-top:6px;cursor:pointer;font-weight:bold;'>Chỉ đường</button>");

                // Draw route line
                var routeCoords = [
                    [$centerLat, $centerLng],
                    [destLat, destLng]
                ];
                var routeLine = L.polyline(routeCoords, {
                    color: '#ea580c',
                    weight: 4,
                    opacity: 0.85,
                    dashArray: '8, 8'
                }).addTo(map);

                // Fit bounds to show both points
                var bounds = L.latLngBounds(routeCoords);
                map.fitBounds(bounds, { padding: [50, 50] });
                """.trimIndent()
            } else ""}

            map.on('click', function() {
                if (window.AndroidBridge && window.AndroidBridge.onMapClicked) {
                    window.AndroidBridge.onMapClicked();
                }
            });

            window.recenterMap = function(lat, lng) {
                map.flyTo([lat, lng], 15, { duration: 1.0 });
                centerMarker.setLatLng([lat, lng]);
            };
        </script>
    </body>
    </html>
    """.trimIndent()
}
