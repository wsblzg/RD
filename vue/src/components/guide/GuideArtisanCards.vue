<template>
  <article class="artisan-panel">
    <header class="panel-head">
      <p class="eyebrow">ARTISANS · 非遗匠人</p>
      <h3>传承人档案与工作站实践</h3>
      <p>本页内容根据《非遗传承人、匠人信息》重制，聚焦龙志雄、李晶及曲江柴烧工作站的真实传承实践。</p>
    </header>

    <div class="type-filter" role="tablist" aria-label="匠人分类筛选">
      <button
        v-for="type in artisanTypes"
        :key="type"
        type="button"
        class="type-btn"
        :class="{ active: activeType === type }"
        @click="activeType = type"
      >
        {{ type }}
      </button>
    </div>

    <div class="artisan-grid">
      <article
        v-for="item in filteredArtisans"
        :key="item.name"
        class="artisan-card"
        :class="{ expanded: expandedCard === item.name }"
        @click="expandedCard = expandedCard === item.name ? '' : item.name"
      >
        <div class="card-main">
          <div class="avatar" :style="{ background: item.bgColor }">
            <span class="avatar-char">{{ item.badge }}</span>
          </div>
          <div class="card-info">
            <h4>{{ item.name }}</h4>
            <p class="role">{{ item.role }}</p>
            <div class="tags">
              <span v-for="tag in item.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="stats-row">
              <span>{{ item.metric1Label }}：{{ item.metric1 }}</span>
              <span>{{ item.metric2Label }}：{{ item.metric2 }}</span>
            </div>
          </div>
          <span class="expand-icon" aria-hidden="true">›</span>
        </div>
        <p v-if="expandedCard === item.name" class="card-desc">{{ item.desc }}</p>
        <blockquote v-if="expandedCard === item.name && item.quote" class="card-quote">
          "{{ item.quote }}"
        </blockquote>
      </article>
    </div>

    <!-- 传承时间线 -->
    <div class="heritage-line">
      <p class="heritage-title">关键时间轴（资料摘录）</p>
      <div class="line-track">
        <div v-for="era in eras" :key="era.year" class="era-node">
          <div class="era-dot"></div>
          <div class="era-info">
            <span class="era-year">{{ era.year }}</span>
            <span class="era-label">{{ era.label }}</span>
          </div>
        </div>
      </div>
    </div>

  </article>
</template>

<script setup>
import { computed, ref } from 'vue'

const expandedCard = ref('')
const activeType = ref('全部')

const artisans = [
  {
    name: '龙志雄',
    type: '传承人',
    role: '韶关市第六批市级非遗代表性传承人 · 曲江柴烧陶艺第四代核心传人',
    desc: '早年从事炼钢工作，对火候控制有长期积累。1998年拜师许赞源系统学习曲江柴烧核心技艺，2005年创立个人柴烧工作室，2019年在马坝人遗址景区建成非遗工作站，形成“生产-展示-传习”一体化路径。',
    quote: '古法柴烧讲究“入窑一色，出窑万彩”，每件器物都不可复制。',
    tags: ['古法柴烧', '落灰釉', '火痕肌理', '石峡文化溯源'],
    metric1Label: '出生年月',
    metric1: '1975年10月',
    metric2Label: '现居地',
    metric2: '广东韶关曲江区',
    badge: '龙',
    bgColor: 'rgba(181,68,46,0.14)'
  },
  {
    name: '李晶',
    type: '教育推广',
    role: '曲江柴烧陶艺非遗教育推广老师 · 陶艺制作辅导老师',
    desc: '围绕“教育+传播”开展展厅导览、陶艺实操和公益培训。主打理论结合实践，面向亲子群体、妇女群体与社会公众持续开展非遗普及，推动柴烧技艺走向日常学习场景。',
    quote: '通过沉浸式教学与公益推广，让更多人看见并理解曲江柴烧。',
    tags: ['展厅导览', '实操教学', '公益培训', '公众传播'],
    metric1Label: '所属机构',
    metric1: '曲江柴烧陶艺非遗工作站',
    metric2Label: '工作地址',
    metric2: '马坝人遗址公园（风度书房后）',
    badge: '李',
    bgColor: 'rgba(115,128,110,0.14)'
  },
  {
    name: '曲江柴烧陶艺非遗工作站',
    type: '工作站',
    role: '创作基地 + 展示平台 + 研学体验场所',
    desc: '工作站位于韶关市曲江区马坝人遗址公园内，承载陶艺创作、非遗展示与研学体验三类功能，是当地非遗活态传承与文旅研学的重要阵地。',
    quote: '',
    tags: ['非遗展示', '研学体验', '创作实践', '乡村振兴'],
    metric1Label: '联系方式',
    metric1: '龙志雄 13726566173',
    metric2Label: '预约咨询',
    metric2: '李老师 13827953005',
    badge: '站',
    bgColor: 'rgba(43,43,43,0.08)'
  }
]

