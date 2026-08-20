package com.example.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.example.data.local.entity.WarungEntity
import com.example.data.repository.SfaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.Locale

object OfflineSyncHelper {

    /**
     * Cek apakah perangkat terhubung ke internet secara aktif
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Aliran reactive status koneksi online/offline
     */
    fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        // Kirim status awal
        trySend(isNetworkAvailable(context))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Terjemahkan Titik Koordinat GPS (Latitude, Longitude) menjadi Alamat Jalan Nyata (Reverse Geocoding).
     * Bekerja aman (graceful) di background thread, tidak memblokir UI jika offline.
     */
    suspend fun reverseGeocodeCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            return@withContext null
        }

        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            @Suppress("DEPRECATION")
            val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (!addressList.isNullOrEmpty()) {
                val addr = addressList[0]
                val street = addr.thoroughfare ?: addr.featureName ?: ""
                val subDistrict = addr.subLocality ?: addr.locality ?: ""
                val city = addr.subAdminArea ?: addr.adminArea ?: ""
                val postal = addr.postalCode ?: ""

                val fullAddress = addr.getAddressLine(0)
                if (!fullAddress.isNullOrBlank()) {
                    return@withContext fullAddress
                }

                val parts = listOf(street, subDistrict, city, postal).filter { it.isNotBlank() }
                if (parts.isNotEmpty()) {
                    return@withContext parts.joinToString(", ")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Sinkronisasi otomatis: mencari semua warung yang didaftarkan saat offline
     * lalu menerjemahkan koordinatnya ke alamat jalan resmi saat online.
     */
    suspend fun syncPendingWarungAddresses(
        context: Context,
        repository: SfaRepository
    ): Int = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) return@withContext 0

        val pendingWarungs = repository.getWarungsPendingAddressSync()
        if (pendingWarungs.isEmpty()) return@withContext 0

        var successCount = 0
        for (warung in pendingWarungs) {
            val resolvedAddress = reverseGeocodeCoordinates(context, warung.latitude, warung.longitude)
            if (!resolvedAddress.isNullOrBlank()) {
                val updatedWarung = warung.copy(
                    alamatLengkap = resolvedAddress,
                    pendingAddressSync = false
                )
                repository.saveWarung(updatedWarung)
                successCount++
            }
        }
        successCount
    }
}
