<template>
  <div class="home-page">
    <NewHeaderNavigation />

    <!-- Hero 首屏 -->
    <section class="hero-section" aria-label="项目首屏">
      <div class="hero-bg" role="img" aria-label="柴烧陶瓷场景">
        <div class="hero-overlay"></div>
      </div>
      <div class="hero-content">
        <p class="hero-kicker">柴智云 · 非遗数字导览馆</p>
        <h1 class="hero-title font-calligraphy-cao">窑火薪传<br />柴烧生光</h1>
        <p class="hero-sub">看见工艺，看懂审美，沉淀数字资产，推动实践参与</p>
        <div class="hero-actions">
          <router-link to="/ceramics/guide/kiln" class="btn-primary">进入柴烧导览</router-link>
          <router-link to="/ceramics/collections/catalog" class="btn-ghost">查看数字藏品</router-link>
          <router-link to="/ceramics/intelligence/appraisal" class="btn-ghost">体验智鉴中枢</router-link>
        </div>
      </div>
      <div class="hero-ink-poem vertical-text font-calligraphy-xing" aria-hidden="true">
        窑火相承 · 薪传未来
      </div>
      <div class="hero-scroll-hint" aria-hidden="true">
        <span></span>
      </div>
    </section>

    <!-- 核心指标带 -->
    <section class="metrics-section" aria-label="核心调研数据">
      <div class="metrics-inner">
        <article v-for="item in coreMetrics" :key="item.label" class="metric-card">
          <span class="metric-value">{{ item.value }}</span>
          <span class="metric-label">{{ item.label }}</span>
        </article>
      </div>
    </section>

    <!-- 快速入口区 -->
    <section class="entries-section" aria-label="一级入口">
      <div class="entries-inner">
        <header class="entries-head">
          <p class="section-kicker">EXPLORE</p>
          <h2>探索非遗数字世界</h2>
          <p>围绕“古灶机理、在地土矿、研学转化”三条证据链组织导览路径，让每一步都能对应到可解释、可复盘的实践结果。</p>
        </header>
        <div class="entries-layout">
          <aside class="entries-rail" aria-label="导览动线建议">
            <article class="route-card">
              <p class="route-kicker">TODAY'S ROUTE</p>
              <h3>{{ guideSuggestion.title }}</h3>
              <p>{{ guideSuggestion.desc }}</p>
              <router-link :to="guideSuggestion.to" class="route-link">按建议进入 →</router-link>
              <p class="route-time">更新于 {{ currentTimeLabel }}</p>
            </article>
            <ol class="route-steps">
              <li v-for="phase in routePhases" :key="phase.stage" class="route-step">
                <span class="step-index">{{ phase.stage }}</span>
                <div class="step-body">
                  <h4>{{ phase.title }}</h4>
                  <p>{{ phase.desc }}</p>
                  <span>{{ phase.meta }}</span>
                </div>
              </li>
            </ol>
          </aside>

          <div class="entries-grid">
            <router-link
              v-for="entry in entries"
              :key="entry.to"
              :to="entry.to"
              class="entry-card"
            >
              <div class="entry-head">
                <div class="entry-icon" :style="{ background: entry.color }" aria-hidden="true">
                  {{ entry.icon }}
                </div>
                <div class="entry-title-wrap">
                  <h3>{{ entry.title }}</h3>
                  <span class="entry-target">{{ entry.scene }}</span>
                </div>
              </div>
              <p>{{ entry.desc }}</p>
              <p class="entry-outcome">可获得：{{ entry.outcome }}</p>
              <div class="entry-foot">
                <span>{{ entry.duration }}</span>
                <span class="entry-cta">进入 {{ entry.cta }} →</span>
              </div>
            </router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- 项目价值主张 -->
    <section class="value-section" aria-label="项目价值">
      <div class="value-inner">
        <div class="value-text">
          <p class="section-kicker">OUR MISSION</p>
          <h2>把古灶经验<br />转成可验证知识</h2>
          <p>
            以古灶工序为主线、在地土矿为变量、研学活动为转化场景，建立“看得见过程、比得出差异、沉淀成成果”的
            数字化实践闭环，让柴烧不只被观看，更能被理解和复用。
          </p>
          <div class="mission-tags">
            <span>古灶工序可视化</span>
            <span>在地土矿可比对</span>
            <span>研学成果可沉淀</span>
          </div>
          <div class="value-actions">
            <router-link to="/ceramics/about/project" class="btn-outline">了解项目背景 →</router-link>
            <section class="community-entry entry-inline">
              <header class="publish-head">
                <h3>社区发布入口</h3>
                <p>发布活动纪实、工艺心得和传承故事，支持图文编辑与图片上传。</p>
              </header>
              <button type="button" class="btn-primary open-publish-btn" @click="goPublishPage">去发布文章</button>
            </section>
          </div>
        </div>

        <div class="value-board community-board">
          <section class="community-feed">
            <header class="community-head">
              <p class="community-kicker">COMMUNITY</p>
              <h3>最新发布 · 非遗社区</h3>
              <p>传承人、参与者与学习者在这里持续记录现场故事与实践事件。</p>
            </header>

            <p v-if="communityLoading" class="community-tip">文章加载中...</p>
            <p v-else-if="communityError" class="community-tip error">{{ communityError }}</p>
            <p v-else-if="latestPosts.length === 0" class="community-tip">暂无文章，欢迎发布第一篇社区动态。</p>

            <div v-else class="post-list">
              <article v-for="post in latestPosts" :key="post.id" class="post-card">
                <img v-if="post.coverImage" :src="resolvePostCover(post.coverImage)" :alt="post.title" class="post-cover" loading="lazy" />
                <div class="post-body">
                  <p class="post-meta">
                    <span>{{ post.authorName || '社区用户' }}</span>
                    <span>{{ formatPostTime(post.createdAt) }}</span>
                  </p>
                  <h4>{{ post.title }}</h4>
                  <p>{{ post.summary || plainPostText(post.contentHtml) }}</p>
                  <div class="post-actions">
                    <button type="button" class="btn-outline post-detail-btn" @click="goPostDetail(post.id)">阅读全文</button>
                  </div>
                </div>
              </article>
            </div>
          </section>

        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NewHeaderNavigation from '@/components/NewHeaderNavigation.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { normalizeMediaUrl } from '@/utils/collectiblesApi'

