<template>
  <div :class="['intelligence-hub', { 'qa-layout-only': qaOnlyMode }]">
    <article v-if="showAppraisal" class="panel">
      <header class="panel-head">
        <p class="eyebrow">AI APPRAISAL</p>
        <h3>上传鉴赏生成报告</h3>
        <p>上传陶瓷图片，系统将从器型、釉色、纹饰与工艺痕迹等维度生成辅助鉴赏报告。</p>
      </header>

      <div class="endpoint-tip">
        <p><strong>鉴赏说明：</strong>报告用于学习与展示参考，不能替代专业机构的实物鉴定。</p>
        <p>建议上传光线清晰、主体完整的正面照片，以便获得更稳定的识别结果。</p>
      </div>

      <div class="upload-row">
        <input
          type="file"
          accept="image/png,image/jpeg"
          @change="handleImageChange"
        />
        <button
          type="button"
          class="action-btn"
          :disabled="recognitionLoading || !selectedImage"
          @click="runRecognition"
        >
          {{ recognitionLoading ? '鉴赏中...' : '开始鉴赏' }}
        </button>
      </div>

      <p v-if="selectedImageName" class="file-tip">当前文件：{{ selectedImageName }}</p>
      <p v-if="recognitionFeedback" class="info-tip">{{ recognitionFeedback }}</p>

      <div v-if="recognitionReport" class="report-card">
        <div class="report-top">
          <div>
            <p class="report-kicker">AI 鉴赏报告</p>
            <h4>{{ recognitionReport.provider }}</h4>
          </div>
          <span class="confidence-chip">置信度 {{ confidencePercent }}%</span>
        </div>
        <div class="confidence-track">
          <span class="confidence-bar" :style="{ width: `${confidencePercent}%` }"></span>
        </div>
        <p class="report-summary">{{ recognitionReport.summary }}</p>
        <div class="feature-list">
          <article v-for="feature in recognitionReport.features" :key="feature.label" class="feature-item">
            <h4>{{ feature.label }}：{{ feature.value }}</h4>
            <p>{{ feature.evidence }}</p>
          </article>
        </div>
        <div class="report-footer">
          <p class="report-time">生成时间：{{ formatTime(recognitionReport.generatedAt) }}</p>
          <button type="button" class="secondary-btn" disabled>导出报告</button>
        </div>
      </div>
    </article>

    <section v-if="showQa" :class="['qa-page', { 'qa-only': qaOnlyMode }]">
      <aside class="qa-sidebar">
        <div class="consultant-card">
          <div class="consultant-badge">
            <img src="/logo.webp" alt="" class="consultant-logo" />
          </div>
          <h4>柴烧知识顾问</h4>
          <p>24 小时在线问答，聚焦柴烧工艺、审美特征与传承知识。</p>
        </div>

        <div class="sidebar-block">
          <h5>快速咨询</h5>
          <button
            v-for="item in consultationTopics"
            :key="item.title"
            type="button"
            class="topic-item"
            :disabled="isAsking"
            @click="askQuick(item.prompt)"
          >
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </button>
        </div>

        <div class="sidebar-block">
          <h5>咨询信息</h5>
          <label class="field-label" for="qa-track">问题方向</label>
          <select id="qa-track" v-model="selectedTrack" class="field-select" :disabled="isAsking">
            <option v-for="item in questionTracks" :key="item" :value="item">{{ item }}</option>
          </select>

          <label class="field-label" for="qa-depth">答复深度</label>
          <select id="qa-depth" v-model="selectedDepth" class="field-select" :disabled="isAsking">
            <option v-for="item in answerDepthOptions" :key="item" :value="item">{{ item }}</option>
          </select>

          <label class="memory-switch">
            <input v-model="enableMemory" type="checkbox" />
            <span>连续对话记忆</span>
          </label>
        </div>
      </aside>

      <article class="qa-main-panel">
        <header class="qa-main-head">
          <div>
            <p class="eyebrow">KNOWLEDGE QA</p>
            <h3>「有柴烧问题，直接问」</h3>
            <p>问窑火、辨落灰、读器型，也听传承故事。若典藏未有确切记载，我们会坦然说明，不让猜测替代答案。</p>
          </div>
          <div class="head-actions">
            <span :class="['status-pill', connectionStatus]">连接状态：{{ connectionStatusText }}</span>
            <button type="button" class="ghost-btn" :disabled="isAsking" @click="clearMessages">清除记录</button>
          </div>
        </header>

        <div ref="messageBoardRef" class="qa-message-board">
          <article v-for="(message, idx) in messages" :key="message.id || idx" :class="['message', message.role]">
            <span class="message-avatar">
              <img
                v-if="message.role === 'assistant'"
                src="/logo.webp"
                alt="窑创未来"
                class="avatar-logo"
              />
              <template v-else>我</template>
            </span>
            <div class="message-body">
              <p class="message-role">{{ message.role === 'user' ? '我' : '柴智云' }}</p>
              <div class="message-bubble">
                <div
                  v-if="message.role === 'assistant'"
                  :class="['message-content', 'markdown-content', { typing: message.typing }]"
                  v-html="renderAssistantContent(message.content)"
                ></div>
                <p v-else :class="['message-content', { typing: message.typing }]">{{ message.content }}</p>
                <div v-if="message.references?.length" class="ref-row">
                  <span v-for="tag in message.references" :key="tag" class="ref-tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <form class="qa-ask-form" @submit.prevent="submitQuestion">
          <textarea
            v-model="questionInput"
            rows="3"
            :disabled="isAsking"
            placeholder="在此输入想了解的问题，Shift + Enter 换行..."
            @keydown.enter.exact.prevent="submitQuestion"
          ></textarea>
          <button type="submit" class="send-btn" :disabled="isAsking || !questionInput.trim()">
            {{ isAsking ? '生成中...' : '发送' }}
          </button>
        </form>
        <p class="qa-tip">提示：可咨询工艺步骤、窑变机理、器型审美、匠人传承等问题。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { askRagQuestion, identifyCeramicByImage } from '@/utils/intelligenceApi'
