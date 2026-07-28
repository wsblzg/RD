<template>
  <CeramicsPageShell
    kicker="SHOP"
    title="文创商城"
    description="围绕柴烧文化衍生商品构建轻量商城：可浏览、可加入购物车、可下单、可审核。"
    :sub-nav-items="shopNavItems"
    :alt="true"
  >
    <section class="shop-page">
      <section class="toolbar">
        <form class="search-box" @submit.prevent="loadProducts(1)">
          <input v-model="keyword" type="text" placeholder="搜索商品名称或编码" />
          <button type="submit" class="action-btn">搜索</button>
        </form>
        <button type="button" class="secondary-btn cart-btn" @click="router.push('/ceramics/shop/cart')">
          进入购物车
        </button>
      </section>

      <section class="shop-sections" aria-label="商城板块">
        <button
          v-for="item in sectionItems"
          :key="item.key"
          type="button"
          class="section-card"
          :class="{ active: activeSection === item.key }"
          @click="switchSection(item.key)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.hint }}</small>
        </button>
      </section>

      <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>
      <p v-if="loading" class="panel-tip">商品加载中...</p>
      <p v-else-if="products.length === 0" class="panel-tip">当前没有可购买的商品。</p>

      <section v-else class="products-grid">
        <article v-for="product in products" :key="product.id" class="product-card" :class="{ 'model-card': product.isModelProduct }">
          <button type="button" class="cover-wrap" @click="openDetail(product.id)">
            <img :src="resolveImage(product.coverUrl)" :alt="product.name" loading="lazy" />
          <span v-if="product.isModelProduct" class="model-lock-badge">正视图预览 · 购买解锁 GLB</span>
          </button>
          <div class="product-body">
            <div class="product-copy">
              <p class="product-code">{{ product.productCode }}</p>
              <h3>{{ product.name }}</h3>
              <p class="product-subtitle">{{ product.subtitle || '柴烧文化衍生文创商品' }}</p>
            </div>
              <div v-if="product.isModelProduct" class="preview-strip" aria-label="3D 模型正视图">
              <span
                v-for="(url, index) in product.previewImageUrls"
                :key="`${product.id}-preview-${index}`"
                class="preview-thumb"
              >
                <img :src="resolveImage(url)" :alt="`${product.name} 正视图`" loading="lazy" />
              </span>
            </div>
            <div class="product-meta">
              <strong>¥{{ formatPrice(product.price) }}</strong>
              <span>{{ product.isModelProduct ? '数字库存' : '库存' }} {{ product.stock ?? 0 }}</span>
            </div>
            <div class="product-actions">
              <button type="button" class="secondary-btn" @click="openDetail(product.id)">查看详情</button>
              <button type="button" class="action-btn" @click="addToCart(product)">加入购物车</button>
            </div>
          </div>
        </article>
      </section>

      <div v-if="pagination.total > pagination.pageSize" class="pagination-row">
        <button type="button" class="secondary-btn" :disabled="pagination.page <= 1" @click="loadProducts(pagination.page - 1)">
          上一页
        </button>
        <span>第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <button
          type="button"
          class="secondary-btn"
          :disabled="pagination.page >= totalPages"
          @click="loadProducts(pagination.page + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { collectiblesAuthAPI, normalizeMediaUrl, shopAPI } from '@/utils/collectiblesApi'
import { buildModelProductView } from '@/utils/shopProductModel'
import { shopNavItems } from './navs'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const products = ref([])
const pagination = ref({ page: 1, pageSize: 12, total: 0 })
const feedbackMessage = ref('')
const feedbackOk = ref(true)
const activeSection = ref('physical')

const sectionItems = [
  { key: 'physical', label: 'SECTION 01', title: '文创商品', hint: '实体商品、文创周边、线下配送' },
  { key: 'model', label: 'SECTION 02', title: '3D 模型', hint: '未购买仅看正视图，审核通过后解锁 GLB' }
]

const totalPages = computed(() => Math.max(1, Math.ceil((pagination.value.total || 0) / (pagination.value.pageSize || 12))))

const resolveImage = (url) => normalizeMediaUrl(url) || '/vcg-kiln-vessels-row.webp'

const formatPrice = (value) => Number(value || 0).toFixed(2)

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const loadProducts = async (page = 1) => {
  loading.value = true
  try {
    const data = await shopAPI.getProducts({
      keyword: keyword.value.trim() || undefined,
      productType: activeSection.value,
      page,
      pageSize: 12
    })
    products.value = Array.isArray(data?.list) ? data.list.map((item) => buildModelProductView(item)) : []
    pagination.value = data?.pagination || { page: 1, pageSize: 12, total: 0 }
  } catch (error) {
    products.value = []
    pagination.value = { page: 1, pageSize: 12, total: 0 }
    setFeedback(error.message || '商品加载失败', false)
  } finally {
    loading.value = false
  }
}

