package com.example.util.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.screens.MapLayerStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

/**
 * High-performance Offline & Online Tile Engine for OpenStreetMap (OSM) and CartoDB.
 * Supports Web Mercator projection, memory LRU cache, local disk persistence,
 * and bulk offline area pre-caching.
 */
object OsmTileEngine {

    private const val USER_AGENT = "SFASalesMobile/2.0 (Android; OpenStreetMap Integration)"
    private const val TILE_SIZE = 256

    // In-memory LRU Cache (expanded to 600 tiles for ultra-smooth 60+ FPS panning & zooming)
    private val memoryCache = object : LruCache<String, ImageBitmap>(600) {}

    // In-flight fetch tracking to prevent duplicate network calls
    private val activeFetches = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Instantly retrieve tile from memory LRU cache without coroutines or disk I/O.
     */
    fun getMemoryCachedTile(style: MapLayerStyle, z: Int, x: Int, y: Int): ImageBitmap? {
        val cacheKey = "${style.name}_${z}_${x}_$y"
        synchronized(memoryCache) {
            return memoryCache.get(cacheKey)
        }
    }

    /**
     * Retrieve parent zoom level tile (z-1) from memory to prevent blank canvas while loading.
     * Returns Triple(ParentBitmap, quadrantX 0..1, quadrantY 0..1).
     */
    fun getParentMemoryCachedTile(style: MapLayerStyle, z: Int, x: Int, y: Int): Triple<ImageBitmap, Int, Int>? {
        if (z <= 3) return null
        val parentZ = z - 1
        val parentX = x / 2
        val parentY = y / 2
        val parentKey = "${style.name}_${parentZ}_${parentX}_$parentY"
        val bmp = synchronized(memoryCache) { memoryCache.get(parentKey) } ?: return null
        val quadX = (x % 2).coerceIn(0, 1)
        val quadY = (y % 2).coerceIn(0, 1)
        return Triple(bmp, quadX, quadY)
    }

    /**
     * Get Tile URL template based on selected map style
     */
    fun getTileUrl(style: MapLayerStyle, z: Int, x: Int, y: Int): String {
        return when (style) {
            MapLayerStyle.OSM_STANDARD -> "https://tile.openstreetmap.org/$z/$x/$y.png"
            MapLayerStyle.CARTO_LIGHT -> "https://a.basemaps.cartocdn.com/rastertiles/voyager/$z/$x/$y.png"
            MapLayerStyle.DARK_NIGHT -> "https://a.basemaps.cartocdn.com/dark_all/$z/$x/$y.png"
            MapLayerStyle.OUTDOOR_TOPO -> "https://a.tile.opentopomap.org/$z/$x/$y.png"
        }
    }

    /**
     * Convert Latitude, Longitude, and Zoom into continuous Web Mercator Tile coordinates.
     */
    fun latLngToTileCoordinates(lat: Double, lng: Double, zoom: Float): Pair<Double, Double> {
        val n = 2.0.pow(zoom.toDouble())
        val x = (lng + 180.0) / 360.0 * n
        val rad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val y = (1.0 - asinh(tan(rad)) / Math.PI) / 2.0 * n
        return Pair(x, y)
    }

    /**
     * Convert continuous Web Mercator Tile coordinates back into Latitude, Longitude.
     */
    fun tileCoordinatesToLatLng(x: Double, y: Double, zoom: Float): Pair<Double, Double> {
        val n = 2.0.pow(zoom.toDouble())
        val lng = x / n * 360.0 - 180.0
        val sinhVal = sinh(Math.PI * (1.0 - 2.0 * y / n))
        val lat = Math.toDegrees(atan(sinhVal))
        return Pair(lat, lng)
    }

    /**
     * Get or fetch tile from Memory -> Disk -> Network (with caching).
     */
    suspend fun getTile(
        context: Context,
        style: MapLayerStyle,
        z: Int,
        x: Int,
        y: Int,
        onLoaded: () -> Unit = {}
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${style.name}_${z}_${x}_$y"

        // 1. Check Memory Cache
        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { return@withContext it }
        }

        // 2. Check Disk Cache
        val diskFile = getDiskTileFile(context, style, z, x, y)
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    synchronized(memoryCache) {
                        memoryCache.put(cacheKey, imageBitmap)
                    }
                    return@withContext imageBitmap
                }
            } catch (e: Exception) {
                diskFile.delete()
            }
        }

        // 3. Fetch from Network (if not already fetching)
        if (activeFetches.add(cacheKey)) {
            try {
                val urlStr = getTileUrl(style, z, x, y)
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", USER_AGENT)
                    doInput = true
                }
                conn.connect()
                if (conn.responseCode == 200) {
                    val inputStream: InputStream = conn.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()

                    if (bytes.isNotEmpty()) {
                        // Save to disk
                        diskFile.parentFile?.mkdirs()
                        FileOutputStream(diskFile).use { it.write(bytes) }

                        // Decode to Bitmap
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            synchronized(memoryCache) {
                                memoryCache.put(cacheKey, imageBitmap)
                            }
                            withContext(Dispatchers.Main) {
                                onLoaded()
                            }
                            return@withContext imageBitmap
                        }
                    }
                }
            } catch (_: Exception) {
                // Offline or network error - gracefully fall back
            } finally {
                activeFetches.remove(cacheKey)
            }
        }

        return@withContext null
    }

    /**
     * File location for tile on persistent storage.
     */
    private fun getDiskTileFile(context: Context, style: MapLayerStyle, z: Int, x: Int, y: Int): File {
        val dir = File(context.cacheDir, "osm_tiles/${style.name}/$z/$x")
        return File(dir, "$y.png")
    }

    /**
     * Calculate total offline cached size in Megabytes.
     */
    fun getOfflineCacheSizeBytes(context: Context): Long {
        val root = File(context.cacheDir, "osm_tiles")
        if (!root.exists()) return 0L
        return root.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }

    /**
     * Pre-cache all tiles for a bounding box (for 100% offline usage in the field).
     */
    suspend fun preCacheArea(
        context: Context,
        style: MapLayerStyle,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        zoomLevels: List<Int> = listOf(13, 14, 15, 16),
        onProgress: (current: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val tilesToFetch = mutableListOf<Triple<Int, Int, Int>>()

        for (z in zoomLevels) {
            val (minX, maxY) = latLngToTileCoordinates(maxLat, minLng, z.toFloat())
            val (maxX, minY) = latLngToTileCoordinates(minLat, maxLng, z.toFloat())

            val startX = floor(minX).toInt()
            val endX = floor(maxX).toInt()
            val startY = floor(minY).toInt()
            val endY = floor(maxY).toInt()

            for (x in startX..endX) {
                for (y in startY..endY) {
                    tilesToFetch.add(Triple(z, x, y))
                }
            }
        }

        var downloadedCount = 0
        val total = tilesToFetch.size

        tilesToFetch.forEachIndexed { index, (z, x, y) ->
            getTile(context, style, z, x, y)
            downloadedCount++
            withContext(Dispatchers.Main) {
                onProgress(index + 1, total)
            }
        }

        downloadedCount
    }

    /**
     * Clear all cached tiles
     */
    fun clearCache(context: Context) {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
        val root = File(context.cacheDir, "osm_tiles")
        if (root.exists()) {
            root.deleteRecursively()
        }
    }
}
