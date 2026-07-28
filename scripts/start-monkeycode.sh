#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"
RAG_DIR="$ROOT_DIR/RAG-Agent"
VUE_DIR="$ROOT_DIR/vue"
SPRING_DIR="$ROOT_DIR/springboot"
RUNTIME_DIR="$ROOT_DIR/.runtime"
RAG_LOG="$RUNTIME_DIR/rag.log"
RAG_PID=""

cleanup() {
  if [[ -n "$RAG_PID" ]] && kill -0 "$RAG_PID" 2>/dev/null; then
    kill "$RAG_PID" 2>/dev/null || true
    wait "$RAG_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

"$ROOT_DIR/scripts/check-deployment.sh"

while IFS='=' read -r key value || [[ -n "$key" ]]; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  key="${key%$'\r'}"
  value="${value%$'\r'}"
  [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { printf 'ERROR: 无效的环境变量名: %s\n' "$key" >&2; exit 1; }
  export "$key=$value"
done < "$ENV_FILE"

mkdir -p "$RUNTIME_DIR"

printf 'Preparing RAG environment...\n'
(
  cd "$RAG_DIR"
  uv sync --frozen --no-dev
)

printf 'Starting RAG on 127.0.0.1:17690...\n'
(
  cd "$RAG_DIR"
  HOST=127.0.0.1 PORT=17690 uv run --frozen --no-dev python main.py
) >"$RAG_LOG" 2>&1 &
RAG_PID=$!

rag_ready=false
for _ in $(seq 1 60); do
  if ! kill -0 "$RAG_PID" 2>/dev/null; then
    printf 'ERROR: RAG 启动失败，请查看 %s\n' "$RAG_LOG" >&2
    exit 1
  fi
  if curl --fail --silent --show-error "http://127.0.0.1:17690/health" >/dev/null; then
    rag_ready=true
    break
  fi
  sleep 1
done
[[ "$rag_ready" == true ]] || { printf 'ERROR: RAG 健康检查超时，请查看 %s\n' "$RAG_LOG" >&2; exit 1; }

printf 'Building Vue frontend...\n'
(
  cd "$VUE_DIR"
  npm ci
  npm run build
)

printf 'Building Spring Boot application...\n'
(
  cd "$SPRING_DIR"
  mvn -DskipTests package
)

jar_file="$(find "$SPRING_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"
[[ -n "$jar_file" ]] || { printf 'ERROR: 未找到 Spring Boot JAR\n' >&2; exit 1; }

printf 'Starting application on 0.0.0.0:8006...\n'
java -jar "$jar_file" &
APP_PID=$!
wait "$APP_PID"
