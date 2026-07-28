<template>
  <article class="guide-card works-panel">
    <header class="card-head">
      <p class="eyebrow">WORKS</p>
      <h3>马坝风采（作品展示）</h3>
      <p>结合钦州坭兴陶、大风江古灶、台湾柴烧与老蛇窑案例重制作品解读，支持弹窗大图查看、左右切换与细节说明。</p>
    </header>

    <div class="works-grid">
      <figure
        v-for="(item, index) in works"
        :key="item.name"
        class="work-item"
        role="button"
        tabindex="0"
        @click="openViewer(index)"
        @keydown.enter.prevent="openViewer(index)"
        @keydown.space.prevent="openViewer(index)"
      >
        <img :src="item.src" :alt="item.name" :style="{ objectPosition: item.position || 'center' }" loading="lazy" />
        <figcaption>
          <strong>{{ item.name }}</strong>
          <span>{{ item.series }}</span>
        </figcaption>
        <span class="work-tag">{{ item.surface }}</span>
      </figure>
    </div>

    <div
      v-if="viewerOpen"
      ref="dialogRef"
      class="viewer-dialog"
      role="dialog"
      aria-modal="true"
      :aria-label="currentItem.name"
      tabindex="-1"
      @click.self="closeViewer"
    >
      <button ref="closeBtnRef" class="viewer-btn close" type="button" aria-label="close" @click="closeViewer">×</button>
      <button class="viewer-btn prev" type="button" aria-label="previous" @click="prevItem">‹</button>
      <figure class="viewer-figure">
        <img :src="currentItem.src" :alt="currentItem.name" />
        <figcaption>{{ currentItem.name }} · {{ activeIndex + 1 }} / {{ works.length }}</figcaption>
      </figure>
      <aside class="viewer-meta">
        <h4>{{ currentItem.name }}</h4>
        <p>{{ currentItem.desc }}</p>
        <div class="meta-tags">
          <span>{{ currentItem.series }}</span>
          <span>{{ currentItem.surface }}</span>
          <span>{{ currentItem.period }}</span>
        </div>
      </aside>
      <button class="viewer-btn next" type="button" aria-label="next" @click="nextItem">›</button>
    </div>
  </article>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const works = [
  {
    name: '大风江古灶开窑记录',
    src: '/vcg-kiln-vessels-row.webp',
    series: '坭兴陶案例',
    surface: '1300℃裸烧',
    period: '2020 开窑活动',
    desc: '参考钦州大风江古灶开窑叙事，强调“土、火、柴、窑”的协同关系，聚焦高温裸烧后的自然落灰层次。'
  },
  {
    name: '坭兴陶窑位火痕观察',
    src: '/vcg-kiln-inside.webp',
    series: '坭兴陶案例',
    surface: '窑位梯度',
    period: '当代柴窑',
    desc: '聚焦火道与窑位温差形成的火痕走向，便于对比高温区与中温区在表面润泽度、流动性的差异。'
  },
  {
    name: '北埔土矿柴烧器',
    src: '/vcg-flambe-vase-museum.webp',
    series: '台湾柴烧',
    surface: '在地土矿',
    period: '北埔路径',
    position: '50% 42%',
    desc: '参考台湾柴烧“在地土矿”实践路径，突出土料杂质与持温策略对器表色域、肌理层次的共同影响。'
  },
  {
    name: '在地土矿肌理近观',
    src: '/vcg-olive-vase-closeup.webp',
    series: '台湾柴烧',
    surface: '落灰与孔隙',
    period: '器用美学',
    position: '54% 34%',
    desc: '局部近景用于观察落灰厚薄、细小孔隙与口沿转折，体现“自然不工整”在柴烧审美中的价值。'
  },
  {
    name: '老蛇窑柴烧作品',
    src: '/vcg-kiln-glow.webp',
    series: '添兴窑案例',
    surface: '蛇窑柴烧',
    period: '一甲子窑脉',
    desc: '依据添兴窑“老蛇窑柴烧”案例，强调每窑光泽不可复制的随机性，也是爱陶者重视的收藏体验来源。'
  },
  {
    name: '竹碳能量陶与璞真烧',
    src: '/vcg-kiln-arch-vertical.webp',
    series: '添兴窑案例',
    surface: '产品体系',
    period: '2005 以后',
    position: 'center 44%',
    desc: '从“生活陶艺品-柴烧-竹碳陶-璞真烧”产品体系切入，呈现传统窑艺在当代生活器与文创场景中的转化。'
  }
]

const viewerOpen = ref(false)
const activeIndex = ref(0)
const dialogRef = ref(null)
const closeBtnRef = ref(null)
const lastActiveEl = ref(null)

const currentItem = computed(() => works[activeIndex.value] || works[0])

const prevItem = () => {
  activeIndex.value = (activeIndex.value - 1 + works.length) % works.length
}

const nextItem = () => {
  activeIndex.value = (activeIndex.value + 1) % works.length
}

