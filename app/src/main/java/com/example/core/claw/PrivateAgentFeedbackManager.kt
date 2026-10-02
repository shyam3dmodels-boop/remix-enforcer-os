package com.example.core.claw

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Multi-Modal Sensory Feedback Manager for PrivateAgent.
 *
 * Implements:
 * 1. Step haptic tick/click pulses.
 * 2. Completion waveform vibrations.
 * 3. Audio tone signals (Success chime, Alert buzzer, Step tick) via ToneGenerator.
 */
class PrivateAgentFeedbackManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var toneGenerator: ToneGenerator? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator could not be initialized", e)
        }
    }

    /**
     * Subtle haptic feedback for each execution step.
     */
    fun triggerStepHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering step haptic", e)
        }
    }

    /**
     * Joyful double-pulse vibration on goal completion.
     */
    fun triggerSuccessHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 100, 150)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 80, 100, 150), -1)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering success haptic", e)
        }
    }

    /**
     * Warning triple vibration + alert tone on action failure or cancellation.
     */
    fun triggerErrorHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 120, 80, 120, 80, 120)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 120, 80, 120, 80, 120), -1)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 350)
        } catch (e: Exception) {
            Log.w(TAG, "Error triggering error haptic", e)
        }
    }

    companion object {
        private const val TAG = "PrivateAgentFeedback"

        @Volatile
        private var instance: PrivateAgentFeedbackManager? = null

        fun getInstance(context: Context): PrivateAgentFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: PrivateAgentFeedbackManager(context).also { instance = it }
            }
        }
    }
}
