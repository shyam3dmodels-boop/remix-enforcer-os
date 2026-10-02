#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Comprehensive GGUF & Ollama Model Downloader for Termux Local AI LLM Engine
# Supports DeepSeek R1, Qwen2.5, Llama 3.2, Phi 3.5, Mistral, SmolLM2 & Vision
# ==============================================================================

MODELS_DIR="$HOME/models"
mkdir -p "$MODELS_DIR"

echo "=================================================================="
echo "🤖 Select an LLM / VLM Model to Download for Local ARM64 Execution:"
echo "=================================================================="
echo "── 🧠 DEEPSEEK REASONING FAMILY ─────────────────────────────────"
echo " 1) deepseek-r1-1.5b  (~1.1 GB, Q4_K_M) [SOTA Mobile Reasoning]"
echo " 2) deepseek-r1-7b    (~4.7 GB, Q4_K_M) [High Accuracy CoT Logic]"
echo " 3) deepseek-r1-8b    (~4.9 GB, Q4_K_M) [Llama 3.1 Distilled CoT]"
echo " 4) deepseek-r1-14b   (~9.0 GB, Q4_K_M) [Heavyweight Math & Logic]"
echo "── ⚡ ALIBABA QWEN 2.5 FAMILY ────────────────────────────────────"
echo " 5) qwen2.5-0.5b      (~390 MB, Q4_K_M) [Ultra Fast Nano Model]"
echo " 6) qwen2.5-1.5b      (~980 MB, Q4_K_M) [Best Balanced Mobile Model]"
echo " 7) qwen2.5-3b        (~1.9 GB, Q4_K_M) [Punchy 3B Dialogue]"
echo " 8) qwen2.5-7b        (~4.7 GB, Q4_K_M) [128K Multilingual Leader]"
echo " 9) qwen2.5-coder-1.5b(~980 MB, Q4_K_M) [Fast Mobile Code Assistant]"
echo "10) qwen2.5-coder-7b  (~4.7 GB, Q4_K_M) [GPT-4o-Mini Tier Coding]"
echo "── 🦙 META LLAMA FAMILY ─────────────────────────────────────────"
echo "11) llama-3.2-1b      (~800 MB, Q4_K_M) [128K Context Lightweight]"
echo "12) llama-3.2-3b      (~2.0 GB, Q4_K_M) [Meta Edge Dialogue & Tools]"
echo "13) llama-3.1-8b      (~4.7 GB, Q4_K_M) [Meta 8B Standard Baseline]"
echo "── 🔬 MICROSOFT PHI & MISTRAL FAMILY ────────────────────────────"
echo "14) phi-3.5-3.8b      (~2.2 GB, Q4_K_M) [128K Synthetic Reasoning]"
echo "15) mistral-7b        (~4.1 GB, Q4_K_M) [Classic 7B Instruction]"
echo "── 📱 HUGGINGFACE SMOLLM NANO FAMILY ────────────────────────────"
echo "16) smollm2-135m      (~90 MB,  Q4_K_M) [Microscopic Edge Model]"
echo "17) smollm2-360m      (~240 MB, Q4_K_M) [Nano Parsing Engine]"
echo "18) smollm2-1.7b      (~1.0 GB, Q4_K_M) [On-Device Powerhouse]"
echo "── 👁️ VISION & EMBEDDINGS ──────────────────────────────────────"
echo "19) moondream-1.8b    (~1.7 GB, Q4_K_M) [Tiny Edge Vision & OCR]"
echo "20) nomic-embed-text  (~274 MB, F16)    [8192 Token Vector Search]"
echo "=================================================================="

CHOICE="${1:-1}"

