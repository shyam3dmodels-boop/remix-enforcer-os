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
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data representing a single completed audio chunk from a rolling recording session.
 */
data class CompletedChunk(
    val chunkIndex: Int,
    val title: String,
    val subject: String,
    val coachingSessionName: String,
    val filePath: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val bookmarks: List<LectureBookmark>,
    val notes: String,
    val isFinalChunk: Boolean
)

/**
 * AudioChunkRecorder manages continuous background lecture recording segmented into
 * rolling 15-minute file chunks (or user-configured duration).
 *
 * This protects against file corruption, prevents massive unmanageable audio files,
 * and ensures that if a session ends abruptly, previously completed chunks are already
 * safely finalized on disk and committed to the database.
 */
class AudioChunkRecorder private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    private var tickerJob: Job? = null
    private var amplitudeJob: Job? = null

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // Total session elapsed seconds across all chunks
    private val _totalElapsedSeconds = MutableStateFlow(0L)
    val totalElapsedSeconds: StateFlow<Long> = _totalElapsedSeconds.asStateFlow()

    // Current active chunk elapsed seconds
    private val _currentChunkElapsedSeconds = MutableStateFlow(0L)
    val currentChunkElapsedSeconds: StateFlow<Long> = _currentChunkElapsedSeconds.asStateFlow()

    // Configured chunk limit in seconds (default 15 minutes = 900 seconds)
    private val _chunkLimitSeconds = MutableStateFlow(15 * 60L)
    val chunkLimitSeconds: StateFlow<Long> = _chunkLimitSeconds.asStateFlow()

    // Current chunk index (Part 1, Part 2, etc.)
    private val _currentChunkIndex = MutableStateFlow(1)
    val currentChunkIndex: StateFlow<Int> = _currentChunkIndex.asStateFlow()

    // Live acoustic amplitude normalized between 0.0 and 1.0
    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    // Rolling amplitude history for live waveform UI
    private val _amplitudeHistory = MutableStateFlow<List<Float>>(List(30) { 0.05f })
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    // Active bookmarks in current session
    private val _sessionBookmarks = MutableStateFlow<List<LectureBookmark>>(emptyList())
    val sessionBookmarks: StateFlow<List<LectureBookmark>> = _sessionBookmarks.asStateFlow()

    // Bookmarks stamped specifically inside current chunk
    private val _chunkBookmarks = MutableStateFlow<List<LectureBookmark>>(emptyList())
    val chunkBookmarks: StateFlow<List<LectureBookmark>> = _chunkBookmarks.asStateFlow()

    // Completed chunks in current recording session
    private val _completedChunks = MutableStateFlow<List<CompletedChunk>>(emptyList())
    val completedChunks: StateFlow<List<CompletedChunk>> = _completedChunks.asStateFlow()

    private var baseTitle: String = ""
    private var subject: String = ""
    private var coachingSessionName: String = ""
    private var initialNotes: String = ""
    private var appContext: Context? = null
    private var onChunkFinalizedCallback: (suspend (CompletedChunk) -> Unit)? = null
    private var sessionStartTimeMs: Long = 0L

    fun startRecording(
        context: Context,
        title: String,
        subject: String,
        coachingSessionName: String = "",
        initialNotes: String = "",
        chunkDurationMinutes: Int = 15,
        onChunkFinalized: (suspend (CompletedChunk) -> Unit)? = null
    ): Boolean {
        val seconds = if (chunkDurationMinutes <= 0) INFINITE_CHUNK_SECONDS else chunkDurationMinutes.toLong() * 60L
        return startRecordingWithSeconds(
            context = context,
            title = title,
            subject = subject,
            coachingSessionName = coachingSessionName,
            initialNotes = initialNotes,
            chunkDurationSeconds = seconds,
            onChunkFinalized = onChunkFinalized
        )
    }

    fun startRecordingWithSeconds(
        context: Context,
        title: String,
        subject: String,
        coachingSessionName: String = "",
        initialNotes: String = "",
        chunkDurationSeconds: Long = 15 * 60L,
        onChunkFinalized: (suspend (CompletedChunk) -> Unit)? = null
    ): Boolean {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Recording already active or paused.")
            return false
        }

        appContext = context.applicationContext
        this.baseTitle = title.ifBlank { "Lecture ${System.currentTimeMillis() % 10000}" }
        this.subject = subject.ifBlank { "General" }
        this.coachingSessionName = coachingSessionName
        this.initialNotes = initialNotes
        this.onChunkFinalizedCallback = onChunkFinalized

        // 10 seconds minimum, or INFINITE_CHUNK_SECONDS (Long.MAX_VALUE)
        val limitSec = if (chunkDurationSeconds <= 0L || chunkDurationSeconds >= INFINITE_CHUNK_SECONDS) {
            INFINITE_CHUNK_SECONDS
        } else {
            chunkDurationSeconds.coerceAtLeast(10L)
        }
        _chunkLimitSeconds.value = limitSec

        _totalElapsedSeconds.value = 0L
        _currentChunkElapsedSeconds.value = 0L
        _currentChunkIndex.value = 1
        _sessionBookmarks.value = emptyList()
        _chunkBookmarks.value = emptyList()
        _completedChunks.value = emptyList()
        sessionStartTimeMs = System.currentTimeMillis()

        return startNewChunk(chunkIndex = 1)
    }

    private fun startNewChunk(chunkIndex: Int): Boolean {
        val context = appContext ?: return false

        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) {
            lecturesDir.mkdirs()
        }

        val cleanTitle = baseTitle.replace("[^a-zA-Z0-9_]".toRegex(), "_")
        val outputFile = File(
            lecturesDir,
            "${cleanTitle}_${sessionStartTimeMs}_chunk_${chunkIndex}.m4a"
        )
        currentOutputFile = outputFile

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
                setAudioChannels(1) // 32kbps mono AAC (.m4a)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _recordingState.value = RecordingState.RECORDING
            _currentChunkIndex.value = chunkIndex
            _currentChunkElapsedSeconds.value = 0L
            _chunkBookmarks.value = emptyList()

            startTimers()
            Log.i(TAG, "Started rolling audio chunk #$chunkIndex at ${outputFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio chunk #$chunkIndex", e)
            releaseRecorder()
            false
        }
    }

    private fun startTimers() {
        tickerJob?.cancel()
        amplitudeJob?.cancel()

        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (_recordingState.value == RecordingState.RECORDING) {
                    _totalElapsedSeconds.value += 1
                    val newChunkSec = _currentChunkElapsedSeconds.value + 1
                    _currentChunkElapsedSeconds.value = newChunkSec

                    // Check if chunk boundary is reached (unless infinite)
                    val limit = _chunkLimitSeconds.value
                    if (limit < INFINITE_CHUNK_SECONDS && newChunkSec >= limit) {
                        Log.i(TAG, "Chunk boundary ($limit sec) reached. Performing seamless rollover.")
                        rolloverToNextChunk()
                    }
                }
            }
        }

        amplitudeJob = scope.launch {
            while (isActive) {
                delay(100L)
                if (_recordingState.value == RecordingState.RECORDING) {
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    val audioSettings = appContext?.let { AudioRecordingSettings.getInstance(it) }
                    val gainMultiplier = audioSettings?.distanceGain?.value?.multiplier ?: 1.0f
                    val humFilter = audioSettings?.isAcHumFilterEnabled?.value ?: true

                    var normalized = (maxAmp / 32767f)
                    if (humFilter) {
                        // High-pass filter gate for low frequency HVAC / fan humming
                        normalized = if (normalized < 0.045f) 0.015f else (normalized - 0.02f) * 1.1f
                    }
                    val boosted = (normalized * gainMultiplier).coerceIn(0.02f, 1f)
                    _currentAmplitude.value = boosted

                    val history = _amplitudeHistory.value.toMutableList()
                    if (history.size >= 30) history.removeAt(0)
                    history.add(boosted)
                    _amplitudeHistory.value = history
                }
            }
        }
    }

    /**
     * Seamlessly finalizes the current chunk and starts the next chunk without stopping the session.
     */
    private fun rolloverToNextChunk() {
        val completedChunk = finalizeCurrentChunk(isFinal = false)
        val nextIndex = _currentChunkIndex.value + 1

        if (completedChunk != null) {
            val list = _completedChunks.value.toMutableList()
            list.add(completedChunk)
            _completedChunks.value = list

            // Emit to callback so database can save it immediately
            scope.launch(Dispatchers.IO) {
                try {
                    onChunkFinalizedCallback?.invoke(completedChunk)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onChunkFinalized callback", e)
                }
                triggerTelegramUpload(completedChunk)
            }
        }

        // Immediately start next chunk
        startNewChunk(chunkIndex = nextIndex)
    }

    fun pauseRecording() {
        if (_recordingState.value == RecordingState.RECORDING) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.pause()
                    _recordingState.value = RecordingState.PAUSED
                    Log.i(TAG, "Recording paused.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause recorder", e)
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value == RecordingState.PAUSED) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder?.resume()
                    _recordingState.value = RecordingState.RECORDING
                    Log.i(TAG, "Recording resumed.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume recorder", e)
            }
        }
    }

    fun addBookmark(label: String): LectureBookmark {
        val totalSec = _totalElapsedSeconds.value
        val chunkSec = _currentChunkElapsedSeconds.value
        val chunkIdx = _currentChunkIndex.value
        val tag = if (label.isNotBlank()) label else "Marker"
        val fullLabel = "[Pt.$chunkIdx @ ${formatSeconds(chunkSec)}] $tag"

        val bookmark = LectureBookmark(timeSeconds = totalSec, label = fullLabel)

        val sessionList = _sessionBookmarks.value.toMutableList()
        sessionList.add(bookmark)
        _sessionBookmarks.value = sessionList

        val chunkList = _chunkBookmarks.value.toMutableList()
        chunkList.add(LectureBookmark(timeSeconds = chunkSec, label = tag))
        _chunkBookmarks.value = chunkList

        Log.i(TAG, "Bookmark added: $fullLabel")
        return bookmark
    }

    /**
     * Stops the active session, finalizes the final chunk, and returns all completed chunks.
     */
    fun stopRecording(): List<CompletedChunk> {
        if (_recordingState.value == RecordingState.IDLE) {
            return _completedChunks.value
        }

        val lastChunk = finalizeCurrentChunk(isFinal = true)
        val all = _completedChunks.value.toMutableList()
        if (lastChunk != null) {
            all.add(lastChunk)
            _completedChunks.value = all
            scope.launch(Dispatchers.IO) {
                try {
                    onChunkFinalizedCallback?.invoke(lastChunk)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving last chunk callback", e)
                }
                triggerTelegramUpload(lastChunk)
            }
        }

        releaseRecorder()
        return all
    }

    private fun triggerTelegramUpload(chunk: CompletedChunk) {
        val context = appContext ?: return
        val config = TelegramConfigManager.getInstance(context)
        if (config.isCloudPipeEnabled()) {
            TelegramUploadWorker.enqueue(
                context = context,
                filePath = chunk.filePath,
                lectureTitle = chunk.title,
                subject = chunk.subject,
                chunkIndex = chunk.chunkIndex,
                durationSec = chunk.durationSeconds
            )
            Log.i(TAG, "Cloud Pipe: Enqueued TelegramUploadWorker for chunk #${chunk.chunkIndex}")
        }
    }

    fun cancelRecording() {
        tickerJob?.cancel()
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        releaseRecorder()

        // Remove the current active file if unneeded
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        _recordingState.value = RecordingState.IDLE
        _completedChunks.value = emptyList()
        Log.i(TAG, "Recording cancelled and active chunk discarded.")
    }

    private fun finalizeCurrentChunk(isFinal: Boolean): CompletedChunk? {
        val file = currentOutputFile ?: return null
        val duration = _currentChunkElapsedSeconds.value
        val chunkIndex = _currentChunkIndex.value

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder on chunk #$chunkIndex", e)
        }

        // Avoid creating corrupt 0-byte or 0-second file records
        val size = if (file.exists()) file.length() else 0L
        if (duration < 2 && size < 1000) {
            try { file.delete() } catch (_: Exception) {}
            return null
        }

        val partTitle = if (chunkIndex == 1 && isFinal && _completedChunks.value.isEmpty()) {
            baseTitle
        } else {
            "$baseTitle (Part $chunkIndex)"
        }

        return CompletedChunk(
            chunkIndex = chunkIndex,
            title = partTitle,
            subject = subject,
            coachingSessionName = coachingSessionName,
            filePath = file.absolutePath,
            durationSeconds = duration,
            fileSizeBytes = size,
            bookmarks = _chunkBookmarks.value,
            notes = initialNotes,
            isFinalChunk = isFinal
        )
    }

    private fun releaseRecorder() {
        tickerJob?.cancel()
        amplitudeJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        currentOutputFile = null
        _recordingState.value = RecordingState.IDLE
        _currentAmplitude.value = 0f
    }

    companion object {
        private const val TAG = "AudioChunkRecorder"
        const val INFINITE_CHUNK_SECONDS = Long.MAX_VALUE
        val instance: AudioChunkRecorder by lazy { AudioChunkRecorder() }

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
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024f * 1024f))
                bytes >= 1024 -> String.format("%.0f KB", bytes / 1024f)
                else -> "$bytes B"
            }
        }
    }
}
