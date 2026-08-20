package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageCompressor {

    private const val PHOTO_DIR_NAME = "outlet_photos"
    private const val DEFAULT_MAX_DIMENSION = 1024
    private const val DEFAULT_JPEG_QUALITY = 75

    fun getOutletPhotosDir(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Compresses and copies an image from any source (Gallery content://, Camera file://, etc.)
     * into the app's persistent internal storage (/files/outlet_photos/).
     *
     * Downscales large photos (e.g. 12MP-48MP camera shots) to max 1024px and compresses with 75% quality.
     * Shrinks size from ~5MB-10MB down to ~80KB-150KB while preserving crisp mobile clarity.
     *
     * @return file Uri string (e.g. file:///data/user/0/com.example/files/outlet_photos/...)
     */
    suspend fun compressAndPersistPhoto(
        context: Context,
        sourceUriString: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): String = withContext(Dispatchers.IO) {
        try {
            val sourceUri = Uri.parse(sourceUriString)
            val photosDir = getOutletPhotosDir(context)

            // If it's already an existing persistent internal photo file, return it
            if (sourceUriString.startsWith("file://") && sourceUriString.contains(PHOTO_DIR_NAME)) {
                val existingFile = File(sourceUri.path ?: "")
                if (existingFile.exists() && existingFile.length() > 0) {
                    return@withContext sourceUriString
                }
            }

            // 1. Determine image dimensions without full allocation
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            openInputStream(context, sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) {
                return@withContext sourceUriString
            }

            // 2. Calculate sample size
            var inSampleSize = 1
            while ((origWidth / inSampleSize) > (maxDimension * 1.5) || (origHeight / inSampleSize) > (maxDimension * 1.5)) {
                inSampleSize *= 2
            }

            // 3. Decode bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val sampledBitmap = openInputStream(context, sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext sourceUriString

            // 4. Calculate exact scaled dimensions
            val currentWidth = sampledBitmap.width
            val currentHeight = sampledBitmap.height
            val scale = (maxDimension.toFloat() / maxOf(currentWidth, currentHeight)).coerceAtMost(1.0f)

            val scaledBitmap = if (scale < 1.0f) {
                val targetW = (currentWidth * scale).toInt().coerceAtLeast(1)
                val targetH = (currentHeight * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true).also {
                    if (it != sampledBitmap) sampledBitmap.recycle()
                }
            } else {
                sampledBitmap
            }

            // 5. Handle EXIF Rotation if present
            val orientation = getExifOrientation(context, sourceUri)
            val rotatedBitmap = if (orientation != 0) {
                val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
                Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true).also {
                    if (it != scaledBitmap) scaledBitmap.recycle()
                }
            } else {
                scaledBitmap
            }

            // 6. Save compressed JPEG to internal storage
            val targetFileName = "outlet_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val targetFile = File(photosDir, targetFileName)

            FileOutputStream(targetFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            rotatedBitmap.recycle()

            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUriString
        }
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            openInputStream(context, uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
        }
    }
}
