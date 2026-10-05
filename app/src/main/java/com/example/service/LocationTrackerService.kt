package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.UserLocationInfo
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Service to manage Fine Location permission checks, High-Accuracy GPS tracking,
 * and real-time coordinates stream for the rescue tracking feature.
 */
class LocationTrackerService(private val context: Context) {

    companion object {
        private const val TAG = "LocationTrackerService"

        // Default coordinates: Hanoi Capital (21.0285° N, 105.8542° E)
        const val DEFAULT_LAT = 21.028512
        const val DEFAULT_LNG = 105.854245
        const val DEFAULT_ADDRESS = "Số 18 Tràng Tiền, Quận Hoàn Kiếm, Hà Nội"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Checks if the ACCESS_FINE_LOCATION permission has been granted.
     */
    fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if at least ACCESS_COARSE_LOCATION has been granted.
     */
    fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if device GPS / Location provider is turned on.
     */
    fun isLocationProviderEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    /**
     * Fetches the current location once using FusedLocationProviderClient with High Accuracy.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): UserLocationInfo = withContext(Dispatchers.IO) {
        val hasFine = hasFineLocationPermission()
        val hasCoarse = hasCoarseLocationPermission()
        val gpsEnabled = isLocationProviderEnabled()

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permissions not granted, using fallback coordinates")
            return@withContext UserLocationInfo(
                latitude = DEFAULT_LAT,
                longitude = DEFAULT_LNG,
                address = DEFAULT_ADDRESS,
                hasFineLocation = false,
                isGpsEnabled = gpsEnabled
            )
        }

        try {
            val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val cts = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(priority, cts.token).await()

            if (location != null) {
                val address = resolveAddress(location.latitude, location.longitude)
                UserLocationInfo(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address.ifBlank { "Vị trí GPS (${String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)})" },
                    accuracy = location.accuracy,
                    altitude = location.altitude,
                    speed = location.speed,
                    hasFineLocation = hasFine,
                    isGpsEnabled = gpsEnabled,
                    timestamp = location.time
                )
            } else {
                // Fallback to last known location if immediate fix is null
                val lastKnown: Location? = fusedLocationClient.lastLocation.await()
                if (lastKnown != null) {
                    val address = resolveAddress(lastKnown.latitude, lastKnown.longitude)
                    UserLocationInfo(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        address = address.ifBlank { DEFAULT_ADDRESS },
                        accuracy = lastKnown.accuracy,
                        altitude = lastKnown.altitude,
                        speed = lastKnown.speed,
                        hasFineLocation = hasFine,
                        isGpsEnabled = gpsEnabled,
                        timestamp = lastKnown.time
                    )
                } else {
                    UserLocationInfo(
                        latitude = DEFAULT_LAT,
                        longitude = DEFAULT_LNG,
                        address = DEFAULT_ADDRESS,
                        hasFineLocation = hasFine,
                        isGpsEnabled = gpsEnabled
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining location from FusedLocationClient: ${e.message}", e)
            UserLocationInfo(
                latitude = DEFAULT_LAT,
                longitude = DEFAULT_LNG,
                address = DEFAULT_ADDRESS,
                hasFineLocation = hasFine,
                isGpsEnabled = gpsEnabled
            )
        }
    }

    /**
     * Real-time Location updates flow for tracking rescue in real-time.
     * Emits a new UserLocationInfo every interval (e.g. 4 seconds) or upon displacement.
     */
    @SuppressLint("MissingPermission")
    fun getRealtimeLocationFlow(intervalMs: Long = 4000L): Flow<UserLocationInfo> = callbackFlow {
        val hasFine = hasFineLocationPermission()
        val hasCoarse = hasCoarseLocationPermission()

        if (!hasFine && !hasCoarse) {
            trySend(
                UserLocationInfo(
                    latitude = DEFAULT_LAT,
                    longitude = DEFAULT_LNG,
                    address = DEFAULT_ADDRESS,
                    hasFineLocation = false,
                    isGpsEnabled = isLocationProviderEnabled()
                )
            )
            close()
            return@callbackFlow
        }

        val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val locationRequest = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(2f)
            .setWaitForAccurateLocation(hasFine)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val addr = resolveAddress(loc.latitude, loc.longitude)
                val info = UserLocationInfo(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    address = addr.ifBlank { "Vị trí (${String.format(Locale.US, "%.5f, %.5f", loc.latitude, loc.longitude)})" },
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    speed = loc.speed,
                    hasFineLocation = hasFine,
                    isGpsEnabled = isLocationProviderEnabled(),
                    timestamp = loc.time
                )
                trySend(info)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    Log.w(TAG, "Location is temporarily unavailable")
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            Log.i(TAG, "Realtime location tracking started (Fine Location: $hasFine)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}", e)
            close()
        }

        awaitClose {
            Log.i(TAG, "Stopping realtime location tracking")
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Resolves human-readable street address from GPS latitude/longitude.
     */
    fun resolveAddress(lat: Double, lng: Double): String {
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
