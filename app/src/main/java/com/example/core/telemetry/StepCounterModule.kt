package com.example.core.telemetry

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pedometer and study break motion telemetry module.
 * Tracks student physical activity to verify healthy walking breaks
 * between continuous deep-focus study blocks.
 */
class StepCounterModule(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _stepsToday = MutableStateFlow(4820) // Baseline matching 4,820 steps today
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    private val _stepGoal = MutableStateFlow(8000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _walkingState = MutableStateFlow("Stationary")
    val walkingState: StateFlow<String> = _walkingState.asStateFlow()

    private val _isSensorActive = MutableStateFlow(false)
    val isSensorActive: StateFlow<Boolean> = _isSensorActive.asStateFlow()

    private var initialCounterValue = -1
    private var lastStepTimestamp = 0L

    fun startListening() {
        if (stepSensor != null && sensorManager != null) {
            val registered = sensorManager.registerListener(
                this,
                stepSensor,
                SensorManager.SENSOR_DELAY_UI
            )
            _isSensorActive.value = registered
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        _isSensorActive.value = false
        _walkingState.value = "Stationary"
    }

    fun addManualBreakSteps(steps: Int) {
        _stepsToday.value = (_stepsToday.value + steps).coerceAtLeast(0)
        _walkingState.value = "Walking"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val now = System.currentTimeMillis()
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values.firstOrNull()?.toInt() ?: return
            if (initialCounterValue < 0) {
                initialCounterValue = totalSteps
            }
            val delta = (totalSteps - initialCounterValue).coerceAtLeast(0)
            _stepsToday.value = 4820 + delta
            _walkingState.value = if (now - lastStepTimestamp < 5000L) "Walking" else "Stationary"
            lastStepTimestamp = now
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            _stepsToday.value += 1
            _walkingState.value = if (now - lastStepTimestamp < 5000L) "Walking" else "Stationary"
            lastStepTimestamp = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    companion object {
        @Volatile
        private var instance: StepCounterModule? = null

        fun getInstance(context: Context): StepCounterModule {
            return instance ?: synchronized(this) {
                instance ?: StepCounterModule(context.applicationContext).also { instance = it }
            }
        }
    }
}
