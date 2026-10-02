package com.example.core.recorder

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Microphone distance amplification presets designed for university & coaching halls.
 */
enum class DistanceMicGain(
    val multiplier: Float,
    val label: String,
    val shortLabel: String,
    val description: String
) {
    FRONT_ROW(
        multiplier = 1.0f,
        label = "1.0× Standard (Front Row)",
        shortLabel = "1.0×",
        description = "Standard sensitivity for front desk or podium distance"
    ),
    MID_HALL(
        multiplier = 1.5f,
        label = "1.5× Boosted (Mid Hall)",
        shortLabel = "1.5×",
        description = "Acoustic amplification for middle rows in 50-100 student classrooms"
    ),
    BACK_ROW(
        multiplier = 2.0f,
        label = "2.0× High Gain (Back Row)",
        shortLabel = "2.0×",
        description = "Distant speaker capture from back benches and corners"
    ),
    SUPER_FAR(
        multiplier = 3.0f,
        label = "3.0× Maximum (Auditorium)",
        shortLabel = "3.0×",
        description = "Maximum digital boost for large auditoriums and soft-spoken lecturers"
    )
}

/**
 * Manages persistent audio hardware & DSP configuration for lecture recordings.
 */
class AudioRecordingSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("audio_recording_prefs", Context.MODE_PRIVATE)

    private val _isVoiceFocusEnabled =
        MutableStateFlow(prefs.getBoolean("voice_focus_enabled", true))
    val isVoiceFocusEnabled: StateFlow<Boolean> = _isVoiceFocusEnabled.asStateFlow()

    private val _isAcHumFilterEnabled =
        MutableStateFlow(prefs.getBoolean("ac_hum_filter_enabled", true))
    val isAcHumFilterEnabled: StateFlow<Boolean> = _isAcHumFilterEnabled.asStateFlow()

    private val savedGain = prefs.getFloat("distance_gain", 1.5f)
    private val initialGain = DistanceMicGain.entries.firstOrNull { it.multiplier == savedGain }
        ?: DistanceMicGain.MID_HALL

    private val _distanceGain = MutableStateFlow(initialGain)
    val distanceGain: StateFlow<DistanceMicGain> = _distanceGain.asStateFlow()

    fun toggleVoiceFocus(): Boolean {
        val next = !_isVoiceFocusEnabled.value
        _isVoiceFocusEnabled.value = next
        prefs.edit().putBoolean("voice_focus_enabled", next).apply()
        return next
    }

    fun setVoiceFocusEnabled(enabled: Boolean) {
        _isVoiceFocusEnabled.value = enabled
        prefs.edit().putBoolean("voice_focus_enabled", enabled).apply()
    }

    fun toggleAcHumFilter(): Boolean {
        val next = !_isAcHumFilterEnabled.value
        _isAcHumFilterEnabled.value = next
        prefs.edit().putBoolean("ac_hum_filter_enabled", next).apply()
        return next
    }

    fun setAcHumFilterEnabled(enabled: Boolean) {
        _isAcHumFilterEnabled.value = enabled
        prefs.edit().putBoolean("ac_hum_filter_enabled", enabled).apply()
    }

    fun setDistanceGain(gain: DistanceMicGain) {
        _distanceGain.value = gain
        prefs.edit().putFloat("distance_gain", gain.multiplier).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioRecordingSettings? = null

        fun getInstance(context: Context): AudioRecordingSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioRecordingSettings(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
