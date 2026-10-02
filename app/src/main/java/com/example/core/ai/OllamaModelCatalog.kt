package com.example.core.ai

/**
 * Complete catalog of Ollama & GGUF models available for local download on mobile ARM64 (Termux)
 * and desktop/server systems.
 */
data class OllamaModelInfo(
    val name: String,
    val family: String,
    val parameterSize: String,
    val downloadSize: String,
    val quantization: String,
    val contextWindow: String,
    val minRamRequired: String,
    val recommendedTier: DeviceTier,
    val description: String,
    val pullCommand: String,
    val directGgufUrl: String? = null
)

enum class DeviceTier {
    MOBILE_LOW_END,   // 4GB - 6GB RAM (SmolLM, Qwen 0.5B, Qwen 1.5B, Llama 3.2 1B)
    MOBILE_FLAGSHIP,  // 8GB - 12GB RAM (DeepSeek R1 1.5B/7B, Qwen 3B/7B, Llama 3.2 3B, Phi 3.5)
    DESKTOP_SERVER    // 16GB - 64GB+ RAM (DeepSeek R1 14B/32B/70B, Llama 3.3 70B, Qwen 32B/72B)
}

object OllamaModelCatalog {

    val ALL_MODELS = listOf(
        // ─── 1. DEEPSEEK REASONING & CHAT FAMILY ──────────────────────────
        OllamaModelInfo(
            name = "deepseek-r1:1.5b",
            family = "DeepSeek",
            parameterSize = "1.5B",
            downloadSize = "1.1 GB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "3.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Distilled Qwen-based mathematical and reasoning champion. Extremely fast on Android ARM64.",
            pullCommand = "ollama run deepseek-r1:1.5b",
            directGgufUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        ),
        OllamaModelInfo(
            name = "deepseek-r1:7b",
            family = "DeepSeek",
            parameterSize = "7B",
            downloadSize = "4.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "64K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "High accuracy reasoning model distilled from Qwen2.5-7B with full chain-of-thought logic.",
            pullCommand = "ollama run deepseek-r1:7b",
            directGgufUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-7B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf"
        ),
        OllamaModelInfo(
            name = "deepseek-r1:8b",
            family = "DeepSeek",
            parameterSize = "8B",
            downloadSize = "4.9 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "DeepSeek reasoning distilled from Meta LLaMA-3.1-8B.",
            pullCommand = "ollama run deepseek-r1:8b"
        ),
        OllamaModelInfo(
            name = "deepseek-r1:14b",
            family = "DeepSeek",
            parameterSize = "14B",
            downloadSize = "9.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "64K",
            minRamRequired = "16.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Heavyweight math and code reasoning distilled into Qwen 14B.",
            pullCommand = "ollama run deepseek-r1:14b"
        ),
        OllamaModelInfo(
            name = "deepseek-r1:32b",
            family = "DeepSeek",
            parameterSize = "32B",
            downloadSize = "20.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "64K",
            minRamRequired = "32.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Frontier-level competitive logic matching OpenAI o1-preview on STEM benchmarks.",
            pullCommand = "ollama run deepseek-r1:32b"
        ),
        OllamaModelInfo(
            name = "deepseek-r1:70b",
            family = "DeepSeek",
            parameterSize = "70B",
            downloadSize = "43.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "64.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Distilled into LLaMA-3.3-70B for maximum open-weight reasoning capacity.",
            pullCommand = "ollama run deepseek-r1:70b"
        ),
        OllamaModelInfo(
            name = "deepseek-v3:latest",
            family = "DeepSeek",
            parameterSize = "671B MoE",
            downloadSize = "404 GB",
            quantization = "Q4_K_M",
            contextWindow = "64K",
            minRamRequired = "450 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Full 671B Mixture-of-Experts benchmark leader from DeepSeek AI.",
            pullCommand = "ollama run deepseek-v3"
        ),

        // ─── 2. ALIBABA QWEN 2.5 FAMILY ────────────────────────────────────
        OllamaModelInfo(
            name = "qwen2.5:0.5b",
            family = "Qwen",
            parameterSize = "0.5B",
            downloadSize = "398 MB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "1.5 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Ultra-compact nano model. Blazing fast keyword extraction, classification, and device intent parsing.",
            pullCommand = "ollama run qwen2.5:0.5b",
            directGgufUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ),
        OllamaModelInfo(
            name = "qwen2.5:1.5b",
            family = "Qwen",
            parameterSize = "1.5B",
            downloadSize = "986 MB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "3.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Top mobile balance. Outstanding multilingual and formatting comprehension.",
            pullCommand = "ollama run qwen2.5:1.5b",
            directGgufUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        ),
        OllamaModelInfo(
            name = "qwen2.5:3b",
            family = "Qwen",
            parameterSize = "3B",
            downloadSize = "1.9 GB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "4.5 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Punchy 3B parameters with exceptional general dialogue and tool calling support.",
            pullCommand = "ollama run qwen2.5:3b"
        ),
        OllamaModelInfo(
            name = "qwen2.5:7b",
            family = "Qwen",
            parameterSize = "7B",
            downloadSize = "4.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Generalist standard with 128k context support and strong multilingual capabilities in 29+ languages.",
            pullCommand = "ollama run qwen2.5:7b"
        ),
        OllamaModelInfo(
            name = "qwen2.5-coder:1.5b",
            family = "Qwen",
            parameterSize = "1.5B",
            downloadSize = "986 MB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "3.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Ultra-fast code completion and script generation tuned specifically for developers.",
            pullCommand = "ollama run qwen2.5-coder:1.5b"
        ),
        OllamaModelInfo(
            name = "qwen2.5-coder:7b",
            family = "Qwen",
            parameterSize = "7B",
            downloadSize = "4.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "High capability code synthesis rivaling GPT-4o-mini in Python, Kotlin, TypeScript, and SQL.",
            pullCommand = "ollama run qwen2.5-coder:7b"
        ),
        OllamaModelInfo(
            name = "qwen2.5:32b",
            family = "Qwen",
            parameterSize = "32B",
            downloadSize = "20.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "32.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Flagship coding and multilingual intelligence.",
            pullCommand = "ollama run qwen2.5:32b"
        ),

        // ─── 3. META LLAMA FAMILY ──────────────────────────────────────────
        OllamaModelInfo(
            name = "llama3.2:1b",
            family = "Meta Llama",
            parameterSize = "1B",
            downloadSize = "1.3 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "2.5 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Meta ultra-compact lightweight model with massive 128k context window support.",
            pullCommand = "ollama run llama3.2:1b",
            directGgufUrl = "https://huggingface.co/unsloth/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ),
        OllamaModelInfo(
            name = "llama3.2:3b",
            family = "Meta Llama",
            parameterSize = "3B",
            downloadSize = "2.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "4.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Meta highly optimized edge dialogue model with tool use and structured outputs.",
            pullCommand = "ollama run llama3.2:3b",
            directGgufUrl = "https://huggingface.co/unsloth/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        ),
        OllamaModelInfo(
            name = "llama3.2-vision:11b",
            family = "Meta Llama",
            parameterSize = "11B",
            downloadSize = "7.9 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "14.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Multimodal vision & image reasoning model capable of reading charts, OCR, and spatial layouts.",
            pullCommand = "ollama run llama3.2-vision:11b"
        ),
        OllamaModelInfo(
            name = "llama3.3:70b",
            family = "Meta Llama",
            parameterSize = "70B",
            downloadSize = "43.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "64.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Meta latest 70B flagship delivering 405B-grade performance at 70B efficiency.",
            pullCommand = "ollama run llama3.3:70b"
        ),
        OllamaModelInfo(
            name = "llama3.1:8b",
            family = "Meta Llama",
            parameterSize = "8B",
            downloadSize = "4.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Versatile 8B parameter industry baseline with 128k context support.",
            pullCommand = "ollama run llama3.1:8b"
        ),

        // ─── 4. MICROSOFT PHI FAMILY ───────────────────────────────────────
        OllamaModelInfo(
            name = "phi4:14b",
            family = "Microsoft Phi",
            parameterSize = "14B",
            downloadSize = "9.1 GB",
            quantization = "Q4_K_M",
            contextWindow = "16K",
            minRamRequired = "16.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "State of the art 14B reasoning model trained on synthetic textbook datasets by Microsoft.",
            pullCommand = "ollama run phi4"
        ),
        OllamaModelInfo(
            name = "phi3.5:3.8b",
            family = "Microsoft Phi",
            parameterSize = "3.8B",
            downloadSize = "2.2 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "5.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "High-density 128k context reasoning model matching much larger 7B-8B architectures.",
            pullCommand = "ollama run phi3.5"
        ),

        // ─── 5. MISTRAL AI FAMILY ──────────────────────────────────────────
        OllamaModelInfo(
            name = "mistral:7b-instruct",
            family = "Mistral AI",
            parameterSize = "7B",
            downloadSize = "4.1 GB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "The classic, reliable 7B instruction fine-tune with fast inference.",
            pullCommand = "ollama run mistral"
        ),
        OllamaModelInfo(
            name = "mistral-nemo:12b",
            family = "Mistral AI",
            parameterSize = "12B",
            downloadSize = "7.1 GB",
            quantization = "Q4_K_M",
            contextWindow = "128K",
            minRamRequired = "12.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Co-developed with NVIDIA. 128k context with Tekken tokenizer for multilingual speed.",
            pullCommand = "ollama run mistral-nemo"
        ),
        OllamaModelInfo(
            name = "mixtral:8x7b",
            family = "Mistral AI",
            parameterSize = "47B MoE",
            downloadSize = "26.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "32.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Pioneering sparse Mixture-of-Experts routing queries through 2 of 8 expert networks.",
            pullCommand = "ollama run mixtral"
        ),
        OllamaModelInfo(
            name = "codestral:22b",
            family = "Mistral AI",
            parameterSize = "22B",
            downloadSize = "13.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "32K",
            minRamRequired = "20.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Specialized generative coding model trained on 80+ programming languages.",
            pullCommand = "ollama run codestral"
        ),

        // ─── 6. GOOGLE GEMMA FAMILY ────────────────────────────────────────
        OllamaModelInfo(
            name = "gemma2:2b",
            family = "Google Gemma",
            parameterSize = "2B",
            downloadSize = "1.6 GB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "3.5 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Google DeepMind lightweight architecture distilled from larger Gemini models.",
            pullCommand = "ollama run gemma2:2b"
        ),
        OllamaModelInfo(
            name = "gemma2:9b",
            family = "Google Gemma",
            parameterSize = "9B",
            downloadSize = "5.5 GB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "10.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Outperforms many older 13B-30B models across general knowledge and science reasoning.",
            pullCommand = "ollama run gemma2:9b"
        ),
        OllamaModelInfo(
            name = "gemma2:27b",
            family = "Google Gemma",
            parameterSize = "27B",
            downloadSize = "16.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "24.0 GB",
            recommendedTier = DeviceTier.DESKTOP_SERVER,
            description = "Heavyweight Google open model for desktop and workstation inference.",
            pullCommand = "ollama run gemma2:27b"
        ),

        // ─── 7. HUGGINGFACE SMOLLM FAMILY ──────────────────────────────────
        OllamaModelInfo(
            name = "smollm2:135m",
            family = "SmolLM",
            parameterSize = "135M",
            downloadSize = "90 MB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "500 MB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Microscopic model for extreme edge IoT and background classification tasks.",
            pullCommand = "ollama run smollm2:135m"
        ),
        OllamaModelInfo(
            name = "smollm2:360m",
            family = "SmolLM",
            parameterSize = "360M",
            downloadSize = "240 MB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "1.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Ultra-fast nano model for real-time mobile on-device parsing.",
            pullCommand = "ollama run smollm2:360m"
        ),
        OllamaModelInfo(
            name = "smollm2:1.7b",
            family = "SmolLM",
            parameterSize = "1.7B",
            downloadSize = "1.0 GB",
            quantization = "Q4_K_M",
            contextWindow = "8K",
            minRamRequired = "3.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Trained on synthetic educational web data. Impressive commonsense dialogue on phone hardware.",
            pullCommand = "ollama run smollm2:1.7b",
            directGgufUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf"
        ),

        // ─── 8. EMBEDDING & VECTOR MODELS ──────────────────────────────────
        OllamaModelInfo(
            name = "nomic-embed-text:latest",
            family = "Embeddings",
            parameterSize = "137M",
            downloadSize = "274 MB",
            quantization = "F16",
            contextWindow = "8K",
            minRamRequired = "1.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Top performing 8192 context text embedding model for RAG and semantic vector search.",
            pullCommand = "ollama pull nomic-embed-text"
        ),
        OllamaModelInfo(
            name = "bge-m3:latest",
            family = "Embeddings",
            parameterSize = "567M",
            downloadSize = "567 MB",
            quantization = "F16",
            contextWindow = "8K",
            minRamRequired = "2.0 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Multi-functionality, multi-lingual, and multi-granularity dense vector search model.",
            pullCommand = "ollama pull bge-m3"
        ),
        OllamaModelInfo(
            name = "all-minilm:latest",
            family = "Embeddings",
            parameterSize = "33M",
            downloadSize = "67 MB",
            quantization = "F16",
            contextWindow = "512",
            minRamRequired = "500 MB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Lightweight sentence transformer for quick vector embeddings on mobile.",
            pullCommand = "ollama pull all-minilm"
        ),

        // ─── 9. VISION & MULTIMODAL MODELS ─────────────────────────────────
        OllamaModelInfo(
            name = "moondream:1.8b",
            family = "Vision",
            parameterSize = "1.8B",
            downloadSize = "1.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "2K",
            minRamRequired = "3.5 GB",
            recommendedTier = DeviceTier.MOBILE_LOW_END,
            description = "Tiny vision model capable of running on edge devices for visual question answering.",
            pullCommand = "ollama run moondream"
        ),
        OllamaModelInfo(
            name = "llava:7b",
            family = "Vision",
            parameterSize = "7B",
            downloadSize = "4.7 GB",
            quantization = "Q4_K_M",
            contextWindow = "4K",
            minRamRequired = "8.0 GB",
            recommendedTier = DeviceTier.MOBILE_FLAGSHIP,
            description = "Large Language and Vision Assistant combining CLIP vision encoder with LLaMA.",
            pullCommand = "ollama run llava"
        )
    )

    fun getModelsByTier(tier: DeviceTier): List<OllamaModelInfo> {
        return ALL_MODELS.filter { it.recommendedTier == tier }
    }

    fun searchModels(query: String): List<OllamaModelInfo> {
        val q = query.trim().lowercase()
        return ALL_MODELS.filter {
            it.name.lowercase().contains(q) ||
            it.family.lowercase().contains(q) ||
            it.description.lowercase().contains(q)
        }
    }
}
