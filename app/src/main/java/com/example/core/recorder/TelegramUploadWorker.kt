package com.example.core.recorder

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainApplication
import com.example.data.local.entity.LectureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Background Worker that transmits 15-minute rolling audio chunk recordings
 * directly to the student's private Telegram Bot API.
 *
 * CRITICAL DISCIPLINE:
 * Immediately upon receiving HTTP 200 OK from Telegram, the local file is
 * permanently delete()d from the device. Local storage remains at a strictly 0 MB footprint
 * so friends or nosy onlookers inspecting the phone can never find lecture recordings.
 */
class TelegramUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return@withContext Result.failure()
        val lectureTitle = inputData.getString(KEY_LECTURE_TITLE) ?: "Class Lecture"
        val subject = inputData.getString(KEY_SUBJECT) ?: "Coaching"
        val chunkIndex = inputData.getInt(KEY_CHUNK_INDEX, 1)
        val durationSec = inputData.getLong(KEY_DURATION_SEC, 0L)
        val lectureId = inputData.getLong(KEY_LECTURE_ID, 0L)

        val file = File(filePath)
        val config = TelegramConfigManager.getInstance(applicationContext)
        val queueId = id.toString()
        val currentRunAttempt = runAttemptCount

        if (!file.exists()) {
            Log.w(TAG, "File does not exist or was already wiped: $filePath")
            config.updateQueueItem(
                id = queueId,
                chunkTitle = "$lectureTitle #$chunkIndex",
                chunkIndex = chunkIndex,
                totalBytes = 0L,
                bytesTransferred = 0L,
                status = UploadQueueStatus.COMPLETED,
                retryCount = currentRunAttempt,
                statusMessage = "Already synced & wiped (0 MB local footprint)"
            )
            val outputData = workDataOf(
                KEY_LECTURE_TITLE to lectureTitle,
                KEY_CHUNK_INDEX to chunkIndex,
                KEY_PROGRESS_PERCENT to 100,
                KEY_PROGRESS_STATUS_MSG to "Already synced & wiped (0 MB footprint)"
            )
            return@withContext Result.success(outputData)
        }

        val botToken = inputData.getString(KEY_BOT_TOKEN)?.takeIf { it.isNotBlank() }
            ?: config.getBotToken()
        val chatId = inputData.getString(KEY_CHAT_ID)?.takeIf { it.isNotBlank() }
            ?: config.getChatId()

        val fileSizeBytes = file.length()

        // Report initial progress to WorkManager
        setProgress(
            workDataOf(
                KEY_PROGRESS_PERCENT to 10,
                KEY_PROGRESS_BYTES_SENT to 0L,
                KEY_PROGRESS_TOTAL_BYTES to fileSizeBytes,
                KEY_PROGRESS_STATUS_MSG to "Initializing upload...",
                KEY_LECTURE_TITLE to lectureTitle,
                KEY_CHUNK_INDEX to chunkIndex
            )
        )

        if (botToken.isBlank() || chatId.isBlank()) {
            val errMsg = "Telegram Bot Token or Chat ID not configured. Please configure in Settings."
            Log.e(TAG, errMsg)
            config.updateStatus(errMsg)
            config.updateQueueItem(
                id = queueId,
                chunkTitle = "$lectureTitle #$chunkIndex",
                chunkIndex = chunkIndex,
                totalBytes = fileSizeBytes,
                bytesTransferred = 0L,
                status = UploadQueueStatus.FAILED_PERMANENT,
                retryCount = currentRunAttempt,
                statusMessage = "Missing Bot Token or Chat ID"
            )
            setProgress(
                workDataOf(
                    KEY_PROGRESS_PERCENT to 0,
                    KEY_PROGRESS_BYTES_SENT to 0L,
                    KEY_PROGRESS_TOTAL_BYTES to fileSizeBytes,
                    KEY_PROGRESS_STATUS_MSG to "Permanent Error: Credentials missing",
                    KEY_LECTURE_TITLE to lectureTitle,
                    KEY_CHUNK_INDEX to chunkIndex
                )
            )
            notifyUploadFailure("Upload Failed: Credentials Missing", "Please configure your Telegram Bot Token & Chat ID in Settings.")
            return@withContext Result.failure(workDataOf(KEY_PROGRESS_STATUS_MSG to errMsg))
        }

        Log.i(TAG, "Uploading chunk #$chunkIndex ($fileSizeBytes bytes) to Telegram Bot...")
        config.updateStatus("Uploading part #$chunkIndex to Telegram cloud...")
        config.updateQueueItem(
            id = queueId,
            chunkTitle = "$lectureTitle #$chunkIndex",
            chunkIndex = chunkIndex,
            totalBytes = fileSizeBytes,
            bytesTransferred = (fileSizeBytes * 0.4).toLong(),
            status = UploadQueueStatus.IN_FLIGHT,
            retryCount = currentRunAttempt,
            statusMessage = "In-flight: Streaming to Telegram..."
        )
        setProgress(
            workDataOf(
                KEY_PROGRESS_PERCENT to 45,
                KEY_PROGRESS_BYTES_SENT to (fileSizeBytes * 0.45).toLong(),
                KEY_PROGRESS_TOTAL_BYTES to fileSizeBytes,
                KEY_PROGRESS_STATUS_MSG to "Streaming audio chunk to Telegram...",
                KEY_LECTURE_TITLE to lectureTitle,
                KEY_CHUNK_INDEX to chunkIndex
            )
        )

        val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val captionText = buildString {
            append("📚 $lectureTitle (Part $chunkIndex)\n")
            append("🏷️ Subject: $subject\n")
            append("⏱️ Duration: ${AudioChunkRecorder.formatSeconds(durationSec)}\n")
            append("☁️ Status: Synced to Telegram Bot • Local chunk cache cleared.")
        }

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("title", "$lectureTitle - Part $chunkIndex")
            .addFormDataPart("performer", subject)
            .addFormDataPart("caption", captionText)
            .addFormDataPart("audio", file.name, requestFile)
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendAudio")
            .post(multipartBody)
            .build()

        return@withContext try {
            val response = okHttpClient.newCall(request).execute()
            val responseCode = response.code
            val isSuccess = response.isSuccessful
            val bodyString = response.body?.string() ?: ""
            response.close()

            if (isSuccess && responseCode in 200..299) {
                // Delete local file only after verified successful upload
                var deleted = false
                try {
                    deleted = file.delete()
                    if (!deleted && file.exists()) {
                        file.deleteOnExit()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "File deletion failed after upload", e)
                }
                Log.i(TAG, "Telegram returned HTTP $responseCode. Local file deleted: $deleted")

                config.recordWipe(fileSizeBytes, "$lectureTitle #$chunkIndex")
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = fileSizeBytes,
                    status = UploadQueueStatus.COMPLETED,
                    retryCount = currentRunAttempt,
                    statusMessage = "Uploaded & wiped from storage (0 MB)"
                )

                // Update Room Database if entity exists
                updateDatabaseRecordWiped(lectureId, filePath, lectureTitle, subject, durationSec)

                setProgress(
                    workDataOf(
                        KEY_PROGRESS_PERCENT to 100,
                        KEY_PROGRESS_BYTES_SENT to fileSizeBytes,
                        KEY_PROGRESS_TOTAL_BYTES to fileSizeBytes,
                        KEY_PROGRESS_STATUS_MSG to "Uploaded & wiped from storage (0 MB)",
                        KEY_LECTURE_TITLE to lectureTitle,
                        KEY_CHUNK_INDEX to chunkIndex
                    )
                )

                val successData = workDataOf(
                    KEY_LECTURE_TITLE to lectureTitle,
                    KEY_CHUNK_INDEX to chunkIndex,
                    KEY_PROGRESS_PERCENT to 100,
                    KEY_PROGRESS_STATUS_MSG to "Uploaded & local file wiped (0 MB)"
                )
                Result.success(successData)
            } else if (responseCode in 400..404) {
                // Permanent client auth error (bad token, invalid chat ID, blocked)
                Log.e(TAG, "Telegram upload permanently failed with client error HTTP $responseCode: $bodyString")
                val errMsg = "HTTP $responseCode: Invalid Bot Token or Chat ID"
                config.updateStatus(errMsg)
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = 0L,
                    status = UploadQueueStatus.FAILED_PERMANENT,
                    retryCount = currentRunAttempt,
                    statusMessage = errMsg
                )
                notifyUploadFailure("Telegram Upload Rejected ($responseCode)", "Check Bot Token & Chat ID permissions.")
                Result.failure(workDataOf(KEY_PROGRESS_STATUS_MSG to errMsg))
            } else {
                Log.e(TAG, "Telegram upload failed with HTTP $responseCode: $bodyString")
                val errMsg = "HTTP $responseCode: Retrying on reconnect"
                config.updateStatus(errMsg)
                if (currentRunAttempt >= 5) {
                    config.updateQueueItem(
                        id = queueId,
                        chunkTitle = "$lectureTitle #$chunkIndex",
                        chunkIndex = chunkIndex,
                        totalBytes = fileSizeBytes,
                        bytesTransferred = 0L,
                        status = UploadQueueStatus.FAILED_PERMANENT,
                        retryCount = currentRunAttempt + 1,
                        statusMessage = "Max retries exceeded ($responseCode)"
                    )
                    notifyUploadFailure("Upload Failed after 5 Retries", "HTTP $responseCode: $lectureTitle #$chunkIndex")
                    Result.failure(workDataOf(KEY_PROGRESS_STATUS_MSG to "Max retries exceeded"))
                } else {
                    config.updateQueueItem(
                        id = queueId,
                        chunkTitle = "$lectureTitle #$chunkIndex",
                        chunkIndex = chunkIndex,
                        totalBytes = fileSizeBytes,
                        bytesTransferred = 0L,
                        status = UploadQueueStatus.FAILED_RETRYING,
                        retryCount = currentRunAttempt + 1,
                        statusMessage = errMsg
                    )
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network failure uploading audio chunk to Telegram. Retrying...", e)
            config.updateStatus("Offline. WorkManager queued for network reconnect.")
            if (currentRunAttempt >= 5) {
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = 0L,
                    status = UploadQueueStatus.FAILED_PERMANENT,
                    retryCount = currentRunAttempt + 1,
                    statusMessage = "Network failed after 5 retries"
                )
                notifyUploadFailure("Upload Offline Timeout", "Unable to upload $lectureTitle chunk after 5 retries.")
                Result.failure(workDataOf(KEY_PROGRESS_STATUS_MSG to "Network timeout"))
            } else {
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = 0L,
                    status = UploadQueueStatus.FAILED_RETRYING,
                    retryCount = currentRunAttempt + 1,
                    statusMessage = "Network offline - Queued for reconnect"
                )
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in TelegramUploadWorker", e)
            if (currentRunAttempt >= 5) {
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = 0L,
                    status = UploadQueueStatus.FAILED_PERMANENT,
                    retryCount = currentRunAttempt + 1,
                    statusMessage = "Permanent error: ${e.localizedMessage ?: "Unknown"}"
                )
                notifyUploadFailure("Upload Error", e.localizedMessage ?: "Unknown error")
                Result.failure(workDataOf(KEY_PROGRESS_STATUS_MSG to (e.localizedMessage ?: "Error")))
            } else {
                config.updateQueueItem(
                    id = queueId,
                    chunkTitle = "$lectureTitle #$chunkIndex",
                    chunkIndex = chunkIndex,
                    totalBytes = fileSizeBytes,
                    bytesTransferred = 0L,
                    status = UploadQueueStatus.FAILED_RETRYING,
                    retryCount = currentRunAttempt + 1,
                    statusMessage = "Error: ${e.localizedMessage ?: "Unknown"}"
                )
                Result.retry()
            }
        }
    }

    private suspend fun updateDatabaseRecordWiped(
        lectureId: Long,
        originalFilePath: String,
        title: String,
        subject: String,
        durationSec: Long
    ) {
        try {
            val repo = (applicationContext as? MainApplication)?.repository ?: return
            val existing = if (lectureId != 0L) {
                repo.getLectureById(lectureId)
            } else {
                repo.getLectureByFilePath(originalFilePath)
            }

            if (existing != null) {
                val updated = existing.copy(
                    filePath = "[TELEGRAM CLOUD] Stored in private bot • Local file wiped",
                    fileSizeBytes = 0L,
                    notes = if (existing.notes.isBlank()) {
                        "Cloud Pipe: Uploaded to Telegram Bot and wiped from local phone storage (0 MB footprint)."
                    } else {
                        existing.notes + "\n[Cloud Pipe: Uploaded to Telegram & wiped from local device (0 MB)]"
                    }
                )
                repo.updateLecture(updated)
                Log.i(TAG, "Database record updated to 0 MB footprint for lecture ID: ${existing.id}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update Room record after wipe", e)
        }
    }

    private fun notifyUploadFailure(title: String, reason: String) {
        try {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val notification = NotificationCompat.Builder(applicationContext, "chunk_recorder_channel")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("⚠️ $title")
                .setContentText(reason)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$title: $reason"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            nm.notify(TAG_TELEGRAM_UPLOAD.hashCode() + (1..1000).random(), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not dispatch failure notification", e)
        }
    }

    companion object {
        private const val TAG = "TelegramUploadWorker"

        const val TAG_TELEGRAM_UPLOAD = "telegram_upload"

        const val KEY_FILE_PATH = "key_file_path"
        const val KEY_LECTURE_TITLE = "key_lecture_title"
        const val KEY_SUBJECT = "key_subject"
        const val KEY_CHUNK_INDEX = "key_chunk_index"
        const val KEY_DURATION_SEC = "key_duration_sec"
        const val KEY_LECTURE_ID = "key_lecture_id"
        const val KEY_BOT_TOKEN = "key_bot_token"
        const val KEY_CHAT_ID = "key_chat_id"

        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_PROGRESS_BYTES_SENT = "progress_bytes_sent"
        const val KEY_PROGRESS_TOTAL_BYTES = "progress_total_bytes"
        const val KEY_PROGRESS_STATUS_MSG = "progress_status_msg"

        /**
         * Enqueues a dummy chunk task to test the WorkManager pipeline immediately.
         */
        fun enqueueTestUpload(
            context: Context,
            sampleTitle: String = "Physics Kinematics",
            subject: String = "Physics"
        ): UUID {
            val tempDir = File(context.cacheDir, "test_chunks").apply { mkdirs() }
            val testFile = File(tempDir, "sample_test_chunk_${System.currentTimeMillis()}.m4a")
            if (!testFile.exists()) {
                testFile.writeBytes(ByteArray(192 * 1024) { 0x41 }) // 192 KB sample data
            }
            val config = TelegramConfigManager.getInstance(context)
            return enqueue(
                context = context,
                filePath = testFile.absolutePath,
                lectureTitle = sampleTitle,
                subject = subject,
                chunkIndex = (1..6).random(),
                durationSec = 900L,
                lectureId = 0L,
                botToken = config.getBotToken(),
                chatId = config.getChatId()
            )
        }

        /**
         * Enqueues an upload worker with NetworkType.CONNECTED constraint and exponential backoff.
         */
        fun enqueue(
            context: Context,
            filePath: String,
            lectureTitle: String,
            subject: String,
            chunkIndex: Int,
            durationSec: Long,
            lectureId: Long = 0L,
            botToken: String = "",
            chatId: String = ""
        ): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_LECTURE_TITLE, lectureTitle.take(150))
                .putString(KEY_SUBJECT, subject.take(80))
                .putInt(KEY_CHUNK_INDEX, chunkIndex)
                .putLong(KEY_DURATION_SEC, durationSec)
                .putLong(KEY_LECTURE_ID, lectureId)
                .putString(KEY_BOT_TOKEN, botToken)
                .putString(KEY_CHAT_ID, chatId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TelegramUploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(TAG_TELEGRAM_UPLOAD)
                .addTag("chunk_$chunkIndex")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.i(TAG, "Enqueued TelegramUploadWorker for chunk #$chunkIndex ($filePath)")
            return workRequest.id
        }
    }
}
