<template>
  <CeramicsPageShell
    kicker="AI CREATION"
    title="AI陶瓷创作与研学中心"
    description="把文字生成 3D 陶瓷、图片辅助创作和分龄研学预约放在同一个体验入口。"
  >
    <div class="ai-study-page">
      <nav class="section-tabs" aria-label="AI陶瓷创作与研学页面导航">
        <a href="#creator">AI 3D创作</a>
        <a href="#examples">示例与作品</a>
        <a href="#study">研学中心</a>
        <a href="#reservation">预约报名</a>
      </nav>

      <section id="creator" class="creator-layout" aria-label="AI 3D 陶瓷创作">
        <article class="creator-panel">
          <p class="panel-kicker">TEXT / IMAGE TO 3D</p>
          <h2>输入想法，生成陶瓷 3D 作品</h2>
          <p class="panel-desc">
            以文字描摹器型气质，也可上传参考图辅助创作，系统将为你的灵感生成可预览、可收藏的陶瓷 3D 作品。
          </p>
          <p class="points-tip">
            本次生成消耗 {{ pointsSummary.ai3dCost || 10 }} 积分；当前积分：{{ pointsDisplay }}。
            <router-link v-if="!pointsSummary.unlimited" to="/ceramics/user-center?tab=points">去充值</router-link>
            <br />
            生成结果可体验约 24 小时；如需长期收藏，可另用 {{ pointsSummary.ai3dPersistCost || 10 }} 积分永久保存。
          </p>

          <label class="field-block">
            <span>创作描述</span>
            <textarea
              v-model="prompt"
              rows="5"
              placeholder="例如：适合送礼的青花梅瓶，瓶身有山水纹样，整体端庄但不厚重"
            ></textarea>
          </label>

          <div class="choice-group" aria-label="风格选择">
            <span class="choice-title">风格快捷选择</span>
            <button
              v-for="item in styleOptions"
              :key="item"
              type="button"
              class="choice-chip"
              :class="{ active: selectedStyle === item }"
              @click="selectedStyle = item"
            >
              {{ item }}
            </button>
          </div>

          <div class="choice-group" aria-label="器型选择">
            <span class="choice-title">器型选择</span>
            <button
              v-for="item in vesselOptions"
              :key="item"
              type="button"
              class="choice-chip"
              :class="{ active: selectedVessel === item }"
              @click="selectedVessel = item"
            >
              {{ item }}
            </button>
          </div>

          <label class="upload-box">
            <input type="file" accept="image/*" @change="handleImageSelect" />
            <span>{{ selectedImageName || '上传参考图片，可用于图片生成 3D' }}</span>
          </label>

          <div class="creator-actions">
            <button type="button" class="primary-btn" :disabled="isGenerating" @click="handleGenerate">
              {{ isGenerating ? '生成中...' : '生成 3D 陶瓷' }}
            </button>
            <button type="button" class="ghost-btn" :disabled="isOptimizing" @click="handleOptimizePrompt">
              {{ isOptimizing ? '优化中...' : 'AI 优化提示词' }}
            </button>
          </div>

          <p v-if="creatorMessage" class="status-line">{{ creatorMessage }}</p>
        </article>

        <aside class="preview-panel" aria-label="生成结果预览">
          <div class="preview-head">
            <p class="panel-kicker">RESULT</p>
            <h2>{{ previewTitle }}</h2>
          </div>

          <div class="model-stage">
            <model-viewer
              v-if="canPreviewModel"
              class="model-viewer"
              :class="{ 'is-hidden': showFrontPreview }"
              :src="generatedResult.modelUrl"
              camera-controls
              auto-rotate
              exposure="1"
              shadow-intensity="0.7"
              @load="handleModelLoad"
              @error="handleModelError"
            ></model-viewer>
            <img
              v-if="showFrontPreview"
              :src="frontPreviewUrl"
              alt="AI陶瓷正视图预览"
              @error="handleFrontPreviewError"
            />
            <div v-if="canPreviewModel && modelLoadState === 'loading'" class="preview-state compact">
              模型加载中...
            </div>
            <div v-if="modelLoadState === 'error'" class="preview-state error">
              模型暂时无法加载，请稍后重试。
            </div>
          </div>

          <div class="result-card">
            <strong>{{ generatedResult?.title || '待生成作品' }}</strong>
            <p>{{ generatedResult?.prompt || normalizedPrompt }}</p>
            <label class="local-model-box">
              <input type="file" accept=".glb,.gltf,model/gltf-binary,model/gltf+json" @change="handleLocalModelSelect" />
              <span>{{ selectedModelName || '选择本地 GLB/GLTF 预览' }}</span>
            </label>
            <div class="result-actions">
              <button type="button" class="ghost-btn" :disabled="!canPersistCurrentWork || isSaving" @click="handleSaveWork">
                {{ saveActionLabel }}
              </button>
              <button type="button" class="ghost-btn" @click="handleGenerate">重新生成</button>
            </div>
          </div>
        </aside>
      </section>

      <section id="examples" class="content-section" aria-label="示例提示词">
        <header class="section-title">
          <p class="panel-kicker">PROMPTS</p>
          <h2>示例提示词</h2>
          <p>从典型器型与工艺风格出发，快速获得一组可继续修改的创作灵感。</p>
        </header>

        <div class="example-grid">
          <article v-for="item in examplePrompts" :key="item.title" class="example-card">
            <img :src="item.image" :alt="item.title" />
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.prompt }}</p>
              <button type="button" class="text-btn" @click="useExample(item)">一键使用</button>
            </div>
          </article>
        </div>
      </section>

      <section id="study" class="content-section" aria-label="研学中心">
        <header class="section-title">
          <p class="panel-kicker">STUDY CENTER</p>
          <h2>智能研学中心</h2>
          <p>待开发</p>
        </header>

      </section>

      <section id="reservation" class="reservation-section" aria-label="研学预约表单">
        <article class="reservation-copy">
          <p class="panel-kicker">RESERVATION</p>
          <h2>预约研学活动</h2>
          <p>
            欢迎学校、机构与亲子家庭预约到访。提交基本信息后，团队将结合人数、年龄段与意向日期，为你匹配合适的研学方案。
          </p>
        </article>

        <form class="reservation-form" @submit.prevent="handleReservationSubmit">
          <label>
            <span>学校/机构</span>
            <input v-model="reservation.school" type="text" required placeholder="例如：曲江实验小学" />
          </label>
          <label>
            <span>联系人</span>
            <input v-model="reservation.contact" type="text" required placeholder="联系人姓名" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="reservation.phone" type="tel" required placeholder="手机号或固定电话" />
          </label>
          <label>
            <span>参与人数</span>
            <input v-model.number="reservation.people" type="number" min="1" required placeholder="例如：30" />
          </label>
          <label>
            <span>年龄段</span>
            <select v-model="reservation.ageGroup">
              <option v-for="item in ageGroups" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>意向日期</span>
            <input v-model="reservation.date" type="date" required />
          </label>
          <label class="full-row">
            <span>备注需求</span>
            <textarea v-model="reservation.note" rows="4" placeholder="课程目标、是否需要亲子课、是否需要 AI 创作环节等"></textarea>
          </label>
          <button type="submit" class="primary-btn" :disabled="isSubmitting">
            {{ isSubmitting ? '提交中...' : '提交预约' }}
          </button>
          <p v-if="reservationMessage" class="status-line">{{ reservationMessage }}</p>
        </form>
      </section>
    </div>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { ensureModelViewer, hasModelViewer } from '@/utils/modelViewerLoader'
