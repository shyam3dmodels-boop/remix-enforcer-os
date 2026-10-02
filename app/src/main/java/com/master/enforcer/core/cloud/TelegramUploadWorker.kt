package com.master.enforcer.core.cloud

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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
 * CoroutineWorker that uploads an audio file to the Telegram Bot API using OkHttp multipart request.
 * Strictly calls file.delete() only upon receiving a successful HTTP 200 response.
 * Returns Result.retry() on failed responses or network issues.
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
        val title = inputData.getString(KEY_TITLE) ?: "Recorded Lecture"
        val caption = inputData.getString(KEY_CAPTION) ?: "Audio Lecture Part"

        val file = File(filePath)
        if (!file.exists()) {
            Log.w(TAG, "Target audio file does not exist or already removed: $filePath")
            return@withContext Result.success()
        }

        val botToken = inputData.getString(KEY_BOT_TOKEN)?.takeIf { it.isNotBlank() } ?: BOT_TOKEN
        val chatId = inputData.getString(KEY_CHAT_ID)?.takeIf { it.isNotBlank() } ?: CHAT_ID

        Log.i(TAG, "Starting audio chunk upload to Telegram. File: ${file.name}, Size: ${file.length()} bytes")

        val audioRequestBody = file.asRequestBody("audio/mp4".toMediaTypeOrNull())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("title", title)
            .addFormDataPart("caption", caption)
            .addFormDataPart("audio", file.name, audioRequestBody)
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendAudio")
            .post(multipartBody)
            .build()

        return@withContext try {
            val response = okHttpClient.newCall(request).execute()
            val responseCode = response.code
            val isSuccess = response.isSuccessful
            val responseBody = response.body?.string() ?: ""
            response.close()

            if (isSuccess && responseCode == 200) {
                // Strictly call file.delete() upon successful HTTP 200 response
                val deleted = file.delete()
                Log.i(TAG, "Telegram upload successful (HTTP 200). file.delete() executed: $deleted")
                Result.success()
            } else {
                Log.e(TAG, "Telegram upload failed with HTTP $responseCode: $responseBody. Retrying...")
                Result.retry()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during audio upload to Telegram. Scheduling retry.", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in TelegramUploadWorker. Scheduling retry.", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "TelegramUploadWorker"

        const val BOT_TOKEN = "8942980083:AAHmhVY4ybuOYSSJDsyuF8Z-1DP66WEbl5k"
        const val CHAT_ID = "-1004445314496"

        const val KEY_FILE_PATH = "key_file_path"
        const val KEY_TITLE = "key_title"
        const val KEY_CAPTION = "key_caption"
        const val KEY_BOT_TOKEN = "key_bot_token"
        const val KEY_CHAT_ID = "key_chat_id"

        /**
         * Helper method to enqueue a work request with network constraints and exponential backoff.
         */
        fun enqueue(
            context: Context,
            filePath: String,
            title: String = "Lecture Audio",
            caption: String = "Lecture Chunk",
            botToken: String = BOT_TOKEN,
            chatId: String = CHAT_ID
        ): UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_TITLE, title)
                .putString(KEY_CAPTION, caption)
                .putString(KEY_BOT_TOKEN, botToken)
                .putString(KEY_CHAT_ID, chatId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TelegramUploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag("telegram_upload_worker")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            return workRequest.id
        }
    }
}
