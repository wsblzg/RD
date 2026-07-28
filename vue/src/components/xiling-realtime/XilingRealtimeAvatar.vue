<template>
  <section class="xiling-panel" aria-labelledby="xiling-title">
    <header class="xiling-header">
      <div class="xiling-heading">
        <img src="/logo.webp" alt="" class="xiling-brand-logo" />
        <div>
          <p class="xiling-eyebrow">DIGITAL DOCENT</p>
          <h2 id="xiling-title">柴智云数字讲解员</h2>
        </div>
      </div>
      <span class="xiling-status" :class="`is-${connectionStatus}`">
        <i aria-hidden="true"></i>
        {{ statusText }}
      </span>
    </header>

    <div class="xiling-dialect-bar">
      <span class="xiling-dialect-title">讲解音色</span>
      <div class="xiling-dialect-list" role="list" aria-label="数字人讲解音色">
        <button
          v-for="dialect in DIALECT_PRESETS"
          :key="dialect.key"
          type="button"
          class="xiling-dialect-button"
          :class="{ active: activeDialectKey === dialect.key }"
          :aria-pressed="activeDialectKey === dialect.key"
          :title="dialect.description"
          @click="switchDialect(dialect.key)"
        >
          <span>{{ dialect.short }}</span>
          {{ dialect.label }}
        </button>
      </div>
    </div>

    <div class="xiling-viewport">
      <iframe
        v-if="config.configured && hasUserGesture"
        :key="iframeKey"
        ref="iframeRef"
        class="xiling-iframe"
        :src="iframeUrl"
        title="柴智云实时数字讲解员"
        allow="autoplay"
        @load="onIframeLoad"
      ></iframe>

      <div v-if="!config.configured" class="xiling-overlay xiling-unconfigured">
        <div class="xiling-orbit-mark" aria-hidden="true">
          <img src="/logo.webp" alt="" class="xiling-orbit-logo" />
        </div>
        <p class="xiling-overlay-kicker">DIGITAL HUMAN OFFLINE</p>
        <h3>数字人服务尚未配置</h3>
        <p>当前仍可正常使用柴烧知识问答。部署时配置曦灵访问令牌后，即可启用实时讲解。</p>
      </div>

      <div
        v-else-if="!hasUserGesture && !isIdleDisconnected"
        class="xiling-overlay xiling-start"
      >
        <div class="xiling-orbit-mark" aria-hidden="true">
          <img src="/logo.webp" alt="" class="xiling-orbit-logo" />
        </div>
        <p class="xiling-overlay-kicker">REALTIME CERAMIC GUIDE</p>
        <h3>让答案被看见，也被听见</h3>
        <p>
          窑火已候，答案一到，讲解员便会循着柴烧文脉徐徐开口；初次连线时，请给它几秒与火色同频。
        </p>
        <button type="button" class="xiling-primary-button" @click="startDigitalHuman">
          启动数字讲解员
        </button>
      </div>

      <div v-else-if="isIdleDisconnected" class="xiling-overlay xiling-idle">
        <div class="xiling-orbit-mark" aria-hidden="true">
          <img src="/logo.webp" alt="" class="xiling-orbit-logo" />
        </div>
        <p class="xiling-overlay-kicker">RESOURCE RELEASED</p>
        <h3>数字人连接已释放</h3>
        <p>连续 {{ idleTimeoutLabel }}未操作，远程画面已自动断开。文字问答不受影响。</p>
        <button type="button" class="xiling-primary-button" @click="reconnect">
          重新连接
        </button>
      </div>

      <div
        v-else-if="showLoadingOverlay"
        class="xiling-overlay xiling-loading"
        aria-live="polite"
      >
        <span class="xiling-loader" aria-hidden="true">
          <i></i><i></i><i></i>
        </span>
        <p class="xiling-overlay-kicker">CONNECTING</p>
        <h3>正在唤醒数字讲解员</h3>
        <p>正在建立视频与语音信令连接，请稍候。</p>
      </div>

      <div v-if="pendingSpeech" class="xiling-pending" aria-live="polite">
        <span>待播报</span>
        <p>{{ pendingPreview }}</p>
      </div>

      <div class="xiling-viewport-caption" aria-hidden="true">
        <span>火</span>
        <i></i>
        <span>土</span>
        <i></i>
        <span>声</span>
      </div>
    </div>

    <div class="xiling-console">
      <div class="xiling-idle-row">
        <span>{{ activeDialect.label }} · {{ activeDialect.region }}</span>
        <span v-if="showIdleCountdown">
          {{ idleSecondsLeft }} 秒后释放连接
        </span>
        <span v-else>RAG 答案实时播报</span>
      </div>

      <div class="xiling-quick-list" aria-label="快捷提问">
        <button
          v-for="item in quickQuestions"
          :key="item"
          type="button"
          @click="askQuickQuestion(item)"
        >
          {{ item }}
        </button>
      </div>

      <div class="xiling-action-list">
        <button
          type="button"
          class="xiling-action-button"
          :disabled="!isSpeaking && !pendingSpeech"
          @click="interrupt"
        >
          打断
        </button>
        <button
          type="button"
          class="xiling-action-button"
          :class="{ active: isMuted }"
          :disabled="!hasUserGesture || isIdleDisconnected"
          @click="toggleMute"
        >
          {{ isMuted ? '取消静音' : '静音' }}
        </button>
        <button
          type="button"
          class="xiling-action-button"
          @click="replayWelcome"
        >
          重播欢迎语
        </button>
        <button
          type="button"
          class="xiling-action-button"
          :disabled="!config.configured"
          @click="reconnect"
        >
          重新连接
        </button>
      </div>

      <p v-if="errorDetail" class="xiling-error" role="alert">
        {{ errorDetail }}
      </p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {
  buildXilingRealtimeUrl,
  isTrustedXilingOrigin,
  resolveXilingAccessToken,
  resolveXilingConfig
} from '@/config/xilingRealtimeConfig'
import {
  DEFAULT_DIALECT_KEY,
  DIALECT_PRESETS,
  getDialectByKey
} from '@/config/dialectConfig'

