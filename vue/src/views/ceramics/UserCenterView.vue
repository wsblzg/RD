<template>
  <CeramicsPageShell
    kicker="USER CENTER"
    title="用户中心"
    description="统一管理个人资料、数字藏品、商城订单与社区文章发布。"
    :alt="true"
  >
    <div class="user-center">
      <section class="center-hero">
        <div class="hero-main">
          <span class="avatar">{{ userInitial }}</span>
          <div class="hero-meta">
            <p class="hero-kicker">PERSONAL DASHBOARD</p>
            <h3>{{ userName }}</h3>
            <p>{{ isLoggedIn ? '已登录，可管理你的数字藏品与文章内容。' : '请先登录后使用个人中心能力。' }}</p>
          </div>
        </div>
        <div class="hero-stats">
          <article class="stat-card">
            <strong>{{ myCollections.length }}</strong>
            <span>我的藏品</span>
          </article>
          <article class="stat-card">
            <strong>{{ myOrders.length }}</strong>
            <span>我的订单</span>
          </article>
          <article class="stat-card">
            <strong>{{ myPosts.length }}</strong>
            <span>我的文章</span>
          </article>
          <article class="stat-card">
            <strong>{{ aiWorkCount }}</strong>
            <span>我的 AI 作品</span>
          </article>
          <article class="stat-card">
            <strong>{{ purchasedModels.length }}</strong>
            <span>已购 3D 模型</span>
          </article>
          <article class="stat-card">
            <strong>{{ pointsDisplay }}</strong>
            <span>我的积分</span>
          </article>
          <article class="stat-card">
            <strong>{{ isLoggedIn ? '在线' : '离线' }}</strong>
            <span>当前状态</span>
          </article>
        </div>
      </section>

      <section class="tab-section" aria-label="个人中心功能区">
        <div class="tab-list" role="tablist">
          <button
            v-for="item in tabItems"
            :key="item.key"
            type="button"
            class="tab-btn"
            :class="{ active: activeTab === item.key }"
            @click="switchTab(item.key)"
          >
            <span>{{ item.label }}</span>
            <small>{{ item.hint }}</small>
          </button>
        </div>
      </section>

      <section v-if="!isLoggedIn" class="panel login-panel">
        <h4>请先登录</h4>
        <p>登录后可查看“我的藏品 / 我的文章 / 我的 AI 作品”，并发布新的社区动态。</p>
        <p>商城下单后，订单审核和发货状态也会在这里统一查看。</p>
        <router-link class="login-link" :to="{ path: '/ceramics/user-login', query: { redirect: '/ceramics/user-center' } }">
          去登录
        </router-link>
      </section>

      <template v-else>
        <section v-if="activeTab === 'profile'" class="panel profile-panel">
          <header class="panel-head">
            <p class="panel-kicker">PROFILE</p>
            <h4>个人资料</h4>
          </header>
          <div class="profile-grid">
            <article class="profile-item">
              <span>昵称</span>
              <strong>{{ currentUser?.displayName || '--' }}</strong>
            </article>
            <article class="profile-item">
              <span>用户名</span>
              <strong>{{ currentUser?.username || '--' }}</strong>
            </article>
            <article class="profile-item">
              <span>账号角色</span>
              <strong>{{ currentUser?.role === 'admin' ? '管理员' : '普通用户' }}</strong>
            </article>
            <article class="profile-item">
              <span>创建时间</span>
              <strong>{{ formatDate(currentUser?.createdAt) }}</strong>
            </article>
            <article class="profile-item">
              <span>AI 3D 积分</span>
              <strong>{{ pointsDisplay }}</strong>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'points'" class="panel points-panel">
          <header class="panel-head">
            <p class="panel-kicker">POINTS</p>
            <h4>积分充值</h4>
            <p>积分用于生成 AI 3D 陶瓷作品；充值付款后由后台管理员审核到账。</p>
          </header>
          <div class="points-overview">
            <article>
              <span>当前积分</span>
              <strong>{{ pointsDisplay }}</strong>
            </article>
            <article>
              <span>生成消耗</span>
              <strong>{{ pointsSummary.ai3dCost || 10 }} / 次</strong>
            </article>
            <article>
              <span>永久保存</span>
              <strong>{{ pointsSummary.ai3dPersistCost || 10 }} / 件</strong>
            </article>
            <article>
              <span>充值换算</span>
              <strong>1 元 = {{ pointsSummary.rechargeRate || 10 }} 积分</strong>
            </article>
          </div>

          <form v-if="!pointsSummary.unlimited" class="points-recharge-form" @submit.prevent="createPointsRecharge">
            <label>
              <span>充值金额</span>
              <input v-model.number="rechargeAmount" type="number" min="1" max="9999" step="1" />
            </label>
            <p>预计到账 {{ rechargePointsPreview }} 积分</p>
            <button type="submit" class="ghost-btn" :disabled="pointsLoading">
              {{ pointsLoading ? '提交中...' : '生成充值单并付款' }}
            </button>
          </form>
          <p v-else class="panel-tip">管理员账号积分额度无限，无需充值。</p>

          <p v-if="pointsFeedbackMessage" :class="['order-feedback-tip', pointsFeedbackOk ? 'ok' : 'error']">{{ pointsFeedbackMessage }}</p>
          <p v-if="pointsLoading" class="panel-tip">积分数据加载中...</p>
          <div v-else class="order-list">
            <article v-for="item in pointRecharges" :key="item.id" class="order-card">
              <div class="order-head">
                <div>
                  <p class="order-no">{{ item.rechargeNo }}</p>
                  <h5>{{ item.statusLabel }}</h5>
                </div>
                <strong>+{{ item.pointsAmount }}</strong>
              </div>
              <div class="order-meta-grid">
                <p><span>金额</span><strong>¥{{ formatMoney(item.amount) }}</strong></p>
                <p><span>状态</span><strong>{{ item.statusLabel }}</strong></p>
                <p><span>提交付款</span><strong>{{ formatDate(item.paymentMarkedAt) }}</strong></p>
                <p><span>创建时间</span><strong>{{ formatDate(item.createdAt) }}</strong></p>
              </div>
              <p v-if="item.paymentReviewRemark" class="order-review-note">审核备注：{{ item.paymentReviewRemark }}</p>
              <div class="order-actions">
                <button
                  v-if="item.status === 'PENDING_PAYMENT'"
                  type="button"
                  class="ghost-btn"
                  @click="openRechargePaymentDialog(item)"
                >
                  继续付款
                </button>
                <button
                  v-if="item.status === 'PENDING_PAYMENT'"
                  type="button"
                  class="ghost-btn"
                  @click="markRechargePaid(item)"
                >
                  我已付款
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'collections'" class="panel collections-panel">
          <header class="panel-head">
            <p class="panel-kicker">MY COLLECTIONS</p>
            <h4>我的数字藏品</h4>
            <p>这里汇总你通过兑换码获得的数字藏品，不再和“数字藏品馆”页面混在一起。</p>
          </header>
          <p v-if="collectionsLoading" class="panel-tip">藏品数据加载中...</p>
          <p v-else-if="myCollections.length === 0" class="panel-tip">你还没有通过兑换码获得藏品。</p>
          <div v-else class="collections-grid">
            <article v-for="item in myCollections" :key="item.id" class="collection-card">
              <img :src="resolveCover(item)" :alt="item.name" loading="lazy" />
              <div class="collection-body">
                <h5>{{ item.name }}</h5>
                <p>{{ item.seriesName || '未分类系列' }} · 稀有度 Lv.{{ item.rarityLevel || 1 }}</p>
                <p class="collection-time">获取于 {{ formatDate(item.acquiredAt || item.createdAt) }}</p>
                <button type="button" class="ghost-btn" @click="goItemDetail(item.id)">查看详情</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'orders'" class="panel orders-panel">
          <header class="panel-head">
            <p class="panel-kicker">MY ORDERS</p>
            <h4>我的订单</h4>
            <p>集中查看文创商城订单、付款审核进度与物流信息。</p>
          </header>
          <p v-if="orderFeedbackMessage" :class="['order-feedback-tip', orderFeedbackOk ? 'ok' : 'error']">{{ orderFeedbackMessage }}</p>
          <p v-if="ordersLoading" class="panel-tip">订单数据加载中...</p>
          <p v-else-if="myOrders.length === 0" class="panel-tip">你还没有商城订单，可以先去文创商城选购。</p>
          <div v-else class="order-list">
            <article v-for="order in myOrders" :key="order.id" class="order-card">
              <div class="order-head">
                <div>
                  <p class="order-no">{{ order.orderNo }}</p>
                  <h5>{{ order.statusLabel }}</h5>
                </div>
                <strong>¥{{ formatMoney(order.totalAmount) }}</strong>
              </div>
              <div class="order-meta-grid">
                <p><span>件数</span><strong>{{ order.totalQuantity }}</strong></p>
                <p><span>收货人</span><strong>{{ order.receiverName }}</strong></p>
                <p><span>联系电话</span><strong>{{ order.receiverPhone }}</strong></p>
                <p><span>下单时间</span><strong>{{ formatDate(order.createdAt) }}</strong></p>
              </div>
              <p class="order-address">{{ order.receiverAddress }}</p>
              <p v-if="order.paymentReviewRemark" class="order-review-note">审核备注：{{ order.paymentReviewRemark }}</p>
              <p v-if="order.shippingCompany || order.trackingNo" class="order-review-note">
                物流信息：{{ order.shippingCompany || '--' }} {{ order.trackingNo || '' }}
              </p>
              <div class="order-actions">
                <button
                  v-if="order.status === 'PENDING_PAYMENT'"
                  type="button"
                  class="ghost-btn"
                  @click="openPaymentDialog(order)"
                >
                  继续付款
                </button>
                <button
                  v-if="order.status === 'PENDING_PAYMENT'"
                  type="button"
                  class="ghost-btn"
                  @click="markOrderPaid(order)"
                >
                  我已付款
                </button>
                <button
                  v-if="order.status === 'PENDING_PAYMENT'"
                  type="button"
                  class="ghost-btn danger-btn"
                  @click="cancelOrder(order)"
                >
                  取消订单
                </button>
                <button
                  v-if="order.status === 'SHIPPED'"
                  type="button"
                  class="ghost-btn"
                  @click="router.push('/ceramics/shop')"
                >
                  再去逛逛
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'posts'" class="panel posts-panel">
          <header class="panel-head">
            <p class="panel-kicker">MY POSTS</p>
            <h4>我的文章</h4>
            <p>仅展示你最近发布的前五条文章，用于快速管理。</p>
            <button type="button" class="ghost-btn publish-link-btn" @click="goCommunityPublish">去发布文章</button>
          </header>
          <p v-if="postsLoading" class="panel-tip">文章数据加载中...</p>
          <p v-else-if="myPosts.length === 0" class="panel-tip">你还没有发布文章，去“发布文章”写第一篇吧。</p>
          <div v-else class="post-list">
            <article v-for="post in myPosts" :key="post.id" class="post-card">
              <img v-if="post.coverImage" :src="resolveCommunityCover(post.coverImage)" :alt="post.title" class="post-cover" />
              <div class="post-body">
                <p class="post-meta">
                  <span>{{ formatDate(post.createdAt) }}</span>
                  <span>{{ (post.imageUrls || []).length }} 张图</span>
                </p>
                <h5>{{ post.title }}</h5>
                <p>{{ post.summary || plainText(post.contentHtml) }}</p>
                <div class="post-actions">
                  <button type="button" class="ghost-btn" @click="goPostDetail(post.id)">查看详情</button>
                  <button type="button" class="ghost-btn" @click="goEditPost(post.id)">编辑文章</button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'works'" class="panel posts-panel">
          <header class="panel-head">
            <p class="panel-kicker">MY AI WORKS</p>
            <h4>我的 AI 作品</h4>
            <p>生成结果会先保留约 24 小时；需要长期收藏或用于文章展示时，可选择永久保存。</p>
          </header>
          <p v-if="worksFeedbackMessage" :class="['order-feedback-tip', worksFeedbackOk ? 'ok' : 'error']">{{ worksFeedbackMessage }}</p>
          <p v-if="worksLoading" class="panel-tip">AI 作品加载中...</p>
          <p v-else-if="aiWorkCount === 0" class="panel-tip">你还没有生成过 AI 作品，先去创作页完成第一件作品吧。</p>
          <template v-else>
            <div class="work-group">
              <div class="work-group-head">
                <h5>临时作品</h5>
                <span>{{ temporaryWorks.length }} 件</span>
              </div>
              <p v-if="temporaryWorks.length === 0" class="panel-tip">当前没有仍在有效期内的临时作品。</p>
              <div v-else class="post-list">
                <article v-for="work in temporaryWorks" :key="work.id" class="post-card">
                  <img
                    :src="resolveAiWorkCover(work)"
                    :alt="work.title"
                    class="post-cover"
                    @error="handleAiWorkCoverError($event, work)"
                  />
                  <div class="post-body">
                    <p class="post-meta">
                      <span>{{ formatDate(work.generatedAt || work.createdAt) }}</span>
                      <span>{{ formatRemaining(work.expiresAt) }}</span>
                    </p>
                    <h5>{{ work.title }}</h5>
                    <p>{{ work.prompt || '这件作品尚未添加创作说明。' }}</p>
                    <div class="post-actions">
                      <button type="button" class="ghost-btn" @click="goAiWorkDetail(work.id)">查看作品</button>
                      <button
                        type="button"
                        class="ghost-btn"
                        :disabled="workSavingId === work.id || !work.canPersist"
                        @click="persistAiWork(work)"
                      >
                        {{ workSavingId === work.id ? '保存中...' : `永久保存 · ${work.persistPointsCost || pointsSummary.ai3dPersistCost || 10} 积分` }}
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </div>

            <div class="work-group">
              <div class="work-group-head">
                <h5>永久作品</h5>
                <span>{{ permanentWorks.length }} 件</span>
              </div>
              <p v-if="permanentWorks.length === 0" class="panel-tip">永久保存后，作品会在这里长期陈列。</p>
              <div v-else class="post-list">
                <article v-for="work in permanentWorks" :key="work.id" class="post-card">
                  <img
                    :src="resolveAiWorkCover(work)"
                    :alt="work.title"
                    class="post-cover"
                    @error="handleAiWorkCoverError($event, work)"
                  />
                  <div class="post-body">
                    <p class="post-meta">
                      <span>{{ formatDate(work.persistedAt || work.createdAt) }}</span>
                      <span>长期收藏</span>
                    </p>
                    <h5>{{ work.title }}</h5>
                    <p>{{ work.prompt || '这件作品尚未添加创作说明。' }}</p>
                    <div class="post-actions">
                      <button type="button" class="ghost-btn" @click="goAiWorkDetail(work.id)">查看作品</button>
                      <button type="button" class="ghost-btn" @click="goCommunityPublish(work.id)">写入文章</button>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </template>
        </section>

        <section v-if="activeTab === 'models'" class="panel posts-panel">
          <header class="panel-head">
            <p class="panel-kicker">PURCHASED 3D MODELS</p>
            <h4>已购买的 3D 模型</h4>
            <p>这里只展示后台管理员审核支付成功后的 GLB 模型商品；AI 自己生成的作品仍在“我的 AI 作品”里。</p>
          </header>
          <p v-if="modelsLoading" class="panel-tip">已购 3D 模型加载中...</p>
          <p v-else-if="purchasedModels.length === 0" class="panel-tip">
            暂无已解锁模型。购买 3D 模型后，请在订单中提交付款并等待管理员审核通过。
          </p>
          <div v-else class="model-purchase-grid">
            <article v-for="item in purchasedModels" :key="item.id" class="model-purchase-card">
              <div class="model-preview-row">
                <img
                  v-for="(url, index) in item.previewImageUrls.slice(0, 1)"
                  :key="`${item.id}-${index}`"
                  :src="normalizeMediaUrl(url)"
                  :alt="`${item.name} 正视图`"
                  loading="lazy"
                />
              </div>
              <div class="model-purchase-body">
                <p class="post-meta">
                  <span>{{ item.productCode }}</span>
                  <span>GLB 已解锁</span>
                </p>
                <h5>{{ item.name }}</h5>
                <p>{{ item.subtitle || '文创商城 3D 模型商品' }}</p>
                <div class="post-actions">
                  <button type="button" class="ghost-btn" @click="goShopProduct(item.id)">查看完整 3D 模型</button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>

      <ShopPaymentDialog
        :visible="paymentDialogVisible"
        :order="paymentOrder"
        :qr-url="paymentQrUrl"
        :submitting="paying"
        @close="paymentDialogVisible = false"
        @confirm-paid="confirmOrderPaid"
      />
      <ShopPaymentDialog
        :visible="rechargePaymentDialogVisible"
        :order="rechargePaymentOrder"
        :qr-url="paymentQrUrl"
        :submitting="recharging"
        kicker="POINTS PAYMENT"
        title="请扫码充值积分"
        amount-label="充值金额"
        order-no-label="充值单号"
        tip="完成付款后点击“我已付款”，充值单会进入管理员审核流程，审核通过后积分自动到账。"
        @close="rechargePaymentDialogVisible = false"
        @confirm-paid="confirmRechargePaid"
      />
    </div>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import ShopPaymentDialog from '@/components/shop/ShopPaymentDialog.vue'
