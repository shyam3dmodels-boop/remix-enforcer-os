package com.example.core.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ui.screens.alarm.WakeChallengeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RelentlessAlarmService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var torchStrobe: TorchStrobeController? = null
    private var toneJob: Job? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        torchStrobe = TorchStrobeController(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ALARM) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val safetyManager = AlarmSafetyManager.getInstance(this)
        val (suppress, reason) = safetyManager.shouldSuppressAlarm()
        if (suppress) {
            Log.i("RelentlessAlarmService", "Alarm suppressed on start: $reason")
            safetyManager.showSuppressedNotification(reason)
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmReason = intent?.getStringExtra(AlarmTriggerReceiver.EXTRA_REASON) ?: "Wakeup Challenge"
        startRelentlessAlarm(alarmReason)
        return START_STICKY
    }

    private fun startRelentlessAlarm(reason: String) {
        val safetyManager = AlarmSafetyManager.getInstance(this)
        safetyManager.setAlarmRinging(true)

        val notification = buildAlarmNotification(reason)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val isVibrateOnly = safetyManager.isVibrateOnly.value

        if (!isVibrateOnly) {
            // Force maximum volume on alarm stream
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.let { am ->
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, AudioManager.FLAG_PLAY_SOUND)
                }
            } catch (e: Exception) {
                Log.e("RelentlessAlarmService", "Failed to force volume", e)
            }

            // Camera strobe
            torchStrobe?.startStrobe(serviceScope)

            // Rotating alarm tones
            startRotatingTones()
        }

        // Continuous vibration
        try {
            val pattern = longArrayOf(0, 400, 150, 400, 150, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("RelentlessAlarmService", "Vibration failed", e)
        }
    }

    private fun startRotatingTones() {
        toneJob?.cancel()
        toneJob = serviceScope.launch {
            val tones = listOf(
                ToneGenerator.TONE_CDMA_HIGH_L,
                ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE,
                ToneGenerator.TONE_PROP_BEEP2,
                ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
            )
            var toneIndex = 0

            while (isActive) {
                try {
                    val currentTone = tones[toneIndex % tones.size]
                    val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    tg.startTone(currentTone, 4000)
                    delay(4200L)
                    tg.release()
                } catch (e: Exception) {
                    Log.e("RelentlessAlarmService", "Tone playback error", e)
                    delay(1000L)
                }
                toneIndex++
            }
        }
    }

    private fun stopAlarm() {
        toneJob?.cancel()
        torchStrobe?.stopStrobe()
        vibrator?.cancel()
        AlarmSafetyManager.getInstance(this).setAlarmRinging(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Enforcer Wake Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Relentless alarm for scheduled routine waking"
                setSound(null, null)
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildAlarmNotification(reason: String): Notification {
        val fullScreenIntent = Intent(this, WakeChallengeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RelentlessAlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ENFORCER OS: RELENTLESS ALARM")
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "🚨 SILENCE NOW (IN CLASS)", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_ALARM = "com.example.enforcer.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.enforcer.STOP_ALARM"
        const val CHANNEL_ID = "enforcer_alarm_channel"
        const val NOTIFICATION_ID = 4040
    }
}
