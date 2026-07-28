<template>
  <article class="kiln-panel">
    <header class="panel-head">
      <p class="eyebrow">板块一 · 柴烧秘境</p>
      <h3>3D 沉浸式窑炉导览 + AI 智能讲解（静音字幕）</h3>
      <p>沿着窑火温度与工序节奏进入柴烧现场，在静音字幕中理解落灰、火痕与釉色的生成逻辑。</p>
    </header>

    <section class="metrics">
      <article class="metric">
        <span>当前步骤</span>
        <strong>{{ currentStep.title }}</strong>
      </article>
      <article class="metric">
        <span>温度区间</span>
        <strong>{{ currentStep.temperature }}</strong>
      </article>
      <article class="metric">
        <span>讲解热点</span>
        <strong>{{ activeHotspot.name }}</strong>
      </article>
    </section>

    <section class="controls">
      <div class="view-tabs">
        <button
          v-for="view in views"
          :key="view.key"
          type="button"
          class="tab-btn"
          :class="{ active: activeView === view.key }"
          @click="switchView(view.key)"
        >
          {{ view.label }}
        </button>
      </div>

      <div class="actions">
        <button type="button" class="btn-primary" @click="toggleAutoPlay">
          {{ isAutoPlaying ? '停止全过程演示' : '一键观看烧制全过程' }}
        </button>
        <button type="button" class="btn-secondary" @click="togglePauseResume" :disabled="!isAutoPlaying">
          {{ isAutoPaused ? '继续播放' : '暂停播放' }}
        </button>
        <button type="button" class="btn-secondary" @click="replayNarration">重播讲解</button>
        <button type="button" class="btn-disabled" disabled>匠人讲解音轨（即将开放）</button>
      </div>

      <div class="speed-controls" aria-label="时间轴播放速度">
        <span>播放速度</span>
        <button
          v-for="speed in speedOptions"
          :key="speed"
          type="button"
          class="speed-btn"
          :class="{ active: playbackSpeed === speed }"
          @click="setPlaybackSpeed(speed)"
        >
          {{ speed }}x
        </button>
      </div>
    </section>

    <section class="content">
      <div class="viewer-card">
        <model-viewer
          v-if="modelViewerReady"
          ref="viewerRef"
          class="viewer"
          :src="modelUrl"
          :camera-orbit="cameraOrbit"
          :camera-target="cameraTarget"
          camera-controls
          shadow-intensity="0.9"
          exposure="0.95"
          environment-image="neutral"
          interaction-prompt="none"
          touch-action="pan-y"
          @load="onModelLoad"
          @error="onModelError"
        >
          <button
            v-for="hotspot in hotspots"
            :key="hotspot.id"
            :slot="`hotspot-${hotspot.id}`"
            class="hotspot"
            :class="{ active: hotspot.id === activeHotspotId }"
            :data-position="hotspot.position"
            :data-normal="hotspot.normal"
            @click.stop="selectHotspot(hotspot.id, true)"
          >
            {{ hotspot.short }}
          </button>
        </model-viewer>
        <div v-else class="viewer-loading">
          <p>{{ modelViewerLoading ? '3D 查看器加载中...' : '3D 查看器暂时不可用' }}</p>
        </div>

        <div class="overlay">
          <span>{{ activeViewLabel }}</span>
          <span>{{ modelError ? '模型异常' : (modelLoaded ? '模型就绪' : '模型加载中') }}</span>
        </div>

        <div class="subtitle">
          <p class="subtitle-title">AI 字幕讲解（静音）</p>
          <p>{{ subtitleText }}</p>
          <p v-if="modelError" class="error">{{ modelError }}</p>
        </div>

        <section class="video-inline">
          <p class="video-inline-title">实景烧窑短片</p>
          <p class="video-inline-desc">用于对照 3D 导览中的关键窑炉动作，建议在切换到对应时间轴步骤时播放观看。</p>
          <div class="kiln-video-wrap">
            <video class="kiln-video" controls preload="metadata" playsinline :src="kilnVideoUrl">
              您的浏览器暂不支持视频播放，请更换浏览器后重试。
            </video>
          </div>
          <a class="video-link" :href="kilnVideoUrl" target="_blank" rel="noopener noreferrer">全屏观看</a>
        </section>
      </div>

      <aside class="side">
        <section class="card">
          <p class="kicker">窑炉结构智能解析</p>
          <h4>{{ activeHotspot.name }}</h4>
          <p>{{ activeHotspot.summary }}</p>
          <ul>
            <li v-for="point in activeHotspot.points" :key="point">{{ point }}</li>
          </ul>

          <div class="curve-card">
            <div class="curve-head">
              <span>温度曲线（热点）</span>
              <small>{{ curveMin }}℃ ~ {{ curveMax }}℃</small>
            </div>
            <svg viewBox="0 0 220 64" class="curve-svg" aria-label="热点温度曲线图">
              <polyline :points="curvePoints" fill="none" stroke="#b5442e" stroke-width="2.4" stroke-linecap="round" />
            </svg>
            <div class="curve-axis">
              <span>点火</span>
              <span>高温段</span>
              <span>出窑</span>
            </div>
          </div>
        </section>

        <section class="card">
          <header class="timeline-head">
            <div>
              <p class="kicker">时间轴视角</p>
              <h4>柴烧工艺全程还原</h4>
            </div>
            <span>{{ currentStepIndex + 1 }}/{{ steps.length }}</span>
          </header>
          <ol class="timeline">
            <li
              v-for="(step, index) in steps"
              :key="step.id"
              class="timeline-item"
              :class="{ active: index === currentStepIndex, done: index < currentStepIndex }"
              @click="jumpToStep(index, true)"
            >
              <span class="index">{{ index + 1 }}</span>
              <div>
                <h5>{{ step.title }}</h5>
                <p>{{ step.summary }}</p>
                <small>{{ step.temperature }}</small>
              </div>
            </li>
          </ol>
        </section>

      </aside>
    </section>

    <section class="status-bar">
      <div class="status-head">
        <strong>任务状态条</strong>
        <span>{{ Math.round(progressPercent) }}%</span>
      </div>
      <div class="track"><span class="progress" :style="{ width: `${progressPercent}%` }"></span></div>
      <p>当前任务：{{ currentStep.title }} · {{ activeHotspot.name }}</p>
      <p>下一任务：{{ nextStepTitle }}</p>
    </section>
  </article>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { collectiblesAPI } from '@/utils/collectiblesApi'