import { collectiblesAPI, collectiblesAuthAPI, communityAPI, normalizeMediaUrl, normalizeMediaUrls, pointsAPI, SHOP_PAYMENT_QR_URL, shopAPI } from '@/utils/collectiblesApi'
import { listCeramicWorks, saveCeramicModel } from '@/utils/ceramicCreationApi'
import { resolveGeneratedCover } from '@/utils/ceramicGenerationSchedule'
import { buildModelProductView } from '@/utils/shopProductModel'

const route = useRoute()
const router = useRouter()
const validTabs = ['profile', 'points', 'collections', 'orders', 'posts', 'works', 'models']
const tabItems = [
  { key: 'profile', label: '个人资料', hint: '账号信息与状态' },
  { key: 'points', label: '积分充值', hint: 'AI 3D 生成额度' },
  { key: 'collections', label: '我的藏品', hint: '查看已获取的数字藏品' },
  { key: 'orders', label: '我的订单', hint: '商城订单与物流状态' },
  { key: 'posts', label: '我的文章', hint: '我发布的社区文章' },
  { key: 'works', label: '我的 AI 作品', hint: '我保存的 3D 作品' },
  { key: 'models', label: '已购 3D 模型', hint: '商城购买并审核通过' }
]

const activeTab = ref('profile')
const currentUser = ref(null)
const collectionsLoading = ref(false)
const ordersLoading = ref(false)
const postsLoading = ref(false)
const worksLoading = ref(false)
const modelsLoading = ref(false)
const pointsLoading = ref(false)
const myCollections = ref([])
const myOrders = ref([])
const myPosts = ref([])
const temporaryWorks = ref([])
const permanentWorks = ref([])
const purchasedModels = ref([])
const pointsSummary = ref({ balance: 0, displayBalance: '0', totalRecharged: 0, totalSpent: 0, unlimited: false, ai3dCost: 10, ai3dPersistCost: 10, rechargeRate: 10 })
const pointRecharges = ref([])
const rechargeAmount = ref(10)
const paymentDialogVisible = ref(false)
const paymentOrder = ref(null)
const rechargePaymentDialogVisible = ref(false)
const rechargePaymentOrder = ref(null)
const paying = ref(false)
const recharging = ref(false)
const paymentQrUrl = ref(SHOP_PAYMENT_QR_URL)
const orderFeedbackMessage = ref('')
const orderFeedbackOk = ref(true)
const pointsFeedbackMessage = ref('')
const pointsFeedbackOk = ref(true)
const worksFeedbackMessage = ref('')
const worksFeedbackOk = ref(true)
const workSavingId = ref(null)

