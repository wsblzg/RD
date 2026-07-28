<template>
  <div :class="['ceramics-route-page', { 'about-layout': isAboutRoute }]">
    <NewHeaderNavigation />

    <section v-if="!isAboutRoute" class="hero-section" aria-label="页面标题区">
      <div class="hero-container" :style="heroStyle">
        <div class="hero-overlay"></div>
        <div class="hero-content">
          <p v-if="kicker" class="hero-kicker">{{ kicker }}</p>
          <div class="hero-title-row">
            <h1 class="hero-title">{{ title }}</h1>
            <span class="hero-seal font-seal">窑</span>
          </div>
          <p v-if="description" class="hero-desc">{{ description }}</p>
        </div>
      </div>
    </section>

    <main class="page-main">
      <section :class="['section', { alt, 'about-theme': isAboutRoute }]">
        <div class="section-inner">
          <header v-if="isAboutRoute" class="about-inline-head">
            <p v-if="kicker" class="about-kicker">{{ kicker }}</p>
            <h1 class="about-title">{{ title }}</h1>
            <p v-if="description" class="about-desc">{{ description }}</p>
          </header>
          <header v-if="subNavItems.length" class="section-head">
            <nav class="sub-nav" aria-label="二级导航">
              <router-link
                v-for="item in subNavItems"
                :key="item.to"
                :to="item.to"
                class="sub-link"
                :class="{ active: isActive(item) }"
              >
                {{ item.label }}
              </router-link>
            </nav>
          </header>
          <div class="page-body">
            <slot />
            <section v-if="contextSection" class="context-section" aria-label="页面延展内容">
              <header class="context-head">
                <p class="context-kicker">{{ contextSection.eyebrow }}</p>
                <h2>{{ contextSection.title }}</h2>
                <p>{{ contextSection.description }}</p>
              </header>

              <div class="context-grid">
                <article
                  v-for="item in contextSection.highlights"
                  :key="item.label"
                  class="context-card"
                >
                  <strong>{{ item.value }}</strong>
                  <span>{{ item.label }}</span>
                </article>
              </div>

              <div class="context-actions">
                <router-link
                  v-for="action in contextSection.actions"
                  :key="action.to"
                  :to="action.to"
                  class="context-action"
                >
                  <span>{{ action.label }}</span>
                  <small>{{ action.hint }}</small>
                </router-link>
              </div>

              <div class="context-faq">
                <details
                  v-for="item in contextSection.faqs"
                  :key="item.q"
                  class="faq-item"
                >
                  <summary>{{ item.q }}</summary>
                  <p>{{ item.a }}</p>
                </details>
              </div>
            </section>
          </div>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NewHeaderNavigation from '@/components/NewHeaderNavigation.vue'
import SiteFooter from '@/components/SiteFooter.vue'

const props = defineProps({
  kicker: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  alt: { type: Boolean, default: false },
  subNavItems: { type: Array, default: () => [] }
})

const route = useRoute()
const normalizedPath = computed(() => {
  const path = String(route.path || '')
  if (!path.startsWith('/ceramics')) {
    return path || '/'
  }
  const stripped = path.replace(/^\/ceramics(?=\/|$)/, '')
  return stripped || '/'
})