import { formatKnowledgeForPrompt, retrieveKnowledge } from '@/utils/localKnowledgeBase'

const props = defineProps({
  viewMode: {
    type: String,
    default: 'all'
  }
})
const emit = defineEmits(['answer-complete'])

const selectedImage = ref(null)
const selectedImageName = ref('')
const recognitionLoading = ref(false)
const recognitionReport = ref(null)
const recognitionFeedback = ref('')

const enableMemory = ref(true)
const isAsking = ref(false)
const questionInput = ref('')
const connectionStatus = ref('idle')
const messageBoardRef = ref(null)

const normalizedMode = computed(() => {
  const validModes = ['all', 'appraisal', 'qa']
  return validModes.includes(props.viewMode) ? props.viewMode : 'all'
})
const showAppraisal = computed(() => ['all', 'appraisal'].includes(normalizedMode.value))
const showQa = computed(() => ['all', 'qa'].includes(normalizedMode.value))
const qaOnlyMode = computed(() => showQa.value && !showAppraisal.value)

const consultationTopics = [
  {
    title: '工艺流程',
    desc: '泥料、装窑、投柴、开窑',
    prompt: '给我梳理柴烧从泥料准备到出窑评估的完整流程。'
  },
  {
    title: '窑变机理',
    desc: '火痕、落灰、温场差异',
    prompt: '窑位和温度为什么会导致不同的窑变效果？'
  },
  {
    title: '审美鉴赏',
    desc: '器型比例、釉色层次',
    prompt: '新手如何判断柴烧作品的火痕与灰釉是否协调？'
  },
  {
    title: '传承故事',
    desc: '匠人、在地文化、非遗脉络',
    prompt: '曲江马坝柴烧的传承特色和在地文化关系是什么？'
  }
]