const isLoggedIn = computed(() => Boolean(collectiblesAuthAPI.getToken()) && Boolean(currentUser.value?.id))
const userName = computed(() => currentUser.value?.displayName || currentUser.value?.username || '访客用户')
const userInitial = computed(() => String(userName.value || '访').slice(0, 1))
const pointsDisplay = computed(() => {
  if (!isLoggedIn.value) return '--'
  if (pointsSummary.value?.unlimited || currentUser.value?.pointsUnlimited) return '无限'
  return String(pointsSummary.value?.displayBalance || currentUser.value?.pointsBalance || 0)
})
const rechargePointsPreview = computed(() => {
  const amount = Math.max(0, Number(rechargeAmount.value || 0))
  const rate = Number(pointsSummary.value?.rechargeRate || 10)
  return Math.floor(amount * rate)
})
const aiWorkCount = computed(() => temporaryWorks.value.length + permanentWorks.value.length)

const syncTabFromRoute = () => {
  const routeTab = String(route.query.tab || 'profile')
  activeTab.value = validTabs.includes(routeTab) ? routeTab : 'profile'
}

const switchTab = (tab) => {
  if (!validTabs.includes(tab)) return
  activeTab.value = tab
  router.replace({ path: '/ceramics/user-center', query: { tab } })
}