const HOME_COMMUNITY_POST_LIMIT = 3
const HOME_COMMUNITY_CACHE_KEY = 'yc_home_latest_posts_v1'
const HOME_COMMUNITY_CACHE_TTL_MS = 5 * 60 * 1000

const coreMetrics = [
  { value: '63.44%', label: '前测样本对马坝柴烧认知不足，存在系统化导览缺口' },
  { value: '70.97%', label: '未到访人群愿意参与窑场研学，具备线下转化基础' },
  { value: '76.88%', label: '认同柴烧工艺差异，可支撑机理化教学表达' },
  { value: '93%+', label: '认同在地窑火文化价值，适合长期社区运营' }
]

const entries = [
  {
    to: '/ceramics/guide/kiln',
    title: '柴烧导览',
    cta: '导览',
    scene: '适合首次了解者',
    desc: '围绕投柴孔、火道、窑门、窑位等关键部位，建立“结构-温场-工艺动作”对应关系。',
    outcome: '形成可复用的古灶讲解路径与工序认知图',
    duration: '建议 6-10 分钟',
    icon: '窑',
    color: 'rgba(176,138,73,0.16)'
  },
  {
    to: '/ceramics/collections/catalog',
    title: '数字藏品馆',
    cta: '藏品馆',
    scene: '适合活动参与者',
    desc: '将开窑观察、研学任务与藏品领取记录关联，沉淀“人-窑次-作品”的参与轨迹。',
    outcome: '形成可追踪的研学参与档案与数字凭证',
    duration: '建议 5-8 分钟',
    icon: '藏',
    color: 'rgba(108,90,77,0.12)'
  },
  {
    to: '/ceramics/intelligence/appraisal',
    title: '智鉴中枢',
    cta: '智鉴',
    scene: '适合深度学习者',
    desc: '结合胎土颗粒、火痕分布、落灰轨迹等表征，辅助判断土矿差异与烧成机理。',
    outcome: '获得“现象-机理-工序”三段式解释结果',
    duration: '建议 8-12 分钟',
    icon: '鉴',
    color: 'rgba(161,75,52,0.1)'
  },
  {
    to: '/ceramics/about/practice',
    title: '实践成果',
    cta: '成果',
    scene: '适合线下活动组织者',
    desc: '集中查看调研证据、成果框架与页面转译逻辑，理解项目如何把非遗研究落成可展示内容。',
    outcome: '沉淀可复用的研学方案、成果说明与活动复盘模板',
    duration: '建议 4-7 分钟',
    icon: '果',
    color: 'rgba(176,138,73,0.1)'
  },
  {
    to: '/ceramics/about/project',
    title: '关于项目',
    cta: '关于',
    scene: '适合评审与合作方',
    desc: '展示项目目标、实施分工、阶段里程碑与合作机制，便于快速对齐落地条件。',
    outcome: '快速判断项目成熟度与协作可行性',
    duration: '建议 3-5 分钟',
    icon: '关',
    color: 'rgba(43,43,43,0.06)'
  }
]

