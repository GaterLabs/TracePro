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
    val satellitesUsed: Int = 0
)

object LocationHelper {

    // Default fallback coordinates (Jakarta Pusat / Monas area)
    const val DEFAULT_LAT = -6.2088
    const val DEFAULT_LNG = 106.8456

    /**
     * Mengecek apakah koordinat lokasi masih segar (bukan cache lama).
     */
    fun isLocationFresh(loc: Location, maxAgeSeconds: Long = 90): Boolean {
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
     * Dapatkan Flow update lokasi GPS satelit terkini secara real-time.
     * Didesain 100% OFFLINE / AIRPLANE MODE: Mengutamakan chip GNSS Satelit Fisik (0m distance delta, 1s interval).
     */
    @SuppressLint("MissingPermission")
    fun observeCurrentLocation(context: Context): Flow<UserGpsLocation> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (locationManager == null) {
            trySend(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "None", isFresh = false))
            close()
            return@callbackFlow
        }

        // Coba ambil last known location terbaik HANYA jika segar (< 60 detik) & akurat
        var initialBestLoc: Location? = null
        try {
            val gpsLast = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            if (gpsLast != null && isLocationFresh(gpsLast, 60)) {
                initialBestLoc = gpsLast
            } else {
                val netLast = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
                if (netLast != null && isLocationFresh(netLast, 30)) {
                    initialBestLoc = netLast
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {}

        if (initialBestLoc != null) {
            trySend(
                UserGpsLocation(
                    latitude = initialBestLoc.latitude,
                    longitude = initialBestLoc.longitude,
                    accuracyMeter = initialBestLoc.accuracy,
                    isAvailable = true,
                    provider = initialBestLoc.provider ?: "GPS",
                    isFresh = true
                )
            )
        } else {
            trySend(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "Searching", isFresh = false))
        }

        var lastEmittedLat = 0.0
        var lastEmittedLng = 0.0
        var lastEmittedTime = 0L
        var lastSatelliteFixTime = 0L

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val isGps = location.provider == LocationManager.GPS_PROVIDER
                val now = System.currentTimeMillis()

                if (isGps) {
                    lastSatelliteFixTime = now
                } else {
                    // Hanya gunakan network jika satelit belum kirim fix dalam 15 detik terakhir
                    if (now - lastSatelliteFixTime <= 15_000L) {
                        return
                    }
                }

                // Filter perubahan: hanya emit jika geser >= 10m atau sudah lewat >= 5 detik
                val dist = calculateDistanceMeters(lastEmittedLat, lastEmittedLng, location.latitude, location.longitude)
                if (lastEmittedLat == 0.0 || dist >= 10.0 || (now - lastEmittedTime >= 6_000L)) {
                    lastEmittedLat = location.latitude
                    lastEmittedLng = location.longitude
                    lastEmittedTime = now

                    trySend(
                        UserGpsLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeter = location.accuracy,
                            isAvailable = true,
                            provider = if (isGps) "Satelit GPS" else (location.provider ?: "Network"),
                            isFresh = true
                        )
                    )
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // Mode Hemat Baterai & Responsif: interval 5000ms dan 10m jarak filter
            val mainLooper = android.os.Looper.getMainLooper()
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, listener, mainLooper)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, listener, mainLooper)
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {}

        awaitClose {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }
    }

    /**
     * Dapatkan lokasi instan terbaik (GPS/Network) secara synchronous/langsung.
     * Memfilter cache kadaluwarsa agar tidak mengembalikan koordinat palsu / rumah lama.
     */
    @SuppressLint("MissingPermission")
    fun getInstantLocation(context: Context): UserGpsLocation {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "None", isFresh = false)

        var bestLoc: Location? = null
        try {
            // Prioritas 1: GPS Hardware Satelit
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (gpsLoc != null && isLocationFresh(gpsLoc, 90)) {
                    bestLoc = gpsLoc
                }
            }
            // Prioritas 2: Network jika GPS belum ada & masih fresh
            if (bestLoc == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (netLoc != null && isLocationFresh(netLoc, 45)) {
                    bestLoc = netLoc
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {}

        return if (bestLoc != null) {
            UserGpsLocation(
                latitude = bestLoc.latitude,
                longitude = bestLoc.longitude,
                accuracyMeter = bestLoc.accuracy,
                isAvailable = true,
                provider = bestLoc.provider ?: "GPS",
                isFresh = true
            )
        } else {
            UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "Mencari Sinyal", isFresh = false)
        }
    }

    /**
     * Mengunci titik koordinat GPS Satelit Segar secara aktif (High Accuracy Lock).
     * Sangat penting untuk Mode Pesawat / 100% Offline di mana user menekan "Ambil Titik GPS".
     * Fungsi ini akan standby menunggu chip satelit HP memberikan fix dengan akurasi tinggi (< targetAccuracyMeters).
     */
    @SuppressLint("MissingPermission")
    suspend fun acquireFreshSatelliteFix(
        context: Context,
        maxTimeoutMs: Long = 10000L,
        targetAccuracyMeters: Float = 25f
    ): UserGpsLocation = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            continuation.resumeWith(Result.success(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "None", isFresh = false)))
            return@suspendCancellableCoroutine
        }

        // Periksa apakah GPS aktif
        var isGpsEnabled = false
        var isNetEnabled = false
        try {
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {}

        if (!isGpsEnabled && !isNetEnabled) {
            continuation.resumeWith(Result.success(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "GPS Nonaktif", isFresh = false)))
            return@suspendCancellableCoroutine
        }

        var bestLocationSoFar: Location? = null
        val isFinished = java.util.concurrent.atomic.AtomicBoolean(false)

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (isFinished.get()) return

                if (bestLocationSoFar == null || loc.accuracy < (bestLocationSoFar?.accuracy ?: Float.MAX_VALUE)) {
                    bestLocationSoFar = loc
                }

                // Jika sudah mencapai target akurasi satelit (misal <= 25 meter)
                if (loc.accuracy <= targetAccuracyMeters && loc.accuracy > 0f) {
                    if (isFinished.compareAndSet(false, true)) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (_: SecurityException) {
                        } catch (_: Exception) {}

                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.success(
                                    UserGpsLocation(
                                        latitude = loc.latitude,
                                        longitude = loc.longitude,
                                        accuracyMeter = loc.accuracy,
                                        isAvailable = true,
                                        provider = "Satelit Standalone",
                                        isFresh = true
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
                continuation.resumeWith(Result.success(UserGpsLocation(DEFAULT_LAT, DEFAULT_LNG, isAvailable = false, provider = "Izin Ditolak", isFresh = false)))
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
                } catch (_: SecurityException) {
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
                                    provider = "Satelit GPS",
                                    isFresh = true
                                )
                            )
                        )
                    } else {
                        // Fallback ke instant location terakhir
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
                } catch (_: SecurityException) {
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