const setOrderFeedback = (message, ok = true) => {
  orderFeedbackMessage.value = message
  orderFeedbackOk.value = ok
}

const setPointsFeedback = (message, ok = true) => {
  pointsFeedbackMessage.value = message
  pointsFeedbackOk.value = ok
}

const setWorksFeedback = (message, ok = true) => {
  worksFeedbackMessage.value = message
  worksFeedbackOk.value = ok
}

const readStoredUser = () => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

const plainText = (html) => {
  const plain = String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return plain.length > 120 ? `${plain.slice(0, 120)}...` : plain
}

const formatMoney = (value) => Number(value || 0).toFixed(2)

const formatDate = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

const formatRemaining = (value) => {
  const expiresAt = new Date(value).getTime()
  if (!Number.isFinite(expiresAt)) return '有效期未知'
  const remaining = expiresAt - Date.now()
  if (remaining <= 0) return '已过期'
  const hours = Math.floor(remaining / (60 * 60 * 1000))
  const minutes = Math.max(1, Math.ceil((remaining % (60 * 60 * 1000)) / (60 * 1000)))
  return hours > 0 ? `剩余 ${hours} 小时 ${minutes} 分钟` : `剩余 ${minutes} 分钟`
}

const resolveCover = (item) => {
  const raw = String(item?.coverUrl || '').trim()
  if (!raw || raw.toLowerCase().endsWith('.glb')) {
    return '/vcg-flambe-vase-museum.webp'
  }
  const normalized = normalizeMediaUrl(raw)
  if (normalized) {
    return normalized
  }
  return `/${raw.replace(/^\/+/, '')}`
}