const emit = defineEmits(['quick-question'])

const config = resolveXilingConfig(import.meta.env)
const configuredIdleTimeoutSeconds = Number.parseInt(
  import.meta.env.VITE_XILING_IDLE_TIMEOUT_SECONDS,
  10
)
const IDLE_TIMEOUT_SECONDS =
  Number.isFinite(configuredIdleTimeoutSeconds) && configuredIdleTimeoutSeconds > 0
    ? configuredIdleTimeoutSeconds
    : 90
const IDLE_TIMEOUT_MS = IDLE_TIMEOUT_SECONDS * 1000
const WELCOME_TEXT =
  '欢迎来到柴智云。您可以询问柴烧工艺、窑变机理、器型审美和非遗传承等问题。'

const quickQuestions = [
  '柴烧为什么会形成自然落灰釉？',
  '新手如何观察柴烧作品的火痕？',
  '曲江柴烧有哪些非遗传承特色？'
]

const iframeRef = ref(null)
const iframeUrl = ref('')
const iframeKey = ref(0)
const hasUserGesture = ref(false)
const activeDialectKey = ref(DEFAULT_DIALECT_KEY)
const videoReady = ref(false)
const wsConnected = ref(false)
const isSpeaking = ref(false)
const isMuted = ref(false)
const isIdleDisconnected = ref(false)
const connectionStatus = ref(config.configured ? 'standby' : 'unconfigured')
const errorDetail = ref('')
const pendingSpeech = ref('')
const idleSecondsLeft = ref(IDLE_TIMEOUT_MS / 1000)

let idleTimer = null
let idleCountdownTimer = null
let audioWakeTimer = null

const activeDialect = computed(() => getDialectByKey(activeDialectKey.value))
const transportReady = computed(() => videoReady.value && wsConnected.value)
const readyToSpeak = computed(
  () =>
    config.configured &&
    hasUserGesture.value &&
    !isIdleDisconnected.value &&
    transportReady.value
)

const showLoadingOverlay = computed(
  () =>
    config.configured &&
    hasUserGesture.value &&
    !isIdleDisconnected.value &&
    !readyToSpeak.value &&
    connectionStatus.value !== 'error'
)

const showIdleCountdown = computed(
  () => readyToSpeak.value && idleSecondsLeft.value > 0
)

const idleTimeoutLabel = computed(() => {
  if (IDLE_TIMEOUT_SECONDS % 60 === 0) {
    return `${IDLE_TIMEOUT_SECONDS / 60} 分钟`
  }
  return `${IDLE_TIMEOUT_SECONDS} 秒`
})

const pendingPreview = computed(() => {
  const text = pendingSpeech.value
  return text.length > 46 ? `${text.slice(0, 46)}...` : text
})