const routePhases = [
  {
    stage: '01',
    title: '古灶机理线',
    desc: '以窑炉结构、火路组织和升温节律为主轴，先建立“为什么这样烧”的底层认知。',
    meta: '入口：柴烧导览'
  },
  {
    stage: '02',
    title: '在地土矿线',
    desc: '对照不同土矿与胎土表现，用可见表征反推矿物差异与烧成反应机制。',
    meta: '入口：智鉴中枢'
  },
  {
    stage: '03',
    title: '研学转化线',
    desc: '将参观体验、任务打卡与成果沉淀联动，形成可持续运营的学习社区闭环。',
    meta: '入口：数字藏品馆 / 关于'
  }
]

const router = useRouter()
const currentTime = ref(new Date())
let clockTimer = null
let loadPostsTimer = null
let loadPostsIdleId = null

const latestPosts = ref([])
const communityLoading = ref(false)
const communityError = ref('')

const currentTimeLabel = computed(() => currentTime.value.toLocaleString('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
}))

const guideSuggestion = computed(() => {
  const hour = currentTime.value.getHours()
  if (hour < 11) {
    return {
      title: '晨间适合“先导览后问答”',
      desc: '先建立窑炉与工艺框架，再进入智鉴问答，理解效率更高。',
      to: '/ceramics/guide/kiln'
    }
  }
  if (hour < 17) {
    return {
      title: '白天适合“导览 + 藏品联动”',
      desc: '先看工艺再看藏品，更容易把纹理、器形与价值关联起来。',
      to: '/ceramics/collections/catalog'
    }
  }
  return {
    title: '晚间适合“智鉴深聊”',
      desc: '建议进入问答区连续对话，围绕窑变机理和审美特征做进阶学习。',
      to: '/ceramics/intelligence/qa'
  }
})

const formatPostTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

const plainPostText = (html) => {
  const plain = String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return plain.length > 80 ? `${plain.slice(0, 80)}...` : plain
}

const resolvePostCover = (url) => normalizeMediaUrl(url)

const goPublishPage = () => {
  router.push('/ceramics/community/publish')
}

const goPostDetail = (id) => {
  if (!id) return
  router.push(`/ceramics/community/post/${id}`)
}

const normalizePostList = (input) => {
  if (!Array.isArray(input)) return []
  return input.slice(0, HOME_COMMUNITY_POST_LIMIT)
}

