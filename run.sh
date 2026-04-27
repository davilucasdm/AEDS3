#!/usr/bin/env bash
# ── Clinica Médica — AED III — Build & Run ──────────────────────────────────
set -e

SRC_DIR="src"
BIN_DIR="bin"
DATA_DIR="data"
WEB_DIR="web"
MAIN_CLASS="clinica.Main"

echo "=== Clínica Médica — AED III ==="

# 1. Compila todos os .java
echo "[1/3] Compilando..."
find "$SRC_DIR" -name "*.java" > /tmp/sources.txt
javac -d "$BIN_DIR" @/tmp/sources.txt
echo "      OK — $(wc -l < /tmp/sources.txt) arquivos compilados"

# 2. Garante que o diretório de dados existe
mkdir -p "$DATA_DIR"

# 3. Executa
echo "[2/3] Iniciando servidor em http://localhost:8080 ..."
echo "      Pressione Ctrl+C para parar"
echo ""
java -cp "$BIN_DIR" \
     -DdataDir="$DATA_DIR" \
     -DwebDir="$WEB_DIR"   \
     -Dporta=8080           \
     "$MAIN_CLASS"
