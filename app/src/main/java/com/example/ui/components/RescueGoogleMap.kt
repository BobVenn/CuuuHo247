package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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
 * Powered by LeafletMapView.
 */
@Composable
fun RescueGoogleMap(
    userLat: Double,
    userLng: Double,
    userAddress: String,
    modifier: Modifier = Modifier,
    onCallPhone: (String) -> Unit = {},
    onRequestRescueAtLocation: (RescueMapLocation) -> Unit = {}
) {
    val rescueStationMarkers = remember(userLat, userLng) {
        val lat = if (userLat != 0.0) userLat else 21.0285
        val lng = if (userLng != 0.0) userLng else 105.8542
        listOf(
            LeafletMapMarker(
                id = "station_1",
                title = "Đội Cứu Hộ Phản Ứng Nhanh 24/7",
                snippet = "Xe sàn trượt thủy lực • Túc trực 24/24 (Cách 1.2km)",
                latitude = lat + 0.0075,
                longitude = lng + 0.0062,
                type = "PATROL"
            ),
            LeafletMapMarker(
                id = "station_2",
                title = "Trạm Cứu Hộ Lưu Động - Xe 29C-782",
                snippet = "Chuyên kích bình & vá lốp lưu động (Cách 1.8km)",
                latitude = lat - 0.0082,
                longitude = lng - 0.0055,
                type = "PATROL"
            ),
            LeafletMapMarker(
                id = "station_3",
                title = "Gara Liên Kết Cứu Hộ Giao Thông",
                snippet = "Cẩu kéo xe tai nạn, hỏng hộp số (Cách 2.5km)",
                latitude = lat + 0.0125,
                longitude = lng - 0.0089,
                type = "PATROL"
            )
        )
    }

    LeafletMapView(
        centerLat = if (userLat != 0.0) userLat else 21.0285,
        centerLng = if (userLng != 0.0) userLng else 105.8542,
        centerAddress = userAddress,
        modifier = modifier,
        markers = rescueStationMarkers,
        onMarkerClick = { marker ->
            onRequestRescueAtLocation(RescueMapLocation(marker.latitude, marker.longitude))
        }
    )
}
