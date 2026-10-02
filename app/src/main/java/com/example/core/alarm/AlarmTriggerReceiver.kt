package com.example.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmTriggerReceiver", "Alarm fired! Action: ${intent?.action}")
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: "Wakeup Challenge"

        val safetyManager = AlarmSafetyManager.getInstance(context)
        val (suppressed, suppressReason) = safetyManager.shouldSuppressAlarm()

        if (suppressed) {
            Log.i("AlarmTriggerReceiver", "Alarm suppressed by safety policy: $suppressReason")
            safetyManager.clearAlarmScheduled()
            safetyManager.showSuppressedNotification(suppressReason)
            return
        }

        safetyManager.setAlarmRinging(true)

        val serviceIntent = Intent(context, RelentlessAlarmService::class.java).apply {
            action = RelentlessAlarmService.ACTION_START_ALARM
            putExtra(EXTRA_REASON, reason)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_ENFORCER_ALARM = "com.example.enforcer.ACTION_EXACT_ALARM"
        const val EXTRA_REASON = "extra_alarm_reason"
    }
}