case $CHOICE in
    1|"deepseek-r1-1.5b")
        MODEL_NAME="DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        ;;
    2|"deepseek-r1-7b")
        MODEL_NAME="DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-7B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf"
        ;;
    3|"deepseek-r1-8b")
        MODEL_NAME="DeepSeek-R1-Distill-Llama-8B-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/unsloth/DeepSeek-R1-Distill-Llama-8B-GGUF/resolve/main/DeepSeek-R1-Distill-Llama-8B-Q4_K_M.gguf"
        ;;
    4|"deepseek-r1-14b")
        MODEL_NAME="DeepSeek-R1-Distill-Qwen-14B-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-14B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-14B-Q4_K_M.gguf"
        ;;
    5|"qwen2.5-0.5b")
        MODEL_NAME="Qwen2.5-0.5B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ;;
    6|"qwen2.5-1.5b")
        MODEL_NAME="Qwen2.5-1.5B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
        ;;
    7|"qwen2.5-3b")
        MODEL_NAME="Qwen2.5-3B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
        ;;
    8|"qwen2.5-7b")
        MODEL_NAME="Qwen2.5-7B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-7B-Instruct-GGUF/resolve/main/qwen2.5-7b-instruct-q4_k_m.gguf"
        ;;
    9|"qwen2.5-coder-1.5b")
        MODEL_NAME="Qwen2.5-Coder-1.5B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf"
        ;;
    10|"qwen2.5-coder-7b")
        MODEL_NAME="Qwen2.5-Coder-7B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/Qwen/Qwen2.5-Coder-7B-Instruct-GGUF/resolve/main/qwen2.5-coder-7b-instruct-q4_k_m.gguf"
        ;;
    11|"llama-3.2-1b")
        MODEL_NAME="Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
        ;;
    12|"llama-3.2-3b")
        MODEL_NAME="Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        ;;
    13|"llama-3.1-8b")
        MODEL_NAME="Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf"
        ;;
    14|"phi-3.5-3.8b")
        MODEL_NAME="Phi-3.5-mini-instruct-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"
        ;;
    15|"mistral-7b")
        MODEL_NAME="Mistral-7B-Instruct-v0.3-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/bartowski/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3-Q4_K_M.gguf"
        ;;
    16|"smollm2-135m")
        MODEL_NAME="smollm2-135m-instruct-q4_k_m.gguf"
        MODEL_URL="https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct-GGUF/resolve/main/smollm2-135m-instruct-q4_k_m.gguf"
        ;;
    17|"smollm2-360m")
        MODEL_NAME="smollm2-360m-instruct-q4_k_m.gguf"
        MODEL_URL="https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf"
        ;;
    18|"smollm2-1.7b")
        MODEL_NAME="smollm2-1.7b-instruct-q4_k_m.gguf"
        MODEL_URL="https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf"
        ;;
    19|"moondream-1.8b")
        MODEL_NAME="moondream2-text-model-q4_k_m.gguf"
        MODEL_URL="https://huggingface.co/vikhyatk/moondream2/resolve/main/moondream2-text-model-q4_k_m.gguf"
        ;;
    20|"nomic-embed-text")
        MODEL_NAME="nomic-embed-text-v1.5.Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/nomic-ai/nomic-embed-text-v1.5-GGUF/resolve/main/nomic-embed-text-v1.5.Q4_K_M.gguf"
        ;;
    *)
        echo "Defaulting to DeepSeek-R1 1.5B (Fast Mobile Reasoning)."
        MODEL_NAME="DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        MODEL_URL="https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        ;;
esac

TARGET_FILE="$MODELS_DIR/$MODEL_NAME"

echo "[*] Selected Model: $MODEL_NAME"
echo "[*] Destination: $TARGET_FILE"
echo "[*] Downloading with resume support (curl -C -)..."

curl -L -C - --progress-bar -o "$TARGET_FILE" "$MODEL_URL"

# Create symlink to current active model
ln -sf "$TARGET_FILE" "$MODELS_DIR/current_model.gguf"

echo "=================================================================="
echo "✅ Download Complete: $TARGET_FILE"
echo "Active Symlink: $MODELS_DIR/current_model.gguf"
echo "Launch llama-server with: bash start_llama_server.sh"
echo "=================================================================="
