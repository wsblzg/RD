<template>
  <CeramicsPageShell
    kicker="COLLECTION DETAIL"
    title="数字藏品馆"
    description="支持查看单件藏品的 3D 模型、状态信息与叙事说明。"
    :sub-nav-items="collectionsNavItems"
    :alt="true"
  >
    <section class="detail-panel" :class="rarityClassName">
      <header class="panel-head">
        <button type="button" class="back-btn" @click="goBack">上一页</button>
        <p class="panel-tip">3D 模式支持拖拽旋转与缩放；可切换封面模式比对图模一致性。</p>
      </header>

      <p v-if="loading" class="loading-text">详情加载中...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div v-else-if="detail" class="detail-grid">
        <article class="viewer-card">
          <header class="viewer-head nft-head">
            <div class="title-wrap">
              <div class="nft-line">
                <span class="network-tag">YAOCHAIN · NFT</span>
                <span class="mint-tag">Minted {{ mintedYear }}</span>
              </div>
              <h3>{{ detail.name }}</h3>
              <p class="model-hint">{{ resolvedModelHint }}</p>
            </div>
            <div class="tag-wrap">
              <span class="rarity-tag">{{ detail.rarity }}</span>
              <span class="code-tag">#{{ detail.itemCode || detail.id }}</span>
            </div>
          </header>

          <div class="viewer-switch">
            <button
              type="button"
              class="switch-btn"
              :class="{ active: viewerMode === 'model' }"
              @click="viewerMode = 'model'"
            >
              3D 预览
            </button>
            <button
              type="button"
              class="switch-btn"
              :class="{ active: viewerMode === 'cover' }"
              :disabled="!resolvedCoverUrl"
              @click="viewerMode = 'cover'"
            >
              封面参考
            </button>
          </div>

          <div class="viewer-wrap">
            <model-viewer
              v-if="viewerMode === 'model' && activeModelUrl && modelViewerReady"
              :src="activeModelUrl"
              camera-controls
              auto-rotate
              shadow-intensity="1"
              exposure="0.95"
              environment-image="neutral"
              touch-action="pan-y"
              class="model-viewer"
              @error="onModelError"
            >
            </model-viewer>
            <div v-else-if="viewerMode === 'model' && activeModelUrl && modelViewerLoading" class="viewer-loading">
              3D 查看器加载中...
            </div>
            <div v-else-if="viewerMode === 'cover' && resolvedCoverUrl" class="cover-preview">
              <img :src="resolvedCoverUrl" :alt="`${detail.name} 封面图`" loading="lazy" />
            </div>
            <div v-else class="viewer-empty">该藏品暂未配置可用 3D 模型地址。</div>
          </div>

          <div class="viewer-meta">
            <span>模型状态：{{ activeModelUrl ? '可交互预览' : '未配置' }}</span>
            <span>操作提示：拖拽旋转 / 滚轮缩放</span>
          </div>

          <p v-if="modelErrorMessage" class="error-text">
            {{ modelErrorMessage }}
          </p>
        </article>

        <article class="info-card">
          <header class="info-head">
            <h3>藏品信息架构</h3>
            <span class="certificate-tag">{{ certificateCode }}</span>
          </header>

          <div class="artifact-strip">
            <span>窑型 {{ kilnType }}</span>
            <span>木材 {{ fuelType }}</span>
            <span>格式 {{ (detail.modelFormat || 'glb').toUpperCase() }}</span>
          </div>

          <section class="meta-section">
            <h4>状态看板</h4>
            <div class="state-grid">
              <p><strong>系列</strong><span>{{ detail.series }}</span></p>
              <p><strong>上架状态</strong><span>{{ detail.onShelf ? '上架中' : '已下架' }}</span></p>
              <p><strong>获取状态</strong><span>{{ detail.collected ? '已获取' : '待兑换' }}</span></p>
              <p><strong>创建时间</strong><span>{{ formatTime(detail.createdAt) }}</span></p>
              <p><strong>更新时间</strong><span>{{ formatTime(detail.updatedAt) }}</span></p>
              <p v-if="detail.acquiredAt"><strong>获取时间</strong><span>{{ formatTime(detail.acquiredAt) }}</span></p>
            </div>
          </section>

          <section class="meta-section narrative-section">
            <h4>藏品叙事</h4>
            <p class="description">{{ displayDescription }}</p>
          </section>

          <div class="action-row">
            <div class="redeem-note">{{ detail.collected ? '该藏品已通过兑换码获取。' : '该藏品仅支持通过兑换码获取。' }}</div>
            <button type="button" class="secondary-btn" @click="goCatalog">
              藏品总览
            </button>
          </div>

          <p v-if="actionMessage" :class="['action-tip', actionOk ? 'ok' : 'error']">
            {{ actionMessage }}
          </p>
        </article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { collectiblesAPI } from '@/utils/collectiblesApi'
