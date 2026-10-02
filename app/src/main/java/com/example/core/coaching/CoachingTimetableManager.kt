package com.example.core.coaching

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.entity.CoachingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages coaching timetable class detection and automatic silent/DND mode
 * during active lecture hours.
 */
class CoachingTimetableManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("coaching_timetable_prefs", Context.MODE_PRIVATE)

    private val geofenceSilentManager = GeofenceSilentManager(context)

    private val _isAutoSilentEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_SILENT_ENABLED, true)
    )
    val isAutoSilentEnabled: StateFlow<Boolean> = _isAutoSilentEnabled.asStateFlow()

    private val _currentActiveSession = MutableStateFlow<CoachingEntity?>(null)
    val currentActiveSession: StateFlow<CoachingEntity?> = _currentActiveSession.asStateFlow()

    private val _isClassSilentActive = MutableStateFlow(false)
    val isClassSilentActive: StateFlow<Boolean> = _isClassSilentActive.asStateFlow()

    fun setAutoSilentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SILENT_ENABLED, enabled).apply()
        _isAutoSilentEnabled.value = enabled
    }

    /**
     * Evaluates all coaching schedules against the current device clock.
     * Returns true if currently within a scheduled class window (session start to +90 mins).
     */
    fun checkAndApplyClassSilentMode(schedules: List<CoachingEntity>): Pair<Boolean, CoachingEntity?> {
        val now = Calendar.getInstance()
        val currentMinutesOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (schedule in schedules) {
            if (!schedule.isActive) continue
            val startMinutes = parseTimeToMinutesOfDay(schedule.sessionTime) ?: continue
            val endMinutes = startMinutes + 90 // Standard 90-minute coaching class duration

            if (currentMinutesOfDay in startMinutes..endMinutes) {
                _currentActiveSession.value = schedule
                if (_isAutoSilentEnabled.value && !_isClassSilentActive.value) {
                    geofenceSilentManager.applyCoachingArrivalSilent(schedule.title)
                    _isClassSilentActive.value = true
                    Log.d("CoachingTimetable", "Auto-silent engaged for: ${schedule.title}")
                }
                return Pair(true, schedule)
            }
        }

        // Not in class
        if (_isClassSilentActive.value) {
            val prev = _currentActiveSession.value
            if (prev != null) {
                geofenceSilentManager.applyCoachingDepartureRestore(prev.title)
            }
            _isClassSilentActive.value = false
            Log.d("CoachingTimetable", "Class ended -> Audio restored")
        }
        _currentActiveSession.value = null
        return Pair(false, null)
    }

    fun forceClassSilent(schedule: CoachingEntity) {
        geofenceSilentManager.applyCoachingArrivalSilent(schedule.title)
        _currentActiveSession.value = schedule
        _isClassSilentActive.value = true
    }

    fun forceRestoreAudio(schedule: CoachingEntity) {
        geofenceSilentManager.applyCoachingDepartureRestore(schedule.title)
        _isClassSilentActive.value = false
    }

    private fun parseTimeToMinutesOfDay(timeStr: String): Int? {
        val clean = timeStr.trim()
        val formats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                val date = sdf.parse(clean)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    companion object {
        private const val KEY_AUTO_SILENT_ENABLED = "key_auto_silent_enabled"
    }
}