const statusText = computed(() => {
  const statusMap = {
    unconfigured: '未配置',
    standby: '等待启动',
    connecting: '连接中',
    'iframe-loaded': '画面加载中',
    connected: '信令已连接',
    ready: '讲解员就绪',
    speaking: '正在播报',
    closed: '连接已释放',
    error: '连接异常'
  }
  return statusMap[connectionStatus.value] || '状态未知'
})

function resetConnectionState() {
  videoReady.value = false
  wsConnected.value = false
  isSpeaking.value = false
  isMuted.value = false
}

async function applyIframeUrl() {
  clearAudioWakeTimer()
  iframeKey.value += 1
  iframeUrl.value = ''
  resetConnectionState()
  connectionStatus.value = 'connecting'

  try {
    const token = await resolveXilingAccessToken(config, activeDialect.value)
    iframeUrl.value = buildXilingRealtimeUrl(config, activeDialect.value, token)
    return true
  } catch (error) {
    connectionStatus.value = 'error'
    errorDetail.value = error?.message || '数字人访问令牌获取失败'
    return false
  }
}

async function startDigitalHuman() {
  if (!config.configured) return
  clearIdleTimer()
  isIdleDisconnected.value = false
  hasUserGesture.value = true
  errorDetail.value = ''
  await applyIframeUrl()
}

async function reconnect() {
  if (!config.configured) return
  hasUserGesture.value = false
  iframeUrl.value = ''
  await startDigitalHuman()
}

async function switchDialect(dialectKey) {
  if (dialectKey === activeDialectKey.value) return
  activeDialectKey.value = dialectKey
  errorDetail.value = ''

  if (hasUserGesture.value && !isIdleDisconnected.value) {
    clearIdleTimer()
    await applyIframeUrl()
  }
}

function getIframeOrigin() {
  try {
    return new URL(iframeUrl.value).origin
  } catch {
    return ''
  }
}

function postToIframe(type, content) {
  const target = iframeRef.value?.contentWindow
  const targetOrigin = getIframeOrigin()
  if (!target || !targetOrigin || !isTrustedXilingOrigin(targetOrigin)) return false
  target.postMessage({ type, content }, targetOrigin)
  return true
}

function createRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

function sendSpeech(text) {
  const sent = postToIframe('message', {
    action: 'TEXT_RENDER',
    body: text,
    requestId: createRequestId()
  })

  if (!sent) {
    pendingSpeech.value = text
    return false
  }

  isSpeaking.value = true
  connectionStatus.value = 'speaking'
  resetIdleTimer()
  return true
}

function speak(text) {
  const normalized = String(text || '').trim()
  if (!normalized) return false

  if (!readyToSpeak.value) {
    pendingSpeech.value = normalized
    return false
  }

  pendingSpeech.value = ''
  return sendSpeech(normalized)
}

function flushPendingSpeech() {
  if (!readyToSpeak.value || !pendingSpeech.value) return
  const latestSpeech = pendingSpeech.value
  pendingSpeech.value = ''
  sendSpeech(latestSpeech)
}

function interrupt() {
  pendingSpeech.value = ''
  if (!readyToSpeak.value) {
    isSpeaking.value = false
    return
  }

  postToIframe('message', {
    action: 'TEXT_RENDER',
    body: '<interrupt></interrupt>',
    requestId: createRequestId()
  })
  isSpeaking.value = false
  connectionStatus.value = 'ready'
  resetIdleTimer()
}

function toggleMute() {
  if (!hasUserGesture.value || isIdleDisconnected.value) return
  const nextMuted = !isMuted.value
  if (
    postToIframe('command', {
      subType: 'muteAudio',
      subContent: nextMuted
    })
  ) {
    isMuted.value = nextMuted
    resetIdleTimer()
  }
}

function replayWelcome() {
  speak(WELCOME_TEXT)
}

function askQuickQuestion(question) {
  emit('quick-question', question)
  resetIdleTimer()
}

function clearIdleTimer() {
  if (idleTimer) {
    clearTimeout(idleTimer)
    idleTimer = null
  }
  if (idleCountdownTimer) {
    clearInterval(idleCountdownTimer)
    idleCountdownTimer = null
  }
}

function clearAudioWakeTimer() {
  if (audioWakeTimer) {
    clearTimeout(audioWakeTimer)
    audioWakeTimer = null
  }
}

