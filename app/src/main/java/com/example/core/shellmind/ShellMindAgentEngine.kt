package com.example.core.shellmind

import android.content.Context
import android.util.Log
import com.example.core.ai.AiKeySyncManager
import com.example.core.termux.TermuxLlmManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Autonomous On-Device AI Coding Engine (ShellMind).
 * Implements an agentic coding loop: analyzes codebases, reads/edits files, runs terminal commands, and fixes errors.
 */
class ShellMindAgentEngine private constructor(private val context: Context) {

    private val workspaceManager = ShellMindWorkspaceManager.getInstance(context)
    private val terminalExecutor = ShellMindTerminalExecutor.getInstance(context)
    private val termuxLlm = TermuxLlmManager.getInstance(context)

    private val _isAgentBusy = MutableStateFlow(false)
    val isAgentBusy: StateFlow<Boolean> = _isAgentBusy.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<CodingChatMessage>>(
        listOf(
            CodingChatMessage(
                sender = "SHELLMIND",
                content = "👋 **ShellMind On-Device AI Coding Agent Ready**.\n\nI can read files, write code, run terminal scripts, and debug errors directly in your workspace or Termux environment.\n\nTry:\n- *\"Create a Python web scraper in scraper.py\"*\n- *\"List all files and check git status\"*\n- *\"Run pytest and fix failing assertions\"*",
                timestamp = System.currentTimeMillis()
            )
        )
    )
    val chatMessages: StateFlow<List<CodingChatMessage>> = _chatMessages.asStateFlow()

    private val _currentThought = MutableStateFlow("")
    val currentThought: StateFlow<String> = _currentThought.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    var activeProvider: String = "groq"
    var activeModel: String = "llama-3.3-70b-versatile"
    var customApiKey: String = ""
    var customBaseUrl: String = "https://api.groq.com/openai/v1"