const artisanTypes = ['全部', '传承人', '教育推广', '工作站']
const filteredArtisans = computed(() => {
  if (activeType.value === '全部') return artisans
  return artisans.filter(item => item.type === activeType.value)
})

const eras = [
  { year: '1998', label: '龙志雄拜师许赞源系统学艺' },
  { year: '2005', label: '创立个人柴烧陶艺工作室' },
  { year: '2019', label: '马坝人遗址景区建成非遗工作站' },
  { year: '2020', label: '入选韶关市第六批市级非遗传承人' },
  { year: '2021', label: '开展亲子及公益培训班' },
  { year: '2025', label: '广东新闻联播等专题报道' }
]
</script>

<style scoped>
.artisan-panel {
  --accent: var(--ym-accent);
  --accent-rgb: var(--ym-accent-rgb);
  --support-rgb: var(--ym-support-rgb);
  --border: var(--ym-border);
  --border-strong: var(--ym-border-strong);
  --text: var(--ym-text);
  --text-secondary: var(--ym-text-secondary);
  --text-muted: var(--ym-text-muted);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 24px;
  background: rgba(255, 250, 240, 0.94);
  display: grid;
  gap: 22px;
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
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

/* ── Artisan Grid ── */
.artisan-grid {
  display: grid;
  gap: 12px;
}

.type-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.type-btn {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 6px 12px;
  background: rgba(255, 250, 240, 0.9);
  color: var(--text-secondary);
  font-size: 0.84rem;
  cursor: pointer;
}

.type-btn.active {
  border-color: rgba(var(--accent-rgb), 0.42);
  background: rgba(var(--accent-rgb), 0.1);
  color: var(--accent);
}

.artisan-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
  transition: all 0.24s ease;
}

.artisan-card:hover {
  border-color: rgba(var(--accent-rgb), 0.3);
  background: rgba(255, 255, 255, 0.96);
}

.artisan-card.expanded {
  border-color: rgba(var(--accent-rgb), 0.36);
  background: rgba(255, 250, 240, 0.98);
}

.card-main {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: start;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.avatar-char {
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--text);
}

.card-info h4 {
  font-size: 1rem;
  margin-bottom: 4px;
}

.role {
  font-size: 0.86rem;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.stats-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-row span {
  font-size: 0.78rem;
  color: var(--text-muted);
  border: 1px dashed var(--border-strong);
  border-radius: 999px;
  padding: 3px 8px;
}

.tag {
  font-size: 0.75rem;
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 3px 9px;
  color: var(--text-secondary);
  background: rgba(255, 250, 240, 0.9);
}

.expand-icon {
  font-size: 1.1rem;
  color: var(--text-muted);
  transition: transform 0.24s ease;
  align-self: center;
  padding-top: 2px;
}

.artisan-card.expanded .expand-icon {
  transform: rotate(90deg);
  color: var(--accent);
}

.card-desc {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 0.92rem;
}

.card-quote {
  margin-top: 10px;
  padding: 10px 14px;
  border-left: 3px solid rgba(var(--accent-rgb), 0.5);
  background: rgba(var(--accent-rgb), 0.05);
  border-radius: 0 8px 8px 0;
  font-style: italic;
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.7;
}

/* ── Heritage Line ── */
.heritage-line {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: rgba(244, 241, 232, 0.7);
}

.heritage-title {
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  color: var(--text-muted);
  text-transform: uppercase;
  margin-bottom: 14px;
}

.line-track {
  display: flex;
  align-items: flex-start;
  gap: 0;
  overflow-x: auto;
}

.era-node {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  position: relative;
  min-width: 80px;
}

.era-node::before {
  content: '';
  position: absolute;
  top: 6px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--border-strong);
  z-index: 0;
}

.era-node:last-child::before {
  display: none;
}

.era-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--accent);
  background: rgba(var(--accent-rgb), 0.18);
  position: relative;
  z-index: 1;
}

.era-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.era-year {
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--text);
}

.era-label {
  font-size: 0.76rem;
  color: var(--text-muted);
  text-align: center;
  white-space: nowrap;
}

@media (max-width: 620px) {
  .stats-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
