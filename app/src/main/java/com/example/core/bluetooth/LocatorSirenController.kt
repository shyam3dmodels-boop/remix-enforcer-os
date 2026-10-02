package com.example.core.bluetooth

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocatorSirenController(private val context: Context) {
    private var sirenJob: Job? = null

    fun isPlaying(): Boolean = sirenJob?.isActive == true

    fun startSiren(scope: CoroutineScope) {
        if (sirenJob?.isActive == true) return

        sirenJob = scope.launch(Dispatchers.Default) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // Maximize stream volume for audio locator
            try {
                am?.let {
                    val max = it.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    it.setStreamVolume(AudioManager.STREAM_ALARM, max, AudioManager.FLAG_PLAY_SOUND)
                }
            } catch (e: Exception) {
                Log.e("LocatorSiren", "Could not set max volume", e)
            }

            while (isActive) {
                try {
                    val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    // High-low emergency beacon siren
                    tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
                    delay(1600L)
                    tg.release()
                } catch (e: Exception) {
                    Log.e("LocatorSiren", "Siren playback exception", e)
                    delay(1000L)
                }
            }
        }
    }

    fun stopSiren() {
        sirenJob?.cancel()
        sirenJob = null
    }
}
