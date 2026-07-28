<template>
  <CeramicsPageShell
    kicker="ADMIN POINTS"
    title="积分充值审核"
    description="管理员审核用户充值付款，审核通过后自动发放积分。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <section class="points-page">
      <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>

      <form class="filter-row" @submit.prevent="loadRecharges(1)">
        <input v-model.trim="filters.keyword" type="text" placeholder="搜索充值单号 / 用户" />
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="PENDING_PAYMENT">待付款</option>
          <option value="PENDING_REVIEW">待审核</option>
          <option value="APPROVED">已到账</option>
          <option value="REJECTED">审核未通过</option>
        </select>
        <button type="submit" class="secondary-btn">筛选</button>
      </form>

      <div class="recharge-list">
        <article v-for="item in recharges" :key="item.id" class="recharge-card">
          <div class="card-head">
            <div>
              <p class="section-kicker">{{ item.rechargeNo }}</p>
              <h3>{{ item.displayName || item.username || '用户' }}</h3>
            </div>
            <span :class="['status-tag', item.status?.toLowerCase()]">{{ item.statusLabel }}</span>
          </div>
          <div class="meta-grid">
            <p><span>金额</span><strong>¥{{ formatMoney(item.amount) }}</strong></p>
            <p><span>到账积分</span><strong>{{ item.pointsAmount }}</strong></p>
            <p><span>提交付款</span><strong>{{ formatDate(item.paymentMarkedAt) }}</strong></p>
            <p><span>创建时间</span><strong>{{ formatDate(item.createdAt) }}</strong></p>
          </div>
          <p v-if="item.paymentReviewRemark" class="remark">审核备注：{{ item.paymentReviewRemark }}</p>
          <div v-if="item.status === 'PENDING_REVIEW'" class="review-box">
            <textarea v-model.trim="reviewRemark[item.id]" rows="2" maxlength="200" placeholder="审核备注（选填）"></textarea>
            <div class="action-row">
              <button type="button" class="secondary-btn" @click="reviewRecharge(item, false)">驳回</button>
              <button type="button" class="action-btn" @click="reviewRecharge(item, true)">审核通过并发放积分</button>
            </div>
          </div>
        </article>
      </div>

      <p v-if="!loading && recharges.length === 0" class="panel-tip">暂无充值单。</p>
      <p v-if="loading" class="panel-tip">充值单加载中...</p>

      <div v-if="pagination.total > pagination.pageSize" class="pagination-row">
        <button type="button" class="secondary-btn" :disabled="pagination.page <= 1" @click="loadRecharges(pagination.page - 1)">
          上一页
        </button>
        <span>第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <button type="button" class="secondary-btn" :disabled="pagination.page >= totalPages" @click="loadRecharges(pagination.page + 1)">
          下一页
        </button>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { adminPointsAPI } from '@/utils/collectiblesApi'
import { adminNavItems } from './adminNavItems'

const filters = reactive({ keyword: '', status: '' })
const recharges = ref([])
const pagination = ref({ page: 1, pageSize: 10, total: 0 })
const reviewRemark = ref({})
const loading = ref(false)
const feedbackMessage = ref('')
const feedbackOk = ref(true)

const totalPages = computed(() => Math.max(1, Math.ceil((pagination.value.total || 0) / (pagination.value.pageSize || 10))))

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

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const loadRecharges = async (page = 1) => {
  loading.value = true
  try {
    const data = await adminPointsAPI.getRecharges({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page,
      pageSize: 10
    })
    recharges.value = Array.isArray(data?.list) ? data.list : []
    pagination.value = data?.pagination || { page: 1, pageSize: 10, total: 0 }
  } catch (error) {
    recharges.value = []
    pagination.value = { page: 1, pageSize: 10, total: 0 }
    setFeedback(error.message || '充值单加载失败', false)
  } finally {
    loading.value = false
  }
}

const reviewRecharge = async (item, approved) => {
  try {
    await adminPointsAPI.reviewRecharge(item.id, {
      approved,
      remark: reviewRemark.value[item.id] || ''
    })
    setFeedback(approved ? '积分已发放到账' : '充值已驳回')
    reviewRemark.value[item.id] = ''
    await loadRecharges(pagination.value.page)
  } catch (error) {
    setFeedback(error.message || '审核失败', false)
  }
}

onMounted(() => {
  loadRecharges()
})
</script>

<style scoped>
.points-page {
  display: grid;
  gap: 14px;
}

.filter-row,
.recharge-card {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 255, 240, 0.92);
  box-sizing: border-box;
}

.filter-row {
  padding: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px auto;
  gap: 10px;
}

.filter-row input,
.filter-row select,
.review-box textarea {
  width: 100%;
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  padding: 10px 12px;
  background: rgba(255, 252, 246, 0.9);
  color: var(--ym-text);
  box-sizing: border-box;
}

.recharge-list {
  display: grid;
  gap: 12px;
}

.recharge-card {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.card-head,
.action-row,
.pagination-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.section-kicker {
  margin: 0 0 4px;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.card-head h3 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.meta-grid p {
  margin: 0;
  border: 1px solid var(--ym-border);
  border-radius: 8px 16px 8px 16px;
  padding: 10px;
  display: grid;
  gap: 4px;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.64);
}

.meta-grid strong {
  color: var(--ym-text);
}

.status-tag {
  border: 1px solid var(--ym-border);
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--ym-text-secondary);
  background: rgba(255, 252, 246, 0.72);
}

.status-tag.pending_review,
.status-tag.approved {
  color: var(--ym-accent);
}

.remark,
.panel-tip,
.feedback-tip {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.feedback-tip.ok {
  color: #2f7a4f;
}

.feedback-tip.error {
  color: #b23b31;
}

.review-box {
  display: grid;
  gap: 10px;
}

.action-btn,
.secondary-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 10px 14px;
  cursor: pointer;
  box-sizing: border-box;
  font-family: var(--ym-font-ui);
}

.action-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.44);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-ink-jiao);
}

.secondary-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 252, 246, 0.84);
  color: var(--ym-text);
}

.secondary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 760px) {
  .filter-row,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