import { ensureModelViewer, hasModelViewer } from '@/utils/modelViewerLoader'
import { collectionsNavItems } from './navs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref(null)
const errorMessage = ref('')
const modelErrorMessage = ref('')
const activeModelUrl = ref('')
const modelViewerReady = ref(hasModelViewer())
const modelViewerLoading = ref(false)
const viewerMode = ref('model')
const actionMessage = ref('')
const actionOk = ref(true)

const rarityLabelMap = {
  1: '基础款',
  2: '传承款',
  3: '典藏款',
  4: '限藏款',
  5: '臻藏款'
}

const modelFallbackByCode = {
  C1963514: 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/models/9d3545802a8249dc9221b54e5f3c145b.glb',
  CERAMICVASE01: 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/models/55733fb87b7e4099951f69d9daf95d83.glb',
  C1930728: 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/models/1b874dc1832042d88a814692b7da6d8a.glb'
}

const coverFallbackByCode = {
  C1963514: '/vcg-flambe-vase-museum.webp',
  CERAMICVASE01: '/vcg-kiln-vessels-row.webp',
  C1930728: '/vcg-olive-vase-closeup.webp'
}

const modelHintsByCode = {
  C1963514: '馆藏花器器型，强调釉色流淌与火痕层次',
  CERAMICVASE01: '标准器型与窑变肌理，适合工艺教学展示',
  C1930728: '带战士纹饰与立体附饰，强调雕塑性与叙事感'
}

const kilnTypeByCode = {
  C1963514: '龙窑',
  CERAMICVASE01: '阶梯窑',
  C1930728: '龙窑'
}

const fuelTypeByCode = {
  C1963514: '松木',
  CERAMICVASE01: '杂木',
  C1930728: '荔枝木'
}

const fallbackByModelPath = [
  {
    keywords: ['9d3545802a8249dc9221b54e5f3c145b', '1963.514_vase'],
    cover: '/vcg-flambe-vase-museum.webp',
    hint: '馆藏花器器型，强调釉色流淌与火痕层次',
    kilnType: '龙窑',
    fuelType: '松木'
  },
  {
    keywords: ['55733fb87b7e4099951f69d9daf95d83', 'ceramic_vase'],
    cover: '/vcg-kiln-vessels-row.webp',
    hint: '标准器型与窑变肌理，适合工艺教学展示',
    kilnType: '阶梯窑',
    fuelType: '杂木'
  },
  {
    keywords: ['1b874dc1832042d88a814692b7da6d8a', '1930.728_vase_with_trophy-heads_and_warriors'],
    cover: '/vcg-olive-vase-closeup.webp',
    hint: '带战士纹饰与立体附饰，强调雕塑性与叙事感',
    kilnType: '龙窑',
    fuelType: '荔枝木'
  }
]

const resolveRarity = (level) => rarityLabelMap[Number(level || 1)] || `Lv.${level || 1}`

const ossBaseUrl = 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com'
const ossProjectMediaPrefix = `${ossBaseUrl}/project-media/`

const normalizeModelUrl = (rawUrl, itemCode) => {
  const fallback = modelFallbackByCode[itemCode]
  const raw = String(rawUrl || '').trim()

  if (!raw) {
    return fallback || ''
  }
  if (raw.startsWith(ossProjectMediaPrefix)) {
    return raw
  }
  if (raw.startsWith('/project-media/')) {
    return `${ossBaseUrl}${raw}`
  }
  if (raw.startsWith('project-media/')) {
    return `${ossBaseUrl}/${raw}`
  }
  if (/^https?:\/\//i.test(raw)) {
    return raw
  }
  if (raw.startsWith('/')) {
    return raw
  }
  return `/${raw}`
}

const getModelPathNormalized = (item = {}) => {
  return String(item?.modelUrl || '').toLowerCase()
}

const matchByModelPath = (item = {}) => {
  const modelPath = getModelPathNormalized(item)
  if (!modelPath) return null
  return fallbackByModelPath.find((entry) => entry.keywords.some(keyword => modelPath.includes(keyword))) || null
}

const normalizeCoverUrl = (rawUrl, item = {}) => {
  const raw = String(rawUrl || '').trim()
  const matched = matchByModelPath(item)
  if (matched?.cover) return matched.cover

  if (!raw || raw.toLowerCase().endsWith('.glb')) {
    return coverFallbackByCode[item?.itemCode] || ''
  }
  if (/^https?:\/\//i.test(raw) || raw.startsWith('/')) {
    return raw
  }
  return `/${raw.replace(/^\/+/, '')}`
}

