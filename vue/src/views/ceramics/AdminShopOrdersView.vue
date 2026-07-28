<template>
  <CeramicsPageShell
    kicker="ADMIN ORDERS"
    title="商城订单管理"
    description="管理员可审核用户付款、驳回异常订单，并在付款通过后录入物流信息。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <section class="orders-page">
      <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>

      <div class="orders-grid">
        <article class="list-panel">
          <header class="section-head">
            <p class="section-kicker">ORDER FILTER</p>
            <h3>订单列表</h3>
          </header>
          <form class="filter-row" @submit.prevent="loadOrders(1)">
            <input v-model.trim="filters.keyword" type="text" placeholder="搜索订单号 / 用户 / 收货人" />
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option value="PENDING_PAYMENT">待付款</option>
              <option value="PENDING_REVIEW">待审核</option>
              <option value="PAYMENT_REJECTED">审核未通过</option>
              <option value="PAID">待发货</option>
              <option value="SHIPPED">已发货</option>
              <option value="CANCELLED">已取消</option>
            </select>
            <button type="submit" class="secondary-btn">筛选</button>
          </form>
          <div class="order-list">
            <article
              v-for="order in orders"
              :key="order.id"
              class="order-card"
              :class="{ active: selectedOrder?.id === order.id }"
              @click="selectOrder(order.id)"
            >
              <div class="card-head">
                <strong>{{ order.orderNo }}</strong>
                <span :class="['status-tag', order.status?.toLowerCase()]">{{ order.statusLabel }}</span>
              </div>
              <p>用户：{{ order.displayName || order.username || '--' }}</p>
              <p>收货人：{{ order.receiverName }} / {{ order.receiverPhone }}</p>
              <p>金额：¥{{ formatPrice(order.totalAmount) }} · 件数 {{ order.totalQuantity }}</p>
              <p>时间：{{ formatDate(order.createdAt) }}</p>
            </article>
          </div>
          <div v-if="pagination.total > pagination.pageSize" class="pagination-row">
            <button type="button" class="secondary-btn" :disabled="pagination.page <= 1" @click="loadOrders(pagination.page - 1)">
              上一页
            </button>
            <span>第 {{ pagination.page }} / {{ totalPages }} 页</span>
            <button type="button" class="secondary-btn" :disabled="pagination.page >= totalPages" @click="loadOrders(pagination.page + 1)">
              下一页
            </button>
          </div>
        </article>

        <article class="detail-panel">
          <header class="section-head">
            <p class="section-kicker">ORDER DETAIL</p>
            <h3>订单详情</h3>
          </header>
          <p v-if="detailLoading" class="panel-tip">订单详情加载中...</p>
          <p v-else-if="!selectedOrder" class="panel-tip">请选择左侧订单查看详情。</p>
          <template v-else>
            <div class="detail-box">
              <p><strong>订单号：</strong>{{ selectedOrder.orderNo }}</p>
              <p><strong>状态：</strong>{{ selectedOrder.statusLabel }}</p>
              <p><strong>用户：</strong>{{ selectedOrder.displayName || selectedOrder.username || '--' }}</p>
              <p><strong>收货信息：</strong>{{ selectedOrder.receiverName }} / {{ selectedOrder.receiverPhone }}</p>
              <p><strong>收货地址：</strong>{{ selectedOrder.receiverAddress }}</p>
              <p><strong>买家备注：</strong>{{ selectedOrder.buyerRemark || '无' }}</p>
              <p><strong>付款提交：</strong>{{ formatDate(selectedOrder.paymentMarkedAt) }}</p>
              <p><strong>审核备注：</strong>{{ selectedOrder.paymentReviewRemark || '无' }}</p>
              <p><strong>物流信息：</strong>{{ selectedOrder.shippingCompany || '--' }} {{ selectedOrder.trackingNo || '' }}</p>
              <p><strong>总金额：</strong>¥{{ formatPrice(selectedOrder.totalAmount) }}</p>
            </div>

            <div class="items-box">
              <article v-for="item in selectedOrder.items || []" :key="item.id" class="detail-item">
                <img :src="resolveImage(item.productCoverUrl)" :alt="item.productName" loading="lazy" />
                <div>
                  <h4>{{ item.productName }}</h4>
                  <p>{{ item.productSubtitle || '商城订单商品' }}</p>
                  <p>单价 ¥{{ formatPrice(item.unitPrice) }} · 数量 {{ item.quantity }} · 小计 ¥{{ formatPrice(item.subtotalAmount) }}</p>
                </div>
              </article>
            </div>

            <div v-if="selectedOrder.status === 'PENDING_REVIEW'" class="review-box">
              <textarea v-model.trim="reviewRemark" rows="3" maxlength="200" placeholder="审核备注（选填）"></textarea>
              <div class="action-row">
                <button type="button" class="secondary-btn" @click="reviewOrder(false)">驳回付款</button>
                <button type="button" class="action-btn" @click="reviewOrder(true)">审核通过</button>
              </div>
            </div>

            <div v-if="selectedOrder.status === 'PAID'" class="ship-box">
              <input v-model.trim="shipForm.shippingCompany" type="text" maxlength="60" placeholder="物流公司" />
              <input v-model.trim="shipForm.trackingNo" type="text" maxlength="80" placeholder="物流单号" />
              <button type="button" class="action-btn" @click="shipOrder">标记已发货</button>
            </div>
          </template>
        </article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { adminShopAPI, normalizeMediaUrl } from '@/utils/collectiblesApi'
