package com.example.weatherapp.utils



import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationUtils(private val fusedLocationClient: FusedLocationProviderClient) {

    /**
     * Gets the last known location. This is often quicker but might return a stale location.
     * Explicitly checks for location permissions before making the call.
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(context: Context): Result<Location> =
        suspendCancellableCoroutine { continuation ->
            // Explicitly check for permissions right before the API call
            if (!hasLocationPermissions(context)) {
                continuation.resume(Result.failure(SecurityException("Location permissions not granted.")))
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(Result.success(location))
                    } else {
                        // Last location is null, which can happen if location hasn't been used recently
                        continuation.resume(Result.failure(Exception("Last known location is null, consider requesting a fresh update.")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }

    /**
     * Requests a single, fresh location update.
     * Explicitly checks for location permissions before making the call.
     */
    @SuppressLint("MissingPermission")
    suspend fun requestSingleLocationUpdate(context: Context): Result<Location> =
        suspendCancellableCoroutine { continuation ->
            // Explicitly check for permissions right before the API call
            if (!hasLocationPermissions(context)) {
                continuation.resume(Result.failure(SecurityException("Location permissions not granted.")))
                return@suspendCancellableCoroutine
            }

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000 // 10 seconds interval (minimum time between updates)
            )
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1) // Only one update
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        if (continuation.isActive) { // Ensure continuation is still active
                            continuation.resume(Result.success(location))
                        }
                        fusedLocationClient.removeLocationUpdates(this) // Remove callback after first result
                    } ?: run {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Failed to get a fresh location update.")))
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper() // Use main looper for callbacks
            )

            // If the coroutine is cancelled, remove location updates
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }

    /**
     * Checks if the app has the necessary location permissions.
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