const resolvedModelUrl = computed(() => {
  if (!detail.value) return ''
  return normalizeModelUrl(detail.value.modelUrl, detail.value.itemCode)
})

const resolvedCoverUrl = computed(() => {
  if (!detail.value) return ''
  return normalizeCoverUrl(detail.value.coverUrl, detail.value)
})

const mintedYear = computed(() => {
  const raw = detail.value?.createdAt
  const year = Number.parseInt(String(raw || '').slice(0, 4), 10)
  return Number.isFinite(year) ? year : new Date().getFullYear()
})

const certificateCode = computed(() => {
  const raw = String(detail.value?.itemCode || detail.value?.id || '0000').toUpperCase()
  return `YAO-${raw.slice(-6).padStart(6, '0')}`
})

const resolvedModelHint = computed(() => {
  const code = detail.value?.itemCode
  if (code && modelHintsByCode[code]) {
    return modelHintsByCode[code]
  }
  return matchByModelPath(detail.value || {})?.hint || '器型与窑变肌理演示模型'
})

const sanitizeNarrative = (rawText) => {
  const text = String(rawText || '').trim()
  if (!text) return ''
  const cleaned = text.replace(/https?:\/\/\S+/gi, '').replace(/\s{2,}/g, ' ').trim()
  if (!cleaned) return ''
  if (/^wsnlzg\.oss-cn-shenzhen\.aliyuncs\.com\/.+/i.test(cleaned)) return ''
  return cleaned
}

const displayDescription = computed(() => {
  const narrative = sanitizeNarrative(detail.value?.description)
  return narrative || '该藏品聚焦柴烧器型、窑火肌理与非遗语境的综合叙事。'
})

const kilnType = computed(() => {
  const code = detail.value?.itemCode
  if (code && kilnTypeByCode[code]) return kilnTypeByCode[code]
  return matchByModelPath(detail.value || {})?.kilnType || '龙窑'
})

const fuelType = computed(() => {
  const code = detail.value?.itemCode
  if (code && fuelTypeByCode[code]) return fuelTypeByCode[code]
  return matchByModelPath(detail.value || {})?.fuelType || '松木'
})

const rarityClassName = computed(() => {
  const rarity = String(detail.value?.rarity || '')
  if (rarity.includes('限藏')) return 'rarity-legend'
  if (rarity.includes('典藏')) return 'rarity-epic'
  if (rarity.includes('传承')) return 'rarity-rare'
  return 'rarity-common'
})

watch(
  resolvedModelUrl,
  (value) => {
    activeModelUrl.value = value
    modelErrorMessage.value = ''
    if (!value) {
      viewerMode.value = resolvedCoverUrl.value ? 'cover' : 'model'
    }
  },
  { immediate: true }
)

watch(
  resolvedCoverUrl,
  (value) => {
    if (!value && viewerMode.value === 'cover') {
      viewerMode.value = 'model'
    }
  },
  { immediate: true }
)

watch(
  [activeModelUrl, viewerMode],
  async ([url, mode]) => {
    if (!url || mode !== 'model' || modelViewerReady.value) return
    modelViewerLoading.value = true
    try {
      await ensureModelViewer()
      modelViewerReady.value = true
      modelErrorMessage.value = ''
    } catch (error) {
      modelErrorMessage.value = '3D 查看器加载失败，请稍后重试。'
    } finally {
      modelViewerLoading.value = false
    }
  },
  { immediate: true }
)

const normalizeDetail = (item = {}) => ({
  id: item.id,
  itemCode: item.itemCode || '',
  series: item.seriesName || '未分类',
  rarityLevel: Number(item.rarityLevel || 1),
  rarity: resolveRarity(Number(item.rarityLevel || 1)),
  name: item.name || '',
  coverUrl: item.coverUrl || '',
  modelUrl: item.modelUrl || '',
  modelFormat: item.modelFormat || 'glb',
  description: item.description || '',
  onShelf: Number(item.isOnShelf) === 1,
  status: Number(item.status || 1),
  collected: Boolean(item.collected),
  acquiredAt: item.acquiredAt || null,
  createdAt: item.createdAt || null,
  updatedAt: item.updatedAt || null
})

const formatTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