const resolveCommunityCover = (url) => normalizeMediaUrl(url)
const resolveAiWorkCover = (work) => normalizeMediaUrl(resolveGeneratedCover(work))
const handleAiWorkCoverError = (event, work) => {
  const fallbackUrl = normalizeMediaUrl(work?.coverUrl) || '/青花梅瓶.webp'
  if (event.currentTarget?.src && !event.currentTarget.src.endsWith(fallbackUrl)) {
    event.currentTarget.src = fallbackUrl
  }
}

const normalizeCommunityPost = (post = {}) => ({
  ...post,
  coverImage: normalizeMediaUrl(post.coverImage),
  imageUrls: normalizeMediaUrls(post.imageUrls)
})

const goItemDetail = (id) => {
  if (!id) return
  router.push(`/collections/item/${id}`)
}

const goCommunityPublish = (aiWorkId = null) => {
  router.push({
    path: '/community/publish',
    query: aiWorkId ? { aiWorkId } : {}
  })
}

const goPostDetail = (id) => {
  if (!id) return
  router.push(`/ceramics/community/post/${id}`)
}

const goEditPost = (id) => {
  if (!id) return
  router.push({ path: '/ceramics/community/publish', query: { postId: id } })
}

const goAiWorkDetail = (id) => {
  if (!id) return
  router.push({ path: '/ceramics/ai-creation', query: { workId: id } })
}

const goShopProduct = (id) => {
  if (!id) return
  router.push(`/ceramics/shop/product/${id}`)
}

const loadCurrentUser = async () => {
  if (!collectiblesAuthAPI.getToken()) {
    currentUser.value = null
    return
  }
  try {
    const user = await collectiblesAuthAPI.me()
    currentUser.value = user
    localStorage.setItem('yc_user', JSON.stringify(user))
  } catch (error) {
    collectiblesAuthAPI.logout()
    currentUser.value = null
    localStorage.removeItem('yc_user')
  }
}

const loadMyCollections = async () => {
  if (!isLoggedIn.value) {
    myCollections.value = []
    return
  }
  collectionsLoading.value = true
  try {
    const data = await collectiblesAPI.getMyFavorites({ page: 1, pageSize: 100 })
    myCollections.value = Array.isArray(data?.list) ? data.list : []
  } catch (error) {
    myCollections.value = []
  } finally {
    collectionsLoading.value = false
  }
}