const questionTracks = ['综合问答', '工艺流程', '窑变机理', '器型审美', '非遗传承']
const answerDepthOptions = ['入门解释', '进阶讲解', '专业分析']
const selectedTrack = ref(questionTracks[0])
const selectedDepth = ref(answerDepthOptions[0])
let messageIdSeed = 0
const nextMessageId = () => {
  messageIdSeed += 1
  return messageIdSeed
}

const buildAssistantWelcome = () => ({
  id: nextMessageId(),
  role: 'assistant',
  typing: false,
  content: [
    '您好！我是您的柴烧知识顾问。',
    '我可以帮助您解答柴烧工艺、窑变机理、器型审美和非遗传承相关问题。',
    '',
    '我可以帮助您：',
    '• 工艺步骤梳理',
    '• 落灰与火痕解读',
    '• 窑位与温场分析',
    '• 匠人传承与文化背景'
  ].join('\n'),
  references: ['系统初始化']
})

const messages = ref([
  buildAssistantWelcome()
])

const typewriterStates = new Map()

const getTypewriterState = (messageId) => {
  if (!typewriterStates.has(messageId)) {
    typewriterStates.set(messageId, {
      target: '',
      running: false,
      timer: null
    })
  }
  return typewriterStates.get(messageId)
}

const stopTypewriter = (messageId) => {
  const state = typewriterStates.get(messageId)
  if (!state) return
  if (state.timer) {
    clearTimeout(state.timer)
    state.timer = null
  }
  state.running = false
}

const clearAllTypewriters = () => {
  for (const [messageId] of typewriterStates) {
    stopTypewriter(messageId)
  }
  typewriterStates.clear()
}

const calcTypeStep = (lag) => {
  if (lag > 180) return 10
  if (lag > 120) return 8
  if (lag > 80) return 6
  if (lag > 40) return 4
  if (lag > 16) return 2
  return 1
}

const typewriterRender = (message, targetText, options = {}) => {
  const target = targetText || ''
  const immediate = options.immediate === true
  const state = getTypewriterState(message.id)
  state.target = target

  if (immediate) {
    stopTypewriter(message.id)
    message.content = target
    return
  }

  if (state.running) return
  state.running = true

  const tick = () => {
    const currentState = getTypewriterState(message.id)
    const currentLen = message.content.length
    const targetLen = currentState.target.length
    if (currentLen < targetLen) {
      const lag = targetLen - currentLen
      const step = calcTypeStep(lag)
      message.content = currentState.target.slice(0, currentLen + step)
      currentState.timer = setTimeout(tick, 16)
      return
    }
    if (currentLen > targetLen) {
      message.content = currentState.target
    }
    currentState.running = false
    currentState.timer = null
    if (message.content.length < currentState.target.length) {
      currentState.running = true
      currentState.timer = setTimeout(tick, 16)
    }
  }

  tick()
}

const waitTypewriterDrain = async (messageId) => {
  while (true) {
    const state = typewriterStates.get(messageId)
    if (!state) return
    if (!state.running && !state.timer) return
    await new Promise((resolve) => setTimeout(resolve, 24))
  }
}

const connectionStatusText = computed(() => {
  if (connectionStatus.value === 'connected') return '已连接'
  if (connectionStatus.value === 'connecting') return '连接中'
  if (connectionStatus.value === 'degraded') return '降级答复'
  return '待连接'
})
const confidencePercent = computed(() => {
  if (!recognitionReport.value?.confidence) return 0
  return Math.round(recognitionReport.value.confidence * 100)
})

const formatTime = (isoText) => {
  if (!isoText) return '-'
  return new Date(isoText).toLocaleString('zh-CN', { hour12: false })
}

const handleImageChange = (event) => {
  const file = event.target.files?.[0]
  selectedImage.value = file || null
  selectedImageName.value = file?.name || ''
}

const runRecognition = async () => {
  recognitionFeedback.value = ''
  recognitionReport.value = null
  if (!selectedImage.value) {
    recognitionFeedback.value = '请先选择图片。'
    return
  }

  recognitionLoading.value = true
  const result = await identifyCeramicByImage(selectedImage.value)
  recognitionLoading.value = false

  recognitionFeedback.value = result.message
  if (result.ok) {
    recognitionReport.value = result.report
  }
}

