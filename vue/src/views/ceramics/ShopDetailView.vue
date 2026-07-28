<template>
  <CeramicsPageShell
    kicker="SHOP DETAIL"
    title="商品详情"
    description="查看商品信息、库存与购买说明，支持直接加入购物车。"
    :sub-nav-items="shopNavItems"
    :alt="true"
  >
    <section class="detail-page">
      <p v-if="loading" class="panel-tip">商品详情加载中...</p>
      <p v-else-if="errorMessage" class="feedback-tip error">{{ errorMessage }}</p>

      <div v-else-if="product" class="detail-grid">
        <article v-if="!product.isModelProduct" class="cover-panel">
          <img :src="resolveImage(product.coverUrl)" :alt="product.name" loading="lazy" />
        </article>

        <article v-else class="model-panel">
          <header class="model-panel-head">
            <p>3D MODEL</p>
            <strong>{{ product.canViewFullModel ? '完整 GLB 已解锁' : '未购买只展示正视图' }}</strong>
          </header>
          <model-viewer
            v-if="product.canViewFullModel && product.modelUrl && modelViewerReady"
            ref="shopModelViewerRef"
            class="model-viewer"
            :src="resolveModelUrl(product.modelUrl)"
            camera-controls
            auto-rotate
            ar
            reveal="manual"
            shadow-intensity="0.8"
            exposure="1"
            @load="modelLoadState = 'ready'"
            @error="modelLoadState = 'error'"
          ></model-viewer>
          <div v-else class="locked-preview">
            <div class="three-view-grid">
              <figure v-for="(url, index) in previewImages" :key="`${product.id}-view-${index}`">
                <img :src="resolveImage(url)" :alt="`${product.name} 正视图`" loading="lazy" />
                <figcaption>正视图</figcaption>
              </figure>
            </div>
            <p class="lock-note">
              完整 GLB 模型将在购买并由后台管理员审核支付成功后解锁；提交“我已付款”后仍需等待审核。
            </p>
          </div>
          <p v-if="product.canViewFullModel && modelLoadState === 'loading'" class="panel-tip">完整模型加载中...</p>
          <p v-if="modelLoadState === 'error'" class="feedback-tip error">完整模型加载失败，请检查 GLB 地址或稍后重试。</p>
        </article>

        <article class="meta-panel">
          <p class="product-code">{{ product.productCode }}</p>
          <h3>{{ product.name }}</h3>
          <p class="product-subtitle">{{ product.subtitle || '柴烧文化衍生商品' }}</p>
          <p v-if="product.isModelProduct" :class="['unlock-badge', product.canViewFullModel ? 'unlocked' : 'locked']">
            {{ product.canViewFullModel ? '已购买：可查看完整 3D 模型' : '未解锁：购买后需管理员审核支付成功' }}
          </p>
          <div class="price-line">
            <strong>¥{{ formatPrice(product.price) }}</strong>
            <span>{{ product.isModelProduct ? '数字库存' : '库存' }} {{ product.stock ?? 0 }}</span>
          </div>
          <div class="quantity-row">
            <label for="shop-qty">购买数量</label>
            <input id="shop-qty" v-model.number="quantity" type="number" min="1" :max="Math.max(1, product.stock || 1)" />
          </div>
          <div class="action-row">
            <button type="button" class="secondary-btn" @click="router.push('/ceramics/shop')">返回商城</button>
            <button type="button" class="action-btn" @click="addToCart">加入购物车</button>
            <button type="button" class="action-btn alt" @click="goCart">去购物车</button>
          </div>
          <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>
        </article>
      </div>

      <section v-if="product" class="detail-content">
        <header class="section-head">
          <p class="section-kicker">DETAIL CONTENT</p>
          <h4>商品说明</h4>
        </header>
        <p>{{ product.detailContent || '当前商品暂未填写更详细的图文说明。' }}</p>
      </section>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { normalizeMediaUrl, shopAPI } from '@/utils/collectiblesApi'
import { ensureModelViewer } from '@/utils/modelViewerLoader'
import { buildModelProductView } from '@/utils/shopProductModel'
import { shopNavItems } from './navs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const product = ref(null)
const errorMessage = ref('')
const feedbackMessage = ref('')
const feedbackOk = ref(true)
const quantity = ref(1)
const modelViewerReady = ref(false)
const modelLoadState = ref('idle')
const shopModelViewerRef = ref(null)

const resolveImage = (url) => normalizeMediaUrl(url) || '/vcg-flambe-vase-museum.webp'
const resolveModelUrl = (url) => normalizeMediaUrl(url)
const formatPrice = (value) => Number(value || 0).toFixed(2)
const previewImages = computed(() => product.value?.previewImageUrls?.length ? product.value.previewImageUrls : [product.value?.coverUrl].filter(Boolean))