import { ensureModelViewer, hasModelViewer } from '@/utils/modelViewerLoader'

const modelUrl = 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/models/four_dragon_neuf.glb'
const kilnVideoUrl = '/2bc8bba6572a5ad71a07d94c6d477e86.mp4'
const speedOptions = [1, 1.5, 2]
const views = [
  { key: 'outside', label: '窑外全景' },
  { key: 'inside', label: '窑内第一视角' },
  { key: 'overhead', label: '俯瞰视角' },
  { key: 'timeline', label: '时间轴视角' }
]
const allowedViewKeys = ['outside', 'inside', 'overhead', 'timeline']

const viewPreset = {
  outside: { label: '窑外全景', orbit: '34deg 74deg 8m', target: '0m 1.5m 0m' },
  inside: { label: '窑内第一视角', orbit: '182deg 88deg 1.2m', target: '0m 1.35m -0.3m' },
  overhead: { label: '俯瞰视角', orbit: '5deg 28deg 9m', target: '0m 1.4m 0m' },
  timeline: { label: '时间轴视角', orbit: '20deg 70deg 7.5m', target: '0m 1.45m 0m' }
}

const defaultHotspots = [
  {
    id: 'feed',
    short: '投',
    name: '投柴孔',
    position: '-0.65m 2.25m 0.95m',
    normal: '0m 1m 0m',
    focusOrbit: '150deg 72deg 3.6m',
    focusTarget: '-0.55m 1.95m 0.72m',
    summary: '投柴节律决定温升与还原气氛稳定性，是烧制中最关键的控制点之一。',
    points: ['投柴间隔不稳会导致温场波动。', '松木投柴油脂更高，利于形成金斑釉面。'],
    temperatureCurve: [120, 380, 620, 860, 1040, 1180, 1260]
  },
  {
    id: 'flue',
    short: '火',
    name: '火道',
    position: '0.15m 1.15m -0.65m',
    normal: '0m 1m 0m',
    focusOrbit: '96deg 82deg 3.2m',
    focusTarget: '0m 1.15m -0.45m',
    summary: '火道控制热量传播路径，不同窑段温差会直接影响落灰与窑变层次。',
    points: ['前段升温快，中段稳定，尾段更细腻。', '火道通畅性影响整体烧成效率。'],
    temperatureCurve: [100, 260, 540, 820, 1010, 1160, 1280]
  },
  {
    id: 'door',
    short: '门',
    name: '窑门',
    position: '0.25m 1.1m 1.45m',
    normal: '0m 0m 1m',
    focusOrbit: '0deg 80deg 3.8m',
    focusTarget: '0.2m 1.1m 1.15m',
    summary: '窑门承担装窑与出窑任务，封窑质量直接决定保温表现与气氛稳定。',
    points: ['封窑材料需分层压实。', '开窑前需确认降温充分，避免开裂。'],
    temperatureCurve: [30, 90, 160, 240, 300, 360, 420]
  },
  {
    id: 'position',
    short: '位',
    name: '窑位',
    position: '1.2m 1.35m 0.18m',
    normal: '1m 0m 0m',
    focusOrbit: '300deg 76deg 4.4m',
    focusTarget: '0.8m 1.28m 0.12m',
    summary: '窑位排布决定受火方向与落灰轨迹，是“入窑一色、出窑万彩”的主要来源。',
    points: ['高位火痕更强，侧位易出现色差。', '器物间距不足会产生阴影烧成。'],
    temperatureCurve: [80, 220, 520, 780, 980, 1130, 1240]
  },
  {
    id: 'chimney',
    short: '烟',
    name: '烟囱',
    position: '2.2m 3m -1.1m',
    normal: '0m 1m 0m',
    focusOrbit: '332deg 62deg 5.4m',
    focusTarget: '1.8m 2.2m -0.95m',
    summary: '烟囱控制排烟与抽力，决定窑压平衡和燃烧效率。',
    points: ['抽力过强会带走热量。', '抽力不足会出现反烟与燃烧不充分。'],
    temperatureCurve: [90, 180, 320, 560, 760, 980, 1150]
  }
]

