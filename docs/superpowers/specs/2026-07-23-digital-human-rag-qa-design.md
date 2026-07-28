# 柴智云数字人 RAG 问答集成设计

**日期：** 2026-07-23  
**状态：** 已批准（自主执行）  
**目标页面：** `/ceramics/intelligence/qa`

## 1. 目标

把 `D:\Users\DAS\Desktop\数字人` 中的百度曦灵实时 2D 数字人能力迁移到柴智云现有问答页面，同时保留现有网站导航栏、页尾、路由和 `/api/ai/chatbot` RAG + AI 问答链。

数字人是回答展示层，不建立第二套知识库，也不替代后端问答服务。用户提交问题后，页面继续调用现有 RAG 接口；同一份最终答案既显示在聊天区，也交给数字人播报。

## 2. 方案比较

### 方案 A：原生 Vue 组件迁移（采用）

- 从数字人项目迁移曦灵 iframe、音色切换、连接状态、静音、打断和空闲断开能力。
- 在现有 `IntelligenceQaView.vue` 中组合问答组件和数字人组件。
- 通过 Vue 事件和 `defineExpose` 连接问答与数字人，不复制原项目的本地知识库。

优点：与现有 Vue 3 工程、路由和视觉体系一致；可直接共享问答状态；移动端适配和错误提示可统一处理。

### 方案 B：嵌入完整数字人项目 iframe（不采用）

优点：迁移量小。  
缺点：形成双重页面和双重问答逻辑，样式、路由、响应式、鉴权和通信都更难维护。

### 方案 C：独立微前端部署（不采用）

优点：两个工程边界清晰。  
缺点：对当前比赛交付规模过度设计，会增加部署、跨域和版本同步成本。

## 3. 页面结构

保留 `NewHeaderNavigation`、问答页标题区、智鉴中枢二级导航和 `SiteFooter`。

标题区下方新增 `digital-qa-stage`：

- 左侧：现有 `IntelligenceHub`，保留快速咨询、问题方向、答复深度、连续对话、Markdown 回答、参考标签和本地知识回退。
- 右侧：新的 `XilingRealtimeAvatar`，包含数字人标题、连接状态、方言音色、数字人画面、启动遮罩、加载状态、待播报提示、静音、打断和重播控制。
- 桌面端：两列布局，数字人固定在右侧。
- 小于 1180px：改为上下布局，数字人舞台先展示，问答区随后展示。
- 小于 720px：缩减间距和圆角，数字人控制按钮换行，聊天输入框保持可触达。

问答主面板和数字人面板采用受控高度。聊天消息只在消息面板内部滚动，数字人画面和控制区不因聊天内容增加而拉长。

## 4. 数据流

1. 用户在 `IntelligenceHub` 输入问题或点击快速咨询。
2. `IntelligenceHub` 调用现有 `askRagQuestion()`。
3. API 成功时显示后端 RAG + AI 答案；API 失败或回答资料不足时使用现有本地检索结果回退。
4. 最终显示文本完成后，`IntelligenceHub` 触发 `answer-complete`，参数为最终答案。
5. `IntelligenceQaView` 调用数字人组件暴露的 `speak(answer)`。
6. 数字人未启动或未就绪时只保存最新一条待播报文本；用户启动并连接成功后自动播报。
7. 数字人快捷提问触发 `quick-question`，父页面调用 `IntelligenceHub.askQuestion(question)`，仍走相同 RAG 链。

## 5. 数字人配置与安全

- 不迁移源码中的 AppKey、静态 token 或前端 HMAC 签名逻辑。
- 前端只读取以下 Vite 环境变量：
  - `VITE_XILING_STATIC_TOKEN`
  - `VITE_XILING_FIGURE_ID`
  - `VITE_XILING_BACKGROUND_IMAGE_URL`
- 形象 ID 可使用非敏感默认值；token 缺失时不创建远程 iframe，显示“数字人未配置”，但文字问答保持可用。
- `postMessage` 接收端只接受 `https://open.xiling.baidu.com`。
- `iframe` 使用明确的 `title`、`allow="autoplay"` 和 `referrerpolicy`。
- 提供 `.env.example` 字段说明，不在 Git 中提交真实凭据。

## 6. 组件边界

### `vue/src/config/xilingRealtimeConfig.js`

负责环境变量归一化、配置校验、可信源判断和曦灵 URL 构建。模块保持纯函数，便于使用 Node 内置测试运行器测试。

### `vue/src/config/dialectConfig.js`

负责音色列表、默认音色和按 key 获取音色。只保留数字人发声所需元数据，不带本地问答资料。

### `vue/src/components/xiling-realtime/XilingRealtimeAvatar.vue`

负责 iframe 生命周期、消息通信、语音播报队列、连接状态、空闲释放和控制 UI。对父组件暴露 `speak()`、`interrupt()`、`reconnect()`。

### `vue/src/components/intelligence/IntelligenceHub.vue`

保留原问答职责，新增：

- `answer-complete` 事件，输出最终答案。
- `askQuestion()` 暴露方法，允许数字人快捷问题复用同一问答链。

### `vue/src/views/ceramics/IntelligenceQaView.vue`

作为集成容器，连接问答与数字人事件，控制页面两列/单列布局，不接管具体问答或数字人内部逻辑。

## 7. 错误和边界状态

- RAG API 失败：保持现有本地知识回退，并把回退答案交给数字人播报。
- 数字人未配置：显示配置提示，不影响问答输入、历史消息和参考标签。
- 数字人尚未启动：缓存最新答案，显示待播报状态。
- iframe 加载或 WebSocket 失败：显示连接异常和重新连接按钮。
- 90 秒无操作：释放数字人 iframe，文字问答保持在线，用户可重新连接。
- 连续快速回答：只保留最新待播报答案，避免旧答案排队造成长时间滞后。
- 组件卸载：移除 `message` 监听器并清理定时器。

## 8. 测试与验证

- 使用 `node:test` 测试配置归一化、缺失 token、URL 参数和可信源。
- 使用源码契约测试确认问答事件、公开方法和页面组件接线存在。
- 运行现有前端 Node 测试。
- 运行 `npm run build` 验证 Vue/Vite 生产构建。
- 启动本地 Vite，使用桌面和移动视口检查布局、内部滚动、启动遮罩、未配置状态和问答交互。
- 通过本地代理或线上接口验证 `/api/ai/chatbot` 仍被调用，不引入数字人项目原有的本地回答链。

