package com.example.core.sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.alarm.SmartAlarmScheduler
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepDetectorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("SleepDetectorReceiver", "Action received: $action")

        when (action) {
            Intent.ACTION_SCREEN_OFF -> {
                recordScreenOff(context)
            }
            Intent.ACTION_SCREEN_ON -> {
                recordScreenOn(context)
            }
            ACTION_FORCE_SLEEP_LOCK -> {
                executeSleepLock(context, isAuto = false)
            }
        }
    }

    private fun recordScreenOff(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SCREEN_OFF, System.currentTimeMillis()).apply()
    }

    private fun recordScreenOn(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastOff = prefs.getLong(KEY_LAST_SCREEN_OFF, 0L)
        if (lastOff > 0L) {
            val offDurationMin = (System.currentTimeMillis() - lastOff) / (60 * 1000)
            if (offDurationMin >= 20) {
                // If screen was off for >= 20 mins, check if sleep mode should be confirmed
                executeSleepLock(context, isAuto = true)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "enforcer_sleep_prefs"
        const val KEY_LAST_SCREEN_OFF = "key_last_screen_off"
        const val ACTION_FORCE_SLEEP_LOCK = "com.example.enforcer.FORCE_SLEEP_LOCK"
        const val CHANNEL_ID = "enforcer_sleep_channel"
        const val NOTIF_ID = 7070

        fun executeSleepLock(context: Context, isAuto: Boolean): Long {
            val safetyManager = com.example.core.alarm.AlarmSafetyManager.getInstance(context)
            if (!safetyManager.isAlarmMasterEnabled.value) {
                Log.w("SleepDetector", "Sleep lock skipped: Alarm Master Switch is disabled")
                return 0L
            }

            val battery = BatteryGuardReceiver.checkBattery(context)
            if (battery.percent < 30 && !battery.isCharging) {
                Log.w("SleepDetector", "Sleep lock delayed: battery < 30% and not charging")
            }

            val scheduler = SmartAlarmScheduler(context)
            val wakeTime = scheduler.scheduleSixHourWakeup()
            val sleepTime = System.currentTimeMillis()

            // Update Database
            val db = AppDatabase.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                val current = db.sleepStateDao().getSleepStateDirect()
                if (current != null) {
                    db.sleepStateDao().updateState(
                        current.copy(
                            sleepRecordedTime = sleepTime,
                            scheduledWakeTime = wakeTime,
                            isAlarmRinging = false
                        )
                    )
                }
            }

            showSleepNotification(context, wakeTime, isAuto)
            return wakeTime
        }

        private fun showSleepNotification(context: Context, wakeTime: Long, isAuto: Boolean) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Enforcer Sleep Protocol",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                nm.createNotificationChannel(channel)
            }

            val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val wakeFormatted = format.format(Date(wakeTime))

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val mode = if (isAuto) "Auto-Detected (>20m Screen-Off)" else "Manual Sleep Lock"
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("SLEEP PROTOCOL LOCKED (6h REST)")
                .setContentText("Wake alarm scheduled for $wakeFormatted ($mode)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Sleep cycle timestamp locked. Exact 6-hour wake alarm will trigger at $wakeFormatted with max volume and torch strobe.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(NOTIF_ID, notification)
        }
    }
}