const loadMyPosts = async () => {
  if (!isLoggedIn.value) {
    myPosts.value = []
    return
  }
  postsLoading.value = true
  try {
    const data = await communityAPI.getMyPosts(5)
    myPosts.value = Array.isArray(data)
      ? data.slice(0, 5).map((post) => normalizeCommunityPost(post))
      : []
  } catch (error) {
    myPosts.value = []
  } finally {
    postsLoading.value = false
  }
}

const loadMyWorks = async () => {
  if (!isLoggedIn.value) {
    temporaryWorks.value = []
    permanentWorks.value = []
    return
  }
  worksLoading.value = true
  try {
    const [temporary, permanent] = await Promise.all([
      listCeramicWorks('temporary', 50),
      listCeramicWorks('permanent', 50)
    ])
    temporaryWorks.value = Array.isArray(temporary) ? temporary : []
    permanentWorks.value = Array.isArray(permanent) ? permanent : []
  } catch (error) {
    temporaryWorks.value = []
    permanentWorks.value = []
    setWorksFeedback(error.message || '作品加载失败', false)
  } finally {
    worksLoading.value = false
  }
}

const persistAiWork = async (work) => {
  if (!work?.taskId || workSavingId.value) return
  const cost = work.persistPointsCost || pointsSummary.value.ai3dPersistCost || 10
  if (!window.confirm(`永久保存将消耗 ${cost} 积分，确认继续吗？`)) return
  workSavingId.value = work.id
  try {
    const saved = await saveCeramicModel(work.taskId, {
      title: work.title,
      prompt: work.prompt,
      style: work.style,
      vessel: work.vessel
    })
    if (saved?.points) pointsSummary.value = saved.points
    setWorksFeedback('作品已永久保存，可用于文章展示')
    await Promise.all([loadMyWorks(), loadPointsSummary()])
  } catch (error) {
    setWorksFeedback(error.message || '作品永久保存失败，请稍后重试', false)
  } finally {
    workSavingId.value = null
  }
}

const loadMyOrders = async () => {
  if (!isLoggedIn.value) {
    myOrders.value = []
    return
  }
  ordersLoading.value = true
  try {
    const data = await shopAPI.getMyOrders({ page: 1, pageSize: 10 })
    myOrders.value = Array.isArray(data?.list) ? data.list : []
  } catch (error) {
    myOrders.value = []
    setOrderFeedback(error.message || '订单加载失败', false)
  } finally {
    ordersLoading.value = false
  }
}

const loadPurchasedModels = async () => {
  if (!isLoggedIn.value) {
    purchasedModels.value = []
    return
  }
  modelsLoading.value = true
  try {
    const data = await shopAPI.getPurchasedModels()
    purchasedModels.value = Array.isArray(data?.list) ? data.list.map((item) => buildModelProductView(item)) : []
  } catch (error) {
    purchasedModels.value = []
  } finally {
    modelsLoading.value = false
  }
}

const loadPointsSummary = async () => {
  if (!isLoggedIn.value) {
    pointsSummary.value = { balance: 0, displayBalance: '0', totalRecharged: 0, totalSpent: 0, unlimited: false, ai3dCost: 10, ai3dPersistCost: 10, rechargeRate: 10 }
    return
  }
  try {
    const data = await pointsAPI.getSummary()
    pointsSummary.value = data || pointsSummary.value
    paymentQrUrl.value = data?.paymentQrUrl || paymentQrUrl.value
  } catch (error) {
    setPointsFeedback(error.message || '积分信息加载失败', false)
  }
}

const loadPointRecharges = async () => {
  if (!isLoggedIn.value) {
    pointRecharges.value = []
    return
  }
  pointsLoading.value = true
  try {
    const data = await pointsAPI.getRecharges({ page: 1, pageSize: 20 })
    pointRecharges.value = Array.isArray(data?.list) ? data.list : []
  } catch (error) {
    pointRecharges.value = []
    setPointsFeedback(error.message || '积分充值记录加载失败', false)
  } finally {
    pointsLoading.value = false
  }
}

const loadPaymentConfig = async () => {
  try {
    const data = await shopAPI.getPaymentConfig()
    paymentQrUrl.value = data?.qrUrl || SHOP_PAYMENT_QR_URL
  } catch (error) {
    paymentQrUrl.value = SHOP_PAYMENT_QR_URL
  }
}

const createPointsRecharge = async () => {
  if (!isLoggedIn.value) return
  pointsLoading.value = true
  try {
    const order = await pointsAPI.createRecharge({ amount: Number(rechargeAmount.value || 0) })
    rechargePaymentOrder.value = order
    rechargePaymentDialogVisible.value = true
    setPointsFeedback('充值单已创建，请扫码付款后提交审核')
    await loadPointRecharges()
  } catch (error) {
    setPointsFeedback(error.message || '创建充值单失败', false)
  } finally {
    pointsLoading.value = false
  }
}

const openPaymentDialog = (order) => {
  paymentOrder.value = order
  paymentDialogVisible.value = true
}

const openRechargePaymentDialog = (order) => {
  rechargePaymentOrder.value = order
  rechargePaymentDialogVisible.value = true
}

