#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux Llama Server Launcher for Secondary Brain 2.0 / Remix OS
# Launches on http://127.0.0.1:8080 with OpenAI /v1/chat/completions API
# ==============================================================================

MODELS_DIR="$HOME/models"
MODEL_PATH="${1:-$MODELS_DIR/current_model.gguf}"
PORT="${2:-8080}"
THREADS="${3:-4}"
CTX_SIZE="${4:-2048}"

if [ ! -f "$MODEL_PATH" ]; then
    echo "[-] Model file not found at: $MODEL_PATH"
    echo "[-] Running model downloader..."
    bash download_model.sh 1
fi

echo "=================================================="
echo "⚡ Starting Termux Local LLM Server on 127.0.0.1:$PORT"
echo "Model:   $MODEL_PATH"
echo "Threads: $THREADS (Tuned for big.LITTLE ARM cores)"
echo "Context: $CTX_SIZE tokens"
echo "=================================================="

# Acquire wake lock to prevent background deep sleep
termux-wake-lock || true

# Kill any existing server on port 8080
pkill -f "llama-server" || true
pkill -f "termux_llm_gateway.py" || true
sleep 1

# Check if native llama-server is installed
if command -v llama-server &> /dev/null; then
    echo "[+] Launching native ARM64 llama-server..."
    llama-server \
        -m "$MODEL_PATH" \
        -c "$CTX_SIZE" \
        -t "$THREADS" \
        --host 127.0.0.1 \
        --port "$PORT" \
        --alias local-model \
        --alias deepseek-r1 \
        --alias qwen2.5 \
        --alias llama3 \
        --log-disable &
elif [ -f "$HOME/llama.cpp/build/bin/llama-server" ]; then
    echo "[+] Launching compiled llama-server..."
    "$HOME/llama.cpp/build/bin/llama-server" \
        -m "$MODEL_PATH" \
        -c "$CTX_SIZE" \
        -t "$THREADS" \
        --host 127.0.0.1 \
        --port "$PORT" \
        --alias local-model \
        --alias deepseek-r1 \
        --alias qwen2.5 \
        --alias llama3 \
        --log-disable &
else
    echo "[!] llama-server binary not found. Launching Python LLM gateway fallback..."
    python3 termux_llm_gateway.py "$PORT" &
fi

SERVER_PID=$!
echo "[+] Local LLM Server running in background (PID: $SERVER_PID)"
echo "[+] Endpoint: http://127.0.0.1:$PORT/v1/chat/completions"
echo "[+] Health Check: http://127.0.0.1:$PORT/v1/models"