const contextSectionMap = {
  '/guide': {
    eyebrow: 'GUIDE EXTENSION',
    title: '把导览体验做成可复习的知识路径',
    description: '按“结构认识-作品观察-工艺步骤-传承人物”顺序浏览，能更快形成完整认知。',
    highlights: [
      { value: '4 段路径', label: '秘境、风采、步骤、匠人串联学习' },
      { value: '8 步工艺', label: '从泥料准备到出窑复盘全链路' },
      { value: '3 类视角', label: '窑外、窑内、俯瞰对比理解温场' }
    ],
    actions: [
      { to: '/guide/process', label: '继续查看制作步骤', hint: '把“看图”转成“看懂流程”' },
      { to: '/guide/artisans', label: '进入非遗匠人页面', hint: '理解传承脉络与人物分工' }
    ],
    faqs: [
      { q: '导览建议先看哪一页？', a: '建议先看柴烧秘境，再看作品展示，最后进入制作步骤与匠人故事。' },
      { q: '如何快速区分不同窑位效果？', a: '先在窑炉视角里定位火道，再到作品页对照火痕与落灰差异。' }
    ]
  },
  '/collections': {
    eyebrow: 'COLLECTIONS EXTENSION',
    title: '把浏览兴趣转成可沉淀的收藏行为',
    description: '上架浏览、详情理解、社区联动三段式路径，适合沉淀线下活动成果。',
    highlights: [
      { value: '馆藏总览', label: '聚焦在架藏品与系列信息展示' },
      { value: '兑换机制', label: '支持线下活动码入库数字藏品' },
      { value: '可持续返访', label: '收藏记录可作为后续运营触点' }
    ],
    actions: [
      { to: '/collections/catalog', label: '查看在架藏品', hint: '按系列与描述快速筛选目标藏品' },
      { to: '/ceramics/user-center?tab=collections', label: '进入个人中心收藏', hint: '在个人中心统一查看收藏与文章' }
    ],
    faqs: [
      { q: '兑换码通常来自哪里？', a: '一般由线下导览、研学活动或公开日等场景发放。' },
      { q: '没有登录能收藏吗？', a: '建议登录后再兑换或收藏，便于后续跨端同步与追踪。' }
    ]
  },
  '/shop': {
    eyebrow: 'SHOP EXTENSION',
    title: '把浏览兴趣转成可下单、可审核、可追踪的实物交易流程',
    description: '商品浏览、购物车结算、人工审核付款和后台发货形成一条清晰的轻量商城闭环。',
    highlights: [
      { value: '固定收款码', label: '不接复杂支付网关也能快速跑通交易链路' },
      { value: '购物车 + 订单', label: '支持加入购物车、填写收货信息、查看状态' },
      { value: '后台审核', label: '管理员可审核付款并录入物流信息' }
    ],
    actions: [
      { to: '/shop', label: '进入文创商城', hint: '浏览商品并加入购物车' },
      { to: '/shop/cart', label: '前往购物车', hint: '填写收货信息并提交订单' }
    ],
    faqs: [
      { q: '为什么结算后不是自动支付成功？', a: '当前采用固定微信收款码 + 人工审核的轻量方案，更适合项目现阶段快速落地。' },
      { q: '订单状态在哪里查看？', a: '提交订单后可在用户中心“我的订单”查看审核和发货进度。' }
    ]
  },
  '/intelligence': {
    eyebrow: 'INTELLIGENCE EXTENSION',
    title: '把单次提问升级为持续学习闭环',
    description: 'AI 鉴赏和知识问答建议联动使用，先识别再追问，结论更稳定。',
    highlights: [
      { value: '双引擎', label: '图片鉴赏 + 知识问答协同运行' },
      { value: '参考标签', label: '问答结果可追溯到命中知识片段' },
      { value: '连续对话', label: '支持上下文记忆与主题深挖' }
    ],
    actions: [
      { to: '/intelligence/appraisal', label: '上传图片做 AI 鉴赏', hint: '先获取结构化报告再继续提问' },
      { to: '/intelligence/qa', label: '进入知识问答', hint: '围绕火痕、器型、传承做定向咨询' }
    ],
    faqs: [
      { q: '什么时候用鉴赏，什么时候用问答？', a: '有具体作品图时优先鉴赏，无图或要解释原理时优先问答。' },
      { q: '答复里出现资料不足怎么办？', a: '可切换提问方向、补充上下文，或先通过鉴赏报告提供线索。' }
    ]
  },
  '/about': {
    eyebrow: 'ABOUT EXTENSION',
    title: '把项目背景、实践成果与参与方式放进同一条说明链路',
    description: '建议先看项目介绍，再看团队与合作、实践成果和到访参与，最后确认版权边界。',
    highlights: [
      { value: 'P0-P2', label: '版本演进路径清晰可追踪' },
      { value: '跨组协作', label: '内容、设计、技术与运营协同推进' },
      { value: '成果+参与', label: '调研成果与线下参与入口纳入统一板块' }
    ],
    actions: [
      { to: '/about/project', label: '查看项目介绍', hint: '了解定位、路线与版本演进' },
      { to: '/about/practice', label: '查看实践成果', hint: '快速抓住关键数据和成果结构' },
      { to: '/about/visit', label: '查看到访参与', hint: '获取预约流程与联系方式' },
      { to: '/about/copyright', label: '查看版权说明', hint: '确认素材使用边界与反馈流程' }
    ],
    faqs: [
      { q: '项目页面素材能否商业使用？', a: '当前仅用于教学展示与非遗传播，商业化使用需另行授权。' },
      { q: '如何参与线下活动或研学？', a: '可先查看“到访参与”页面，按团队规模、目标和时间提交预约需求。' }
    ]
  }
}

