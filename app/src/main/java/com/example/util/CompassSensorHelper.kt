package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Real-time Device Compass & Orientation Provider using Android Hardware Sensors
 * (Rotation Vector, Accelerometer + Magnetometer).
 *
 * Provides smooth bearing (0 - 359 degrees) indicating the physical direction
 * the user / smartphone is facing (Vision Cone / Forward Direction).
 */
object CompassSensorHelper {

    /**
     * Observes the physical azimuth/compass heading of the phone in degrees (0 - 360).
     * Automatically applies low-pass smoothing to eliminate jitter when standing still.
     */
    fun observeHeading(context: Context): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            close()
            return@callbackFlow
        }

        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var lastAzimuth = 0f
        var hasInitialized = false

        // Low-pass filter smoothing coefficient (0.0 to 1.0)
        // Higher alpha = faster response, lower alpha = smoother output
        val alpha = 0.25f

        val listener = object : SensorEventListener {
            private val gravity = FloatArray(3)
            private val geomagnetic = FloatArray(3)
            private var hasGravity = false
            private var hasGeomagnetic = false

            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                var azimuthDeg: Float? = null

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val rad = orientationAngles[0]
                    azimuthDeg = ((Math.toDegrees(rad.toDouble()).toFloat() % 360f) + 360f) % 360f
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                    hasGeomagnetic = true
                }

                if (azimuthDeg == null && hasGravity && hasGeomagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        SensorManager.getOrientation(r, orientationAngles)
                        val rad = orientationAngles[0]
                        azimuthDeg = ((Math.toDegrees(rad.toDouble()).toFloat() % 360f) + 360f) % 360f
                    }
                }

                azimuthDeg?.let { currentDeg ->
                    if (!hasInitialized) {
                        lastAzimuth = currentDeg
                        hasInitialized = true
                        trySend(currentDeg)
                        return
                    }

                    // Handle 359 <-> 0 wrap-around gracefully for smoothing
                    var diff = currentDeg - lastAzimuth
                    while (diff < -180f) diff += 360f
                    while (diff > 180f) diff -= 360f

                    // Filter out sub-degree jitter (< 0.6 deg)
                    if (abs(diff) > 0.6f) {
                        lastAzimuth = (lastAzimuth + diff * alpha + 360f) % 360f
                        trySend(lastAzimuth)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register best available sensor
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
