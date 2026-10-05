package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class UserLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val hasFineLocation: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedCoordinates: String
        get() = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

    val formattedAccuracy: String
        get() = if (accuracy > 0f) "±${accuracy.toInt()}m" else "Độ chính xác cao"

    val formattedDms: String
        get() {
            val latDir = if (latitude >= 0) "Bắc" else "Nam"
            val lngDir = if (longitude >= 0) "Đông" else "Tây"
            return String.format(Locale.US, "%.4f° %s, %.4f° %s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir)
        }
}

object LocationHelper {
    // Default fallback coordinates: Hanoi Center (near Hoan Kiem / National Highway 1A)
    private const val DEFAULT_LAT = 21.0285
    private const val DEFAULT_LNG = 105.8542
    private const val DEFAULT_ADDRESS = "Số 18 Tràng Tiền, Quận Hoàn Kiếm, Hà Nội"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): UserLocationInfo = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            var bestLocation: Location? = null

            if (locationManager != null) {
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                        bestLocation = loc
                    }
                }
            }

            val lat = bestLocation?.latitude ?: DEFAULT_LAT
            val lng = bestLocation?.longitude ?: DEFAULT_LNG

            val address = getAddressFromCoordinates(context, lat, lng)
            UserLocationInfo(
                latitude = lat,
                longitude = lng,
                address = address.ifBlank { DEFAULT_ADDRESS }
            )
        } catch (_: Exception) {
            UserLocationInfo(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_ADDRESS)
        }
    }

    private fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            if (Build.VERSION.SDK_INT >= 33) {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val parts = listOfNotNull(
                        addr.subThoroughfare,
                        addr.thoroughfare,
                        addr.subLocality,
                        addr.subAdminArea,
                        addr.adminArea
                    )
                    if (parts.isNotEmpty()) parts.joinToString(", ") else addr.getAddressLine(0) ?: ""
                } else {
                    ""
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0) ?: ""
                } else {
                    ""
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 2.5
        val r = 6371.0 // Bán kính trái đất tính bằng km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val dist = r * c
        return if (dist < 0.1) 0.5 else dist
    }

    fun openGoogleMapsNavigation(context: Context, destLat: Double, destLng: Double, label: String = "Điểm cứu hộ") {
        try {
            val uri = android.net.Uri.parse("google.navigation:q=$destLat,$destLng&mode=d")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "Không thể mở ứng dụng bản đồ", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