const heroImageMap = {
  '/home': '/vcg-kiln-glow.webp',
  '/guide': '/vcg-kiln-inside.webp',
  '/collections': '/vcg-kiln-vessels-row.webp',
  '/community': '/community-hero.webp',
  '/shop': '/青花梅瓶.webp',
  '/intelligence': '/vcg-pottery-window.webp',
  '/ai-creation': '/vcg-flambe-vase-museum.webp',
  '/about': '/vcg-ancient-kiln-painting.webp'
}

const heroImage = computed(() => {
  const matchedPrefix = Object.keys(heroImageMap).find(prefix => normalizedPath.value.startsWith(prefix))
  return matchedPrefix ? heroImageMap[matchedPrefix] : '/vcg-kiln-inside.webp'
})

const heroPositionMap = {
  '/home': 'center',
  '/guide': 'center 28%',
  '/collections': 'center 44%',
  '/shop': 'center 40%',
  '/intelligence': '66% center',
  '/ai-creation': 'center 42%',
  '/about': '50% 44%'
}

const heroMobilePositionMap = {
  '/home': 'center',
  '/guide': 'center 34%',
  '/collections': 'center 52%',
  '/shop': 'center 44%',
  '/intelligence': '72% center',
  '/ai-creation': 'center 48%',
  '/about': '56% 42%'
}

const resolveByRoute = (map, fallback = 'center') => {
  const matchedPrefix = Object.keys(map).find(prefix => normalizedPath.value.startsWith(prefix))
  return matchedPrefix ? map[matchedPrefix] : fallback
}

const heroStyle = computed(() => ({
  '--hero-position': resolveByRoute(heroPositionMap, 'center'),
  '--hero-mobile-position': resolveByRoute(heroMobilePositionMap, 'center'),
  backgroundImage: `linear-gradient(160deg, rgba(18,14,10,0.62) 0%, rgba(18,14,10,0.28) 100%), url('${heroImage.value}')`
}))

const isAboutRoute = computed(() => normalizedPath.value.startsWith('/about'))
const contextSection = computed(() => {
  const matchedPrefix = Object.keys(contextSectionMap).find(prefix => normalizedPath.value.startsWith(prefix))
  return matchedPrefix ? contextSectionMap[matchedPrefix] : null
})

const isActive = (item) => {
  if (item.exact) return normalizedPath.value === item.to
  return normalizedPath.value === item.to || normalizedPath.value.startsWith(`${item.to}/`)
}
</script>

<style scoped>
.ceramics-route-page {
  min-height: 100vh;
  background: var(--ym-bg);
  color: var(--ym-text);
  position: relative;
}

.ceramics-route-page::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.2;
  background:
    radial-gradient(circle at 8% 26%, rgba(26, 26, 26, 0.2), transparent 34%),
    radial-gradient(circle at 86% 78%, rgba(26, 26, 26, 0.14), transparent 30%);
  filter: blur(22px);
}

.ceramics-route-page.about-layout::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image: url('/vcg-ancient-kiln-painting.webp');
  background-size: cover;
  background-position: center 46%;
}

.ceramics-route-page.about-layout::before {
  background:
    linear-gradient(175deg, rgba(24, 20, 16, 0.64) 0%, rgba(24, 20, 16, 0.56) 100%),
    radial-gradient(circle at 8% 26%, rgba(26, 26, 26, 0.2), transparent 34%),
    radial-gradient(circle at 86% 78%, rgba(26, 26, 26, 0.14), transparent 30%);
  filter: none;
  opacity: 1;
}

/* ── Hero ── */
.hero-section {
  width: min(1260px, 94vw);
  margin: 22px auto 0;
  position: relative;
  z-index: 1;
}

