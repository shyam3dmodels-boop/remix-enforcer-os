package com.example.core.ai

/**
 * Complete catalog of Cloud AI API Key Providers supported across Secondary Brain 2.0.
 */
data class ApiProviderInfo(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val envVarName: String,
    val baseUrl: String,
    val freeTierInfo: String,
    val signupUrl: String,
    val keyFormatHint: String,
    val models: List<String>,
    val defaultModel: String,
    val description: String
)

object ApiProviderCatalog {

    val ALL_PROVIDERS = listOf(
        // ─── 1. GROQ (FASTEST LPU INFERENCE) ──────────────────────────────
        ApiProviderInfo(
            id = "groq",
            name = "Groq Cloud",
            iconEmoji = "⚡",
            envVarName = "GROQ_API_KEY",
            baseUrl = "https://api.groq.com/openai/v1",
            freeTierInfo = "Generous Free Tier (30 RPM, 14.4k RPD on LLaMA 3.3 70B & Whisper)",
            signupUrl = "https://console.groq.com/keys",
            keyFormatHint = "gsk_...",
            models = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "llama-guard-3-8b",
                "whisper-large-v3-turbo",
                "whisper-large-v3",
                "deepseek-r1-distill-llama-70b",
                "gemma2-9b-it"
            ),
            defaultModel = "llama-3.3-70b-versatile",
            description = "Sub-200ms latency inference powered by Language Processing Units (LPUs)."
        ),

        // ─── 2. GOOGLE GEMINI ──────────────────────────────────────────────
        ApiProviderInfo(
            id = "gemini",
            name = "Google Gemini AI",
            iconEmoji = "🌟",
            envVarName = "GEMINI_API_KEY",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            freeTierInfo = "Free Tier in Google AI Studio (15 RPM, 1,500 RPD on 2.0 Flash)",
            signupUrl = "https://aistudio.google.com/app/apikey",
            keyFormatHint = "AIzaSy...",
            models = listOf(
                "gemini-2.0-flash",
                "gemini-2.0-flash-lite-preview",
                "gemini-1.5-pro",
                "gemini-1.5-flash",
                "gemini-1.5-flash-8b",
                "text-embedding-004"
            ),
            defaultModel = "gemini-2.0-flash",
            description = "Google multimodal frontier models with 1M-2M token context window & Maps grounding."
        ),