import {
  generateCeramicModel,
  getCeramicSession,
  getCeramicWork,
  optimizeCeramicPrompt,
  saveCeramicModel,
  queryCeramicModel,
  submitStudyReservation
} from '@/utils/ceramicCreationApi'
import {
  FOLLOW_UP_INTERVAL_MS,
  MAX_FOLLOW_UP_QUERIES,
  getNextQueryDelayMs,
  resolveGeneratedCover,
  selectGenerationPreview
} from '@/utils/ceramicGenerationSchedule'
import { collectiblesAuthAPI, pointsAPI } from '@/utils/collectiblesApi'

const styleOptions = ['青花瓷', '汝窑', '唐三彩', '现代极简', '柴烧自然落灰']
const vesselOptions = ['花瓶', '茶杯', '茶壶', '碗', '盘', '香炉']
const ageGroups = ['幼儿启蒙', '小学', '初中', '高中', '亲子家庭', '学校团体']

const examplePrompts = [
  {
    title: '青花梅瓶',
    prompt: '青花梅瓶，瓶身有山水纹样，口沿收束，适合作为礼品陈设。',
    style: '青花瓷',
    vessel: '花瓶',
    image: '/青花梅瓶.webp'
  },
  {
    title: '汝窑圆腹瓶',
    prompt: '汝窑圆腹瓶，天青釉色，器型圆润安静，表面有细密开片。',
    style: '汝窑',
    vessel: '花瓶',
    image: '/汝窑圆腹瓶.webp'
  },
  {
    title: '柴烧香炉',
    prompt: '柴烧香炉，三足结构，落灰自然形成灰青与琥珀色过渡。',
    style: '柴烧自然落灰',
    vessel: '香炉',
    image: '/柴烧香炉.webp'
  },
  {
    title: '极简白瓷杯',
    prompt: '现代极简白瓷杯，杯身薄而稳，适合日常茶饮和数字作品展示。',
    style: '现代极简',
    vessel: '茶杯',
    image: '/极简白瓷杯.webp'
  }
]

