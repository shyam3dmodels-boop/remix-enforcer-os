package com.example.core.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central safety coordinator for Enforcer OS alarms.
 * Provides unconditional alarm suppression in classes/meetings,
 * emergency kill switches, scheduled mute windows, and timetable protection.
 */
class AlarmSafetyManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Master Switch: when disabled, no alarm can ever sound
    private val _isAlarmMasterEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MASTER_ENABLED, true)
    )
    val isAlarmMasterEnabled: StateFlow<Boolean> = _isAlarmMasterEnabled.asStateFlow()

    // Class Shield: unconditionally suppresses alarm sound/strobe
    private val _isClassShieldActive = MutableStateFlow(
        prefs.getBoolean(KEY_CLASS_SHIELD, false)
    )
    val isClassShieldActive: StateFlow<Boolean> = _isClassShieldActive.asStateFlow()

    // Timed Mute: e.g. Mute for next 1h, 2h, 4h during a lecture
    private val _silenceUntilMillis = MutableStateFlow(
        prefs.getLong(KEY_SILENCE_UNTIL, 0L)
    )
    val silenceUntilMillis: StateFlow<Long> = _silenceUntilMillis.asStateFlow()

    // Auto-suppress if current time is within scheduled coaching timetable class
    private val _autoCoachingSuppress = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_COACHING_SUPPRESS, true)
    )
    val autoCoachingSuppress: StateFlow<Boolean> = _autoCoachingSuppress.asStateFlow()

    // Vibrate-only mode (silent stealth alarm instead of 100% blaring siren)
    private val _isVibrateOnly = MutableStateFlow(
        prefs.getBoolean(KEY_VIBRATE_ONLY, false)
    )
    val isVibrateOnly: StateFlow<Boolean> = _isVibrateOnly.asStateFlow()

    // Track active scheduled alarm metadata
    private val _scheduledAlarmTime = MutableStateFlow(
        prefs.getLong(KEY_SCHEDULED_TIME, 0L)
    )
    val scheduledAlarmTime: StateFlow<Long> = _scheduledAlarmTime.asStateFlow()

    private val _scheduledAlarmReason = MutableStateFlow(
        prefs.getString(KEY_SCHEDULED_REASON, "") ?: ""
    )
    val scheduledAlarmReason: StateFlow<String> = _scheduledAlarmReason.asStateFlow()

    // Track whether alarm is actively ringing right now
    private val _isAlarmRingingNow = MutableStateFlow(
        prefs.getBoolean(KEY_IS_RINGING, false)
    )
    val isAlarmRingingNow: StateFlow<Boolean> = _isAlarmRingingNow.asStateFlow()

    fun setAlarmMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _isAlarmMasterEnabled.value = enabled
        if (!enabled) {
            // Cancel active schedules when master switch turned off
            SmartAlarmScheduler(context).cancelAlarm()
            clearAlarmScheduled()
        }
    }

    fun setClassShieldActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_CLASS_SHIELD, active).apply()
        _isClassShieldActive.value = active
        if (active) {
            // If alarm is currently ringing, kill it immediately!
            emergencyKillAllAlarms()
        }
    }

    fun silenceAlarmsForDuration(hours: Float) {
        val until = System.currentTimeMillis() + (hours * 3600 * 1000L).toLong()
        prefs.edit().putLong(KEY_SILENCE_UNTIL, until).apply()
        _silenceUntilMillis.value = until
        emergencyKillAllAlarms()
    }

    fun clearTimedSilence() {
        prefs.edit().putLong(KEY_SILENCE_UNTIL, 0L).apply()
        _silenceUntilMillis.value = 0L
    }

    fun setAutoCoachingSuppress(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COACHING_SUPPRESS, enabled).apply()
        _autoCoachingSuppress.value = enabled
    }

    fun setVibrateOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ONLY, enabled).apply()
        _isVibrateOnly.value = enabled
    }

    fun recordAlarmScheduled(triggerTime: Long, reason: String) {
        prefs.edit()
            .putLong(KEY_SCHEDULED_TIME, triggerTime)
            .putString(KEY_SCHEDULED_REASON, reason)
            .apply()
        _scheduledAlarmTime.value = triggerTime
        _scheduledAlarmReason.value = reason
    }

    fun clearAlarmScheduled() {
        prefs.edit()
            .putLong(KEY_SCHEDULED_TIME, 0L)
            .putString(KEY_SCHEDULED_REASON, "")
            .putBoolean(KEY_IS_RINGING, false)
            .apply()
        _scheduledAlarmTime.value = 0L
        _scheduledAlarmReason.value = ""
        _isAlarmRingingNow.value = false
    }

    fun setAlarmRinging(ringing: Boolean) {
        prefs.edit().putBoolean(KEY_IS_RINGING, ringing).apply()
        _isAlarmRingingNow.value = ringing
    }

    /**
     * Determines whether an incoming alarm should be suppressed under any condition.
     * Returns Pair(shouldSuppress: Boolean, reason: String).
     */
    fun shouldSuppressAlarm(): Pair<Boolean, String> {
        // 1. Master switch check
        if (!_isAlarmMasterEnabled.value) {
            return Pair(true, "Alarm Master Switch is disabled")
        }

        // 2. Class Shield active check
        if (_isClassShieldActive.value) {
            return Pair(true, "Class Shield is Active (Safe Silent Classroom Mode)")
        }

        // 3. Timed silence window check
        val silenceUntil = _silenceUntilMillis.value
        val now = System.currentTimeMillis()
        if (silenceUntil > now) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return Pair(true, "Muted for class until ${sdf.format(Date(silenceUntil))}")
        }

        // 4. Coaching Timetable check (if enabled)
        if (_autoCoachingSuppress.value) {
            try {
                val db = AppDatabase.getInstance(context)
                val activeSchedules = db.coachingDao().getAllDirect()
                val timetableManager = com.example.core.coaching.CoachingTimetableManager(context)
                val (inClass, session) = timetableManager.checkAndApplyClassSilentMode(activeSchedules)
                if (inClass && session != null) {
                    return Pair(true, "Auto-Suppressed: Currently in class [${session.title}]")
                }
            } catch (e: Exception) {
                Log.w("AlarmSafetyManager", "Error checking coaching schedule", e)
            }
        }

        return Pair(false, "Permitted to sound")
    }

    /**
     * Unconditional Kill Switch:
     * Disables and silences the alarm immediately under ANY condition.
     * 1. Stops the RelentlessAlarmService (tones, strobe, vibration, notification)
     * 2. Dismisses WakeChallengeActivity
     * 3. Cancels pending alarms in AlarmManager
     * 4. Resets database and shared preference states
     */
    fun emergencyKillAllAlarms() {
        Log.i("AlarmSafetyManager", "EMERGENCY KILL SWITCH TRIGGERED: Silencing all alarms.")

        // 1. Stop Foreground Service
        val stopIntent = Intent(context, RelentlessAlarmService::class.java).apply {
            action = RelentlessAlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        // 2. Broadcast Dismiss to WakeChallengeActivity
        val dismissIntent = Intent(ACTION_DISMISS_WAKE_CHALLENGE)
        context.sendBroadcast(dismissIntent)

        // 3. Cancel AlarmManager pending intent
        SmartAlarmScheduler(context).cancelAlarm()

        // 4. Update states
        setAlarmRinging(false)
        clearAlarmScheduled()

        // 5. Update Database SleepState
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val current = db.sleepStateDao().getSleepStateDirect()
                if (current != null) {
                    db.sleepStateDao().updateState(
                        current.copy(
                            scheduledWakeTime = 0L,
                            isAlarmRinging = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("AlarmSafetyManager", "Failed to update DB on emergency kill", e)
            }
        }
    }

    /**
     * Shows a discreet notification informing the user that their alarm was
     * safely suppressed to prevent disruption in class.
     */
    fun showSuppressedNotification(reason: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SUPPRESSED_CHANNEL_ID,
                "Enforcer Class Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when alarms are suppressed to avoid class disturbance"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, SUPPRESSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🛡️ Alarm Suppressed (Class Protected)")
            .setContentText(reason)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("An alarm was scheduled to ring now, but was silenced by Enforcer Class Shield:\n$reason")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(SUPPRESSED_NOTIF_ID, notif)
    }

    companion object {
        const val PREFS_NAME = "enforcer_alarm_safety_prefs"
        const val KEY_MASTER_ENABLED = "key_alarm_master_enabled"
        const val KEY_CLASS_SHIELD = "key_class_shield_active"
        const val KEY_SILENCE_UNTIL = "key_silence_until_millis"
        const val KEY_AUTO_COACHING_SUPPRESS = "key_auto_coaching_suppress"
        const val KEY_VIBRATE_ONLY = "key_vibrate_only"
        const val KEY_SCHEDULED_TIME = "key_scheduled_time"
        const val KEY_SCHEDULED_REASON = "key_scheduled_reason"
        const val KEY_IS_RINGING = "key_is_ringing"

        const val ACTION_DISMISS_WAKE_CHALLENGE = "com.example.enforcer.ACTION_DISMISS_WAKE_CHALLENGE"
        const val SUPPRESSED_CHANNEL_ID = "enforcer_suppressed_alarm_channel"
        const val SUPPRESSED_NOTIF_ID = 5055

        @Volatile
        private var INSTANCE: AlarmSafetyManager? = null

        fun getInstance(context: Context): AlarmSafetyManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AlarmSafetyManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