.hero-container {
  position: relative;
  min-height: 340px;
  border-radius: 8px 28px 10px 32px;
  overflow: hidden;
  background-size: cover;
  background-position: var(--hero-position, center);
  display: flex;
  align-items: flex-end;
  box-shadow: 0 16px 34px rgba(32, 25, 21, 0.14);
  border: 1px solid rgba(58, 47, 40, 0.28);
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(165deg, rgba(20, 16, 13, 0.54), rgba(20, 16, 13, 0.22)),
    radial-gradient(circle at 8% 20%, rgba(255, 248, 236, 0.08), transparent 38%),
    radial-gradient(circle at 80% 76%, rgba(20, 16, 13, 0.26), transparent 45%);
}

.hero-overlay::after {
  content: '';
  position: absolute;
  inset: auto -22% -36% -6%;
  height: 62%;
  background:
    radial-gradient(ellipse at 25% 65%, rgba(20, 20, 20, 0.44), transparent 58%),
    radial-gradient(ellipse at 72% 55%, rgba(20, 20, 20, 0.28), transparent 52%);
  filter: blur(24px);
  opacity: 0.54;
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 32px 36px;
  color: #fff7ea;
}

.hero-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  opacity: 0.82;
  margin-bottom: 10px;
  font-family: var(--ym-font-serif);
}

.hero-title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.hero-title {
  font-family: var(--ym-font-calligraphy-cao);
  font-size: clamp(1.9rem, 4vw, 3rem);
  line-height: 1.15;
  margin-bottom: 10px;
  letter-spacing: 0.06em;
}

.hero-seal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  font-size: 1rem;
  border-radius: 4px;
  border: 1px solid rgba(var(--ym-gold-rgb), 0.54);
  background: rgba(var(--ym-gold-rgb), 0.16);
  color: rgba(255, 246, 234, 0.94);
  box-shadow: inset 0 0 0 1px rgba(255, 245, 235, 0.3);
}

.hero-desc {
  max-width: 680px;
  line-height: 1.82;
  opacity: 0.82;
  font-size: 0.97rem;
  font-family: var(--ym-font-sans);
}

/* ── Section ── */
.section {
  position: relative;
  scroll-margin-top: 90px;
  z-index: 1;
}

.section.alt {
  background: rgba(255, 249, 240, 0.58);
}

.section.about-theme {
  isolation: isolate;
}

.section.about-theme::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background: transparent;
}

.section.about-theme::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(175deg, rgba(24, 20, 16, 0.64) 0%, rgba(24, 20, 16, 0.48) 100%);
}

.about-inline-head {
  margin-bottom: 18px;
  color: #f8efe2;
}

.about-kicker {
  font-size: 0.74rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  opacity: 0.82;
}

.about-inline-head h1 {
  margin-top: 8px;
  line-height: 1.2;
}

.about-title {
  margin-top: 8px;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(1.8rem, 3vw, 2.5rem);
  letter-spacing: 0.05em;
}

.about-desc {
  margin-top: 8px;
  max-width: 760px;
  line-height: 1.8;
  opacity: 0.9;
}

.section-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
  padding: 28px 0 72px;
}

/* ── Sub Nav ── */
.section-head {
  margin-bottom: 22px;
}

.sub-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 118px;
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  padding: 7px 16px;
  text-decoration: none;
  color: var(--ym-text-secondary);
  background: rgba(255, 249, 240, 0.88);
  font-size: 0.9rem;
  transition: all 0.2s ease;
  font-family: var(--ym-font-ui);
  letter-spacing: 0.04em;
}

.sub-link:hover {
  border-color: rgba(58, 47, 40, 0.34);
  background: rgba(var(--ym-accent-rgb), 0.07);
  color: var(--ym-ink-jiao);
  transform: translateY(-1px);
}

.sub-link.active {
  border-color: rgba(58, 47, 40, 0.4);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-ink-jiao);
  font-weight: 500;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);
}

.section.about-theme .sub-link {
  border-color: rgba(112, 84, 58, 0.5);
  color: #5f4736;
  background: rgba(255, 248, 236, 0.6);
}

.section.about-theme .sub-link:hover {
  border-color: rgba(112, 84, 58, 0.72);
  background: rgba(112, 84, 58, 0.1);
  color: #4b3829;
}

