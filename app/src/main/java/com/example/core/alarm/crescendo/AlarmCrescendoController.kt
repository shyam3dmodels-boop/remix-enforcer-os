package com.example.core.alarm.crescendo

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Gradual volume crescendo controller inspired by AlarmClockXtreme.
 * Smoothly ramps alarm volume from starting minimum to 100% max over configured seconds.
 */
class AlarmCrescendoController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var crescendoJob: Job? = null

    /**
     * Starts gradual volume escalation on STREAM_ALARM.
     * @param durationSeconds Total seconds to reach maximum volume (e.g. 30s)
     * @param startRatio Initial volume fraction (e.g. 0.10 for 10%)
     */
    fun startCrescendo(
        scope: CoroutineScope,
        durationSeconds: Int = 30,
        startRatio: Float = 0.10f
    ) {
        crescendoJob?.cancel()
        val am = audioManager ?: return

        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val initialVol = (maxVolume * startRatio).roundToInt().coerceAtLeast(1)

        try {
            am.setStreamVolume(AudioManager.STREAM_ALARM, initialVol, 0)
        } catch (e: Exception) {
            Log.e("AlarmCrescendo", "Failed to set initial crescendo volume", e)
        }

        crescendoJob = scope.launch {
            val totalSteps = (maxVolume - initialVol).coerceAtLeast(1)
            val stepDelayMs = (durationSeconds * 1000L) / totalSteps

            for (step in 1..totalSteps) {
                if (!isActive) break
                delay(stepDelayMs)
                val targetVol = initialVol + step
                try {
                    am.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0)
                    Log.d("AlarmCrescendo", "Crescendo step $step/$totalSteps: Volume = $targetVol/$maxVolume")
                } catch (e: Exception) {
                    Log.e("AlarmCrescendo", "Error updating crescendo step", e)
                }
            }
        }
    }

    fun stopCrescendo() {
        crescendoJob?.cancel()
        crescendoJob = null
    }
}
