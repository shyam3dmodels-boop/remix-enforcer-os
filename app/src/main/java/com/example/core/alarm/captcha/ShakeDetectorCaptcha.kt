package com.example.core.alarm.captcha

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Accelerometer shake sensor listener inspired by Sleep as Android CAPTCHA.
 * Measures directional acceleration surges to ensure physical activity before dismissing alarm.
 */
class ShakeDetectorCaptcha(
    context: Context,
    private val requiredShakes: Int = 30,
    private val onProgress: (current: Int, target: Int) -> Unit,
    private val onComplete: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var isListening = false

    fun start() {
        if (isListening || accelerometer == null) return
        shakeCount = 0
        isListening = true
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onProgress(shakeCount, requiredShakes)
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate total G-force (excluding normal gravity ~9.8m/s^2)
        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

        if (gForce > SHAKE_THRESHOLD_G) {
            val now = System.currentTimeMillis()
            // Enforce minimum 180ms debounce between shakes to prevent trivial micro-vibrations
            if (now - lastShakeTime > SHAKE_DEBOUNCE_MS) {
                lastShakeTime = now
                shakeCount++
                onProgress(shakeCount, requiredShakes)

                if (shakeCount >= requiredShakes) {
                    stop()
                    onComplete()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val SHAKE_THRESHOLD_G = 1.95f
        private const val SHAKE_DEBOUNCE_MS = 180L
    }
}