const buildLocalAnswer = (question, references = []) => {
  const refs = references.length ? references : ['柴烧知识库']
  return `当前已降级到本地答复。你问的是：“${question}”。建议从器型、胎土、火痕、落灰层四个维度观察，并结合窑位与温度曲线判断。参考依据：${refs.join(' / ')}。`
}

const isInsufficientAnswer = (text) => {
  const normalized = (text || '').replace(/\s+/g, '')
  if (!normalized) return true
  if (normalized === '资料不足' || normalized === '资料不足。' || normalized === '资料不足，无法回答。') {
    return true
  }
  return normalized.includes('资料不足') && normalized.length < 32
}

const cleanAnswerOpening = (text = '') => String(text)
  .trim()
  .replace(
    /^(?:(?:根据|基于)(?:以上|上述)?(?:参考文档|参考资料|检索资料|检索到的资料)(?:内容)?[，,:：]?\s*)+/,
    ''
  )
  .trim()

const escapeHtml = (value = '') => String(value)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const renderInlineMarkdown = (line = '') => escapeHtml(line)
  .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  .replace(/`([^`]+?)`/g, '<code>$1</code>')

const renderAssistantContent = (content = '') => {
  const lines = String(content || '').replace(/\r\n/g, '\n').split('\n')
  const html = []
  let listOpen = false
  let paragraphBuffer = []

  const closeList = () => {
    if (listOpen) {
      html.push('</ul>')
      listOpen = false
    }
  }

  const flushParagraph = () => {
    if (!paragraphBuffer.length) return
    closeList()
    html.push(`<p>${paragraphBuffer.map(renderInlineMarkdown).join('<br>')}</p>`)
    paragraphBuffer = []
  }

  lines.forEach((rawLine) => {
    const line = rawLine.trim()
    if (!line) {
      flushParagraph()
      closeList()
      return
    }

    const heading = line.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      flushParagraph()
      closeList()
      const level = Math.min(heading[1].length + 2, 5)
      html.push(`<h${level}>${renderInlineMarkdown(heading[2])}</h${level}>`)
      return
    }

    const bullet = line.match(/^[-*•]\s*(.+)$/)
    if (bullet) {
      flushParagraph()
      if (!listOpen) {
        html.push('<ul>')
        listOpen = true
      }
      html.push(`<li>${renderInlineMarkdown(bullet[1])}</li>`)
      return
    }

    paragraphBuffer.push(line)
  })

  flushParagraph()
  closeList()
  return html.join('')
}

const buildGroundedAnswerFromChunks = (question, chunks = [], references = []) => {
  if (!chunks.length) return buildLocalAnswer(question, references)
  const lines = chunks
    .slice(0, 3)
    .map((chunk, index) => `${index + 1}. ${chunk.content}`)

  return [
    `基于已检索资料，关于“${question}”可先这样理解：`,
    ...lines,
    '如果你希望，我可以继续按“师承脉络、工艺特色、在地文化、传承实践”四个维度展开讲解。'
  ].join('\n')
}

const buildPrompt = (question, chunks) => {
  const memoryContext = enableMemory.value
    ? messages.value
      .slice(-6)
      .map((item) => `${item.role === 'user' ? '用户' : '助手'}：${item.content}`)
      .join('\n')
    : '未启用连续对话记忆。'

  const recognitionContext = recognitionReport.value
    ? `最近鉴赏报告摘要：${recognitionReport.value.summary}`
    : '暂无鉴赏报告。'
  const knowledgeContext = formatKnowledgeForPrompt(chunks)
  const profileContext = `咨询信息：问题方向=${selectedTrack.value}；答复深度=${selectedDepth.value}。`

  return [
    '你是“窑创未来”项目中的柴烧知识问答助手。',
    '请严格优先依据给定知识片段回答，不要编造来源。',
    '回答要求：简洁、可解释。',
    '当检索到知识片段时，必须先基于片段给出要点，不要仅回复“资料不足”。',
    '仅当确实没有任何相关片段时，才可说明“资料不足”。',
    '在结尾用“参考依据：参考1, 参考2”格式标注命中的参考片段编号。',
    recognitionContext,
    profileContext,
    `知识片段：\n${knowledgeContext}`,
    `对话上下文：\n${memoryContext}`,
    `用户问题：${question}`
  ].join('\n\n')
}

const sendQuestion = async (question) => {
  const normalized = question.trim()
  if (!normalized || isAsking.value) return
  const retrievalResult = retrieveKnowledge(normalized, { limit: 4 })
  const referenceTags = retrievalResult.references.length
    ? retrievalResult.references
    : ['柴烧知识库']

  messages.value.push({
    id: nextMessageId(),
    role: 'user',
    typing: false,
    content: normalized,
    references: []
  })

  const assistantMessage = {
    id: nextMessageId(),
    role: 'assistant',
    typing: true,
    content: '',
    references: []
  }
  messages.value.push(assistantMessage)
  isAsking.value = true

  let fullAnswer = ''
  let finalAnswer = ''
  try {
    connectionStatus.value = 'connecting'
    fullAnswer = cleanAnswerOpening(await askRagQuestion(normalized))
    connectionStatus.value = 'connected'
    if (!fullAnswer.trim()) {
      throw new Error('空响应')
    }
    const useLocalFallback = isInsufficientAnswer(fullAnswer)
    finalAnswer = useLocalFallback
      ? buildGroundedAnswerFromChunks(normalized, retrievalResult.chunks, referenceTags)
      : fullAnswer
    typewriterRender(assistantMessage, finalAnswer)
    assistantMessage.references = useLocalFallback ? referenceTags : []
  } catch (error) {
    finalAnswer = buildGroundedAnswerFromChunks(normalized, retrievalResult.chunks, referenceTags)
    typewriterRender(assistantMessage, finalAnswer)
    assistantMessage.references = referenceTags
    connectionStatus.value = 'degraded'
  } finally {
    await waitTypewriterDrain(assistantMessage.id)
    assistantMessage.typing = false
    isAsking.value = false
    emit('answer-complete', finalAnswer)
  }
}

const submitQuestion = async () => {
  const current = questionInput.value
  questionInput.value = ''
  await sendQuestion(current)
}

const askQuick = async (question) => {
  await sendQuestion(question)
}

const askQuestion = async (question) => sendQuestion(String(question || ''))
defineExpose({ askQuestion })

const clearMessages = () => {
  clearAllTypewriters()
  messages.value = [buildAssistantWelcome()]
}

watch(
  () => messages.value.length,
  async () => {
    await nextTick()
    if (messageBoardRef.value) {
      messageBoardRef.value.scrollTop = messageBoardRef.value.scrollHeight
    }
  }
)

onUnmounted(() => {
  clearAllTypewriters()
})
</script>

<style scoped>
.intelligence-hub {
  --panel-bg: rgba(255, 255, 255, 0.94);
  --surface: rgba(255, 255, 255, 0.9);
  --text-primary: #111111;
  --text-secondary: #333333;
  --text-muted: #666666;
  --border: rgba(17, 17, 17, 0.16);
  --border-strong: rgba(17, 17, 17, 0.24);
  --accent: #e10600;
  --accent-rgb: 225, 6, 0;
  --success: #2d7d46;
  --success-bg: rgba(45, 125, 70, 0.12);
  --warning: #9a5f00;
  --warning-bg: rgba(245, 159, 0, 0.2);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 22px;
  background: var(--panel-bg);
  color: var(--text-primary);
}

.panel-head h3 {
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
  margin: 8px 0 8px;
}

.panel-head p {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 0.93rem;
}

.eyebrow {
  font-size: 0.76rem;
  color: var(--text-muted);
  letter-spacing: 0.16em;
}

.endpoint-tip {
  margin-top: 10px;
  border: 1px dashed var(--border-strong);
  border-radius: 10px;
  padding: 8px 10px;
  background:
    linear-gradient(90deg, rgba(217, 217, 217, 0.24) 0%, rgba(255, 255, 255, 0.78) 24%);
}

.endpoint-tip code {
  font-family: Consolas, 'Courier New', monospace;
}

.upload-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.file-tip,
.info-tip {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.report-card {
  margin-top: 14px;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: var(--surface);
}

.report-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.report-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--text-muted);
  text-transform: uppercase;
}

.report-top h4 {
  margin-top: 4px;
  font-family: var(--ym-font-display);
}

.confidence-chip {
  border: 1px solid rgba(var(--accent-rgb), 0.34);
  background: rgba(var(--accent-rgb), 0.1);
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 0.8rem;
  color: var(--accent);
}

.confidence-track {
  margin-top: 10px;
  height: 8px;
  border-radius: 999px;
  background: rgba(17, 17, 17, 0.1);
  overflow: hidden;
}

.confidence-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(var(--accent-rgb), 0.85), rgba(var(--accent-rgb), 0.42));
  transition: width 0.6s ease;
}

.report-summary {
  margin-top: 8px;
  line-height: 1.7;
}

.feature-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.feature-item {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.74);
}

.feature-item h4 {
  font-size: 0.92rem;
}

.feature-item p {
  margin-top: 4px;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 0.88rem;
}

.report-time {
  font-size: 0.82rem;
  color: var(--text-muted);
}

.report-footer {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.intelligence-hub.qa-layout-only {
  display: block;
  height: 100%;
  min-height: 0;
}

.qa-page {
  display: grid;
  grid-template-columns: minmax(240px, 260px) minmax(0, 1fr);
  gap: 18px;
}

.qa-page.qa-only {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.qa-sidebar {
  grid-column: 1;
  grid-row: 1;
  height: 100%;
  border: 1px solid var(--border);
  border-radius: 24px;
  background: #f3f3f3;
  padding: 18px;
  display: grid;
  gap: 14px;
  align-content: start;
  min-height: 0;
  overflow-y: auto;
}

.consultant-card {
  background: #ffffff;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: 16px;
  padding: 14px;
  text-align: center;
}

.consultant-badge {
  width: 60px;
  height: 60px;
  margin: 0 auto 10px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: transparent;
  color: var(--accent);
  font-family: var(--ym-font-display);
  font-size: 1.25rem;
}

.consultant-logo {
  display: block;
  width: 60px;
  height: 60px;
  object-fit: contain;
}

.consultant-card h4 {
  font-family: var(--ym-font-display);
  font-size: 1.18rem;
}

.consultant-card p {
  margin-top: 6px;
  font-size: 0.9rem;
  color: var(--text-secondary);
  line-height: 1.68;
}

.sidebar-block {
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: 14px;
  background: #ffffff;
  padding: 12px;
}

.sidebar-block h5 {
  font-size: 1rem;
  color: #3d352d;
  margin-bottom: 10px;
}

.topic-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(225, 140, 62, 0.28);
  background: #efbd86;
  border-radius: 12px;
  padding: 10px 12px;
  color: #6b3b12;
  display: grid;
  gap: 3px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.topic-item + .topic-item {
  margin-top: 8px;
}

.topic-item strong {
  font-size: 1rem;
}

.topic-item span {
  font-size: 0.82rem;
  color: rgba(107, 59, 18, 0.88);
}

.topic-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(225, 140, 62, 0.18);
}

.field-label {
  display: block;
  margin: 6px 0 6px;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.field-select {
  width: 100%;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #ffffff;
  color: var(--text-primary);
  padding: 0 10px;
  font: inherit;
}

.memory-switch {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.qa-main-panel {
  grid-column: 2;
  grid-row: 1;
  border: 1px solid rgba(58, 47, 40, 0.15);
  border-radius: 24px;
  background:
    radial-gradient(circle at 86% 8%, rgba(var(--gold-rgb), 0.08), transparent 24%),
    rgba(255, 251, 244, 0.9);
  overflow: hidden;
  display: grid;
  grid-template-rows: auto 1fr auto auto;
  height: 100%;
  min-height: 660px;
  min-width: 0;
  box-shadow: 0 18px 42px rgba(58, 47, 40, 0.09);
}

.qa-page.qa-only .qa-main-panel {
  min-height: 0;
}

.qa-main-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid rgba(58, 47, 40, 0.1);
  background: rgba(255, 253, 248, 0.48);
}

.qa-main-head h3 {
  font-family: var(--ym-font-display);
  font-size: clamp(1.55rem, 2.1vw, 1.9rem);
  line-height: 1.2;
  margin: 6px 0;
}

.qa-main-head p {
  max-width: 620px;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.72;
}

.head-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
  flex: 0 0 auto;
}

.status-pill {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.8rem;
  border: 1px solid transparent;
}

.status-pill.idle {
  background: rgba(17, 17, 17, 0.08);
  color: var(--text-secondary);
}

.status-pill.connecting {
  background: rgba(var(--accent-rgb), 0.1);
  border-color: rgba(var(--accent-rgb), 0.2);
  color: var(--accent);
}

.status-pill.connected {
  background: var(--success-bg);
  border-color: rgba(45, 125, 70, 0.2);
  color: var(--success);
}

.status-pill.degraded {
  background: var(--warning-bg);
  border-color: rgba(154, 95, 0, 0.18);
  color: var(--warning);
}

.ghost-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 11px;
  border: 1px solid rgba(58, 47, 40, 0.14);
  background: rgba(255, 253, 248, 0.8);
  color: var(--text-secondary);
  font: inherit;
  cursor: pointer;
}

.qa-message-board {
  padding: 16px 24px 20px;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  gap: 20px;
  align-content: start;
  scrollbar-width: thin;
  scrollbar-color: rgba(var(--accent-rgb), 0.22) transparent;
}

.message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-body {
  display: grid;
  gap: 4px;
  width: min(92%, 720px);
}

.message-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.92rem;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.message.user {
  flex-direction: row-reverse;
}

.message.assistant {
  flex-direction: row;
}

.message.user .message-avatar {
  background: rgba(var(--accent-rgb), 0.15);
  color: var(--accent);
}

.message.assistant .message-avatar {
  background: rgba(45, 125, 70, 0.16);
  color: var(--success);
}

.message-role {
  margin: 2px 2px 0;
  font-size: 0.82rem;
  color: var(--text-muted);
  line-height: 1.2;
}

.message-bubble {
  width: 100%;
  border-radius: 17px;
  padding: 13px 15px;
  line-height: 1.75;
}

.message.user .message-bubble {
  background: #efbd86;
  border: 1px solid rgba(225, 140, 62, 0.45);
  color: #3b2415;
  border-top-right-radius: 6px;
}

.message.assistant .message-bubble {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(58, 47, 40, 0.1);
  border-top-left-radius: 6px;
  box-shadow: 0 7px 20px rgba(58, 47, 40, 0.06);
}

.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-content {
  white-space: normal;
}

.markdown-content :deep(p) {
  margin: 0 0 10px;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5) {
  margin: 14px 0 8px;
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.45;
}

.markdown-content :deep(h3:first-child),
.markdown-content :deep(h4:first-child),
.markdown-content :deep(h5:first-child) {
  margin-top: 0;
}

.markdown-content :deep(ul) {
  margin: 6px 0 12px;
  padding-left: 1.2em;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(strong) {
  font-weight: 700;
  color: var(--accent);
}

.markdown-content :deep(code) {
  padding: 1px 6px;
  border-radius: 6px;
  background: rgba(17, 17, 17, 0.06);
  color: var(--text-primary);
  font-size: 0.92em;
}

.message-content.typing::after {
  content: '▍';
  margin-left: 2px;
  color: rgba(var(--accent-rgb), 0.8);
  animation: typingCursor 0.8s steps(1, end) infinite;
}

@keyframes typingCursor {
  0%, 49% { opacity: 1; }
  50%, 100% { opacity: 0; }
}

.ref-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.ref-tag {
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 0.75rem;
  border: 1px solid var(--border-strong);
  background: rgba(255, 255, 255, 0.72);
}

.qa-ask-form {
  margin: 0 24px;
  padding: 14px 0 8px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: end;
  border-top: 1px solid rgba(58, 47, 40, 0.1);
}

.qa-ask-form textarea {
  min-height: 84px;
  border: 1px solid rgba(58, 47, 40, 0.16);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-primary);
  line-height: 1.65;
  padding: 14px 16px;
  font: inherit;
  resize: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.send-btn {
  height: 56px;
  min-width: 96px;
  border: none;
  border-radius: 15px;
  background: #bfbfbf;
  color: #ffffff;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.send-btn:not(:disabled) {
  background: linear-gradient(135deg, rgba(var(--accent-rgb), 0.84), rgba(176, 65, 24, 0.9));
}

.qa-tip {
  color: var(--text-muted);
  font-size: 0.8rem;
  padding: 0 24px 16px;
  text-align: center;
}

.action-btn,
.secondary-btn {
  border: 1px solid var(--border-strong);
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.action-btn {
  background: rgba(var(--accent-rgb), 0.1);
  border-color: rgba(var(--accent-rgb), 0.38);
  color: var(--text-primary);
}

.secondary-btn {
  background: #ffffff;
  color: var(--text-primary);
}

.action-btn:hover,
.secondary-btn:hover {
  border-color: rgba(var(--accent-rgb), 0.44);
  background: rgba(var(--accent-rgb), 0.08);
}

button:focus-visible,
input:focus-visible,
select:focus-visible,
textarea:focus-visible {
  outline: 2px solid rgba(var(--accent-rgb), 0.58);
  outline-offset: 2px;
}

@media (max-width: 980px) {
  .intelligence-hub,
  .upload-row {
    grid-template-columns: 1fr;
  }

  .qa-page {
    grid-template-columns: minmax(0, 1fr);
  }

  .qa-page.qa-only {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .qa-sidebar {
    grid-column: 1;
    grid-row: 2;
    order: 2;
    max-height: none;
    overflow: visible;
  }

  .qa-main-head {
    flex-direction: column;
  }

  .head-actions {
    width: 100%;
    justify-content: space-between;
  }

  .qa-main-panel {
    grid-column: 1;
    grid-row: 1;
    order: 1;
    height: auto;
    min-height: 620px;
  }

  .qa-page.qa-only .qa-main-panel {
    height: min(720px, calc(100vh - 120px));
    min-height: 560px;
  }

  .qa-ask-form {
    grid-template-columns: 1fr;
  }

  .send-btn {
    width: 100%;
    height: 44px;
  }
}

@media (max-width: 720px) {
  .intelligence-hub,
  .qa-page,
  .qa-main-panel,
  .qa-sidebar {
    min-width: 0;
    max-width: 100%;
  }

  .qa-sidebar {
    padding: 14px;
    border-radius: 20px;
  }

  .qa-main-head {
    padding: 18px 16px 14px;
  }

  .qa-main-head h3 {
    font-size: clamp(1.45rem, 7vw, 1.8rem);
  }

  .head-actions {
    flex-wrap: wrap;
  }

  .qa-message-board {
    padding: 10px 14px 16px;
  }

  .message-body {
    width: calc(100% - 54px);
  }

  .qa-ask-form {
    grid-template-columns: minmax(0, 1fr) auto;
    margin: 0 14px;
    padding-top: 12px;
    align-items: stretch;
  }

  .qa-ask-form textarea {
    min-width: 0;
    padding: 11px 12px;
  }

  .send-btn {
    width: auto;
    min-width: 72px;
    height: auto;
    min-height: 48px;
  }

  .qa-tip {
    padding: 0 14px 14px;
  }
}
</style>
