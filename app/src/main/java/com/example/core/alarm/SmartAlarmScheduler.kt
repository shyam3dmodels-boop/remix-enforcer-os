package com.example.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class SmartAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val safetyManager = AlarmSafetyManager.getInstance(context)

    fun scheduleExactAlarm(triggerAtMillis: Long, reason: String = "Wakeup Alarm"): Boolean {
        // If master switch disabled, disallow scheduling
        if (!safetyManager.isAlarmMasterEnabled.value) {
            Log.w("SmartAlarmScheduler", "Alarm scheduling blocked: Master switch is OFF")
            return false
        }

        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            action = AlarmTriggerReceiver.ACTION_ENFORCER_ALARM
            putExtra(AlarmTriggerReceiver.EXTRA_REASON, reason)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("SmartAlarmScheduler", "Exact alarm permission not granted, using non-exact fallback")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    safetyManager.recordAlarmScheduled(triggerAtMillis, reason)
                    return true
                }
            }

            val showIntent = Intent(context, AlarmTriggerReceiver::class.java)
            val showPendingIntent = PendingIntent.getActivity(
                context,
                ALARM_CLOCK_REQUEST_CODE,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            safetyManager.recordAlarmScheduled(triggerAtMillis, reason)
            Log.d("SmartAlarmScheduler", "Exact alarm scheduled at $triggerAtMillis ($reason)")
            return true
        } catch (e: Exception) {
            Log.e("SmartAlarmScheduler", "Error scheduling exact alarm", e)
            return false
        }
    }

    /**
     * Schedules an alarm for a specific hour (0-23) and minute (0-59).
     * If the time is in the past for today, it automatically schedules for tomorrow.
     */
    fun scheduleManualAlarm(hourOfDay: Int, minute: Int, reason: String = "Manual Alarm"): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target is before or equal to right now, schedule for the next day
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerTime = target.timeInMillis
        scheduleExactAlarm(triggerTime, reason)
        return triggerTime
    }

    fun schedulePresetAlarm(minutesDelay: Int, reason: String): Long {
        val triggerTime = System.currentTimeMillis() + (minutesDelay * 60 * 1000L)
        scheduleExactAlarm(triggerTime, reason)
        return triggerTime
    }

    fun scheduleQuickTestAlarm(secondsDelay: Int = 10): Boolean {
        val triggerTime = System.currentTimeMillis() + (secondsDelay * 1000L)
        return scheduleExactAlarm(triggerTime, "Enforcer Test Alarm ($secondsDelay s)")
    }

    /**
     * Schedules the automatic 6-hour sleep cycle wakeup alarm.
     * Optional bufferMinutes accounts for sleep onset latency (e.g. 15 mins to fall asleep).
     */
    fun scheduleSixHourWakeup(bufferMinutes: Int = 0): Long {
        val wakeTime = calculate6HourWakeTime(System.currentTimeMillis(), bufferMinutes)
        val reason = if (bufferMinutes > 0) {
            "6-Hour Sleep Completed (+${bufferMinutes}m buffer) - Rise & Conquer!"
        } else {
            "6-Hour Sleep Completed (4 Full Cycles) - Rise & Conquer!"
        }
        scheduleExactAlarm(wakeTime, reason)
        return wakeTime
    }

    fun cancelAlarm() {
        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            action = AlarmTriggerReceiver.ACTION_ENFORCER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        safetyManager.clearAlarmScheduled()
    }

    companion object {
        const val ALARM_REQUEST_CODE = 9001
        const val ALARM_CLOCK_REQUEST_CODE = 9002

        /**
         * Calculates 6-hour wakeup timestamp from a given start timestamp.
         */
        fun calculate6HourWakeTime(fromMillis: Long, bufferMinutes: Int = 0): Long {
            val sixHoursMillis = 6L * 60 * 60 * 1000L
            val bufferMillis = bufferMinutes * 60 * 1000L
            return fromMillis + sixHoursMillis + bufferMillis
        }

        /**
         * Calculates target bedtime given a target wake-up time to get 6 hours of sleep.
         */
        fun calculateBedtimeForWake(wakeMillis: Long, bufferMinutes: Int = 0): Long {
            val sixHoursMillis = 6L * 60 * 60 * 1000L
            val bufferMillis = bufferMinutes * 60 * 1000L
            return wakeMillis - sixHoursMillis - bufferMillis
        }
    }
}
