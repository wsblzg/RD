# MonkeyCode 单端口部署说明

## 部署结构

- 对外服务：Spring Boot `0.0.0.0:8006`
- 前端：Vue 构建产物打入 Spring Boot JAR，由同一端口托管
- RAG：仅监听 `127.0.0.1:17690`，不对公网暴露
- 数据库：继续连接原远程 MySQL，不在 MonkeyCode 内安装 MySQL
- 文件存储：继续使用现有阿里云 OSS

## 前置条件

MonkeyCode 环境需要提供：

- Java 17
- Maven
- Node.js 与 npm
- Python 3.11
- uv
- curl

根目录 `.env` 已保存实际配置且被 Git 忽略。不要提交、打印或通过聊天发送该文件。

## 获取代码

优先使用 Git 拉取源码，避免网页的单文件上传限制：

```bash
git clone "$REPO_URL" moneycode
cd moneycode
```

如果实例被回收，重新拉取仓库后，从安全备份恢复根目录 `.env`。

## 检查与启动

```bash
chmod +x scripts/check-deployment.sh scripts/start-monkeycode.sh
./scripts/check-deployment.sh
./scripts/start-monkeycode.sh
```

启动脚本会依次执行：

1. 校验运行时和必要环境变量。
2. 使用 `uv sync --frozen --no-dev` 准备 RAG 环境。
3. 在 `127.0.0.1:17690` 启动 RAG 并检查 `/health`。
4. 使用 `npm ci && npm run build` 构建 Vue。
5. 使用 Maven 将 `vue/dist` 打入 Spring Boot JAR。
6. 在 `0.0.0.0:8006` 启动唯一公网服务。

RAG 启动日志写入 `.runtime/rag.log`。脚本退出时会同时停止 RAG 子进程。

## 平台端口配置

MonkeyCode 只需暴露应用端口：

```text
8006
```

不要暴露 `17690`，它仅供 Spring Boot 通过回环地址调用。

## 验收

应用启动后检查：

```bash
curl -I http://127.0.0.1:8006/
curl -I http://127.0.0.1:8006/ceramics/home
curl http://127.0.0.1:17690/health
```

预期：

- `/` 和 `/ceramics/home` 返回前端页面。
- `/assets/不存在.js` 返回 404，而不是 `index.html`。
- `/api/不存在` 返回 404，而不是 `index.html`。
- RAG 健康检查返回 `{"status":"ok"}`。
- Spring Boot 监听 `0.0.0.0:8006`。
- RAG 只监听 `127.0.0.1:17690`。

随后在页面验证：

- 登录与验证码流程。
- 一个只读接口，例如商品或藏品列表。
- OSS 图片上传。
- 柴烧知识问答调用 RAG。

## 域名切换

先取得 MonkeyCode 提供的 HTTPS 访问地址并完成上述验收，再在现有域名服务商或反向代理中将：

- `yaochuangfuture.cn`
- `www.yaochuangfuture.cn`

转发到该 HTTPS 地址。保留原服务作为回滚入口，确认 HTTPS、静态资源、API 与上传都正常后再正式切换。

## 构建验证

在提交或部署前执行：

```bash
cd RAG-Agent && uv sync --frozen && RAG_BUILD_INDEX_ON_STARTUP=false uv run --frozen pytest
cd ../vue && npm ci && npm run build
cd ../springboot && mvn test && mvn -DskipTests package
```

说明：代码交付环境若无法执行终端命令，必须在 MonkeyCode 实例或可用 CI 中补跑以上命令；只有三组命令全部通过，才可视为运行时验收完成。

## 当前暂缓项

本轮按要求不处理：

- `vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4` 的 OSS 迁移。
- “所有 Git 文件小于 10MB”的限制。

因此，如果最终使用网页上传而不是 Git 拉取，该视频仍会受到 10MB 单文件限制；不要删除或改动它，后续获得授权后再迁移。

## 清理与恢复

构建验证后可以删除以下可重建产物：

```text
springboot/target/
vue/dist/
RAG-Agent/.venv/
RAG-Agent/__pycache__/
RAG-Agent/.pytest_cache/
.runtime/
```

必须保留：

```text
.env
vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4
vue/dist.zip
```

RAG 的 `vector_db` 可由 `RAG-Agent/docs` 重新生成；若需要缩短冷启动时间，可另外做非公开备份，但不要提交敏感日志或 `.env`。