const prompt = ref('适合送礼的青花梅瓶，瓶身有山水纹样，整体端庄但不厚重')
const selectedStyle = ref('青花瓷')
const selectedVessel = ref('花瓶')
const selectedImage = ref(null)
const selectedImagePreview = ref('')
const selectedModelName = ref('')
const generatedResult = ref(null)
const latestServerWork = ref(null)
const activeTask = ref(null)
const creatorMessage = ref('')
const isSaving = ref(false)
const modelLoadState = ref('idle')
const reservationMessage = ref('')
const isGenerating = ref(false)
const isOptimizing = ref(false)
const isSubmitting = ref(false)
const modelViewerReady = ref(hasModelViewer())
const pointsSummary = ref({ balance: 0, displayBalance: '0', unlimited: false, ai3dCost: 10, ai3dPersistCost: 10 })
const route = useRoute()
const router = useRouter()
let generationTimer = null
let followUpQueries = 0

const reservation = ref({
  school: '',
  contact: '',
  phone: '',
  people: 30,
  ageGroup: '小学',
  date: '',
  note: ''
})

const normalizedPrompt = computed(() => {
  const text = prompt.value.trim()
  return text || '请先输入陶瓷作品描述。'
})

const selectedImageName = computed(() => selectedImage.value?.name || '')
const canPreviewModel = computed(() => {
  const format = String(generatedResult.value?.modelFormat || '').toLowerCase()
  return generatedResult.value?.modelUrl && modelViewerReady.value && ['glb', 'gltf'].includes(format)
})
const showFrontPreview = computed(() => !canPreviewModel.value || modelLoadState.value !== 'ready')
const frontPreviewUrl = computed(() => (
  resolveGeneratedCover(generatedResult.value, selectedImagePreview.value || undefined)
))
const previewTitle = computed(() => modelLoadState.value === 'ready' ? '模型作品' : '正视图预览')
const isLoggedIn = computed(() => Boolean(collectiblesAuthAPI.getToken()))
const canPersistCurrentWork = computed(() => (
  isLoggedIn.value
  && generatedResult.value?.taskId
  && generatedResult.value?.status === 'ready'
  && generatedResult.value?.permanent !== true
  && generatedResult.value?.canPersist === true
))
const saveActionLabel = computed(() => {
  if (isSaving.value) return '保存中...'
  if (generatedResult.value?.permanent) return '已永久保存'
  return `永久保存 · ${generatedResult.value?.persistPointsCost || pointsSummary.value.ai3dPersistCost || 10} 积分`
})
const pointsDisplay = computed(() => {
  if (!isLoggedIn.value) return '未登录'
  if (pointsSummary.value?.unlimited) return '无限'
  return pointsSummary.value?.displayBalance || String(pointsSummary.value?.balance || 0)
})
const canAffordAi3d = computed(() => {
  if (!isLoggedIn.value) return false
  if (pointsSummary.value?.unlimited) return true
  return Number(pointsSummary.value?.balance || 0) >= Number(pointsSummary.value?.ai3dCost || 10)
})

const loadPointsSummary = async () => {
  if (!isLoggedIn.value) return
  try {
    pointsSummary.value = await pointsAPI.getSummary()
  } catch (error) {
    pointsSummary.value = isCurrentStoredAdmin()
      ? { balance: null, displayBalance: '无限', unlimited: true, ai3dCost: 10, ai3dPersistCost: 10 }
      : { balance: 0, displayBalance: '0', unlimited: false, ai3dCost: 10, ai3dPersistCost: 10 }
  }
}