const switchSection = (section) => {
  if (!section || section === activeSection.value) return
  activeSection.value = section
  feedbackMessage.value = ''
  loadProducts(1)
}

const openDetail = (id) => {
  router.push(`/ceramics/shop/product/${id}`)
}

const addToCart = async (product) => {
  try {
    await shopAPI.addCartItem({ productId: product.id, quantity: 1 })
    setFeedback(`已将“${product.name}”加入购物车`)
  } catch (error) {
    if ((error.message || '').includes('请先登录')) {
      router.push({ path: '/ceramics/user-login', query: { redirect: '/ceramics/shop' } })
      return
    }
    setFeedback(error.message || '加入购物车失败', false)
  }
}

onMounted(() => {
  if (!collectiblesAuthAPI.getToken()) {
    setFeedback('未登录也可以浏览商品；加入购物车和下单时会要求登录。')
  }
  loadProducts()
})
</script>

<style scoped>
.shop-page {
  display: grid;
  gap: 14px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px;
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 249, 240, 0.78);
}

.shop-sections {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.section-card {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 249, 240, 0.86);
  padding: 16px;
  text-align: left;
  display: grid;
  gap: 6px;
  cursor: pointer;
  color: var(--ym-text);
  transition: all 0.2s ease;
}

.section-card span {
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

.section-card strong {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.2rem;
}

.section-card small {
  color: var(--ym-text-secondary);
  line-height: 1.6;
}

.section-card.active,
.section-card:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.45);
  background: linear-gradient(145deg, rgba(var(--ym-accent-rgb), 0.11), rgba(255, 249, 240, 0.94));
  transform: translateY(-1px);
}

.search-box {
  flex: 1;
  min-width: min(420px, 100%);
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-box input {
  flex: 1;
  min-width: 220px;
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 18px 8px 18px;
  padding: 12px 14px;
  background: rgba(255, 252, 246, 0.9);
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.product-card {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 249, 240, 0.9);
  overflow: hidden;
  display: grid;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.product-card:hover {
  transform: translateY(-1px);
  border-color: rgba(58, 47, 40, 0.3);
  box-shadow: 0 12px 22px rgba(32, 25, 21, 0.08);
}

.cover-wrap {
  position: relative;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.cover-wrap img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.model-lock-badge {
  position: absolute;
  left: 10px;
  bottom: 10px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  background: rgba(32, 25, 21, 0.72);
  color: #fff9ed;
  padding: 5px 10px;
  font-size: 0.74rem;
  backdrop-filter: blur(8px);
}

.model-card {
  background: linear-gradient(160deg, rgba(255, 249, 240, 0.94), rgba(var(--ym-gold-rgb), 0.1));
}

.product-body {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.product-code {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  color: var(--ym-text-muted);
}

.product-copy h3,
.product-subtitle,
.product-meta strong,
.product-meta span,
.panel-tip,
.feedback-tip {
  margin: 0;
}

.product-copy h3 {
  margin-top: 6px;
  font-family: var(--ym-font-calligraphy-ma);
}

.product-subtitle {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  line-height: 1.75;
  min-height: 48px;
}

.preview-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.preview-thumb {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.72);
}

.preview-thumb img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
}

.product-meta strong {
  color: var(--ym-accent);
  font-size: 1.2rem;
}

.product-meta span {
  color: var(--ym-text-muted);
  font-size: 0.84rem;
}

.product-actions,
.pagination-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.pagination-row {
  justify-content: center;
}

.action-btn,
.secondary-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--ym-font-ui);
}

.action-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.44);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-ink-jiao);
}

.secondary-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 252, 246, 0.88);
  color: var(--ym-text);
}

.action-btn:hover,
.secondary-btn:hover {
  transform: translateY(-1px);
}

.action-btn:hover {
  background: rgba(var(--ym-accent-rgb), 0.18);
  border-color: rgba(58, 47, 40, 0.4);
}

.secondary-btn:hover {
  background: rgba(255, 249, 240, 0.98);
  border-color: rgba(58, 47, 40, 0.34);
}

.feedback-tip {
  border-radius: 8px 18px 8px 18px;
  padding: 12px 14px;
}

.feedback-tip.ok {
  background: var(--ym-success-bg);
  color: var(--ym-success);
}

.feedback-tip.error {
  background: var(--ym-danger-bg);
  color: var(--ym-danger);
}

@media (max-width: 980px) {
  .products-grid,
  .shop-sections {
    grid-template-columns: 1fr;
  }
}
</style>
