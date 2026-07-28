<template>
  <CeramicsPageShell
    kicker="SHOP CART"
    title="购物车"
    description="填写收货信息并提交订单，系统将弹出固定微信收款码，付款后由后台人工审核。"
    :sub-nav-items="shopNavItems"
    :alt="true"
  >
    <section class="cart-page">
      <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>

      <section v-if="!isLoggedIn" class="empty-panel">
        <h3>请先登录后查看购物车</h3>
        <p>登录后可以添加商品、提交订单并在个人中心查看审核与发货状态。</p>
        <router-link class="action-btn link-btn" :to="{ path: '/ceramics/user-login', query: { redirect: '/ceramics/shop/cart' } }">
          去登录
        </router-link>
      </section>

      <template v-else>
        <p v-if="loading" class="panel-tip">购物车加载中...</p>
        <section v-else class="cart-grid">
          <article class="cart-panel">
            <header class="section-head">
              <p class="section-kicker">CART ITEMS</p>
              <h3>待结算商品</h3>
            </header>
            <p v-if="cartItems.length === 0" class="panel-tip">购物车还是空的，先去商城挑选商品。</p>
            <div v-else class="cart-list">
              <article v-for="item in cartItems" :key="item.id" class="cart-item">
                <label class="select-wrap">
                  <input v-model="selectedIds" type="checkbox" :value="item.id" />
                </label>
                <img :src="resolveImage(item.coverUrl)" :alt="item.productName" class="item-cover" loading="lazy" />
                <div class="item-body">
                  <div class="item-summary">
                    <p class="item-code">{{ item.productCode }}</p>
                    <h4>{{ item.productName }}</h4>
                    <p class="item-subtitle">{{ item.productSubtitle || '柴烧文化衍生商品' }}</p>
                  </div>
                  <div class="item-inline">
                    <span>单价 ¥{{ formatPrice(item.price) }}</span>
                    <span>小计 ¥{{ formatPrice(item.subtotalAmount) }}</span>
                    <span>库存 {{ item.stock ?? 0 }}</span>
                  </div>
                  <div class="item-actions">
                    <div class="qty-actions">
                      <button type="button" class="secondary-btn small-btn" @click="changeQuantity(item, item.quantity - 1)">-</button>
                      <input
                        :value="item.quantity"
                        type="number"
                        min="1"
                        :max="Math.max(1, item.stock || 1)"
                        @change="onQuantityInput(item, $event)"
                      />
                      <button type="button" class="secondary-btn small-btn" @click="changeQuantity(item, item.quantity + 1)">+</button>
                    </div>
                    <button type="button" class="secondary-btn small-btn" @click="removeItem(item.id)">删除</button>
                  </div>
                </div>
              </article>
            </div>
          </article>

          <article class="checkout-panel">
            <header class="section-head">
              <p class="section-kicker">CHECKOUT</p>
              <h3>提交订单</h3>
            </header>
            <form class="checkout-form" @submit.prevent="submitOrder">
              <label>
                <span>收货人</span>
                <input v-model.trim="checkoutForm.receiverName" type="text" maxlength="40" placeholder="请输入收货人姓名" />
              </label>
              <label>
                <span>联系电话</span>
                <input v-model.trim="checkoutForm.receiverPhone" type="text" maxlength="11" placeholder="请输入11位手机号" />
              </label>
              <label>
                <span>收货地址</span>
                <textarea v-model.trim="checkoutForm.receiverAddress" rows="4" maxlength="200" placeholder="请输入详细收货地址"></textarea>
              </label>
              <label>
                <span>备注信息</span>
                <textarea v-model.trim="checkoutForm.buyerRemark" rows="3" maxlength="200" placeholder="选填，例如配送时间要求"></textarea>
              </label>
              <div class="summary-board">
                <p><span>已选商品</span><strong>{{ selectedCount }} 件</strong></p>
                <p><span>应付金额</span><strong>¥{{ formatPrice(selectedAmount) }}</strong></p>
              </div>
              <button type="submit" class="action-btn full-btn" :disabled="submittingOrder || selectedIds.length === 0 || cartItems.length === 0">
                {{ submittingOrder ? '提交中...' : '提交订单并去付款' }}
              </button>
            </form>
          </article>
        </section>
      </template>

      <ShopPaymentDialog
        :visible="paymentDialogVisible"
        :order="currentOrder"
        :qr-url="paymentQrUrl"
        :submitting="submittingPaid"
        @close="paymentDialogVisible = false"
        @confirm-paid="handlePaid"
      />
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import ShopPaymentDialog from '@/components/shop/ShopPaymentDialog.vue'
import { collectiblesAuthAPI, normalizeMediaUrl, shopAPI, SHOP_PAYMENT_QR_URL } from '@/utils/collectiblesApi'
import { shopNavItems } from './navs'