const readLatestPostsCache = () => {
  try {
    const raw = localStorage.getItem(HOME_COMMUNITY_CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    const list = normalizePostList(parsed?.list)
    const timestamp = Number(parsed?.timestamp || 0)
    if (!list.length || !Number.isFinite(timestamp)) return null
    return { list, timestamp }
  } catch (error) {
    return null
  }
}

const writeLatestPostsCache = (list) => {
  try {
    const payload = {
      list: normalizePostList(list),
      timestamp: Date.now()
    }
    localStorage.setItem(HOME_COMMUNITY_CACHE_KEY, JSON.stringify(payload))
  } catch (error) {
    // localStorage 不可用时跳过缓存写入，不阻断主流程
  }
}

const loadLatestPosts = async ({ silent = false } = {}) => {
  if (!silent) {
    communityLoading.value = true
  }
  communityError.value = ''
  try {
    const { communityAPI } = await import('@/utils/collectiblesApi')
    const data = await communityAPI.getLatestPosts(HOME_COMMUNITY_POST_LIMIT)
    const list = normalizePostList(data)
    latestPosts.value = list
    if (list.length) {
      writeLatestPostsCache(list)
    }
  } catch (error) {
    if (!latestPosts.value.length) {
      latestPosts.value = []
      communityError.value = error?.message || '社区文章加载失败'
    }
  } finally {
    if (!silent) {
      communityLoading.value = false
    }
  }
}

onMounted(() => {
  clockTimer = window.setInterval(() => {
    currentTime.value = new Date()
  }, 60000)

  const cache = readLatestPostsCache()
  const hasCache = Boolean(cache?.list?.length)
  if (hasCache) {
    latestPosts.value = cache.list
  }
  const needRefresh =
    !cache || (Date.now() - cache.timestamp > HOME_COMMUNITY_CACHE_TTL_MS)

  if (needRefresh) {
    if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
      loadPostsIdleId = window.requestIdleCallback(() => {
        loadLatestPosts({ silent: hasCache })
      }, { timeout: 1200 })
    } else {
      loadPostsTimer = window.setTimeout(() => {
        loadLatestPosts({ silent: hasCache })
      }, hasCache ? 600 : 300)
    }
  }
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
  if (loadPostsTimer) {
    window.clearTimeout(loadPostsTimer)
    loadPostsTimer = null
  }
  if (loadPostsIdleId !== null && typeof window !== 'undefined' && typeof window.cancelIdleCallback === 'function') {
    window.cancelIdleCallback(loadPostsIdleId)
    loadPostsIdleId = null
  }
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--ym-bg);
  color: var(--ym-text);
  position: relative;
}

.home-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.18;
  background:
    radial-gradient(circle at 14% 26%, rgba(26, 26, 26, 0.26), transparent 34%),
    radial-gradient(circle at 86% 72%, rgba(26, 26, 26, 0.18), transparent 30%);
  filter: blur(28px);
}

/* ── Hero ── */
.hero-section {
  position: relative;
  min-height: 92vh;
  display: flex;
  align-items: center;
  overflow: hidden;
  z-index: 1;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background-image:
    url('/vcg-kiln-glow.webp');
  background-size: cover;
  background-position: 62% center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(145deg, rgba(20, 16, 13, 0.74) 0%, rgba(20, 16, 13, 0.44) 64%, rgba(20, 16, 13, 0.24) 100%),
    radial-gradient(circle at 74% 76%, rgba(20, 16, 13, 0.28), transparent 40%);
}

.hero-overlay::after {
  content: '';
  position: absolute;
  inset: auto -16% -30% -4%;
  height: 56%;
  background:
    radial-gradient(ellipse at 22% 58%, rgba(16, 16, 16, 0.42), transparent 56%),
    radial-gradient(ellipse at 78% 46%, rgba(16, 16, 16, 0.3), transparent 52%);
  filter: blur(24px);
  opacity: 0.6;
}

.hero-content {
  position: relative;
  z-index: 1;
  width: min(1260px, 94vw);
  margin: 0 auto;
  padding: 120px 0 100px;
}

.hero-kicker {
  font-size: 0.78rem;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  color: rgba(255, 246, 234, 0.82);
  margin-bottom: 20px;
  font-family: var(--ym-font-serif);
}

.hero-title {
  font-family: var(--ym-font-calligraphy-cao);
  font-size: clamp(2.8rem, 7vw, 5rem);
  line-height: 1.1;
  color: #fff7ea;
  margin-bottom: 20px;
  letter-spacing: 0.06em;
  text-shadow: 0 8px 18px rgba(0, 0, 0, 0.32);
}

.hero-sub {
  font-size: clamp(1rem, 2vw, 1.2rem);
  color: rgba(255, 245, 220, 0.86);
  line-height: 1.7;
  max-width: 520px;
  margin-bottom: 36px;
  font-family: var(--ym-font-sans);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  padding: 13px 28px;
  background: rgba(var(--ym-accent-rgb), 0.24);
  color: #fff7ea;
  border-radius: 4px 14px 4px 14px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.48);
  text-decoration: none;
  font-weight: 500;
  letter-spacing: 0.02em;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  font-family: var(--ym-font-calligraphy-ma);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.btn-primary:hover {
  background: rgba(var(--ym-accent-rgb), 0.3);
  border-color: rgba(var(--ym-accent-rgb), 0.62);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(20, 16, 13, 0.18);
}

