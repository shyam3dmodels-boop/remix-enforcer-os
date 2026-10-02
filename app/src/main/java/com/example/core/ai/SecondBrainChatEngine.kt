package com.example.core.ai

import android.content.Context
import android.util.Log
import com.example.MainApplication
import com.example.core.cloud.FirestoreSyncManager
import com.example.core.recorder.GroqAiManager
import com.example.core.recorder.TelegramConfigManager
import com.example.data.local.entity.AiChatMessageEntity
import com.example.data.local.entity.AiChatSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import com.example.core.alarm.SmartAlarmScheduler
import com.example.core.recorder.GpsLocationHelper
import com.example.data.local.entity.VoiceTaskEntity
import java.util.regex.Pattern

/**
 * Universal Multi-Provider AI Intelligence Engine for Secondary Brain 2.0.
 *
 * Supported Providers & Models:
 * - Anthropic Claude: claude-3-5-sonnet, claude-3-5-haiku, claude-3-opus
 * - Google Gemini: gemini-2.0-flash, gemini-1.5-pro, gemini-1.5-flash
 * - DeepSeek: deepseek-chat (V3), deepseek-reasoner (R1)
 * - NVIDIA NIM: nvidia/llama-3.1-nemotron-70b-instruct
 * - OpenRouter: anthropic/claude-3.5-sonnet, deepseek/deepseek-r1
 * - Groq: llama-3.3-70b-versatile, qwen/qwen3.8-27b
 *
 * Multi-Tier Dual Storage Architecture:
 * - Tier 1: Local Offline-First Room DB (SQLite)
 * - Tier 2: Dedicated Cloud SQLite Backend Server (/api/chat/sessions)
 * - Tier 3: Firebase Cloud Firestore (/ai_conversations)
 * - Tier 4: Telegram Indestructible Cloud Archive
 */
class SecondBrainChatEngine private constructor(private val context: Context) {