const defaultSteps = [
  {
    id: 'prepare',
    title: '泥坯入窑',
    summary: '按器型与受热需求分组入窑。',
    temperature: '室温 ~ 120℃',
    hotspotId: 'door',
    viewKey: 'outside',
    narration: '泥坯入窑阶段要先建立安全间距和受热方向，避免后续升温时应力集中。',
    durationMs: 5200
  },
  {
    id: 'layout',
    title: '分层摆窑',
    summary: '根据窑位温场差异进行分层布局。',
    temperature: '120℃ ~ 450℃',
    hotspotId: 'position',
    viewKey: 'overhead',
    narration: '分层摆窑决定后续落灰方向和窑变层次，是烧成结果分化的基础。',
    durationMs: 5200
  },
  {
    id: 'feed',
    title: '投柴烧制',
    summary: '按节律持续投柴并保持气氛稳定。',
    temperature: '450℃ ~ 980℃',
    hotspotId: 'feed',
    viewKey: 'inside',
    narration: '投柴频率直接影响还原气氛，节奏稳定可以形成连续均匀的釉面过渡。',
    durationMs: 6200
  },
  {
    id: 'control',
    title: '控温烧窑',
    summary: '高温段精细控温，平衡火道与窑压。',
    temperature: '980℃ ~ 1300℃',
    hotspotId: 'flue',
    viewKey: 'timeline',
    narration: '1300℃是落灰成釉关键温段，木灰熔融与胎体结合后会形成自然肌理。',
    durationMs: 6500
  },
  {
    id: 'open',
    title: '开窑出瓷',
    summary: '降温到安全阈值后开窑并复盘。',
    temperature: '1300℃ → 常温返落',
    hotspotId: 'chimney',
    viewKey: 'outside',
    narration: '开窑前先确认窑压正常与应力释放，再按顺序出窑并记录窑位差异。',
    durationMs: 6000
  }
]

const viewerRef = ref(null)
const hotspots = ref(defaultHotspots)
const steps = ref(defaultSteps)
const activeView = ref('outside')
const activeHotspotId = ref(defaultHotspots[0].id)
const currentStepIndex = ref(0)
const subtitleText = ref('')
const modelLoaded = ref(false)
const modelError = ref('')
const modelViewerReady = ref(hasModelViewer())
const modelViewerLoading = ref(false)
const isAutoPlaying = ref(false)
const isAutoPaused = ref(false)
const playbackSpeed = ref(1)