.btn-ghost {
  display: inline-flex;
  align-items: center;
  padding: 12px 24px;
  border: 1px solid rgba(255, 245, 220, 0.44);
  color: #fff7ea;
  border-radius: 4px 12px 4px 12px;
  text-decoration: none;
  backdrop-filter: blur(4px);
  background: rgba(255, 249, 240, 0.08);
  transition: all 0.2s ease;
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.04em;
}

.btn-ghost:hover {
  border-color: rgba(255, 245, 220, 0.62);
  background: rgba(255, 249, 240, 0.12);
}

.hero-scroll-hint {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
}

.hero-ink-poem {
  position: absolute;
  right: min(4vw, 52px);
  top: 23%;
  z-index: 1;
  color: rgba(255, 246, 232, 0.78);
  letter-spacing: 0.16em;
  font-size: clamp(1.05rem, 1.8vw, 1.6rem);
  line-height: 1.7;
  text-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
}

.hero-scroll-hint span {
  display: block;
  width: 1px;
  height: 52px;
  background: linear-gradient(to bottom, rgba(255,245,220,0.6), transparent);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.4; transform: scaleY(1); }
  50% { opacity: 1; transform: scaleY(0.6); }
}

/* ── Metrics ── */
.metrics-section {
  background: linear-gradient(180deg, rgba(32, 25, 21, 0.96), rgba(48, 40, 34, 0.96));
}

.metrics-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  divide-x: 1px solid rgba(255,255,255,0.12);
}

.metric-card {
  padding: 28px 24px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-card:last-child {
  border-right: none;
}

.metric-value {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  color: #d4af37;
  line-height: 1.1;
}

.metric-label {
  font-size: 0.88rem;
  color: rgba(255, 245, 220, 0.64);
  line-height: 1.6;
}

/* ── Entries ── */
.entries-section {
  padding: 82px 0 92px;
  position: relative;
}

.entries-section::before {
  content: '';
  position: absolute;
  left: -10%;
  top: 8%;
  width: 42%;
  height: 48%;
  background: radial-gradient(circle at 40% 30%, rgba(26, 26, 26, 0.16), transparent 62%);
  filter: blur(18px);
  opacity: 0.24;
  pointer-events: none;
}

.entries-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
}

.entries-head {
  margin-bottom: 36px;
  max-width: 560px;
}

.section-kicker {
  font-size: 0.76rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--ym-text-muted);
  margin-bottom: 10px;
}

.entries-head h2 {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(1.6rem, 3.2vw, 2.2rem);
  margin-bottom: 12px;
  letter-spacing: 0.04em;
}

.entries-head > p {
  color: var(--ym-text-secondary);
  line-height: 1.8;
}

.entries-layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) 1fr;
  gap: 16px;
  align-items: start;
}

.entries-rail {
  display: grid;
  gap: 10px;
  position: sticky;
  top: 92px;
}

.route-card {
  border: 1px solid rgba(58, 47, 40, 0.24);
  border-radius: 6px 20px 6px 20px;
  background: linear-gradient(155deg, rgba(255, 249, 240, 0.96), rgba(245, 239, 230, 0.9));
  padding: 16px;
  display: grid;
  gap: 8px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(32, 25, 21, 0.06);
}

.route-card::after {
  content: '';
  position: absolute;
  inset: auto -8% -24% -12%;
  height: 46%;
  background: radial-gradient(circle at 24% 52%, rgba(26, 26, 26, 0.18), transparent 60%);
  filter: blur(14px);
  opacity: 0.24;
  pointer-events: none;
}

.route-kicker {
  margin: 0;
  font-size: 0.7rem;
  letter-spacing: 0.18em;
  color: var(--ym-text-muted);
}

.route-card h3 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.12rem;
}

.route-card > p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.75;
  font-size: 0.88rem;
}

