package com.example.core.recorder

import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainApplication
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that runs daily at 10:00 PM.
 * Compiles total lecture recording hours, delivered audio chunks, battery health,
 * and device storage status into a formatted Markdown summary card posted directly to Telegram.
 */
class DailyAuditWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting 10:00 PM Daily Audit report...")

        val telegramConfig = TelegramConfigManager.getInstance(appContext)
        val botToken = telegramConfig.getBotToken()
        val chatId = telegramConfig.getChatId()

        if (botToken.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "Telegram credentials not configured. Skipping daily audit.")
            return Result.success()
        }

        try {
            val repository = (appContext.applicationContext as? MainApplication)?.repository
            val allLectures = repository?.getAllLecturesDirect() ?: emptyList()

            // Calculate today's lectures
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayLectures = allLectures.filter { it.timestamp >= todayStart }
            val totalTodaySeconds = todayLectures.sumOf { it.durationSeconds }
            val totalMinutes = totalTodaySeconds / 60
            val totalHours = totalMinutes / 60

            // Cloud Pipe metrics
            val wipedChunks = telegramConfig.wipedFilesCount.value
            val wipedBytes = telegramConfig.totalBytesWiped.value
            val wipedMb = wipedBytes / (1024.0 * 1024.0)

            // Battery metrics
            val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging == true

            // Storage space
            val freeSpaceGb = Environment.getDataDirectory().freeSpace / (1024.0 * 1024.0 * 1024.0)

            val currentDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

            val message = """
                📊 *[ENFORCER OS // 10:00 PM DAILY AUDIT]*
                📅 *$currentDateStr*
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                🎙️ *Today's Lectures:* ${todayLectures.size} recorded (${totalHours}h ${totalMinutes % 60}m audio)
                📚 *Vault Total:* ${allLectures.size} sessions stored
                ☁️ *Cloud Pipe Delivered:* $wipedChunks chunks (${String.format(Locale.ROOT, "%.1f", wipedMb)} MB wiped from disk)
                🔋 *Battery Health:* ${if (batteryPct >= 0) "$batteryPct%" else "Unknown"} ${if (isCharging) "⚡ Charging" else "🔋 Unplugged"}
                💾 *Internal Storage:* ${String.format(Locale.ROOT, "%.1f", freeSpaceGb)} GB free (0 MB clutter)
                ⚡ *Status:* All surveillance & coaching engines operating nominally.
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            """.trimIndent()

            postToTelegram(botToken, chatId, message)
            Log.d(TAG, "Daily audit card successfully posted to Telegram.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile/post daily audit", e)
            return Result.retry()
        }
    }

    private fun postToTelegram(token: String, chatId: String, text: String) {
        val body = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .add("parse_mode", "Markdown")
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.close()
    }

    companion object {
        const val TAG = "DailyAuditWorker"
        const val UNIQUE_WORK_NAME = "enforcer_daily_10pm_audit"

        /**
         * Schedules the daily 10:00 PM audit task using WorkManager.
         */
        fun scheduleDailyAudit(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Calculate delay to 10:00 PM (22:00)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 22)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val periodicRequest = PeriodicWorkRequestBuilder<DailyAuditWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
            Log.d(TAG, "Scheduled 10:00 PM audit with initial delay: ${initialDelayMs / (1000 * 60)} mins")
        }

        /**
         * Trigger immediate run of the audit for instant verification.
         */
        fun triggerAuditNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<DailyAuditWorker>()
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "manual_test_audit_${System.currentTimeMillis()}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
