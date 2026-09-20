package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RuteEntity
import com.example.data.local.entity.WarungEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.AppStrings
import com.example.util.CompassSensorHelper
import com.example.util.MapMatchingHelper
import com.example.util.LocationHelper
import com.example.util.UserGpsLocation
import com.example.util.maps.OsmRoutingEngine
import com.example.util.maps.OsmTileEngine
import com.example.util.maps.RoadRouteResult
import com.example.util.maps.RouteStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

enum class MapLayerStyle(
    val labelId: String,
    val labelEn: String,
    val bgColor: Color,
    val gridColor: Color,
    val roadColor: Color
) {
    OSM_STANDARD("Standar OSM", "Standard OSM", Color(0xFFF2EFE9), Color(0xFFE5E0D5), Color(0xFFFFFFFF)),
    CARTO_LIGHT("Terang (Light)", "Carto Light", Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFFFFFFF)),
    DARK_NIGHT("Malam (Dark)", "Dark Night", Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)),
    OUTDOOR_TOPO("Topografi", "Topography", Color(0xFFEAF2E8), Color(0xFFD3E4D0), Color(0xFFFFFFFF))
}

enum class ManeuverType(val icon: ImageVector, val descId: String, val descEn: String) {
    STRAIGHT(Icons.Default.Straight, "Lurus terus di jalur utama", "Continue straight on main road"),
    TURN_RIGHT(Icons.Default.TurnRight, "Belok kanan di persimpangan depan", "Turn right at next intersection"),
    TURN_LEFT(Icons.Default.TurnLeft, "Belok kiri di persimpangan depan", "Turn left at next intersection"),
    SLIGHT_RIGHT(Icons.Default.TurnSlightRight, "Ambil jalur serong kanan", "Keep slight right"),
    SLIGHT_LEFT(Icons.Default.TurnSlightLeft, "Ambil jalur serong kiri", "Keep slight left"),
    U_TURN(Icons.Default.UTurnLeft, "Putar balik di u-turn terdekat", "Make a U-turn when safe"),
    ARRIVED(Icons.Default.CheckCircle, "Tiba di tujuan outlet!", "Arrived at destination outlet!")
}