const openViewer = async (index) => {
  lastActiveEl.value = document.activeElement instanceof HTMLElement ? document.activeElement : null
  activeIndex.value = index
  viewerOpen.value = true
  await nextTick()
  closeBtnRef.value?.focus()
}

const closeViewer = () => {
  viewerOpen.value = false
  if (lastActiveEl.value) {
    nextTick(() => lastActiveEl.value?.focus())
  }
}

const trapTab = (event) => {
  const root = dialogRef.value
  if (!root) return
  const focusables = root.querySelectorAll(
    'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
  )
  if (!focusables.length) return
  const first = focusables[0]
  const last = focusables[focusables.length - 1]
  const active = document.activeElement
  if (event.shiftKey) {
    if (active === first || !root.contains(active)) {
      event.preventDefault()
      last.focus()
    }
  } else if (active === last || !root.contains(active)) {
    event.preventDefault()
    first.focus()
  }
}

const handleKeydown = (event) => {
  if (!viewerOpen.value) return
  if (event.key === 'Escape') {
    closeViewer()
    return
  }
  if (event.key === 'ArrowLeft') {
    prevItem()
    return
  }
  if (event.key === 'ArrowRight') {
    nextItem()
    return
  }
  if (event.key === 'Tab') {
    trapTab(event)
  }
}

watch(viewerOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

window.addEventListener('keydown', handleKeydown)

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.guide-card {
  --text-primary: var(--ym-text);
  --text-secondary: var(--ym-text-secondary);
  --text-muted: var(--ym-text-muted);
  --border: var(--ym-border);
  --border-strong: var(--ym-border-strong);
  --accent-rgb: var(--ym-accent-rgb);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 24px;
  background: rgba(255, 250, 240, 0.94);
  display: grid;
  gap: 18px;
}

.card-head h3 {
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
  margin: 8px 0 8px;
}

.card-head p {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 0.93rem;
}

.eyebrow {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.work-item {
  position: relative;
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  background: rgba(255, 250, 240, 0.9);
  cursor: pointer;
  transition: all 0.24s ease;
  margin: 0;
}

.work-item:hover {
  border-color: rgba(var(--accent-rgb), 0.36);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.work-item img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  display: block;
  transition: transform 0.36s ease;
}

.work-item:hover img {
  transform: scale(1.04);
}

.work-item figcaption {
  padding: 10px 12px;
  display: grid;
  gap: 2px;
}

.work-item figcaption strong {
  font-size: 0.88rem;
  color: var(--text-primary);
}

.work-item figcaption span {
  font-size: 0.76rem;
  color: var(--text-muted);
}

.work-tag {
  position: absolute;
  left: 10px;
  top: 10px;
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 0.72rem;
  color: #fff7ea;
  background: rgba(18, 14, 10, 0.64);
  backdrop-filter: blur(4px);
}

/* ── Viewer ── */
.viewer-dialog {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(10, 8, 6, 0.88);
  backdrop-filter: blur(8px);
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr) minmax(220px, 300px) 60px;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.viewer-figure {
  margin: 0;
  display: grid;
  gap: 10px;
  justify-items: center;
}

.viewer-figure img {
  width: min(860px, 78vw);
  max-height: 76vh;
  border-radius: 14px;
  object-fit: contain;
  background: rgba(0,0,0,0.5);
}

.viewer-figure figcaption {
  color: rgba(255, 245, 220, 0.7);
  font-size: 0.88rem;
}

.viewer-meta {
  align-self: stretch;
  border-radius: 14px;
  background: rgba(255, 250, 240, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 16px;
  color: #2f2b26;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.viewer-meta h4 {
  font-family: var(--ym-font-display);
  font-size: 1rem;
}

.viewer-meta p {
  line-height: 1.8;
  font-size: 0.88rem;
  color: #4f4940;
  flex: 1;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-tags span {
  border: 1px solid rgba(43, 43, 43, 0.2);
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 0.75rem;
  color: #4f4940;
  background: rgba(244, 241, 232, 0.8);
}

.viewer-btn {
  border: 1px solid rgba(255, 245, 220, 0.3);
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255,255,255,0.12);
  backdrop-filter: blur(4px);
  color: #fff7ea;
  font-size: 1.5rem;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.18s ease;
}

.viewer-btn:hover {
  background: rgba(255,255,255,0.22);
  border-color: rgba(255, 245, 220, 0.55);
}

.viewer-btn.close {
  position: fixed;
  top: 18px;
  right: 18px;
  font-size: 1.3rem;
}

.viewer-btn:focus-visible,
.work-item:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

@media (max-width: 880px) {
  .works-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1120px) {
  .viewer-dialog {
    grid-template-columns: 52px 1fr 52px;
  }
  .viewer-meta {
    display: none;
  }
}

@media (max-width: 580px) {
  .works-grid {
    grid-template-columns: 1fr;
  }

  .viewer-dialog {
    grid-template-columns: auto 1fr auto;
    gap: 8px;
    padding: 14px;
  }
}
</style>