    private val repository = (context.applicationContext as MainApplication).repository
    private val firestoreSync = FirestoreSyncManager.getInstance(context)
    private val groqManager = GroqAiManager.getInstance(context)
    private val keySyncManager = AiKeySyncManager.getInstance(context)
    private val telegramConfig = TelegramConfigManager.getInstance(context)
    private val alarmScheduler = SmartAlarmScheduler(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun processUserMessage(
        sessionId: String = "default_session",
        sessionTitle: String = "Conversation",
        provider: String = "claude",
        model: String = "claude-3-5-sonnet-20241022",
        userText: String,
        stepCount: Int,
        batteryPercent: Int,
        batteryCharging: Boolean,
        screenTimeMins: Int,
        locationLabel: String,
        recentNotes: List<String>
    ): String = withContext(Dispatchers.IO) {
        val groundingMeta = "steps:$stepCount|battery:$batteryPercent|loc:$locationLabel"

        // 1. Ensure Session exists in Room SQLite
        val existingSession = repository.getSessionMessagesDirect(sessionId)
        if (existingSession.isEmpty()) {
            val title = if (sessionTitle.isNotBlank() && sessionTitle != "Conversation") {
                sessionTitle
            } else {
                userText.take(40).trim()
            }
            repository.saveChatSession(
                AiChatSessionEntity(
                    sessionId = sessionId,
                    title = title,
                    provider = provider,
                    model = model,
                    messageCount = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            firestoreSync.syncChatSessionMeta(sessionId, title, provider, model, 0)
        }

        // 2. Record USER message to Room Local SQLite
        repository.saveChatMessage(
            AiChatMessageEntity(
                sessionId = sessionId,
                sender = "USER",
                content = userText,
                groundingMeta = groundingMeta,
                modelUsed = model,
                timestamp = System.currentTimeMillis()
            )
        )

        // 3. Record USER message to Firebase Firestore
        firestoreSync.sendChatMessage(
            chatId = sessionId,
            sender = "USER",
            content = userText,
            stepsAtTime = stepCount,
            activeLocation = locationLabel
        )

        // 4. Build Grounded Context for Selected AI Provider with Tool Capabilities
        val systemContext = """
            You are Secondary Brain 2.0, a high-performance personal AI companion.
            Current Real-Time Grounding Context:
            - Device Location: $locationLabel
            - Steps Today: $stepCount steps
            - Battery Level: $batteryPercent% (Charging: $batteryCharging)
            - Screen Usage Today: ${screenTimeMins / 60}h ${screenTimeMins % 60}m
            - Recent Voice Notes & Tasks:
            ${recentNotes.take(5).joinToString("\n") { "  * $it" }}

            Autonomous Agent Capabilities:
            You can manage the user's alarms, to-dos, and pinned locations on-device.
            When the user requests an action, execute it clearly and inform them:
            - To set an alarm: Use format TOOL:SET_ALARM(HH:MM, "Label")
            - To add a to-do task: Use format TOOL:ADD_TODO("Task Title")
            - To pin GPS location: Use format TOOL:PIN_LOCATION("Location Label")

            Answer concisely, friendly, and smartly in 2-4 sentences. Leverage the real-time context when relevant.
        """.trimIndent()

        // 5. Generate AI Response via Multi-Provider Dispatcher
        var aiResponse = generateMultiProviderResponse(
            provider = provider,
            model = model,
            userPrompt = userText,
            systemPrompt = systemContext,
            sessionId = sessionId
        )

        // 6. Execute Autonomous Device Agent Actions (Alarms, To-Dos, Location Fixes)
        val actionResult = executeAutonomousAgentActions(userText, aiResponse)
        if (actionResult.isNotBlank()) {
            aiResponse = "$aiResponse\n\n$actionResult"
        }

        // 7. Record AI message to Room Local SQLite
        repository.saveChatMessage(
            AiChatMessageEntity(
                sessionId = sessionId,
                sender = "AI",
                content = aiResponse,
                groundingMeta = groundingMeta,
                modelUsed = model,
                timestamp = System.currentTimeMillis()
            )
        )

        // 8. Record AI message to Firebase Firestore
        firestoreSync.sendChatMessage(
            chatId = sessionId,
            sender = "AI",
            content = aiResponse,
            stepsAtTime = stepCount,
            activeLocation = locationLabel
        )

        // 9. Sync message to Server Backend SQLite in background
        syncMessageToServer(sessionId, userText, aiResponse, provider, model, groundingMeta)

        aiResponse
    }

    /**
     * Executes native on-device actions requested by the user or triggered by AI tool directives.
     */
    private suspend fun executeAutonomousAgentActions(userText: String, aiResponse: String): String {
        val feedback = mutableListOf<String>()
        val combinedText = "$userText\n$aiResponse".lowercase()

        // 1. Alarm Scheduling Action
        // e.g. "TOOL:SET_ALARM(06:30, "Gym")" or user says "set alarm for 7:00 AM" or "6 baje alarm"
        val alarmPattern = Pattern.compile("(?i)(?:tool:set_alarm\\((\\d{1,2}):(\\d{2})|alarm(?:\\s+for|\\s+at)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?)")
        val alarmMatcher = alarmPattern.matcher(combinedText)
        if (alarmMatcher.find()) {
            try {
                var hour = alarmMatcher.group(1)?.toIntOrNull() ?: alarmMatcher.group(3)?.toIntOrNull() ?: 6
                val minute = alarmMatcher.group(2)?.toIntOrNull() ?: alarmMatcher.group(4)?.toIntOrNull() ?: 0
                val amPm = alarmMatcher.group(5)?.lowercase()

                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0

                val triggerTime = alarmScheduler.scheduleManualAlarm(hour, minute, "Secondary Brain AI Alarm")
                if (triggerTime > 0) {
                    val formatted = String.format("%02d:%02d", hour, minute)
                    feedback.add("⏰ [AI Action Executed]: Alarm scheduled for $formatted on your Android device.")
                }
            } catch (e: Exception) {
                Log.w("SecondBrainChatEngine", "Alarm action parsing notice: ${e.message}")
            }
        }

        // 2. To-Do / Voice Task Creation Action
        // e.g. "TOOL:ADD_TODO("Buy medicine")" or user says "add to-do buy groceries"
        if (combinedText.contains("tool:add_todo") || combinedText.startsWith("add todo") || combinedText.startsWith("add task") || combinedText.contains("remind me to")) {
            try {
                val taskContent = if (combinedText.contains("tool:add_todo")) {
                    val start = aiResponse.indexOf("TOOL:ADD_TODO(") + 14
                    val end = aiResponse.indexOf(")", start)
                    if (start > 13 && end > start) aiResponse.substring(start, end).replace("\"", "").trim() else userText
                } else {
                    userText.replace("add todo", "", ignoreCase = true)
                        .replace("add task", "", ignoreCase = true)
                        .replace("remind me to", "", ignoreCase = true)
                        .trim()
                }

                if (taskContent.isNotBlank()) {
                    val taskEntity = VoiceTaskEntity(
                        title = taskContent,
                        category = "DUE_DATE",
                        lectureTitle = "AI Agent Task",
                        urgencyLevel = "HIGH",
                        rawVoiceText = userText,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertVoiceTask(taskEntity)
                    feedback.add("📝 [AI Action Executed]: Added to-do task: \"$taskContent\" in Room SQLite & synced to Cloud.")
                }
            } catch (e: Exception) {
                Log.w("SecondBrainChatEngine", "Todo action notice: ${e.message}")
            }
        }

        // 3. Location Pin Action
        // e.g. "TOOL:PIN_LOCATION" or user says "pin my location" / "save gps"
        if (combinedText.contains("tool:pin_location") || combinedText.contains("pin location") || combinedText.contains("save location") || combinedText.contains("where am i")) {
            try {
                GpsLocationHelper.requestLocation(context) { report, err ->
                    if (report != null) {
                        firestoreSync.syncLocation(
                            latitude = report.latitude,
                            longitude = report.longitude,
                            accuracyMeters = report.accuracyMeters,
                            locationLabel = report.locationLabel.ifBlank { "Pinned AI Anchor Point" }
                        )
                    }
                }
                feedback.add("📍 [AI Action Executed]: Real-time GPS beacon queried and coordinates synced.")
            } catch (e: Exception) {
                Log.w("SecondBrainChatEngine", "Location action notice: ${e.message}")
            }
        }

        return feedback.joinToString("\n")
    }

    private suspend fun generateMultiProviderResponse(
        provider: String,
        model: String,
        userPrompt: String,
        systemPrompt: String,
        sessionId: String
    ): String {
        val p = provider.lowercase().trim()
        val keyEntity = repository.getAiKeyDirect(p)
        val apiKey = keyEntity?.apiKey?.trim() ?: ""

        // Try direct call based on provider
        try {
            when (p) {
                "claude", "anthropic" -> {
                    if (apiKey.isNotBlank()) {
                        return callClaudeDirect(apiKey, model, userPrompt, systemPrompt)
                    }
                }
                "gemini" -> {
                    if (apiKey.isNotBlank()) {
                        return callGeminiDirect(apiKey, model, userPrompt, systemPrompt)
                    }
                }
                "deepseek" -> {
                    if (apiKey.isNotBlank()) {
                        return callOpenAiCompatible(
                            url = "https://api.deepseek.com/v1/chat/completions",
                            apiKey = apiKey,
                            model = model.ifEmpty { "deepseek-chat" },
                            userPrompt = userPrompt,
                            systemPrompt = systemPrompt
                        )
                    }
                }
                "nvidia" -> {
                    if (apiKey.isNotBlank()) {
                        return callOpenAiCompatible(
                            url = "https://integrate.api.nvidia.com/v1/chat/completions",
                            apiKey = apiKey,
                            model = model.ifEmpty { "nvidia/llama-3.1-nemotron-70b-instruct" },
                            userPrompt = userPrompt,
                            systemPrompt = systemPrompt
                        )
                    }
                }
                "openrouter" -> {
                    if (apiKey.isNotBlank()) {
                        return callOpenAiCompatible(
                            url = "https://openrouter.ai/api/v1/chat/completions",
                            apiKey = apiKey,
                            model = model.ifEmpty { "anthropic/claude-3.5-sonnet" },
                            userPrompt = userPrompt,
                            systemPrompt = systemPrompt,
                            extraHeaders = mapOf(
                                "HTTP-Referer" to "https://secondary-brain.internal",
                                "X-Title" to "Secondary Brain 2.0"
                            )
                        )
                    }
                }
                "groq" -> {
                    val groqKey = if (apiKey.isNotBlank()) apiKey else groqManager.apiKey.value.ifBlank {
                        "gsk_K58U6OirwzD7HwA6tY6ZWGdyb3FYp9Z1bZ4z3WvC7M9x0A2B1C"
                    }
                    return callOpenAiCompatible(
                        url = "https://api.groq.com/openai/v1/chat/completions",
                        apiKey = groqKey,
                        model = model.ifEmpty { "llama-3.3-70b-versatile" },
                        userPrompt = userPrompt,
                        systemPrompt = systemPrompt
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("SecondBrainChatEngine", "Direct provider call failed, trying server proxy: ${e.message}")
        }

        // Try Backend Server Proxy
        try {
            val serverResp = callServerAiProxy(p, model, userPrompt, systemPrompt, sessionId)
            if (serverResp.isNotBlank()) {
                return serverResp
            }
        } catch (e: Exception) {
            Log.w("SecondBrainChatEngine", "Server AI proxy notice: ${e.message}")
        }

        // Informative guidance fallback
        if (apiKey.isBlank() && p != "groq") {
            return "🔑 [${p.uppercase()} Key Required]: Please tap the Settings icon ⚙️ at the top right of this chat and enter your ${p.replaceFirstChar { it.uppercase() }} API key to enable ${model}."
        }

        return "⚠️ [Cloud AI Unreachable]: Failed to generate response from ${p.uppercase()} ($model). Please verify your internet connection or check your API key pool in Chat Settings."
    }

    private fun callClaudeDirect(apiKey: String, model: String, userPrompt: String, systemPrompt: String): String {
        val json = JSONObject().apply {
            put("model", model.ifEmpty { "claude-3-5-sonnet-20241022" })
            put("max_tokens", 800)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val resp = okHttpClient.newCall(request).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string() ?: ""
            val obj = JSONObject(body)
            val contentArr = obj.optJSONArray("content")
            val text = contentArr?.optJSONObject(0)?.optString("text", "")
            if (!text.isNullOrBlank()) return text.trim()
        }
        throw RuntimeException("Claude API error: ${resp.code} ${resp.message}")
    }

    private fun callGeminiDirect(apiKey: String, model: String, userPrompt: String, systemPrompt: String): String {
        val effectiveModel = model.ifEmpty { "gemini-1.5-flash" }
        val fullText = "System Directives: $systemPrompt\n\nUser: $userPrompt"

        val json = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", fullText)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.6)
                put("maxOutputTokens", 800)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val resp = okHttpClient.newCall(request).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string() ?: ""
            val obj = JSONObject(body)
            val candidates = obj.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
            if (!text.isNullOrBlank()) return text.trim()
        }
        throw RuntimeException("Gemini API error: ${resp.code} ${resp.message}")
    }

    private fun callOpenAiCompatible(
        url: String,
        apiKey: String,
        model: String,
        userPrompt: String,
        systemPrompt: String,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val json = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.6)
            put("max_tokens", 800)
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))

        extraHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }

        val resp = okHttpClient.newCall(reqBuilder.build()).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string() ?: ""
            val obj = JSONObject(body)
            val choices = obj.optJSONArray("choices")
            val text = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "")
            if (!text.isNullOrBlank()) return text.trim()
        }
        throw RuntimeException("API error: ${resp.code} ${resp.message}")
    }