const isLoggedIn = computed(() => Boolean(collectiblesAuthAPI.getToken()))
const loading = ref(false)
const cartItems = ref([])
const selectedIds = ref([])
const feedbackMessage = ref('')
const feedbackOk = ref(true)
const submittingOrder = ref(false)
const paymentDialogVisible = ref(false)
const submittingPaid = ref(false)
const currentOrder = ref(null)
const paymentQrUrl = ref(SHOP_PAYMENT_QR_URL)

const checkoutForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  buyerRemark: ''
})

const selectedItems = computed(() => cartItems.value.filter((item) => selectedIds.value.includes(item.id)))
const selectedCount = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const selectedAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.subtotalAmount || 0), 0))

const resolveImage = (url) => normalizeMediaUrl(url) || '/vcg-kiln-vessels-row.webp'
const formatPrice = (value) => Number(value || 0).toFixed(2)

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const loadCart = async () => {
  if (!isLoggedIn.value) return
  loading.value = true
  try {
    const data = await shopAPI.getCart()
    cartItems.value = Array.isArray(data?.list) ? data.list : []
    selectedIds.value = cartItems.value.map((item) => item.id)
  } catch (error) {
    cartItems.value = []
    selectedIds.value = []
    setFeedback(error.message || '购物车加载失败', false)
  } finally {
    loading.value = false
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

const changeQuantity = async (item, nextQuantity) => {
  if (!item || nextQuantity < 1) return
  try {
    const data = await shopAPI.updateCartItem(item.id, { quantity: nextQuantity })
    cartItems.value = Array.isArray(data?.list) ? data.list : []
    selectedIds.value = selectedIds.value.filter((id) => cartItems.value.some((cartItem) => cartItem.id === id))
    if (selectedIds.value.length === 0) {
      selectedIds.value = cartItems.value.map((cartItem) => cartItem.id)
    }
  } catch (error) {
    setFeedback(error.message || '更新数量失败', false)
  }
}

const onQuantityInput = (item, event) => {
  const value = Number(event.target.value || item.quantity || 1)
  changeQuantity(item, value)
}

const removeItem = async (id) => {
  try {
    await shopAPI.deleteCartItem(id)
    setFeedback('购物车商品已删除')
    await loadCart()
  } catch (error) {
    setFeedback(error.message || '删除失败', false)
  }
}

const submitOrder = async () => {
  if (selectedIds.value.length === 0) {
    setFeedback('请先勾选要结算的商品', false)
    return
  }
  submittingOrder.value = true
  try {
    const order = await shopAPI.createOrder({
      cartItemIds: selectedIds.value,
      receiverName: checkoutForm.receiverName,
      receiverPhone: checkoutForm.receiverPhone,
      receiverAddress: checkoutForm.receiverAddress,
      buyerRemark: checkoutForm.buyerRemark
    })
    currentOrder.value = order
    paymentDialogVisible.value = true
    setFeedback('订单已创建，请完成扫码付款并点击“我已付款”')
    await loadCart()
  } catch (error) {
    setFeedback(error.message || '提交订单失败', false)
  } finally {
    submittingOrder.value = false
  }
}

const handlePaid = async () => {
  if (!currentOrder.value?.id) return
  submittingPaid.value = true
  try {
    currentOrder.value = await shopAPI.markOrderPaid(currentOrder.value.id)
    paymentDialogVisible.value = false
    setFeedback('已提交付款信息，等待管理员审核')
  } catch (error) {
    setFeedback(error.message || '提交付款状态失败', false)
  } finally {
    submittingPaid.value = false
  }
}

onMounted(() => {
  loadPaymentConfig()
  loadCart()
})
</script>

<style scoped>
.cart-page {
  display: grid;
  gap: 14px;
}

.cart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(340px, 0.75fr);
  gap: 14px;
}

.cart-panel,
.checkout-panel,
.empty-panel {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 249, 240, 0.9);
  padding: 16px;
  display: grid;
  gap: 12px;
}

