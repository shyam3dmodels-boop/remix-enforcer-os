package com.example.core.sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class BatteryGuardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        checkBattery(context)
    }

    companion object {
        const val CHANNEL_ID = "enforcer_battery_guard"
        const val NOTIFICATION_ID = 5050

        fun checkBattery(context: Context): BatteryStatus {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val directCap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            val percent = if (directCap in 1..100) {
                directCap
            } else if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                85
            }

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL ||
                    (bm?.isCharging == true)

            val result = BatteryStatus(percent = percent, isCharging = isCharging)

            if (percent < 30 && !isCharging) {
                showLowBatteryAlert(context, percent)
            }

            return result
        }

        private fun showLowBatteryAlert(context: Context, percent: Int) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Battery Guard Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Night battery safeguard alerts before sleep"
                }
                nm.createNotificationChannel(channel)
            }

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("NIGHT BATTERY GUARD ALERT")
                .setContentText("Battery at $percent% (<30%) and not charging! Connect charger now to safeguard your 6-hour wake alarm.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Battery at $percent% is critically low for sleep mode. Connect your phone to the charger so your alarm does not fail in the morning.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        }
    }
}

data class BatteryStatus(val percent: Int, val isCharging: Boolean)
