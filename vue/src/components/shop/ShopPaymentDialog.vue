<template>
  <transition name="dialog-fade">
    <div v-if="visible" class="payment-dialog-mask" @click.self="$emit('close')">
      <div class="payment-dialog">
        <header class="dialog-head">
          <div>
            <p class="dialog-kicker">{{ kicker }}</p>
            <h3>{{ title }}</h3>
          </div>
          <button type="button" class="close-btn" @click="$emit('close')">关闭</button>
        </header>

        <div class="dialog-body">
          <div class="qr-panel">
            <img :src="qrUrl" alt="微信收款码" class="qr-image" />
          </div>
          <div class="meta-panel">
            <p class="amount-line">{{ amountLabel }}</p>
            <strong class="amount-value">¥{{ formatPrice(displayAmount) }}</strong>
            <p class="order-line">{{ orderNoLabel }}：{{ orderNoValue }}</p>
            <p class="tip-line">{{ tip }}</p>
            <div class="dialog-actions">
              <button type="button" class="secondary-btn" @click="$emit('close')">稍后再付</button>
              <button type="button" class="primary-btn" :disabled="submitting" @click="$emit('confirm-paid')">
                {{ submitting ? submittingLabel : confirmLabel }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  order: { type: Object, default: null },
  qrUrl: { type: String, default: '/picket.webp' },
  submitting: { type: Boolean, default: false },
  kicker: { type: String, default: 'WECHAT PAYMENT' },
  title: { type: String, default: '请扫码付款' },
  amountLabel: { type: String, default: '应付金额' },
  orderNoLabel: { type: String, default: '订单号' },
  tip: {
    type: String,
    default: '完成付款后点击“我已付款”，订单会进入管理员审核流程，审核通过后再发货。'
  },
  confirmLabel: { type: String, default: '我已付款' },
  submittingLabel: { type: String, default: '提交中...' }
})

defineEmits(['close', 'confirm-paid'])

const formatPrice = (value) => {
  const amount = Number(value || 0)
  return amount.toFixed(2)
}

const orderNoValue = computed(() => {
  return props.order?.orderNo || props.order?.rechargeNo || '--'
})

const displayAmount = computed(() => {
  return props.order?.totalAmount ?? props.order?.amount ?? 0
})
</script>

<style scoped>
.payment-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  background: rgba(18, 18, 18, 0.56);
  display: grid;
  place-items: center;
  padding: 20px;
}

.payment-dialog {
  width: min(760px, 100%);
  border: 1px solid rgba(58, 47, 40, 0.24);
  border-radius: 12px 28px 12px 28px;
  background: linear-gradient(160deg, rgba(255, 249, 240, 0.98), rgba(245, 239, 230, 0.96));
  box-shadow: 0 24px 50px rgba(0, 0, 0, 0.22);
  overflow: hidden;
}

.dialog-head {
  padding: 18px 20px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.dialog-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

.dialog-head h3 {
  margin: 6px 0 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.close-btn,
.secondary-btn,
.primary-btn {
  border-radius: 6px 12px 6px 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--ym-font-ui);
}

.close-btn,
.secondary-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 252, 246, 0.84);
  color: var(--ym-text);
}

.primary-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.44);
  background: rgba(var(--ym-accent-rgb), 0.12);
  color: var(--ym-ink-jiao);
}

.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.dialog-body {
  padding: 8px 20px 20px;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
}

.qr-panel {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 252, 246, 0.88);
  display: grid;
  place-items: center;
  padding: 18px;
}

.qr-image {
  width: 100%;
  max-width: 220px;
  aspect-ratio: 1;
  object-fit: contain;
  border-radius: 12px;
  border: 1px solid rgba(26, 26, 26, 0.12);
  background: #fff;
}

.meta-panel {
  display: grid;
  gap: 10px;
  align-content: start;
}

.amount-line,
.order-line,
.tip-line {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.amount-value {
  font-size: 2rem;
  color: var(--ym-accent);
  font-family: var(--ym-font-calligraphy-ma);
}

.dialog-actions {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.close-btn:hover,
.secondary-btn:hover,
.primary-btn:hover {
  transform: translateY(-1px);
}

.close-btn:hover,
.secondary-btn:hover {
  background: rgba(255, 249, 240, 0.98);
  border-color: rgba(58, 47, 40, 0.34);
}

.primary-btn:hover {
  background: rgba(var(--ym-accent-rgb), 0.18);
  border-color: rgba(58, 47, 40, 0.4);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

@media (max-width: 720px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }
}
</style>
