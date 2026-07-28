# Acceptance Criteria: MonkeyCode 单端口部署

**Spec:** `docs/superpowers/specs/2026-07-28-monkeycode-single-port-deployment-design.md`
**Date:** 2026-07-28
**Status:** Approved

---

## Criteria

| ID | Description | Test Type | Preconditions | Expected Result |
|----|-------------|-----------|---------------|-----------------|
| AC-001 | 仓库不再包含应用运行所需的明文数据库密码或第三方服务密钥 | Logic | 检出待部署提交并运行敏感信息扫描 | `application.yml`、Docker 配置和示例环境文件仅包含环境变量引用或空示例值，扫描不命中已知旧凭据 |
| AC-002 | 部署所需环境变量有完整且不含真实凭据的示例 | Logic | 检出待部署提交 | 示例文件列出 MySQL、JWT、OSS、AI、腾讯云和 RAG 所需变量名，所有值均为空或使用明显占位值 |
| AC-003 | Git 跟踪文件均不超过 10MB | Logic | 检出待部署提交 | 遍历 `git ls-files` 后没有文件大小超过 10MB |
| AC-004 | 大视频由 OSS HTTPS 地址加载 | UI interaction | 应用已启动且首页可访问 | 浏览器加载相关页面时视频请求指向 HTTPS OSS 地址，仓库中不存在原 20.49MB 视频文件 |
| AC-005 | 可重建构建产物不进入部署仓库 | Logic | 检出待部署提交 | `vue/dist` 与 `vue/dist.zip` 未被 Git 跟踪，且 `.gitignore` 能排除二者 |
| AC-006 | Vue 可以通过锁文件进行确定性生产构建 | Logic | Node.js 与 npm 可用 | 在 `vue` 目录执行 `npm ci` 和 `npm run build` 均返回 0，并生成 `dist/index.html` |
| AC-007 | Vue 构建产物由 Spring Boot 提供 | API | 已完成统一构建并启动 Spring Boot | 请求 `http://127.0.0.1:8006/` 返回 200，响应正文包含 Vue 构建后的首页标记 |
| AC-008 | Vue history 路由刷新不会返回 404 | API | Spring Boot 已托管 Vue 构建产物 | 请求至少一个非 API 前端路由返回 200 和 `index.html`，不存在的静态资源仍返回 404 |
| AC-009 | 前端 API 使用同源相对路径 | Logic | 已完成 Vue 生产构建 | 构建配置未写入固定 MonkeyCode 域名或旧服务器 API 地址，API 默认基址为空并使用当前站点来源 |
| AC-010 | Spring Boot 对外监听唯一应用端口 | API | 应用已启动 | `8006` 监听于 `0.0.0.0`，RAG 的 `17690` 仅监听 `127.0.0.1`，部署不启动 Vite `5173` 服务 |
| AC-011 | RAG 服务使用项目级 uv 虚拟环境启动 | Logic | Python 与 uv 可用 | 项目存在 `.venv`，依赖通过 `uv sync` 或等价的 uv 项目命令安装，启动过程不调用 `pip install` |
| AC-012 | Spring Boot 能调用本机 RAG 服务 | API | RAG 与 Spring Boot 均已启动并配置有效模型凭据 | RAG 健康检查成功，调用 Spring Boot 的 RAG 问答接口不会因连接 `127.0.0.1:17690` 失败而返回错误 |
| AC-013 | MySQL 继续使用现有远程服务器 | API | MonkeyCode 出口网络可访问 `47.113.113.212:3306`，提供新数据库凭据 | 启动前只读连接检查成功，Spring Boot 启动日志没有数据库连接失败，应用可完成一项只读数据库查询 |
| AC-014 | 数据库不可达时部署快速失败 | Logic | 临时提供不可达的数据库地址 | 启动脚本在启动主应用前返回非零状态，并输出数据库连接失败信息，不安装本地 MySQL |
| AC-015 | RAG 不可用时部署不会进入半可用状态 | Logic | 临时使 RAG 启动命令失败或健康检查超时 | 启动脚本返回非零状态且不继续前台启动 Spring Boot |
| AC-016 | 单一启动入口能够恢复全部应用进程 | Logic | 依赖已安装且环境变量有效 | 执行一次仓库级启动命令后，RAG 就绪、Spring Boot 前台运行，并且无需 systemd、Supervisor、宝塔或 UFW |
| AC-017 | 文件上传继续使用 OSS | API | Spring Boot 已启动并配置轮换后的 OSS 凭据 | 上传一个小于应用限制的测试文件后返回 OSS HTTPS 地址，容器本地目录不是最终存储位置 |
| AC-018 | 平台项目地址可完成核心流程验收 | UI interaction | MonkeyCode 已为 `8006` 生成 HTTPS 地址 | 首页、前端路由、登录、至少一个数据库读取接口和 RAG 问答均可通过同一 HTTPS 来源完成 |
| AC-019 | 域名切换前保留现有线上解析 | Logic | MonkeyCode 项目地址尚未通过 AC-018 | `yaochuangfuture.cn` 的现有 DNS 记录不被修改 |
| AC-020 | 自定义域名通过 HTTPS 反向代理访问平台项目 | UI interaction | AC-018 已通过且反向代理已配置 | 访问 `https://yaochuangfuture.cn` 返回 MonkeyCode 上的新应用，证书有效，前端 API 与 RAG 请求不发生混合内容或跨域错误 |
| AC-021 | 部署说明明确环境可回收风险 | Logic | 检出待部署提交 | 部署文档说明 MonkeyCode 非生产级 VPS、可能回收，并列出源码、配置和 RAG 索引的恢复方式 |
