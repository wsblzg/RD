#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

command -v java >/dev/null 2>&1 || fail "Java 17 未安装或不在 PATH"
command -v mvn >/dev/null 2>&1 || fail "Maven 未安装或不在 PATH"
command -v node >/dev/null 2>&1 || fail "Node.js 未安装或不在 PATH"
command -v npm >/dev/null 2>&1 || fail "npm 未安装或不在 PATH"
command -v uv >/dev/null 2>&1 || fail "uv 未安装或不在 PATH"
command -v curl >/dev/null 2>&1 || fail "curl 未安装或不在 PATH"

[[ -f "$ENV_FILE" ]] || fail "缺少 $ENV_FILE"

while IFS='=' read -r key value || [[ -n "$key" ]]; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  key="${key%$'\r'}"
  value="${value%$'\r'}"
  [[ "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || fail "无效的环境变量名: $key"
  export "$key=$value"
done < "$ENV_FILE"

required_vars=(
  DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET
  XILING_APP_ID XILING_APP_KEY
  OSS_ACCESS_KEY_ID OSS_ACCESS_KEY_SECRET
  XUNFEI_APP_ID XUNFEI_API_KEY XUNFEI_API_SECRET
  NVIDIA_API_KEY DEEPSEEK_API_KEY
  TENCENT_AI3D_SECRET_ID TENCENT_AI3D_SECRET_KEY
)

for name in "${required_vars[@]}"; do
  [[ -n "${!name:-}" ]] || fail "环境变量 $name 未配置"
done

java_major="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
[[ "$java_major" == "17" ]] || fail "需要 Java 17，当前主版本为 ${java_major:-unknown}"

[[ -f "$ROOT_DIR/vue/package-lock.json" ]] || fail "缺少 vue/package-lock.json"
[[ -f "$ROOT_DIR/RAG-Agent/uv.lock" ]] || fail "缺少 RAG-Agent/uv.lock"
[[ -f "$ROOT_DIR/springboot/pom.xml" ]] || fail "缺少 springboot/pom.xml"

printf 'OK: 部署前检查通过。\n'
printf 'Spring Boot: 0.0.0.0:8006\n'
printf 'RAG: 127.0.0.1:17690\n'
