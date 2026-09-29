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
    val address: String
)

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
}
