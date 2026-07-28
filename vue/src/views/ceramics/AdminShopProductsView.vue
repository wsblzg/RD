<template>
  <CeramicsPageShell
    kicker="ADMIN SHOP"
    title="商城商品管理"
    description="管理员可在这里新增商品、维护详情、上传封面、调整库存并控制上下架。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <section class="admin-shop-page">
      <p v-if="feedbackMessage" :class="['feedback-tip', feedbackOk ? 'ok' : 'error']">{{ feedbackMessage }}</p>

      <div class="admin-grid">
        <article class="form-panel">
          <header class="section-head">
            <p class="section-kicker">PRODUCT FORM</p>
            <h3>{{ editingId ? '编辑商品' : '新增商品' }}</h3>
          </header>

          <form class="product-form" @submit.prevent="submitProduct">
            <label>
              <span>商品编码</span>
              <input v-model.trim="form.productCode" type="text" maxlength="64" placeholder="如 SHOP-CUP-001" />
            </label>
            <label>
              <span>商品名称</span>
              <input v-model.trim="form.name" type="text" maxlength="120" placeholder="请输入商品名称" />
            </label>
            <label>
              <span>副标题</span>
              <input v-model.trim="form.subtitle" type="text" maxlength="255" placeholder="请输入副标题" />
            </label>
            <label>
              <span>封面图地址</span>
              <div class="upload-row">
                <input v-model.trim="form.coverUrl" type="text" placeholder="可直接填写 URL 或上传图片" />
                <input ref="uploadInputRef" type="file" accept="image/*" hidden @change="handleCoverUpload" />
                <button type="button" class="secondary-btn" :disabled="coverUploading" @click="uploadInputRef?.click()">
                  {{ coverUploading ? '上传中...' : '上传封面' }}
                </button>
              </div>
              <div v-if="coverUploading || coverUploadDone" class="upload-status" :class="{ done: coverUploadDone && !coverUploading }">
                <div class="upload-progress">
                  <span :style="{ width: `${coverUploadProgress}%` }"></span>
                </div>
                <span>{{ coverUploading ? `上传中 ${coverUploadProgress}%` : '上传完成，已更新封面地址' }}</span>
              </div>
            </label>
            <div class="inline-fields three-cols">
              <label>
                <span>价格</span>
                <input v-model.number="form.price" type="number" min="0.01" step="0.01" />
              </label>
              <label>
                <span>库存</span>
                <input v-model.number="form.stock" type="number" min="0" step="1" />
              </label>
              <label>
                <span>排序</span>
                <input v-model.number="form.sortNo" type="number" min="0" step="1" />
              </label>
            </div>
            <div class="inline-fields two-cols">
              <label>
                <span>上架状态</span>
                <select v-model.number="form.isOnShelf">
                  <option :value="1">上架</option>
                  <option :value="0">下架</option>
                </select>
              </label>
              <label>
                <span>有效状态</span>
                <select v-model.number="form.status">
                  <option :value="1">有效</option>
                  <option :value="0">停用</option>
                </select>
              </label>
            </div>
            <label>
              <span>商品详情</span>
              <textarea v-model.trim="form.detailContent" rows="6" placeholder="请输入商品说明、材质、适用场景等信息"></textarea>
            </label>
            <div class="form-actions">
              <button type="submit" class="action-btn">{{ submitting ? '保存中...' : editingId ? '保存修改' : '新增商品' }}</button>
              <button v-if="editingId" type="button" class="secondary-btn" @click="resetForm">取消编辑</button>
            </div>
          </form>
        </article>

        <article class="list-panel">
          <header class="section-head">
            <p class="section-kicker">PRODUCT LIST</p>
            <h3>商品列表</h3>
          </header>
          <form class="filter-row" @submit.prevent="loadProducts">
            <input v-model.trim="filters.keyword" type="text" placeholder="搜索商品名称或编码" />
            <button type="submit" class="secondary-btn">筛选</button>
          </form>
          <div v-if="products.length > 0" class="product-list">
            <article v-for="product in products" :key="product.id" class="product-card">
              <img :src="resolveImage(product.coverUrl)" :alt="product.name || product.productCode || '商品封面'" loading="lazy" />
              <div class="product-info">
                <div>
                  <p class="product-code">{{ product.productCode }}</p>
                  <h4>{{ product.name || '未命名商品' }}</h4>
                  <p class="product-subtitle">{{ product.subtitle || '无副标题' }}</p>
                </div>
                <div class="meta-row">
                  <span>¥{{ formatPrice(product.price) }}</span>
                  <span>库存 {{ product.stock ?? 0 }}</span>
                  <span>{{ product.isOnShelf ? '上架中' : '已下架' }}</span>
                </div>
                <div class="card-actions">
                  <button type="button" class="secondary-btn small-btn" @click="editProduct(product)">编辑</button>
                  <button type="button" class="secondary-btn small-btn" @click="toggleShelf(product)">
                    {{ product.isOnShelf ? '下架' : '上架' }}
                  </button>
                  <button type="button" class="secondary-btn small-btn danger-btn" @click="deleteProduct(product)">删除</button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="empty-products">
            <p>暂无商品数据</p>
            <span>新增商品后会在这里显示，也可以调整关键词重新筛选。</span>
          </div>
        </article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { adminShopAPI, normalizeMediaUrl } from '@/utils/collectiblesApi'