const isCurrentStoredAdmin = () => {
  try {
    const user = JSON.parse(localStorage.getItem('yc_user') || 'null')
    return user?.role === 'admin' || user?.username === 'ycadmin'
  } catch (error) {
    return false
  }
}

const saveCurrentWork = async () => {
  if (!isLoggedIn.value) {
    creatorMessage.value = '请先登录后再永久保存作品'
    router.push({ path: '/ceramics/user-login', query: { redirect: '/ceramics/ai-creation' } })
    return false
  }
  if (!canPersistCurrentWork.value) {
    creatorMessage.value = generatedResult.value?.permanent
      ? '这件作品已经永久保存'
      : '当前作品暂不可永久保存'
    return false
  }
  isSaving.value = true
  try {
    const saved = await saveCeramicModel(generatedResult.value.taskId, {
      title: generatedResult.value.title,
      prompt: generatedResult.value.prompt,
      style: selectedStyle.value,
      vessel: selectedVessel.value
    })
    generatedResult.value = { ...generatedResult.value, ...saved }
    latestServerWork.value = generatedResult.value
    if (saved?.points) pointsSummary.value = saved.points
    creatorMessage.value = '作品已永久保存，可用于文章展示与后续收藏'
    return true
  } catch (error) {
    creatorMessage.value = error.message || '作品永久保存失败，请稍后重试'
    return false
  } finally {
    isSaving.value = false
  }
}

const handleImageSelect = (event) => {
  const file = event.target.files?.[0]
  selectedImage.value = file || null
  selectedImagePreview.value = file ? URL.createObjectURL(file) : ''
}

const handleLocalModelSelect = (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  const url = URL.createObjectURL(file)
  const format = file.name.split('.').pop()?.toLowerCase() || 'glb'
  selectedModelName.value = file.name
  modelLoadState.value = 'loading'
  generatedResult.value = {
    ...(generatedResult.value || {}),
    id: `local-model-${Date.now()}`,
    title: generatedResult.value?.title || '本地 GLB 模型',
    prompt: generatedResult.value?.prompt || normalizedPrompt.value,
    status: 'ready',
    modelUrl: url,
    modelFormat: format,
    coverUrl: ''
  }
  creatorMessage.value = '已载入本地模型，可在右侧旋转预览。'
}

const handleModelLoad = () => {
  modelLoadState.value = 'ready'
}

const handleModelError = () => {
  modelLoadState.value = 'error'
}

const handleFrontPreviewError = (event) => {
  const fallbackUrl = generatedResult.value?.coverUrl || '/青花梅瓶.webp'
  if (event.currentTarget?.src && !event.currentTarget.src.endsWith(fallbackUrl)) {
    event.currentTarget.src = fallbackUrl
  }
}

const handleOptimizePrompt = async () => {
  if (!normalizedPrompt.value) return
  isOptimizing.value = true
  creatorMessage.value = ''
  try {
    prompt.value = await optimizeCeramicPrompt({
      message: normalizedPrompt.value,
      style: selectedStyle.value,
      vessel: selectedVessel.value
    })
    creatorMessage.value = '提示词已优化，可继续生成 3D 模型。'
  } catch (error) {
    creatorMessage.value = error.message || '提示词优化失败。'
  } finally {
    isOptimizing.value = false
  }
}

const clearGenerationTimer = () => {
  if (generationTimer) {
    window.clearTimeout(generationTimer)
    generationTimer = null
  }
}

const applyWaitingPreview = () => {
  const selectedPreview = selectedImagePreview.value
    || (!generatedResult.value?.modelUrl
      ? (generatedResult.value?.coverWebpUrl || generatedResult.value?.coverUrl)
      : '')
  const preview = selectGenerationPreview({
    previousWork: latestServerWork.value,
    selectedImagePreview: selectedPreview,
    fallbackUrl: '/青花梅瓶.webp'
  })
  if (preview.type === 'model') {
    generatedResult.value = latestServerWork.value
    return
  }
  generatedResult.value = {
    title: '创作参考图',
    prompt: normalizedPrompt.value,
    coverUrl: preview.url,
    modelUrl: '',
    modelFormat: ''
  }
  modelLoadState.value = 'idle'
}

