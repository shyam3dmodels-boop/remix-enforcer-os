package com.example.core.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.data.local.entity.LectureBookmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED
}

data class CompletedRecording(
    val title: String,
    val subject: String,
    val coachingSessionName: String,
    val filePath: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val bookmarks: List<LectureBookmark>,
    val notes: String
)

class LectureRecorderManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private var tickerJob: Job? = null
    private var amplitudeJob: Job? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(List(30) { 0.05f })
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<LectureBookmark>>(emptyList())
    val bookmarks: StateFlow<List<LectureBookmark>> = _bookmarks.asStateFlow()

    private var activeTitle: String = ""
    private var activeSubject: String = ""
    private var activeCoachingSession: String = ""
    private var activeNotes: String = ""
    private var appContext: Context? = null

    fun startRecording(
        context: Context,
        title: String,
        subject: String,
        coachingSessionName: String = "",
        initialNotes: String = ""
    ): Boolean {
        if (_recordingState.value != RecordingState.IDLE) {
            return false
        }

        appContext = context.applicationContext
        activeTitle = title.ifBlank { "Lecture ${System.currentTimeMillis() % 10000}" }
        activeSubject = subject.ifBlank { "General" }
        activeCoachingSession = coachingSessionName
        activeNotes = initialNotes

        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) {
            lecturesDir.mkdirs()
        }

        val file = File(lecturesDir, "lecture_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file

        return try {
            val audioSettings = AudioRecordingSettings.getInstance(context)
            val audioSource = if (audioSettings.isVoiceFocusEnabled.value) {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            } else {
                MediaRecorder.AudioSource.MIC
            }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _recordingState.value = RecordingState.RECORDING
            _durationSeconds.value = 0L
            _bookmarks.value = emptyList()

            LectureRecorderService.startService(
                context,
                title = activeTitle,
                subject = activeSubject
            )

            startTickers()
            true
        } catch (e: Exception) {
            Log.e("LectureRecorderManager", "Failed to start recording", e)
            cleanup()
            false
        }
    }

    fun pauseRecording(context: Context) {
        if (_recordingState.value == RecordingState.RECORDING) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    _recordingState.value = RecordingState.PAUSED
                    LectureRecorderService.updateNotification(context, isPaused = true)
                }
            } catch (e: Exception) {
                Log.e("LectureRecorderManager", "Error pausing recorder", e)
            }
        }
    }

    fun resumeRecording(context: Context) {
        if (_recordingState.value == RecordingState.PAUSED) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    _recordingState.value = RecordingState.RECORDING
                    LectureRecorderService.updateNotification(context, isPaused = false)
                }
            } catch (e: Exception) {
                Log.e("LectureRecorderManager", "Error resuming recorder", e)
            }
        }
    }

    fun addBookmark(label: String) {
        val currentSec = _durationSeconds.value
        val cleanLabel = label.ifBlank { "Bookmark @ ${formatSeconds(currentSec)}" }
        _bookmarks.value = _bookmarks.value + LectureBookmark(currentSec, cleanLabel)
    }

    fun stopRecording(context: Context): CompletedRecording? {
        if (_recordingState.value == RecordingState.IDLE) return null

        val finalDuration = _durationSeconds.value
        val finalBookmarks = _bookmarks.value
        val file = currentOutputFile

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("LectureRecorderManager", "Error stopping recorder", e)
        }

        cleanup()
        LectureRecorderService.stopService(context)

        return if (file != null && file.exists() && file.length() > 0) {
            CompletedRecording(
                title = activeTitle,
                subject = activeSubject,
                coachingSessionName = activeCoachingSession,
                filePath = file.absolutePath,
                durationSeconds = finalDuration,
                fileSizeBytes = file.length(),
                bookmarks = finalBookmarks,
                notes = activeNotes
            )
        } else {
            null
        }
    }

    fun cancelRecording(context: Context) {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        currentOutputFile?.delete()
        cleanup()
        LectureRecorderService.stopService(context)
    }

    private fun startTickers() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (_recordingState.value == RecordingState.RECORDING) {
                    _durationSeconds.value += 1L
                }
            }
        }

        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive) {
                delay(120L)
                if (_recordingState.value == RecordingState.RECORDING) {
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    val audioSettings = appContext?.let { AudioRecordingSettings.getInstance(it) }
                    val gainMultiplier = audioSettings?.distanceGain?.value?.multiplier ?: 1.0f
                    val humFilter = audioSettings?.isAcHumFilterEnabled?.value ?: true

                    var normalized = (maxAmp.toFloat() / 25000f)
                    if (humFilter) {
                        normalized = if (normalized < 0.045f) 0.015f else (normalized - 0.02f) * 1.1f
                    }
                    val boosted = (normalized * gainMultiplier).coerceIn(0.04f, 1.0f)
                    _currentAmplitude.value = boosted
                    val currentList = _amplitudeHistory.value
                    _amplitudeHistory.value = (currentList.drop(1) + boosted)
                } else if (_recordingState.value == RecordingState.PAUSED) {
                    _currentAmplitude.value = 0.05f
                }
            }
        }
    }

    private fun cleanup() {
        tickerJob?.cancel()
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _recordingState.value = RecordingState.IDLE
        _currentAmplitude.value = 0f
    }

    companion object {
        val instance: LectureRecorderManager by lazy { LectureRecorderManager() }

        fun formatSeconds(seconds: Long): String {
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return if (hrs > 0) {
                String.format("%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format("%02d:%02d", mins, secs)
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toFloat() / (1024 * 1024))
                bytes >= 1024 -> String.format("%.1f KB", bytes.toFloat() / 1024)
                else -> "$bytes B"
            }
        }
    }
}
