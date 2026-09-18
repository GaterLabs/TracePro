package com.example.util

import kotlin.math.*

/**
 * 1D Kalman Filter for GPS latitude and longitude smoothing.
 *
 * Removes jitter, spikes, and random satellite deviations while maintaining
 * quick reaction when the user genuinely moves.
 */
class GpsKalmanFilter(
    private val processNoiseQ: Double = 0.000003, // Process noise covariance (Q)
    private val measurementNoiseR: Double = 0.00005 // Default measurement noise covariance (R)
) {
    private var latEstimate: Double = 0.0
    private var lngEstimate: Double = 0.0
    private var variance: Double = -1.0 // Uninitialized

    val isInitialized: Boolean get() = variance >= 0.0

    /**
     * Resets the Kalman filter state to uninitialized.
     */
    fun reset() {
        variance = -1.0
        latEstimate = 0.0
        lngEstimate = 0.0
    }

    /**
     * Updates the filter with a new raw GPS observation and returns the smoothed (lat, lng).
     *
     * @param lat Raw latitude from GPS sensor
     * @param lng Raw longitude from GPS sensor
     * @param accuracyMeters Reported GPS accuracy in meters (used to dynamically adjust measurement noise R)
     * @return Smoothed Pair(latitude, longitude)
     */
    fun filter(lat: Double, lng: Double, accuracyMeters: Float = 10f): Pair<Double, Double> {
        if (variance < 0.0) {
            // First fix: initialize directly
            latEstimate = lat
            lngEstimate = lng
            variance = (accuracyMeters * accuracyMeters).toDouble() * 1e-9
            return Pair(latEstimate, lngEstimate)
        }

        // 1. Prediction step: increase variance by process noise
        variance += processNoiseQ

        // 2. Dynamic measurement noise R based on sensor accuracy
        val dynamicR = max(1e-7, (accuracyMeters * accuracyMeters).toDouble() * 1e-9)

        // 3. Kalman Gain K = P / (P + R)
        val k = variance / (variance + dynamicR)

        // 4. Update estimate with measurement
        latEstimate += k * (lat - latEstimate)
        lngEstimate += k * (lng - lngEstimate)

        // 5. Update error covariance P = (1 - K) * P
        variance = (1.0 - k) * variance

        return Pair(latEstimate, lngEstimate)
    }

    /**
     * Get current best smoothed coordinate estimate.
     */
    fun getEstimate(): Pair<Double, Double> = Pair(latEstimate, lngEstimate)
}

/**
 * Map Matching / Snap-to-Route Utility.
 *
 * Projects a raw GPS point onto the closest polyline route segment.
 * If the user is within [snapThresholdMeters] (e.g. 25m) of the route, snaps their position
 * directly onto the road/path center line, eliminating off-road drift.
 */
object MapMatchingHelper {

    /**
     * Snaps a coordinate (lat, lng) to the nearest segment of [routeWaypoints].
     *
     * @return Snapped Pair(lat, lng) if within threshold, or the original point if farther away.
     */
    fun snapToRoute(
        lat: Double,
        lng: Double,
        routeWaypoints: List<Pair<Double, Double>>,
        snapThresholdMeters: Double = 25.0
    ): Pair<Double, Double> {
        if (routeWaypoints.size < 2) return Pair(lat, lng)

        var minDistance = Double.MAX_VALUE
        var bestPoint = Pair(lat, lng)

        for (i in 0 until routeWaypoints.size - 1) {
            val a = routeWaypoints[i]
            val b = routeWaypoints[i + 1]

            val projected = projectPointOnSegment(lat, lng, a.first, a.second, b.first, b.second)
            val dist = LocationHelper.calculateDistanceMeters(lat, lng, projected.first, projected.second)

            if (dist < minDistance) {
                minDistance = dist
                bestPoint = projected
            }
        }

        return if (minDistance <= snapThresholdMeters) bestPoint else Pair(lat, lng)
    }

    /**
     * Projects point P (pLat, pLng) onto line segment AB (aLat, aLng) -> (bLat, bLng).
     */
    private fun projectPointOnSegment(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Pair<Double, Double> {
        val cosLat = cos(Math.toRadians(aLat))
        val x = (pLng - aLng) * cosLat
        val y = pLat - aLat
        val dx = (bLng - aLng) * cosLat
        val dy = bLat - aLat

        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return Pair(aLat, aLng)

        val t = ((x * dx + y * dy) / lenSq).coerceIn(0.0, 1.0)
        val projLat = aLat + t * (bLat - aLat)
        val projLng = aLng + t * (bLng - aLng)

        return Pair(projLat, projLng)
    }
}
