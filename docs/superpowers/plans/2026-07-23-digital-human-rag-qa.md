# 柴智云数字人 RAG 问答集成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. It will decide whether each batch should run in parallel or serial subagent mode and will pass only task-local context to each subagent. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `/ceramics/intelligence/qa` 原生集成百度曦灵数字人，并让现有 RAG + AI 最终答案驱动数字人播报。

**Architecture:** `IntelligenceHub` 继续独占问答与回退逻辑，通过 `answer-complete` 输出最终答案并暴露 `askQuestion()`；`IntelligenceQaView` 只负责连接问答与数字人；`XilingRealtimeAvatar` 负责 iframe 生命周期、音色和播报状态。数字人配置使用纯函数模块从 Vite 环境变量读取，不在源码中提交凭据。

**Tech Stack:** Vue 3、Vite 6、原生 `fetch`、百度曦灵 realtime iframe、Node.js `node:test`、CSS Grid。

---

## File Map

- Create: `vue/src/config/dialectConfig.js`
  - 数字人音色列表和默认音色。
- Create: `vue/src/config/xilingRealtimeConfig.js`
  - 环境变量归一化、配置校验、可信源判断、realtime URL 构建。
- Create: `vue/src/config/xilingRealtimeConfig.test.js`
  - 配置纯函数单元测试。
- Create: `vue/src/components/xiling-realtime/XilingRealtimeAvatar.vue`
  - 数字人 UI、iframe、连接状态、待播报、控制和空闲断开。
- Create: `vue/src/views/ceramics/IntelligenceQaView.digital-human.test.js`
  - 问答与数字人的源码契约测试。
- Modify: `vue/src/components/intelligence/IntelligenceHub.vue`
  - 新增最终答案事件和公开提问方法。
- Modify: `vue/src/views/ceramics/IntelligenceQaView.vue`
  - 组合问答和数字人，增加桌面/移动布局。
- Create: `vue/.env.example`
  - 数字人环境变量说明。

### Task 1: 数字人配置纯函数

**Files:**
- Create: `vue/src/config/xilingRealtimeConfig.test.js`
- Create: `vue/src/config/xilingRealtimeConfig.js`
- Create: `vue/src/config/dialectConfig.js`

- [ ] **Step 1: Write the failing tests**

测试必须覆盖：

```js
test('missing token disables remote digital human', () => {
  const config = resolveXilingConfig({})
  assert.equal(config.configured, false)
  assert.equal(buildXilingRealtimeUrl(config, DIALECT_PRESETS[0]), '')
})

test('configured digital human URL contains required params', () => {
  const config = resolveXilingConfig({
    VITE_XILING_STATIC_TOKEN: 'test-token',
    VITE_XILING_FIGURE_ID: '353922',
    VITE_XILING_BACKGROUND_IMAGE_URL: 'https://example.com/bg.png'
  })
  const url = new URL(buildXilingRealtimeUrl(config, DIALECT_PRESETS[0]))
  assert.equal(url.origin, 'https://open.xiling.baidu.com')
  assert.equal(url.searchParams.get('token'), 'test-token')
  assert.equal(url.searchParams.get('figureId'), '353922')
  assert.equal(url.searchParams.get('ttsPer'), 'CAP_4193')
})

test('only the xiling origin is trusted', () => {
  assert.equal(isTrustedXilingOrigin('https://open.xiling.baidu.com'), true)
  assert.equal(isTrustedXilingOrigin('https://example.com'), false)
})
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
node --test src/config/xilingRealtimeConfig.test.js
```

Expected: FAIL because the config modules do not exist.

- [ ] **Step 3: Implement the minimal config modules**

`resolveXilingConfig(env)` returns:

```js
{
  token,
  figureId,
  backgroundImageUrl,
  configured: Boolean(token)
}
```

`buildXilingRealtimeUrl(config, dialect)` returns an empty string when not configured; otherwise it builds `https://open.xiling.baidu.com/cloud/realtime` with `token`、`figureId`、`initMode=noAudio`、resolution、`ttsPer`、`cp-ttsSample`、background and position parameters.

- [ ] **Step 4: Run the tests and verify GREEN**

Run:

```powershell
node --test src/config/xilingRealtimeConfig.test.js
```

Expected: 3 tests pass.

### Task 2: 问答输出契约

**Files:**
- Create: `vue/src/views/ceramics/IntelligenceQaView.digital-human.test.js`
- Modify: `vue/src/components/intelligence/IntelligenceHub.vue`

- [ ] **Step 1: Write the failing integration contract test**

测试读取 Vue 源码并断言：

```js
assert.match(hubSource, /defineEmits\(\['answer-complete'\]\)/)
assert.match(hubSource, /emit\('answer-complete', finalAnswer\)/)
assert.match(hubSource, /defineExpose\(\{[\s\S]*askQuestion/)
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
node --test src/views/ceramics/IntelligenceQaView.digital-human.test.js
```

Expected: FAIL because事件和公开方法尚不存在。

- [ ] **Step 3: Implement the minimal contract**

在 `IntelligenceHub.vue`：

```js
const emit = defineEmits(['answer-complete'])
```

远程回答和本地回退统一生成 `finalAnswer`；打字完成后：

```js
emit('answer-complete', finalAnswer)
```

并暴露：

```js
const askQuestion = async (question) => sendQuestion(String(question || ''))
defineExpose({ askQuestion })
```

- [ ] **Step 4: Run the test and verify GREEN**

Run相同命令，Expected: 对应断言通过。

### Task 3: 数字人组件