.route-link {
  justify-self: flex-start;
  margin-top: 2px;
  text-decoration: none;
  color: var(--ym-ink-nong);
  font-size: 0.84rem;
  border-bottom: 1px solid rgba(var(--ym-gold-rgb), 0.28);
  transition: border-color 0.2s ease;
}

.route-link:hover {
  border-color: rgba(var(--ym-gold-rgb), 0.64);
}

.route-time {
  margin-top: 2px;
  font-size: 0.74rem;
  color: var(--ym-text-muted);
}

.route-steps {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.route-step {
  border: 1px solid var(--ym-border);
  border-radius: 4px 14px 6px 16px;
  background: rgba(255, 249, 240, 0.76);
  padding: 10px 11px;
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 8px;
}

.step-index {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  display: grid;
  place-items: center;
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--ym-ink-nong);
  background: rgba(var(--ym-gold-rgb), 0.12);
  border: 1px solid rgba(var(--ym-gold-rgb), 0.3);
}

.step-body h4 {
  margin: 0 0 4px;
  font-size: 0.92rem;
  font-family: var(--ym-font-calligraphy-ma);
}

.step-body p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.66;
  font-size: 0.82rem;
}

.step-body span {
  display: inline-block;
  margin-top: 6px;
  font-size: 0.74rem;
  color: var(--ym-text-muted);
}

.entries-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.entry-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 20px 6px 22px;
  padding: 22px;
  text-decoration: none;
  color: inherit;
  background: rgba(255, 249, 240, 0.88);
  display: grid;
  gap: 10px;
  transition: all 0.22s ease;
  position: relative;
  overflow: hidden;
}

.entry-card:hover {
  border-color: rgba(58, 47, 40, 0.3);
  background: rgba(255, 249, 240, 0.96);
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(32, 25, 21, 0.08);
}

.entry-card::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 2px;
  background: linear-gradient(90deg, rgba(var(--ym-gold-rgb), 0.62), rgba(var(--ym-accent-rgb), 0.46));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.22s ease;
}

.entry-card:hover::after {
  transform: scaleX(1);
}

.entry-head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.entry-icon {
  width: 48px;
  height: 48px;
  border-radius: 4px 12px 4px 12px;
  display: grid;
  place-items: center;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--ym-text);
  flex-shrink: 0;
}

.entry-title-wrap h3 {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.05rem;
  margin: 0;
}

.entry-target {
  display: inline-flex;
  margin-top: 4px;
  font-size: 0.74rem;
  color: var(--ym-text-muted);
}

.entry-card > p {
  margin: 0;
  color: var(--ym-text-secondary);
  font-size: 0.9rem;
  line-height: 1.75;
}

.entry-outcome {
  font-size: 0.82rem;
  color: var(--ym-text);
  padding: 8px 10px;
  border-radius: 4px 12px 4px 12px;
  border: 1px dashed rgba(var(--ym-gold-rgb), 0.42);
  background: rgba(var(--ym-gold-rgb), 0.08);
}

.entry-foot {
  margin-top: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.entry-foot > span:first-child {
  font-size: 0.78rem;
  color: var(--ym-text-muted);
}

.entry-cta {
  font-size: 0.84rem;
  color: var(--ym-ink-nong);
}

/* ── Value ── */
.value-section {
  background: rgba(255, 249, 240, 0.72);
  border-top: 1px solid var(--ym-border);
  border-bottom: 1px solid var(--ym-border);
  padding: 80px 0;
}

.value-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 26px;
  align-items: center;
}

.value-text .section-kicker {
  margin-bottom: 14px;
}

.value-text h2 {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(1.8rem, 3.6vw, 2.6rem);
  line-height: 1.25;
  margin-bottom: 18px;
  letter-spacing: 0.04em;
}

.value-text > p {
  color: var(--ym-text-secondary);
  line-height: 1.9;
  max-width: 520px;
  margin-bottom: 18px;
}

.mission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.mission-tags span {
  display: inline-flex;
  border-radius: 4px 12px 4px 12px;
  border: 1px solid var(--ym-border-strong);
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 0.78rem;
  color: var(--ym-text-secondary);
}

.value-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 12px;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  padding: 11px 22px;
  border: 1px solid var(--ym-border-strong);
  border-radius: 4px 12px 4px 12px;
  text-decoration: none;
  color: var(--ym-text);
  font-size: 0.92rem;
  transition: all 0.2s ease;
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.03em;
  background: rgba(255, 252, 246, 0.72);
}

