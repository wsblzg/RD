# Acceptance Criteria: 柴智云数字人 RAG 问答集成

**Spec:** `docs/superpowers/specs/2026-07-23-digital-human-rag-qa-design.md`  
**Date:** 2026-07-23  
**Status:** Approved

---

## Criteria

| ID | Description | Test Type | Preconditions | Expected Result |
|----|-------------|-----------|---------------|-----------------|
| AC-001 | 问答请求继续使用现有 RAG 接口 | Logic | 读取 `intelligenceApi.js` 和问答组件 | 问答调用仍为 `POST /api/ai/chatbot`，未改为数字人项目的本地知识库或星火 WebSocket |
| AC-002 | RAG 最终答案可传给数字人播报 | Logic | 运行集成契约测试 | `IntelligenceHub` 声明并触发 `answer-complete`，页面接收事件并调用数字人 `speak()` |
| AC-003 | 数字人快捷提问复用相同 RAG 链 | Logic | 运行集成契约测试 | 数字人触发 `quick-question` 后，父页面调用 `IntelligenceHub.askQuestion()`，该方法进入现有 `sendQuestion()` |
| AC-004 | 数字人配置不包含硬编码 AppKey 或静态 token | Logic | 扫描新增数字人配置和组件 | Git 跟踪文件中不存在源项目的 AppKey、静态 token 或前端 HMAC token 生成代码 |
| AC-005 | 缺少数字人 token 时文字问答仍可使用 | UI interaction | 不设置 `VITE_XILING_STATIC_TOKEN`，启动前端 | 数字人区域显示未配置提示且不创建远程 iframe；问答输入框、发送按钮和聊天记录仍正常显示 |
| AC-006 | 数字人配置完整时 URL 参数正确 | Logic | 运行配置单元测试并传入测试 token、形象 ID、背景图和音色 | 生成 URL 的 origin 为 `https://open.xiling.baidu.com`，并包含 token、figureId、ttsPer、分辨率和背景图参数 |
| AC-007 | 只处理可信曦灵消息源 | Logic | 运行配置单元测试 | `https://open.xiling.baidu.com` 返回可信，其他 origin 返回不可信 |
| AC-008 | 数字人未就绪时保留最新待播报答案 | UI interaction | 数字人未启动时完成两次问答 | 面板显示待播报状态；启动并就绪后只播报最后一次回答，不顺序播放过期回答 |
| AC-009 | 桌面端导航栏、页尾和二级导航保持存在 | UI interaction | 以 1440px 宽度打开 `/ceramics/intelligence/qa` | 顶部主导航、智鉴中枢二级导航和网站页尾均可见，当前二级导航为知识问答 |
| AC-010 | 桌面端数字人位于问答区右侧 | UI interaction | 以 1440px 宽度打开目标页面 | 问答工作区和数字人舞台为两列，数字人列位于右侧且宽度不挤压聊天输入区域 |
| AC-011 | 长对话不拉高问答与数字人面板 | UI interaction | 连续产生足以超过一屏的消息 | 问答面板外框高度保持不变，消息区域出现内部纵向滚动，数字人面板高度不随消息数量变化 |
| AC-012 | 移动端页面可用且无水平溢出 | UI interaction | 以 390px 宽度打开目标页面 | 数字人和问答区纵向排列；输入框、发送按钮、启动按钮和控制按钮均未被遮挡；页面无水平滚动条 |
| AC-013 | 数字人连接控制状态完整 | UI interaction | 配置有效 token 并启动数字人 | 页面依次可显示初始化、连接/就绪、播报、断开或错误状态，静音、打断和重新连接按钮按状态启用 |
| AC-014 | 空闲断开时不影响文字问答 | UI interaction | 数字人连接后达到空闲超时 | iframe 被释放并显示重新连接入口；问答区仍能提交问题并得到文字答案 |
| AC-015 | 前端测试和生产构建通过 | Logic | 安装现有依赖 | `node --test` 相关测试退出码为 0，`npm run build` 退出码为 0 |