function startIdleTimer() {
  clearIdleTimer()
  idleSecondsLeft.value = IDLE_TIMEOUT_MS / 1000

  idleCountdownTimer = setInterval(() => {
    idleSecondsLeft.value = Math.max(0, idleSecondsLeft.value - 1)
  }, 1000)

  idleTimer = setTimeout(disconnectForIdle, IDLE_TIMEOUT_MS)
}

function resetIdleTimer() {
  if (!readyToSpeak.value) return
  startIdleTimer()
}

function disconnectForIdle() {
  clearIdleTimer()
  clearAudioWakeTimer()
  hasUserGesture.value = false
  iframeUrl.value = ''
  isIdleDisconnected.value = true
  resetConnectionState()
  connectionStatus.value = 'closed'
}

function wakeMobileAudio() {
  clearAudioWakeTimer()
  postToIframe('command', {
    subType: 'muteAudio',
    subContent: true
  })

  audioWakeTimer = setTimeout(() => {
    postToIframe('command', {
      subType: 'muteAudio',
      subContent: false
    })
    audioWakeTimer = null
  }, 260)
}

function updateReadyState() {
  if (!(videoReady.value && wsConnected.value)) return
  connectionStatus.value = isSpeaking.value ? 'speaking' : 'ready'
  errorDetail.value = ''
  startIdleTimer()
  flushPendingSpeech()
}

function onIframeLoad() {
  if (connectionStatus.value === 'connecting') {
    connectionStatus.value = 'iframe-loaded'
  }
}

function onMessage(event) {
  if (!isTrustedXilingOrigin(event.origin)) return
  if (event.source !== iframeRef.value?.contentWindow) return
  if (isIdleDisconnected.value) return

  const { type, content } = event.data || {}

  if (type === 'rtcState') {
    if (
      content?.action === 'remoteVideoConnected' ||
      content?.action === 'remotevideoon'
    ) {
      const wasReady = videoReady.value
      videoReady.value = true
      if (!wasReady) wakeMobileAudio()
      updateReadyState()
      return
    }

    if (content?.action === 'error') {
      connectionStatus.value = 'error'
      errorDetail.value =
        content?.errorMsg || content?.message || '数字人视频连接异常'
      clearIdleTimer()
      clearAudioWakeTimer()
    }
    return
  }

  if (type === 'wsState') {
    if (content?.readyState === 1) {
      wsConnected.value = true
      connectionStatus.value = videoReady.value ? 'ready' : 'connected'
      updateReadyState()
      return
    }

    if (content?.readyState === 2 || content?.readyState === 3) {
      wsConnected.value = false
      connectionStatus.value = 'closed'
      errorDetail.value =
        content?.reason || content?.message || '数字人语音连接已断开'
      clearIdleTimer()
      clearAudioWakeTimer()
    }
    return
  }

  if (type !== 'msg') return

  if (content?.action === 'RENDER_START') {
    isSpeaking.value = true
    connectionStatus.value = 'speaking'
    resetIdleTimer()
    return
  }

  if (
    content?.action === 'FINISHED' ||
    content?.action === 'RENDER_INTERRUPTED' ||
    content?.action === 'RENDER_ERROR'
  ) {
    isSpeaking.value = false
    connectionStatus.value = readyToSpeak.value ? 'ready' : 'connected'
    if (content?.action === 'RENDER_ERROR') {
      errorDetail.value = content?.message || '数字人播报失败'
    }
    resetIdleTimer()
  }
}

onMounted(() => {
  window.addEventListener('message', onMessage)
  if (config.configured) {
    void startDigitalHuman()
  }
})

onUnmounted(() => {
  window.removeEventListener('message', onMessage)
  clearIdleTimer()
  clearAudioWakeTimer()
  iframeUrl.value = ''
})

defineExpose({
  speak,
  interrupt,
  reconnect,
  replayWelcome
})
</script>