data class WaypointItem(
    val warung: WarungEntity,
    val sequence: Int,
    val distanceMeters: Double,
    val isVisitedToday: Boolean,
    val estimatedMinutes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(
    viewModel: SfaViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.appLanguage.collectAsState()
    val allWarungs by viewModel.warungs.collectAsState()
    val allRutes by viewModel.rutes.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val userGps by viewModel.currentGpsLocation.collectAsState()
    val selectedRuteId by viewModel.selectedRuteId.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Map Viewport state (Center Lat/Lng, Zoom Level, 3D Pitch/Tilt, Bearing/Rotation)
    var mapCenterLat by remember { mutableStateOf(userGps.latitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LAT) }
    var mapCenterLng by remember { mutableStateOf(userGps.longitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LNG) }
    var zoomLevel by remember { mutableFloatStateOf(14.5f) } // 4.0f to 20.0f (Ultra-wide country view down to street detail)
    var mapTiltDeg by remember { mutableFloatStateOf(0f) } // 0f (2D Flat) to 55f (3D Perspective Pitch)
    var mapBearingDeg by remember { mutableFloatStateOf(0f) } // Compass rotation angle in degrees
    val is3DMode by remember { derivedStateOf { mapTiltDeg > 15f } }

    // Map style & display filters
    var currentMapStyle by remember { mutableStateOf(MapLayerStyle.OSM_STANDARD) }
    var showOnlyUnvisited by remember { mutableStateOf(false) }
    var showPolylineRoute by remember { mutableStateOf(true) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var showNavBanner by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Selected Pin & Waypoint sheet state
    var selectedWarung by remember { mutableStateOf<WarungEntity?>(null) }
    var showWaypointListSheet by remember { mutableStateOf(false) }
    var activeTargetIndex by remember { mutableIntStateOf(0) }

    // Add Marker / Pin Dropper & Outlet Creation state
    var droppedPinLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isCrosshairMode by remember { mutableStateOf(false) }
    var showAddMarkerSheet by remember { mutableStateOf(false) }

    // Offline Cache management state & dialog
    var showOfflineDialog by remember { mutableStateOf(false) }
    var isDownloadingOffline by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("") }
    var cacheSizeMb by remember { mutableDoubleStateOf(0.0) }

    // Physical Compass & Device Orientation Sensor (Magnetometer + Accelerometer / Rotation Vector)
    val compassHeadingDeg by CompassSensorHelper.observeHeading(context).collectAsState(initial = 0f)

    // Navigation Mode (Real GPS vs Demo Simulation)
    var isSimulatingNav by remember { mutableStateOf(false) }
    var simGpsLat by remember { mutableDoubleStateOf(userGps.latitude) }
    var simGpsLng by remember { mutableDoubleStateOf(userGps.longitude) }
    var simVehicleBearingDeg by remember { mutableFloatStateOf(0f) }
    var simSpeedMultiplier by remember { mutableIntStateOf(1) }
    var isAcquiringGps by remember { mutableStateOf(false) }
    var hasCenteredUserLocation by remember { mutableStateOf(false) }
    var isFollowingUserGps by remember { mutableStateOf(true) }

    // Dynamic Location Permission Launcher for Direct Acquisition
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            isAcquiringGps = true
            coroutineScope.launch {
                try {
                    val loc = viewModel.acquireAccurateGps()
                    if (loc.isAvailable && loc.latitude != 0.0) {
                        isFollowingUserGps = true
                        mapCenterLat = loc.latitude
                        mapCenterLng = loc.longitude
                        zoomLevel = 16.5f
                        Toast.makeText(context, AppStrings.tr("📍 Lokasi GPS Anda terkunci (±${loc.accuracyMeter.toInt()}m)", "📍 GPS locked (±${loc.accuracyMeter.toInt()}m)", lang), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isAcquiringGps = false
                }
            }
        } else {
            Toast.makeText(context, AppStrings.tr("Izin lokasi GPS diperlukan untuk mendeteksi posisi Anda.", "GPS permission required to detect your location.", lang), Toast.LENGTH_LONG).show()
        }
    }

    // Auto-center map to real user GPS once acquired and follow dynamically while moving
    LaunchedEffect(userGps.isAvailable, userGps.latitude, userGps.longitude, isFollowingUserGps) {
        if (userGps.isAvailable && userGps.latitude != 0.0 && !isSimulatingNav) {
            if (!hasCenteredUserLocation) {
                hasCenteredUserLocation = true
                mapCenterLat = userGps.latitude
                mapCenterLng = userGps.longitude
                zoomLevel = 16.0f
            } else if (isFollowingUserGps && !isCrosshairMode) {
                mapCenterLat = userGps.latitude
                mapCenterLng = userGps.longitude
            }
        }
    }

    // Road Snapping & Real OSRM Network Route state
    var roadRouteResult by remember { mutableStateOf<RoadRouteResult?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var isOffRoute by remember { mutableStateOf(false) }
    var isRerouting by remember { mutableStateOf(false) }

    // Trigger state to force map tile redraw on tile downloaded
    var tileRefreshKey by remember { mutableIntStateOf(0) }

    // Calculate Cache Size on load
    LaunchedEffect(Unit) {
        val bytes = OsmTileEngine.getOfflineCacheSizeBytes(context)
        cacheSizeMb = bytes / (1024.0 * 1024.0)
    }

    // Effective GPS coordinate (Hardware, Filtered, Snap-to-Route, or Simulation)
    val effectiveGps = remember(isSimulatingNav, simGpsLat, simGpsLng, userGps, roadRouteResult) {
        if (isSimulatingNav) {
            UserGpsLocation(simGpsLat, simGpsLng, accuracyMeter = 5f, provider = "SIMULATOR")
        } else {
            val routePolyline = roadRouteResult?.polylinePoints
            if (!routePolyline.isNullOrEmpty() && routePolyline.size >= 2 && userGps.latitude != 0.0) {
                // Apply Snap-to-Route map matching if salesman is within 25 meters of the planned route
                val snapped = MapMatchingHelper.snapToRoute(
                    lat = userGps.latitude,
                    lng = userGps.longitude,
                    routeWaypoints = routePolyline,
                    snapThresholdMeters = 25.0
                )
                userGps.copy(latitude = snapped.first, longitude = snapped.second)
            } else {
                userGps
            }
        }
    }

    // Set of warung IDs visited today
    val todayVisitedIds = remember(transactions) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        transactions.filter { it.tanggal == todayStr }.map { it.warungId }.toSet()
    }

    // Filtered warungs with valid GPS coordinates
    val filteredWarungs = remember(allWarungs, selectedRuteId, showOnlyUnvisited, todayVisitedIds, searchQuery) {
        allWarungs.filter { w ->
            (w.latitude != 0.0 || w.longitude != 0.0) &&
            (selectedRuteId == null || w.ruteId == selectedRuteId) &&
            (!showOnlyUnvisited || !todayVisitedIds.contains(w.id)) &&
            (searchQuery.isBlank() || w.namaWarung.contains(searchQuery, ignoreCase = true) || w.alamatLengkap.contains(searchQuery, ignoreCase = true))
        }.sortedBy { it.urutanKunjungan }
    }

    // Fetch real road-following geometry (OSRM OpenStreetMap Driving Profile) & Auto-Reroute on Deviation
    LaunchedEffect(filteredWarungs, userGps.latitude, userGps.longitude) {
        if (filteredWarungs.isNotEmpty() && !isSimulatingNav) {
            val startLat = if (userGps.latitude != 0.0) userGps.latitude else LocationHelper.DEFAULT_LAT
            val startLng = if (userGps.longitude != 0.0) userGps.longitude else LocationHelper.DEFAULT_LNG

            val polyline = roadRouteResult?.polylinePoints
            val offRoute = if (polyline != null && polyline.size >= 2 && userGps.latitude != 0.0) {
                OsmRoutingEngine.isOffRoute(userGps.latitude, userGps.longitude, polyline, thresholdMeters = 40.0)
            } else false

            isOffRoute = offRoute
            if (offRoute) {
                isRerouting = true
            } else {
                isCalculatingRoute = true
            }

            val coords = mutableListOf<Pair<Double, Double>>()
            coords.add(Pair(startLat, startLng))
            filteredWarungs.forEach { w ->
                coords.add(Pair(w.latitude, w.longitude))
            }
            val route = OsmRoutingEngine.getRoadRoute(coords)
            roadRouteResult = route
            isCalculatingRoute = false
            isRerouting = false
            isOffRoute = false
        } else if (filteredWarungs.isEmpty()) {
            roadRouteResult = null
            isOffRoute = false
            isRerouting = false
        }
    }

    // Waypoints ordered list
    val waypoints = remember(filteredWarungs, effectiveGps, todayVisitedIds) {
        filteredWarungs.mapIndexed { idx, w ->
            val dist = LocationHelper.calculateDistanceMeters(effectiveGps.latitude, effectiveGps.longitude, w.latitude, w.longitude)
            val estMin = max(1, (dist / (25000.0 / 60.0)).roundToInt()) // ~25 km/h avg speed
            WaypointItem(
                warung = w,
                sequence = idx + 1,
                distanceMeters = dist,
                isVisitedToday = todayVisitedIds.contains(w.id),
                estimatedMinutes = estMin
            )
        }
    }

    // Active waypoint target
    val currentTarget = remember(waypoints, activeTargetIndex) {
        if (waypoints.isNotEmpty()) {
            waypoints.getOrNull(activeTargetIndex.coerceIn(0, waypoints.size - 1))
        } else null
    }

    // Calculate next maneuver direction & instruction from OSRM Road Steps or compass
    val activeRoadStep = remember(roadRouteResult, activeTargetIndex) {
        roadRouteResult?.steps?.getOrNull(activeTargetIndex) ?: roadRouteResult?.steps?.firstOrNull()
    }

    val currentManeuver = remember(effectiveGps, currentTarget, activeRoadStep) {
        if (currentTarget == null) {
            ManeuverType.STRAIGHT
        } else if (currentTarget.distanceMeters < 30.0) {
            ManeuverType.ARRIVED
        } else if (activeRoadStep != null) {
            when (activeRoadStep.modifier) {
                "right", "sharp right" -> ManeuverType.TURN_RIGHT
                "slight right" -> ManeuverType.SLIGHT_RIGHT
                "left", "sharp left" -> ManeuverType.TURN_LEFT
                "slight left" -> ManeuverType.SLIGHT_LEFT
                "uturn" -> ManeuverType.U_TURN
                else -> ManeuverType.STRAIGHT
            }
        } else {
            val dLat = currentTarget.warung.latitude - effectiveGps.latitude
            val dLon = currentTarget.warung.longitude - effectiveGps.longitude
            val bearing = (Math.toDegrees(atan2(dLon, dLat)) + 360) % 360
            when {
                bearing in 335.0..360.0 || bearing in 0.0..25.0 -> ManeuverType.STRAIGHT
                bearing in 25.0..70.0 -> ManeuverType.SLIGHT_RIGHT
                bearing in 70.0..115.0 -> ManeuverType.TURN_RIGHT
                bearing in 115.0..160.0 -> ManeuverType.TURN_RIGHT
                bearing in 160.0..200.0 -> ManeuverType.U_TURN
                bearing in 200.0..245.0 -> ManeuverType.TURN_LEFT
                bearing in 245.0..290.0 -> ManeuverType.TURN_LEFT
                else -> ManeuverType.SLIGHT_LEFT
            }
        }
    }

    // Simulation loop effect (Ultra-smooth 60 FPS Dense Interpolation along Road Geometry)
    LaunchedEffect(isSimulatingNav, activeTargetIndex, simSpeedMultiplier) {
        if (isSimulatingNav && waypoints.isNotEmpty()) {
            val target = waypoints.getOrNull(activeTargetIndex.coerceIn(0, waypoints.size - 1))
            if (target != null) {
                val startLat = simGpsLat
                val startLng = simGpsLng
                val endLat = target.warung.latitude
                val endLng = target.warung.longitude

                // Fetch or calculate road segment from start point to this waypoint target
                val route = OsmRoutingEngine.getRoadRoute(listOf(Pair(startLat, startLng), Pair(endLat, endLng)))
                val basePoints = route?.polylinePoints?.takeIf { it.size >= 2 }
                    ?: listOf(Pair(startLat, startLng), Pair(endLat, endLng))

                // Subdivide polyline into high-density ~1.5 meter steps for buttery-smooth glide
                val densePoints = OsmRoutingEngine.interpolateDensePath(basePoints, targetIntervalMeters = 1.5)

                if (densePoints.size >= 2) {
                    val stepStride = when (simSpeedMultiplier) {
                        1 -> 1
                        2 -> 2
                        else -> 4
                    }

                    var idx = 0
                    while (idx < densePoints.size - 1) {
                        if (!isSimulatingNav) break
                        val currentPt = densePoints[idx]
                        val nextLookaheadIdx = min(idx + max(2, stepStride * 2), densePoints.size - 1)
                        val nextPt = densePoints[nextLookaheadIdx]

                        // Compute vehicle forward heading
                        val targetBearing = OsmRoutingEngine.calculateBearing(currentPt.first, currentPt.second, nextPt.first, nextPt.second)
                        val currentB = simVehicleBearingDeg
                        val diff = ((targetBearing - currentB + 540f) % 360f) - 180f
                        simVehicleBearingDeg = (currentB + diff * 0.35f + 360f) % 360f

                        simGpsLat = currentPt.first
                        simGpsLng = currentPt.second
                        mapCenterLat = currentPt.first
                        mapCenterLng = currentPt.second

                        idx += stepStride
                        delay(25L) // Smooth continuous motion
                    }

                    if (isSimulatingNav) {
                        simGpsLat = endLat
                        simGpsLng = endLng
                        mapCenterLat = endLat
                        mapCenterLng = endLng
                    }
                }

                if (isSimulatingNav) {
                    if (activeTargetIndex < waypoints.size - 1) {
                        delay(600L) // Short dwell at target store
                        activeTargetIndex++
                    } else {
                        isSimulatingNav = false
                        Toast.makeText(context, AppStrings.tr("Simulasi rute jalan selesai! Sampai di toko tujuan.", "Road route simulation complete! Arrived at destination.", lang), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Recenter map to user GPS initially
    LaunchedEffect(userGps) {
        if (!isSimulatingNav && userGps.latitude != 0.0 && mapCenterLat == LocationHelper.DEFAULT_LAT) {
            mapCenterLat = userGps.latitude
            mapCenterLng = userGps.longitude
            simGpsLat = userGps.latitude
            simGpsLng = userGps.longitude
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentMapStyle.bgColor)
    ) {
        // --- 1. FULL INTERACTIVE MAP CANVAS (ONLINE & OFFLINE TILE RENDERING) ---
        val textMeasurer = rememberTextMeasurer()

        // Cache of loaded ImageBitmaps for current viewport & in-flight tracking
        val tileMapState = remember { mutableStateMapOf<String, ImageBitmap?>() }
        val inFlightTileRequests = remember { mutableSetOf<String>() }
        var flingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        var lastPanVelocity by remember { mutableStateOf(Offset.Zero) }
        var lastPanTimestamp by remember { mutableLongStateOf(0L) }

        // State snapshots for pointer gesture callbacks to avoid resetting gesture detectors
        val currentFilteredWarungs by rememberUpdatedState(filteredWarungs)
        val currentCenterLat by rememberUpdatedState(mapCenterLat)
        val currentCenterLng by rememberUpdatedState(mapCenterLng)
        val currentZoom by rememberUpdatedState(zoomLevel)
        val currentBearing by rememberUpdatedState(mapBearingDeg)
        val currentCrosshair by rememberUpdatedState(isCrosshairMode)

        LaunchedEffect(currentMapStyle) {
            tileMapState.clear()
            inFlightTileRequests.clear()
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("interactive_map_canvas")
                .graphicsLayer {
                    // 3D Perspective Pitch / Tilt & Compass Rotation (Google Maps style)
                    rotationX = mapTiltDeg
                    rotationZ = -mapBearingDeg
                    cameraDistance = 12f * density
                }
                .pointerInput(Unit) {
                    while (true) {
                        lastPanVelocity = Offset.Zero
                        lastPanTimestamp = 0L

                        detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
                            flingJob?.cancel()

                            // 1. Calculate responsive zoom change with high sensitivity (1.35x)
                            var effectiveZoom = zoomLevel
                            if (zoom != 1.0f) {
                                val logDelta = (kotlin.math.ln(zoom.toDouble().coerceAtLeast(0.0001)) / kotlin.math.ln(2.0)).toFloat()
                                effectiveZoom = (zoomLevel + logDelta * 1.35f).coerceIn(3.0f, 21.0f)
                            }

                            // 2. Rotate with deadzone to prevent accidental twist during pinch
                            if (abs(rotation) > 2.2f) {
                                mapBearingDeg = ((mapBearingDeg + rotation * 0.9f) % 360f + 360f) % 360f
                            }

                            if (pan.getDistanceSquared() > 2f) {
                                isFollowingUserGps = false
                            }

                            // 3. Track instantaneous pan velocity for momentum glide / fling
                            val now = System.currentTimeMillis()
                            if (lastPanTimestamp > 0L) {
                                val dt = (now - lastPanTimestamp).coerceIn(8L, 100L)
                                val instantVx = (pan.x / dt) * 1000f
                                val instantVy = (pan.y / dt) * 1000f
                                lastPanVelocity = Offset(
                                    lastPanVelocity.x * 0.35f + instantVx * 0.65f,
                                    lastPanVelocity.y * 0.35f + instantVy * 0.65f
                                )
                            }
                            lastPanTimestamp = now

                            // 4. Centroid-Aware Coordinate Transformation (Zooms seamlessly around pinch focal point)
                            val canvasW = size.width.toFloat()
                            val canvasH = size.height.toFloat()
                            val cx = canvasW / 2f
                            val cy = canvasH / 2f

                            val radBearing = Math.toRadians(mapBearingDeg.toDouble())
                            val cosB = cos(radBearing)
                            val sinB = sin(radBearing)

                            // Rotate pan offset into world coordinates
                            val rotPanX = pan.x * cosB - pan.y * sinB
                            val rotPanY = pan.x * sinB + pan.y * cosB

                            // Current and new world pixel scale (256 * 2^zoom)
                            val wCur = 256.0 * 2.0.pow(zoomLevel.toDouble())
                            val wNew = 256.0 * 2.0.pow(effectiveZoom.toDouble())

                            // Current normalized center in Web Mercator [0..1]
                            val curNormX = (mapCenterLng + 180.0) / 360.0
                            val radLat = Math.toRadians(mapCenterLat.coerceIn(-85.0511, 85.0511))
                            val curNormY = (1.0 - asinh(tan(radLat)) / Math.PI) / 2.0

                            // Pinch centroid vector from screen center rotated into world coordinate space
                            val pinchDx = (centroid.x - cx).toDouble()
                            val pinchDy = (centroid.y - cy).toDouble()
                            val rotPinchDx = pinchDx * cosB - pinchDy * sinB
                            val rotPinchDy = pinchDx * sinB + pinchDy * cosB

                            // Exact projection formula: Point under user's fingers remains 100% stationary
                            val nextNormX = curNormX + rotPinchDx * (1.0 / wCur - 1.0 / wNew) - (rotPanX / wNew)
                            val nextNormY = curNormY + rotPinchDy * (1.0 / wCur - 1.0 / wNew) - (rotPanY / wNew)

                            val newLng = nextNormX * 360.0 - 180.0
                            val sinhVal = sinh(Math.PI * (1.0 - 2.0 * nextNormY))
                            val newLat = Math.toDegrees(atan(sinhVal))

                            mapCenterLat = newLat.coerceIn(-85.0511, 85.0511)
                            mapCenterLng = ((newLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
                            zoomLevel = effectiveZoom
                        }

                        // When gesture ends (fingers lifted), if speed is high enough, launch smooth inertial glide!
                        val speed = lastPanVelocity.getDistance()
                        if (speed > 300f) {
                            flingJob = coroutineScope.launch {
                                var curVx = lastPanVelocity.x.coerceIn(-5000f, 5000f)
                                var curVy = lastPanVelocity.y.coerceIn(-5000f, 5000f)
                                val friction = 0.93f
                                val frameDt = 0.016

                                while (hypot(curVx, curVy) > 50f) {
                                    val pX = (curVx * frameDt).toFloat()
                                    val pY = (curVy * frameDt).toFloat()

                                    val radB = Math.toRadians(mapBearingDeg.toDouble())
                                    val rX = pX * cos(radB) - pY * sin(radB)
                                    val rY = pX * sin(radB) + pY * cos(radB)

                                    val (cTileX, cTileY) = OsmTileEngine.latLngToTileCoordinates(mapCenterLat, mapCenterLng, zoomLevel)
                                    val nTileX = cTileX - (rX / 256.0)
                                    val nTileY = cTileY - (rY / 256.0)
                                    val (fLat, fLng) = OsmTileEngine.tileCoordinatesToLatLng(nTileX, nTileY, zoomLevel)

                                    mapCenterLat = fLat.coerceIn(-85.0511, 85.0511)
                                    mapCenterLng = ((fLng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

                                    curVx *= friction
                                    curVy *= friction
                                    delay(16)
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            flingJob?.cancel()
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val (centerTileX, centerTileY) = OsmTileEngine.latLngToTileCoordinates(currentCenterLat, currentCenterLng, currentZoom)

                            val rad = Math.toRadians(currentBearing.toDouble())
                            val cosB = cos(rad).toFloat()
                            val sinB = sin(rad).toFloat()
                            val cx = canvasWidth / 2f
                            val cy = canvasHeight / 2f

                            val unrotX = cx + (tapOffset.x - cx) * cosB - (tapOffset.y - cy) * sinB
                            val unrotY = cy + (tapOffset.x - cx) * sinB + (tapOffset.y - cy) * cosB

                            var closest: WarungEntity? = null
                            var minDistancePx = 48.dp.toPx() // Accessible touch target tolerance

                            currentFilteredWarungs.forEach { w ->
                                val (wx, wy) = OsmTileEngine.latLngToTileCoordinates(w.latitude, w.longitude, currentZoom)
                                val px = cx + ((wx - centerTileX) * 256.0).toFloat()
                                val py = cy + ((wy - centerTileY) * 256.0).toFloat()

                                val distTip = hypot(unrotX - px, unrotY - py)
                                val distHead = hypot(unrotX - px, unrotY - (py - 22.dp.toPx()))
                                val dist = minOf(distTip, distHead)

                                if (dist < minDistancePx) {
                                    minDistancePx = dist
                                    closest = w
                                }
                            }

                            if (closest != null) {
                                selectedWarung = closest
                                droppedPinLocation = null
                            } else {
                                selectedWarung = null
                                if (droppedPinLocation != null) {
                                    droppedPinLocation = null
                                }
                            }
                        },
                        onLongPress = { tapOffset ->
                            flingJob?.cancel()
                            if (!currentCrosshair) {
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val (centerTileX, centerTileY) = OsmTileEngine.latLngToTileCoordinates(currentCenterLat, currentCenterLng, currentZoom)

                                val rad = Math.toRadians(currentBearing.toDouble())
                                val cosB = cos(rad).toFloat()
                                val sinB = sin(rad).toFloat()
                                val cx = canvasWidth / 2f
                                val cy = canvasHeight / 2f

                                val unrotX = cx + (tapOffset.x - cx) * cosB - (tapOffset.y - cy) * sinB
                                val unrotY = cy + (tapOffset.x - cx) * sinB + (tapOffset.y - cy) * cosB

                                val tappedTileX = centerTileX + (unrotX - cx) / 256.0
                                val tappedTileY = centerTileY + (unrotY - cy) / 256.0
                                val (tappedLat, tappedLng) = OsmTileEngine.tileCoordinatesToLatLng(tappedTileX, tappedTileY, currentZoom)

                                droppedPinLocation = Pair(tappedLat, tappedLng)
                                selectedWarung = null
                                Toast.makeText(context, AppStrings.tr("📍 Pin outlet baru terpasang!", "📍 New outlet pin placed!", lang), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            flingJob?.cancel()
                            if (!currentCrosshair) {
                                val canvasWidth = size.width.toFloat()
                                val canvasHeight = size.height.toFloat()
                                val (centerTileX, centerTileY) = OsmTileEngine.latLngToTileCoordinates(currentCenterLat, currentCenterLng, currentZoom)

                                val rad = Math.toRadians(currentBearing.toDouble())
                                val cosB = cos(rad).toFloat()
                                val sinB = sin(rad).toFloat()
                                val cx = canvasWidth / 2f
                                val cy = canvasHeight / 2f

                                val unrotX = cx + (tapOffset.x - cx) * cosB - (tapOffset.y - cy) * sinB
                                val unrotY = cy + (tapOffset.x - cx) * sinB + (tapOffset.y - cy) * cosB

                                val tappedTileX = centerTileX + (unrotX - cx) / 256.0
                                val tappedTileY = centerTileY + (unrotY - cy) / 256.0
                                val (tappedLat, tappedLng) = OsmTileEngine.tileCoordinatesToLatLng(tappedTileX, tappedTileY, currentZoom)

                                coroutineScope.launch {
                                    val startLat = mapCenterLat
                                    val startLng = mapCenterLng
                                    val startZoom = zoomLevel
                                    val targetZoom = (startZoom + 1.25f).coerceAtMost(20.0f)
                                    androidx.compose.animation.core.animate(
                                        initialValue = 0f,
                                        targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                    ) { progress, _ ->
                                        mapCenterLat = startLat + (tappedLat - startLat) * progress * 0.65
                                        mapCenterLng = startLng + (tappedLng - startLng) * progress * 0.65
                                        zoomLevel = startZoom + (targetZoom - startZoom) * progress
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val currentIntZoom = zoomLevel.roundToInt().coerceIn(3, 19)
            val scaleFactor = 2.0.pow((zoomLevel - currentIntZoom).toDouble()).toFloat()
            val scaledTilePx = 256f * scaleFactor

            val (centerTileX, centerTileY) = OsmTileEngine.latLngToTileCoordinates(mapCenterLat, mapCenterLng, currentIntZoom.toFloat())

            fun latLngToPixel(lat: Double, lng: Double): Offset {
                val (tx, ty) = OsmTileEngine.latLngToTileCoordinates(lat, lng, currentIntZoom.toFloat())
                val px = (canvasWidth / 2f) + ((tx - centerTileX).toFloat() * scaledTilePx)
                val py = (canvasHeight / 2f) + ((ty - centerTileY).toFloat() * scaledTilePx)
                return Offset(px, py)
            }

            // 1.1 Clean Background
            drawRect(color = currentMapStyle.bgColor, size = size)

            // 1.2 Render Real Online & Offline OpenStreetMap / Carto Tiles
            val n = 2.0.pow(currentIntZoom.toDouble())

            val halfTilesX = (canvasWidth / (scaledTilePx * 2f)).toInt() + 2
            val halfTilesY = (canvasHeight / (scaledTilePx * 2f)).toInt() + 2

            val minX = (centerTileX.toInt() - halfTilesX).coerceAtLeast(0)
            val maxX = (centerTileX.toInt() + halfTilesX).coerceAtMost(n.toInt() - 1)
            val minY = (centerTileY.toInt() - halfTilesY).coerceAtLeast(0)
            val maxY = (centerTileY.toInt() + halfTilesY).coerceAtMost(n.toInt() - 1)

            // Draw Real Map Tiles with zero-delay memory cache & parent-tile fallback
            for (tx in minX..maxX) {
                for (ty in minY..maxY) {
                    val tilePixelX = (canvasWidth / 2f) + ((tx - centerTileX).toFloat() * scaledTilePx)
                    val tilePixelY = (canvasHeight / 2f) + ((ty - centerTileY).toFloat() * scaledTilePx)

                    val key = "${currentMapStyle.name}_${currentIntZoom}_${tx}_$ty"
                    val cachedTile = tileMapState[key] ?: OsmTileEngine.getMemoryCachedTile(currentMapStyle, currentIntZoom, tx, ty)

                    if (cachedTile != null) {
                        tileMapState[key] = cachedTile
                        drawImage(
                            image = cachedTile,
                            dstOffset = IntOffset(tilePixelX.roundToInt(), tilePixelY.roundToInt()),
                            dstSize = IntSize(ceil(scaledTilePx).toInt() + 1, ceil(scaledTilePx).toInt() + 1)
                        )
                    } else {
                        // Check if parent tile (zoom-1) exists in memory for seamless placeholder
                        val parentInfo = OsmTileEngine.getParentMemoryCachedTile(currentMapStyle, currentIntZoom, tx, ty)
                        if (parentInfo != null) {
                            val (parentTile, qx, qy) = parentInfo
                            val subX = qx * 128
                            val subY = qy * 128
                            drawImage(
                                image = parentTile,
                                srcOffset = IntOffset(subX, subY),
                                srcSize = IntSize(128, 128),
                                dstOffset = IntOffset(tilePixelX.roundToInt(), tilePixelY.roundToInt()),
                                dstSize = IntSize(ceil(scaledTilePx).toInt() + 1, ceil(scaledTilePx).toInt() + 1)
                            )
                        }

                        // Tile not yet in local memory -> Request background fetch once without duplicating
                        if (key !in inFlightTileRequests) {
                            inFlightTileRequests.add(key)
                            coroutineScope.launch {
                                val loaded = OsmTileEngine.getTile(context, currentMapStyle, currentIntZoom, tx, ty)
                                if (loaded != null) {
                                    tileMapState[key] = loaded
                                }
                                inFlightTileRequests.remove(key)
                            }
                        }
                    }
                }
            }

            // 1.3 Draw Multi-Waypoint Polyline Route (Road Network Geometry Snapping)
            if (showPolylineRoute && filteredWarungs.isNotEmpty()) {
                val routePoints = mutableListOf<Offset>()
                val roadPoints = roadRouteResult?.polylinePoints

                if (!roadPoints.isNullOrEmpty() && roadPoints.size >= 2) {
                    // Use actual road-following coordinates from OSRM
                    roadPoints.forEach { (lat, lng) ->
                        routePoints.add(latLngToPixel(lat, lng))
                    }
                } else {
                    // Fallback to straight-line connection if offline or loading
                    if (effectiveGps.latitude != 0.0) {
                        routePoints.add(latLngToPixel(effectiveGps.latitude, effectiveGps.longitude))
                    }
                    filteredWarungs.forEach { w ->
                        routePoints.add(latLngToPixel(w.latitude, w.longitude))
                    }
                }

                if (routePoints.size >= 2) {
                    val pathGlow = Path().apply {
                        moveTo(routePoints[0].x, routePoints[0].y)
                        for (i in 1 until routePoints.size) {
                            lineTo(routePoints[i].x, routePoints[i].y)
                        }
                    }
                    // Outer Soft Glow
                    drawPath(
                        path = pathGlow,
                        color = Color(0xFF2563EB).copy(alpha = 0.28f),
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Main Route Line
                    drawPath(
                        path = pathGlow,
                        color = Color(0xFF2563EB),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Inner Dashed Direction Indicator
                    drawPath(
                        path = pathGlow,
                        color = Color.White,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // 1.4 Draw Outlet Pins (Numbered & Status-coded)
            filteredWarungs.forEachIndexed { index, warung ->
                val pinPos = latLngToPixel(warung.latitude, warung.longitude)
                val isSelected = selectedWarung?.id == warung.id
                val isVisited = todayVisitedIds.contains(warung.id)
                val isTarget = currentTarget?.warung?.id == warung.id
                val hasHighDebt = warung.saldoPiutang > 0 && warung.saldoPiutang >= warung.limitHutangMaksimal * 0.8

                val pinColor = when {
                    isTarget -> Color(0xFFEA580C) // Orange Flame for active target
                    isVisited -> EmeraldSuccess // Green for visited
                    hasHighDebt -> RoseDanger // Red for high debt
                    else -> Color(0xFF2563EB) // Blue standard
                }

                // Draw Pin Marker
                drawMarkerPin(
                    center = pinPos,
                    sequence = index + 1,
                    name = warung.namaWarung,
                    color = pinColor,
                    isSelected = isSelected,
                    isTarget = isTarget,
                    isVisited = isVisited,
                    textMeasurer = textMeasurer,
                    zoom = zoomLevel
                )
            }

            // 1.4.1 Draw Dropped Location Pin (if user held/double-tapped to add new outlet)
            droppedPinLocation?.let { (pinLat, pinLng) ->
                val pinPos = latLngToPixel(pinLat, pinLng)
                drawDroppedMarkerPin(center = pinPos, textMeasurer = textMeasurer)
            }

            // 1.5 Draw Salesman Current GPS Marker (Navigation Directional Arrow & Radar Glow)
            if (effectiveGps.latitude != 0.0) {
                val userPos = latLngToPixel(effectiveGps.latitude, effectiveGps.longitude)

                // Real Satellite Accuracy Radius Halo (indicates GPS precision on the ground)
                val metersPerPx = (156543.03392 * cos(Math.toRadians(effectiveGps.latitude.coerceIn(-85.0, 85.0))) / 2.0.pow(zoomLevel.toDouble())).toFloat()
                val accPx = (effectiveGps.accuracyMeter / metersPerPx).coerceIn(12.dp.toPx(), 220.dp.toPx())

                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                    radius = accPx,
                    center = userPos
                )
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = 0.35f),
                    radius = accPx,
                    center = userPos,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Determine effective bearing:
                // If moving fast: use GPS bearing. If standing still or walking: smoothly use physical Compass sensor!
                val isMovingFast = effectiveGps.speedMps > 1.2f && effectiveGps.hasBearing
                val effectiveHeadingDeg = when {
                    isSimulatingNav -> simVehicleBearingDeg
                    isMovingFast -> effectiveGps.bearing
                    compassHeadingDeg != 0f -> compassHeadingDeg
                    effectiveGps.hasBearing -> effectiveGps.bearing
                    else -> 0f
                }

                val hasCompassBeam = isSimulatingNav || isMovingFast || compassHeadingDeg != 0f || effectiveGps.hasBearing

                if (hasCompassBeam) {
                    // Google Maps Navigation Vehicle Puck with Dynamic Forward Direction & Compass Beam
                    withTransform({
                        rotate(degrees = effectiveHeadingDeg, pivot = userPos)
                    }) {
                        // Forward Light Beam / Vision Cone (Like Google Maps Flashlight Beam)
                        val conePath = Path().apply {
                            moveTo(userPos.x, userPos.y)
                            lineTo(userPos.x - 22.dp.toPx(), userPos.y - 48.dp.toPx())
                            lineTo(userPos.x + 22.dp.toPx(), userPos.y - 48.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = conePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.42f), Color.Transparent),
                                startY = userPos.y - 48.dp.toPx(),
                                endY = userPos.y
                            )
                        )
                        // Outer Radar Pulse
                        drawCircle(
                            color = Color(0xFF2563EB).copy(alpha = 0.20f),
                            radius = 18.dp.toPx(),
                            center = userPos
                        )

                        if (isMovingFast || isSimulatingNav) {
                            // Directional Navigation Chevron/Arrow when moving fast
                            val arrowPath = Path().apply {
                                moveTo(userPos.x, userPos.y - 14.dp.toPx()) // Tip
                                lineTo(userPos.x - 10.dp.toPx(), userPos.y + 11.dp.toPx()) // Bottom Left
                                lineTo(userPos.x, userPos.y + 5.dp.toPx()) // Center Indent
                                lineTo(userPos.x + 10.dp.toPx(), userPos.y + 11.dp.toPx()) // Bottom Right
                                close()
                            }
                            drawPath(
                                path = arrowPath,
                                color = Color.White,
                                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = arrowPath,
                                color = Color(0xFF2563EB)
                            )
                        } else {
                            // High-precision Circular Core with Heading Pointer Pip (Google Maps style)
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = userPos
                            )
                            drawCircle(
                                color = Color(0xFF2563EB),
                                radius = 7.dp.toPx(),
                                center = userPos
                            )
                            // Forward heading indicator pip on top of the circle
                            drawCircle(
                                color = Color(0xFF93C5FD),
                                radius = 2.5.dp.toPx(),
                                center = Offset(userPos.x, userPos.y - 8.dp.toPx())
                            )
                        }

                        // Center white core dot
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = userPos
                        )
                    }
                } else {
                    // Standard Stationary GPS Radar Marker
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = 0.25f),
                        radius = 18.dp.toPx(),
                        center = userPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = userPos
                    )
                    drawCircle(
                        color = Color(0xFF2563EB),
                        radius = 7.dp.toPx(),
                        center = userPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = userPos
                    )
                }
            }
        }

        // --- 2. TOP HUD: TURN-BY-TURN NAVIGATION BANNER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Top App Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Drawer Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Slate800)
                    }
                }

                // Rute Filter Chip Selector & Cache Status (Clean, Flexible Center)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { showOfflineDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = allRutes.find { it.id == selectedRuteId }?.namaRute ?: AppStrings.tr("Semua Rute", "All Routes", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Rute",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Search Outlet Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (showSearchOverlay) Color(0xFF2563EB) else Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { showSearchOverlay = !showSearchOverlay }) {
                        Icon(
                            if (showSearchOverlay) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cari Outlet",
                            tint = if (showSearchOverlay) Color.White else Slate800
                        )
                    }
                }

                // Waypoints List Sheet Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { showWaypointListSheet = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Daftar Waypoint", tint = Slate800)
                            if (waypoints.isNotEmpty()) {
                                Badge(
                                    containerColor = Color(0xFF2563EB),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-8).dp)
                                ) {
                                    Text("${waypoints.size}", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar Popdown
            AnimatedVisibility(visible = showSearchOverlay) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(AppStrings.tr("Cari nama warung, toko atau alamat...", "Search outlet name or address...", lang), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Turn-by-Turn Navigation HUD Card
            AnimatedVisibility(visible = showNavBanner && currentTarget != null) {
                if (currentTarget != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate950,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Big Maneuver Icon
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentManeuver.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Next Stop Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = LocationHelper.formatDistance(currentTarget.distanceMeters),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isRerouting || isOffRoute) Color(0xFFF59E0B).copy(alpha = 0.25f) else EmeraldSuccess.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (isRerouting || isOffRoute) "🔄 Hitung Ulang..." else "± ${currentTarget.estimatedMinutes} mnt",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isRerouting || isOffRoute) Color(0xFFFBBF24) else EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${currentTarget.sequence}. ${currentTarget.warung.namaWarung}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate100,
                                        maxLines = 1
                                    )
                                    val roadInstruction = activeRoadStep?.instruction?.ifBlank { null }
                                        ?: currentTarget.warung.alamatLengkap.ifBlank { null }
                                        ?: AppStrings.tr(currentManeuver.descId, currentManeuver.descEn, lang)
                                    Text(
                                        text = roadInstruction,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (activeRoadStep != null) Color(0xFF93C5FD) else Slate400,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                // Quick Action: Open Transaction & Dismiss HUD
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val warung = currentTarget.warung
                                            val state = if (warung.stokTitipanPcs > 0) {
                                                TransactionDialogState.TarikSisa(warung)
                                            } else {
                                                TransactionDialogState.TitipBaru(warung)
                                            }
                                            viewModel.openTransactionDialog(state)
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldSuccess)
                                    ) {
                                        Icon(Icons.Default.Storefront, contentDescription = "Kunjungan", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { showNavBanner = false },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Tutup Banner", tint = Slate400, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Simulation & Target Stepper
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Slate800, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Target Index Navigator
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { if (activeTargetIndex > 0) activeTargetIndex-- },
                                        enabled = activeTargetIndex > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = if (activeTargetIndex > 0) Color.White else Slate600, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = "${activeTargetIndex + 1} / ${waypoints.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate300
                                    )
                                    IconButton(
                                        onClick = { if (activeTargetIndex < waypoints.size - 1) activeTargetIndex++ },
                                        enabled = activeTargetIndex < waypoints.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = if (activeTargetIndex < waypoints.size - 1) Color.White else Slate600, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Demo Mode Simulation Toggle
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isSimulatingNav) {
                                        FilledTonalButton(
                                            onClick = { simSpeedMultiplier = if (simSpeedMultiplier == 1) 2 else if (simSpeedMultiplier == 2) 4 else 1 },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("${simSpeedMultiplier}x", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (isSimulatingNav) {
                                                isSimulatingNav = false
                                            } else {
                                                if (activeTargetIndex >= waypoints.size) {
                                                    activeTargetIndex = 0
                                                }
                                                val startLat = userGps.latitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LAT
                                                val startLng = userGps.longitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LNG
                                                simGpsLat = startLat
                                                simGpsLng = startLng
                                                mapCenterLat = startLat
                                                mapCenterLng = startLng
                                                isSimulatingNav = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSimulatingNav) RoseDanger else Color(0xFF4F46E5)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            if (isSimulatingNav) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSimulatingNav) AppStrings.tr("Hentikan Demo", "Stop Demo", lang) else AppStrings.tr("Simulasi GPS", "Simulate GPS", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2.5 CROSSHAIR OVERLAY (Precision Center Reticle for New Outlet Pinning) ---
        if (isCrosshairMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Precision Crosshair Canvas at exact center (0,0)
                Canvas(modifier = Modifier.size(60.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Center ground target reticle
                    drawCircle(
                        color = RoseDanger.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = RoseDanger,
                        radius = 16.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // 4 Crosshair Hairlines
                    drawLine(color = RoseDanger, start = Offset(cx - 24.dp.toPx(), cy), end = Offset(cx - 6.dp.toPx(), cy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = RoseDanger, start = Offset(cx + 6.dp.toPx(), cy), end = Offset(cx + 24.dp.toPx(), cy), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = RoseDanger, start = Offset(cx, cy - 24.dp.toPx()), end = Offset(cx, cy - 6.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = RoseDanger, start = Offset(cx, cy + 6.dp.toPx()), end = Offset(cx, cy + 24.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

                    // Ground target center dot
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = RoseDanger, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
                }

                // Map Pin Floating directly above center target, tip touching (cx, cy)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-32).dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950.copy(alpha = 0.92f),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = AppStrings.tr("🎯 Titik Pusat Outlet", "🎯 Outlet Center Point", lang),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.US, "%.6f, %.6f", mapCenterLat, mapCenterLng),
                                color = EmeraldSuccess,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = RoseDanger,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // --- 3. FLOATING MAP ACTION BUTTONS (Right Side) ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add Outlet / Marker FAB
            Surface(
                shape = CircleShape,
                color = EmeraldSuccess,
                shadowElevation = 8.dp,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(onClick = { showAddMarkerSheet = true }) {
                    Icon(
                        Icons.Default.AddLocationAlt,
                        contentDescription = "Tambah Outlet ke Peta",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Re-Center My GPS with Active High-Accuracy Lock & Follow Toggle
            Surface(
                shape = CircleShape,
                color = if (isFollowingUserGps) Color(0xFF2563EB) else Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = {
                    isFollowingUserGps = true
                    if (effectiveGps.isAvailable && effectiveGps.latitude != 0.0) {
                        coroutineScope.launch {
                            val startLat = mapCenterLat
                            val startLng = mapCenterLng
                            val startZoom = zoomLevel
                            val targetLat = effectiveGps.latitude
                            val targetLng = effectiveGps.longitude
                            val targetZoom = 16.5f
                            androidx.compose.animation.core.animate(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                            ) { p, _ ->
                                mapCenterLat = startLat + (targetLat - startLat) * p
                                mapCenterLng = startLng + (targetLng - startLng) * p
                                zoomLevel = startZoom + (targetZoom - startZoom) * p
                            }
                        }
                    }

                    if (!LocationHelper.isLocationServiceEnabled(context)) {
                        Toast.makeText(context, AppStrings.tr("Aktifkan GPS / Lokasi di Pengaturan HP Anda", "Please enable GPS / Location in phone settings", lang), Toast.LENGTH_LONG).show()
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                        return@IconButton
                    }

                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )

                    isAcquiringGps = true
                    Toast.makeText(context, AppStrings.tr("Mengunci posisi GPS presisi...", "Locking high accuracy GPS...", lang), Toast.LENGTH_SHORT).show()

                    coroutineScope.launch {
                        try {
                            val loc = viewModel.acquireAccurateGps()
                            if (loc.isAvailable && loc.latitude != 0.0) {
                                isFollowingUserGps = true
                                mapCenterLat = loc.latitude
                                mapCenterLng = loc.longitude
                                zoomLevel = 16.5f
                                Toast.makeText(context, AppStrings.tr("📍 Lokasi GPS terkunci (±${loc.accuracyMeter.toInt()}m)", "📍 GPS locked (±${loc.accuracyMeter.toInt()}m)", lang), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, AppStrings.tr("Sedang mencari sinyal satelit GPS...", "Still searching GPS satellite signal...", lang), Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            isAcquiringGps = false
                        }
                    }
                }) {
                    if (isAcquiringGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = if (isFollowingUserGps) Color.White else Color(0xFF2563EB)
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Lokasi Saya",
                            tint = if (isFollowingUserGps) Color.White else Color(0xFF2563EB)
                        )
                    }
                }
            }

            // Fit All Waypoints Overview
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = {
                    if (filteredWarungs.isNotEmpty()) {
                        val minLat = min(effectiveGps.latitude.takeIf { it != 0.0 } ?: filteredWarungs.minOf { it.latitude }, filteredWarungs.minOf { it.latitude })
                        val maxLat = max(effectiveGps.latitude.takeIf { it != 0.0 } ?: filteredWarungs.maxOf { it.latitude }, filteredWarungs.maxOf { it.latitude })
                        val minLng = min(effectiveGps.longitude.takeIf { it != 0.0 } ?: filteredWarungs.minOf { it.longitude }, filteredWarungs.minOf { it.longitude })
                        val maxLng = max(effectiveGps.longitude.takeIf { it != 0.0 } ?: filteredWarungs.maxOf { it.longitude }, filteredWarungs.maxOf { it.longitude })
                        mapCenterLat = (minLat + maxLat) / 2.0
                        mapCenterLng = (minLng + maxLng) / 2.0
                        zoomLevel = 13.0f
                    }
                }) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Overview Rute", tint = Slate800)
                }
            }

            // Turn-by-Turn Navigation HUD Toggle
            Surface(
                shape = CircleShape,
                color = if (showNavBanner) Color(0xFF2563EB) else Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = { showNavBanner = !showNavBanner }) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Toggle Nav HUD",
                        tint = if (showNavBanner) Color.White else Slate800
                    )
                }
            }

            // 3D Perspective Mode Toggle (Google Maps style 2D / 3D Isometric Pitch)
            Surface(
                shape = CircleShape,
                color = if (is3DMode) Color(0xFF4F46E5) else Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = {
                    if (mapTiltDeg > 10f) {
                        mapTiltDeg = 0f
                        Toast.makeText(context, AppStrings.tr("Mode Peta: 2D Datar", "Map Mode: 2D Flat", lang), Toast.LENGTH_SHORT).show()
                    } else {
                        mapTiltDeg = 48f
                        Toast.makeText(context, AppStrings.tr("Mode Peta: 3D Perspektif", "Map Mode: 3D Perspective", lang), Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(
                        text = if (is3DMode) "3D" else "2D",
                        color = if (is3DMode) Color.White else Slate800,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            // Compass Reset to North Button (Active when rotated)
            if (abs(mapBearingDeg) > 2f) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    IconButton(onClick = {
                        mapBearingDeg = 0f
                        Toast.makeText(context, AppStrings.tr("Peta diarahkan ke Utara", "Compass reset to North", lang), Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = "Reset Kompas Utara",
                            tint = RoseDanger,
                            modifier = Modifier.graphicsLayer { rotationZ = mapBearingDeg }
                        )
                    }
                }
            }

            // Zoom Controls (+) and (-)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.width(46.dp)
            ) {
                Column(
                    modifier = Modifier.width(46.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (zoomLevel + 1.0f).coerceAtMost(21.0f)
                                androidx.compose.animation.core.animate(
                                    initialValue = zoomLevel,
                                    targetValue = target,
                                    animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) { v, _ -> zoomLevel = v }
                            }
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Slate800)
                    }
                    HorizontalDivider(
                        color = Slate200,
                        thickness = 1.dp,
                        modifier = Modifier.width(30.dp)
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (zoomLevel - 1.0f).coerceAtLeast(3.0f)
                                androidx.compose.animation.core.animate(
                                    initialValue = zoomLevel,
                                    targetValue = target,
                                    animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                                ) { v, _ -> zoomLevel = v }
                            }
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Slate800)
                    }
                }
            }

            // Map Style Switcher (Standard OSM, Carto Light, Dark Night, Topo)
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                IconButton(onClick = {
                    currentMapStyle = when (currentMapStyle) {
                        MapLayerStyle.OSM_STANDARD -> MapLayerStyle.CARTO_LIGHT
                        MapLayerStyle.CARTO_LIGHT -> MapLayerStyle.DARK_NIGHT
                        MapLayerStyle.DARK_NIGHT -> MapLayerStyle.OUTDOOR_TOPO
                        MapLayerStyle.OUTDOOR_TOPO -> MapLayerStyle.OSM_STANDARD
                    }
                }) {
                    Icon(Icons.Default.Layers, contentDescription = "Ganti Tampilan Peta", tint = Slate800)
                }
            }
        }

        // --- 3.5 BOTTOM-LEFT FLOATING GPS STATUS BADGE ---
        if (!isCrosshairMode && droppedPinLocation == null && selectedWarung == null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (userGps.isAvailable) EmeraldSuccess.copy(alpha = 0.6f) else AmberWarning.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 16.dp)
                    .clickable {
                        if (!LocationHelper.isLocationServiceEnabled(context)) {
                            Toast.makeText(context, AppStrings.tr("Aktifkan GPS di Pengaturan HP", "Enable GPS in phone settings", lang), Toast.LENGTH_LONG).show()
                            try {
                                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            return@clickable
                        }
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        isAcquiringGps = true
                        Toast.makeText(context, AppStrings.tr("Mengunci satelit GPS...", "Locking GPS satellites...", lang), Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            try {
                                val loc = viewModel.acquireAccurateGps()
                                if (loc.isAvailable && loc.latitude != 0.0) {
                                    mapCenterLat = loc.latitude
                                    mapCenterLng = loc.longitude
                                    zoomLevel = 16f
                                    Toast.makeText(context, AppStrings.tr("📍 GPS Terkunci: ±${loc.accuracyMeter.toInt()}m (${loc.provider})", "📍 GPS Locked: ±${loc.accuracyMeter.toInt()}m (${loc.provider})", lang), Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isAcquiringGps = false
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (userGps.isAvailable) EmeraldSuccess else AmberWarning)
                    )
                    Text(
                        text = if (userGps.isAvailable) {
                            val provTag = if (userGps.isFused) "Fused" else "GPS"
                            val compassTag = if (compassHeadingDeg != 0f) " • ${compassHeadingDeg.toInt()}°" else ""
                            "$provTag: ±${userGps.accuracyMeter.toInt().coerceAtLeast(1)}m$compassTag"
                        } else {
                            AppStrings.tr("Mencari Sinyal GPS", "Searching GPS Signal", lang)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userGps.isAvailable) Slate800 else AmberWarning
                    )
                    if (isAcquiringGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }

        // --- 4. BOTTOM FLOATING BAR: CROSSHAIR MODE CONTROLS ---
        if (isCrosshairMode) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RoseDanger.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = AppStrings.tr("Mode Bidik Titik Outlet", "Crosshair Outlet Mode", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.5f, %.5f", mapCenterLat, mapCenterLng)}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                        IconButton(onClick = { isCrosshairMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Batal", tint = Slate500)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isCrosshairMode = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(AppStrings.tr("Batal", "Cancel", lang), fontSize = 12.sp)
                        }

                        // Snap Crosshair to Current GPS Location Button
                        Button(
                            onClick = {
                                if (effectiveGps.isAvailable && effectiveGps.latitude != 0.0) {
                                    mapCenterLat = effectiveGps.latitude
                                    mapCenterLng = effectiveGps.longitude
                                    zoomLevel = 17.0f
                                    Toast.makeText(context, AppStrings.tr("🎯 Bidikan diarahkan ke posisi GPS Anda", "🎯 Reticle snapped to your GPS location", lang), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, AppStrings.tr("Sinyal GPS belum tersedia", "GPS signal not available", lang), Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Ke GPS", "To GPS", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val targetLat = mapCenterLat
                                val targetLng = mapCenterLng
                                val newWarung = WarungEntity(
                                    namaWarung = "",
                                    namaPemilik = "",
                                    noHp = "",
                                    kategoriWarung = "Kelontong",
                                    alamatLengkap = "Koordinat: ${String.format(Locale.US, "%.6f, %.6f", targetLat, targetLng)}",
                                    latitude = targetLat,
                                    longitude = targetLng,
                                    ruteId = selectedRuteId ?: allRutes.firstOrNull()?.id ?: "RUTE-01",
                                    urutanKunjungan = allWarungs.size + 1,
                                    saldoPiutang = 0.0,
                                    stokTitipanPcs = 0,
                                    limitHutangMaksimal = 500000.0,
                                    notes = ""
                                )
                                isCrosshairMode = false
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(newWarung))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(1.6f)
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Pasang", "Place", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 4.1 BOTTOM FLOATING BAR: DROPPED PIN LOCATION CONTROLS ---
        if (droppedPinLocation != null && !isCrosshairMode && selectedWarung == null) {
            val (pinLat, pinLng) = droppedPinLocation!!
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RoseDanger.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = AppStrings.tr("Titik Peta Dipilih", "Map Point Selected", lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.5f, %.5f", pinLat, pinLng)}",
                                    fontSize = 12.sp,
                                    color = Slate600
                                )
                            }
                        }
                        IconButton(onClick = { droppedPinLocation = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { droppedPinLocation = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(AppStrings.tr("Hapus Pin", "Clear", lang))
                        }
                        Button(
                            onClick = {
                                val newWarung = WarungEntity(
                                    namaWarung = "",
                                    namaPemilik = "",
                                    noHp = "",
                                    kategoriWarung = "Kelontong",
                                    alamatLengkap = "Koordinat: ${String.format(Locale.US, "%.5f, %.5f", pinLat, pinLng)}",
                                    latitude = pinLat,
                                    longitude = pinLng,
                                    ruteId = selectedRuteId ?: allRutes.firstOrNull()?.id ?: "RUTE-01",
                                    urutanKunjungan = allWarungs.size + 1,
                                    saldoPiutang = 0.0,
                                    stokTitipanPcs = 0,
                                    limitHutangMaksimal = 500000.0,
                                    notes = ""
                                )
                                droppedPinLocation = null
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(newWarung))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.tr("Daftarkan Outlet Baru", "Register New Outlet", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 4.2 BOTTOM FLOATING CARD: SELECTED OUTLET DETAIL & TRANSACTIONS ---
        selectedWarung?.let { warung ->
            val dist = LocationHelper.calculateDistanceMeters(effectiveGps.latitude, effectiveGps.longitude, warung.latitude, warung.longitude)
            val isVisited = todayVisitedIds.contains(warung.id)

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Drag Handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Slate300)
                            .align(Alignment.CenterHorizontally)
                    )

                    // Header: Outlet Name & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isVisited) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF2563EB).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Urutan #${warung.urutanKunjungan}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVisited) EmeraldSuccess else Color(0xFF2563EB),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (isVisited) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EmeraldSuccess
                                    ) {
                                        Text(
                                            text = AppStrings.tr("SUDAH DIKUNJUNGI", "VISITED TODAY", lang),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = warung.namaWarung,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = warung.alamatLengkap.ifBlank { "Lat: ${warung.latitude}, Lng: ${warung.longitude}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { selectedWarung = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                        }
                    }

                    // Stats: Jarak, Piutang, Titipan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(AppStrings.tr("Jarak GPS", "GPS Distance", lang), fontSize = 10.sp, color = Slate500)
                                Text(LocationHelper.formatDistance(dist), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (warung.saldoPiutang > 0) RoseDanger.copy(alpha = 0.05f) else Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (warung.saldoPiutang > 0) RoseDanger.copy(alpha = 0.3f) else Slate200),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(AppStrings.tr("Piutang Bon", "Debt Balance", lang), fontSize = 10.sp, color = if (warung.saldoPiutang > 0) RoseDanger else Slate500)
                                Text("Rp ${String.format(Locale.GERMAN, "%,d", warung.saldoPiutang.toLong())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (warung.saldoPiutang > 0) RoseDanger else Slate900)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(AppStrings.tr("Stok Titip", "Active Drop", lang), fontSize = 10.sp, color = Slate500)
                                Text("${warung.stokTitipanPcs} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                        }
                    }

                    // Direct Transactions Button Row (Titip Baru, Tarik Sisa / Pelunasan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.TitipBaru(warung))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Titip Baru", "Drop Stock", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.TarikSisa(warung))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (warung.stokTitipanPcs > 0 || warung.saldoPiutang > 0) EmeraldSuccess else Slate700),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Tarik / Bayar", "Collect / Settle", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Secondary Fast Actions: Harga Khusus, Edit Toko, Statistik, Navigasi Target, GMaps, Telepon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val idx = waypoints.indexOfFirst { it.warung.id == warung.id }
                                if (idx >= 0) {
                                    activeTargetIndex = idx
                                    mapCenterLat = warung.latitude
                                    mapCenterLng = warung.longitude
                                    Toast.makeText(context, AppStrings.tr("Target navigasi: ${warung.namaWarung}", "Target: ${warung.namaWarung}", lang), Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Navigasi", "Target", lang), fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.ManageCustomPrices(warung))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Harga Khusus", "Custom Price", lang), fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(warung))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Edit Toko", "Edit", lang), fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.OutletStatistics(warung))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.tr("Statistik", "Stats", lang), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val uri = Uri.parse("google.navigation:q=${warung.latitude},${warung.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${warung.latitude},${warung.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = "GMaps", tint = Slate700, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GMaps", fontSize = 11.sp, color = Slate700)
                        }

                        if (warung.noHp.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${warung.noHp}"))
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Telepon", tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(AppStrings.tr("Telepon", "Call", lang), fontSize = 11.sp, color = EmeraldSuccess)
                            }
                        }
                    }
                }
            }
        }

        // --- 4.3 MODAL BOTTOM SHEET: TAMBAH OUTLET / MARKER BARU ---
        if (showAddMarkerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddMarkerSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                            Text(
                                text = AppStrings.tr("Tambah Outlet / Marker Baru", "Add New Outlet / Marker", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                    }
                    Text(
                        text = AppStrings.tr("Pilih metode penentuan lokasi toko outlet di peta:", "Choose how to determine outlet store location:", lang),
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    // Option 1: Live Current GPS
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMarkerSheet = false
                                viewModel.refreshGpsLocation()
                                val targetLat = if (effectiveGps.latitude != 0.0) effectiveGps.latitude else LocationHelper.DEFAULT_LAT
                                val targetLng = if (effectiveGps.longitude != 0.0) effectiveGps.longitude else LocationHelper.DEFAULT_LNG
                                val newWarung = WarungEntity(
                                    namaWarung = "",
                                    namaPemilik = "",
                                    noHp = "",
                                    kategoriWarung = "Kelontong",
                                    alamatLengkap = "Koordinat GPS Saya",
                                    latitude = targetLat,
                                    longitude = targetLng,
                                    ruteId = selectedRuteId ?: allRutes.firstOrNull()?.id ?: "RUTE-01",
                                    urutanKunjungan = allWarungs.size + 1,
                                    saldoPiutang = 0.0,
                                    stokTitipanPcs = 0,
                                    limitHutangMaksimal = 500000.0,
                                    notes = ""
                                )
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(newWarung))
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2563EB).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF2563EB))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(AppStrings.tr("Gunakan Lokasi GPS Saya Saat Ini", "Use My Current GPS Location", lang), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                                Text(
                                    text = if (effectiveGps.latitude != 0.0) "${String.format(Locale.US, "%.5f, %.5f", effectiveGps.latitude, effectiveGps.longitude)} (±${effectiveGps.accuracyMeter.toInt()}m)" else "Ambil koordinat live dari satelit GPS",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                        }
                    }

                    // Option 2: Crosshair Center Pin Mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMarkerSheet = false
                                isCrosshairMode = true
                                selectedWarung = null
                                droppedPinLocation = null
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(EmeraldSuccess.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, tint = EmeraldSuccess)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(AppStrings.tr("Pilih Titik di Peta (Mode Bidik)", "Pick Point on Map (Crosshair)", lang), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                                Text(AppStrings.tr("Geser peta bebas dan kunci posisi toko yang diinginkan", "Pan and zoom map freely to point at exact store", lang), fontSize = 11.sp, color = Slate500)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                        }
                    }

                    // Option 3: Current Map Center
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddMarkerSheet = false
                                val targetLat = mapCenterLat
                                val targetLng = mapCenterLng
                                val newWarung = WarungEntity(
                                    namaWarung = "",
                                    namaPemilik = "",
                                    noHp = "",
                                    kategoriWarung = "Kelontong",
                                    alamatLengkap = "Koordinat: ${String.format(Locale.US, "%.5f, %.5f", targetLat, targetLng)}",
                                    latitude = targetLat,
                                    longitude = targetLng,
                                    ruteId = selectedRuteId ?: allRutes.firstOrNull()?.id ?: "RUTE-01",
                                    urutanKunjungan = allWarungs.size + 1,
                                    saldoPiutang = 0.0,
                                    stokTitipanPcs = 0,
                                    limitHutangMaksimal = 500000.0,
                                    notes = ""
                                )
                                viewModel.openTransactionDialog(TransactionDialogState.AddEditWarung(newWarung))
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEA580C).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFFEA580C))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(AppStrings.tr("Gunakan Titik Tengah Peta", "Use Current Map Center", lang), fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                                Text("${String.format(Locale.US, "%.5f, %.5f", mapCenterLat, mapCenterLng)}", fontSize = 11.sp, color = Slate500)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                        }
                    }

                    // Fast Gesture Hint
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate100,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Slate600, modifier = Modifier.size(18.dp))
                            Text(
                                text = AppStrings.tr("Tips: Sentuh & Tahan (Hold) atau Ketuk 2x (Double-Tap) di titik peta manapun untuk langsung menjatuhkan pin!", "Tip: Hold or double-tap anywhere on map to drop a pin directly!", lang),
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // --- 5. OFFLINE MAP DOWNLOAD & CACHE MANAGEMENT DIALOG ---
        if (showOfflineDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDownloadingOffline) showOfflineDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF2563EB))
                        Text(AppStrings.tr("Peta Offline OpenStreetMap", "Offline OpenStreetMap Cache", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = AppStrings.tr(
                                "Unduh potongan ubin peta (tiles) di sekitar rute outlet Anda untuk disimpan ke memori HP. Peta akan tetap tampil jelas tanpa sinyal internet sama sekali saat di lapangan.",
                                "Download map tiles around your outlet route directly to device storage for 100% offline navigation in the field.",
                                lang
                            ),
                            fontSize = 12.sp,
                            color = Slate600
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(AppStrings.tr("Kapasitas Cache Offline:", "Cached Tile Storage:", lang), fontSize = 11.sp, color = Slate700)
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", cacheSizeMb)} MB",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }

                        if (isDownloadingOffline) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = downloadStatusText,
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (filteredWarungs.isNotEmpty()) {
                                isDownloadingOffline = true
                                coroutineScope.launch {
                                    val minLat = filteredWarungs.minOf { it.latitude } - 0.05
                                    val maxLat = filteredWarungs.maxOf { it.latitude } + 0.05
                                    val minLng = filteredWarungs.minOf { it.longitude } - 0.05
                                    val maxLng = filteredWarungs.maxOf { it.longitude } + 0.05

                                    val count = OsmTileEngine.preCacheArea(
                                        context = context,
                                        style = currentMapStyle,
                                        minLat = minLat,
                                        maxLat = maxLat,
                                        minLng = minLng,
                                        maxLng = maxLng,
                                        zoomLevels = listOf(13, 14, 15)
                                    ) { cur, tot ->
                                        downloadProgress = cur.toFloat() / max(1, tot).toFloat()
                                        downloadStatusText = "Mengunduh tile: $cur / $tot (${(downloadProgress * 100).roundToInt()}%)"
                                    }

                                    isDownloadingOffline = false
                                    showOfflineDialog = false
                                    cacheSizeMb = OsmTileEngine.getOfflineCacheSizeBytes(context) / (1024.0 * 1024.0)
                                    Toast.makeText(context, AppStrings.tr("Berhasil menyimpan $count tile peta offline!", "Successfully cached $count offline tiles!", lang), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, AppStrings.tr("Tidak ada outlet untuk diunduh", "No outlets to download", lang), Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isDownloadingOffline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(if (isDownloadingOffline) AppStrings.tr("Mengunduh...", "Downloading...", lang) else AppStrings.tr("Unduh Peta Rute Ini", "Download Route Map", lang))
                    }
                },
                dismissButton = {
                    if (!isDownloadingOffline) {
                        TextButton(
                            onClick = {
                                OsmTileEngine.clearCache(context)
                                cacheSizeMb = 0.0
                                tileMapState.clear()
                                tileRefreshKey++
                                Toast.makeText(context, AppStrings.tr("Cache peta dibersihkan", "Map cache cleared", lang), Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(AppStrings.tr("Bersihkan Cache", "Clear Cache", lang), color = RoseDanger)
                        }
                    }
                }
            )
        }

        // --- 6. WAYPOINT ITINERARY DRAWER SHEET ---
        if (showWaypointListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWaypointListSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Title & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = AppStrings.tr("Daftar Urutan Kunjungan (Waypoints)", "Waypoint Visit Itinerary", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${waypoints.size} Outlet • ${waypoints.count { it.isVisitedToday }} Selesai Dikunjungi",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        IconButton(onClick = { showWaypointListSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Route Actions: TSP Optimizer & Google Maps Multi-Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (filteredWarungs.size <= 2) {
                                    Toast.makeText(context, AppStrings.tr("Minimal butuh 3 outlet untuk dioptimasi", "Need at least 3 outlets to optimize", lang), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Nearest Neighbor Optimization
                                val unvisited = filteredWarungs.toMutableList()
                                val sorted = mutableListOf<WarungEntity>()
                                var curLat = effectiveGps.latitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LAT
                                var curLng = effectiveGps.longitude.takeIf { it != 0.0 } ?: LocationHelper.DEFAULT_LNG

                                while (unvisited.isNotEmpty()) {
                                    val nearest = unvisited.minByOrNull {
                                        LocationHelper.calculateDistanceMeters(curLat, curLng, it.latitude, it.longitude)
                                    } ?: unvisited.first()
                                    sorted.add(nearest)
                                    unvisited.remove(nearest)
                                    curLat = nearest.latitude
                                    curLng = nearest.longitude
                                }

                                val updatedWarungs = sorted.mapIndexed { idx, w -> w.copy(urutanKunjungan = idx + 1) }
                                viewModel.updateWarungsBatch(updatedWarungs)
                                Toast.makeText(context, AppStrings.tr("Jalur rute berhasil dioptimalkan dari lokasi Anda!", "Route optimized from your location!", lang), Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.tr("Optimalkan Rute (TSP)", "Optimize TSP", lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                LocationHelper.openMultiStopGoogleMapsRoute(context, filteredWarungs, "Rute SFA")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.tr("Google Maps", "Google Maps", lang),
                                fontSize = 11.sp,
                                color = Slate700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Waypoint List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(waypoints) { idx, item ->
                            val isTarget = idx == activeTargetIndex

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTarget) Color(0xFFEFF6FF) else Slate50,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isTarget) 2.dp else 1.dp,
                                    color = if (isTarget) Color(0xFF2563EB) else Slate200
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTargetIndex = idx
                                        selectedWarung = item.warung
                                        mapCenterLat = item.warung.latitude
                                        mapCenterLng = item.warung.longitude
                                        showWaypointListSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Number Badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (item.isVisitedToday) EmeraldSuccess
                                                else if (isTarget) Color(0xFF2563EB)
                                                else Slate300
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.isVisitedToday) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("${item.sequence}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isTarget) Color.White else Slate800)
                                        }
                                    }

                                    // Outlet Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.warung.namaWarung,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = item.warung.alamatLengkap.ifBlank { "Lokasi Koordinat GPS" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }

                                    // Distance & ETA
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = LocationHelper.formatDistance(item.distanceMeters),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                        Text(
                                            text = "±${item.estimatedMinutes} mnt",
                                            fontSize = 10.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draw custom teardrop marker pin on map with sequence number, title bubble, and state.
 * The needle tip (bottom point) lands precisely at [center].
 */
private fun DrawScope.drawMarkerPin(
    center: Offset,
    sequence: Int,
    name: String,
    color: Color,
    isSelected: Boolean,
    isTarget: Boolean,
    isVisited: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    zoom: Float
) {
    val headRadius = if (isSelected || isTarget) 16.dp.toPx() else 13.dp.toPx()
    val stemHeight = headRadius * 1.5f
    val headCenterY = center.y - stemHeight - headRadius

    // 1. Ground contact shadow oval directly under needle tip
    drawOval(
        color = Color.Black.copy(alpha = 0.32f),
        topLeft = Offset(center.x - 7.dp.toPx(), center.y - 2.5.dp.toPx()),
        size = Size(14.dp.toPx(), 5.dp.toPx())
    )

    // Target Pulsing Ring around needle tip
    if (isTarget) {
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = 20.dp.toPx(),
            center = center
        )
    }

    // 2. Teardrop Pin Path pointing down to needle tip at (center.x, center.y)
    val pinPath = Path().apply {
        moveTo(center.x, center.y)
        // Left side curve to head
        cubicTo(
            center.x - 3.dp.toPx(), center.y - stemHeight * 0.45f,
            center.x - headRadius, headCenterY + headRadius * 0.7f,
            center.x - headRadius, headCenterY
        )
        // Top semicircle arc around head
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = center.x - headRadius,
                top = headCenterY - headRadius,
                right = center.x + headRadius,
                bottom = headCenterY + headRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        // Right side curve back to tip
        cubicTo(
            center.x + headRadius, headCenterY + headRadius * 0.7f,
            center.x + 3.dp.toPx(), center.y - stemHeight * 0.45f,
            center.x, center.y
        )
        close()
    }

    // White outer border stroke
    drawPath(
        path = pinPath,
        color = Color.White,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Filled Teardrop Pin Body
    drawPath(
        path = pinPath,
        color = color
    )

    // Inner White Disc inside Head for High Contrast Sequence Number
    val discRadius = headRadius * 0.72f
    drawCircle(
        color = Color.White,
        radius = discRadius,
        center = Offset(center.x, headCenterY)
    )

    // Sequence Number or Checkmark Text
    val seqStr = if (isVisited) "✓" else sequence.toString()
    val textResult = textMeasurer.measure(
        text = seqStr,
        style = TextStyle(
            color = if (isVisited) EmeraldSuccess else color,
            fontSize = if (isSelected || isTarget) 12.sp else 10.sp,
            fontWeight = FontWeight.Black
        )
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = center.x - (textResult.size.width / 2f),
            y = headCenterY - (textResult.size.height / 2f)
        )
    )

    // Outlet Title Label (Only shown when explicitly selected/focused)
    if (isSelected) {
        val labelStr = if (name.length > 20) "${name.take(18)}.." else name
        val labelResult = textMeasurer.measure(
            text = labelStr,
            style = TextStyle(
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        val padH = 8.dp.toPx()
        val padV = 4.dp.toPx()
        val bubbleWidth = labelResult.size.width + (padH * 2)
        val bubbleHeight = labelResult.size.height + (padV * 2)
        val bubbleTop = headCenterY - headRadius - bubbleHeight - 6.dp.toPx()
        val bubbleLeft = center.x - (bubbleWidth / 2f)

        // Draw sleek dark rounded pill background
        drawRoundRect(
            color = Slate950.copy(alpha = 0.94f),
            topLeft = Offset(bubbleLeft, bubbleTop),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        // Draw Label Text
        drawText(
            textLayoutResult = labelResult,
            topLeft = Offset(bubbleLeft + padH, bubbleTop + padV)
        )
    }
}

/**
 * Draws a special animated dropped pin marker for newly selected map locations.
 * The needle tip points directly to [center].
 */
private fun DrawScope.drawDroppedMarkerPin(
    center: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val headRadius = 16.dp.toPx()
    val stemHeight = headRadius * 1.5f
    val headCenterY = center.y - stemHeight - headRadius

    // 1. Ground contact shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(center.x - 8.dp.toPx(), center.y - 3.dp.toPx()),
        size = Size(16.dp.toPx(), 6.dp.toPx())
    )

    // Pulsing outer halo at tip
    drawCircle(
        color = RoseDanger.copy(alpha = 0.25f),
        radius = 24.dp.toPx(),
        center = center
    )

    // Teardrop Pin Path pointing down to center
    val pinPath = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            center.x - 3.dp.toPx(), center.y - stemHeight * 0.45f,
            center.x - headRadius, headCenterY + headRadius * 0.7f,
            center.x - headRadius, headCenterY
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = center.x - headRadius,
                top = headCenterY - headRadius,
                right = center.x + headRadius,
                bottom = headCenterY + headRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        cubicTo(
            center.x + headRadius, headCenterY + headRadius * 0.7f,
            center.x + 3.dp.toPx(), center.y - stemHeight * 0.45f,
            center.x, center.y
        )
        close()
    }

    // White outer border
    drawPath(
        path = pinPath,
        color = Color.White,
        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Main Pin Body
    drawPath(
        path = pinPath,
        color = RoseDanger
    )

    // Inner White Disc
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.72f,
        center = Offset(center.x, headCenterY)
    )

    // Plus icon / text
    val textResult = textMeasurer.measure(
        text = "+",
        style = TextStyle(
            color = RoseDanger,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    )
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x = center.x - (textResult.size.width / 2f),
            y = headCenterY - (textResult.size.height / 2f)
        )
    )

    // Dropped label bubble
    val labelResult = textMeasurer.measure(
        text = "📍 Outlet Baru",
        style = TextStyle(
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    )
    val padH = 8.dp.toPx()
    val padV = 4.dp.toPx()
    val bubbleWidth = labelResult.size.width + (padH * 2)
    val bubbleHeight = labelResult.size.height + (padV * 2)
    val bubbleTop = headCenterY - headRadius - bubbleHeight - 6.dp.toPx()
    val bubbleLeft = center.x - (bubbleWidth / 2f)

    drawRoundRect(
        color = Slate950.copy(alpha = 0.94f),
        topLeft = Offset(bubbleLeft, bubbleTop),
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawText(
        textLayoutResult = labelResult,
        topLeft = Offset(bubbleLeft + padH, bubbleTop + padV)
    )
}

