package com.example.core.ai

import android.content.Context
import android.util.Log
import com.example.MainApplication
import com.example.core.cloud.FirestoreSyncManager
import com.example.core.recorder.TelegramConfigManager
import com.example.data.local.entity.AiKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ModelOption(
    val id: String,
    val displayName: String,
    val provider: String,
    val iconEmoji: String,
    val badge: String,
    val description: String
)

class AiKeySyncManager private constructor(private val context: Context) {

    private val repository = (context.applicationContext as MainApplication).repository
    private val firestoreSync = FirestoreSyncManager.getInstance(context)
    private val telegramConfig = TelegramConfigManager.getInstance(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val allKeysFlow: Flow<List<AiKeyEntity>> = repository.allAiKeys

    companion object {
        private const val TAG = "AiKeySyncManager"
        const val DEFAULT_SERVER_URL = "https://encore-secondary-brain-server.onrender.com"
        const val LOCAL_EMULATOR_URL = "http://10.0.2.2:10000"

        val AVAILABLE_MODELS = listOf(
            // Anthropic Claude
            ModelOption(
                id = "claude-3-5-sonnet-20241022",
                displayName = "Claude 3.5 Sonnet",
                provider = "claude",
                iconEmoji = "✨",
                badge = "Smartest",
                description = "Anthropic flagship for complex reasoning, code & deep logic."
            ),
            ModelOption(
                id = "claude-3-5-haiku-20241022",
                displayName = "Claude 3.5 Haiku",
                provider = "claude",
                iconEmoji = "⚡",
                badge = "Ultra Fast",
                description = "Lightning-quick conversational intelligence from Anthropic."
            ),
            // Google Gemini
            ModelOption(
                id = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash",
                provider = "gemini",
                iconEmoji = "🌟",
                badge = "Next-Gen",
                description = "Google multimodal powerhouse with 1M context."
            ),
            ModelOption(
                id = "gemini-1.5-pro",
                displayName = "Gemini 1.5 Pro",
                provider = "gemini",
                iconEmoji = "💎",
                badge = "Deep Pro",
                description = "High-fidelity reasoning and context synthesis."
            ),
            // DeepSeek
            ModelOption(
                id = "deepseek-chat",
                displayName = "DeepSeek V3",
                provider = "deepseek",
                iconEmoji = "🔵",
                badge = "Versatile",
                description = "Top-tier open weights benchmark leader from DeepSeek."
            ),
            ModelOption(
                id = "deepseek-reasoner",
                displayName = "DeepSeek R1",
                provider = "deepseek",
                iconEmoji = "🧠",
                badge = "CoT Reasoning",
                description = "Chain-of-thought advanced mathematical and logic reasoner."
            ),
            // NVIDIA NIM
            ModelOption(
                id = "nvidia/llama-3.1-nemotron-70b-instruct",
                displayName = "NVIDIA Nemotron 70B",
                provider = "nvidia",
                iconEmoji = "🟢",
                badge = "Hardware Tuned",
                description = "Accelerated LLaMA 3.1 fine-tuned by NVIDIA AI research."
            ),
            // OpenRouter
            ModelOption(
                id = "anthropic/claude-3.5-sonnet",
                displayName = "OpenRouter / Claude 3.5",
                provider = "openrouter",
                iconEmoji = "🟣",
                badge = "Unified Hub",
                description = "Access Claude, LLaMA & Mistral with one OpenRouter API key."
            ),
            ModelOption(
                id = "deepseek/deepseek-r1",
                displayName = "OpenRouter / DeepSeek R1",
                provider = "openrouter",
                iconEmoji = "🟣",
                badge = "Unified Hub",
                description = "DeepSeek R1 reasoning routed via OpenRouter cloud."
            ),
            // Groq
            ModelOption(
                id = "llama-3.3-70b-versatile",
                displayName = "Groq LLaMA 3.3 70B",
                provider = "groq",
                iconEmoji = "⚡",
                badge = "300 T/s",
                description = "LPU inference speed for instant, zero-latency answers."
            ),
            ModelOption(
                id = "deepseek-r1-distill-llama-70b",
                displayName = "Groq DeepSeek R1 70B",
                provider = "groq",
                iconEmoji = "🧠",
                badge = "Fast CoT",
                description = "DeepSeek R1 reasoning at 300+ tokens/sec on Groq LPUs."
            ),
            // Cerebras
            ModelOption(
                id = "llama3.3-70b",
                displayName = "Cerebras LLaMA 3.3 70B",
                provider = "cerebras",
                iconEmoji = "🚀",
                badge = "1800 T/s",
                description = "World's fastest inference speed on Wafer-Scale Engine hardware."
            ),
            // SambaNova
            ModelOption(
                id = "DeepSeek-R1",
                displayName = "SambaNova DeepSeek R1 671B",
                provider = "sambanova",
                iconEmoji = "⚡",
                badge = "Full Precision",
                description = "Free full-precision 671B DeepSeek R1 on SambaNova DataScale."
            ),
            // OpenAI
            ModelOption(
                id = "gpt-4o",
                displayName = "OpenAI GPT-4o",
                provider = "openai",
                iconEmoji = "🟢",
                badge = "Omni Multi",
                description = "Flagship multimodal vision, audio, and reasoning from OpenAI."
            ),
            ModelOption(
                id = "gpt-4o-mini",
                displayName = "OpenAI GPT-4o Mini",
                provider = "openai",
                iconEmoji = "🟢",
                badge = "Fast & Cheap",
                description = "High-speed compact multimodal intelligence."
            ),
            // Mistral AI
            ModelOption(
                id = "mistral-large-latest",
                displayName = "Mistral Large 2",
                provider = "mistral",
                iconEmoji = "🌊",
                badge = "128K Context",
                description = "Flagship reasoning, multilingual nuance, and coding."
            ),
            // Local Ollama / Termux
            ModelOption(
                id = "ollama/deepseek-r1:1.5b",
                displayName = "Local DeepSeek R1 (1.5B)",
                provider = "ollama",
                iconEmoji = "🦙",
                badge = "100% Offline",
                description = "Runs locally on device ARM64 CPU via Termux llama-server with zero cloud access."
            ),
            ModelOption(
                id = "ollama/qwen2.5:1.5b",
                displayName = "Local Qwen 2.5 (1.5B)",
                provider = "ollama",
                iconEmoji = "🦙",
                badge = "100% Offline",
                description = "Fast local general dialogue and formatting."
            )
        )

        @Volatile
        private var instance: AiKeySyncManager? = null

        fun getInstance(context: Context): AiKeySyncManager {
            return instance ?: synchronized(this) {
                instance ?: AiKeySyncManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun saveKeyAndSync(
        provider: String,
        apiKey: String,
        selectedModel: String,
        serverBaseUrl: String = DEFAULT_SERVER_URL
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmedProv = provider.lowercase().trim()
        val trimmedKey = apiKey.trim()
        val model = selectedModel.trim().ifEmpty {
            AVAILABLE_MODELS.firstOrNull { it.provider == trimmedProv }?.id ?: ""
        }

        // 1. Save to Room local database
        val entity = AiKeyEntity(
            provider = trimmedProv,
            apiKey = trimmedKey,
            selectedModel = model,
            isActive = trimmedKey.isNotBlank(),
            isSyncedWithServer = false,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveAiKey(entity)

        // 2. Sync to Firebase Firestore
        firestoreSync.syncApiKey(trimmedProv, trimmedKey, model, entity.isActive)

        // 3. Sync to Backend Server SQLite database
        val serverSynced = syncKeyToServer(entity, serverBaseUrl)
        if (serverSynced) {
            repository.saveAiKey(entity.copy(isSyncedWithServer = true))
        }

        true
    }

    suspend fun deleteKeyAndSync(provider: String, serverBaseUrl: String = DEFAULT_SERVER_URL): Boolean = withContext(Dispatchers.IO) {
        val p = provider.lowercase().trim()
        repository.deleteAiKey(p)
        firestoreSync.syncApiKey(p, "", "", false)

        try {
            val req = Request.Builder()
                .url("$serverBaseUrl/api/keys/$p")
                .delete()
                .build()
            okHttpClient.newCall(req).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Server key delete notice: ${e.message}")
        }
        true
    }

    private fun syncKeyToServer(entity: AiKeyEntity, serverBaseUrl: String): Boolean {
        return try {
            val json = JSONObject().apply {
                put("provider", entity.provider)
                put("api_key", entity.apiKey)
                put("selected_model", entity.selectedModel)
                put("is_active", entity.isActive)
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url("$serverBaseUrl/api/keys")
                .post(body)
                .build()
            val resp = okHttpClient.newCall(req).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Server syncKeyToServer notice: ${e.message}")
            false
        }
    }

    suspend fun fetchKeysFromServer(serverBaseUrl: String = DEFAULT_SERVER_URL): List<AiKeyEntity> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$serverBaseUrl/api/keys")
                .get()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val dataArr = json.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<AiKeyEntity>()
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    val prov = obj.optString("provider")
                    val key = obj.optString("api_key")
                    val model = obj.optString("selected_model")
                    val active = obj.optBoolean("is_active", true)
                    if (prov.isNotBlank()) {
                        val ent = AiKeyEntity(
                            provider = prov,
                            apiKey = key,
                            selectedModel = model,
                            isActive = active,
                            isSyncedWithServer = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveAiKey(ent)
                        list.add(ent)
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchKeysFromServer error: ${e.message}")
        }
        emptyList()
    }

    suspend fun backupSessionToTelegram(
        sessionId: String,
        sessionTitle: String,
        serverBaseUrl: String = DEFAULT_SERVER_URL
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val token = telegramConfig.getBotToken()
            val chatId = telegramConfig.getChatId()

            // 1. Try via Server Telegram Backup Endpoint
            val json = JSONObject().apply {
                put("session_id", sessionId)
                put("title", sessionTitle)
                if (chatId.isNotBlank()) put("chat_id", chatId)
            }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val req = Request.Builder()
                .url("$serverBaseUrl/api/chat/backup-telegram")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                repository.markSessionTelegramSynced(sessionId)
                return@withContext Pair(true, "Cloud backup archived in Telegram channel!")
            }

            // 2. Direct fallback via Telegram bot API if server offline
            if (token.isNotBlank() && chatId.isNotBlank()) {
                val msgs = repository.getSessionMessagesDirect(sessionId)
                val transcript = StringBuilder()
                transcript.append("☁️ *[TELEGRAM CLOUD BACKUP]*\n")
                transcript.append("📂 *Session*: `${sessionTitle}`\n")
                transcript.append("💬 *Messages*: ${msgs.size}\n\n")
                for (m in msgs) {
                    val badge = if (m.sender.equals("USER", true)) "👤 *USER*" else "🤖 *AI*"
                    transcript.append("$badge: ${m.content}\n\n")
                }

                val directBody = okhttp3.FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", transcript.toString().take(4000))
                    .add("parse_mode", "Markdown")
                    .build()

                val directReq = Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(directBody)
                    .build()

                val directResp = okHttpClient.newCall(directReq).execute()
                if (directResp.isSuccessful) {
                    repository.markSessionTelegramSynced(sessionId)
                    return@withContext Pair(true, "Cloud backup sent directly to Telegram Bot!")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Telegram backup failed", e)
            return@withContext Pair(false, "Backup error: ${e.message}")
        }
        Pair(false, "Please configure Telegram Bot token in Settings to enable Cloud Backup.")
    }
}