const loadDetail = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    product.value = buildModelProductView(await shopAPI.getProductDetail(route.params.id))
    quantity.value = 1
    modelLoadState.value = product.value?.canViewFullModel && product.value?.modelUrl ? 'loading' : 'idle'
    if (product.value?.canViewFullModel && product.value?.modelUrl) {
      await prepareModelViewer()
    }
  } catch (error) {
    product.value = null
    errorMessage.value = error.message || '商品详情加载失败'
  } finally {
    loading.value = false
  }
}

const prepareModelViewer = async () => {
  try {
    modelViewerReady.value = await ensureModelViewer()
    await nextTick()
    const viewer = shopModelViewerRef.value
    if (viewer?.dismissPoster) {
      viewer.dismissPoster()
    }
  } catch (error) {
    modelLoadState.value = 'error'
  }
}

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const addToCart = async () => {
  if (!product.value) return
  try {
    await shopAPI.addCartItem({
      productId: product.value.id,
      quantity: Number(quantity.value || 1)
    })
    setFeedback(`已将“${product.value.name}”加入购物车`)
  } catch (error) {
    if ((error.message || '').includes('请先登录')) {
      router.push({ path: '/ceramics/user-login', query: { redirect: route.fullPath } })
      return
    }
    setFeedback(error.message || '加入购物车失败', false)
  }
}

const goCart = () => {
  router.push('/ceramics/shop/cart')
}

watch(() => route.params.id, () => loadDetail())
onMounted(() => loadDetail())
</script>

<style scoped>
.detail-page {
  display: grid;
  gap: 14px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: 14px;
}

.cover-panel,
.model-panel,
.meta-panel,
.detail-content {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 249, 240, 0.9);
  overflow: hidden;
}

.cover-panel img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.model-panel {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.model-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.model-panel-head p,
.model-panel-head strong,
.lock-note {
  margin: 0;
}

.model-panel-head p {
  font-size: 0.74rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

.model-panel-head strong {
  color: var(--ym-accent);
}

.model-viewer {
  width: 100%;
  min-height: 460px;
  aspect-ratio: 4 / 3;
  border: 1px solid var(--ym-border);
  border-radius: 10px 22px 10px 22px;
  background: radial-gradient(circle at 50% 32%, rgba(255, 252, 246, 0.96), rgba(230, 213, 190, 0.36));
}

.locked-preview {
  display: grid;
  gap: 12px;
}

.three-view-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.three-view-grid figure {
  margin: 0;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.78);
}

.three-view-grid img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.three-view-grid figcaption {
  padding: 8px 10px;
  color: var(--ym-text-muted);
  font-size: 0.78rem;
}

.lock-note {
  border: 1px dashed rgba(var(--ym-accent-rgb), 0.34);
  border-radius: 8px 18px 8px 18px;
  background: rgba(var(--ym-accent-rgb), 0.08);
  padding: 12px 14px;
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.meta-panel,
.detail-content {
  padding: 18px;
  display: grid;
  gap: 10px;
}

.product-code,
.product-subtitle,
.feedback-tip,
.section-kicker,
.detail-content p {
  margin: 0;
}

.product-code {
  font-size: 0.74rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.meta-panel h3 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.product-subtitle,
.detail-content p {
  color: var(--ym-text-secondary);
  line-height: 1.8;
}

.unlock-badge {
  margin: 0;
  justify-self: start;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 0.82rem;
}

.unlock-badge.locked {
  border: 1px solid rgba(var(--ym-gold-rgb), 0.34);
  background: rgba(var(--ym-gold-rgb), 0.12);
  color: var(--ym-ink-nong);
}

.unlock-badge.unlocked {
  border: 1px solid rgba(46, 139, 87, 0.28);
  background: rgba(46, 139, 87, 0.12);
  color: #1e7d50;
}

.price-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.price-line strong {
  color: var(--ym-accent);
  font-size: 1.9rem;
}

.price-line span {
  color: var(--ym-text-muted);
}

.quantity-row {
  display: grid;
  gap: 8px;
}

.quantity-row input {
  width: 140px;
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 18px 8px 18px;
  padding: 10px 12px;
  background: rgba(255, 252, 246, 0.9);
}

.action-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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

.action-btn.alt {
  background: rgba(var(--ym-gold-rgb), 0.12);
  border-color: rgba(var(--ym-gold-rgb), 0.32);
  color: var(--ym-ink-nong);
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

.action-btn.alt:hover {
  background: rgba(var(--ym-gold-rgb), 0.18);
  border-color: rgba(var(--ym-gold-rgb), 0.42);
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

.section-head h4 {
  margin: 6px 0 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.section-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .three-view-grid {
    grid-template-columns: 1fr;
  }
}
</style>