let subtitleTimer = null
let autoTimer = null

const currentStep = computed(() => steps.value[currentStepIndex.value] || steps.value[0] || defaultSteps[0])
const activeHotspot = computed(() => hotspots.value.find(h => h.id === activeHotspotId.value) || hotspots.value[0] || defaultHotspots[0])
const activeViewLabel = computed(() => viewPreset[activeView.value]?.label || viewPreset.outside.label)
const progressPercent = computed(() => ((currentStepIndex.value + 1) / Math.max(steps.value.length, 1)) * 100)
const nextStepTitle = computed(() => {
  if (currentStepIndex.value >= steps.value.length - 1) return '本轮流程完成，可重播或切换热点继续查看。'
  return steps.value[currentStepIndex.value + 1]?.title || '--'
})

const temperatureCurve = computed(() => {
  const curve = Array.isArray(activeHotspot.value?.temperatureCurve) ? activeHotspot.value.temperatureCurve : []
  const numbers = curve.map(value => Number(value)).filter(value => Number.isFinite(value))
  return numbers.length ? numbers : [100, 300, 600, 900, 1200]
})

const curveMin = computed(() => Math.min(...temperatureCurve.value))
const curveMax = computed(() => Math.max(...temperatureCurve.value))
const curvePoints = computed(() => {
  const points = temperatureCurve.value
  const width = 220
  const height = 64
  const padding = 6
  const max = Math.max(...points)
  const min = Math.min(...points)
  const denominator = Math.max(1, max - min)
  return points
    .map((value, index) => {
      const x = padding + (index * (width - padding * 2)) / Math.max(1, points.length - 1)
      const y = height - padding - ((value - min) / denominator) * (height - padding * 2)
      return `${x},${y}`
    })
    .join(' ')
})

const cameraOrbit = computed(() => {
  if (activeView.value === 'timeline') {
    return viewPreset[currentStep.value.viewKey]?.orbit || viewPreset.timeline.orbit
  }
  return viewPreset[activeView.value]?.orbit || viewPreset.outside.orbit
})

const cameraTarget = computed(() => {
  if (activeView.value === 'timeline') {
    return viewPreset[currentStep.value.viewKey]?.target || viewPreset.timeline.target
  }
  return viewPreset[activeView.value]?.target || viewPreset.outside.target
})

const clearSubtitleTimer = () => {
  if (!subtitleTimer) return
  window.clearInterval(subtitleTimer)
  subtitleTimer = null
}

const clearAutoTimer = () => {
  if (!autoTimer) return
  window.clearTimeout(autoTimer)
  autoTimer = null
}

const typeNarration = (text) => {
  clearSubtitleTimer()
  const full = String(text || '').trim()
  subtitleText.value = ''
  if (!full) return
  let i = 0
  subtitleTimer = window.setInterval(() => {
    i += 1
    subtitleText.value = full.slice(0, i)
    if (i >= full.length) clearSubtitleTimer()
  }, 28)
}

const focusCamera = (orbit, target) => {
  const viewer = viewerRef.value
  if (!viewer) return
  viewer.cameraOrbit = orbit
  viewer.cameraTarget = target
}

const normalizeHotspot = (item, index) => {
  const id = String(item?.hotspotCode || item?.id || `hotspot-${index + 1}`).trim()
  return {
    id,
    short: String(item?.shortLabel || item?.short || id.slice(0, 1)).trim().slice(0, 2),
    name: String(item?.name || `热点${index + 1}`).trim(),
    position: String(item?.position || '0m 1.5m 0m').trim(),
    normal: String(item?.normal || '0m 1m 0m').trim(),
    focusOrbit: String(item?.focusOrbit || '34deg 74deg 8m').trim(),
    focusTarget: String(item?.focusTarget || '0m 1.5m 0m').trim(),
    summary: String(item?.summary || '暂无讲解说明').trim(),
    points: Array.isArray(item?.points) ? item.points.map(v => String(v || '').trim()).filter(Boolean) : [],
    temperatureCurve: Array.isArray(item?.temperatureCurve)
      ? item.temperatureCurve.map(v => Number(v)).filter(v => Number.isFinite(v))
      : []
  }
}