const markOrderPaid = async (order) => {
  if (!order?.id) return
  paying.value = true
  try {
    await shopAPI.markOrderPaid(order.id)
    paymentDialogVisible.value = false
    paymentOrder.value = null
    setOrderFeedback('已提交付款信息，等待管理员审核')
    await loadMyOrders()
  } catch (error) {
    setOrderFeedback(error.message || '提交付款状态失败', false)
  } finally {
    paying.value = false
  }
}

const cancelOrder = async (order) => {
  if (!order?.id) return
  if (!window.confirm(`确认取消订单 ${order.orderNo || ''} 吗？取消后会释放库存。`)) return
  try {
    await shopAPI.cancelOrder(order.id)
    if (paymentOrder.value?.id === order.id) {
      paymentDialogVisible.value = false
      paymentOrder.value = null
    }
    setOrderFeedback('订单已取消，库存已释放')
    await loadMyOrders()
  } catch (error) {
    setOrderFeedback(error.message || '取消订单失败', false)
  }
}

const confirmOrderPaid = async () => {
  if (!paymentOrder.value?.id) return
  await markOrderPaid(paymentOrder.value)
}

const markRechargePaid = async (order) => {
  if (!order?.id) return
  recharging.value = true
  try {
    await pointsAPI.markRechargePaid(order.id)
    rechargePaymentDialogVisible.value = false
    rechargePaymentOrder.value = null
    setPointsFeedback('已提交付款信息，等待管理员审核到账')
    await Promise.all([loadPointsSummary(), loadPointRecharges()])
  } catch (error) {
    setPointsFeedback(error.message || '提交充值付款状态失败', false)
  } finally {
    recharging.value = false
  }
}

const confirmRechargePaid = async () => {
  if (!rechargePaymentOrder.value?.id) return
  await markRechargePaid(rechargePaymentOrder.value)
}

const refreshUserCenter = async () => {
  await loadCurrentUser()
  await Promise.all([loadPaymentConfig(), loadPointsSummary(), loadPointRecharges(), loadMyCollections(), loadMyOrders(), loadMyPosts(), loadMyWorks(), loadPurchasedModels()])
}

watch(
  () => route.query.tab,
  () => syncTabFromRoute(),
  { immediate: true }
)

onMounted(() => {
  currentUser.value = collectiblesAuthAPI.getToken() ? readStoredUser() : null
  refreshUserCenter()
})
</script>

<style scoped>
.user-center {
  display: grid;
  gap: 14px;
}

.center-hero {
  border: 1px solid var(--ym-border);
  border-radius: 8px 24px 8px 24px;
  background: linear-gradient(150deg, rgba(255, 255, 240, 0.95), rgba(250, 240, 230, 0.86));
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
}

.hero-main {
  display: flex;
  gap: 12px;
  align-items: center;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.45);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-accent);
  font-family: var(--ym-font-seal);
  display: grid;
  place-items: center;
  font-size: 1.3rem;
}

.hero-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  color: var(--ym-text-muted);
}

.hero-meta h3 {
  margin: 4px 0 6px;
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.04em;
}

.hero-meta p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.7;
  font-size: 0.9rem;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-card {
  min-width: 96px;
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 255, 0.8);
  padding: 10px;
  display: grid;
  gap: 2px;
}

.stat-card strong {
  color: var(--ym-accent);
  font-family: var(--ym-font-calligraphy-ma);
}

.stat-card span {
  color: var(--ym-text-muted);
  font-size: 0.78rem;
}

.tab-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.tab-btn {
  border: 1px solid var(--ym-border-strong);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 240, 0.88);
  color: var(--ym-text);
  padding: 10px;
  text-align: left;
  display: grid;
  gap: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn span {
  font-size: 0.92rem;
}

.tab-btn small {
  font-size: 0.74rem;
  color: var(--ym-text-muted);
}

.tab-btn.active,
.tab-btn:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.5);
  background: rgba(var(--ym-accent-rgb), 0.1);
}

.panel {
  border: 1px solid var(--ym-border);
  border-radius: 8px 22px 8px 22px;
  background: rgba(255, 255, 240, 0.9);
  padding: 16px;
  display: grid;
  gap: 10px;
}

.panel-head h4 {
  margin: 6px 0 4px;
  font-family: var(--ym-font-calligraphy-ma);
}

.panel-head p {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.7;
  font-size: 0.88rem;
}

.panel-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

.panel-tip {
  margin: 0;
  color: var(--ym-text-muted);
}

.login-panel h4 {
  margin: 0;
}

.login-link {
  justify-self: start;
  text-decoration: none;
  color: var(--ym-accent);
  border-bottom: 1px solid rgba(var(--ym-accent-rgb), 0.3);
}

.login-link:hover {
  border-bottom-color: rgba(var(--ym-accent-rgb), 0.7);
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.profile-item {
  border: 1px solid var(--ym-border);
  border-radius: 6px 12px 6px 12px;
  background: rgba(255, 255, 255, 0.8);
  padding: 10px;
  display: grid;
  gap: 4px;
}

.profile-item span {
  color: var(--ym-text-muted);
  font-size: 0.8rem;
}

.points-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.points-overview article,
.points-recharge-form {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 255, 0.8);
  padding: 10px;
}