const loadDetail = async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    errorMessage.value = '藏品ID无效。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  actionMessage.value = ''
  modelErrorMessage.value = ''
  try {
    const data = await collectiblesAPI.getItemDetail(id)
    detail.value = normalizeDetail(data || {})
  } catch (error) {
    errorMessage.value = error?.message || '加载藏品详情失败。'
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const goCatalog = () => {
  router.push('/collections/catalog')
}

const onModelError = () => {
  const fallback = modelFallbackByCode[detail.value?.itemCode]
  if (fallback && activeModelUrl.value !== fallback) {
    activeModelUrl.value = fallback
    modelErrorMessage.value = '主模型加载失败，已自动切换到备用模型。'
    return
  }
  modelErrorMessage.value = '3D 模型加载失败。请检查模型地址、CORS 或 OSS 访问权限。'
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-panel {
  border: 1px solid var(--ym-border);
  border-radius: 8px 30px 10px 28px;
  padding: 18px;
  background:
    radial-gradient(circle at 84% 10%, rgba(var(--ym-support-rgb), 0.08), transparent 28%),
    radial-gradient(circle at 16% 92%, rgba(var(--ym-accent-rgb), 0.08), transparent 34%),
    linear-gradient(150deg, rgba(255, 251, 242, 0.95), rgba(248, 237, 220, 0.88));
  box-shadow: 0 14px 30px rgba(51, 44, 35, 0.08);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.back-btn,
.secondary-btn,
.switch-btn,
.model-link {
  border-radius: 4px 10px 4px 10px;
  border: 1px solid var(--ym-border-strong);
  padding: 8px 12px;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.back-btn {
  background: rgba(255, 255, 255, 0.85);
  color: var(--ym-text);
}

.panel-tip {
  font-size: 0.86rem;
  color: var(--ym-text-muted);
}

.loading-text,
.error-text {
  margin-top: 12px;
}

.error-text {
  color: var(--ym-danger);
}

.detail-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 14px;
}

.viewer-card,
.info-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 24px 10px 26px;
  background: rgba(255, 255, 255, 0.86);
  padding: 14px;
  position: relative;
  overflow: hidden;
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}

.viewer-card:hover,
.info-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--ym-accent-rgb), 0.32);
  box-shadow: 0 16px 28px rgba(51, 44, 35, 0.12);
}

.viewer-card::before,
.info-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(140deg, rgba(var(--ym-accent-rgb), 0.12), transparent 26%),
    linear-gradient(320deg, rgba(var(--ym-support-rgb), 0.08), transparent 34%);
  pointer-events: none;
  opacity: 0.55;
}

.viewer-head,
.info-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.viewer-head h3,
.info-head h3 {
  margin: 0;
  font-family: var(--ym-font-display);
  color: var(--ym-text);
}

.nft-head {
  align-items: flex-start;
}

.title-wrap {
  display: grid;
  gap: 6px;
}

.nft-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.network-tag,
.mint-tag {
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border: 1px solid var(--ym-border);
  padding: 2px 8px;
}

.network-tag {
  border-radius: 3px 8px 3px 8px;
  color: var(--ym-text-muted);
  background: rgba(255, 255, 255, 0.7);
}

.mint-tag {
  border-radius: 8px 3px 8px 3px;
  color: var(--ym-support);
  border-color: rgba(var(--ym-support-rgb), 0.38);
  background: rgba(var(--ym-support-rgb), 0.12);
}

.tag-wrap {
  display: grid;
  gap: 8px;
}

.model-hint {
  margin: 0;
  font-size: 0.84rem;
  line-height: 1.72;
  color: #4f3a2d;
  padding: 6px 8px;
  border-left: 2px solid rgba(var(--ym-accent-rgb), 0.45);
  background: rgba(255, 248, 236, 0.84);
}

.code-tag,
.rarity-tag {
  font-size: 0.78rem;
  border-radius: 3px 10px 3px 10px;
  padding: 3px 8px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.4);
  color: var(--ym-accent);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.code-tag {
  background: rgba(43, 43, 43, 0.76);
  color: #fff7ea;
  border-color: transparent;
}

.certificate-tag {
  font-size: 0.74rem;
  border-radius: 4px 10px 4px 10px;
  padding: 3px 8px;
  border: 1px solid rgba(var(--ym-support-rgb), 0.38);
  color: var(--ym-support);
  background: rgba(var(--ym-support-rgb), 0.12);
}

.viewer-switch {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.switch-btn {
  background: rgba(255, 255, 255, 0.9);
  color: var(--ym-text);
}

.switch-btn.active {
  border-color: rgba(var(--ym-accent-rgb), 0.42);
  background: rgba(var(--ym-accent-rgb), 0.1);
}

.switch-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.viewer-wrap {
  margin-top: 10px;
  border: 1px solid rgba(120, 98, 73, 0.25);
  border-radius: 8px 26px 10px 24px;
  min-height: 440px;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 18%, rgba(var(--ym-accent-rgb), 0.15), transparent 42%),
    radial-gradient(circle at 78% 84%, rgba(var(--ym-support-rgb), 0.14), transparent 46%),
    radial-gradient(circle at top, rgba(255, 240, 220, 0.44), rgba(220, 198, 170, 0.2));
}