    private fun callServerAiProxy(
        provider: String,
        model: String,
        userPrompt: String,
        systemPrompt: String,
        sessionId: String
    ): String {
        val serverUrl = AiKeySyncManager.DEFAULT_SERVER_URL
        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("provider", provider)
            put("model", model)
            put("prompt", userPrompt)
            put("system_prompt", systemPrompt)
        }

        val request = Request.Builder()
            .url("$serverUrl/api/chat/generate")
            .header("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val resp = okHttpClient.newCall(request).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string() ?: ""
            val obj = JSONObject(body)
            return obj.optString("reply", "")
        }
        return ""
    }

    private fun syncMessageToServer(
        sessionId: String,
        userText: String,
        aiText: String,
        provider: String,
        model: String,
        groundingMeta: String
    ) {
        try {
            val serverUrl = AiKeySyncManager.DEFAULT_SERVER_URL
            val json = JSONObject().apply {
                put("session_id", sessionId)
                put("title", userText.take(40))
                put("provider", provider)
                put("model_used", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("session_id", sessionId)
                        put("sender", "USER")
                        put("content", userText)
                        put("grounding_info", groundingMeta)
                    })
                    put(JSONObject().apply {
                        put("session_id", sessionId)
                        put("sender", "AI")
                        put("content", aiText)
                        put("grounding_info", groundingMeta)
                    })
                })
            }
            val request = Request.Builder()
                .url("$serverUrl/api/chat/sessions")
                .post(json.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w("SecondBrainChatEngine", "Server session sync notice: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: SecondBrainChatEngine? = null

        fun getInstance(context: Context): SecondBrainChatEngine {
            return instance ?: synchronized(this) {
                instance ?: SecondBrainChatEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
