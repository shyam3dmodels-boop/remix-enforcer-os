package com.example.core.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.VoiceTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * EndOfDaySummaryNotifier:
 * Parses voice-to-task notes for any missed or pending 'HOMEWORK' or 'IMPORTANT_TEST' deadlines
 * and posts a silent, non-intrusive end-of-day notification summary.
 */
class EndOfDaySummaryNotifier private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _isDailySummaryEnabled = MutableStateFlow(prefs.getBoolean(KEY_DAILY_SUMMARY_ENABLED, true))
    val isDailySummaryEnabled: StateFlow<Boolean> = _isDailySummaryEnabled.asStateFlow()

    private val _summaryHour = MutableStateFlow(prefs.getInt(KEY_SUMMARY_HOUR, 21)) // Default 9:00 PM (21:00)
    val summaryHour: StateFlow<Int> = _summaryHour.asStateFlow()

    private val _summaryMinute = MutableStateFlow(prefs.getInt(KEY_SUMMARY_MINUTE, 30)) // Default 9:30 PM
    val summaryMinute: StateFlow<Int> = _summaryMinute.asStateFlow()

    private val _lastSummarySentTime = MutableStateFlow(prefs.getLong(KEY_LAST_SUMMARY_SENT, 0L))
    val lastSummarySentTime: StateFlow<Long> = _lastSummarySentTime.asStateFlow()

    private val _lastSummaryPendingCount = MutableStateFlow(prefs.getInt(KEY_LAST_SUMMARY_PENDING_COUNT, 0))
    val lastSummaryPendingCount: StateFlow<Int> = _lastSummaryPendingCount.asStateFlow()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Academic Summary",
                NotificationManager.IMPORTANCE_LOW // Low importance = completely SILENT (no sound, no vibration)
            ).apply {
                description = "Silent evening summary of pending homework and exam tasks parsed from lectures"
                enableVibration(false)
                setSound(null, null)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun setSummaryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_SUMMARY_ENABLED, enabled).apply()
        _isDailySummaryEnabled.value = enabled
    }

    fun setSummaryTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_SUMMARY_HOUR, hour)
            .putInt(KEY_SUMMARY_MINUTE, minute)
            .apply()
        _summaryHour.value = hour
        _summaryMinute.value = minute
    }

    /**
     * Scans Room database for pending voice tasks in HOMEWORK, IMPORTANT_TEST, or DUE_DATE categories,
     * and sends a clean silent NotificationCompat summary.
     */
    suspend fun evaluateAndSendDailySummary(forceNow: Boolean = false): SummaryResult {
        if (!forceNow && !_isDailySummaryEnabled.value) {
            return SummaryResult.Skipped("Daily summary is disabled in settings")
        }

        val db = AppDatabase.getInstance(context)
        val allTasks = db.voiceTaskDao().getAllTasksDirect()

        val pendingTasks = allTasks.filter { !it.isCompleted }
        val pendingHomework = pendingTasks.filter { it.category == "HOMEWORK" }
        val pendingTests = pendingTasks.filter { it.category == "IMPORTANT_TEST" }
        val pendingDueDates = pendingTasks.filter { it.category == "DUE_DATE" }
        val otherPending = pendingTasks.filter { it.category != "HOMEWORK" && it.category != "IMPORTANT_TEST" && it.category != "DUE_DATE" }

        val totalPendingCount = pendingTasks.size
        val missedHomeworkCount = pendingHomework.size
        val missedExamCount = pendingTests.size

        // Build notification
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "recorder_tasks")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (totalPendingCount == 0) {
            "✅ All Class Tasks Completed Today"
        } else {
            "📚 End-of-Day Summary: $totalPendingCount Pending Task${if (totalPendingCount > 1) "s" else ""}"
        }

        val inboxStyle = NotificationCompat.InboxStyle()
        inboxStyle.setBigContentTitle(title)

        if (missedHomeworkCount > 0) {
            inboxStyle.addLine("📚 Homework (${missedHomeworkCount}):")
            pendingHomework.take(3).forEach { task ->
                inboxStyle.addLine("  • ${task.title}")
            }
            if (pendingHomework.size > 3) {
                inboxStyle.addLine("  • +${pendingHomework.size - 3} more homework items")
            }
        }

        if (missedExamCount > 0) {
            inboxStyle.addLine("🎯 Exam Topics (${missedExamCount}):")
            pendingTests.take(3).forEach { task ->
                inboxStyle.addLine("  • ${task.title}")
            }
            if (pendingTests.size > 3) {
                inboxStyle.addLine("  • +${pendingTests.size - 3} more exam topics")
            }
        }

        if (pendingDueDates.isNotEmpty()) {
            inboxStyle.addLine("⏰ Due Dates (${pendingDueDates.size}):")
            pendingDueDates.take(2).forEach { task ->
                inboxStyle.addLine("  • ${task.title}")
            }
        }

        if (totalPendingCount == 0) {
            inboxStyle.addLine("Zero outstanding homework or exam items logged today. Great consistency!")
        } else {
            inboxStyle.setSummaryText("${missedHomeworkCount} HW • ${missedExamCount} Tests • Total ${totalPendingCount}")
        }

        val shortMessage = when {
            totalPendingCount == 0 -> "All homework & test topics logged today have been completed."
            missedHomeworkCount > 0 && missedExamCount > 0 ->
                "$missedHomeworkCount pending homework tasks & $missedExamCount exam revision topics require attention."
            missedHomeworkCount > 0 ->
                "$missedHomeworkCount homework tasks pending from today's classes."
            missedExamCount > 0 ->
                "$missedExamCount crucial exam test topics pending review."
            else -> "$totalPendingCount task(s) logged from class pending completion."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(inboxStyle)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent notification priority
            .setSilent(true) // Explicitly silent - no sound or vibration
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_SUMMARY_SENT, now)
            .putInt(KEY_LAST_SUMMARY_PENDING_COUNT, totalPendingCount)
            .apply()

        _lastSummarySentTime.value = now
        _lastSummaryPendingCount.value = totalPendingCount

        Log.d(TAG, "Sent silent end-of-day summary: $totalPendingCount pending ($missedHomeworkCount HW, $missedExamCount Tests)")
        return SummaryResult.Success(
            pendingCount = totalPendingCount,
            homeworkCount = missedHomeworkCount,
            testCount = missedExamCount
        )
    }

    sealed class SummaryResult {
        data class Success(val pendingCount: Int, val homeworkCount: Int, val testCount: Int) : SummaryResult()
        data class Skipped(val reason: String) : SummaryResult()
    }

    companion object {
        const val CHANNEL_ID = "daily_academic_summary_channel"
        const val NOTIFICATION_ID = 9021
        private const val TAG = "EndOfDaySummaryNotifier"

        private const val PREFS_NAME = "enforcer_daily_summary_prefs"
        private const val KEY_DAILY_SUMMARY_ENABLED = "daily_summary_enabled"
        private const val KEY_SUMMARY_HOUR = "summary_hour"
        private const val KEY_SUMMARY_MINUTE = "summary_minute"
        private const val KEY_LAST_SUMMARY_SENT = "last_summary_sent"
        private const val KEY_LAST_SUMMARY_PENDING_COUNT = "last_summary_pending_count"

        @Volatile
        private var instance: EndOfDaySummaryNotifier? = null

        fun getInstance(context: Context): EndOfDaySummaryNotifier {
            return instance ?: synchronized(this) {
                instance ?: EndOfDaySummaryNotifier(context.applicationContext).also { instance = it }
            }
        }
    }
}