const waitingMessage = () => (
  generatedResult.value?.modelUrl
    ? '正在生成新的作品，当前展示上一次作品。预计 5 分钟后查看结果。'
    : '作品正在生成，当前展示创作参考图。预计 5 分钟后查看结果。'
)

const scheduleActiveTaskQuery = (delayMs) => {
  clearGenerationTimer()
  generationTimer = window.setTimeout(() => {
    queryActiveTask()
  }, Math.max(0, delayMs))
}

const queryActiveTask = async () => {
  const taskId = activeTask.value?.taskId
  if (!taskId) return
  try {
    const result = await queryCeramicModel(taskId)
    if (result.status === 'ready') {
      activeTask.value = null
      latestServerWork.value = result
      generatedResult.value = result
      modelLoadState.value = result.modelUrl ? 'loading' : 'idle'
      isGenerating.value = false
      followUpQueries = 0
      creatorMessage.value = result.modelUrl
        ? '作品生成完成，可旋转查看；如需长期收藏，请选择永久保存。'
        : '作品生成完成，预览资源正在准备中。'
      await loadPointsSummary()
      return
    }
    if (result.status === 'failed') {
      activeTask.value = null
      isGenerating.value = false
      followUpQueries = 0
      creatorMessage.value = result.message || '作品生成失败，已退还本次生成积分。'
      await loadPointsSummary()
      return
    }

    activeTask.value = { ...activeTask.value, ...result }
    if (followUpQueries < MAX_FOLLOW_UP_QUERIES) {
      followUpQueries += 1
      creatorMessage.value = `作品仍在生成，将在 1 分钟后再次查看（${followUpQueries}/${MAX_FOLLOW_UP_QUERIES}）。`
      scheduleActiveTaskQuery(FOLLOW_UP_INTERVAL_MS)
      return
    }
    creatorMessage.value = '作品仍在生成，可稍后重新进入本页查看最新进度。'
  } catch (error) {
    if (followUpQueries < MAX_FOLLOW_UP_QUERIES) {
      followUpQueries += 1
      creatorMessage.value = '暂时未能取得最新进度，将在 1 分钟后重试。'
      scheduleActiveTaskQuery(FOLLOW_UP_INTERVAL_MS)
      return
    }
    creatorMessage.value = error.message || '创作进度暂时无法更新，请稍后重新进入本页查看。'
  }
}

const resumeActiveTask = (task) => {
  if (!task?.taskId) return
  activeTask.value = task
  isGenerating.value = true
  followUpQueries = 0
  applyWaitingPreview()
  const delayMs = getNextQueryDelayMs(task.createdAt || new Date().toISOString())
  creatorMessage.value = waitingMessage()
  scheduleActiveTaskQuery(delayMs)
}

const handleGenerate = async () => {
  if (!prompt.value.trim()) {
    creatorMessage.value = '请先填写创作描述。'
    return
  }
  if (!isLoggedIn.value) {
    creatorMessage.value = '请先登录后再生成 AI 3D 模型。'
    router.push({ path: '/ceramics/user-login', query: { redirect: '/ceramics/ai-creation' } })
    return
  }
  if (activeTask.value?.taskId) {
    creatorMessage.value = '已有作品正在生成，请等待当前任务完成。'
    return
  }
  await loadPointsSummary()
  if (!canAffordAi3d.value) {
    creatorMessage.value = `积分不足，生成一次需要 ${pointsSummary.value.ai3dCost || 10} 积分。`
    router.push({ path: '/ceramics/user-center', query: { tab: 'points' } })
    return
  }
  isGenerating.value = true
  creatorMessage.value = ''
  try {
    const task = await generateCeramicModel({
      prompt: normalizedPrompt.value,
      style: selectedStyle.value,
      vessel: selectedVessel.value,
      imageFile: selectedImage.value
    })
    if (task?.points) {
      pointsSummary.value = task.points
    } else {
      await loadPointsSummary()
    }
    resumeActiveTask(task)
  } catch (error) {
    isGenerating.value = false
    creatorMessage.value = error.message || '作品生成请求提交失败，请稍后重试。'
  }
}

const handleSaveWork = async () => {
  await saveCurrentWork()
}

