package com.example.core.discipline

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import java.util.Calendar

class AppUsageTracker(private val context: Context) {

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageStatsSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUsageTracker", "Could not open usage settings", e)
        }
    }

    @Volatile
    private var cachedUsageMap: Map<String, Int>? = null
    @Volatile
    private var lastCacheTimestamp: Long = 0L

    fun getTodayUsageMinutesMap(forceRefresh: Boolean = false): Map<String, Int> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedUsageMap != null && (now - lastCacheTimestamp) < 15_000L) {
            return cachedUsageMap!!
        }

        if (!hasUsageStatsPermission()) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = now

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyMap()

        val resultMap = HashMap<String, Int>(stats.size)
        for (stat in stats) {
            val minutes = (stat.totalTimeInForeground / (1000 * 60)).toInt()
            if (minutes > 0) {
                resultMap[stat.packageName] = minutes
            }
        }
        cachedUsageMap = resultMap
        lastCacheTimestamp = now
        return resultMap
    }

    fun getTodayUsageMinutes(packageName: String): Int {
        val map = getTodayUsageMinutesMap()
        return map[packageName] ?: 0
    }
}