import { adminNavItems } from './adminNavItems'

const filters = reactive({ keyword: '', status: '' })
const shipForm = reactive({ shippingCompany: '', trackingNo: '' })
const reviewRemark = ref('')
const orders = ref([])
const selectedOrder = ref(null)
const detailLoading = ref(false)
const feedbackMessage = ref('')
const feedbackOk = ref(true)
const pagination = ref({ page: 1, pageSize: 10, total: 0 })

const totalPages = computed(() => Math.max(1, Math.ceil((pagination.value.total || 0) / (pagination.value.pageSize || 10))))

const resolveImage = (url) => normalizeMediaUrl(url) || '/vcg-flambe-vase-museum.webp'
const formatPrice = (value) => Number(value || 0).toFixed(2)
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

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const loadOrders = async (page = 1) => {
  try {
    const data = await adminShopAPI.getOrders({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page,
      pageSize: 10
    })
    orders.value = Array.isArray(data?.list) ? data.list : []
    pagination.value = data?.pagination || { page: 1, pageSize: 10, total: 0 }
    if (!selectedOrder.value && orders.value.length > 0) {
      selectOrder(orders.value[0].id)
    }
  } catch (error) {
    orders.value = []
    pagination.value = { page: 1, pageSize: 10, total: 0 }
    selectedOrder.value = null
    setFeedback(error.message || '订单列表加载失败', false)
  }
}

const selectOrder = async (id) => {
  detailLoading.value = true
  reviewRemark.value = ''
  shipForm.shippingCompany = ''
  shipForm.trackingNo = ''
  try {
    selectedOrder.value = await adminShopAPI.getOrderDetail(id)
  } catch (error) {
    selectedOrder.value = null
    setFeedback(error.message || '订单详情加载失败', false)
  } finally {
    detailLoading.value = false
  }
}

const reviewOrder = async (approved) => {
  if (!selectedOrder.value?.id) return
  try {
    selectedOrder.value = await adminShopAPI.reviewPayment(selectedOrder.value.id, {
      approved,
      remark: reviewRemark.value
    })
    setFeedback(approved ? '订单付款已审核通过' : '订单付款已驳回')
    await loadOrders(pagination.value.page)
  } catch (error) {
    setFeedback(error.message || '审核失败', false)
  }
}

const shipOrder = async () => {
  if (!selectedOrder.value?.id) return
  try {
    selectedOrder.value = await adminShopAPI.shipOrder(selectedOrder.value.id, {
      shippingCompany: shipForm.shippingCompany,
      trackingNo: shipForm.trackingNo
    })
    setFeedback('订单已标记发货')
    await loadOrders(pagination.value.page)
  } catch (error) {
    setFeedback(error.message || '发货失败', false)
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  display: grid;
  gap: 14px;
}

.orders-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.9fr) minmax(0, 1.1fr);
  gap: 14px;
  align-items: start;
}

.list-panel,
.detail-panel {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 255, 240, 0.92);
  padding: 16px;
  display: grid;
  gap: 12px;
  align-content: start;
}

.section-kicker,
.feedback-tip,
.panel-tip,
.order-card p,
.detail-box p,
.detail-item p {
  margin: 0;
}

.section-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.section-head h3,
.detail-item h4 {
  margin: 6px 0 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.pagination-row,
.action-row,
.ship-box {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 154px auto;
  gap: 10px;
  align-items: center;
}

.filter-row input,
.filter-row select,
.review-box textarea,
.ship-box input {
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 18px 8px 18px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.88);
}

.filter-row input {
  min-width: 0;
}

.filter-row select {
  width: 100%;
  min-width: 0;
}

.filter-row .secondary-btn {
  white-space: nowrap;
}

.order-list,
.detail-box,
.items-box {
  display: grid;
  gap: 10px;
}

.order-card {
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
  display: grid;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.order-card.active,
.order-card:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.42);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.status-tag {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.76rem;
  background: rgba(26, 26, 26, 0.08);
  color: var(--ym-text-muted);
}

.status-tag.pending_review {
  background: rgba(214, 138, 43, 0.14);
  color: #996118;
}

.status-tag.paid {
  background: rgba(46, 139, 87, 0.14);
  color: #1e7d50;
}

.status-tag.shipped {
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-accent);
}

.status-tag.payment_rejected {
  background: rgba(154, 63, 48, 0.12);
  color: var(--ym-danger);
}

.status-tag.cancelled {
  background: rgba(26, 26, 26, 0.08);
  color: var(--ym-text-muted);
}

.detail-box {
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
}

.detail-item {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 12px;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 255, 255, 0.82);
  padding: 10px;
}

.detail-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 8px 14px 8px 14px;
}

.review-box,
.ship-box {
  display: grid;
  gap: 10px;
}

.review-box textarea {
  resize: vertical;
}

.action-btn,
.secondary-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 10px 14px;
  cursor: pointer;
}

.action-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.5);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-accent);
}

.secondary-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.82);
  color: var(--ym-text);
}

.feedback-tip {
  border-radius: 8px 18px 8px 18px;
  padding: 12px 14px;
}

.feedback-tip.ok {
  background: rgba(46, 139, 87, 0.12);
  color: #1e7d50;
}

.feedback-tip.error {
  background: rgba(154, 63, 48, 0.12);
  color: var(--ym-danger);
}

@media (max-width: 1080px) {
  .orders-grid,
  .detail-item {
    grid-template-columns: 1fr;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .detail-item img {
    height: auto;
    aspect-ratio: 16 / 9;
  }
}
</style>