<style scoped>
.xiling-panel {
  --xr-ink: var(--ym-text, #201915);
  --xr-ink-soft: var(--ym-text-secondary, #3a2f28);
  --xr-muted: var(--ym-text-muted, #6c5a4d);
  --xr-paper: var(--ym-bg, #f5efe6);
  --xr-paper-light: var(--ym-surface, #fff9f0);
  --xr-accent: var(--ym-accent, #a14b34);
  --xr-accent-rgb: var(--ym-accent-rgb, 161, 75, 52);
  --xr-gold: var(--ym-gold, #b08a49);
  --xr-gold-rgb: var(--ym-gold-rgb, 176, 138, 73);
  --xr-support: var(--ym-support, #6b7f6a);
  --xr-border: var(--ym-border, rgba(58, 47, 40, 0.18));

  position: relative;
  display: grid;
  grid-template-rows: auto auto auto auto;
  width: 100%;
  max-width: 420px;
  height: auto;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--xr-border);
  border-radius: 26px;
  color: var(--xr-ink);
  background:
    radial-gradient(circle at 88% 8%, rgba(var(--xr-gold-rgb), 0.14), transparent 24%),
    radial-gradient(circle at 8% 92%, rgba(var(--xr-accent-rgb), 0.1), transparent 26%),
    linear-gradient(145deg, rgba(255, 249, 240, 0.98), rgba(239, 230, 216, 0.94));
  box-shadow: 0 22px 54px rgba(58, 47, 40, 0.13);
  font-family: var(--ym-font-sans, 'Noto Serif SC', serif);
}

.xiling-panel::before {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  content: '';
  opacity: 0.32;
  background-image:
    linear-gradient(rgba(58, 47, 40, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(58, 47, 40, 0.025) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(to bottom, black, transparent 68%);
}

.xiling-header,
.xiling-dialect-bar,
.xiling-viewport,
.xiling-console {
  position: relative;
  z-index: 1;
}

.xiling-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 17px 18px 15px;
  border-bottom: 1px solid var(--xr-border);
}

.xiling-heading {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 11px;
}

.xiling-brand-logo {
  display: block;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.xiling-eyebrow {
  margin: 0 0 2px;
  color: var(--xr-gold);
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.19em;
}

.xiling-heading h2 {
  overflow: hidden;
  margin: 0;
  color: var(--xr-ink);
  font-family: var(--ym-font-display, serif);
  font-size: clamp(1.05rem, 2vw, 1.26rem);
  font-weight: 600;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xiling-status {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  padding: 6px 10px;
  border: 1px solid var(--xr-border);
  border-radius: 999px;
  color: var(--xr-muted);
  background: rgba(255, 249, 240, 0.66);
  font-size: 0.72rem;
  white-space: nowrap;
}

.xiling-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 4px rgba(108, 90, 77, 0.08);
}

.xiling-status.is-ready {
  color: var(--xr-support);
  border-color: rgba(107, 127, 106, 0.3);
  background: rgba(107, 127, 106, 0.1);
}

.xiling-status.is-speaking {
  color: var(--xr-accent);
  border-color: rgba(var(--xr-accent-rgb), 0.36);
  background: rgba(var(--xr-accent-rgb), 0.1);
}

.xiling-status.is-speaking i {
  animation: xiling-status-pulse 1.2s ease-in-out infinite;
}

.xiling-status.is-error,
.xiling-status.is-unconfigured {
  color: var(--xr-accent);
}

@keyframes xiling-status-pulse {
  50% {
    opacity: 0.36;
    transform: scale(0.72);
  }
}

.xiling-dialect-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--xr-border);
  background: rgba(255, 249, 240, 0.46);
}

.xiling-dialect-title {
  color: var(--xr-muted);
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.xiling-dialect-list {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  justify-content: flex-end;
  min-width: 0;
  gap: 6px;
  overflow: visible;
  scrollbar-width: none;
}

.xiling-dialect-list::-webkit-scrollbar {
  display: none;
}

.xiling-dialect-button,
.xiling-quick-list button,
.xiling-action-button,
.xiling-primary-button {
  font: inherit;
  -webkit-tap-highlight-color: transparent;
}

.xiling-dialect-button {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  color: var(--xr-muted);
  background: rgba(255, 249, 240, 0.62);
  cursor: pointer;
  font-size: 0.72rem;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.xiling-dialect-button span {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 50%;
  color: var(--xr-ink-soft);
  background: rgba(58, 47, 40, 0.08);
  font-size: 0.66rem;
  font-weight: 700;
}

.xiling-dialect-button:hover,
.xiling-dialect-button.active {
  border-color: rgba(var(--xr-accent-rgb), 0.34);
  color: var(--xr-accent);
  background: rgba(var(--xr-accent-rgb), 0.09);
}

.xiling-dialect-button.active span {
  color: var(--xr-paper-light);
  background: var(--xr-accent);
}

.xiling-viewport {
  width: 100%;
  aspect-ratio: 9 / 16;
  max-height: 560px;
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 82%, rgba(var(--xr-accent-rgb), 0.16), transparent 36%),
    linear-gradient(180deg, #ddd3c5 0%, #eee6da 54%, #d8c7b3 100%);
  isolation: isolate;
}

.xiling-viewport::before,
.xiling-viewport::after {
  position: absolute;
  z-index: 0;
  border-radius: 50%;
  pointer-events: none;
  content: '';
  filter: blur(2px);
}

.xiling-viewport::before {
  right: -18%;
  bottom: -19%;
  width: 70%;
  aspect-ratio: 1;
  border: 1px solid rgba(var(--xr-gold-rgb), 0.28);
  box-shadow:
    0 0 0 28px rgba(var(--xr-gold-rgb), 0.06),
    0 0 0 58px rgba(var(--xr-accent-rgb), 0.035);
}

.xiling-viewport::after {
  top: -16%;
  left: -22%;
  width: 60%;
  aspect-ratio: 1;
  background: rgba(255, 249, 240, 0.34);
}

.xiling-iframe {
  position: absolute;
  inset: -6% -8%;
  z-index: 1;
  display: block;
  width: 116%;
  height: 112%;
  border: 0;
  background: transparent;
}

.xiling-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px;
  color: var(--xr-ink-soft);
  background:
    radial-gradient(circle at 50% 42%, rgba(255, 249, 240, 0.38), transparent 30%),
    linear-gradient(160deg, rgba(245, 239, 230, 0.9), rgba(239, 230, 216, 0.96));
  text-align: center;
  backdrop-filter: blur(8px);
}

.xiling-orbit-mark {
  position: relative;
  display: grid;
  width: 74px;
  height: 74px;
  margin-bottom: 19px;
  place-items: center;
  border: 1px solid rgba(var(--xr-accent-rgb), 0.38);
  border-radius: 50%;
  color: var(--xr-accent);
  background: rgba(255, 249, 240, 0.62);
  box-shadow:
    0 0 0 10px rgba(var(--xr-accent-rgb), 0.05),
    0 13px 28px rgba(58, 47, 40, 0.1);
  font-family: var(--ym-font-display, serif);
  font-size: 1.75rem;
}

.xiling-orbit-logo {
  display: block;
  width: 38px;
  height: 38px;
  object-fit: contain;
}

.xiling-orbit-mark::after {
  position: absolute;
  inset: -12px;
  border: 1px dashed rgba(var(--xr-gold-rgb), 0.34);
  border-radius: 50%;
  content: '';
  animation: xiling-orbit 18s linear infinite;
}

@keyframes xiling-orbit {
  to {
    transform: rotate(360deg);
  }
}

.xiling-overlay-kicker {
  margin: 0 0 7px;
  color: var(--xr-gold);
  font-size: 0.64rem;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.xiling-overlay h3 {
  margin: 0;
  color: var(--xr-ink);
  font-family: var(--ym-font-display, serif);
  font-size: clamp(1.25rem, 2.8vw, 1.68rem);
  font-weight: 600;
}

.xiling-overlay > p:last-of-type {
  max-width: 330px;
  margin: 10px 0 0;
  color: var(--xr-muted);
  font-size: 0.84rem;
  line-height: 1.72;
}

.xiling-primary-button {
  margin-top: 20px;
  padding: 11px 24px;
  border: 1px solid rgba(var(--xr-accent-rgb), 0.65);
  border-radius: 999px;
  color: #fffaf3;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.11), transparent),
    var(--xr-accent);
  box-shadow: 0 10px 22px rgba(var(--xr-accent-rgb), 0.22);
  cursor: pointer;
  font-size: 0.86rem;
  font-weight: 700;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.xiling-primary-button:hover {
  box-shadow: 0 13px 28px rgba(var(--xr-accent-rgb), 0.27);
  transform: translateY(-2px);
}

.xiling-loader {
  display: flex;
  align-items: flex-end;
  height: 48px;
  margin-bottom: 17px;
  gap: 7px;
}

.xiling-loader i {
  display: block;
  width: 7px;
  height: 20px;
  border-radius: 999px;
  background: var(--xr-accent);
  animation: xiling-wave 1.05s ease-in-out infinite;
}

.xiling-loader i:nth-child(2) {
  height: 36px;
  animation-delay: 0.15s;
}

.xiling-loader i:nth-child(3) {
  height: 26px;
  animation-delay: 0.3s;
}

@keyframes xiling-wave {
  50% {
    height: 9px;
    opacity: 0.45;
  }
}

.xiling-pending {
  position: absolute;
  right: 13px;
  bottom: 34px;
  left: 13px;
  z-index: 4;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  padding: 9px 11px;
  border: 1px solid rgba(var(--xr-gold-rgb), 0.34);
  border-radius: 13px;
  color: var(--xr-ink-soft);
  background: rgba(255, 249, 240, 0.9);
  box-shadow: 0 8px 24px rgba(58, 47, 40, 0.12);
  backdrop-filter: blur(9px);
}

.xiling-pending span {
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--xr-accent);
  background: rgba(var(--xr-accent-rgb), 0.1);
  font-size: 0.68rem;
  white-space: nowrap;
}

.xiling-pending p {
  overflow: hidden;
  margin: 0;
  font-size: 0.74rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xiling-viewport-caption {
  position: absolute;
  right: 14px;
  bottom: 10px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(58, 47, 40, 0.44);
  font-family: var(--ym-font-display, serif);
  font-size: 0.68rem;
  letter-spacing: 0.08em;
}

.xiling-viewport-caption i {
  width: 15px;
  height: 1px;
  background: currentColor;
}

.xiling-console {
  display: grid;
  gap: 10px;
  padding: 12px 14px 14px;
  border-top: 1px solid var(--xr-border);
  background: rgba(255, 249, 240, 0.72);
}

.xiling-idle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--xr-muted);
  font-size: 0.69rem;
}

.xiling-quick-list,
.xiling-action-list {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 7px;
}

.xiling-quick-list button {
  flex: 1 1 120px;
  min-width: 0;
  padding: 7px 9px;
  overflow: hidden;
  border: 1px solid rgba(var(--xr-gold-rgb), 0.28);
  border-radius: 10px;
  color: var(--xr-ink-soft);
  background: rgba(var(--xr-gold-rgb), 0.07);
  cursor: pointer;
  font-size: 0.72rem;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
}

.xiling-quick-list button:hover {
  border-color: rgba(var(--xr-accent-rgb), 0.38);
  background: rgba(var(--xr-accent-rgb), 0.08);
}

.xiling-action-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.xiling-action-button {
  min-width: 0;
  padding: 8px 7px;
  border: 1px solid var(--xr-border);
  border-radius: 10px;
  color: var(--xr-ink-soft);
  background: rgba(255, 249, 240, 0.78);
  cursor: pointer;
  font-size: 0.73rem;
  transition:
    color 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.xiling-action-button:hover:not(:disabled),
.xiling-action-button.active {
  border-color: rgba(var(--xr-accent-rgb), 0.4);
  color: var(--xr-accent);
  background: rgba(var(--xr-accent-rgb), 0.08);
}

.xiling-action-button:disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.xiling-error {
  margin: 0;
  padding: 8px 10px;
  border: 1px solid rgba(var(--xr-accent-rgb), 0.2);
  border-radius: 10px;
  color: var(--xr-accent);
  background: rgba(var(--xr-accent-rgb), 0.07);
  font-size: 0.72rem;
  line-height: 1.5;
  word-break: break-word;
}

button:focus-visible {
  outline: 2px solid var(--ym-focus, rgba(161, 75, 52, 0.45));
  outline-offset: 2px;
}

@media (max-width: 720px) {
  .xiling-panel {
    height: auto;
    min-height: 0;
    border-radius: 20px;
  }

  .xiling-header {
    padding: 14px;
  }

  .xiling-dialect-bar {
    grid-template-columns: 1fr;
    gap: 7px;
  }

  .xiling-viewport {
    max-height: none;
    min-height: 0;
  }

  .xiling-overlay {
    padding: 22px 18px;
  }

  .xiling-action-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .xiling-idle-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }
}

@media (max-width: 420px) {
  .xiling-heading h2 {
    max-width: 176px;
  }

  .xiling-status {
    padding: 5px 8px;
    font-size: 0.66rem;
  }

  .xiling-viewport {
    min-height: 0;
  }

  .xiling-quick-list {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .xiling-quick-list::-webkit-scrollbar {
    display: none;
  }

  .xiling-quick-list button {
    flex: 0 0 184px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .xiling-status i,
  .xiling-orbit-mark::after,
  .xiling-loader i {
    animation: none;
  }

  .xiling-primary-button {
    transition: none;
  }
}
</style>