const loadCreationSession = async () => {
  if (!isLoggedIn.value) return
  try {
    const session = await getCeramicSession()
    latestServerWork.value = session?.latestPreview || null
    generatedResult.value = latestServerWork.value

    const workId = Number(route.query.workId || 0)
    if (workId > 0) {
      const selectedWork = await getCeramicWork(workId)
      generatedResult.value = selectedWork || generatedResult.value
    }

    if (session?.activeTask) {
      prompt.value = session.activeTask.prompt || prompt.value
      selectedStyle.value = session.activeTask.style || selectedStyle.value
      selectedVessel.value = session.activeTask.vessel || selectedVessel.value
      resumeActiveTask(session.activeTask)
    } else if (generatedResult.value?.modelUrl) {
      modelLoadState.value = 'loading'
    }
  } catch (error) {
    creatorMessage.value = error.message || '创作记录暂时无法加载。'
  }
}

const useExample = (item) => {
  prompt.value = item.prompt
  selectedStyle.value = item.style
  selectedVessel.value = item.vessel
  generatedResult.value = {
    id: item.title,
    title: item.title,
    prompt: item.prompt,
    coverUrl: item.image,
    modelUrl: ''
  }
  modelLoadState.value = 'idle'
  creatorMessage.value = '示例提示词已填入创作区。'
}

const handleReservationSubmit = async () => {
  isSubmitting.value = true
  reservationMessage.value = ''
  try {
    const result = await submitStudyReservation(reservation.value)
    reservationMessage.value = result?.message || '预约信息已提交。'
  } catch (error) {
    reservationMessage.value = error.message || '预约提交失败。'
  } finally {
    isSubmitting.value = false
  }
}

watch(
  () => generatedResult.value?.modelUrl,
  (url) => {
    if (!url) return
    modelLoadState.value = 'loading'
  }
)

onMounted(async () => {
  if (!modelViewerReady.value) {
    try {
      await ensureModelViewer()
      modelViewerReady.value = true
    } catch (error) {
      modelViewerReady.value = false
    }
  }
  await loadPointsSummary()
  await loadCreationSession()
})

onBeforeUnmount(() => {
  clearGenerationTimer()
})
</script>

<style scoped>
.ai-study-page {
  display: grid;
  gap: 18px;
}

.section-tabs {
  position: sticky;
  top: 96px;
  z-index: 12;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 249, 240, 0.92);
  backdrop-filter: blur(10px);
}

.section-tabs a {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  padding: 7px 14px;
  color: var(--ym-text-secondary);
  text-decoration: none;
  background: rgba(255, 252, 246, 0.84);
}

.section-tabs a:hover {
  border-color: rgba(58, 47, 40, 0.36);
  color: var(--ym-ink-jiao);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.creator-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 16px;
  align-items: stretch;
}

.creator-panel,
.preview-panel,
.content-section,
.reservation-section {
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 24px 8px 24px;
  background: rgba(255, 249, 240, 0.88);
  box-shadow: 0 12px 26px rgba(32, 25, 21, 0.07);
}

.creator-panel,
.preview-panel {
  padding: 18px;
}

.panel-kicker {
  color: var(--ym-text-muted);
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  font-family: var(--ym-font-serif);
}

h2,
h3 {
  color: var(--ym-ink-jiao);
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.04em;
}

.creator-panel h2,
.preview-head h2,
.section-title h2,
.reservation-copy h2 {
  margin-top: 6px;
  font-size: clamp(1.35rem, 2.3vw, 1.8rem);
}

.panel-desc,
.section-title p,
.reservation-copy p,
.result-card p,
.example-card p {
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.points-tip {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.26);
  border-radius: 6px 14px 6px 14px;
  background: rgba(var(--ym-accent-rgb), 0.08);
  padding: 10px 12px;
  color: var(--ym-text-secondary);
  line-height: 1.7;
}

.points-tip a {
  color: var(--ym-accent);
  text-decoration: none;
  border-bottom: 1px solid rgba(var(--ym-accent-rgb), 0.34);
}

.field-block,
.reservation-form label {
  display: grid;
  gap: 7px;
}

.field-block {
  margin-top: 14px;
}

.field-block span,
.reservation-form span,
.choice-title {
  color: var(--ym-ink-nong);
  font-size: 0.9rem;
  font-weight: 600;
}

textarea,
input,
select {
  width: 100%;
}