.section.about-theme .sub-link.active {
  border-color: rgba(112, 84, 58, 0.78);
  background: rgba(112, 84, 58, 0.14);
  color: #4b3829;
}

.about-layout :deep(.site-footer) {
  margin-top: 0;
  position: relative;
  z-index: 1;
}

.page-body {
  display: grid;
  gap: 16px;
}

.context-section {
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 20px 8px 20px;
  padding: 18px;
  background: rgba(255, 249, 240, 0.86);
  display: grid;
  gap: 14px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(32, 25, 21, 0.06);
}

.context-section::before {
  content: '';
  position: absolute;
  inset: -20% auto auto -14%;
  width: 44%;
  height: 58%;
  pointer-events: none;
  background: radial-gradient(circle at 35% 35%, rgba(26, 26, 26, 0.2), transparent 64%);
  filter: blur(18px);
  opacity: 0.22;
}

.context-head h2 {
  margin-top: 6px;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(1.15rem, 2.1vw, 1.5rem);
  letter-spacing: 0.04em;
}

.context-head p {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.context-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  color: var(--ym-text-muted);
  text-transform: uppercase;
  font-family: var(--ym-font-serif);
}

.context-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.context-card {
  border: 1px solid var(--ym-border);
  border-radius: 4px 14px 4px 14px;
  padding: 12px;
  background: rgba(255, 252, 246, 0.88);
  display: grid;
  gap: 4px;
}

.context-card strong {
  font-family: var(--ym-font-calligraphy-ma);
  color: var(--ym-ink-nong);
}

.context-card span {
  color: var(--ym-text-secondary);
  font-size: 0.86rem;
  line-height: 1.7;
}

.context-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.context-action {
  border: 1px solid var(--ym-border-strong);
  border-radius: 6px 14px 6px 14px;
  padding: 10px 12px;
  text-decoration: none;
  background: rgba(255, 252, 246, 0.92);
  display: grid;
  gap: 2px;
  transition: all 0.2s ease;
}

.context-action span {
  color: var(--ym-text);
  font-weight: 500;
}

.context-action small {
  color: var(--ym-text-secondary);
  line-height: 1.6;
}

.context-action:hover {
  border-color: rgba(58, 47, 40, 0.36);
  background: rgba(var(--ym-accent-rgb), 0.06);
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(32, 25, 21, 0.06);
}

.context-faq {
  display: grid;
  gap: 8px;
}

.faq-item {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 252, 246, 0.84);
  padding: 10px 12px;
}

.faq-item summary {
  cursor: pointer;
  color: var(--ym-text);
  font-weight: 500;
  list-style: none;
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.03em;
}

.faq-item summary::-webkit-details-marker {
  display: none;
}

.faq-item p {
  margin-top: 8px;
  color: var(--ym-text-secondary);
  font-size: 0.9rem;
  line-height: 1.75;
}

.section.about-theme .context-section {
  border-color: rgba(255, 245, 230, 0.38);
  background: rgba(255, 248, 236, 0.68);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
}

.section.about-theme .context-card,
.section.about-theme .faq-item,
.section.about-theme .context-action {
  border-color: rgba(255, 245, 230, 0.48);
  background: rgba(255, 252, 246, 0.62);
}

.section.about-theme .context-head p,
.section.about-theme .context-card span,
.section.about-theme .faq-item p,
.section.about-theme .context-action small {
  color: #5b4634;
}

.section.about-theme .context-kicker {
  color: #6f5642;
}

.section.about-theme .context-head h2,
.section.about-theme .faq-item summary,
.section.about-theme .context-action span {
  color: #4b3829;
}

.section.about-theme .context-card strong {
  color: #6a4a31;
}

.context-action:focus-visible,
.faq-item summary:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

/* ── Responsive ── */
@media (max-width: 980px) {
  .hero-container {
    min-height: 260px;
    background-position: var(--hero-mobile-position, var(--hero-position, center));
  }

  .hero-content {
    padding: 24px;
  }

  .section-inner {
    padding: 20px 0 48px;
  }

  .context-grid,
  .context-actions {
    grid-template-columns: 1fr;
  }

  .section.about-theme::before {
    background-position: 56% 42%;
  }
}
</style>