const normalizeStep = (item, index) => {
  const viewKey = String(item?.viewKey || 'timeline').trim()
  return {
    id: String(item?.stepCode || item?.id || `step-${index + 1}`).trim(),
    title: String(item?.title || `步骤${index + 1}`).trim(),
    summary: String(item?.summary || '暂无步骤说明').trim(),
    temperature: String(item?.temperature || '--').trim(),
    hotspotId: String(item?.hotspotCode || item?.hotspotId || '').trim(),
    viewKey: allowedViewKeys.includes(viewKey) ? viewKey : 'timeline',
    narration: String(item?.narration || item?.summary || '').trim(),
    durationMs: Math.max(800, Number(item?.durationMs) || 5000)
  }
}

const loadGuideConfig = async () => {
  try {
    const data = await collectiblesAPI.getGuideConfig()
    const remoteHotspotsRaw = Array.isArray(data?.hotspots) ? data.hotspots : []
    const remoteStepsRaw = Array.isArray(data?.steps) ? data.steps : []
    const remoteHotspots = remoteHotspotsRaw.map(normalizeHotspot).filter(item => item.id)
    const remoteSteps = remoteStepsRaw.map(normalizeStep).filter(item => item.id)

    if (remoteHotspots.length) {
      hotspots.value = remoteHotspots
    }
    if (remoteSteps.length) {
      steps.value = remoteSteps
    }
  } catch (error) {
    // 使用默认配置回退
  }

  const firstHotspotId = hotspots.value[0]?.id || defaultHotspots[0].id
  activeHotspotId.value = firstHotspotId
  if (currentStepIndex.value >= steps.value.length) {
    currentStepIndex.value = 0
  }
}

const selectHotspot = (id, fromUser = false) => {
  const target = hotspots.value.find(h => h.id === id)
  if (!target) return
  activeHotspotId.value = id
  typeNarration(target.summary)
  if (fromUser) {
    if (isAutoPlaying.value) {
      isAutoPlaying.value = false
      isAutoPaused.value = false
      clearAutoTimer()
    }
    focusCamera(target.focusOrbit, target.focusTarget)
  }
}

const jumpToStep = (index, fromUser = false) => {
  if (index < 0 || index >= steps.value.length) return
  currentStepIndex.value = index
  const step = steps.value[index]
  if (step.hotspotId && hotspots.value.some(item => item.id === step.hotspotId)) {
    activeHotspotId.value = step.hotspotId
  }
  typeNarration(step.narration || step.summary)
  if (fromUser) {
    activeView.value = 'timeline'
    if (isAutoPlaying.value) {
      isAutoPlaying.value = false
      isAutoPaused.value = false
      clearAutoTimer()
    }
  }
}

const scheduleNextStep = () => {
  clearAutoTimer()
  if (!isAutoPlaying.value || isAutoPaused.value) return
  const duration = Math.max(800, Math.round(currentStep.value.durationMs / playbackSpeed.value))
  autoTimer = window.setTimeout(() => {
    if (!isAutoPlaying.value || isAutoPaused.value) return
    if (currentStepIndex.value >= steps.value.length - 1) {
      isAutoPlaying.value = false
      isAutoPaused.value = false
      return
    }
    jumpToStep(currentStepIndex.value + 1, false)
    scheduleNextStep()
  }, duration)
}

const toggleAutoPlay = () => {
  if (isAutoPlaying.value) {
    isAutoPlaying.value = false
    isAutoPaused.value = false
    clearAutoTimer()
    return
  }
  isAutoPlaying.value = true
  isAutoPaused.value = false
  activeView.value = 'timeline'
  jumpToStep(0, false)
  scheduleNextStep()
}

const togglePauseResume = () => {
  if (!isAutoPlaying.value) return
  if (isAutoPaused.value) {
    isAutoPaused.value = false
    scheduleNextStep()
    return
  }
  isAutoPaused.value = true
  clearAutoTimer()
}

const setPlaybackSpeed = (speed) => {
  playbackSpeed.value = speed
  if (isAutoPlaying.value && !isAutoPaused.value) {
    scheduleNextStep()
  }
}

const replayNarration = () => {
  typeNarration(currentStep.value.narration || currentStep.value.summary)
}

const switchView = (key) => {
  activeView.value = key
  if (key !== 'timeline' && isAutoPlaying.value) {
    isAutoPlaying.value = false
    isAutoPaused.value = false
    clearAutoTimer()
  }
}

const onModelLoad = () => {
  modelLoaded.value = true
  modelError.value = ''
}