textarea {
  resize: vertical;
}

.choice-group {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.choice-title {
  width: 100%;
}

.choice-chip,
.ghost-btn,
.primary-btn,
.text-btn {
  cursor: pointer;
  transition: all 0.2s ease;
}

.choice-chip {
  border: 1px solid var(--ym-border);
  border-radius: 999px;
  padding: 7px 12px;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.88);
}

.choice-chip:hover,
.choice-chip.active {
  border-color: rgba(var(--ym-accent-rgb), 0.42);
  background: rgba(var(--ym-accent-rgb), 0.1);
  color: var(--ym-accent);
}

.upload-box {
  margin-top: 12px;
  border: 1px dashed rgba(58, 47, 40, 0.34);
  border-radius: 8px 18px 8px 18px;
  padding: 14px;
  display: block;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.66);
  cursor: pointer;
}

.upload-box input {
  display: none;
}

.creator-actions,
.result-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.primary-btn,
.ghost-btn,
.text-btn {
  border: 1px solid var(--ym-border-strong);
  border-radius: 4px 12px 4px 12px;
  padding: 9px 14px;
}

.primary-btn {
  background: var(--ym-accent);
  border-color: rgba(var(--ym-accent-rgb), 0.72);
  color: #fff9f0;
}

.ghost-btn,
.text-btn,
.download-link {
  background: rgba(255, 252, 246, 0.88);
  color: var(--ym-text-secondary);
}

.primary-btn:disabled,
.ghost-btn:disabled,
.download-link.disabled {
  cursor: not-allowed;
  opacity: 0.58;
  pointer-events: none;
}

.primary-btn:not(:disabled):hover,
.ghost-btn:not(:disabled):hover,
.download-link:not(.disabled):hover,
.text-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(32, 25, 21, 0.08);
}

.download-link {
  display: inline-block;
  text-decoration: none;
}

.status-line {
  margin-top: 10px;
  color: var(--ym-accent);
  line-height: 1.7;
}

.preview-panel {
  display: grid;
  grid-template-rows: auto minmax(320px, 1fr) auto;
  gap: 14px;
}

.model-stage {
  position: relative;
  border: 1px solid var(--ym-border);
  border-radius: 8px 20px 8px 20px;
  min-height: 320px;
  overflow: hidden;
  background: rgba(255, 252, 246, 0.84);
}

.preview-state {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 18px;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.72);
  text-align: center;
}

.preview-state.compact {
  inset: auto 14px 14px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.24);
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(255, 252, 246, 0.88);
  backdrop-filter: blur(8px);
}

.preview-state.error {
  color: var(--ym-accent);
}

.model-stage img,
.model-viewer {
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: block;
  object-fit: cover;
}

.model-viewer.is-hidden {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.result-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 16px 6px 16px;
  padding: 12px;
  background: rgba(255, 252, 246, 0.82);
}

.result-card strong {
  display: block;
  margin-bottom: 4px;
}

.local-model-box {
  margin-top: 10px;
  border: 1px dashed rgba(58, 47, 40, 0.34);
  border-radius: 6px 14px 6px 14px;
  padding: 10px 12px;
  display: block;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.66);
  cursor: pointer;
}

.local-model-box input {
  display: none;
}

.content-section {
  padding: 18px;
}

.section-title {
  margin-bottom: 14px;
}

.example-grid {
  display: grid;
  gap: 10px;
}

.example-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.example-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 16px 6px 16px;
  background: rgba(255, 252, 246, 0.82);
  overflow: hidden;
}

.example-card img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.example-card div {
  padding: 12px;
}

.example-card strong {
  display: block;
  margin-bottom: 5px;
  color: var(--ym-ink-jiao);
}

.text-btn {
  margin-top: 8px;
  padding: 7px 12px;
}

.reservation-section {
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(260px, 0.7fr) minmax(0, 1.3fr);
  gap: 16px;
}

.reservation-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.reservation-form .full-row,
.reservation-form button,
.reservation-form .status-line {
  grid-column: 1 / -1;
}

@media (max-width: 1100px) {
  .creator-layout,
  .reservation-section {
    grid-template-columns: 1fr;
  }

  .example-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .section-tabs {
    position: static;
  }

  .example-grid,
  .reservation-form {
    grid-template-columns: 1fr;
  }
}
</style>
