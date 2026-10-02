package com.example.core.recorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
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

class LecturePlayerManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentPlayingId = MutableStateFlow<Long?>(null)
    val currentPlayingId: StateFlow<Long?> = _currentPlayingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun play(lectureId: Long, filePath: String) {
        if (_currentPlayingId.value == lectureId && mediaPlayer != null) {
            resume()
            return
        }

        stop()

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            // For demo/sample lecture or missing file, simulate playback
            simulateDemoPlayback(lectureId)
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    stopProgressTracking()
                }
            }
            mediaPlayer = player
            _currentPlayingId.value = lectureId
            _durationMs.value = player.duration
            applySpeed(_playbackSpeed.value)
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        } catch (e: Exception) {
            Log.e("LecturePlayerManager", "Failed to start media player", e)
            simulateDemoPlayback(lectureId)
        }
    }

    private fun simulateDemoPlayback(lectureId: Long) {
        _currentPlayingId.value = lectureId
        _durationMs.value = 840 * 1000 // 14 mins
        _currentPositionMs.value = 0
        _isPlaying.value = true

        startProgressTracking()
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
        _isPlaying.value = true
        startProgressTracking()
    }

    fun togglePlayPause(lectureId: Long, filePath: String) {
        if (_currentPlayingId.value == lectureId) {
            if (_isPlaying.value) pause() else resume()
        } else {
            play(lectureId, filePath)
        }
    }

    fun seekTo(positionMs: Int) {
        val clamped = positionMs.coerceIn(0, _durationMs.value.coerceAtLeast(1))
        _currentPositionMs.value = clamped
        try {
            mediaPlayer?.seekTo(clamped)
        } catch (_: Exception) {}
    }

    fun skipForward(seconds: Int = 10) {
        val newPos = _currentPositionMs.value + (seconds * 1000)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val newPos = _currentPositionMs.value - (seconds * 1000)
        seekTo(newPos)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            try {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            } catch (e: Exception) {
                Log.e("LecturePlayerManager", "Failed to set playback speed", e)
            }
        }
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentPlayingId.value = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
        _durationMs.value = 0
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(200L)
                if (_isPlaying.value) {
                    val pos = try {
                        mediaPlayer?.currentPosition ?: (_currentPositionMs.value + (200 * _playbackSpeed.value).toInt())
                    } catch (_: Exception) {
                        _currentPositionMs.value + 200
                    }
                    _currentPositionMs.value = pos
                    if (_durationMs.value > 0 && pos >= _durationMs.value) {
                        _isPlaying.value = false
                        _currentPositionMs.value = 0
                        stop()
                        break
                    }
                }
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        val instance: LecturePlayerManager by lazy { LecturePlayerManager() }
    }
}