const onModelError = () => {
  modelLoaded.value = false
  modelError.value = '模型加载失败，请检查 OSS 模型地址或跨域配置。'
}

onMounted(async () => {
  if (!modelViewerReady.value) {
    modelViewerLoading.value = true
    try {
      await ensureModelViewer()
      modelViewerReady.value = true
    } catch (error) {
      modelError.value = '3D 查看器加载失败，请刷新页面后重试。'
    } finally {
      modelViewerLoading.value = false
    }
  }
  await loadGuideConfig()
  jumpToStep(0, false)
})

onBeforeUnmount(() => {
  clearSubtitleTimer()
  clearAutoTimer()
})
</script>

<style scoped>
.kiln-panel {
  border: 1px solid var(--ym-border);
  border-radius: 8px 24px 10px 22px;
  padding: 18px;
  background: rgba(255, 250, 240, 0.94);
  display: grid;
  gap: 12px;
}

.eyebrow, .kicker {
  margin: 0;
  font-size: 0.72rem;
  color: var(--ym-text-muted);
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.panel-head h3 {
  margin: 6px 0;
  font-family: var(--ym-font-display);
}

.panel-head p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.72;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.metric {
  border: 1px solid var(--ym-border);
  border-radius: 6px 12px 6px 12px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.8);
  display: grid;
  gap: 2px;
}

.metric span {
  font-size: 0.78rem;
  color: var(--ym-text-muted);
}

.metric strong {
  color: var(--ym-accent);
  font-family: var(--ym-font-calligraphy-ma);
}

.controls {
  display: grid;
  gap: 8px;
}

.view-tabs,
.actions,
.speed-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tab-btn,
.btn-primary,
.btn-secondary,
.btn-disabled,
.speed-btn {
  border: 1px solid var(--ym-border-strong);
  border-radius: 5px 11px 5px 11px;
  padding: 7px 12px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--ym-text);
  cursor: pointer;
}

.tab-btn.active,
.tab-btn:hover,
.speed-btn.active {
  border-color: rgba(var(--ym-accent-rgb), 0.45);
  background: rgba(var(--ym-accent-rgb), 0.1);
  color: var(--ym-accent);
}

.btn-primary {
  border-color: rgba(var(--ym-accent-rgb), 0.5);
  background: rgba(var(--ym-accent-rgb), 0.14);
}

.btn-disabled:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.speed-controls > span {
  font-size: 0.82rem;
  color: var(--ym-text-secondary);
}

.content {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 10px;
}

.viewer-card {
  border: 1px solid var(--ym-border);
  border-radius: 8px 20px 8px 20px;
  overflow: hidden;
  position: relative;
  background: rgba(255, 255, 255, 0.86);
}

.viewer {
  width: 100%;
  height: 520px;
  background: linear-gradient(180deg, #f3ede0, #e8dcc7);
}

.viewer-loading {
  width: 100%;
  height: 520px;
  display: grid;
  place-items: center;
  padding: 24px;
  text-align: center;
  color: var(--ym-text-secondary);
  background: linear-gradient(180deg, #f3ede0, #e8dcc7);
}

.viewer-loading p {
  font-size: 0.92rem;
  letter-spacing: 0.04em;
}

.hotspot {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.6);
  background: rgba(255, 250, 240, 0.95);
  color: var(--ym-accent);
  font-family: var(--ym-font-seal);
  cursor: pointer;
}

.hotspot.active {
  border-color: rgba(var(--ym-support-rgb), 0.7);
  color: var(--ym-support);
  box-shadow: 0 0 0 3px rgba(var(--ym-support-rgb), 0.2);
}

.overlay {
  position: absolute;
  left: 10px;
  top: 10px;
  z-index: 2;
  display: flex;
  gap: 6px;
}

.overlay span {
  font-size: 0.74rem;
  border: 1px solid var(--ym-border);
  border-radius: 4px 8px 4px 8px;
  padding: 3px 8px;
  background: rgba(255, 250, 240, 0.9);
  color: var(--ym-text-secondary);
}

.subtitle {
  border-top: 1px solid var(--ym-border);
  padding: 10px 12px;
  min-height: 86px;
  background: rgba(255, 255, 255, 0.92);
}

.subtitle-title {
  margin: 0;
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  color: var(--ym-text-muted);
}

.subtitle p {
  margin: 6px 0 0;
  color: var(--ym-text-secondary);
  line-height: 1.72;
  font-size: 0.87rem;
}

.subtitle .error {
  color: var(--ym-danger);
}

.side {
  display: grid;
  gap: 8px;
}

.card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 255, 0.88);
  padding: 11px;
}