.points-overview article {
  display: grid;
  gap: 4px;
}

.points-overview span,
.points-recharge-form span {
  color: var(--ym-text-muted);
  font-size: 0.8rem;
}

.points-overview strong {
  color: var(--ym-accent);
  font-family: var(--ym-font-calligraphy-ma);
}

.points-recharge-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: end;
}

.points-recharge-form label {
  display: grid;
  gap: 6px;
}

.points-recharge-form input {
  border: 1px solid var(--ym-border);
  border-radius: 4px 10px 4px 10px;
  background: rgba(255, 252, 246, 0.86);
  padding: 8px 10px;
}

.points-recharge-form p {
  margin: 0;
  color: var(--ym-text-secondary);
}

.collections-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.collection-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 16px 6px 16px;
  background: rgba(255, 255, 255, 0.86);
  overflow: hidden;
}

.collection-card img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.collection-body {
  padding: 10px;
  display: grid;
  gap: 6px;
}

.collection-body h5 {
  margin: 0;
  font-size: 0.94rem;
}

.collection-body p {
  margin: 0;
  color: var(--ym-text-secondary);
  font-size: 0.84rem;
  line-height: 1.65;
}

.collection-time {
  color: var(--ym-text-muted);
}

.ghost-btn {
  border: 1px solid var(--ym-border-strong);
  border-radius: 4px 10px 4px 10px;
  background: rgba(255, 255, 240, 0.86);
  color: var(--ym-text);
  padding: 6px 10px;
  cursor: pointer;
}

.ghost-btn:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.5);
  color: var(--ym-accent);
}

.ghost-btn:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.ghost-btn.danger-btn {
  color: var(--ym-danger);
}

.order-feedback-tip {
  margin: 0;
  border-radius: 6px 14px 6px 14px;
  padding: 10px 12px;
  font-size: 0.86rem;
}

.order-feedback-tip.ok {
  background: rgba(46, 139, 87, 0.12);
  color: #1e7d50;
}

.order-feedback-tip.error {
  background: rgba(154, 63, 48, 0.12);
  color: var(--ym-danger);
}

.order-list {
  display: grid;
  gap: 8px;
}

.order-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 255, 0.84);
  padding: 12px;
  display: grid;
  gap: 8px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.order-head h5,
.order-no,
.order-address,
.order-review-note {
  margin: 0;
}

.order-no {
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  color: var(--ym-text-muted);
}

.order-head h5 {
  margin-top: 4px;
  font-size: 0.98rem;
}

.order-head strong {
  color: var(--ym-accent);
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.08rem;
}

.order-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.order-meta-grid p {
  margin: 0;
  border: 1px solid var(--ym-border);
  border-radius: 6px 12px 6px 12px;
  background: rgba(255, 255, 240, 0.72);
  padding: 8px 10px;
  display: grid;
  gap: 4px;
}

.order-meta-grid span {
  color: var(--ym-text-muted);
  font-size: 0.76rem;
}

.order-address,
.order-review-note {
  color: var(--ym-text-secondary);
  line-height: 1.7;
  font-size: 0.84rem;
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.post-list {
  display: grid;
  gap: 8px;
}

.work-group {
  display: grid;
  gap: 8px;
}

.work-group + .work-group {
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--ym-border);
}

.work-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.work-group-head h5,
.work-group-head span {
  margin: 0;
}

.work-group-head span {
  color: var(--ym-text-muted);
  font-size: 0.8rem;
}

.post-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  background: rgba(255, 255, 255, 0.84);
  display: grid;
  grid-template-columns: 140px 1fr;
  overflow: hidden;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-body {
  padding: 10px;
  display: grid;
  gap: 5px;
}

.post-meta {
  margin: 0;
  display: flex;
  justify-content: space-between;
  font-size: 0.74rem;
  color: var(--ym-text-muted);
}

.post-body h5 {
  margin: 0;
  font-size: 0.94rem;
}

.post-body p:last-child {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.7;
  font-size: 0.84rem;
}

.post-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.model-purchase-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.model-purchase-card {
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 255, 255, 0.84);
  overflow: hidden;
  display: grid;
  gap: 0;
}

.model-preview-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1px;
  background: var(--ym-border);
}

.model-preview-row img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
  background: rgba(255, 255, 255, 0.9);
}

.model-purchase-body {
  padding: 10px;
  display: grid;
  gap: 6px;
}

.model-purchase-body h5,
.model-purchase-body p {
  margin: 0;
}

.model-purchase-body p {
  color: var(--ym-text-secondary);
  line-height: 1.65;
  font-size: 0.84rem;
}

.publish-link-btn {
  margin-top: 6px;
}

button:focus-visible,
input:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

@media (max-width: 980px) {
  .center-hero,
  .hero-stats,
  .tab-list,
  .profile-grid,
  .points-overview,
  .points-recharge-form,
  .collections-grid,
  .order-meta-grid,
  .model-purchase-grid {
    grid-template-columns: 1fr;
  }

  .post-card {
    grid-template-columns: 1fr;
  }

  .post-cover {
    aspect-ratio: 16 / 9;
    height: auto;
  }
}
</style>
