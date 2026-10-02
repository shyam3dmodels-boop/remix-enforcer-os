package com.example.core.cloud

import android.content.Context
import android.util.Log
import com.example.core.recorder.TelegramConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Telegram Drive Unlimited Cloud Storage Engine.
 *
 * Inspired by `caamer20/Telegram-Drive`.
 * Transforms Telegram Bot API into a secure, unlimited cloud drive with:
 * 1. Virtual folder categorization (/Lectures, /Surveillance, /Backups, /Audio).
 * 2. Direct streaming URL resolution (zero-wait in-app media playback).
 * 3. File download & streaming chunking.
 * 4. Automatic zero-footprint local cleanup.
 */
class TelegramDriveEngine private constructor(private val context: Context) {

    private val appContext = context.applicationContext
    private val configManager = TelegramConfigManager.getInstance(appContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    data class DriveFileItem(
        val id: String = UUID.randomUUID().toString(),
        val fileName: String,
        val virtualFolder: String,
        val fileSizeBytes: Long,
        val mimeType: String,
        val telegramFileId: String,
        val telegramMessageId: Long,
        val directStreamUrl: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _driveItems = MutableStateFlow<List<DriveFileItem>>(emptyList())
    val driveItems: StateFlow<List<DriveFileItem>> = _driveItems.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    /**
     * Uploads any file to the user's private Telegram Cloud Drive.
     */
    suspend fun uploadFile(
        file: File,
        virtualFolder: String = "/General",
        onProgress: ((Int) -> Unit)? = null
    ): Result<DriveFileItem> = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext Result.failure(IOException("File not found or empty: ${file.absolutePath}"))
        }

        val botToken = configManager.getBotToken()
        val chatId = configManager.getChatId()

        if (botToken.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Telegram Bot Token or Chat ID not configured."))
        }

        _isUploading.value = true
        onProgress?.invoke(10)

        try {
            val fileName = file.name
            val mimeType = when {
                fileName.endsWith(".m4a", true) || fileName.endsWith(".mp3", true) -> "audio/mp4"
                fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) -> "image/jpeg"
                fileName.endsWith(".json", true) -> "application/json"
                fileName.endsWith(".apk", true) -> "application/vnd.android.package-archive"
                else -> "application/octet-stream"
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart(
                    "caption",
                    "📁 Telegram-Drive: `$virtualFolder/$fileName`\n📊 Size: ${formatBytes(file.length())}\n🕒 Sync: ${System.currentTimeMillis()}"
                )
                .addFormDataPart("parse_mode", "Markdown")
                .addFormDataPart(
                    "document",
                    fileName,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()

            onProgress?.invoke(40)

            val url = "https://api.telegram.org/bot$botToken/sendDocument"
            val request = Request.Builder().url(url).post(requestBody).build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                _isUploading.value = false
                return@withContext Result.failure(IOException("Telegram API Error ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            if (!json.optBoolean("ok", false)) {
                _isUploading.value = false
                return@withContext Result.failure(IOException("Telegram upload rejected: $responseBody"))
            }

            val resultObj = json.getJSONObject("result")
            val messageId = resultObj.getLong("message_id")
            val docObj = resultObj.optJSONObject("document")
            val fileId = docObj?.optString("file_id") ?: ""

            onProgress?.invoke(80)

            // Resolve Direct Stream Link
            val directStreamUrl = resolveDirectStreamUrl(botToken, fileId)

            val driveItem = DriveFileItem(
                fileName = fileName,
                virtualFolder = virtualFolder,
                fileSizeBytes = file.length(),
                mimeType = mimeType,
                telegramFileId = fileId,
                telegramMessageId = messageId,
                directStreamUrl = directStreamUrl
            )

            _driveItems.value = _driveItems.value + driveItem
            onProgress?.invoke(100)
            _isUploading.value = false

            Log.i(TAG, "✓ File uploaded to Telegram Drive: $virtualFolder/$fileName (ID: $fileId)")
            Result.success(driveItem)
        } catch (e: Exception) {
            _isUploading.value = false
            Log.e(TAG, "Failed to upload file to Telegram Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves official Telegram file_id to direct streaming URL.
     */
    suspend fun resolveDirectStreamUrl(botToken: String, fileId: String): String? = withContext(Dispatchers.IO) {
        if (fileId.isBlank()) return@withContext null
        try {
            val url = "https://api.telegram.org/bot$botToken/getFile?file_id=$fileId"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@use null
                val json = JSONObject(body)
                if (json.optBoolean("ok", false)) {
                    val filePath = json.getJSONObject("result").getString("file_path")
                    "https://api.telegram.org/file/bot$botToken/$filePath"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve stream URL for $fileId", e)
            null
        }
    }

    /**
     * Downloads a file directly from Telegram Drive to local destination.
     */
    suspend fun downloadFile(
        fileId: String,
        destinationFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val botToken = configManager.getBotToken()
        val directUrl = resolveDirectStreamUrl(botToken, fileId)
            ?: return@withContext Result.failure(IOException("Could not resolve stream URL for fileId: $fileId"))

        try {
            val req = Request.Builder().url(directUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@use Result.failure(IOException("Download failed: ${resp.code}"))
                }

                val body = resp.body ?: return@use Result.failure(IOException("Empty download body"))
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalRead * 100) / contentLength).toInt()
                        onProgress?.invoke(progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Result.success(destinationFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading from Telegram Drive", e)
            Result.failure(e)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) "%.2f MB".format(mb) else "%.1f KB".format(kb)
    }

    companion object {
        private const val TAG = "TelegramDrive"

        @Volatile
        private var instance: TelegramDriveEngine? = null

        fun getInstance(context: Context): TelegramDriveEngine {
            return instance ?: synchronized(this) {
                instance ?: TelegramDriveEngine(context).also { instance = it }
            }
        }
    }
}