.card h4 {
  margin: 6px 0;
  font-family: var(--ym-font-display);
}

.card p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.68;
  font-size: 0.86rem;
}

.card ul {
  margin: 8px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 5px;
}

.card li {
  color: var(--ym-text-secondary);
  font-size: 0.82rem;
  line-height: 1.62;
}

.curve-card {
  margin-top: 10px;
  border: 1px solid var(--ym-border);
  border-radius: 6px 10px 6px 10px;
  background: rgba(255, 255, 255, 0.88);
  padding: 8px;
}

.curve-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.76rem;
  color: var(--ym-text-muted);
}

.curve-svg {
  margin-top: 6px;
  width: 100%;
  height: 64px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(var(--ym-accent-rgb), 0.06));
  border-radius: 6px;
}

.curve-axis {
  margin-top: 4px;
  display: flex;
  justify-content: space-between;
  color: var(--ym-text-muted);
  font-size: 0.72rem;
}

.timeline-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-end;
}

.timeline-head span {
  font-size: 0.8rem;
  color: var(--ym-text-muted);
}

.timeline {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 7px;
}

.timeline-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 10px 4px 10px;
  padding: 8px;
  display: grid;
  grid-template-columns: 26px 1fr;
  gap: 8px;
  cursor: pointer;
}

.timeline-item.active {
  border-color: rgba(var(--ym-accent-rgb), 0.5);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.timeline-item.done {
  border-color: rgba(var(--ym-support-rgb), 0.35);
  background: rgba(var(--ym-support-rgb), 0.08);
}

.index {
  width: 26px;
  height: 26px;
  border: 1px solid var(--ym-border);
  border-radius: 3px 7px 3px 7px;
  display: grid;
  place-items: center;
  font-size: 0.76rem;
  color: var(--ym-text-secondary);
}

.timeline-item h5 {
  margin: 0;
  font-size: 0.88rem;
}

.timeline-item p {
  margin: 3px 0 0;
  font-size: 0.8rem;
}

.timeline-item small {
  display: inline-block;
  margin-top: 4px;
  font-size: 0.72rem;
  color: var(--ym-text-muted);
}

.video-inline {
  border-top: 1px solid var(--ym-border);
  padding: 10px 12px 12px;
  background: rgba(255, 255, 255, 0.92);
  display: grid;
  gap: 8px;
}

.video-inline-title {
  margin: 0;
  font-size: 0.82rem;
  letter-spacing: 0.12em;
  color: var(--ym-text-muted);
  text-transform: uppercase;
}

.video-inline-desc {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.68;
  font-size: 0.84rem;
}

.kiln-video-wrap {
  width: min(520px, 100%);
  aspect-ratio: 4 / 3;
  border: 1px solid var(--ym-border);
  border-radius: 6px 10px 6px 10px;
  background: #000;
  overflow: hidden;
  margin: 0 auto;
}

.kiln-video {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.video-link {
  justify-self: center;
  border: 1px solid var(--ym-border);
  border-radius: 5px 10px 5px 10px;
  padding: 6px 10px;
  font-size: 0.78rem;
  color: var(--ym-text-secondary);
  text-decoration: none;
  background: rgba(255, 255, 255, 0.82);
}

.video-link:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.45);
  color: var(--ym-accent);
}

.status-bar {
  margin-left: auto;
  width: min(460px, 100%);
  border: 1px solid var(--ym-border);
  border-radius: 6px 12px 6px 12px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.88);
}

.status-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.86rem;
}

.track {
  margin-top: 7px;
  height: 7px;
  border-radius: 999px;
  background: rgba(79, 73, 64, 0.16);
  overflow: hidden;
}

.progress {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(var(--ym-accent-rgb), 0.9), rgba(var(--ym-support-rgb), 0.7));
  transition: width 0.35s ease;
}

.status-bar p {
  margin: 7px 0 0;
  font-size: 0.81rem;
  color: var(--ym-text-secondary);
}

button:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

@media (max-width: 1080px) {
  .content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .metrics {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
    align-items: stretch;
  }

  .viewer {
    height: 380px;
  }
}
</style>
