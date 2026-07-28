<template>
  <CeramicsPageShell
    kicker="ADMIN FEATURES"
    title="新增功能管理"
    description="集中进入本次新增能力的后台维护入口，包括 AI 作品、社区内容、数字藏品、商城商品、订单审核与积分审核。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <section class="admin-features-page">
      <section v-if="!isAdmin" class="feature-panel access-panel">
        <p class="section-kicker">ADMIN ACCESS</p>
        <h3>{{ isLoggedIn ? '当前账号没有后台权限' : '请先登录管理员账号' }}</h3>
        <p>{{ isLoggedIn ? '该页面仅管理员可访问。' : '登录管理员账号后可进入新增功能管理。' }}</p>
        <div class="panel-actions">
          <button type="button" class="secondary-btn" @click="goAdminLogin">
            {{ isLoggedIn ? '切换管理员登录' : '去登录' }}
          </button>
        </div>
      </section>

      <template v-else>
        <section class="feature-panel overview-panel">
          <div>
            <p class="section-kicker">FEATURES</p>
            <h3>新增能力总览</h3>
            <p>集中管理近期上线的核心能力，管理员可从本页快速进入对应模块，完成内容维护、审核与运营配置。</p>
          </div>
          <div class="stats-row">
            <span>6 个入口</span>
            <span>统一后台导航</span>
            <span>统一维护流程</span>
          </div>
        </section>

        <section class="feature-grid" aria-label="新增功能管理入口">
          <article v-for="item in featureItems" :key="item.to" class="feature-card">
            <div class="card-copy">
              <p class="section-kicker">{{ item.kicker }}</p>
              <h3>{{ item.title }}</h3>
              <p>{{ item.desc }}</p>
            </div>
            <button type="button" class="action-btn" @click="goRoute(item.to)">进入管理</button>
          </article>
        </section>
        <p v-if="loading" class="panel-tip">新增功能配置加载中...</p>
        <p v-if="feedbackMessage" class="panel-tip">{{ feedbackMessage }}</p>
      </template>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { adminFeaturesAPI } from '@/utils/collectiblesApi'
import { adminNavItems } from './adminNavItems'

const router = useRouter()
const currentUser = ref(null)

const readStoredUser = () => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

currentUser.value = readStoredUser()

const isLoggedIn = computed(() => Boolean(currentUser.value?.id))
const isAdmin = computed(() => currentUser.value?.role === 'admin')

const defaultFeatureItems = [
  {
    kicker: 'AI STUDY',
    title: '窑火造物',
    desc: '查看前台 AI 生成、作品展示、用户发布与作品挂载相关页面。',
    to: '/ceramics/ai-creation'
  },
  {
    kicker: 'COLLECTIONS',
    title: '数字藏品与 GLB',
    desc: '维护藏品上新、上下架、GLB 上传、封面与兑换码。',
    to: '/ceramics/admin/collectibles'
  },
  {
    kicker: 'COMMUNITY',
    title: '社区文章',
    desc: '集中搜索、查看、编辑和删除用户发布的社区内容。',
    to: '/ceramics/admin/community'
  },
  {
    kicker: 'SHOP',
    title: '商城商品',
    desc: '维护文创商品和 3D 模型商品的价格、库存、封面与上下架。',
    to: '/ceramics/admin/shop/products'
  },
  {
    kicker: 'ORDERS',
    title: '订单审核',
    desc: '审核用户付款状态，付款通过后录入物流发货信息。',
    to: '/ceramics/admin/shop/orders'
  },
  {
    kicker: 'POINTS',
    title: '积分充值审核',
    desc: '审核用户积分充值付款，审核通过后自动发放积分。',
    to: '/ceramics/admin/points'
  }
]

const featureItems = ref(defaultFeatureItems)
const loading = ref(false)
const feedbackMessage = ref('')

const goRoute = (path) => {
  router.push(path)
}

const goAdminLogin = () => {
  router.push({
    path: '/ceramics/user-login',
    query: { mode: 'admin', redirect: '/ceramics/admin/features' }
  })
}

const loadFeatures = async () => {
  if (!isAdmin.value) return
  loading.value = true
  feedbackMessage.value = ''
  try {
    const data = await adminFeaturesAPI.getFeatures()
    if (Array.isArray(data) && data.length > 0) {
      featureItems.value = data
    }
  } catch (error) {
    feedbackMessage.value = '新增功能入口加载失败，已启用默认管理入口。'
    featureItems.value = defaultFeatureItems
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFeatures()
})
</script>

<style scoped>
.admin-features-page {
  display: grid;
  gap: 14px;
}

.feature-panel,
.feature-card {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 255, 240, 0.92);
  box-sizing: border-box;
}

.feature-panel {
  padding: 18px;
  display: grid;
  gap: 12px;
}

.overview-panel {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.feature-card {
  min-height: 220px;
  padding: 18px;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  gap: 16px;
}

.card-copy {
  display: grid;
  gap: 8px;
  align-content: start;
}

.section-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.feature-panel h3,
.feature-card h3 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.feature-panel p,
.feature-card p,
.panel-tip {
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.panel-tip {
  margin: 0;
}

.stats-row,
.panel-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stats-row span {
  border: 1px solid var(--ym-border);
  border-radius: 999px;
  padding: 8px 12px;
  color: var(--ym-text-secondary);
  background: rgba(255, 255, 255, 0.52);
  white-space: nowrap;
}

.action-btn,
.secondary-btn {
  width: 100%;
  min-height: 42px;
  border-radius: 6px 14px 6px 14px;
  padding: 10px 14px;
  cursor: pointer;
  box-sizing: border-box;
}

.action-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.5);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-accent);
}

.secondary-btn {
  max-width: 180px;
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.82);
  color: var(--ym-text);
}

@media (max-width: 1080px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
