package com.opportunity.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordinates(val latitude: Double, val longitude: Double)

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cachedLocation: Coordinates? = null
    private var cacheTimestamp: Long = 0L
    private val cacheTtlMs = 2 * 60 * 1000L // 2 minutes

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** One-shot current location. Returns null if permission is missing or location is unavailable. */
    suspend fun currentLocation(): Coordinates? {
        if (!hasPermission()) return null
        val now = System.currentTimeMillis()
        cachedLocation?.let { cached ->
            if (now - cacheTimestamp < cacheTtlMs) return cached
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        return runCatching {
            val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            location?.let { 
                val coords = Coordinates(it.latitude, it.longitude)
                cachedLocation = coords
                cacheTimestamp = now
                coords
            }
        }.getOrNull()
    }

    companion object {
        /** Haversine distance in meters -- good enough at "which named place am I near" scale. */
        fun distanceMeters(a: Coordinates, b: Coordinates): Double {
            val earthRadiusMeters = 6_371_000.0
            val dLat = Math.toRadians(b.latitude - a.latitude)
            val dLon = Math.toRadians(b.longitude - a.longitude)
            val lat1 = Math.toRadians(a.latitude)
            val lat2 = Math.toRadians(b.latitude)
            val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
            return 2 * earthRadiusMeters * atan2(sqrt(h), sqrt(1 - h))
        }
    }
}
