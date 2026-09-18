package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.RuteEntity
import com.example.data.local.entity.WarungEntity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class UserGpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeter: Float = 0f,
    val isAvailable: Boolean = true,
    val provider: String = "GPS",
    val isFresh: Boolean = true,
    val satellitesUsed: Int = 0,
    val bearing: Float = 0f,
    val hasBearing: Boolean = false,
    val speedMps: Float = 0f,
    val altitude: Double = 0.0,
    val timeMs: Long = 0L,
    val rawLatitude: Double = latitude,
    val rawLongitude: Double = longitude,
    val isFused: Boolean = false
)

object LocationHelper {

    // Default fallback coordinates (Jakarta Pusat / Monas area)
    const val DEFAULT_LAT = -6.2088
    const val DEFAULT_LNG = 106.8456

    /**
     * Algoritma penentu apakah lokasi baru secara objektif lebih akurat dan valid dibanding lokasi sekarang.
     * Menggunakan standar resmi Google Android Location filtering untuk mencegah lonjakan ke cell tower (Network)
     * saat bergerak di lapangan.
     */
    fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        if (location.latitude == 0.0 && location.longitude == 0.0) return false
        if (location.latitude < -90.0 || location.latitude > 90.0) return false
        if (location.longitude < -180.0 || location.longitude > 180.0) return false
        if (location.accuracy <= 0f || location.accuracy.isNaN()) return false

        if (currentBestLocation == null) {
            return true
        }

        val timeDelta: Long = location.time - currentBestLocation.time
        val isSignificantlyNewer: Boolean = timeDelta > 120_000L
        val isSignificantlyOlder: Boolean = timeDelta < -120_000L
        val isNewer: Boolean = timeDelta > 0

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        val accuracyDelta: Float = location.accuracy - currentBestLocation.accuracy
        val isLessAccurate: Boolean = accuracyDelta > 0f
        val isMoreAccurate: Boolean = accuracyDelta < 0f
        val isSignificantlyLessAccurate: Boolean = accuracyDelta > 150f

        val isFromSameProvider: Boolean = location.provider == currentBestLocation.provider
        val isNewGps: Boolean = location.provider == LocationManager.GPS_PROVIDER
        val isOldGps: Boolean = currentBestLocation.provider == LocationManager.GPS_PROVIDER

        // Jika lokasi saat ini didapat dari Satelit GPS dan masih segar (< 30 detik),
        // tolak update dari Network cell tower yang akurasinya rendah (> 45m).
        if (isOldGps && !isNewGps && (System.currentTimeMillis() - currentBestLocation.time < 30_000L) && location.accuracy > 45f) {
            return false
        }

