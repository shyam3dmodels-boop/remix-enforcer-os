#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# Termux Local AI LLM Engine Setup Script for Secondary Brain 2.0 / Remix OS
# Automates packages, toolchains, llama.cpp compilation, and model directories.
# ==============================================================================

set -e

echo "=================================================="
echo "⚡ Setting up Termux Local AI LLM Engine (ARM64)"
echo "=================================================="

# 1. Update and install required build dependencies
echo "[1/4] Updating package repos and installing toolchains..."
pkg update -y
pkg install -y \
    clang \
    cmake \
    git \
    python \
    ninja \
    openblas \
    libopenblas \
    pkg-config \
    libcurl \
    termux-tools \
    curl \
    wget \
    jq

# 2. Setup Models Directory
MODELS_DIR="$HOME/models"
mkdir -p "$MODELS_DIR"
echo "[2/4] Created models directory at $MODELS_DIR"

# 3. Clone & Compile llama.cpp natively on ARM64
LLAMA_DIR="$HOME/llama.cpp"
if [ ! -d "$LLAMA_DIR" ]; then
    echo "[3/4] Cloning llama.cpp repository..."
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$LLAMA_DIR"
else
    echo "[3/4] llama.cpp already present. Updating..."
    cd "$LLAMA_DIR" && git pull || true
fi

cd "$LLAMA_DIR"
echo "[-] Compiling llama.cpp with ARM NEON and OpenBLAS..."
cmake -B build \
    -DGGML_OPENBLAS=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DGGML_NATIVE=ON

cmake --build build --config Release -j4 --target llama-server llama-cli

# Copy binary to user path for instant CLI access
if [ -f "$LLAMA_DIR/build/bin/llama-server" ]; then
    cp "$LLAMA_DIR/build/bin/llama-server" "$PREFIX/bin/llama-server"
    chmod +x "$PREFIX/bin/llama-server"
    echo "[+] llama-server successfully installed to $PREFIX/bin/llama-server"
fi

# 4. Install Python requirements for fallback gateway
echo "[4/4] Installing Python dependencies..."
pip install --upgrade pip
pip install requests uvicorn fastapi httpx urllib3 || true

# 5. Acquire Termux Wake Lock to prevent Android throttling
termux-wake-lock || true

echo "=================================================="
echo "✅ Termux Local LLM Setup Complete!"
echo "Run 'bash download_model.sh' to fetch a GGUF model."
echo "=================================================="
