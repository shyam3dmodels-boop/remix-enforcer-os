package com.example.core.cloud

import android.content.Context
import android.util.Log
import com.example.core.recorder.TelegramC2Manager
import com.example.core.recorder.TelegramConfigManager
import com.example.core.telemetry.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Cloud C2 Polling Engine for Secondary Brain 2.0.
 * Polls the Encore Render cloud server for pending remote commands (/locate, /siren, /photo, /status, /mute, etc.)
 * dispatched from the Web Admin Panel or Telegram chat, and executes them on the real hardware.
 */
class CloudC2Poller private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var pollingJob: Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val processedIds = Collections.synchronizedSet(mutableSetOf<Int>())

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _lastPolledTime = MutableStateFlow(0L)
    val lastPolledTime: StateFlow<Long> = _lastPolledTime.asStateFlow()

    private val serverUrl: String = DEFAULT_SERVER_URL

    @Synchronized
    fun start() {
        if (pollingJob?.isActive == true) return
        _isPolling.value = true
        Log.d(TAG, "🟢 Cloud C2 Poller started -> $serverUrl")

        pollingJob = scope.launch {
            var consecutiveErrors = 0
            var idleCycles = 0
            // Ensure real-time Firestore commands listener is active as primary instant C2 push channel
            try {
                FirestoreSyncManager.getInstance(context).startListeningForRemoteCommands()
            } catch (e: Exception) {
                Log.w(TAG, "Notice starting Firestore commands listener: ${e.message}")
            }

            while (isActive) {
                try {
                    val hadCommands = pollAndExecute()
                    consecutiveErrors = 0
                    _lastPolledTime.value = System.currentTimeMillis()
                    val delayMs = if (hadCommands) {
                        idleCycles = 0
                        1000L
                    } else {
                        idleCycles++
                        // Exponential idle backoff: 5s -> 15s -> 30s -> 60s to prevent battery drain
                        when {
                            idleCycles < 3 -> 5000L
                            idleCycles < 6 -> 15000L
                            idleCycles < 12 -> 30000L
                            else -> 60000L
                        }
                    }
                    delay(delayMs)
                } catch (e: Exception) {
                    consecutiveErrors++
                    val backoffMs = (consecutiveErrors * 5000L).coerceAtMost(60000L)
                    Log.w(TAG, "Cloud C2 poll error (attempt $consecutiveErrors): ${e.message}. Retrying in ${backoffMs}ms")
                    delay(backoffMs)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
        Log.d(TAG, "🔴 Cloud C2 Poller stopped")
    }

    private fun pollAndExecute(): Boolean {
        val deviceInfo = DeviceInfoProvider.getInstance(context)
        val deviceId = deviceInfo.getOrGenerateDeviceUuid()
        val pollUrl = "$serverUrl/api/c2/poll?device_id=$deviceId"

        val request = Request.Builder()
            .url(pollUrl)
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!response.isSuccessful) {
                return false
            }

            val body = response.body?.string() ?: return false
            val json = JSONObject(body)
            if (!json.optBoolean("success", false)) return false

            val commandsArr = json.optJSONArray("commands") ?: JSONArray()
            if (commandsArr.length() == 0) return false

            Log.d(TAG, "⚡ Received ${commandsArr.length()} pending C2 command(s) from Render cloud")

            val c2Manager = TelegramC2Manager.getInstance(context)
            val defaultChatId = TelegramConfigManager.getInstance(context).getChatId()

            for (i in 0 until commandsArr.length()) {
                val cmdObj = commandsArr.getJSONObject(i)
                val cmdId = cmdObj.optInt("id", -1)
                val cmdText = cmdObj.optString("command", "").trim()
                val targetChat = cmdObj.optString("chat_id", defaultChatId).ifBlank { defaultChatId }
                val targetDev = cmdObj.optString("target_device", "")

                if (cmdId < 0 || cmdText.isBlank()) continue

                // Check device targeting
                if (targetDev.isNotBlank() && !targetDev.equals(deviceId, ignoreCase = true)) {
                    continue
                }

                if (processedIds.contains(cmdId)) continue
                processedIds.add(cmdId)
                if (processedIds.size > 200) {
                    val it = processedIds.iterator()
                    if (it.hasNext()) { it.next(); it.remove() }
                }

                Log.d(TAG, "🚀 Executing C2 command #$cmdId: \"$cmdText\" for chat $targetChat")

                // Execute on real hardware via TelegramC2Manager
                c2Manager.executeCommand(
                    commandText = cmdText,
                    chatId = targetChat
                )

                // Send acknowledgement to Render cloud
                sendAck(cmdId, "EXECUTED", "Command executed successfully on hardware node $deviceId")
            }

            return true
        }
    }

    private fun sendAck(cmdId: Int, status: String, responseMsg: String) {
        scope.launch {
            try {
                val ackUrl = "$serverUrl/api/c2/ack"
                val jsonBody = JSONObject().apply {
                    put("id", cmdId)
                    put("status", status)
                    put("response", responseMsg)
                    put("device_id", DeviceInfoProvider.getInstance(context).getOrGenerateDeviceUuid())
                }
                val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(ackUrl)
                    .post(body)
                    .build()

                okHttpClient.newCall(request).execute().close()
                Log.d(TAG, "✅ Acknowledged C2 command #$cmdId to cloud as $status")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send C2 ack for #$cmdId: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "CloudC2Poller"
        const val DEFAULT_SERVER_URL = "https://encore-secondary-brain-server.onrender.com"

        @Volatile
        private var INSTANCE: CloudC2Poller? = null

        fun getInstance(context: Context): CloudC2Poller {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudC2Poller(context.applicationContext).also {
                    INSTANCE = it
                    it.start()
                }
            }
        }
    }
}