.section-head h3,
.empty-panel h3,
.item-body h4,
.item-code,
.item-subtitle,
.section-kicker,
.panel-tip,
.feedback-tip,
.summary-board p {
  margin: 0;
}

.section-kicker,
.item-code {
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.section-head h3,
.empty-panel h3 {
  margin-top: 6px;
  font-family: var(--ym-font-calligraphy-ma);
}

.cart-list {
  display: grid;
  gap: 10px;
}

.cart-item {
  display: grid;
  grid-template-columns: auto 110px minmax(0, 1fr);
  gap: 12px;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 252, 246, 0.84);
  padding: 10px;
}

.select-wrap {
  display: flex;
  align-items: flex-start;
  padding-top: 8px;
}

.item-cover {
  width: 110px;
  height: 110px;
  object-fit: cover;
  border-radius: 8px 14px 8px 14px;
}

.item-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px 16px;
  min-width: 0;
  align-items: center;
}

.item-summary,
.item-inline {
  min-width: 0;
}

.item-summary {
  display: grid;
  gap: 4px;
}

.item-subtitle {
  margin-top: 4px;
  color: var(--ym-text-secondary);
  line-height: 1.7;
}

.item-inline,
.summary-board {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.item-inline {
  grid-column: 1 / -1;
}

.item-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: nowrap;
}

.qty-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.item-inline span {
  font-size: 0.84rem;
  color: var(--ym-text-secondary);
}

.qty-actions input,
.checkout-form input,
.checkout-form textarea {
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 18px 8px 18px;
  padding: 10px 12px;
  background: rgba(255, 252, 246, 0.9);
}

.qty-actions input {
  width: 76px;
  min-height: 42px;
  padding: 0 10px;
  text-align: center;
}

.qty-actions .small-btn {
  width: 42px;
  min-width: 42px;
  min-height: 42px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.item-actions > .small-btn {
  min-width: 68px;
  min-height: 42px;
}

.checkout-form {
  display: grid;
  gap: 12px;
}

.checkout-form label {
  display: grid;
  gap: 8px;
}

.checkout-form span {
  color: var(--ym-text-secondary);
  font-size: 0.88rem;
}

.checkout-form textarea {
  resize: vertical;
}

.summary-board {
  justify-content: space-between;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 252, 246, 0.88);
  padding: 12px 14px;
}

.summary-board strong {
  color: var(--ym-accent);
  margin-left: 8px;
}

.action-btn,
.secondary-btn,
.link-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  text-align: center;
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
.secondary-btn:hover,
.link-btn:hover {
  transform: translateY(-1px);
}

.action-btn:hover,
.link-btn:hover {
  background: rgba(var(--ym-accent-rgb), 0.18);
  border-color: rgba(58, 47, 40, 0.4);
}

.secondary-btn:hover {
  background: rgba(255, 249, 240, 0.98);
  border-color: rgba(58, 47, 40, 0.34);
}

.small-btn {
  padding: 8px 12px;
}

.full-btn {
  width: 100%;
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
  .cart-grid,
  .cart-item {
    grid-template-columns: 1fr;
  }

  .item-body {
    grid-template-columns: 1fr;
  }

  .item-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .item-cover {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }
}
</style>
