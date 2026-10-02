package com.example.core.coaching

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.core.alarm.AlarmSafetyManager
import com.example.core.recorder.LectureRecorderManager
import com.example.data.local.entity.CoachingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActiveEnforcerMode(val label: String, val description: String) {
    STUDY_MODE("STUDY MODE", "Strict focus active. Ringer normal, alarms ready, discipline limits running."),
    CLASS_RECORDER_MODE("CLASS + RECORDER MODE", "Classroom perimeter detected. Class Shield active, ringer silenced, background lecture recorder triggered."),
    SAFE_CLASS_MODE("SAFE CLASS MODE", "Silent mode active. All audio and alarm sirens blocked.")
}

/**
 * Intelligent Smart Wi-Fi and Geofence Mode Switching Engine.
 * 1. Tracks proximity to Coaching/School locations via GPS distance.
 * 2. Detects connection to designated School/Campus Wi-Fi SSIDs (e.g. 'Coaching-WiFi', 'Campus-5G', 'College_Net').
 * 3. Automatically triggers:
 *    - Mode Transition: STUDY_MODE <-> CLASS_RECORDER_MODE
 *    - Alarm Class Shield (silencing sirens, strobes, buzzers)
 *    - Automatic Background Lecture Recording
 *    - Lockscreen wallpaper clearing
 */
class SmartClassEnvironmentEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_class_env_prefs", Context.MODE_PRIVATE)

    private val alarmSafety = AlarmSafetyManager.getInstance(context)
    private val geofenceSilent = GeofenceSilentManager(context)
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _currentMode = MutableStateFlow(ActiveEnforcerMode.STUDY_MODE)
    val currentMode: StateFlow<ActiveEnforcerMode> = _currentMode.asStateFlow()

    private val _isWifiAutoMuteEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WIFI_AUTO_MUTE, true)
    )
    val isWifiAutoMuteEnabled: StateFlow<Boolean> = _isWifiAutoMuteEnabled.asStateFlow()

    private val _isGeofenceAutoRecordEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_GEOFENCE_AUTO_RECORD, true)
    )
    val isGeofenceAutoRecordEnabled: StateFlow<Boolean> = _isGeofenceAutoRecordEnabled.asStateFlow()

    private val _isTimetableAutoRecordEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_TIMETABLE_AUTO_RECORD, true)
    )
    val isTimetableAutoRecordEnabled: StateFlow<Boolean> = _isTimetableAutoRecordEnabled.asStateFlow()

    private val _savedWifiKeywords = MutableStateFlow(
        prefs.getString(KEY_WIFI_KEYWORDS, "Office,Campus,Studio,Library,Work,Quiet_Zone") ?: ""
    )
    val savedWifiKeywords: StateFlow<String> = _savedWifiKeywords.asStateFlow()

    private val _detectedSsid = MutableStateFlow<String?>(null)
    val detectedSsid: StateFlow<String?> = _detectedSsid.asStateFlow()

    private val _activeLocationGeofence = MutableStateFlow<String?>(null)
    val activeLocationGeofence: StateFlow<String?> = _activeLocationGeofence.asStateFlow()

    fun setWifiAutoMuteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_AUTO_MUTE, enabled).apply()
        _isWifiAutoMuteEnabled.value = enabled
    }

    fun setGeofenceAutoRecordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GEOFENCE_AUTO_RECORD, enabled).apply()
        _isGeofenceAutoRecordEnabled.value = enabled
    }

    fun setTimetableAutoRecordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TIMETABLE_AUTO_RECORD, enabled).apply()
        _isTimetableAutoRecordEnabled.value = enabled
    }

    fun setWifiKeywords(keywords: String) {
        prefs.edit().putString(KEY_WIFI_KEYWORDS, keywords).apply()
        _savedWifiKeywords.value = keywords
    }

    /**
     * Inspects current Wi-Fi connection and checks if it matches coaching/school Wi-Fi networks.
     */
    fun checkWifiAndApplyClassShield(): Boolean {
        if (!_isWifiAutoMuteEnabled.value) return false
        try {
            val connectionInfo: WifiInfo? = wifiManager?.connectionInfo
            val rawSsid = connectionInfo?.ssid?.replace("\"", "")?.trim()
            _detectedSsid.value = rawSsid

            if (!rawSsid.isNullOrBlank() && rawSsid != "<unknown ssid>") {
                val keywords = _savedWifiKeywords.value.split(",").map { it.trim().lowercase() }
                val matches = keywords.any { kw -> kw.isNotEmpty() && rawSsid.lowercase().contains(kw) }

                if (matches) {
                    alarmSafety.setClassShieldActive(true)
                    geofenceSilent.applyCoachingArrivalSilent("Wi-Fi: $rawSsid")
                    transitionToMode(ActiveEnforcerMode.SAFE_CLASS_MODE, "Connected to Coaching Wi-Fi: $rawSsid")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("SmartClassEnv", "Error checking Wi-Fi status", e)
        }
        return false
    }

    /**
     * Evaluates current user GPS coordinates against all active coaching locations.
     * If user is inside radius (<= radiusMeters):
     * -> Enters CLASS_RECORDER_MODE (Class Shield ON, Audio Muted, Auto-Recorder triggered)
     * If user leaves radius:
     * -> Restores to STUDY_MODE (Class Shield OFF, Audio restored)
     */
    fun evaluateGeofenceTransition(
        currentLat: Double,
        currentLng: Double,
        schedules: List<CoachingEntity>
    ): Pair<ActiveEnforcerMode, CoachingEntity?> {
        for (schedule in schedules) {
            if (!schedule.isActive) continue
            val distance = calculateDistanceMeters(currentLat, currentLng, schedule.latitude, schedule.longitude)
            if (distance <= schedule.radiusMeters) {
                // Inside coaching perimeter
                _activeLocationGeofence.value = schedule.title
                val isInsideMode = if (_isGeofenceAutoRecordEnabled.value) {
                    ActiveEnforcerMode.CLASS_RECORDER_MODE
                } else {
                    ActiveEnforcerMode.SAFE_CLASS_MODE
                }

                transitionToMode(isInsideMode, "Entered geofence for ${schedule.title} ($distance m)")

                // Trigger background lecture recorder if enabled and not already recording
                if (_isGeofenceAutoRecordEnabled.value) {
                    triggerAutoRecordingIfIdle(schedule.title, "Geofence Auto-Record")
                }

                // Activate Class Shield & phone ringer silence
                alarmSafety.setClassShieldActive(true)
                geofenceSilent.applyCoachingArrivalSilent(schedule.title)

                return Pair(isInsideMode, schedule)
            }
        }

        // Outside all geofences
        if (_activeLocationGeofence.value != null || _currentMode.value != ActiveEnforcerMode.STUDY_MODE) {
            val prevName = _activeLocationGeofence.value ?: "Coaching Center"
            geofenceSilent.applyCoachingDepartureRestore(prevName)
            alarmSafety.setClassShieldActive(false)
            _activeLocationGeofence.value = null
            transitionToMode(ActiveEnforcerMode.STUDY_MODE, "Left geofence perimeter -> Restoring Study Mode")
        }

        return Pair(ActiveEnforcerMode.STUDY_MODE, null)
    }

    /**
     * Triggered by timetable scheduler when class session start time is reached.
     */
    fun onTimetableClassStarted(schedule: CoachingEntity) {
        alarmSafety.setClassShieldActive(true)
        geofenceSilent.applyCoachingArrivalSilent(schedule.title)

        val targetMode = if (_isTimetableAutoRecordEnabled.value) {
            triggerAutoRecordingIfIdle(schedule.title, "Timetable Auto-Record")
            ActiveEnforcerMode.CLASS_RECORDER_MODE
        } else {
            ActiveEnforcerMode.SAFE_CLASS_MODE
        }
        transitionToMode(targetMode, "Timetable class started: ${schedule.title}")
    }

    fun onTimetableClassEnded(schedule: CoachingEntity) {
        geofenceSilent.applyCoachingDepartureRestore(schedule.title)
        alarmSafety.setClassShieldActive(false)
        transitionToMode(ActiveEnforcerMode.STUDY_MODE, "Timetable class concluded: ${schedule.title}")
    }

    private fun triggerAutoRecordingIfIdle(title: String, subject: String) {
        val recorder = LectureRecorderManager.instance
        if (recorder.recordingState.value == com.example.core.recorder.RecordingState.IDLE) {
            recorder.startRecording(context, title, subject)
            Log.d("SmartClassEnv", "Auto-recording started for: $title")
        }
    }

    private fun transitionToMode(mode: ActiveEnforcerMode, reason: String) {
        _currentMode.value = mode
        Log.d("SmartClassEnv", "MODE SWITCH: $mode | Reason: $reason")
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    companion object {
        private const val KEY_WIFI_AUTO_MUTE = "key_wifi_auto_mute"
        private const val KEY_GEOFENCE_AUTO_RECORD = "key_geofence_auto_record"
        private const val KEY_TIMETABLE_AUTO_RECORD = "key_timetable_auto_record"
        private const val KEY_WIFI_KEYWORDS = "key_wifi_keywords"

        @Volatile
        private var INSTANCE: SmartClassEnvironmentEngine? = null

        fun getInstance(context: Context): SmartClassEnvironmentEngine {
            return INSTANCE ?: synchronized(this) {
                val inst = SmartClassEnvironmentEngine(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }
}
