package com.example.core.recorder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * DailySummaryReceiver:
 * Broadcast receiver scheduled at the end of each day (default 9:30 PM) to parse
 * voice-to-task notes and issue the silent notification summary for pending homework/tests.
 */
class DailySummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "DailySummaryReceiver triggered: action=$action")

        val notifier = EndOfDaySummaryNotifier.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                notifier.evaluateAndSendDailySummary(forceNow = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating daily task summary notification", e)
            } finally {
                // Reschedule for the next day
                scheduleDailySummary(context)
            }
        }
    }

    companion object {
        private const val TAG = "DailySummaryReceiver"
        const val ACTION_TRIGGER_DAILY_SUMMARY = "com.example.enforcer.ACTION_TRIGGER_DAILY_SUMMARY"
        private const val SUMMARY_REQUEST_CODE = 8842

        /**
         * Schedules an exact or repeating alarm for the configured end-of-day summary time.
         */
        fun scheduleDailySummary(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val notifier = EndOfDaySummaryNotifier.getInstance(context)

            val intent = Intent(context, DailySummaryReceiver::class.java).apply {
                action = ACTION_TRIGGER_DAILY_SUMMARY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SUMMARY_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val targetHour = notifier.summaryHour.value
            val targetMinute = notifier.summaryMinute.value

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If already passed for today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled end-of-day summary alarm for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule daily summary alarm", e)
            }
        }
    }
}
