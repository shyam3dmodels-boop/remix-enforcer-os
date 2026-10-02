package com.example.core.claw

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Full-featured Autonomous ReAct Automation Agent for PrivateAgent.
 *
 * Implements:
 * 1. DeepSeek R1 / Groq / OpenAI / Ollama compatible `/chat/completions` API reasoning loop.
 * 2. Visual layout extraction with self-overlay exclusion.
 * 3. Structured JSON action parsing & dispatching.
 * 4. Step-by-step telemetry stream to Floating Bubble HUD and Main App Studio.
 */
class PrivateAgentReActEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeJob: Job? = null
    private val actionExecutor = PrivateAgentActionExecutor.getInstance(context)
    private val feedbackManager = PrivateAgentFeedbackManager.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ─── Observable State ─────────────────────────────────────────────────────

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentGoal = MutableStateFlow("")
    val currentGoal: StateFlow<String> = _currentGoal.asStateFlow()

    private val _currentThought = MutableStateFlow("Ready for autonomous workflows.")
    val currentThought: StateFlow<String> = _currentThought.asStateFlow()

    private val _executionLogs = MutableStateFlow<List<String>>(emptyList())
    val executionLogs: StateFlow<List<String>> = _executionLogs.asStateFlow()

    // Configurable LLM Provider
    var baseUrl: String = "https://api.deepseek.com/v1"
    var apiKey: String = ""
    var modelName: String = "deepseek-chat" // e.g. "deepseek-reasoner", "deepseek-chat", "llama-3.3-70b-versatile"

    init {
        instance = this
    }

    /**
     * Executes a high-level natural language goal autonomously.
     */
    fun startGoal(goal: String) {
        activeJob?.cancel()
        activeJob = scope.launch {
            _isRunning.value = true
            _currentGoal.value = goal
            _executionLogs.value = listOf("🎯 Goal started: \"$goal\"")
            _currentThought.value = "Perceiving device layout..."

            val service = MobileClawAccessibilityService.instance
            if (service == null) {
                appendLog("❌ Accessibility Service not active. Enable in Settings.")
                _currentThought.value = "Accessibility Service not active."
                feedbackManager.triggerErrorHaptic()
                _isRunning.value = false
                return@launch
            }

            try {
                // If API Key is provided or local Ollama is active, run LLM ReAct Loop.
                // Otherwise run fast heuristic fallback.
                if (apiKey.isNotBlank() || baseUrl.contains("127.0.0.1") || baseUrl.contains("10.0.2.2") || baseUrl.contains("localhost")) {
                    runLlmReActLoop(goal)
                } else {
                    runHeuristicWorkflow(goal)
                }
            } catch (e: CancellationException) {
                appendLog("⏹ Automation stopped by user.")
                _currentThought.value = "Stopped."
            } catch (e: Exception) {
                Log.e(TAG, "ReAct loop error", e)
                appendLog("⚠️ Error: ${e.message}")
                _currentThought.value = "Execution failed: ${e.message}"
                feedbackManager.triggerErrorHaptic()
            } finally {
                _isRunning.value = false
            }
        }
    }

    /**
     * Multi-turn autonomous LLM ReAct loop with screen perception.
     */
    private suspend fun runLlmReActLoop(goal: String) {
        val service = MobileClawAccessibilityService.instance ?: return
        val conversationHistory = JSONArray()

        val systemPrompt = """
            You are PrivateAgent, an expert autonomous mobile device assistant.
            You perceive the current Android screen layout and execute actions to achieve the user's goal.
            
            Always reply with a SINGLE valid JSON object in this exact format:
            {
              "thought": "Brief explanation of what you see and what step you are taking",
              "action": "click" | "type" | "press_key" | "scroll" | "launch_app" | "toggle_flashlight" | "adjust_volume" | "open_wifi" | "open_bluetooth" | "call" | "send_sms" | "finish",
              "params": { ... }
            }
            
            Action parameters:
            - click: {"x": number, "y": number} OR {"text": "exact button text"} OR {"res_id": "view_id"}
            - type: {"text": "string to type"}
            - press_key: {"key": "enter" | "back" | "home" | "recents"}
            - scroll: {"direction": "up" | "down" | "left" | "right", "amount": 0.6}
            - launch_app: {"package": "pkg.name"} OR {"app_name": "youtube" | "whatsapp" | "settings" | ...}
            - toggle_flashlight: {"enabled": true | false}
            - adjust_volume: {"level": "up" | "down" | "mute" | "50"}
            - call: {"number": "1234567890"}
            - send_sms: {"number": "1234567890", "text": "message"}
            - finish: {"message": "summary of accomplished task"}
        """.trimIndent()

        conversationHistory.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        conversationHistory.put(JSONObject().apply {
            put("role", "user")
            put("content", "User Goal: $goal")
        })

        var stepCount = 0
        val maxSteps = 15

        while (stepCount < maxSteps && _isRunning.value) {
            stepCount++
            appendLog("--- Step $stepCount ---")

            // 1. Perception: Dump screen hierarchy with self-overlay filtering
            val rootNode = service.rootInActiveWindow
            val (parsedNodes, hierarchyJson) = PrivateAgentScreenParser.dumpScreenHierarchy(rootNode, filterSelfOverlay = true)

            val screenObservation = "Active App: ${service.activePackageName.value}\nScreen Elements:\n$hierarchyJson"
            conversationHistory.put(JSONObject().apply {
                put("role", "user")
                put("content", "Current Screen Observation:\n$screenObservation")
            })

            _currentThought.value = "Reasoning step $stepCount..."

            // 2. LLM Call
            val responseText = callChatCompletionsApi(conversationHistory)
            if (responseText.isNullOrBlank()) {
                appendLog("⚠️ Empty response from LLM provider")
                break
            }

            // 3. Parse Action JSON
            val actionJson = extractJsonFromResponse(responseText)
            if (actionJson == null) {
                appendLog("⚠️ Could not parse JSON from: $responseText")
                break
            }

            val thought = actionJson.optString("thought", "Executing action...")
            _currentThought.value = thought
            appendLog("💭 Thought: $thought")

            // 4. Execute Action
            val result = actionExecutor.executeAction(actionJson)
            appendLog(if (result.isSuccess) "✓ ${result.message}" else "✗ ${result.message}")

            conversationHistory.put(JSONObject().apply {
                put("role", "assistant")
                put("content", actionJson.toString())
            })

            if (result.isFinished) {
                appendLog("🎉 Goal achieved in $stepCount steps!")
                _currentThought.value = "✅ Finished: ${result.message}"
                break
            }

            delay(1500) // Settle delay before next observation
        }
    }

    /**
     * Executes fast heuristic multi-app workflows without API key.
     */
    private suspend fun runHeuristicWorkflow(goal: String) {
        val lower = goal.lowercase()
        val service = MobileClawAccessibilityService.instance ?: return

        when {
            lower.contains("flashlight") || lower.contains("torch") -> {
                _currentThought.value = "Toggling Flashlight..."
                val turnOn = !lower.contains("off")
                actionExecutor.executeAction(JSONObject().apply {
                    put("action", "toggle_flashlight")
                    put("params", JSONObject().apply { put("enabled", turnOn) })
                })
                appendLog("✓ Flashlight toggled.")
            }

            lower.contains("youtube") -> {
                _currentThought.value = "Launching YouTube..."
                actionExecutor.executeAction(JSONObject().apply {
                    put("action", "launch_app")
                    put("params", JSONObject().apply { put("package", "com.google.android.youtube") })
                })
                delay(2200)

                val query = goal.replace("search youtube for", "", ignoreCase = true)
                    .replace("search youtube", "", ignoreCase = true)
                    .replace("open youtube", "", ignoreCase = true)
                    .replace("youtube", "", ignoreCase = true)
                    .trim()
                    .ifBlank { "Lofi Beats" }

                _currentThought.value = "Searching YouTube for: $query"
                val (w, h) = service.getScreenDimensions()
                service.clickByText("Search") || service.tap(w * 0.85f, 120f)
                delay(1200)
                service.typeText(query)
                delay(1000)
                service.tap(w * 0.90f, h * 0.92f) // Enter
                delay(1500)
                service.tap(w * 0.5f, h * 0.35f) // Top video
                appendLog("✓ YouTube search and play completed.")
                feedbackManager.triggerSuccessHaptic()
            }

            lower.contains("call") -> {
                val digits = goal.filter { it.isDigit() }
                _currentThought.value = "Calling $digits..."
                actionExecutor.executeAction(JSONObject().apply {
                    put("action", "call")
                    put("params", JSONObject().apply { put("number", digits) })
                })
                appendLog("✓ Call initiated.")
            }

            lower.contains("wifi") -> {
                _currentThought.value = "Opening WiFi settings..."
                actionExecutor.executeAction(JSONObject().apply {
                    put("action", "open_wifi")
                })
                appendLog("✓ Opened WiFi settings.")
            }

            lower.contains("scroll") -> {
                val isUp = lower.contains("up")
                _currentThought.value = "Scrolling..."
                actionExecutor.executeAction(JSONObject().apply {
                    put("action", "scroll")
                    put("params", JSONObject().apply { put("direction", if (isUp) "up" else "down") })
                })
                appendLog("✓ Screen scrolled.")
            }

            else -> {
                _currentThought.value = "Navigating..."
                service.pressHome()
                appendLog("✓ Returned to Home.")
            }
        }
    }

    private fun callChatCompletionsApi(messages: JSONArray): String? {
        val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "${baseUrl.trimEnd('/')}/chat/completions"
        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", messages)
            put("temperature", 0.2)
            put("response_format", JSONObject().apply { put("type", "json_object") })
        }

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(payload.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        return try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                } else {
                    Log.e(TAG, "API Error: ${response.code} - $body")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "API call exception", e)
            null
        }
    }

    private fun extractJsonFromResponse(raw: String): JSONObject? {
        val clean = raw.trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            val jsonStr = clean.substring(start, end + 1)
            return try {
                JSONObject(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun stop() {
        activeJob?.cancel()
        _isRunning.value = false
        _currentThought.value = "Stopped."
        appendLog("⏹ Automation stopped.")
    }

    private fun appendLog(msg: String) {
        _executionLogs.value = _executionLogs.value + msg
    }

    companion object {
        private const val TAG = "PrivateAgentEngine"

        @Volatile
        var instance: PrivateAgentReActEngine? = null
            private set

        fun getInstance(context: Context): PrivateAgentReActEngine {
            return instance ?: synchronized(this) {
                instance ?: PrivateAgentReActEngine(context).also { instance = it }
            }
        }
    }
}
