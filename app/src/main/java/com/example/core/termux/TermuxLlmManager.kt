package com.example.core.termux

import android.content.Context
import android.content.Intent
import android.util.Log
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
 * Termux Local AI LLM Supervisor & IPC Manager.
 *
 * Implements:
 * 1. Live health monitoring for local LLM servers (http://127.0.0.1:8080 or :11434).
 * 2. Termux `RUN_COMMAND` intent dispatching to start server & download models.
 * 3. On-device inference benchmarking (speed test & latency calculation).
 * 4. Model inventory & selection state.
 */
class TermuxLlmManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var monitorJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── Observable State ─────────────────────────────────────────────────────

    private val _isServerOnline = MutableStateFlow(false)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    private val _serverUrl = MutableStateFlow("http://127.0.0.1:8080/v1")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _activeModelName = MutableStateFlow("DeepSeek-R1-1.5B (Local)")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(
        listOf("DeepSeek-R1-1.5B", "Qwen2.5-0.5B", "Qwen2.5-1.5B", "Llama-3.2-1B", "SmolLM2-1.7B")
    )
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _tokensPerSec = MutableStateFlow<Float?>(null)
    val tokensPerSec: StateFlow<Float?> = _tokensPerSec.asStateFlow()

    private val _lastBenchmarkLog = MutableStateFlow("Local LLM ready for on-device inference.")
    val lastBenchmarkLog: StateFlow<String> = _lastBenchmarkLog.asStateFlow()

    init {
        startHealthMonitoring()
    }

    /**
     * Starts continuous health check loop for localhost LLM server.
     */
    fun startHealthMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                checkServerHealth()
                delay(5000)
            }
        }
    }

    private suspend fun checkServerHealth() {
        val checkUrls = listOf(
            "http://127.0.0.1:8080/v1/models",
            "http://127.0.0.1:11434/api/tags",
            "http://127.0.0.1:8080/health"
        )

        var foundOnline = false
        for (url in checkUrls) {
            try {
                val req = Request.Builder().url(url).get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        foundOnline = true
                        _serverUrl.value = if (url.contains(":8080")) "http://127.0.0.1:8080/v1" else "http://127.0.0.1:11434/v1"
                        return@use
                    }
                }
            } catch (e: Exception) {
                // Ignore failure on single port
            }
            if (foundOnline) break
        }

        _isServerOnline.value = foundOnline
    }

    /**
     * Dispatches Termux Intent to launch llama-server in background.
     */
    fun launchTermuxServer(modelChoice: Int = 1) {
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
                putExtra(
                    "com.termux.RUN_COMMAND_ARGUMENTS",
                    arrayOf("-c", "cd ~ && bash termux_scripts/start_llama_server.sh $modelChoice")
                )
                putExtra("com.termux.RUN_COMMAND_IN_BACKGROUND", true)
            }
            context.startService(intent)
            _lastBenchmarkLog.value = "🚀 Dispatched start command to Termux..."
            Log.i(TAG, "Dispatched Termux LLM Server start command")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch Termux command", e)
            _lastBenchmarkLog.value = "⚠️ Termux RUN_COMMAND dispatch failed: ${e.message}"
        }
    }

    /**
     * Runs an on-device speed & inference benchmark test.
     */
    fun runLocalBenchmarkTest(prompt: String = "Explain gravity in 10 words.") {
        scope.launch {
            _lastBenchmarkLog.value = "⚡ Running local benchmark with prompt: '$prompt'..."
            val startMs = System.currentTimeMillis()

            val endpoint = "${_serverUrl.value.trimEnd('/')}/chat/completions"
            val payload = JSONObject().apply {
                put("model", "local-model")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", 60)
                put("temperature", 0.3)
            }

            val req = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                httpClient.newCall(req).execute().use { resp ->
                    val elapsedSec = (System.currentTimeMillis() - startMs) / 1000f
                    val body = resp.body?.string()

                    if (resp.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val text = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                        val tokens = json.optJSONObject("usage")?.optInt("completion_tokens", text.split(" ").size) ?: text.split(" ").size
                        val tps = if (elapsedSec > 0) tokens / elapsedSec else 0f
                        _tokensPerSec.value = tps
                        _lastBenchmarkLog.value = "✅ Response (${"%.1f".format(tps)} tok/s, ${"%.2f".format(elapsedSec)}s):\n$text"
                    } else {
                        _lastBenchmarkLog.value = "❌ Server error ${resp.code}: $body"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Benchmark test failed", e)
                _lastBenchmarkLog.value = "❌ Connection failed to ${_serverUrl.value}. Ensure Termux server is running."
            }
        }
    }

    companion object {
        private const val TAG = "TermuxLlmManager"

        @Volatile
        private var instance: TermuxLlmManager? = null

        fun getInstance(context: Context): TermuxLlmManager {
            return instance ?: synchronized(this) {
                instance ?: TermuxLlmManager(context).also { instance = it }
            }
        }
    }
}