        // Jika update baru adalah Satelit GPS sedangkan sebelumnya Network, prioritaskan GPS selama akurasinya wajar (< 100m)
        if (isNewGps && !isOldGps && location.accuracy <= 100f) {
            return true
        }

        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }

    /**
     * Mengecek apakah layanan lokasi (GPS / Network) aktif di perangkat.
     */
    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            gpsEnabled || networkEnabled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mengecek apakah koordinat lokasi masih segar (bukan cache lama).
     */
    fun isLocationFresh(loc: Location, maxAgeSeconds: Long = 300): Boolean {
        val ageMs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            (android.os.SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1_000_000L
        } else {
            System.currentTimeMillis() - loc.time
        }
        return ageMs < (maxAgeSeconds * 1000L)
    }

    /**
     * Hitung jarak lurus (Great-Circle Distance / Haversine) antara 2 titik koordinat.
     * 100% OFFLINE, murni kalkulasi matematis tanpa butuh koneksi internet.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return Double.MAX_VALUE
        if (lat2 == 0.0 && lon2 == 0.0) return Double.MAX_VALUE

        val r = 6371000.0 // Radius Bumi dalam meter
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Format jarak meter ke string yang mudah dibaca sales di lapangan:
     * - < 1000m -> "120 m"
     * - >= 1000m -> "1.4 km"
     */
    fun formatDistance(meters: Double): String {
        if (meters == Double.MAX_VALUE || meters < 0) return "- m"
        return if (meters < 1000.0) {
            "${meters.toInt()} m"
        } else {
            val km10 = (meters / 100).roundToInt()
            "${km10 / 10}.${km10 % 10} km"
        }
    }

    /**
     * Dapatkan Flow update lokasi presisi tinggi menggunakan Google FusedLocationProviderClient
     * (menggabungkan Satelit GNSS + Wi-Fi Positioning System + Cell Tower) dilengkapi
     * GpsKalmanFilter untuk menghapus jitter/lonjakan sinyal saat salesman bergerak atau diam di outlet.
     */
    @SuppressLint("MissingPermission")
    fun observeCurrentLocation(context: Context): Flow<UserGpsLocation> = callbackFlow {
        val kalmanFilter = GpsKalmanFilter()
        val fusedClient = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (_: Exception) {
            null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        // 1. Emit instant bootstrap location immediately
        val instant = getInstantLocation(context)
        if (instant.isAvailable && instant.latitude != 0.0) {
            val (sLat, sLng) = kalmanFilter.filter(instant.latitude, instant.longitude, instant.accuracyMeter)
            trySend(instant.copy(latitude = sLat, longitude = sLng, rawLatitude = instant.latitude, rawLongitude = instant.longitude, isFused = fusedClient != null))
        } else {
            trySend(instant)
        }

        var currentBestLocation: Location? = getBestLastKnownLocation(context)

        var isFusedActive = false
        var isNativeActive = false
        var locationCallback: LocationCallback? = null
        var nativeListener: LocationListener? = null

        // Try Fused Location Provider Client (Google Play Services) first
        if (fusedClient != null) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(500L)
                .setMinUpdateDistanceMeters(0f)
                .setMaxUpdateDelayMillis(1500L)
                .build()

            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (loc.latitude == 0.0 && loc.longitude == 0.0) return

                    currentBestLocation = loc
                    val (smoothLat, smoothLng) = kalmanFilter.filter(loc.latitude, loc.longitude, loc.accuracy)

                    trySend(
                        UserGpsLocation(
                            latitude = smoothLat,
                            longitude = smoothLng,
                            accuracyMeter = loc.accuracy,
                            isAvailable = true,
                            provider = "Google Fused (GNSS+Wi-Fi)",
                            isFresh = true,
                            bearing = if (loc.hasBearing()) loc.bearing else 0f,
                            hasBearing = loc.hasBearing(),
                            speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                            altitude = if (loc.hasAltitude()) loc.altitude else 0.0,
                            timeMs = loc.time,
                            rawLatitude = loc.latitude,
                            rawLongitude = loc.longitude,
                            isFused = true
                        )
                    )
                }
            }
            locationCallback = cb

            try {
                fusedClient.requestLocationUpdates(locationRequest, cb, android.os.Looper.getMainLooper())
                isFusedActive = true
            } catch (_: Exception) {
                isFusedActive = false
            }
        }

        // Fallback: Native Android LocationManager (Multi-Provider: GPS + Network + Passive)
        if (!isFusedActive && locationManager != null) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!isBetterLocation(location, currentBestLocation)) return
                    currentBestLocation = location

                    val isGps = location.provider == LocationManager.GPS_PROVIDER
                    val (smoothLat, smoothLng) = kalmanFilter.filter(location.latitude, location.longitude, location.accuracy)

                    trySend(
                        UserGpsLocation(
                            latitude = smoothLat,
                            longitude = smoothLng,
                            accuracyMeter = location.accuracy,
                            isAvailable = true,
                            provider = if (isGps) "Satelit Standalone" else (location.provider ?: "Network"),
                            isFresh = true,
                            bearing = if (location.hasBearing()) location.bearing else 0f,
                            hasBearing = location.hasBearing(),
                            speedMps = if (location.hasSpeed()) location.speed else 0f,
                            altitude = if (location.hasAltitude()) location.altitude else 0.0,
                            timeMs = location.time,
                            rawLatitude = location.latitude,
                            rawLongitude = location.longitude,
                            isFused = false
                        )
                    )
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            nativeListener = listener

            try {
                val mainLooper = android.os.Looper.getMainLooper()
                val providers = listOfNotNull(
                    LocationManager.GPS_PROVIDER.takeIf { locationManager.isProviderEnabled(it) },
                    LocationManager.NETWORK_PROVIDER.takeIf { locationManager.isProviderEnabled(it) },
                    LocationManager.PASSIVE_PROVIDER
                )
                for (prov in providers) {
                    try {
                        locationManager.requestLocationUpdates(prov, 1000L, 0f, listener, mainLooper)
                        isNativeActive = true
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        if (!isFusedActive && !isNativeActive) {
            close()
            return@callbackFlow
        }

        awaitClose {
            if (isFusedActive && locationCallback != null && fusedClient != null) {
                try {
                    fusedClient.removeLocationUpdates(locationCallback)
                } catch (_: Exception) {}
            }
            if (isNativeActive && nativeListener != null && locationManager != null) {
                try {
                    locationManager.removeUpdates(nativeListener)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Mengambil lokasi cache terbaik dari semua provider yang tersedia (GPS, Network, Passive).
     */
    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(locationManager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (prov in providers) {
            try {
                if (locationManager.isProviderEnabled(prov)) {
                    val loc = locationManager.getLastKnownLocation(prov) ?: continue
                    if (best == null || isBetterLocation(loc, best)) {
                        best = loc
                    }
                }
            } catch (_: Exception) {}
        }
        return best
    }

    /**
     * Mengambil lokasi cache terbaik langsung dari Context.
     */
    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return getBestLastKnownLocation(locationManager)
    }

    /**
     * Dapatkan lokasi instan terbaik (GPS/Network/Passive) secara synchronous/langsung.
     */
    @SuppressLint("MissingPermission")
    fun getInstantLocation(context: Context): UserGpsLocation {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "None", isFresh = false)

        val bestLoc = getBestLastKnownLocation(locationManager)

        return if (bestLoc != null) {
            UserGpsLocation(
                latitude = bestLoc.latitude,
                longitude = bestLoc.longitude,
                accuracyMeter = bestLoc.accuracy,
                isAvailable = true,
                provider = bestLoc.provider ?: "GPS",
                isFresh = isLocationFresh(bestLoc, 300)
            )
        } else {
            UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "Mencari Sinyal", isFresh = false)
        }
    }

    /**
     * Mengunci titik koordinat GPS Satelit Segar secara aktif (High Accuracy Lock).
     * Sangat penting untuk Mode Pesawat / 100% Offline di mana user menekan "Ambil Titik GPS" atau "Lokasi Saya".
     */
    @SuppressLint("MissingPermission")
    suspend fun acquireFreshSatelliteFix(
        context: Context,
        maxTimeoutMs: Long = 8000L,
        targetAccuracyMeters: Float = 30f
    ): UserGpsLocation = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            continuation.resumeWith(Result.success(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "None", isFresh = false)))
            return@suspendCancellableCoroutine
        }

        val fusedClient = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (_: Exception) {
            null
        }

        // Coba FusedLocationProviderClient terlebih dahulu (prioritas tertinggi Play Services)
        if (fusedClient != null) {
            try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { fusedLoc ->
                        if (fusedLoc != null && fusedLoc.latitude != 0.0 && fusedLoc.accuracy <= targetAccuracyMeters) {
                            if (continuation.isActive) {
                                continuation.resumeWith(
                                    Result.success(
                                        UserGpsLocation(
                                            latitude = fusedLoc.latitude,
                                            longitude = fusedLoc.longitude,
                                            accuracyMeter = fusedLoc.accuracy,
                                            isAvailable = true,
                                            provider = "Google Fused (Presisi Tinggi)",
                                            isFresh = true,
                                            bearing = if (fusedLoc.hasBearing()) fusedLoc.bearing else 0f,
                                            hasBearing = fusedLoc.hasBearing(),
                                            speedMps = if (fusedLoc.hasSpeed()) fusedLoc.speed else 0f,
                                            altitude = if (fusedLoc.hasAltitude()) fusedLoc.altitude else 0.0,
                                            timeMs = fusedLoc.time,
                                            isFused = true
                                        )
                                    )
                                )
                            }
                        }
                    }
            } catch (_: Exception) {}
        }
        var isGpsEnabled = false
        var isNetEnabled = false
        try {
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {}

        if (!isGpsEnabled && !isNetEnabled) {
            val cached = getInstantLocation(context)
            continuation.resumeWith(Result.success(cached.copy(provider = "GPS Nonaktif (Gunakan Pengaturan)")))
            return@suspendCancellableCoroutine
        }

        var bestLocationSoFar: Location? = getBestLastKnownLocation(locationManager)
        val isFinished = java.util.concurrent.atomic.AtomicBoolean(false)

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (isFinished.get()) return

                if (bestLocationSoFar == null || isBetterLocation(loc, bestLocationSoFar)) {
                    bestLocationSoFar = loc
                }

                // Jika sudah mencapai target akurasi
                if (loc.accuracy <= targetAccuracyMeters && loc.accuracy > 0f) {
                    if (isFinished.compareAndSet(false, true)) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: Exception) {}

                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.success(
                                    UserGpsLocation(
                                        latitude = loc.latitude,
                                        longitude = loc.longitude,
                                        accuracyMeter = loc.accuracy,
                                        isAvailable = true,
                                        provider = if (loc.provider == LocationManager.GPS_PROVIDER) "Satelit Standalone" else (loc.provider ?: "Network"),
                                        isFresh = true,
                                        bearing = if (loc.hasBearing()) loc.bearing else 0f,
                                        hasBearing = loc.hasBearing(),
                                        speedMps = if (loc.hasSpeed()) loc.speed else 0f,
                                        altitude = if (loc.hasAltitude()) loc.altitude else 0.0,
                                        timeMs = loc.time
                                    )
                                )
                            )
                        }
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val mainLooper = android.os.Looper.getMainLooper()
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener, mainLooper)
            }
            if (isNetEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500L, 0f, listener, mainLooper)
            }
        } catch (_: SecurityException) {
            if (isFinished.compareAndSet(false, true)) {
                continuation.resumeWith(Result.success(getInstantLocation(context)))
            }
            return@suspendCancellableCoroutine
        } catch (_: Exception) {
            if (isFinished.compareAndSet(false, true)) {
                continuation.resumeWith(Result.success(getInstantLocation(context)))
            }
            return@suspendCancellableCoroutine
        }

        // Timeout handler jika dalam X detik belum dapat < targetAccuracy, kembalikan lokasi terbaik yang berhasil ditangkap
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (isFinished.compareAndSet(false, true)) {
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}

                if (continuation.isActive) {
                    val finalLoc = bestLocationSoFar
                    if (finalLoc != null) {
                        continuation.resumeWith(
                            Result.success(
                                UserGpsLocation(
                                    latitude = finalLoc.latitude,
                                    longitude = finalLoc.longitude,
                                    accuracyMeter = finalLoc.accuracy,
                                    isAvailable = true,
                                    provider = finalLoc.provider ?: "GPS",
                                    isFresh = true,
                                    bearing = if (finalLoc.hasBearing()) finalLoc.bearing else 0f,
                                    hasBearing = finalLoc.hasBearing(),
                                    speedMps = if (finalLoc.hasSpeed()) finalLoc.speed else 0f,
                                    altitude = if (finalLoc.hasAltitude()) finalLoc.altitude else 0.0,
                                    timeMs = finalLoc.time
                                )
                            )
                        )
                    } else {
                        continuation.resumeWith(Result.success(getInstantLocation(context)))
                    }
                }
            }
        }
        handler.postDelayed(timeoutRunnable, maxTimeoutMs)

        continuation.invokeOnCancellation {
            if (isFinished.compareAndSet(false, true)) {
                handler.removeCallbacks(timeoutRunnable)
                try {
                    locationManager.removeUpdates(listener)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Terjemahkan koordinat GPS ke nama jalan / wilayah (Geocoder).
     */
    fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        try {
            if (android.location.Geocoder.isPresent()) {
                val geocoder = android.location.Geocoder(context, java.util.Locale("id", "ID"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val parts = mutableListOf<String>()
                    val thoroughfare = addr.thoroughfare ?: addr.featureName
                    val subThoroughfare = addr.subThoroughfare
                    if (!thoroughfare.isNullOrBlank()) {
                        parts.add(if (!subThoroughfare.isNullOrBlank()) "$thoroughfare No. $subThoroughfare" else thoroughfare)
                    }
                    val subLocality = addr.subLocality ?: addr.locality
                    if (!subLocality.isNullOrBlank() && !parts.contains(subLocality)) {
                        parts.add(subLocality)
                    }
                    val subAdminArea = addr.subAdminArea
                    if (!subAdminArea.isNullOrBlank() && !parts.contains(subAdminArea)) {
                        parts.add(subAdminArea)
                    }
                    val adminArea = addr.adminArea
                    if (!adminArea.isNullOrBlank() && !parts.contains(adminArea)) {
                        parts.add(adminArea)
                    }
                    if (parts.isNotEmpty()) {
                        return parts.joinToString(", ")
                    }
                }
            }
        } catch (_: Exception) {}
        return "Koordinat: ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)}"
    }

    /**
     * Buka rute navigasi Google Maps menuju 1 titik koordinat outlet.
     */
    fun openGoogleMapsNavigation(context: Context, lat: Double, lng: Double, outletName: String) {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val fallbackUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(outletName)})")
                val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka Google Maps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Buka Rute Multi-Stop (Turn-by-Turn / Multi-Waypoint) di Google Maps untuk semua warung dalam rute.
     * Google Maps akan otomatis membuat rute navigasi berurutan: Titik Sekarang -> Warung 1 -> Warung 2 -> ... -> Warung Terakhir.
     */
    fun openMultiStopGoogleMapsRoute(
        context: Context,
        warungs: List<WarungEntity>,
        routeTitle: String = "Rute Harian"
    ) {
        val validWarungs = warungs.filter { (it.latitude != 0.0 || it.longitude != 0.0) && it.status == "Aktif" }
        if (validWarungs.isEmpty()) {
            Toast.makeText(context, "Tidak ada outlet dengan koordinat GPS valid.", Toast.LENGTH_LONG).show()
            return
        }

        if (validWarungs.size == 1) {
            val single = validWarungs.first()
            openGoogleMapsNavigation(context, single.latitude, single.longitude, single.namaWarung)
            return
        }

        try {
            // Google Maps Directions API URL Scheme (Mendukung hingga banyak waypoints)
            val destination = "${validWarungs.last().latitude},${validWarungs.last().longitude}"
            val intermediateWarungs = validWarungs.subList(0, validWarungs.size - 1)
            val waypointsString = intermediateWarungs.joinToString("|") { "${it.latitude},${it.longitude}" }

            val uriBuilder = StringBuilder("https://www.google.com/maps/dir/?api=1")
            uriBuilder.append("&destination=").append(destination)
            if (waypointsString.isNotEmpty()) {
                uriBuilder.append("&waypoints=").append(Uri.encode(waypointsString))
            }
            uriBuilder.append("&travelmode=driving")

            val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString())).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (mapsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapsIntent)
            } else {
                // Fallback web browser jika Google Maps belum terinstall
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.toString()))
                context.startActivity(browserIntent)
            }
            Toast.makeText(context, "Membuka rute ${validWarungs.size} outlet di Google Maps...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka rute Google Maps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Ekspor seluruh data outlet ke format standar KML (Keyhole Markup Language) untuk Google My Maps / Google Earth.
     * Pin akan otomatis terkelompokkan dan diberi warna per Rute Kunjungan.
     */
    fun generateWarungsKml(
        warungs: List<WarungEntity>,
        rutes: List<RuteEntity>
    ): String {
        val ruteMap = rutes.associateBy { it.id }
        val colorPalette = listOf(
            "ff0000ff", // Red (KML is aabbggrr)
            "ff00a5ff", // Orange
            "ff00d7ff", // Yellow
            "ff00ff00", // Green
            "ffff0000", // Blue
            "ff800080", // Purple
            "ff808080"  // Gray
        )

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Titik Outlet SFA Konsinyasi</name>
    <description>Database Titik Koordinat Outlet dan Konsinyasi Warung</description>
""")

        // Styles per rute
        rutes.forEachIndexed { index, rute ->
            val color = colorPalette[index % colorPalette.size]
            sb.append("""
    <Style id="style_rute_${rute.id}">
      <IconStyle>
        <color>$color</color>
        <scale>1.1</scale>
        <Icon>
          <href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href>
        </Icon>
      </IconStyle>
    </Style>
""")
        }

        // Folder grouping by Rute
        val groupedByRute = warungs.groupBy { it.ruteId }
        groupedByRute.forEach { (ruteId, outletList) ->
            val ruteObj = ruteMap[ruteId]
            val ruteName = ruteObj?.namaRute ?: "Rute Umum"
            val hari = ruteObj?.hariKunjungan ?: "-"

            sb.append("""
    <Folder>
      <name><![CDATA[$ruteName ($hari)]]></name>
""")
            outletList.forEach { w ->
                if (w.latitude != 0.0 || w.longitude != 0.0) {
                    val descHtml = """
                      <![CDATA[
                        <h3>${w.namaWarung}</h3>
                        <p><b>Pemilik:</b> ${w.namaPemilik.ifBlank { "-" }}</p>
                        <p><b>No HP:</b> ${w.noHp.ifBlank { "-" }}</p>
                        <p><b>Kategori:</b> ${w.kategoriWarung}</p>
                        <p><b>Alamat:</b> ${w.alamatLengkap}</p>
                        <p><b>Rute:</b> $ruteName ($hari)</p>
                        <p><b>Total Piutang/Bon:</b> Rp ${String.format(Locale.GERMAN, "%,d", w.saldoPiutang.toLong())}</p>
                        <p><b>Titipan Aktif:</b> ${w.stokTitipanPcs} Pcs</p>
                        <p><b>Catatan:</b> ${w.notes.ifBlank { "-" }}</p>
                      ]]>
                    """.trimIndent()

                    sb.append("""
      <Placemark>
        <name><![CDATA[${w.namaWarung}]]></name>
        <description>$descHtml</description>
        <styleUrl>#style_rute_$ruteId</styleUrl>
        <Point>
          <coordinates>${w.longitude},${w.latitude},0</coordinates>
        </Point>
      </Placemark>
""")
                }
            }
            sb.append("""
    </Folder>
""")
        }

        sb.append("""
  </Document>
</kml>
""")
        return sb.toString()
    }

    /**
     * Ekspor seluruh data outlet ke format CSV untuk import ke Google My Maps atau Excel.
     */
    fun generateWarungsCsv(
        warungs: List<WarungEntity>,
        rutes: List<RuteEntity>
    ): String {
        val ruteMap = rutes.associateBy { it.id }
        val sb = StringBuilder()
        sb.append("Nama Outlet,Pemilik,Nomor HP,Kategori,Alamat Lengkap,Jalur Rute,Hari Kunjungan,Latitude,Longitude,Limit Bon,Total Piutang,Total Titipan Aktif,Status,Catatan\n")

        warungs.forEach { w ->
            val ruteObj = ruteMap[w.ruteId]
            val ruteName = ruteObj?.namaRute ?: "-"
            val hari = ruteObj?.hariKunjungan ?: "-"

            val line = listOf(
                escapeCsv(w.namaWarung),
                escapeCsv(w.namaPemilik),
                escapeCsv(w.noHp),
                escapeCsv(w.kategoriWarung),
                escapeCsv(w.alamatLengkap),
                escapeCsv(ruteName),
                escapeCsv(hari),
                w.latitude.toString(),
                w.longitude.toString(),
                w.limitHutangMaksimal.toLong().toString(),
                w.saldoPiutang.toLong().toString(),
                w.stokTitipanPcs.toString(),
                escapeCsv(w.status),
                escapeCsv(w.notes)
            ).joinToString(",")

            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        val cleaned = text.replace("\"", "\"\"").replace("\n", " ").trim()
        return "\"$cleaned\""
    }

    /**
     * Simpan file hasil generate ke cache dan buka dialog Share/Kirim (WhatsApp, Drive, Email, dll).
     */
    fun shareExportedMapFile(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String
    ) {
        try {
            val mapsDir = File(context.cacheDir, "maps")
            if (!mapsDir.exists()) mapsDir.mkdirs()

            val file = File(mapsDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, "Data Peta Titik Outlet: $fileName\nImport ke Google My Maps (mymaps.google.com)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Kirim/Simpan File Peta: $fileName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengekspor file peta: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
