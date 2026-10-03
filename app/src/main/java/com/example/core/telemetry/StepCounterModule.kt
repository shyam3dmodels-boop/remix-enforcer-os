package com.example.core.telemetry

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real Hardware Step Counter & Physical Activity Telemetry Module.
 *
 * Guarantees:
 * 1. Zero fake/mock baseline steps (starts from 0 on new days).
 * 2. Persistent daily step storage via SharedPreferences keyed by date (YYYYMMDD).
 * 3. Automatic midnight reset.
 * 4. Dual hardware sensor support:
 *    - Sensor.TYPE_STEP_COUNTER (measures total steps since device boot)
 *    - Sensor.TYPE_STEP_DETECTOR (fires event on each individual step)
 */
class StepCounterModule(private val context: Context) : SensorEventListener {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _stepsToday = MutableStateFlow(getPersistedStepsForToday())
    val stepsToday: StateFlow<Int> = _stepsToday.asStateFlow()

    private val _stepGoal = MutableStateFlow(8000)
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    private val _walkingState = MutableStateFlow("Stationary")
    val walkingState: StateFlow<String> = _walkingState.asStateFlow()

    private val _isSensorActive = MutableStateFlow(false)
    val isSensorActive: StateFlow<Boolean> = _isSensorActive.asStateFlow()

    private var lastStepTimestamp = 0L

    init {
        checkAndResetIfNewDay()
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }

    private fun getPersistedStepsForToday(): String {
        val todayKey = getTodayDateKey()
        val savedDate = prefs.getString(KEY_SAVED_DATE, "") ?: ""
        return if (savedDate == todayKey) {
            prefs.getInt(KEY_DAILY_STEPS, 0).toString()
        } else {
            "0"
        }
    }.let { it.toIntOrNull() ?: 0 }

    private fun checkAndResetIfNewDay() {
        val todayKey = getTodayDateKey()
        val savedDate = prefs.getString(KEY_SAVED_DATE, "") ?: ""
        if (savedDate != todayKey) {
            // New day detected: reset daily steps to 0 and clear boot offset
            prefs.edit()
                .putString(KEY_SAVED_DATE, todayKey)
                .putInt(KEY_DAILY_STEPS, 0)
                .putFloat(KEY_BOOT_BASELINE, -1f)
                .apply()
            _stepsToday.value = 0
            _walkingState.value = "Stationary"
            Log.d(TAG, "New day initialized ($todayKey). Steps reset to 0.")
        }
    }

    fun startListening() {
        if (sensorManager == null) {
            Log.w(TAG, "SensorManager unavailable on this hardware.")
            return
        }

        checkAndResetIfNewDay()

        var registered = false
        if (stepCounterSensor != null) {
            registered = sensorManager.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        if (!registered && stepDetectorSensor != null) {
            registered = sensorManager.registerListener(
                this,
                stepDetectorSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        _isSensorActive.value = registered
        Log.d(TAG, "Step sensor listener registered: $registered")
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        _isSensorActive.value = false
        _walkingState.value = "Stationary"
    }

    fun addManualBreakSteps(steps: Int) {
        checkAndResetIfNewDay()
        val current = _stepsToday.value
        val updated = (current + steps).coerceAtLeast(0)
        _stepsToday.value = updated
        prefs.edit().putInt(KEY_DAILY_STEPS, updated).apply()
        _walkingState.value = "Walking"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        checkAndResetIfNewDay()

        val now = System.currentTimeMillis()

        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values.firstOrNull() ?: return
            val savedBaseline = prefs.getFloat(KEY_BOOT_BASELINE, -1f)

            if (savedBaseline < 0f || totalStepsSinceBoot < savedBaseline) {
                // Device just rebooted or first run today
                prefs.edit().putFloat(KEY_BOOT_BASELINE, totalStepsSinceBoot).apply()
            }

            val currentBaseline = prefs.getFloat(KEY_BOOT_BASELINE, totalStepsSinceBoot)
            val deltaFromBaseline = (totalStepsSinceBoot - currentBaseline).toInt().coerceAtLeast(0)
            
            val baseSteps = prefs.getInt(KEY_BASE_STEPS_TODAY, 0)
            val calculatedSteps = baseSteps + deltaFromBaseline

            _stepsToday.value = calculatedSteps
            prefs.edit().putInt(KEY_DAILY_STEPS, calculatedSteps).apply()

            _walkingState.value = if (now - lastStepTimestamp < 6000L) "Walking" else "Stationary"
            lastStepTimestamp = now
        } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            val updated = _stepsToday.value + 1
            _stepsToday.value = updated
            prefs.edit().putInt(KEY_DAILY_STEPS, updated).apply()

            _walkingState.value = if (now - lastStepTimestamp < 6000L) "Walking" else "Stationary"
            lastStepTimestamp = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Hardware accuracy updates
    }

    companion object {
        private const val TAG = "StepCounterModule"
        private const val PREFS_NAME = "enforcer_step_tracker"
        private const val KEY_SAVED_DATE = "key_step_saved_date"
        private const val KEY_DAILY_STEPS = "key_daily_steps"
        private const val KEY_BOOT_BASELINE = "key_boot_baseline"
        private const val KEY_BASE_STEPS_TODAY = "key_base_steps_today"

        @Volatile
        private var instance: StepCounterModule? = null

        fun getInstance(context: Context): StepCounterModule {
            return instance ?: synchronized(this) {
                instance ?: StepCounterModule(context.applicationContext).also { instance = it }
            }
        }
    }
}
