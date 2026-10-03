package com.example.core.ai

/**
 * Universal Single-Input API Key Provider Detector for Android.
 *
 * Automatically inspects raw pasted API key tokens and returns:
 * - Provider identifier ("gemini", "claude", "groq", "openai", "openrouter", "deepseek", "perplexity", "cerebras", "fireworks", "nvidia", "huggingface")
 * - Human-readable brand label ("Google Gemini", "Anthropic Claude", "Groq Cloud", etc.)
 * - Default flagship model ID
 * - Recommended emoji icon
 */
data class DetectedKeyInfo(
    val provider: String,
    val displayName: String,
    val defaultModel: String,
    val iconEmoji: String,
    val description: String
)

object AiKeyDetector {

    fun detectProvider(rawKey: String): DetectedKeyInfo {
        val key = rawKey.trim()

        return when {
            // Google Gemini API keys start with AIzaSy
            key.startsWith("AIzaSy") -> DetectedKeyInfo(
                provider = "gemini",
                displayName = "Google Gemini",
                defaultModel = "gemini-2.0-flash",
                iconEmoji = "🌟",
                description = "Gemini 2.0 Flash / 1.5 Pro Multimodal Engine"
            )

            // Anthropic Claude keys start with sk-ant-
            key.startsWith("sk-ant-") -> DetectedKeyInfo(
                provider = "claude",
                displayName = "Anthropic Claude",
                defaultModel = "claude-3-5-sonnet-20241022",
                iconEmoji = "✨",
                description = "Claude 3.5 Sonnet / Haiku Reasoning Engine"
            )

            // Groq Cloud keys start with gsk_
            key.startsWith("gsk_") -> DetectedKeyInfo(
                provider = "groq",
                displayName = "Groq Cloud",
                defaultModel = "llama-3.3-70b-versatile",
                iconEmoji = "⚡",
                description = "LPU Ultra-Fast Whisper & LLaMA 3.3 Inference"
            )

            // OpenRouter keys start with sk-or-
            key.startsWith("sk-or-") -> DetectedKeyInfo(
                provider = "openrouter",
                displayName = "OpenRouter",
                defaultModel = "deepseek/deepseek-r1",
                iconEmoji = "🔀",
                description = "Unified Routing for 200+ Frontier & Open Models"
            )

            // Perplexity keys start with pplx-
            key.startsWith("pplx-") -> DetectedKeyInfo(
                provider = "perplexity",
                displayName = "Perplexity AI",
                defaultModel = "sonar-pro",
                iconEmoji = "🔍",
                description = "Real-Time Web Search & Citation Intelligence"
            )

            // Cerebras keys start with csk-
            key.startsWith("csk-") -> DetectedKeyInfo(
                provider = "cerebras",
                displayName = "Cerebras Fast LLM",
                defaultModel = "llama3.3-70b",
                iconEmoji = "🚀",
                description = "World's Fastest 2000+ tok/s Hardware Inference"
            )

            // Fireworks AI keys start with fw_
            key.startsWith("fw_") -> DetectedKeyInfo(
                provider = "fireworks",
                displayName = "Fireworks AI",
                defaultModel = "accounts/fireworks/models/deepseek-v3",
                iconEmoji = "🎆",
                description = "Production Speculative Decoding Engine"
            )

            // Hugging Face user access tokens start with hf_
            key.startsWith("hf_") -> DetectedKeyInfo(
                provider = "huggingface",
                displayName = "Hugging Face",
                defaultModel = "Qwen/Qwen2.5-72B-Instruct",
                iconEmoji = "🤗",
                description = "Open-Source Model Hub & Serverless Inference"
            )

            // NVIDIA NIM keys start with nvapi-
            key.startsWith("nvapi-") -> DetectedKeyInfo(
                provider = "nvidia",
                displayName = "NVIDIA NIM",
                defaultModel = "meta/llama-3.3-70b-instruct",
                iconEmoji = "🟢",
                description = "GPU-Accelerated Microservices & Reasoning"
            )

            // OpenAI project/standard keys
            key.startsWith("sk-proj-") || key.startsWith("sk-svcacct-") || (key.startsWith("sk-") && key.length in 48..56) -> DetectedKeyInfo(
                provider = "openai",
                displayName = "OpenAI",
                defaultModel = "gpt-4o",
                iconEmoji = "🧠",
                description = "GPT-4o & o1 Reasoning Capabilities"
            )

            // DeepSeek API keys (typically 32-35 char alphanumeric starting with sk-)
            key.startsWith("sk-") && key.length in 32..40 -> DetectedKeyInfo(
                provider = "deepseek",
                displayName = "DeepSeek AI",
                defaultModel = "deepseek-chat",
                iconEmoji = "🔵",
                description = "DeepSeek V3 / R1 Mathematical Logic"
            )

            // Generic fallback / Custom OpenAI-compatible endpoint
            else -> DetectedKeyInfo(
                provider = "custom",
                displayName = "Custom AI Provider",
                defaultModel = "custom-model",
                iconEmoji = "🔑",
                description = "Universal API Key (Auto-Routed)"
            )
        }
    }
}
