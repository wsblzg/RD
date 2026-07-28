<template>
  <CeramicsPageShell
    kicker="COMMUNITY SQUARE"
    title="社区广场"
    description="查看最新社区文章，按分类筛选，并对自己的文章进行编辑或删除。"
    :alt="true"
  >
    <div class="community-square">
      <section class="filter-panel">
        <label class="field">
          <span>关键词</span>
          <input v-model.trim="filters.keyword" type="text" placeholder="标题/摘要关键词" @keyup.enter="searchPosts" />
        </label>
        <label class="field">
          <span>分类</span>
          <select v-model="filters.category">
            <option value="">全部分类</option>
            <option v-for="item in categoryOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label class="field">
          <span>标签</span>
          <input v-model.trim="filters.tag" type="text" placeholder="如：开窑 研学 匠人" @keyup.enter="searchPosts" />
        </label>
        <div class="actions">
          <button type="button" class="btn-primary" @click="searchPosts">搜索</button>
          <button type="button" class="btn-ghost" @click="resetFilters">重置</button>
          <button type="button" class="btn-ghost" @click="goPublish">发布文章</button>
        </div>
      </section>

      <section class="list-panel">
        <p v-if="loading" class="list-tip">文章加载中...</p>
        <p v-else-if="list.length === 0" class="list-tip">暂时没有匹配文章，换个筛选条件试试。</p>
        <div v-else class="post-grid">
          <article v-for="post in list" :key="post.id" class="post-card">
            <img v-if="post.coverImage" :src="resolvePostCover(post.coverImage)" :alt="post.title" class="post-cover" loading="lazy" />
            <div class="post-body">
              <div class="meta-row">
                <span>{{ post.authorName || '社区用户' }}</span>
                <span>{{ formatDate(post.createdAt) }}</span>
              </div>
              <h4>{{ post.title }}</h4>
              <p class="summary">{{ post.summary || plainText(post.contentHtml) }}</p>
              <div class="tag-row">
                <span class="category">{{ post.category || '未分类' }}</span>
                <span v-if="post.aiWorkId" class="category ai-badge">关联作品</span>
                <span v-for="tag in post.tags || []" :key="`${post.id}-${tag}`">#{{ tag }}</span>
              </div>
              <div v-if="post.aiWorkId" class="attached-work-strip">
                <span class="work-icon">AI</span>
                <div>
                  <strong>挂载 AI 3D 作品</strong>
                  <p>进入详情可查看作者关联的作品资产</p>
                </div>
              </div>
              <div class="card-actions">
                <button type="button" class="ghost-btn" @click="viewDetail(post.id)">查看详情</button>
                <button v-if="canManage(post)" type="button" class="ghost-btn" @click="editPost(post.id)">编辑</button>
                <button v-if="canManage(post)" type="button" class="danger-btn" @click="removePost(post.id)">删除</button>
              </div>
            </div>
          </article>
        </div>

        <footer class="pager" v-if="pagination.total > 0">
          <button type="button" class="ghost-btn" :disabled="pagination.page <= 1 || loading" @click="changePage(pagination.page - 1)">上一页</button>
          <span>第 {{ pagination.page }} / {{ totalPages }} 页 · 共 {{ pagination.total }} 条</span>
          <button type="button" class="ghost-btn" :disabled="pagination.page >= totalPages || loading" @click="changePage(pagination.page + 1)">下一页</button>
        </footer>
      </section>
    </div>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { communityAPI, normalizeMediaUrl, normalizeMediaUrls } from '@/utils/collectiblesApi'

const router = useRouter()

const categoryOptions = ['活动纪实', '工艺分享', '传承故事', '研学记录', '作品解读', '未分类']
const loading = ref(false)
const list = ref([])
const filters = reactive({
  keyword: '',
  category: '',
  tag: ''
})
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const currentUser = computed(() => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
})
const totalPages = computed(() => Math.max(1, Math.ceil((pagination.total || 0) / pagination.pageSize)))

const parseId = (value) => {
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : null
}

const plainText = (html) => {
  const plain = String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return plain.length > 100 ? `${plain.slice(0, 100)}...` : plain
}

const resolvePostCover = (url) => normalizeMediaUrl(url)

const normalizePost = (post = {}) => ({
  ...post,
  coverImage: normalizeMediaUrl(post.coverImage),
  imageUrls: normalizeMediaUrls(post.imageUrls)
})