.btn-outline:hover {
  border-color: rgba(58, 47, 40, 0.34);
  background: rgba(var(--ym-accent-rgb), 0.06);
  color: var(--ym-ink-jiao);
}

.value-board {
  display: block;
}

.community-board {
  border: 1px solid var(--ym-border);
  border-radius: 8px 26px 8px 26px;
  background: rgba(255, 249, 240, 0.88);
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.community-feed,
.community-publish {
  border: 1px solid var(--ym-border);
  border-radius: 6px 18px 8px 20px;
  background: rgba(255, 252, 246, 0.84);
  padding: 14px;
}

.community-head h3,
.publish-head h3 {
  font-family: var(--ym-font-calligraphy-ma);
  margin: 6px 0 4px;
  font-size: 1.1rem;
}

.community-entry {
  border: 1px solid var(--ym-border);
  border-radius: 6px 18px 8px 20px;
  background: linear-gradient(150deg, rgba(255, 252, 246, 0.92), rgba(245, 239, 230, 0.88));
  padding: 14px;
  display: grid;
  gap: 10px;
}

.entry-inline {
  width: min(360px, 100%);
  padding: 12px;
}

.entry-inline .publish-head {
  margin-bottom: 0;
}

.entry-inline .publish-head h3 {
  font-size: 0.98rem;
}

.entry-inline .publish-head p {
  font-size: 0.8rem;
  line-height: 1.62;
}

.open-publish-btn {
  justify-self: start;
}

.community-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.community-head p:last-child,
.publish-head p {
  margin: 0;
  font-size: 0.86rem;
  line-height: 1.68;
  color: var(--ym-text-secondary);
}

.community-tip {
  margin-top: 10px;
  color: var(--ym-text-muted);
  font-size: 0.86rem;
}

.community-tip.error {
  color: var(--ym-danger);
  background: var(--ym-danger-bg);
  border: 1px solid rgba(var(--ym-accent-rgb), 0.18);
  border-radius: 6px 14px 6px 14px;
  padding: 10px 12px;
}

.post-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.post-card {
  border: 1px solid var(--ym-border);
  border-radius: 5px 14px 5px 14px;
  background: rgba(255, 252, 246, 0.86);
  overflow: hidden;
  display: grid;
  grid-template-columns: 120px 1fr;
  min-height: 100px;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.post-body {
  padding: 9px 10px;
  display: grid;
  gap: 5px;
}

.post-meta {
  margin: 0;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--ym-text-muted);
  font-size: 0.74rem;
}

.post-body h4 {
  margin: 0;
  font-size: 0.94rem;
  color: var(--ym-text);
}

.post-body p:last-child {
  margin: 0;
  font-size: 0.84rem;
  line-height: 1.62;
  color: var(--ym-text-secondary);
}

.post-actions {
  display: flex;
  justify-content: flex-start;
}

.post-detail-btn {
  padding: 6px 12px;
  font-size: 0.8rem;
}

.publish-head {
  margin-bottom: 8px;
}

/* ── Responsive ── */
@media (max-width: 1020px) {
  .entries-layout {
    grid-template-columns: 1fr;
  }

  .entries-rail {
    position: static;
  }

  .entries-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .value-inner {
    grid-template-columns: 1fr;
  }

  .community-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .hero-bg {
    background-position: 66% center;
  }

  .hero-ink-poem {
    display: none;
  }

  .metrics-inner {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-card:nth-child(2) {
    border-right: none;
  }

  .metric-card:nth-child(1),
  .metric-card:nth-child(2) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
}

@media (max-width: 680px) {
  .hero-bg {
    background-position: 70% center;
  }

  .hero-title {
    font-size: 2.4rem;
  }

  .entries-grid {
    grid-template-columns: 1fr;
  }

  .entry-foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .metrics-inner {
    grid-template-columns: 1fr;
  }

  .metric-card {
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .metric-card:last-child {
    border-bottom: none;
  }

  .hero-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .value-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .entry-inline {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .post-card {
    grid-template-columns: 1fr;
  }

  .post-cover {
    aspect-ratio: 16/9;
    height: auto;
  }
}
</style>