    companion object {
        private const val TAG = "ShellMindAgentEngine"

        @Volatile
        private var instance: ShellMindAgentEngine? = null

        fun getInstance(context: Context): ShellMindAgentEngine {
            return instance ?: synchronized(this) {
                instance ?: ShellMindAgentEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun sendUserPrompt(prompt: String): Unit = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isAgentBusy.value) return@withContext

        appendMessage(CodingChatMessage(sender = "USER", content = trimmed))
        _isAgentBusy.value = true

        val conversationHistory = _chatMessages.value.takeLast(12)
        var currentStep = 0
        val maxSteps = 6

        var systemPrompt = """
            You are ShellMind, an elite on-device autonomous AI coding agent and developer copilot.
            Workspace Root: ${workspaceManager.getWorkspacePath()}
            
            You have access to tools. When you want to take an action, reply strictly with a JSON object in this format:
            {
              "thought": "your step-by-step reasoning",
              "action": "read_file" | "write_file" | "list_dir" | "search_code" | "run_command" | "finish",
              "params": { ... }
            }
            
            Tool Parameters:
            - read_file: {"path": "file_path"}
            - write_file: {"path": "file_path", "content": "full_code_content"}
            - list_dir: {"path": "optional_sub_directory"}
            - search_code: {"query": "search_keyword"}
            - run_command: {"command": "sh_or_termux_command"}
            - finish: {"summary": "final explanation to the user"}
            
            Always verify your changes and provide clean, modular, production-ready code.
        """.trimIndent()

        try {
            while (currentStep < maxSteps && _isAgentBusy.value) {
                currentStep++
                _currentThought.value = "Reasoning step $currentStep/$maxSteps..."

                val llmResponse = callLlmApi(systemPrompt, conversationHistory)
                val cleanResponse = extractJsonFromResponse(llmResponse)

                val jsonObj = try {
                    JSONObject(cleanResponse)
                } catch (e: Exception) {
                    // Fallback to direct conversational response
                    appendMessage(CodingChatMessage(sender = "SHELLMIND", content = llmResponse))
                    break
                }

                val thought = jsonObj.optString("thought", "")
                val action = jsonObj.optString("action", "finish")
                val params = jsonObj.optJSONObject("params") ?: JSONObject()

                if (thought.isNotBlank()) {
                    _currentThought.value = thought
                }

                if (action == "finish") {
                    val summary = params.optString("summary", thought.ifEmpty { "Task completed successfully." })
                    appendMessage(CodingChatMessage(sender = "SHELLMIND", content = summary))
                    break
                }

                // Execute tool
                val toolResult = executeCodingTool(action, params)
                appendMessage(
                    CodingChatMessage(
                        sender = "SHELLMIND",
                        content = "**Action: `$action`**\n```\n$toolResult\n```",
                        isToolOutput = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Coding agent loop error", e)
            appendMessage(CodingChatMessage(sender = "SHELLMIND", content = "⚠️ Agent execution notice: ${e.message}"))
        } finally {
            _isAgentBusy.value = false
            _currentThought.value = ""
        }
    }

    private suspend fun executeCodingTool(action: String, params: JSONObject): String {
        return when (action) {
            "read_file" -> {
                val path = params.optString("path")
                val content = workspaceManager.readFile(path)
                if (content != null) "File content ($path):\n$content" else "Error: File not found at '$path'"
            }
            "write_file" -> {
                val path = params.optString("path")
                val content = params.optString("content")
                val success = workspaceManager.writeFile(path, content)
                if (success) "Successfully written ${content.length} characters to '$path'" else "Error writing to '$path'"
            }
            "list_dir" -> {
                val path = params.optString("path", "")
                val items = workspaceManager.listFiles(path)
                if (items.isEmpty()) "Directory is empty or does not exist." else {
                    items.joinToString("\n") { (if (it.isDirectory) "📁 " else "📄 ") + it.relativePath + " (${it.sizeBytes} B)" }
                }
            }
            "search_code" -> {
                val query = params.optString("query")
                val results = workspaceManager.searchFiles(query)
                if (results.isEmpty()) "No matches found for '$query'" else {
                    results.take(15).joinToString("\n") { "${it.filePath}:${it.lineNumber}: ${it.snippet}" }
                }
            }
            "run_command" -> {
                val cmd = params.optString("command")
                val result = terminalExecutor.executeCommand(cmd)
                "Exit code: ${result.exitCode}\nStdout:\n${result.stdout}\nStderr:\n${result.stderr}"
            }
            else -> "Unknown action: $action"
        }
    }

    private suspend fun callLlmApi(systemPrompt: String, history: List<CodingChatMessage>): String {
        // If offline local termux model selected
        if (activeProvider == "ollama" || activeProvider == "termux") {
            if (termuxLlm.isServerOnline.value) {
                val promptBuilder = StringBuilder("$systemPrompt\n\n")
                history.forEach { promptBuilder.append("${it.sender}: ${it.content}\n") }
                val (success, resp) = termuxLlm.queryLocalLlm(promptBuilder.toString())
                if (success) return resp
            }
        }

        // Cloud OpenAI-compatible endpoint (Groq, DeepSeek, OpenRouter, OpenAI)
        val messagesArr = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            history.forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.sender == "USER") "user" else "assistant")
                    put("content", msg.content)
                })
            }
        }

        val requestBody = JSONObject().apply {
            put("model", activeModel)
            put("messages", messagesArr)
            put("temperature", 0.2)
            put("max_tokens", 2048)
        }.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val finalUrl = if (customBaseUrl.endsWith("/chat/completions")) customBaseUrl else "$customBaseUrl/chat/completions"
        val reqBuilder = Request.Builder()
            .url(finalUrl)
            .post(requestBody)

        if (customApiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $customApiKey")
        }

        val response = okHttpClient.newCall(reqBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: "{}"
        if (!response.isSuccessful) {
            throw RuntimeException("API error HTTP ${response.code}: $bodyStr")
        }

        val json = JSONObject(bodyStr)
        val choices = json.optJSONArray("choices") ?: return bodyStr
        if (choices.length() > 0) {
            return choices.getJSONObject(0).optJSONObject("message")?.optString("content") ?: ""
        }
        return ""
    }

    private fun extractJsonFromResponse(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val firstOpen = trimmed.indexOf("{")
        val lastClose = trimmed.lastIndexOf("}")
        if (firstOpen != -1 && lastClose > firstOpen) {
            return trimmed.substring(firstOpen, lastClose + 1)
        }
        return trimmed
    }

    private fun appendMessage(msg: CodingChatMessage) {
        val list = _chatMessages.value.toMutableList()
        list.add(msg)
        _chatMessages.value = list
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }
}

data class CodingChatMessage(
    val sender: String,
    val content: String,
    val isToolOutput: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