const formatDate = (value) => {
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

const canManage = (post) => {
  if (!post || !currentUser.value?.id) return false
  if (currentUser.value.role === 'admin') return true
  return Number(post.userId) === Number(currentUser.value.id)
}

const loadPosts = async () => {
  loading.value = true
  try {
    const data = await communityAPI.getPosts({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      category: filters.category || undefined,
      tag: filters.tag || undefined
    })
    list.value = Array.isArray(data?.list) ? data.list.map((post) => normalizePost(post)) : []
    pagination.total = Number(data?.pagination?.total || 0)
  } catch (error) {
    list.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  const postId = parseId(id)
  if (!postId) return
  router.push(`/ceramics/community/post/${postId}`)
}

const editPost = (id) => {
  const postId = parseId(id)
  if (!postId) return
  router.push({ path: '/community/publish', query: { postId } })
}

const removePost = async (id) => {
  const postId = parseId(id)
  if (!postId) return
  if (!window.confirm('确认删除该文章？删除后不可恢复。')) return
  try {
    await communityAPI.deletePost(postId)
    await loadPosts()
  } catch (error) {
    window.alert(error?.message || '删除失败')
  }
}

const changePage = async (page) => {
  pagination.page = Math.max(1, page)
  await loadPosts()
}

const searchPosts = async () => {
  pagination.page = 1
  await loadPosts()
}

const resetFilters = async () => {
  filters.keyword = ''
  filters.category = ''
  filters.tag = ''
  pagination.page = 1
  await loadPosts()
}

const goPublish = () => {
  router.push('/community/publish')
}

onMounted(async () => {
  await loadPosts()
})
</script>

<style scoped>
.community-square {
  display: grid;
  gap: 12px;
}

.filter-panel,
.list-panel {
  border: 1px solid var(--ym-border);
  border-radius: 8px 22px 8px 22px;
  background: rgba(255, 255, 240, 0.9);
  padding: 14px;
}

.filter-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 10px;
  align-items: end;
}

.field {
  display: grid;
  gap: 5px;
}

.field span {
  font-size: 0.8rem;
  color: var(--ym-text-muted);
}

.field input,
.field select {
  height: 40px;
  border-radius: 6px 14px 6px 14px;
  border: 1px solid var(--ym-border);
  background: rgba(255, 255, 255, 0.92);
  padding: 0 12px;
  color: var(--ym-text);
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-primary,
.btn-ghost {
  border-radius: 6px 14px 6px 14px;
  padding: 8px 14px;
  cursor: pointer;
}

.btn-primary {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.48);
  background: rgba(var(--ym-accent-rgb), 0.16);
  color: var(--ym-text);
}

.btn-ghost {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.8);
  color: var(--ym-text);
}

.list-tip {
  margin: 0;
  color: var(--ym-text-muted);
}

.post-grid {
  display: grid;
  gap: 10px;
}

.post-card {
  border: 1px solid var(--ym-border);
  border-radius: 8px 16px 8px 16px;
  background: rgba(255, 255, 255, 0.86);
  display: grid;
  grid-template-columns: 180px 1fr;
  overflow: hidden;
}

.post-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-body {
  padding: 10px 12px;
  display: grid;
  gap: 6px;
}

.meta-row {
  font-size: 0.76rem;
  color: var(--ym-text-muted);
  display: flex;
  justify-content: space-between;
}

.post-body h4 {
  margin: 0;
  font-size: 1rem;
}

.summary {
  margin: 0;
  color: var(--ym-text-secondary);
  line-height: 1.72;
  font-size: 0.88rem;
}

.tag-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-row span {
  font-size: 0.74rem;
  padding: 2px 8px;
  border: 1px solid var(--ym-border);
  border-radius: 999px;
  color: var(--ym-text-muted);
}

.tag-row .category {
  color: var(--ym-accent);
  border-color: rgba(var(--ym-accent-rgb), 0.42);
}

.tag-row .ai-badge {
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.attached-work-strip {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.22);
  border-radius: 8px 16px 8px 16px;
  background: linear-gradient(135deg, rgba(var(--ym-accent-rgb), 0.08), rgba(var(--ym-gold-rgb), 0.08));
  padding: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.work-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px 14px 8px 14px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.25);
  background: rgba(255, 255, 255, 0.74);
  color: var(--ym-accent);
  font-family: var(--ym-font-seal);
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.attached-work-strip strong,
.attached-work-strip p {
  margin: 0;
}

.attached-work-strip strong {
  font-size: 0.84rem;
}

.attached-work-strip p {
  color: var(--ym-text-muted);
  font-size: 0.76rem;
  margin-top: 2px;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ghost-btn,
.danger-btn {
  border-radius: 5px 12px 5px 12px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 0.82rem;
}

.ghost-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.9);
  color: var(--ym-text);
}

.danger-btn {
  border: 1px solid rgba(184, 72, 72, 0.45);
  background: rgba(184, 72, 72, 0.1);
  color: #b84848;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  color: var(--ym-text-secondary);
  font-size: 0.86rem;
}

@media (max-width: 980px) {
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }

  .actions {
    grid-column: 1 / -1;
  }

  .post-card {
    grid-template-columns: 1fr;
  }

  .post-cover {
    aspect-ratio: 16 / 9;
    height: auto;
  }
}

@media (max-width: 680px) {
  .filter-panel {
    grid-template-columns: 1fr;
  }
}
</style>
