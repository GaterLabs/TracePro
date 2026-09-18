package com.example.util.maps

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.*

/**
 * Data model for a detailed Turn-by-Turn step maneuver along the road network.
 */
data class RouteStep(
    val instruction: String,
    val streetName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String,
    val modifier: String,
    val location: Pair<Double, Double> // Lat, Lng
)

/**
 * Data model for the complete road-following route.
 */
data class RoadRouteResult(
    val polylinePoints: List<Pair<Double, Double>>, // Lat, Lng coordinates along the actual road curves
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val steps: List<RouteStep>,
    val isRoadSnapped: Boolean = true
)

/**
 * High-performance Road Network Routing Engine using OpenStreetMap & OSRM (Open Source Routing Machine).
 * Provides true road-following polylines (tidak garis lurus) and actual street-level turn-by-turn maneuvers.
 */
object OsmRoutingEngine {

    private const val TAG = "OsmRoutingEngine"
    private const val USER_AGENT = "SFASalesMobile/2.0 (Android; OpenStreetMap OSRM Routing)"

    // In-memory cache for computed routes between coordinate pairs
    private val routeCache = mutableMapOf<String, RoadRouteResult>()

    /**
     * Fetch a real road-following route from origin to destination through any number of waypoints.
     * Uses OSRM Driving profile (`/route/v1/driving/`).
     */
    suspend fun getRoadRoute(
        coordinates: List<Pair<Double, Double>> // List of (Lat, Lng)
    ): RoadRouteResult = withContext(Dispatchers.IO) {
        if (coordinates.size < 2) {
            return@withContext RoadRouteResult(
                polylinePoints = coordinates,
                totalDistanceMeters = 0.0,
                totalDurationSeconds = 0.0,
                steps = emptyList(),
                isRoadSnapped = false
            )
        }

        // Cache key based on rounded coordinates to 5 decimal places (~1m)
        val cacheKey = coordinates.joinToString(";") {
            "${String.format(Locale.US, "%.4f", it.first)},${String.format(Locale.US, "%.4f", it.second)}"
        }

        synchronized(routeCache) {
            routeCache[cacheKey]?.let { return@withContext it }
        }

        try {
            // OSRM expects coordinates in "lng,lat" order separated by semicolons
            val coordsString = coordinates.joinToString(";") { "${it.second},${it.first}" }
            val urlString = "https://router.project-osrm.org/route/v1/driving/$coordsString?overview=full&geometries=polyline&steps=true&annotations=false"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val code = json.optString("code")
                if (code == "Ok") {
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val encodedPolyline = firstRoute.getString("geometry")
                        val distance = firstRoute.getDouble("distance")
                        val duration = firstRoute.getDouble("duration")

                        // Decode Google/OSRM Polyline algorithm into (Lat, Lng) points
                        val points = decodePolyline(encodedPolyline)

                        // Parse turn-by-turn steps
                        val stepsList = mutableListOf<RouteStep>()
                        val legs = firstRoute.optJSONArray("legs")
                        if (legs != null) {
                            for (l in 0 until legs.length()) {
                                val leg = legs.getJSONObject(l)
                                val steps = leg.optJSONArray("steps")
                                if (steps != null) {
                                    for (s in 0 until steps.length()) {
                                        val stepObj = steps.getJSONObject(s)
                                        val stepDist = stepObj.optDouble("distance", 0.0)
                                        val stepDur = stepObj.optDouble("duration", 0.0)
                                        val name = stepObj.optString("name", "")
                                        val maneuver = stepObj.optJSONObject("maneuver")
                                        val mType = maneuver?.optString("type") ?: "turn"
                                        val mMod = maneuver?.optString("modifier") ?: "straight"
                                        val locArr = maneuver?.optJSONArray("location")
                                        val stepLoc = if (locArr != null && locArr.length() >= 2) {
                                            Pair(locArr.getDouble(1), locArr.getDouble(0))
                                        } else {
                                            points.firstOrNull() ?: Pair(0.0, 0.0)
                                        }

                                        val instruction = generateIndonesianInstruction(mType, mMod, name, stepDist)
                                        stepsList.add(
                                            RouteStep(
                                                instruction = instruction,
                                                streetName = name.ifBlank { "Jalan Utama" },
                                                distanceMeters = stepDist,
                                                durationSeconds = stepDur,
                                                maneuverType = mType,
                                                modifier = mMod,
                                                location = stepLoc
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val result = RoadRouteResult(
                            polylinePoints = points,
                            totalDistanceMeters = distance,
                            totalDurationSeconds = duration,
                            steps = stepsList,
                            isRoadSnapped = true
                        )

                        synchronized(routeCache) {
                            if (routeCache.size > 50) routeCache.clear()
                            routeCache[cacheKey] = result
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM road routing fallback due to: ${e.message}")
        }

        // Offline / Error Fallback: Direct line connection
        val fallbackResult = RoadRouteResult(
            polylinePoints = coordinates,
            totalDistanceMeters = calculateDirectDistance(coordinates),
            totalDurationSeconds = calculateDirectDistance(coordinates) / 8.33, // avg 30 km/h
            steps = emptyList(),
            isRoadSnapped = false
        )
        return@withContext fallbackResult
    }

    /**
     * Decodes an encoded polyline string (Precision 5, standard OSRM / Google Polyline).
     */
    fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = ArrayList<Pair<Double, Double>>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val pLat = lat.toDouble() / 1E5
            val pLng = lng.toDouble() / 1E5
            poly.add(Pair(pLat, pLng))
        }

        return poly
    }

    /**
     * Helper to calculate simple direct distance across coordinates in meters
     */
    private fun calculateDirectDistance(points: List<Pair<Double, Double>>): Double {
        var dist = 0.0
        for (i in 0 until points.size - 1) {
            dist += haversineMeters(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second)
        }
        return dist
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates the minimum perpendicular distance (in meters) from a user's location to the nearest point on the route polyline.
     */
    fun getDistanceToPolylineMeters(lat: Double, lng: Double, polyline: List<Pair<Double, Double>>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineMeters(lat, lng, polyline[0].first, polyline[0].second)

        var minDistance = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val p1 = polyline[i]
            val p2 = polyline[i + 1]
            val dist = distanceToSegmentMeters(lat, lng, p1.first, p1.second, p2.first, p2.second)
            if (dist < minDistance) {
                minDistance = dist
            }
        }
        return minDistance
    }

    /**
     * Checks if the user is currently deviating/off the planned road route (threshold typically 45m).
     */
    fun isOffRoute(lat: Double, lng: Double, polyline: List<Pair<Double, Double>>, thresholdMeters: Double = 45.0): Boolean {
        if (polyline.size < 2) return false
        val dist = getDistanceToPolylineMeters(lat, lng, polyline)
        return dist > thresholdMeters
    }

    private fun distanceToSegmentMeters(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Double {
        val dx = bLng - aLng
        val dy = bLat - aLat
        if (dx == 0.0 && dy == 0.0) {
            return haversineMeters(pLat, pLng, aLat, aLng)
        }

        val t = ((pLng - aLng) * dx + (pLat - aLat) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0.0, 1.0)
        val projLat = aLat + clampedT * dy
        val projLng = aLng + clampedT * dx

        return haversineMeters(pLat, pLng, projLat, projLng)
    }

    /**
     * Subdivides a coarse polyline into dense, high-resolution equidistant waypoints (e.g. every ~1.5 - 2 meters),
     * enabling fluid 60-FPS vehicle animation without discrete jumping or stuttering.
     */
    fun interpolateDensePath(
        polyline: List<Pair<Double, Double>>,
        targetIntervalMeters: Double = 1.8
    ): List<Pair<Double, Double>> {
        if (polyline.size < 2) return polyline
        val dense = ArrayList<Pair<Double, Double>>()
        dense.add(polyline.first())

        for (i in 0 until polyline.size - 1) {
            val p1 = polyline[i]
            val p2 = polyline[i + 1]
            val dist = haversineMeters(p1.first, p1.second, p2.first, p2.second)
            if (dist <= targetIntervalMeters) {
                dense.add(p2)
            } else {
                val segments = max(1, (dist / targetIntervalMeters).roundToInt())
                for (s in 1..segments) {
                    val frac = s.toDouble() / segments
                    val lat = p1.first + (p2.first - p1.first) * frac
                    val lng = p1.second + (p2.second - p1.second) * frac
                    dense.add(Pair(lat, lng))
                }
            }
        }
        return dense
    }

    /**
     * Calculates heading/bearing angle in degrees (0..360) from point A to point B.
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    /**
     * Translates OSRM maneuver type & modifier to natural Indonesian turn-by-turn guidance.
     */
    private fun generateIndonesianInstruction(
        type: String,
        modifier: String,
        street: String,
        distanceMeters: Double
    ): String {
        val streetSuffix = if (street.isNotBlank()) " menuju $street" else ""
        return when (type) {
            "depart" -> "Mulai perjalanan$streetSuffix"
            "arrive" -> "Tiba di lokasi outlet tujuan"
            "turn" -> when (modifier) {
                "sharp right" -> "Belok tajam ke kanan$streetSuffix"
                "right" -> "Belok kanan$streetSuffix"
                "slight right" -> "Serong kanan$streetSuffix"
                "sharp left" -> "Belok tajam ke kiri$streetSuffix"
                "left" -> "Belok kiri$streetSuffix"
                "slight left" -> "Serong kiri$streetSuffix"
                "uturn" -> "Putar balik di U-Turn"
                else -> "Lurus terus$streetSuffix"
            }
            "fork" -> when (modifier) {
                "left", "slight left" -> "Ambil jalur kiri di percabangan$streetSuffix"
                "right", "slight right" -> "Ambil jalur kanan di percabangan$streetSuffix"
                else -> "Tetap di jalur utama"
            }
            "roundabout", "rotary" -> "Masuk bundaran dan ambil jalan keluar$streetSuffix"
            "end of road" -> when (modifier) {
                "left" -> "Di ujung jalan, belok kiri$streetSuffix"
                "right" -> "Di ujung jalan, belok kanan$streetSuffix"
                else -> "Di ujung jalan, lanjutkan$streetSuffix"
            }
            "continue", "new name" -> "Lurus terus di $street"
            else -> "Lurus ikuti jalan$streetSuffix"
        }
    }
}