import { adminNavItems } from './adminNavItems'

const uploadInputRef = ref(null)
const products = ref([])
const editingId = ref(null)
const submitting = ref(false)
const coverUploading = ref(false)
const coverUploadProgress = ref(0)
const coverUploadDone = ref(false)
const feedbackMessage = ref('')
const feedbackOk = ref(true)
const filters = reactive({ keyword: '' })
const form = reactive({
  productCode: '',
  name: '',
  subtitle: '',
  coverUrl: '',
  detailContent: '',
  price: 0,
  stock: 0,
  sortNo: 0,
  isOnShelf: 1,
  status: 1
})

const resolveImage = (url) => normalizeMediaUrl(url) || '/vcg-kiln-vessels-row.webp'
const formatPrice = (value) => Number(value || 0).toFixed(2)

const setFeedback = (message, ok = true) => {
  feedbackMessage.value = message
  feedbackOk.value = ok
}

const resetUploadState = () => {
  coverUploading.value = false
  coverUploadProgress.value = 0
  coverUploadDone.value = false
}

const resetForm = () => {
  editingId.value = null
  form.productCode = ''
  form.name = ''
  form.subtitle = ''
  form.coverUrl = ''
  form.detailContent = ''
  form.price = 0
  form.stock = 0
  form.sortNo = 0
  form.isOnShelf = 1
  form.status = 1
  resetUploadState()
}

const loadProducts = async () => {
  try {
    const data = await adminShopAPI.getProducts({ keyword: filters.keyword || undefined })
    products.value = Array.isArray(data?.list) ? data.list : (Array.isArray(data) ? data : [])
  } catch (error) {
    products.value = []
    setFeedback(error.message || '商品列表加载失败', false)
  }
}

const submitProduct = async () => {
  submitting.value = true
  try {
    const payload = {
      productCode: form.productCode,
      name: form.name,
      subtitle: form.subtitle,
      coverUrl: form.coverUrl,
      detailContent: form.detailContent,
      price: Number(form.price),
      stock: Number(form.stock),
      sortNo: Number(form.sortNo),
      isOnShelf: Number(form.isOnShelf),
      status: Number(form.status)
    }
    if (editingId.value) {
      await adminShopAPI.updateProduct(editingId.value, payload)
      setFeedback('商品信息已更新')
    } else {
      await adminShopAPI.createProduct(payload)
      setFeedback('商品已新增')
    }
    resetForm()
    await loadProducts()
  } catch (error) {
    setFeedback(error.message || '保存商品失败', false)
  } finally {
    submitting.value = false
  }
}

const editProduct = (product) => {
  editingId.value = product.id
  form.productCode = product.productCode || ''
  form.name = product.name || ''
  form.subtitle = product.subtitle || ''
  form.coverUrl = product.coverUrl || ''
  form.detailContent = product.detailContent || ''
  form.price = Number(product.price || 0)
  form.stock = Number(product.stock || 0)
  form.sortNo = Number(product.sortNo || 0)
  form.isOnShelf = product.isOnShelf ? 1 : 0
  form.status = Number(product.status ?? 1)
  resetUploadState()
}

const toggleShelf = async (product) => {
  try {
    await adminShopAPI.updateShelf(product.id, { isOnShelf: product.isOnShelf ? 0 : 1 })
    setFeedback(`商品已${product.isOnShelf ? '下架' : '上架'}`)
    await loadProducts()
  } catch (error) {
    setFeedback(error.message || '切换上下架失败', false)
  }
}

const deleteProduct = async (product) => {
  if (!window.confirm(`确认删除商品“${product.name}”吗？`)) return
  try {
    await adminShopAPI.deleteProduct(product.id)
    setFeedback('商品已删除')
    if (editingId.value === product.id) {
      resetForm()
    }
    await loadProducts()
  } catch (error) {
    setFeedback(error.message || '删除商品失败', false)
  }
}

const handleCoverUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  coverUploadProgress.value = 0
  coverUploadDone.value = false
  try {
    const result = await adminShopAPI.uploadCover(file, {
      onUploadProgress: (progressEvent) => {
        const total = Number(progressEvent?.total || 0)
        const loaded = Number(progressEvent?.loaded || 0)
        if (total > 0) {
          coverUploadProgress.value = Math.min(100, Math.round((loaded / total) * 100))
        }
      }
    })
    form.coverUrl = result?.coverUrl || ''
    coverUploadProgress.value = 100
    coverUploadDone.value = true
    setFeedback('封面上传成功')
  } catch (error) {
    resetUploadState()
    setFeedback(error.message || '封面上传失败', false)
  } finally {
    coverUploading.value = false
    event.target.value = ''
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.admin-shop-page {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
  min-width: 0;
}

.form-panel,
.list-panel {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 255, 240, 0.92);
  padding: 16px;
  display: grid;
  gap: 12px;
  align-self: stretch;
  height: clamp(640px, 72vh, 860px);
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.form-panel {
  grid-template-rows: auto minmax(0, 1fr);
}

.list-panel {
  grid-template-rows: auto auto minmax(0, 1fr);
}

.section-kicker,
.feedback-tip,
.product-code,
.product-subtitle,
.meta-row span {
  margin: 0;
}

.section-kicker,
.product-code {
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: var(--ym-text-muted);
}

.section-head h3,
.product-info h4 {
  margin: 6px 0 0;
  font-family: var(--ym-font-calligraphy-ma);
}

.product-form,
.filter-row,
.product-list {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.product-form {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
}

.filter-row {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.product-list {
  align-content: start;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
  scrollbar-gutter: stable;
}

.empty-products {
  border: 1px dashed rgba(var(--ym-accent-rgb), 0.28);
  border-radius: 10px 22px 10px 22px;
  background:
    radial-gradient(circle at 18% 10%, rgba(var(--ym-accent-rgb), 0.08), transparent 32%),
    rgba(255, 255, 255, 0.58);
  color: var(--ym-text-secondary);
  display: grid;
  gap: 6px;
  min-height: 180px;
  place-content: center;
  text-align: center;
}

.empty-products p {
  color: var(--ym-text);
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.08rem;
  margin: 0;
}

.empty-products span {
  font-size: 0.9rem;
}

.product-form label {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.product-form span {
  color: var(--ym-text-secondary);
  font-size: 0.88rem;
}

.product-form input,
.product-form textarea,
.product-form select,
.filter-row input {
  border: 1px solid var(--ym-border-strong);
  border-radius: 8px 18px 8px 18px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.88);
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
}

.product-form textarea {
  resize: vertical;
}

.upload-row,
.form-actions,
.card-actions,
.meta-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.upload-row input[type='text'] {
  flex: 1 1 220px;
  min-width: 0;
}

.upload-status {
  display: grid;
  gap: 6px;
  color: var(--ym-text-secondary);
  font-size: 0.82rem;
}

.upload-status.done {
  color: #1e7d50;
}

.upload-progress {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(var(--ym-accent-rgb), 0.12);
  overflow: hidden;
}

.upload-progress span {
  display: block;
  height: 100%;
  background: rgba(var(--ym-accent-rgb), 0.68);
  transition: width 0.2s ease;
}

.inline-fields {
  display: grid;
  gap: 10px;
}

.inline-fields.three-cols {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.inline-fields.two-cols {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.inline-fields label {
  min-width: 0;
}

.inline-fields input,
.inline-fields select {
  width: 100%;
}

.secondary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.product-card {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 12px;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(255, 255, 255, 0.8);
  overflow: hidden;
  min-height: 192px;
  min-width: 0;
}

.product-card img {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 192px;
  object-fit: cover;
}

.product-info {
  padding: 12px;
  display: grid;
  gap: 8px;
  align-content: start;
  min-width: 0;
}

.product-info > div {
  min-width: 0;
}

.product-code,
.product-info h4,
.product-subtitle {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.product-subtitle {
  margin-top: 4px;
  color: var(--ym-text-secondary);
  line-height: 1.7;
}

.meta-row {
  align-items: center;
}

.card-actions {
  align-items: center;
  padding-top: 4px;
}

.meta-row span {
  font-size: 0.84rem;
  color: var(--ym-text-secondary);
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

.small-btn {
  padding: 8px 12px;
}

.danger-btn {
  color: var(--ym-danger);
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
  .admin-grid,
  .product-card {
    grid-template-columns: 1fr;
  }

  .form-panel,
  .list-panel {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .product-form,
  .product-list {
    overflow: visible;
  }

  .product-card img {
    aspect-ratio: 16 / 9;
    height: auto;
    min-height: 0;
  }
}

@media (max-width: 720px) {
  .upload-row,
  .filter-row,
  .inline-fields.three-cols,
  .inline-fields.two-cols {
    grid-template-columns: 1fr;
  }

  .upload-row {
    display: grid;
  }
}
</style>
