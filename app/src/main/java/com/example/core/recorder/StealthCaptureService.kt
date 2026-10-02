package com.example.core.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MainApplication
import com.example.R
import com.example.data.local.entity.LectureEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Disguised notification profiles for stealth lecture capture.
 * Designed so peers, friends, or passersby glancing at the student's phone
 * only see routine background OS or service telemetry, completely disguising
 * the ongoing audio lecture recording and revision capture.
 */
enum class DisguiseProfile(
    val id: String,
    val displayName: String,
    val notificationTitle: String,
    val notificationText: String,
    val subText: String,
    val disguisedPauseText: String,
    val disguisedResumeText: String,
    val disguisedBookmarkText: String,
    val disguisedStopText: String
) {
    GOOGLE_PLAY_SERVICES(
        id = "play_services",
        displayName = "Google Play Services (Account Sync)",
        notificationTitle = "Google Play services",
        notificationText = "Syncing background account data…",
        subText = "Sync Active",
        disguisedPauseText = "Pause Sync",
        disguisedResumeText = "Resume Sync",
        disguisedBookmarkText = "Flag Event",
        disguisedStopText = "Finish Sync"
    ),
    BATTERY_OPTIMIZER(
        id = "battery_care",
        displayName = "Battery Care & Power Guard",
        notificationTitle = "Battery Care Service",
        notificationText = "Analyzing background power consumption…",
        subText = "Power Management",
        disguisedPauseText = "Pause Scan",
        disguisedResumeText = "Resume Scan",
        disguisedBookmarkText = "Mark Peak",
        disguisedStopText = "Complete"
    ),
    ANDROID_SYSTEM(
        id = "android_system",
        displayName = "Android System (Maintenance)",
        notificationTitle = "Android System",
        notificationText = "Indexing storage and cached app resources…",
        subText = "Maintenance",
        disguisedPauseText = "Pause Index",
        disguisedResumeText = "Resume Index",
        disguisedBookmarkText = "Log Tag",
        disguisedStopText = "Finish"
    ),
    NETWORK_DIAGNOSTIC(
        id = "network_diagnostic",
        displayName = "Carrier Network Telemetry",
        notificationTitle = "Carrier Network Service",
        notificationText = "Monitoring cellular signal stability…",
        subText = "Network Health",
        disguisedPauseText = "Pause Probe",
        disguisedResumeText = "Resume Probe",
        disguisedBookmarkText = "Drop Ping",
        disguisedStopText = "End Check"
    ),
    AUDIO_CALIBRATION(
        id = "audio_calibration",
        displayName = "Dolby Audio Calibration",
        notificationTitle = "Sound System Calibration",
        notificationText = "Acoustic latency room calibration active…",
        subText = "Sound EQ",
        disguisedPauseText = "Pause Tune",
        disguisedResumeText = "Resume Tune",
        disguisedBookmarkText = "Save Tone",
        disguisedStopText = "Finish Tune"
    );

    companion object {
        fun fromId(id: String): DisguiseProfile {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: GOOGLE_PLAY_SERVICES
        }
    }
}

/**
 * Foreground Service that captures audio lectures in the background while
 * displaying a disguised system notification and executing rolling 15-minute file handling.
 */