.model-viewer {
  width: 100%;
  height: 440px;
  background: transparent;
}

.cover-preview {
  width: 100%;
  height: 440px;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at top, rgba(255, 246, 232, 0.66), rgba(232, 210, 184, 0.2));
}

.cover-preview img {
  width: min(92%, 700px);
  height: min(90%, 410px);
  object-fit: cover;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.24);
  border-radius: 6px 18px 6px 18px;
  box-shadow: 0 14px 24px rgba(51, 44, 35, 0.1);
}

.viewer-empty {
  min-height: 440px;
  display: grid;
  place-items: center;
  color: var(--ym-text-muted);
  font-size: 0.92rem;
}

.viewer-loading {
  min-height: 440px;
  display: grid;
  place-items: center;
  color: var(--ym-text-secondary);
  font-size: 0.92rem;
}

.viewer-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.viewer-meta span {
  font-size: 0.78rem;
  color: var(--ym-text-muted);
  border: 1px solid var(--ym-border);
  border-radius: 3px 10px 3px 10px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.84);
}

.artifact-strip {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.artifact-strip span {
  border: 1px solid rgba(var(--ym-support-rgb), 0.24);
  border-radius: 3px 10px 4px 12px;
  padding: 4px 6px;
  font-size: 0.74rem;
  color: var(--ym-text-secondary);
  background: rgba(var(--ym-support-rgb), 0.08);
  text-align: center;
}

.meta-section {
  margin-top: 12px;
}

.meta-section h4 {
  margin: 0;
  font-size: 0.92rem;
  font-family: var(--ym-font-display);
  color: var(--ym-text);
}

.state-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  color: var(--ym-text-secondary);
}

.state-grid p {
  margin: 0;
  line-height: 1.7;
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.82);
}

.state-grid strong {
  display: block;
  font-size: 0.74rem;
  color: var(--ym-text-muted);
  letter-spacing: 0.05em;
}

.state-grid span {
  display: inline-flex;
  margin-top: 4px;
  color: var(--ym-text);
  font-size: 0.86rem;
}

.description {
  margin-top: 8px;
  color: var(--ym-text);
  line-height: 1.75;
  background: rgba(255, 248, 236, 0.86);
  border-left: 2px solid rgba(var(--ym-accent-rgb), 0.48);
  border-radius: 4px 12px 4px 12px;
  padding: 9px 11px;
}

.action-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.redeem-note {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 42px;
  padding: 0 10px;
  border: 1px dashed rgba(var(--ym-support-rgb), 0.35);
  border-radius: 8px 16px 8px 16px;
  color: var(--ym-text-secondary);
  background: rgba(var(--ym-support-rgb), 0.08);
  font-size: 0.84rem;
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.92);
  color: var(--ym-text);
}

.secondary-btn:hover,
.back-btn:hover,
.switch-btn:hover:not(:disabled) {
  border-color: rgba(var(--ym-accent-rgb), 0.46);
  background: rgba(var(--ym-accent-rgb), 0.1);
}

.action-tip {
  margin-top: 10px;
  font-size: 0.9rem;
}

.action-tip.ok {
  color: var(--ym-success);
}

.action-tip.error {
  color: var(--ym-danger);
}

.rarity-legend {
  border-color: rgba(190, 90, 42, 0.45);
}

.rarity-legend .rarity-tag {
  border-color: rgba(190, 90, 42, 0.55);
  color: #b5482e;
  background: rgba(190, 90, 42, 0.12);
}

.rarity-epic {
  border-color: rgba(138, 96, 52, 0.45);
}

.rarity-rare {
  border-color: rgba(98, 121, 104, 0.45);
}

button:focus-visible,
a:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

@media (max-width: 1080px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .action-row,
  .artifact-strip,
  .viewer-switch {
    grid-template-columns: 1fr;
  }

  .viewer-wrap,
  .model-viewer,
  .cover-preview,
  .viewer-empty {
    min-height: 380px;
    height: 380px;
  }
}

@media (max-width: 760px) {
  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .viewer-wrap,
  .model-viewer,
  .cover-preview,
  .viewer-empty {
    min-height: 300px;
    height: 300px;
  }

  .state-grid {
    grid-template-columns: 1fr;
  }
}
</style>