**Files:**
- Modify: `vue/src/views/ceramics/IntelligenceQaView.digital-human.test.js`
- Create: `vue/src/components/xiling-realtime/XilingRealtimeAvatar.vue`

- [ ] **Step 1: Extend the failing contract test**

增加以下断言：

```js
assert.match(avatarSource, /defineExpose\(\{[\s\S]*speak/)
assert.match(avatarSource, /pendingSpeech/)
assert.match(avatarSource, /isTrustedXilingOrigin/)
assert.doesNotMatch(avatarSource, /XILING_APP_KEY|HmacSHA256/)
```

- [ ] **Step 2: Run the test and verify RED**

Expected: FAIL because数字人组件尚不存在。

- [ ] **Step 3: Implement the component**

组件必须：

- 从 `resolveXilingConfig(import.meta.env)` 获取配置。
- 未配置时显示说明，不渲染 iframe。
- 用户点击后才创建 iframe，满足浏览器自动播放限制。
- 只处理可信 origin 的 `postMessage`。
- `speak(text)` 在未就绪时覆盖 `pendingSpeech`，就绪时立即播报。
- iframe 和 WebSocket 均就绪后自动消费最新 `pendingSpeech`。
- 提供音色切换、静音、打断、重播、90 秒空闲断开和重新连接。
- `quick-question` 只输出问题文本，不直接回答。
- 组件卸载时移除监听器和计时器。

- [ ] **Step 4: Run config and contract tests**

Run:

```powershell
node --test src/config/xilingRealtimeConfig.test.js src/views/ceramics/IntelligenceQaView.digital-human.test.js
```

Expected: all pass.

### Task 4: 页面接线和固定布局

**Files:**
- Modify: `vue/src/views/ceramics/IntelligenceQaView.digital-human.test.js`
- Modify: `vue/src/views/ceramics/IntelligenceQaView.vue`
- Modify: `vue/src/components/intelligence/IntelligenceHub.vue`

- [ ] **Step 1: Extend the failing page contract test**

增加：

```js
assert.match(viewSource, /<NewHeaderNavigation/)
assert.match(viewSource, /<SiteFooter/)
assert.match(viewSource, /<XilingRealtimeAvatar/)
assert.match(viewSource, /@answer-complete="handleAnswerComplete"/)
assert.match(viewSource, /@quick-question="handleQuickQuestion"/)
assert.match(viewSource, /avatarRef\.value\.speak/)
assert.match(viewSource, /hubRef\.value\.askQuestion/)
```

- [ ] **Step 2: Run the test and verify RED**

Expected: FAIL because页面尚未接入数字人。

- [ ] **Step 3: Implement page integration**

在页面中创建 `hubRef`、`avatarRef`：

```js
const handleAnswerComplete = (answer) => avatarRef.value?.speak(answer)
const handleQuickQuestion = (question) => hubRef.value?.askQuestion(question)
```

模板使用：

```vue
<section class="digital-qa-stage">
  <div class="digital-qa-dialogue">
    <IntelligenceHub
      ref="hubRef"
      view-mode="qa"
      @answer-complete="handleAnswerComplete"
    />
  </div>
  <aside class="digital-human-rail">
    <XilingRealtimeAvatar
      ref="avatarRef"
      @quick-question="handleQuickQuestion"
    />
  </aside>
</section>
```

CSS 要求：

- Desktop: `grid-template-columns: minmax(0, 1fr) minmax(310px, 360px)`。
- 两列高度使用 `clamp(660px, calc(100vh - 230px), 820px)`。
- 问答内部改为约 `230px + 1fr`，消息板保持 `overflow-y: auto`。
- `max-width: 1180px` 改为单列，数字人排在问答前。
- `max-width: 720px` 去除固定高度并保证按钮和输入框不溢出。

- [ ] **Step 4: Run contract tests**

Expected: all contract assertions pass.

### Task 5: 环境示例和全量验证

**Files:**
- Create: `vue/.env.example`
- Modify only if required by failing checks: files from Tasks 1-4

- [ ] **Step 1: Add non-secret environment example**

```env
VITE_API_BASE_URL=
VITE_XILING_STATIC_TOKEN=
VITE_XILING_FIGURE_ID=353922
VITE_XILING_BACKGROUND_IMAGE_URL=
```

- [ ] **Step 2: Run all frontend Node tests**

Run:

```powershell
$tests = rg --files src -g '*.test.js'
node --test $tests
```

Expected: exit code 0.

- [ ] **Step 3: Run production build**

Run:

```powershell
npm run build
```

Expected: Vite build exits 0 and produces `dist/`.

- [ ] **Step 4: Verify secret scan**

Run:

```powershell
rg -n "su187090qht36p1f7a7a|1c1d24e48d875cd99bf116ac098c248018ed900e87cf417355b70bd23d5b6940|XILING_APP_KEY|HmacSHA256" src .env.example
```

Expected: no matches.

- [ ] **Step 5: Browser verification**

Start Vite on an available local port and check `/ceramics/intelligence/qa` at 1440x1000 and 390x844:

- Navigation, sub-navigation and footer remain present.
- Desktop digital human is on the right.
- Mobile has no horizontal overflow.
- Missing-token state does not block text QA.
- Long chat scrolls inside the chat panel.

- [ ] **Step 6: Commit implementation**

```powershell
git add vue/src/config vue/src/components/xiling-realtime vue/src/components/intelligence/IntelligenceHub.vue vue/src/views/ceramics/IntelligenceQaView.vue vue/src/views/ceramics/IntelligenceQaView.digital-human.test.js vue/.env.example
git commit -m "feat: integrate digital human with RAG knowledge QA"
```

