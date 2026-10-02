package com.example.core.alarm.wakeiq

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Smart Period Wake Window Engine inspired by WakeIQ.
 * Monitors actigraphy motion in a 15-30 minute window before target alarm to wake user during light sleep.
 */
class SmartWakeWindowEngine(
    context: Context,
    private val onLightSleepDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isMonitoring = false
    private var lastMovementTime = 0L
    private var movementSurgeCount = 0

    fun startSmartMonitoring() {
        if (isMonitoring || accelerometer == null) return
        isMonitoring = true
        movementSurgeCount = 0
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        Log.d("SmartWakeWindow", "Smart Period actigraphy monitoring started")
    }

    fun stopSmartMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        sensorManager?.unregisterListener(this)
        Log.d("SmartWakeWindow", "Smart Period actigraphy monitoring stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isMonitoring) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        val delta = abs(magnitude - SensorManager.GRAVITY_EARTH)

        // Light movement threshold indicating shift in sleep phase (tossing, turning)
        if (delta > LIGHT_SLEEP_DELTA_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastMovementTime > DEBOUNCE_MS) {
                lastMovementTime = now
                movementSurgeCount++
                Log.d("SmartWakeWindow", "Movement surge detected: $movementSurgeCount/$REQUIRED_SURGES")

                if (movementSurgeCount >= REQUIRED_SURGES) {
                    stopSmartMonitoring()
                    Log.i("SmartWakeWindow", "Optimal light sleep detected! Triggering gentle wake.")
                    onLightSleepDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val LIGHT_SLEEP_DELTA_THRESHOLD = 0.85
        private const val DEBOUNCE_MS = 2500L
        private const val REQUIRED_SURGES = 3 // 3 distinct movement events in window
    }
}