        // ─── 3. DEEPSEEK ───────────────────────────────────────────────────
        ApiProviderInfo(
            id = "deepseek",
            name = "DeepSeek AI",
            iconEmoji = "🧠",
            envVarName = "DEEPSEEK_API_KEY",
            baseUrl = "https://api.deepseek.com/v1",
            freeTierInfo = "5M Free Tokens on Signup ($0.14 - $0.55 per 1M tokens afterwards)",
            signupUrl = "https://platform.deepseek.com/api_keys",
            keyFormatHint = "sk-...",
            models = listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            ),
            defaultModel = "deepseek-reasoner",
            description = "Top-tier chain-of-thought logic (R1) & universal generalist chat (V3)."
        ),

        // ─── 4. ANTHROPIC CLAUDE ───────────────────────────────────────────
        ApiProviderInfo(
            id = "claude",
            name = "Anthropic Claude",
            iconEmoji = "✨",
            envVarName = "ANTHROPIC_API_KEY",
            baseUrl = "https://api.anthropic.com/v1",
            freeTierInfo = "$5 Free credits on signup (Pay-as-you-go)",
            signupUrl = "https://console.anthropic.com/settings/keys",
            keyFormatHint = "sk-ant-api03-...",
            models = listOf(
                "claude-3-5-sonnet-20241022",
                "claude-3-5-haiku-20241022",
                "claude-3-opus-20240229"
            ),
            defaultModel = "claude-3-5-sonnet-20241022",
            description = "Industry standard for complex multi-step reasoning, coding, and document analysis."
        ),

        // ─── 5. OPENROUTER (UNIFIED MULTI-MODEL GATEWAY) ───────────────────
        ApiProviderInfo(
            id = "openrouter",
            name = "OpenRouter",
            iconEmoji = "🟣",
            envVarName = "OPENROUTER_API_KEY",
            baseUrl = "https://openrouter.ai/api/v1",
            freeTierInfo = "Free models available (e.g. deepseek-r1:free, meta-llama/llama-3.2-3b-instruct:free)",
            signupUrl = "https://openrouter.ai/keys",
            keyFormatHint = "sk-or-v1-...",
            models = listOf(
                "deepseek/deepseek-r1",
                "deepseek/deepseek-r1:free",
                "anthropic/claude-3.5-sonnet",
                "openai/gpt-4o",
                "meta-llama/llama-3.3-70b-instruct",
                "mistralai/mistral-large-2411",
                "google/gemini-2.0-flash-exp:free"
            ),
            defaultModel = "deepseek/deepseek-r1",
            description = "Single API key routing across 200+ models with automated fallback & free tier endpoints."
        ),

        // ─── 6. OPENAI ─────────────────────────────────────────────────────
        ApiProviderInfo(
            id = "openai",
            name = "OpenAI",
            iconEmoji = "🟢",
            envVarName = "OPENAI_API_KEY",
            baseUrl = "https://api.openai.com/v1",
            freeTierInfo = "$5 Free trial credits for new accounts",
            signupUrl = "https://platform.openai.com/api-keys",
            keyFormatHint = "sk-proj-...",
            models = listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "o1",
                "o1-mini",
                "o3-mini",
                "text-embedding-3-small",
                "text-embedding-3-large"
            ),
            defaultModel = "gpt-4o-mini",
            description = "OpenAI multimodal models, structured JSON output, and reasoning series."
        ),

        // ─── 7. MISTRAL AI ─────────────────────────────────────────────────
        ApiProviderInfo(
            id = "mistral",
            name = "Mistral AI",
            iconEmoji = "🌊",
            envVarName = "MISTRAL_API_KEY",
            baseUrl = "https://api.mistral.ai/v1",
            freeTierInfo = "Free 'Experimentation' Tier with rate limits in La Plateforme",
            signupUrl = "https://console.mistral.ai/api-keys/",
            keyFormatHint = "mis_...",
            models = listOf(
                "mistral-large-latest",
                "mistral-small-latest",
                "codestral-latest",
                "pixtral-12b-2409",
                "ministral-8b-latest",
                "mistral-embed"
            ),
            defaultModel = "mistral-large-latest",
            description = "European open-weight leader with top multilingual reasoning and coding capabilities."
        ),

        // ─── 8. CEREBRAS (ULTRA-HIGH SPEED) ────────────────────────────────
        ApiProviderInfo(
            id = "cerebras",
            name = "Cerebras AI",
            iconEmoji = "🚀",
            envVarName = "CEREBRAS_API_KEY",
            baseUrl = "https://api.cerebras.ai/v1",
            freeTierInfo = "Free Tier with 1M tokens per day (1,800+ tokens/sec output speed)",
            signupUrl = "https://cloud.cerebras.ai/",
            keyFormatHint = "csk-...",
            models = listOf(
                "llama3.3-70b",
                "llama3.1-8b"
            ),
            defaultModel = "llama3.3-70b",
            description = "World's fastest inference (1,800-2,100 tokens/sec) on Wafer-Scale Engine hardware."
        ),

        // ─── 9. TOGETHER AI ────────────────────────────────────────────────
        ApiProviderInfo(
            id = "together",
            name = "Together AI",
            iconEmoji = "🤝",
            envVarName = "TOGETHER_API_KEY",
            baseUrl = "https://api.together.xyz/v1",
            freeTierInfo = "$5 Free trial credits",
            signupUrl = "https://api.together.ai/settings/api-keys",
            keyFormatHint = "tog_...",
            models = listOf(
                "deepseek-ai/DeepSeek-R1",
                "deepseek-ai/DeepSeek-V3",
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "Qwen/Qwen2.5-Coder-32B-Instruct",
                "black-forest-labs/FLUX.1-schnell"
            ),
            defaultModel = "deepseek-ai/DeepSeek-R1",
            description = "High performance serverless endpoint for DeepSeek, LLaMA, and Flux image generation."
        ),

        // ─── 10. FIREWORKS AI ──────────────────────────────────────────────
        ApiProviderInfo(
            id = "fireworks",
            name = "Fireworks AI",
            iconEmoji = "🎆",
            envVarName = "FIREWORKS_API_KEY",
            baseUrl = "https://api.fireworks.ai/inference/v1",
            freeTierInfo = "$1 Free credit on signup (sub-cent pricing)",
            signupUrl = "https://fireworks.ai/api-keys",
            keyFormatHint = "fw_...",
            models = listOf(
                "accounts/fireworks/models/deepseek-r1",
                "accounts/fireworks/models/deepseek-v3",
                "accounts/fireworks/models/llama-v3p3-70b-instruct",
                "accounts/fireworks/models/qwen2p5-coder-32b-instruct"
            ),
            defaultModel = "accounts/fireworks/models/deepseek-r1",
            description = "Ultra-fast speculative decoding inference for DeepSeek R1 and LLaMA 3.3."
        ),

        // ─── 11. SAMBANOVA CLOUD ───────────────────────────────────────────
        ApiProviderInfo(
            id = "sambanova",
            name = "SambaNova Cloud",
            iconEmoji = "⚡",
            envVarName = "SAMBANOVA_API_KEY",
            baseUrl = "https://api.sambanova.ai/v1",
            freeTierInfo = "Free Developer Tier with zero billing card required (unlimited trial)",
            signupUrl = "https://cloud.sambanova.ai/apis",
            keyFormatHint = "samba_...",
            models = listOf(
                "DeepSeek-R1",
                "DeepSeek-R1-Distill-Llama-70B",
                "Meta-Llama-3.3-70B-Instruct",
                "Qwen2.5-72B-Instruct"
            ),
            defaultModel = "DeepSeek-R1",
            description = "Free full-precision 671B DeepSeek R1 & 70B LLaMA running on DataScale SN40L chips."
        ),

        // ─── 12. PERPLEXITY AI (REALTIME SEARCH) ───────────────────────────
        ApiProviderInfo(
            id = "perplexity",
            name = "Perplexity AI",
            iconEmoji = "🔍",
            envVarName = "PERPLEXITY_API_KEY",
            baseUrl = "https://api.perplexity.ai",
            freeTierInfo = "Included with Perplexity Pro ($5 monthly API credit)",
            signupUrl = "https://www.perplexity.ai/settings/api",
            keyFormatHint = "pplx-...",
            models = listOf(
                "sonar-reasoning-pro",
                "sonar-reasoning",
                "sonar-pro",
                "sonar"
            ),
            defaultModel = "sonar-pro",
            description = "Online web search grounding and real-time live citation search synthesis."
        ),

        // ─── 13. ELEVENLABS (NEURAL VOICE SYNTHESIS) ───────────────────────
        ApiProviderInfo(
            id = "elevenlabs",
            name = "ElevenLabs Voice",
            iconEmoji = "🎙️",
            envVarName = "ELEVENLABS_API_KEY",
            baseUrl = "https://api.elevenlabs.io/v1",
            freeTierInfo = "10,000 Free characters/month (3 custom voices)",
            signupUrl = "https://elevenlabs.io/app/speech-synthesis",
            keyFormatHint = "xi-...",
            models = listOf(
                "eleven_multilingual_v2",
                "eleven_turbo_v2_5",
                "eleven_flash_v2_5"
            ),
            defaultModel = "eleven_turbo_v2_5",
            description = "Ultra-realistic human voice cloning and expressive speech synthesis."
        ),

        // ─── 14. HUGGING FACE INFERENCE ────────────────────────────────────
        ApiProviderInfo(
            id = "huggingface",
            name = "Hugging Face",
            iconEmoji = "🤗",
            envVarName = "HF_TOKEN",
            baseUrl = "https://api-inference.huggingface.co/models",
            freeTierInfo = "Free Serverless Inference API with User Access Token",
            signupUrl = "https://huggingface.co/settings/tokens",
            keyFormatHint = "hf_...",
            models = listOf(
                "deepseek-ai/DeepSeek-R1",
                "meta-llama/Llama-3.3-70B-Instruct",
                "Qwen/Qwen2.5-72B-Instruct",
                "BAAI/bge-m3"
            ),
            defaultModel = "deepseek-ai/DeepSeek-R1",
            description = "Universal open AI hub with thousands of open-source weights and embeddings."
        )
    )

    fun getProviderById(id: String): ApiProviderInfo? {
        return ALL_PROVIDERS.find { it.id.equals(id, ignoreCase = true) }
    }
}
