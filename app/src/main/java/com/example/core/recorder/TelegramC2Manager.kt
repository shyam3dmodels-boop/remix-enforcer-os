package com.example.core.recorder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.core.bluetooth.LocatorSirenController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.LinkedHashSet
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Two-Way Telegram Command Controller (C2 Controller).
 * Securely listens to authorized Telegram commands (/status, /mute, /siren, /wipe)
 * and executes safe device management operations.
 */
class TelegramC2Manager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var pollingJob: Job? = null
    private var lastUpdateId: Long = 0L
    @Volatile
    private var activePollCall: okhttp3.Call? = null
    private val processedMessageIds = Collections.synchronizedSet(LinkedHashSet<String>())

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val sirenController = LocatorSirenController(context)
    private val telegramConfig = TelegramConfigManager.getInstance(context)

    private val _isPollingActive = MutableStateFlow(false)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    private val _lastExecutedCommand = MutableStateFlow<String>("None")
    val lastExecutedCommand: StateFlow<String> = _lastExecutedCommand.asStateFlow()

    private val _commandLogs = MutableStateFlow<List<String>>(emptyList())
    val commandLogs: StateFlow<List<String>> = _commandLogs.asStateFlow()

    @Synchronized
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        _isPollingActive.value = true
        logEvent("🟢 Telegram Remote Listener started")

        // Also guarantee Firestore Real-Time C2 Commands Listener and Cloud C2 Poller are active
        try {
            com.example.core.cloud.FirestoreSyncManager.getInstance(context).startListeningForRemoteCommands()
            com.example.core.cloud.CloudC2Poller.getInstance(context).start()
        } catch (e: Exception) {
            Log.w("TelegramC2", "Cloud listener init warning: ${e.message}")
        }

        // To avoid HTTP 409 Conflict with the 24/7 Render Cloud Server holding the single Telegram polling stream,
        // direct getUpdates polling is disabled by default on mobile nodes. Commands are delivered instantly via Cloud C2/Firestore.
        if (!telegramConfig.isDirectPollingEnabled()) {
            logEvent("☁️ Cloud C2 Active: Commands routed via central Render/Firestore pipeline (409 conflict avoided).")
            return
        }

        logEvent("⚡ Direct Telegram Polling enabled (Standalone mode)")
        pollingJob = scope.launch {
            while (isActive) {
                val token = telegramConfig.getBotToken()

                if (token.isBlank()) {
                    delay(4000L)
                    continue
                }

                var hadConflict = false
                try {
                    hadConflict = pollTelegramUpdates(token)
                } catch (e: Exception) {
                    Log.e("TelegramC2", "Polling error: ${e.message}")
                }

                if (hadConflict) {
                    // Back off 15 seconds to avoid ping-pong 409 collisions with another instance
                    delay(15000L)
                } else {
                    delay(3000L)
                }
            }
        }
    }

    @Synchronized
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        try {
            activePollCall?.cancel()
        } catch (_: Exception) {}
        activePollCall = null
        _isPollingActive.value = false
        logEvent("🔴 Telegram Remote Listener stopped")
        try {
            com.example.core.cloud.FirestoreSyncManager.getInstance(context).stopListeningForRemoteCommands()
        } catch (_: Exception) {}
    }

    private fun deleteWebhook(botToken: String) {
        try {
            val url = "https://api.telegram.org/bot$botToken/deleteWebhook?drop_pending_updates=false"
            val req = Request.Builder().url(url).build()
            okHttpClient.newCall(req).execute().close()
            Log.d("TelegramC2", "Telegram webhook reset via deleteWebhook")
        } catch (e: Exception) {
            Log.w("TelegramC2", "deleteWebhook failed: ${e.message}")
        }
    }

    private fun pollTelegramUpdates(botToken: String): Boolean {
        val url = "https://api.telegram.org/bot$botToken/getUpdates?offset=${lastUpdateId + 1}&timeout=5"
        val request = Request.Builder().url(url).build()

        var conflictOccurred = false
        val call = okHttpClient.newCall(request)
        activePollCall = call

        try {
            val response = call.execute()
            response.use {
                if (!response.isSuccessful) {
                    val code = response.code
                    val err = response.body?.string() ?: ""
                    if (code == 409) {
                        conflictOccurred = true
                        Log.w("TelegramC2", "Telegram getUpdates HTTP 409 Conflict: $err")
                        logEvent("⚠️ Bot conflict (409): Another instance or getUpdates request is active. Backing off 15s...")
                        deleteWebhook(botToken)
                    } else {
                        Log.e("TelegramC2", "Telegram getUpdates HTTP $code: $err")
                    }
                    return conflictOccurred
                }
                val bodyString = response.body?.string() ?: return false
                val json = JSONObject(bodyString)
                if (!json.optBoolean("ok", false)) return false

                val resultArr = json.optJSONArray("result") ?: return false
                val unrepliedMessages = mutableListOf<Triple<Long, String, String>>()

                for (i in 0 until resultArr.length()) {
                    val updateObj = resultArr.getJSONObject(i)
                    val updateId = updateObj.optLong("update_id", 0L)
                    if (updateId > lastUpdateId) {
                        lastUpdateId = updateId
                    }

                    val message = updateObj.optJSONObject("message")
                        ?: updateObj.optJSONObject("channel_post")
                        ?: updateObj.optJSONObject("edited_message")
                        ?: updateObj.optJSONObject("edited_channel_post")
                        ?: continue

                    val messageId = message.optLong("message_id", 0L)
                    val chat = message.optJSONObject("chat") ?: continue
                    val chatId = chat.opt("id")?.toString() ?: ""
                    val text = message.optString("text", "").trim()

                    if (text.isNotBlank() && chatId.isNotBlank()) {
                        val key = "$chatId:$messageId"
                        if (!processedMessageIds.contains(key)) {
                            unrepliedMessages.add(Triple(messageId, text, chatId))
                        }
                    }
                }

                // Instant Auto-Responder: Execute every unreplied command immediately to its source chat
                if (unrepliedMessages.isNotEmpty()) {
                    for ((msgId, text, chatId) in unrepliedMessages) {
                        val key = "$chatId:$msgId"
                        processedMessageIds.add(key)
                        if (processedMessageIds.size > 500) {
                            val it = processedMessageIds.iterator()
                            if (it.hasNext()) { it.next(); it.remove() }
                        }
                        logEvent("⚡ Auto-responding to command from $chatId: \"$text\"")
                        executeCommand(text, botToken, chatId)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is java.io.IOException && call.isCanceled()) {
                // Polling call was cancelled intentionally
            } else {
                Log.e("TelegramC2", "Fetch updates failed", e)
            }
        } finally {
            if (activePollCall === call) {
                activePollCall = null
            }
        }
        return conflictOccurred
    }

    fun executeCommand(commandText: String, botToken: String = telegramConfig.getBotToken(), chatId: String = telegramConfig.getChatId()) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        // Extract slash command if present anywhere in string (e.g. "📍 /locate", "🚨 /siren 20", or "/status")
        val slashMatch = Regex("""(/[\w_-]+)(?:\s+(.*))?""").find(trimmed)
        val (command, arg, parts) = if (slashMatch != null) {
            val cmd = slashMatch.groupValues[1].lowercase(Locale.ROOT).substringBefore("@")
            val rawArg = slashMatch.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
            val p = rawArg?.split("\\s+".toRegex()) ?: emptyList()
            Triple(cmd, p.getOrNull(0), listOf(cmd) + p)
        } else {
            val p = trimmed.split("\\s+".toRegex())
            val cmd = p.getOrNull(0)?.lowercase(Locale.ROOT)?.substringBefore("@") ?: ""
            Triple(cmd, p.getOrNull(1), p)
        }

        _lastExecutedCommand.value = "$commandText (${SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())})"
        logEvent("📥 Received from $chatId: $commandText")

        val deviceInfo = com.example.core.telemetry.DeviceInfoProvider.getInstance(context)
        val myUserId = deviceInfo.getUserId()
        val myDeviceUuid = deviceInfo.getOrGenerateDeviceUuid()

        // Check if command is specifically targeted to a different user ID / device ID
        if (arg != null && (arg.startsWith("usr-", ignoreCase = true) || arg.startsWith("dev-", ignoreCase = true))) {
            if (!myUserId.equals(arg, ignoreCase = true) && !myDeviceUuid.equals(arg, ignoreCase = true)) {
                // Command was targeted specifically to another node
                return
            }
        }

        // 🔒 Strict Chat ID Whitelist Authorization Guard (Finding 1.2)
        if (!telegramConfig.isChatAuthorized(chatId)) {
            Log.w("TelegramC2", "⛔ Unauthorized C2 command blocked from Chat ID: $chatId | Command: $commandText")
            logEvent("⛔ Unauthorized attempt blocked from $chatId: $commandText")

            // Only /start is allowed to non-authorized chats so user learns their Chat ID
            if (command == "/start") {
                handleUnauthorizedStartCommand(botToken, chatId)
                return
            }

            // Send Security Rejection to the caller
            sendTelegramReply(
                botToken,
                chatId,
                "⛔ <b>[ACCESS DENIED — ENFORCER OS]</b>\n" +
                "Your Telegram Chat ID <code>$chatId</code> is not authorized to control this Android device.\n\n" +
                "To authorize, configure this Chat ID in the Web Admin or Settings."
            )

            // Alert the authorized primary owner
            val authorizedOwnerChat = telegramConfig.getChatId().trim()
            if (authorizedOwnerChat.isNotEmpty() && authorizedOwnerChat != chatId) {
                sendTelegramReply(
                    botToken,
                    authorizedOwnerChat,
                    "🚨 <b>[SECURITY ALERT — UNAUTHORIZED C2 ATTEMPT]</b>\n" +
                    "• <b>Source Chat ID</b>: <code>$chatId</code>\n" +
                    "• <b>Attempted Command</b>: <code>$commandText</code>\n" +
                    "• <b>Status</b>: 🛡️ Blocked immediately"
                )
            }
            return
        }

        when {
            command == "/start" -> handleStartCommand(botToken, chatId)
            command == "/connect" || command == "/bind" -> handleConnectCommand(botToken, chatId)
            command in listOf("/signup", "/register", "signup", "register") -> {
                val nameQuery = parts.drop(1).joinToString(" ")
                handleSignupCommand(botToken, chatId, nameQuery)
            }
            command in listOf("/list", "/commands", "/help", "/menu", "list", "help", "menu") -> handleHelpCommand(botToken, chatId)
            command.startsWith("/status") || command == "status" -> handleStatusCommand(botToken, chatId)
            command.startsWith("/mute") || command == "mute" -> handleMuteCommand(botToken, chatId)
            command.startsWith("/photo") || command.startsWith("/snap") -> {
                val preferFront = arg?.lowercase(Locale.ROOT) in listOf("front", "selfie", "user")
                handlePhotoCommand(botToken, chatId, preferFront)
            }
            command.startsWith("/record") || command.startsWith("/listen") -> {
                val durationSec = arg?.toIntOrNull() ?: 30
                handleRecordCommand(botToken, chatId, durationSec)
            }
            command.startsWith("/locate") || command.startsWith("/gps") -> handleLocateCommand(botToken, chatId)
            command.startsWith("/siren") -> handleSirenCommand(botToken, chatId, arg)
            command.startsWith("/wipe") -> handleWipeCommand(botToken, chatId)
            command.startsWith("/summary") || command.startsWith("/recap") -> handleSummaryCommand(botToken, chatId)
            command.startsWith("/metrics") || command.startsWith("/proof") || command.startsWith("/study_proof") -> handleMetricsCommand(botToken, chatId)
            command.startsWith("/tasks") || command == "tasks" -> handleTasksCommand(botToken, chatId)
            command.startsWith("/shield") || command.startsWith("/stealth") || command.startsWith("/class_shield") -> handleShieldCommand(botToken, chatId, arg)
            command.startsWith("/app") || command.startsWith("/open") -> handleOpenAppCommand(botToken, chatId, arg)
            command.startsWith("/play") -> handlePlayMediaCommand(botToken, chatId, parts.drop(1).joinToString(" "))
            command.startsWith("/arp") -> handleArpSentinelCommand(botToken, chatId)
            command.startsWith("/netscan") -> handleNetscanCommand(botToken, chatId)
            command.startsWith("/memory") -> handleMemoryCommand(botToken, chatId)
            command.startsWith("/fingerprint") || command == "fingerprint" -> handleFingerprintCommand(botToken, chatId)

            // ── MobileClaw Remote UI Automation Agent ──
            command.startsWith("/claw_tap") || command == "claw_tap" -> handleClawTapCommand(botToken, chatId, parts)
            command.startsWith("/claw_swipe") || command == "claw_swipe" -> handleClawSwipeCommand(botToken, chatId, parts)
            command.startsWith("/claw_type") || command == "claw_type" -> handleClawTypeCommand(botToken, chatId, parts.drop(1).joinToString(" "))
            command.startsWith("/claw_click") || command == "claw_click" -> handleClawClickCommand(botToken, chatId, parts.drop(1).joinToString(" "))
            command.startsWith("/claw_read") || command.startsWith("/claw_dump") || command == "claw_read" -> handleClawReadCommand(botToken, chatId)
            command in listOf("/claw_back", "claw_back") -> handleClawGlobalCommand(botToken, chatId, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK, "Back")
            command in listOf("/claw_home", "claw_home") -> handleClawGlobalCommand(botToken, chatId, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME, "Home")
            command in listOf("/claw_recents", "claw_recents") -> handleClawGlobalCommand(botToken, chatId, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS, "Recents")
            command in listOf("/claw_status", "/claw", "claw") -> handleClawStatusCommand(botToken, chatId)

            command.startsWith("/") -> {
                sendTelegramReply(
                    botToken,
                    chatId,
                    "⚠️ Unrecognized command: $command\nSend /list or /help to see all supported commands."
                )
            }
            else -> {
                // If user typed without slash (e.g. "locate" or "status")
                when (command) {
                    "locate", "gps" -> handleLocateCommand(botToken, chatId)
                    "record", "listen" -> handleRecordCommand(botToken, chatId, arg?.toIntOrNull() ?: 30)
                    "siren" -> handleSirenCommand(botToken, chatId, arg)
                    "photo", "snap" -> handlePhotoCommand(botToken, chatId, false)
                    "mute" -> handleMuteCommand(botToken, chatId)
                    "wipe" -> handleWipeCommand(botToken, chatId)
                    "arp" -> handleArpSentinelCommand(botToken, chatId)
                    "netscan" -> handleNetscanCommand(botToken, chatId)
                    "fingerprint" -> handleFingerprintCommand(botToken, chatId)
                    "memory" -> handleMemoryCommand(botToken, chatId)
                    "claw_read", "read_screen" -> handleClawReadCommand(botToken, chatId)
                    "claw_status" -> handleClawStatusCommand(botToken, chatId)
                    else -> {
                        sendTelegramReply(
                            botToken,
                            chatId,
                            "🤖 <b>[REMIX ENFORCER OS]</b>\nReceived query: <i>\"$commandText\"</i>\nStatus: <b>Active &amp; Responsive</b>\nSend <b>/list</b> to view available commands."
                        )
                    }
                }
            }
        }
    }

    private fun handleUnauthorizedStartCommand(botToken: String, chatId: String) {
        val welcome = """
            🤖 <b>[ENFORCER OS — AUTHENTICATION REQUIRED]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Your Telegram Chat ID: <code>$chatId</code>
            Status: ⛔ <b>Unauthorized / Unpaired</b>

            This device is protected by strict Chat ID whitelisting.
            To authorize commands from this chat, register Chat ID <code>$chatId</code> in the Mission Control Admin Panel.
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        sendTelegramReply(botToken, chatId, welcome)
    }

    private fun handleStartCommand(botToken: String, chatId: String) {
        val welcome = """
            🤖 <b>[ENFORCER OS REMOTE CONTROLLER]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Device connected and active!
            Your Telegram Chat ID: <code>$chatId</code>

            • Send <b>/list</b> to see all available remote commands.
            • Send <b>/connect</b> to bind this chat as your primary upload pipe destination for lecture recordings.
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        sendTelegramReply(botToken, chatId, welcome)
    }

    private fun handleConnectCommand(botToken: String, chatId: String) {
        telegramConfig.setCredentials(botToken, chatId)
        logEvent("🔗 Bound primary Cloud Pipe target to Chat ID: $chatId")
        val msg = """
            🔗 <b>[PRIMARY CLOUD PIPE LINKED]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Target Chat ID: <code>$chatId</code>
            All lecture audio chunks &amp; stealth audio recordings will now automatically stream to this chat!
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        sendTelegramReply(botToken, chatId, msg)
    }

    private fun handleOpenAppCommand(botToken: String, chatId: String, appName: String?) {
        if (appName.isNullOrBlank()) {
            sendTelegramReply(botToken, chatId, "⚠️ Please specify an app: e.g. <code>/app spotify</code> or <code>/app whatsapp</code>")
            return
        }
        val clean = appName.trim().lowercase(Locale.ROOT)
        val packageMap = mapOf(
            "spotify" to "com.spotify.music",
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "yt" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera",
            "settings" to "com.android.settings",
            "telegram" to "org.telegram.messenger"
        )
        val targetPkg = packageMap[clean] ?: clean
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(targetPkg)

        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            sendTelegramReply(botToken, chatId, "🚀 <b>[APP LAUNCHED]</b>\nSuccessfully opened <code>$clean</code> on Android screen.")
        } else {
            sendTelegramReply(botToken, chatId, "⚠️ Could not find launch intent for package <code>$targetPkg</code>. Check if installed.")
        }
    }

    private fun handlePlayMediaCommand(botToken: String, chatId: String, query: String) {
        if (query.isBlank()) {
            sendTelegramReply(botToken, chatId, "⚠️ Please specify search term: e.g. <code>/play synthwave</code>")
            return
        }
        val encoded = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            sendTelegramReply(botToken, chatId, "🎵 <b>[STREAMING]</b> Playing <code>$query</code> on Spotify.")
        } catch (e: Exception) {
            val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(ytIntent)
                sendTelegramReply(botToken, chatId, "▶️ <b>[STREAMING]</b> Playing <code>$query</code> on YouTube.")
            } catch (err: Exception) {
                sendTelegramReply(botToken, chatId, "⚠️ Media playback failed: ${err.message}")
            }
        }
    }

    private fun handleArpSentinelCommand(botToken: String, chatId: String) {
        val arpEntries = mutableMapOf<String, MutableList<String>>()
        var spoofingDetected = false
        try {
            val arpDoc = File("/proc/net/arp")
            if (arpDoc.exists()) {
                arpDoc.readLines().drop(1).forEach { line ->
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val mac = tokens[3].lowercase(Locale.ROOT)
                        if (mac != "00:00:00:00:00:00" && !mac.contains("<incomplete>")) {
                            arpEntries.getOrPut(mac) { mutableListOf() }.add(ip)
                        }
                    }
                }
                for ((_, ips) in arpEntries) {
                    if (ips.distinct().size > 1) {
                        spoofingDetected = true
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TelegramC2", "ARP check error: ${e.message}")
        }

        val statusText = if (spoofingDetected) {
            "🚨 <b>ALERT: Potential ARP Poisoning / MITM Detected!</b> Duplicate MAC entries found."
        } else {
            "🟢 <b>SECURE:</b> No ARP spoofing threats detected on current Wi-Fi interface (${arpEntries.size} inspected entries)."
        }
        sendTelegramReply(botToken, chatId, "🛡️ <b>[NETWORK SENTINEL AUDIT]</b>\n━━━━━━━━━━━━━━━━━━━━\n$statusText")
    }

    private fun handleNetscanCommand(botToken: String, chatId: String) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isWifi) {
            sendTelegramReply(
                botToken, chatId,
                "🌐 <b>[LOCAL NETWORK SCAN]</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                "⚠️ Device is not on Wi-Fi. Connect to a Wi-Fi network to scan the local subnet.\n" +
                "• Active Interface: Cellular / Mobile Data"
            )
            return
        }

        // Immediately confirm scan started
        sendTelegramReply(
            botToken, chatId,
            "🌐 <b>[SUBNET SCAN STARTED]</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
            "⚡ Sweeping Class-C subnet for live hosts... Results incoming."
        )

        scope.launch(Dispatchers.IO) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val dhcpInfo = wifiManager?.dhcpInfo

                if (dhcpInfo == null || dhcpInfo.gateway == 0) {
                    sendTelegramReply(botToken, chatId, "⚠️ <b>[SCAN FAILED]</b>\nCould not determine gateway IP. Ensure Wi-Fi is fully connected.")
                    return@launch
                }

                // Convert gateway int to subnet prefix (e.g. 192.168.1)
                val gatewayInt = dhcpInfo.gateway
                val o1 = gatewayInt and 0xff
                val o2 = (gatewayInt shr 8) and 0xff
                val o3 = (gatewayInt shr 16) and 0xff
                val subnetPrefix = "$o1.$o2.$o3"

                val myIpInt = wifiManager.connectionInfo?.ipAddress ?: 0
                val myIp = "${myIpInt and 0xff}.${(myIpInt shr 8) and 0xff}.${(myIpInt shr 16) and 0xff}.${(myIpInt shr 24) and 0xff}"

                val respondingHosts = mutableListOf<String>()
                val pingJobs = (1..30).map { suffix ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            val addr = java.net.InetAddress.getByName("$subnetPrefix.$suffix")
                            if (addr.isReachable(800)) {
                                synchronized(respondingHosts) {
                                    respondingHosts.add("$subnetPrefix.$suffix${if ("$subnetPrefix.$suffix" == myIp) " (This Device)" else ""}")
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
                pingJobs.forEach { it.join() }
                respondingHosts.sort()

                val hostList = if (respondingHosts.isEmpty()) {
                    "• No live hosts found in <code>$subnetPrefix.1-30</code>"
                } else {
                    respondingHosts.joinToString("\n") { "• <code>$it</code>" }
                }

                sendTelegramReply(
                    botToken, chatId,
                    "🌐 <b>[CLASS-C SUBNET SCAN COMPLETE]</b>\n━━━━━━━━━━━━━━━━━━━━\n" +
                    "• Subnet Scanned: <code>$subnetPrefix.1-30</code>\n" +
                    "• My IP: <code>$myIp</code>\n" +
                    "• Interface: Wi-Fi (High-Speed)\n" +
                    "• Responding Hosts (${respondingHosts.size}):\n$hostList\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    "<i>Network Sentinel Active. Run /arp for ARP spoofing audit.</i>"
                )
            } catch (e: Exception) {
                Log.e("TelegramC2", "Netscan error", e)
                sendTelegramReply(botToken, chatId, "⚠️ <b>[SCAN ERROR]</b>\nSubnet scan failed: ${e.message}")
            }
        }
    }

    private fun handleSignupCommand(botToken: String, chatId: String, nameQuery: String) {
        val deviceInfo = com.example.core.telemetry.DeviceInfoProvider.getInstance(context)
        if (nameQuery.isBlank()) {
            val currentName = deviceInfo.getUserName()
            val currentUid = deviceInfo.getUserId()
            val devUuid = deviceInfo.getOrGenerateDeviceUuid()
            val msg = """
                👤 <b>[USER SIGNUP &amp; REGISTRATION]</b>
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                • <b>Active User</b>: $currentName
                • <b>Assigned User ID</b>: <code>$currentUid</code>
                • <b>Device Node</b>: <code>$devUuid</code>
                
                💡 <i>To sign up with a new name, send:</i>
                <code>/signup Your Name</code> (e.g. <code>/signup Alex Vance</code>)
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            """.trimIndent()
            sendTelegramReply(botToken, chatId, msg)
            return
        }

        val newUid = deviceInfo.registerUser(name = nameQuery.trim(), role = "Field Agent")
        val devUuid = deviceInfo.getOrGenerateDeviceUuid()
        logEvent("📝 User registered: \"$nameQuery\" -> Assigned ID: $newUid")

        val confirm = """
            ✅ <b>[USER REGISTERED IN DATABASE]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            • <b>User Name</b>: <b>$nameQuery</b>
            • <b>Generated User ID</b>: <code>$newUid</code>
            • <b>Bound Device Node</b>: <code>$devUuid</code>
            • <b>Assigned Role</b>: Field Agent
            • <b>Health Status</b>: HEALTHY (Online)
            
            All future telemetry, battery logs, actions and C2 commands from this device are now tied to <code>$newUid</code>.
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        sendTelegramReply(botToken, chatId, confirm)
    }

    private fun handleStatusCommand(botToken: String, chatId: String) {
        val deviceInfo = com.example.core.telemetry.DeviceInfoProvider.getInstance(context)
        val userName = deviceInfo.getUserName()
        val userId = deviceInfo.getUserId()
        val userRole = deviceInfo.getUserRole()
        val devUuid = deviceInfo.getOrGenerateDeviceUuid()

        // Battery status
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = bm?.isCharging == true

        // Network status
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val netType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi (High-Speed)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular / 5G Mobile Data"
            else -> "Connected (Other)"
        }

        // Audio Ringer status
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerText = when (am?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent Mode 🔕"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate Mode 📳"
            AudioManager.RINGER_MODE_NORMAL -> "Normal Ringing 🔔"
            else -> "Unknown"
        }

        // Storage / Cache status
        val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
        val audioFiles = cacheFiles.filter { it.name.endsWith(".m4a") || it.name.endsWith(".aac") }
        val cacheBytes = audioFiles.sumOf { it.length() }
        val cacheMb = cacheBytes / (1024.0 * 1024.0)

        val freeSpaceBytes = Environment.getDataDirectory().freeSpace
        val freeSpaceGb = freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)

        val wipedCount = telegramConfig.wipedFilesCount.value
        val wipedMb = telegramConfig.totalBytesWiped.value / (1024.0 * 1024.0)

        val statusCard = """
            📱 <b>[ENFORCER OS // SYSTEM STATUS]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            👤 <b>User:</b> $userName (<code>$userId</code>)
            🎖️ <b>Role:</b> $userRole
            🆔 <b>Device Node:</b> <code>$devUuid</code>
            🔋 <b>Battery:</b> ${if (batteryPct >= 0) "$batteryPct%" else "Unknown"} ${if (isCharging) "⚡ Charging" else "🔋 On Battery"}
            📶 <b>Network:</b> $netType
            🔊 <b>Audio Profile:</b> $ringerText
            💾 <b>Cache Storage:</b> ${String.format(Locale.ROOT, "%.2f", cacheMb)} MB (${audioFiles.size} temp files)
            💽 <b>Device Free Space:</b> ${String.format(Locale.ROOT, "%.1f", freeSpaceGb)} GB Available
            ☁️ <b>Cloud Pipe Delivered:</b> $wipedCount chunks (${String.format(Locale.ROOT, "%.1f", wipedMb)} MB wiped)
            ⚡ <b>Remote Listener:</b> Active &amp; Responsive
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()

        sendTelegramReply(botToken, chatId, statusCard)

        // Real-time sync hardware telemetry to Firestore for Admin Dashboard
        try {
            com.example.core.cloud.FirestoreSyncManager.getInstance(context).syncRealTelemetryFromHardware(0, 0)
        } catch (_: Exception) {}
    }

    private fun handleMuteCommand(botToken: String, chatId: String) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                val streams = listOf(
                    AudioManager.STREAM_RING,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.STREAM_SYSTEM
                )
                streams.forEach { stream ->
                    try {
                        am.setStreamVolume(stream, 0, 0)
                    } catch (_: Exception) {}
                }
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
                sendTelegramReply(
                    botToken,
                    chatId,
                    "🔇 <b>[FORCE HARD MUTE SUCCESS]</b>\nAll phone audio channels (Ring, Notification, Media, System) locked to 0% for class."
                )
            } catch (e: Exception) {
                sendTelegramReply(
                    botToken,
                    chatId,
                    "⚠️ Mute partially applied: ${e.message}"
                )
            }
        }
    }

    private fun handleSirenCommand(botToken: String, chatId: String, durationArg: String?) {
        val seconds = durationArg?.toIntOrNull()?.coerceIn(5, 60) ?: 20
        scope.launch(Dispatchers.Default) {
            sirenController.startSiren(this)
            flashTorch(seconds)
            delay(seconds * 1000L)
            sirenController.stopSiren()
        }

        sendTelegramReply(
            botToken,
            chatId,
            "🚨 <b>[LOCATOR BEACON ACTIVE]</b>\n100% Volume emergency locator siren and flashlight strobe activated for ${seconds}s."
        )
    }

    private fun handleWipeCommand(botToken: String, chatId: String) {
        var count = 0
        var bytesFreed = 0L

        try {
            val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
            for (f in cacheFiles) {
                if (f.name.endsWith(".m4a") || f.name.endsWith(".aac") || f.name.startsWith("chunk_") || f.name.startsWith("bin_")) {
                    bytesFreed += f.length()
                    if (f.delete()) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramC2", "Error during cache wipe", e)
        }

        val freedMb = bytesFreed / (1024.0 * 1024.0)
        sendTelegramReply(
            botToken,
            chatId,
            "🧹 <b>[EMERGENCY CACHE PURGE]</b>\nInstantly wiped $count temporary cached audio chunks.\nFreed ${String.format(Locale.ROOT, "%.2f", freedMb)} MB from internal app storage."
        )
    }

    private fun handlePhotoCommand(botToken: String, chatId: String, preferFront: Boolean) {
        val facingLabel = if (preferFront) "Front (Selfie)" else "Rear (Main)"
        logEvent("📸 Initiating snapshot: $facingLabel camera...")
        sendTelegramReply(
            botToken,
            chatId,
            "📸 <b>[CAPTURING SNAPSHOT]</b>\nActivating $facingLabel camera sensor for remote environment capture..."
        )

        CameraSnapshotHelper.takeSnapshot(context, preferFront) { photoFile, errorMsg ->
            if (errorMsg != null || photoFile == null) {
                logEvent("❌ Snapshot failed: $errorMsg")
                sendTelegramReply(
                    botToken,
                    chatId,
                    "⚠️ <b>[SNAPSHOT FAILED]</b>\nUnable to capture frame: $errorMsg\nEnsure camera permission is allowed on device."
                )
            } else {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val timeStr = SimpleDateFormat("dd MMM yyyy \u2022 hh:mm:ss a", Locale.getDefault()).format(Date())
                // Plain text caption — Telegram sendPhoto is stricter than sendMessage with HTML
                val caption = "[📸 ENFORCER REMOTE SNAPSHOT]\n" +
                    "Facing: $facingLabel\n" +
                    "Captured: $timeStr\n" +
                    "Battery: ${if (batteryPct >= 0) "$batteryPct%" else "N/A"}\n" +
                    "Size: ${photoFile.length() / 1024} KB"

                sendTelegramPhoto(botToken, chatId, photoFile, caption)
            }
        }
    }

    private fun sendTelegramPhoto(botToken: String, chatId: String, photoFile: File, caption: String) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)  // Plain text — no parse_mode to avoid HTML parse errors
            .addFormDataPart("disable_notification", "true")
            .addFormDataPart(
                "photo",
                photoFile.name,
                photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendPhoto")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e("TelegramC2", "Send photo failed: ${e.message}")
                logEvent("❌ Failed delivering photo to Telegram: ${e.message}")
                sendTelegramReply(botToken, chatId, "⚠️ <b>[PHOTO DISPATCH FAILED]</b>\nNetwork error: ${e.message}")
                try { photoFile.delete() } catch (_: Exception) {}
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val isOk = response.isSuccessful
                val body = response.body?.string() ?: ""
                response.close()
                if (isOk) {
                    logEvent("📤 Snapshot photo delivered to Telegram ($chatId)!")
                } else {
                    Log.e("TelegramC2", "sendPhoto returned HTTP ${response.code}: $body")
                    logEvent("❌ Send photo error: ${response.code}")
                    sendTelegramReply(botToken, chatId, "⚠️ <b>[TELEGRAM SENDPHOTO ERROR ${response.code}]</b>\n${body.take(80)}")
                }
                try { photoFile.delete() } catch (_: Exception) {}
            }
        })
    }

    private fun handleRecordCommand(botToken: String, chatId: String, requestedSeconds: Int) {
        val clampedSec = requestedSeconds.coerceIn(5, 120)
        logEvent("🎙️ Initiating remote audio capture: ${clampedSec}s...")
        sendTelegramReply(
            botToken,
            chatId,
            "🎙️ <b>[REMOTE AUDIO CAPTURE]</b>\nRecording ${clampedSec}s ambient audio snippet on device microphone...\nAudio will be dispatched immediately once completed."
        )

        AudioSnippetHelper.recordSnippet(
            context = context,
            durationSeconds = clampedSec,
            onProgress = { /* quiet progress */ },
            onResult = { audioFile, errorMsg ->
                if (errorMsg != null || audioFile == null) {
                    logEvent("❌ Remote audio capture failed: $errorMsg")
                    sendTelegramReply(
                        botToken,
                        chatId,
                        "⚠️ <b>[AUDIO CAPTURE FAILED]</b>\nUnable to record snippet: $errorMsg\nEnsure microphone permission is granted."
                    )
                } else {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                    val timeStr = SimpleDateFormat("dd MMM yyyy • hh:mm:ss a", Locale.getDefault()).format(Date())
                    val caption = """
                        🎙️ <b>[ENFORCER REMOTE AUDIO SNIPPET]</b>
                        ━━━━━━━━━━━━━━━━━━━━
                        • <b>Duration</b>: ${clampedSec}s
                        • <b>Captured</b>: $timeStr
                        • <b>Battery</b>: ${if (batteryPct >= 0) "$batteryPct%" else "N/A"}
                        • <b>File Size</b>: ${(audioFile.length() / 1024)} KB (.m4a)
                        ━━━━━━━━━━━━━━━━━━━━
                    """.trimIndent()

                    sendTelegramAudio(botToken, chatId, audioFile, caption, clampedSec)
                }
            }
        )
    }

    private fun sendTelegramAudio(botToken: String, chatId: String, audioFile: File, caption: String, durationSec: Int) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", caption)
            .addFormDataPart("parse_mode", "HTML")
            .addFormDataPart("title", "Remote Ambient Capture (${durationSec}s)")
            .addFormDataPart("performer", "Enforcer OS Remote Mic")
            .addFormDataPart(
                "audio",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendAudio")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e("TelegramC2", "Send audio snippet failed: ${e.message}")
                logEvent("❌ Failed delivering audio snippet to Telegram: ${e.message}")
                sendTelegramReply(botToken, chatId, "⚠️ <b>[AUDIO DISPATCH FAILED]</b>\nNetwork error: ${e.message}")
                try { audioFile.delete() } catch (_: Exception) {}
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val isOk = response.isSuccessful
                val body = response.body?.string() ?: ""
                response.close()
                if (isOk) {
                    logEvent("📤 Audio snippet (${durationSec}s) delivered to Telegram ($chatId)!")
                } else {
                    Log.e("TelegramC2", "sendAudio returned HTTP ${response.code}: $body")
                    logEvent("❌ Send audio error: ${response.code}")
                    sendTelegramReply(botToken, chatId, "⚠️ <b>[TELEGRAM SENDAUDIO ERROR ${response.code}]</b>\n${body.take(80)}")
                }
                try { audioFile.delete() } catch (_: Exception) {}
            }
        })
    }

    private fun handleLocateCommand(botToken: String, chatId: String) {
        logEvent("📍 Querying device GPS & network location...")
        sendTelegramReply(
            botToken,
            chatId,
            "📍 <b>[QUERYING GPS BEACON]</b>\nAcquiring real-time satellite fix &amp; cell tower triangulation..."
        )

        GpsLocationHelper.requestLocation(context) { report, errorMsg ->
            if (errorMsg != null || report == null) {
                logEvent("❌ Location query failed: $errorMsg")
                sendTelegramReply(
                    botToken,
                    chatId,
                    "⚠️ <b>[GPS BEACON FAILED]</b>\n$errorMsg\nVerify Location permission and device GPS toggles are enabled."
                )
            } else {
                logEvent("📍 Location acquired: ${report.latitude}, ${report.longitude} (±${report.accuracyMeters.toInt()}m)")
                val altText = report.altitudeMeters?.let { "• <b>Altitude</b>: ${it.toInt()}m\n" } ?: ""
                val spdText = report.speedKmh?.let { "• <b>Speed</b>: ${it.toInt()} km/h\n" } ?: ""
                val batteryText = if (report.batteryPct >= 0) "${report.batteryPct}%" else "N/A"

                val locationMessage = """
                    📍 <b>[ENFORCER LIVE GPS BEACON]</b>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    • <b>Coordinates</b>: <code>${report.latitude}, ${report.longitude}</code>
                    • <b>Accuracy</b>: ±${report.accuracyMeters.toInt()}m (${report.provider.uppercase()})
                    $altText$spdText• <b>Battery</b>: $batteryText
                    • <b>Fix Time</b>: ${report.timeString}

                    🗺️ <a href="${report.googleMapsUrl}">Open in Google Maps</a>
                    🧭 <a href="${report.openStreetMapUrl}">Open in OpenStreetMap</a>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """.trimIndent()

                sendTelegramReply(botToken, chatId, locationMessage)

                // Real-time sync GPS fix to Firestore locations/latest for Admin Dashboard
                try {
                    com.example.core.cloud.FirestoreSyncManager.getInstance(context).syncLocation(
                        latitude = report.latitude,
                        longitude = report.longitude,
                        accuracyMeters = report.accuracyMeters,
                        locationLabel = "Telegram Remote C2 Beacon (${report.provider.uppercase()})"
                    )
                } catch (e: Exception) {
                    Log.w("TelegramC2", "Failed to sync GPS to Firestore: ${e.message}")
                }
            }
        }
    }

    private fun handleSummaryCommand(botToken: String, chatId: String) {
        logEvent("📊 Generating System Intelligence Summary for Telegram...")
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                val allTasks = db.voiceTaskDao().getAllTasksDirect()

                val pendingTasks = allTasks.filter { !it.isCompleted }
                val completedCount = allTasks.count { it.isCompleted }

                val taskLines = if (pendingTasks.isEmpty()) "• <i>All tasks complete</i>" else pendingTasks.take(5).joinToString("\n") { "• ${it.title} [${it.urgencyLevel}]" }

                val summaryMessage = """
                    📊 <b>[SECONDARY BRAIN 2.0 // INTELLIGENCE SUMMARY]</b>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    Total Tasks &amp; Notes: <b>${allTasks.size}</b> (${completedCount} Completed)
                    Active Pending: <b>${pendingTasks.size}</b>

                    ⚡ <b>Pending Action Items:</b>
                    $taskLines
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    <i>Generated from Secondary Brain 2.0 Real-Time Engine</i>
                """.trimIndent()

                sendTelegramReply(botToken, chatId, summaryMessage)
            } catch (e: Exception) {
                sendTelegramReply(botToken, chatId, "⚠️ Failed to compile summary: ${e.message}")
            }
        }
    }

    private fun handleTasksCommand(botToken: String, chatId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                val pendingTasks = db.voiceTaskDao().getAllTasksDirect().filter { !it.isCompleted }

                if (pendingTasks.isEmpty()) {
                    sendTelegramReply(
                        botToken,
                        chatId,
                        "✅ <b>[ZERO PENDING TASKS]</b>\nAll secondary brain tasks and notes are complete!"
                    )
                    return@launch
                }

                val listText = pendingTasks.joinToString("\n\n") { task ->
                    val badge = when (task.category) {
                        "IMPORTANT_TEST" -> "🎯 [PRIORITY]"
                        "DUE_DATE" -> "⏰ [DEADLINE]"
                        else -> "📝 [TASK]"
                    }
                    "$badge <b>${task.title}</b>\n<i>Source: ${task.lectureTitle} • Urgency: ${task.urgencyLevel}</i>"
                }

                val msg = """
                    📋 <b>[PENDING SECONDARY BRAIN TASKS (${pendingTasks.size})]</b>
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    $listText
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                """.trimIndent()

                sendTelegramReply(botToken, chatId, msg)
            } catch (e: Exception) {
                sendTelegramReply(botToken, chatId, "⚠️ Failed to fetch tasks: ${e.message}")
            }
        }
    }

    private fun handleShieldCommand(botToken: String, chatId: String, arg: String?) {
        val safetyManager = com.example.core.alarm.AlarmSafetyManager.getInstance(context)
        val enable = when (arg?.lowercase(Locale.ROOT)) {
            "on", "1", "enable", "arm" -> true
            "off", "0", "disable", "disarm" -> false
            else -> !safetyManager.isClassShieldActive.value
        }

        safetyManager.setClassShieldActive(enable)
        val statusText = if (enable) "🛡️ <b>[STEALTH FOCUS SHIELD ARMED]</b>\nAudio streams muted and suppressed for maximum stealth."
                         else "🔓 <b>[STEALTH FOCUS SHIELD DISARMED]</b>\nNormal audio and notification schedules restored."

        sendTelegramReply(botToken, chatId, statusText)
    }

    private fun handleMetricsCommand(botToken: String, chatId: String) {
        logEvent("📊 Generating System Metrics Report for Telegram...")
        scope.launch(Dispatchers.IO) {
            try {
                val studyProofManager = com.example.core.discipline.StudyProofManager.getInstance(context)
                val report = studyProofManager.generateStudyProofReport()
                val cleanCard = report.formattedCardText
                    .replace("ACADEMIC ACCOUNTABILITY AUDIT", "SYSTEM PERFORMANCE & METRICS AUDIT")
                    .replace("Coaching / Safe-Zone", "Sanctum Safe-Zone")
                    .replace("Target Exam", "Milestone Target")
                sendTelegramReply(botToken, chatId, cleanCard)
            } catch (e: Exception) {
                sendTelegramReply(botToken, chatId, "⚠️ Failed to compile metrics report: ${e.message}")
            }
        }
    }

    private fun handleMemoryCommand(botToken: String, chatId: String) {
        val memoryCard = """
            🧠 <b>[MEM0 SECONDARY BRAIN MEMORY CORE]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            • <b>User Entity</b>: Harsh
            • <b>Architecture</b>: Mem0 Semantic / Vector Memory Layer
            • <b>Sync Status</b>: Real-Time Firestore &amp; SQLite Dual-Storage
            • <b>Cloud Host</b>: Render Cloud Infrastructure
            • <b>Status</b>: Active &amp; Autonomous
            • <b>Features</b>: Auto-Reflection, Fact Extraction, Multi-Turn Context
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        """.trimIndent()
        sendTelegramReply(botToken, chatId, memoryCard)
    }

    private fun handleFingerprintCommand(botToken: String, chatId: String) {
        logEvent("🔐 Biometric verification challenge dispatched via Telegram...")

        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            val reason = when (canAuthenticate) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available on this device."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware temporarily unavailable."
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled. Please set up fingerprint in device Settings."
                else -> "Biometric authentication not available (code: $canAuthenticate)."
            }
            sendTelegramReply(
                botToken, chatId,
                "🔐 <b>[BIOMETRIC GATE]</b>\n⚠️ Cannot launch biometric prompt:\n• $reason"
            )
            return
        }

        sendTelegramReply(
            botToken, chatId,
            "🔐 <b>[BIOMETRIC GATE ACTIVATED]</b>\nFingerprint / face verification challenge launched on device. Awaiting physical authentication..."
        )

        Handler(Looper.getMainLooper()).post {
            try {
                val activity = context as? FragmentActivity
                if (activity == null || activity.isFinishing || activity.isDestroyed) {
                    sendTelegramReply(botToken, chatId, "⚠️ <b>[BIOMETRIC GATE]</b>\nApp must be open and in foreground for biometric prompt. Please open the app and retry.")
                    return@post
                }

                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            logEvent("✅ Biometric verification PASSED")
                            sendTelegramReply(
                                botToken, chatId,
                                "✅ <b>[BIOMETRIC GATE PASSED]</b>\n🔓 Identity verified successfully.\n• Result: <b>Authentication Granted</b>\n• Method: ${result.authenticationType.let { if (it == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC) "Fingerprint / Face" else "Device Credential" }}"
                            )
                        }

                        override fun onAuthenticationFailed() {
                            logEvent("⚠️ Biometric verification attempt failed")
                            sendTelegramReply(
                                botToken, chatId,
                                "⚠️ <b>[BIOMETRIC GATE — ATTEMPT FAILED]</b>\nFingerprint not recognized. Retrying..."
                            )
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            logEvent("❌ Biometric authentication error ($errorCode): $errString")
                            sendTelegramReply(
                                botToken, chatId,
                                "❌ <b>[BIOMETRIC GATE ERROR]</b>\nError $errorCode: $errString"
                            )
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("🔐 Remix Enforcer OS — Identity Gate")
                    .setSubtitle("Remote biometric verification requested via Telegram C2")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                prompt.authenticate(promptInfo)
            } catch (e: Exception) {
                Log.e("TelegramC2", "Biometric prompt error", e)
                sendTelegramReply(botToken, chatId, "⚠️ <b>[BIOMETRIC GATE ERROR]</b>\n${e.message}")
            }
        }
    }

    private fun handleHelpCommand(botToken: String, chatId: String) {
        val helpText = """
            🛠️ <b>[SECONDARY BRAIN 2.0 // COMMAND LIST]</b>
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            • <code>/list</code> or <code>/help</code> - Show full command list
            • <code>/status</code> - Real-time battery, temp, Wi-Fi, audio &amp; storage telemetry
            • <code>/locate</code> or <code>/gps</code> - GPS satellite fix &amp; Google Maps beacon
            • <code>/photo [front|back]</code> - Remote camera snapshot sent directly to chat
            • <code>/record [sec]</code> - Remote mic ambient audio snippet delivered to chat
            • <code>/siren [sec]</code> - 100% volume beacon &amp; torch strobe locator
            • <code>/mute</code> - Force silent mode across all device channels
            • <code>/app [name]</code> - Launch app on device screen (e.g. <code>/app spotify</code>)
            • <code>/play [query]</code> - Stream music / audio on Spotify or YouTube
            • <code>/arp</code> - Network Sentinel: Check for ARP spoofing / MITM threats
            • <code>/netscan</code> - Real Class-C subnet sweep of connected Wi-Fi network
            • <code>/fingerprint</code> - Launch biometric identity verification on device
            • <code>/memory</code> - Query Mem0 Secondary Brain long-term memory
            • <code>/tasks</code> - List pending secondary brain tasks
            • <code>/summary</code> - Daily recap of voice notes and telemetry
            • <code>/metrics</code> - System performance and usage metrics report
            • <code>/shield [on|off]</code> - Toggle stealth focus mode
            • <code>/wipe</code> - Emergency purge of local audio cache
            • <code>/connect</code> - Bind this chat as primary upload destination

            🤖 <b>[MOBILECLAW AUTOMATION AGENT]</b>
            • <code>/claw_read</code> - Screen inspection &amp; interactive element dump
            • <code>/claw_tap &lt;x&gt; &lt;y&gt;</code> - Dispatch screen tap at coordinates
            • <code>/claw_swipe &lt;x1&gt; &lt;y1&gt; &lt;x2&gt; &lt;y2&gt;</code> - Dispatch swipe gesture
            • <code>/claw_type &lt;text&gt;</code> - Type text into focused input field
            • <code>/claw_click &lt;text|id&gt;</code> - Find &amp; click UI element
            • <code>/claw_back</code> | <code>/claw_home</code> | <code>/claw_recents</code> - Global navigation
            • <code>/claw_status</code> - MobileClaw agent service health
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            <i>Instant Auto-Responder Active • Responds to commands from any sender</i>
        """.trimIndent()
        sendTelegramReply(botToken, chatId, helpText)
    }

    private fun flashTorch(seconds: Int) {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        scope.launch(Dispatchers.Default) {
            val cameraId = try {
                cm.cameraIdList.firstOrNull()
            } catch (_: Exception) {
                null
            } ?: return@launch

            val end = System.currentTimeMillis() + (seconds * 1000L)
            var torchOn = false
            while (System.currentTimeMillis() < end && isActive) {
                try {
                    torchOn = !torchOn
                    cm.setTorchMode(cameraId, torchOn)
                } catch (_: Exception) {}
                delay(300L)
            }
            try {
                cm.setTorchMode(cameraId, false)
            } catch (_: Exception) {}
        }
    }

    // ─── MobileClaw Agent Remote Handlers ─────────────────────────────────────

    private fun handleClawTapCommand(botToken: String, chatId: String, parts: List<String>) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(
                botToken,
                chatId,
                "⚠️ <b>MobileClaw Inactive</b>\nAccessibility Service is not enabled.\nGo to Android Settings &gt; Accessibility &gt; Enable <i>Secondary Brain (MobileClaw)</i>."
            )
            return
        }

        val x = parts.getOrNull(1)?.toFloatOrNull()
        val y = parts.getOrNull(2)?.toFloatOrNull()

        if (x == null || y == null) {
            sendTelegramReply(
                botToken,
                chatId,
                "⚠️ Usage: <code>/claw_tap &lt;x&gt; &lt;y&gt;</code>\nExample: <code>/claw_tap 540 1200</code>"
            )
            return
        }

        val dispatched = service.tap(x, y) { success ->
            if (success) {
                logEvent("✓ MobileClaw tap callback confirmed at ($x, $y)")
            }
        }

        sendTelegramReply(
            botToken,
            chatId,
            if (dispatched) "🎯 <b>[MobileClaw]</b> Tap dispatched at <code>($x, $y)</code>"
            else "❌ <b>[MobileClaw]</b> Tap dispatch failed at <code>($x, $y)</code>"
        )
    }

    private fun handleClawSwipeCommand(botToken: String, chatId: String, parts: List<String>) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(
                botToken,
                chatId,
                "⚠️ <b>MobileClaw Inactive</b>\nPlease enable Accessibility Service in Android Settings."
            )
            return
        }

        val x1 = parts.getOrNull(1)?.toFloatOrNull()
        val y1 = parts.getOrNull(2)?.toFloatOrNull()
        val x2 = parts.getOrNull(3)?.toFloatOrNull()
        val y2 = parts.getOrNull(4)?.toFloatOrNull()
        val dur = parts.getOrNull(5)?.toLongOrNull() ?: 300L

        if (x1 == null || y1 == null || x2 == null || y2 == null) {
            sendTelegramReply(
                botToken,
                chatId,
                "⚠️ Usage: <code>/claw_swipe &lt;x1&gt; &lt;y1&gt; &lt;x2&gt; &lt;y2&gt; [duration_ms]</code>\nExample: <code>/claw_swipe 500 1500 500 500 300</code>"
            )
            return
        }

        val dispatched = service.swipe(x1, y1, x2, y2, dur) { success ->
            if (success) logEvent("✓ MobileClaw swipe completed")
        }

        sendTelegramReply(
            botToken,
            chatId,
            if (dispatched) "👉 <b>[MobileClaw]</b> Swipe from <code>($x1, $y1)</code> to <code>($x2, $y2)</code> (${dur}ms)"
            else "❌ <b>[MobileClaw]</b> Swipe dispatch failed"
        )
    }

    private fun handleClawTypeCommand(botToken: String, chatId: String, text: String) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(botToken, chatId, "⚠️ <b>MobileClaw Inactive</b>\nPlease enable Accessibility Service.")
            return
        }

        if (text.isBlank()) {
            sendTelegramReply(botToken, chatId, "⚠️ Usage: <code>/claw_type &lt;text to input&gt;</code>")
            return
        }

        val typed = service.typeText(text)
        sendTelegramReply(
            botToken,
            chatId,
            if (typed) "⌨️ <b>[MobileClaw]</b> Typed: <i>\"$text\"</i>"
            else "⚠️ <b>[MobileClaw]</b> Type attempted (focused input field not found or protected)"
        )
    }

    private fun handleClawClickCommand(botToken: String, chatId: String, target: String) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(botToken, chatId, "⚠️ <b>MobileClaw Inactive</b>\nPlease enable Accessibility Service.")
            return
        }

        if (target.isBlank()) {
            sendTelegramReply(botToken, chatId, "⚠️ Usage: <code>/claw_click &lt;text or view_id&gt;</code>")
            return
        }

        val clicked = if (target.contains(":id/")) {
            service.clickByViewId(target)
        } else {
            service.clickByText(target)
        }

        sendTelegramReply(
            botToken,
            chatId,
            if (clicked) "🖱️ <b>[MobileClaw]</b> Clicked element matching <i>\"$target\"</i>"
            else "⚠️ <b>[MobileClaw]</b> No clickable element found matching <i>\"$target\"</i>"
        )
    }

    private fun handleClawReadCommand(botToken: String, chatId: String) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(
                botToken,
                chatId,
                "⚠️ <b>MobileClaw Inactive</b>\nPlease enable Secondary Brain Accessibility Service in Android Settings to inspect screen hierarchy."
            )
            return
        }

        val summary = service.summarizeScreen()
        sendTelegramReply(botToken, chatId, summary, isHtml = false)
    }

    private fun handleClawGlobalCommand(botToken: String, chatId: String, action: Int, actionName: String) {
        val service = com.example.core.claw.MobileClawAccessibilityService.instance
        if (service == null) {
            sendTelegramReply(botToken, chatId, "⚠️ <b>MobileClaw Inactive</b>\nPlease enable Accessibility Service.")
            return
        }

        val ok = service.performGlobalAction(action)
        sendTelegramReply(
            botToken,
            chatId,
            if (ok) "⚡ <b>[MobileClaw]</b> Global action <b>$actionName</b> executed."
            else "❌ <b>[MobileClaw]</b> Global action $actionName failed."
        )
    }

    private fun handleClawStatusCommand(botToken: String, chatId: String) {
        val active = com.example.core.claw.MobileClawAccessibilityService.isRunning()
        val pkg = com.example.core.claw.MobileClawAccessibilityService.instance?.let {
            it.dumpHierarchyJson().optString("package", "Unknown")
        } ?: "N/A"

        val msg = """
            🤖 <b>MobileClaw Agent Automation Status</b>
            • Service Running: <b>${if (active) "🟢 ACTIVE" else "🔴 INACTIVE"}</b>
            • Active Package: <code>$pkg</code>

            <b>Supported Remote Commands:</b>
            • <code>/claw_read</code> - Inspect active screen elements
            • <code>/claw_tap &lt;x&gt; &lt;y&gt;</code> - Dispatch screen tap
            • <code>/claw_swipe &lt;x1&gt; &lt;y1&gt; &lt;x2&gt; &lt;y2&gt;</code> - Dispatch swipe
            • <code>/claw_type &lt;text&gt;</code> - Type into focused field
            • <code>/claw_click &lt;text&gt;</code> - Click element by text/id
            • <code>/claw_back</code> | <code>/claw_home</code> | <code>/claw_recents</code>
        """.trimIndent()
        sendTelegramReply(botToken, chatId, msg)
    }

    private fun sendTelegramReply(botToken: String, chatId: String, messageText: String, isHtml: Boolean = true) {
        if (botToken.isBlank() || chatId.isBlank()) {
            logEvent("⚠️ Cannot reply: botToken or chatId is blank")
            return
        }

        val requestBodyBuilder = FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", messageText)

        if (isHtml) {
            requestBodyBuilder.add("parse_mode", "HTML")
        }

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendMessage")
            .post(requestBodyBuilder.build())
            .build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e("TelegramC2", "Reply failed: ${e.message}")
                logEvent("❌ Reply network failure: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val isOk = response.isSuccessful
                val code = response.code
                val body = response.body?.string() ?: ""
                response.close()
                if (isOk) {
                    logEvent("📤 Sent reply to $chatId: ${messageText.take(35).replace("<[^>]*>".toRegex(), "")}...")
                } else {
                    Log.e("TelegramC2", "sendMessage returned HTTP $code: $body")
                    if (isHtml) {
                        // Retry with plain text stripped of HTML tags
                        val plainText = messageText.replace("<[^>]*>".toRegex(), "")
                        sendTelegramReply(botToken, chatId, plainText, isHtml = false)
                    } else {
                        logEvent("❌ Telegram send error ($code): ${body.take(40)}")
                    }
                }
            }
        })
    }

    private fun logEvent(msg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timestamp] $msg"
        val current = _commandLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 20) {
            current.removeAt(current.size - 1)
        }
        _commandLogs.value = current
    }

    companion object {
        @Volatile
        private var INSTANCE: TelegramC2Manager? = null

        fun getInstance(context: Context): TelegramC2Manager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelegramC2Manager(context.applicationContext).also {
                    INSTANCE = it
                    it.startPolling()
                }
            }
        }
    }
}