class StealthCaptureService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var activeProfile: DisguiseProfile = DisguiseProfile.GOOGLE_PLAY_SERVICES
    private var lectureTitle: String = "Lecture"
    private var lectureSubject: String = "Study"
    private var coachingSession: String = ""
    private var chunkSeconds: Long = 15 * 60L
    private var isPaused: Boolean = false
    private var isEmergencySaving: Boolean = false

    private val emergencyBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED && !isEmergencySaving) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (level >= 0 && scale > 0) {
                    val batteryPercent = (level * 100) / scale
                    if (batteryPercent <= 5 && !isCharging) {
                        isEmergencySaving = true
                        Log.w(TAG, "EMERGENCY BATTERY AUTO-SAVE TRIGGERED: $batteryPercent% battery (<5%). Finalizing active lecture chunk!")
                        triggerEmergencyAutoSave(batteryPercent)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(emergencyBatteryReceiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register emergencyBatteryReceiver", e)
        }
    }

    private fun triggerEmergencyAutoSave(batteryPercent: Int) {
        serviceScope.launch {
            val completedChunks = AudioChunkRecorder.instance.stopRecording()
            for (chunk in completedChunks) {
                saveChunkToDatabase(chunk)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "⚠️ Low Battery ($batteryPercent%): Current lecture chunk sealed & saved safely before shutdown!",
                    Toast.LENGTH_LONG
                ).show()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            _isStealthServiceRunning.value = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_STEALTH
        when (action) {
            ACTION_START_STEALTH -> {
                lectureTitle = intent?.getStringExtra(EXTRA_TITLE) ?: "Coaching Lecture"
                lectureSubject = intent?.getStringExtra(EXTRA_SUBJECT) ?: "Class"
                coachingSession = intent?.getStringExtra(EXTRA_COACHING_SESSION) ?: ""
                chunkSeconds = intent?.getLongExtra(EXTRA_CHUNK_SECONDS, 15 * 60L)
                    ?: (intent?.getIntExtra(EXTRA_CHUNK_MINUTES, 15)?.toLong()?.times(60L) ?: 15 * 60L)
                val profileId = intent?.getStringExtra(EXTRA_DISGUISE_KEY) ?: DisguiseProfile.GOOGLE_PLAY_SERVICES.id
                activeProfile = DisguiseProfile.fromId(profileId)

                _isStealthServiceRunning.value = true
                _activeStealthProfile.value = activeProfile

                // Start chunked recording engine
                val started = AudioChunkRecorder.instance.startRecordingWithSeconds(
                    context = this,
                    title = lectureTitle,
                    subject = lectureSubject,
                    coachingSessionName = coachingSession,
                    chunkDurationSeconds = chunkSeconds,
                    onChunkFinalized = { chunk ->
                        saveChunkToDatabase(chunk)
                        updateDisguisedNotification()
                    }
                )

                if (started) {
                    val notification = buildDisguisedNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    Log.i(TAG, "StealthCaptureService active with disguised profile: ${activeProfile.displayName}")
                } else {
                    Log.e(TAG, "Failed to start AudioChunkRecorder.")
                    stopSelf()
                }
            }

            ACTION_PAUSE -> {
                isPaused = true
                AudioChunkRecorder.instance.pauseRecording()
                updateDisguisedNotification()
            }

            ACTION_RESUME -> {
                isPaused = false
                AudioChunkRecorder.instance.resumeRecording()
                updateDisguisedNotification()
            }

            ACTION_BOOKMARK -> {
                val label = intent?.getStringExtra(EXTRA_BOOKMARK_LABEL) ?: "Key Concept"
                AudioChunkRecorder.instance.addBookmark(label)
            }

            ACTION_STOP_AND_SAVE -> {
                serviceScope.launch {
                    val completedChunks = AudioChunkRecorder.instance.stopRecording()
                    for (chunk in completedChunks) {
                        saveChunkToDatabase(chunk)
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    _isStealthServiceRunning.value = false
                    stopSelf()
                }
            }

            ACTION_CANCEL -> {
                AudioChunkRecorder.instance.cancelRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                _isStealthServiceRunning.value = false
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun saveChunkToDatabase(chunk: CompletedChunk) {
        val repo = (application as? MainApplication)?.repository ?: return
        serviceScope.launch {
            try {
                val entity = LectureEntity(
                    title = chunk.title,
                    subject = chunk.subject,
                    coachingSessionName = chunk.coachingSessionName,
                    filePath = chunk.filePath,
                    durationSeconds = chunk.durationSeconds,
                    fileSizeBytes = chunk.fileSizeBytes,
                    timestamp = System.currentTimeMillis(),
                    bookmarksJson = LectureEntity.encodeBookmarks(chunk.bookmarks),
                    notes = chunk.notes
                )
                val id = repo.saveLecture(entity)
                Log.i(TAG, "Saved chunk #${chunk.chunkIndex} to database with ID: $id (${chunk.title})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist lecture chunk to database", e)
            }
        }
    }

    private fun buildDisguisedNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_RECORDER", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePauseIntent = Intent(this, StealthCaptureService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePausePendingIntent = PendingIntent.getService(
            this,
            102,
            togglePauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseActionText = if (isPaused) "Resume" else "Pause"

        val bookmarkIntent = Intent(this, StealthCaptureService::class.java).apply {
            action = ACTION_BOOKMARK
            putExtra(EXTRA_BOOKMARK_LABEL, "Key Point")
        }
        val bookmarkPendingIntent = PendingIntent.getService(
            this,
            103,
            bookmarkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, StealthCaptureService::class.java).apply {
            action = ACTION_STOP_AND_SAVE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            104,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chunkIdx = AudioChunkRecorder.instance.currentChunkIndex.value
        val statusText = if (isPaused) {
            "Paused • Part #$chunkIdx • Tap to resume"
        } else {
            "Recording Audio • Part #$chunkIdx [$lectureSubject]"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Lecture: $lectureTitle")
            .setContentText(statusText)
            .setSubText("Audio Recorder")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, pauseActionText, togglePausePendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Bookmark", bookmarkPendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Stop & Save", stopPendingIntent)
            .build()
    }

    private fun updateDisguisedNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildDisguisedNotification())
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lecture Audio Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing lecture audio recording notifications and controls"
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(emergencyBatteryReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()
        _isStealthServiceRunning.value = false
        Log.i(TAG, "StealthCaptureService destroyed.")
    }

    companion object {
        private const val TAG = "StealthCaptureService"
        const val CHANNEL_ID = "system_power_monitor"
        private const val NOTIFICATION_ID = 3003

        const val ACTION_START_STEALTH = "com.example.recorder.START_STEALTH"
        const val ACTION_PAUSE = "com.example.recorder.STEALTH_PAUSE"
        const val ACTION_RESUME = "com.example.recorder.STEALTH_RESUME"
        const val ACTION_BOOKMARK = "com.example.recorder.STEALTH_BOOKMARK"
        const val ACTION_STOP_AND_SAVE = "com.example.recorder.STEALTH_STOP"
        const val ACTION_CANCEL = "com.example.recorder.STEALTH_CANCEL"

        const val EXTRA_TITLE = "extra_stealth_title"
        const val EXTRA_SUBJECT = "extra_stealth_subject"
        const val EXTRA_COACHING_SESSION = "extra_stealth_coaching"
        const val EXTRA_DISGUISE_KEY = "extra_stealth_disguise"
        const val EXTRA_CHUNK_MINUTES = "extra_stealth_chunk_mins"
        const val EXTRA_CHUNK_SECONDS = "extra_stealth_chunk_secs"
        const val EXTRA_BOOKMARK_LABEL = "extra_stealth_bm_label"

        private val _isStealthServiceRunning = MutableStateFlow(false)
        val isStealthServiceRunning: StateFlow<Boolean> = _isStealthServiceRunning.asStateFlow()

        private val _activeStealthProfile = MutableStateFlow(DisguiseProfile.GOOGLE_PLAY_SERVICES)
        val activeStealthProfile: StateFlow<DisguiseProfile> = _activeStealthProfile.asStateFlow()

        fun startStealthCapture(
            context: Context,
            title: String,
            subject: String,
            coachingSessionName: String = "",
            disguiseProfile: DisguiseProfile = DisguiseProfile.GOOGLE_PLAY_SERVICES,
            chunkDurationMinutes: Int = 15
        ) {
            val seconds = if (chunkDurationMinutes <= 0) AudioChunkRecorder.INFINITE_CHUNK_SECONDS else chunkDurationMinutes.toLong() * 60L
            startStealthCaptureWithSeconds(
                context = context,
                title = title,
                subject = subject,
                coachingSessionName = coachingSessionName,
                disguiseProfile = disguiseProfile,
                chunkDurationSeconds = seconds
            )
        }

        fun startStealthCaptureWithSeconds(
            context: Context,
            title: String,
            subject: String,
            coachingSessionName: String = "",
            disguiseProfile: DisguiseProfile = DisguiseProfile.GOOGLE_PLAY_SERVICES,
            chunkDurationSeconds: Long = 15 * 60L
        ) {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_START_STEALTH
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_COACHING_SESSION, coachingSessionName)
                putExtra(EXTRA_DISGUISE_KEY, disguiseProfile.id)
                putExtra(EXTRA_CHUNK_SECONDS, chunkDurationSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseStealthCapture(context: Context) {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeStealthCapture(context: Context) {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun addStealthBookmark(context: Context, label: String = "Exam Point") {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_BOOKMARK
                putExtra(EXTRA_BOOKMARK_LABEL, label)
            }
            context.startService(intent)
        }

        fun stopAndSaveStealthCapture(context: Context) {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_STOP_AND_SAVE
            }
            context.startService(intent)
        }

        fun cancelStealthCapture(context: Context) {
            val intent = Intent(context, StealthCaptureService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
