package com.example.core.recorder

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PocketGestureManager manages silent haptic vibration patterns and hardware volume-key
 * pocket bookmarks for discreet classroom operations.
 */
class PocketGestureManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("pocket_gesture_prefs", Context.MODE_PRIVATE)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _isPocketGestureEnabled =
        MutableStateFlow(prefs.getBoolean("pocket_gesture_enabled", true))
    val isPocketGestureEnabled: StateFlow<Boolean> = _isPocketGestureEnabled.asStateFlow()

    private val _isHapticCuesEnabled =
        MutableStateFlow(prefs.getBoolean("haptic_cues_enabled", true))
    val isHapticCuesEnabled: StateFlow<Boolean> = _isHapticCuesEnabled.asStateFlow()

    // Key press tracking for pocket double-click bookmark detection
    private var lastVolumePressTime = 0L
    private var lastVolumeKeyCode = -1

    fun togglePocketGesture(): Boolean {
        val next = !_isPocketGestureEnabled.value
        _isPocketGestureEnabled.value = next
        prefs.edit().putBoolean("pocket_gesture_enabled", next).apply()
        return next
    }

    fun toggleHapticCues(): Boolean {
        val next = !_isHapticCuesEnabled.value
        _isHapticCuesEnabled.value = next
        prefs.edit().putBoolean("haptic_cues_enabled", next).apply()
        return next
    }

    /**
     * Intercepts volume key clicks. Returns true if a double-press bookmark was detected.
     */
    fun onVolumeKeyPressed(keyCode: Int, onBookmarkTriggered: () -> Unit): Boolean {
        if (!_isPocketGestureEnabled.value) return false

        val now = System.currentTimeMillis()
        val elapsed = now - lastVolumePressTime

        if (elapsed in 120..950 && keyCode == lastVolumeKeyCode) {
            // Double press detected!
            lastVolumePressTime = 0L
            lastVolumeKeyCode = -1
            vibrateBookmarkPinned()
            onBookmarkTriggered()
            Log.i("PocketGesture", "Pocket Double-Click detected on key $keyCode -> Stamped Bookmark!")
            return true
        } else {
            lastVolumePressTime = now
            lastVolumeKeyCode = keyCode
            return false
        }
    }

    /**
     * Subtle double-tick haptic pattern confirming bookmark stamp (50ms - 40ms pause - 50ms)
     */
    fun vibrateBookmarkPinned() {
        if (!_isHapticCuesEnabled.value || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 40, 50)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 40, 50), -1)
            }
        } catch (e: Exception) {
            Log.w("PocketGesture", "Vibration failed: ${e.message}")
        }
    }

    /**
     * Single crisp tick for recording start confirmation
     */
    fun vibrateRecordingStarted() {
        if (!_isHapticCuesEnabled.value || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80L, 220))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Double buzz for recording pause/stop confirmation
     */
    fun vibrateRecordingStopped() {
        if (!_isHapticCuesEnabled.value || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 90, 60, 90)
                val amplitudes = intArrayOf(0, 180, 0, 180)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 90, 60, 90), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Low pulse for auto-silent mode change
     */
    fun vibrateSilentModeChanged(isSilent: Boolean) {
        if (!_isHapticCuesEnabled.value || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isSilent) 150L else 70L
                val amp = if (isSilent) 120 else 200
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amp))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isSilent) 150L else 70L)
            }
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var INSTANCE: PocketGestureManager? = null

        fun getInstance(context: Context): PocketGestureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PocketGestureManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
