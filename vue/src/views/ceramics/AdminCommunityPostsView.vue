<template>
  <CeramicsPageShell
    kicker="ADMIN"
    title="后台管理"
    description="管理员专用入口：集中管理社区文章，支持搜索、查看详情、编辑与删除。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <div class="admin-community">
      <section v-if="!isAdmin" class="panel access-panel">
        <header class="panel-head">
          <p class="eyebrow">ADMIN ACCESS</p>
          <h3>{{ isLoggedIn ? '当前账号没有后台权限' : '请先登录管理员账号' }}</h3>
          <p>{{ isLoggedIn ? '该页面仅管理员可访问。' : '你已进入后台文章管理路由，但当前还未登录。' }}</p>
        </header>
        <div class="panel-actions">
          <button type="button" class="ghost-btn" @click="goAdminLogin">{{ isLoggedIn ? '切换管理员登录' : '去登录' }}</button>
          <button type="button" class="ghost-btn" @click="goSquare">返回社区广场</button>
        </div>
      </section>

      <template v-else>
        <section class="panel filter-panel">
          <label class="field">
            <span>关键词</span>
            <input v-model.trim="filters.keyword" type="text" placeholder="标题/摘要/正文关键词" @keyup.enter="searchPosts" />
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
            <input v-model.trim="filters.tag" type="text" placeholder="输入标签关键词" @keyup.enter="searchPosts" />
          </label>
          <div class="toolbar-actions">
            <button type="button" class="primary-btn" @click="searchPosts">搜索</button>
            <button type="button" class="ghost-btn" @click="resetFilters">重置</button>
            <button type="button" class="ghost-btn" @click="goPublish">新建文章</button>
          </div>
        </section>

        <section class="panel list-panel">
          <header class="panel-head compact">
            <div>
              <p class="eyebrow">POSTS</p>
              <h3>文章管理</h3>
            </div>
            <p class="panel-tip">共 {{ pagination.total }} 篇文章，当前第 {{ pagination.page }} 页。</p>
          </header>

          <p v-if="loading" class="panel-tip">文章加载中...</p>
          <p v-else-if="list.length === 0" class="panel-tip">暂无匹配文章。</p>

          <div v-else class="post-list">
            <article v-for="post in list" :key="post.id" class="post-card">
              <img
                v-if="post.coverImage"
                :src="resolveMedia(post.coverImage)"
                :alt="post.title"
                class="post-cover"
                loading="lazy"
              />
              <div class="post-body">
                <div class="meta-row">
                  <span>{{ post.authorName || '社区用户' }}</span>
                  <span>{{ formatDate(post.createdAt) }}</span>
                </div>
                <h4>{{ post.title }}</h4>
                <p class="summary">{{ post.summary || plainText(post.contentHtml) }}</p>
                <div class="tag-row">
                  <span class="category">{{ post.category || '未分类' }}</span>
                  <span v-for="tag in post.tags || []" :key="`${post.id}-${tag}`">#{{ tag }}</span>
                </div>
                <div class="meta-strip">
                  <span>{{ (post.imageUrls || []).length }} 张图</span>
                  <span>点赞 {{ post.likeCount || 0 }}</span>
                  <span>评论 {{ post.commentCount || 0 }}</span>
                </div>
                <div class="card-actions">
                  <button type="button" class="ghost-btn" @click="viewDetail(post.id)">查看详情</button>
                  <button type="button" class="ghost-btn" @click="editPost(post.id)">编辑</button>
                  <button type="button" class="danger-btn" @click="removePost(post.id)">删除</button>
                </div>
              </div>
            </article>
          </div>

          <footer class="pager" v-if="pagination.total > 0">
            <button type="button" class="ghost-btn" :disabled="pagination.page <= 1 || loading" @click="changePage(pagination.page - 1)">上一页</button>
            <span>第 {{ pagination.page }} / {{ totalPages }} 页</span>
            <button type="button" class="ghost-btn" :disabled="pagination.page >= totalPages || loading" @click="changePage(pagination.page + 1)">下一页</button>
          </footer>
        </section>
      </template>
    </div>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { collectiblesAuthAPI, communityAPI, normalizeMediaUrl, normalizeMediaUrls } from '@/utils/collectiblesApi'
import { adminNavItems } from './adminNavItems'

const router = useRouter()
const categoryOptions = ['活动纪实', '工艺分享', '传承故事', '研学记录', '作品解读', '未分类']

const currentUser = ref(null)
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

const isLoggedIn = computed(() => Boolean(currentUser.value?.id))
const isAdmin = computed(() => currentUser.value?.role === 'admin')
const totalPages = computed(() => Math.max(1, Math.ceil((pagination.total || 0) / pagination.pageSize)))

const readStoredUser = () => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

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

const plainText = (html) => {
  const plain = String(html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return plain.length > 120 ? `${plain.slice(0, 120)}...` : plain
}

const resolveMedia = (url) => normalizeMediaUrl(url)

const parseId = (value) => {
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : null
}

const loadCurrentUser = async () => {
  currentUser.value = readStoredUser()
  if (!collectiblesAuthAPI.getToken()) return
  try {
    const user = await collectiblesAuthAPI.me()
    currentUser.value = user
    localStorage.setItem('yc_user', JSON.stringify(user))
  } catch (error) {
    currentUser.value = null
  }
}

const loadPosts = async () => {
  if (!isAdmin.value) {
    list.value = []
    pagination.total = 0
    return
  }
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

const changePage = async (page) => {
  pagination.page = Math.max(1, page)
  await loadPosts()
}

const viewDetail = (id) => {
  const postId = parseId(id)
  if (!postId) return
  router.push(`/ceramics/community/post/${postId}`)
}

const editPost = (id) => {
  const postId = parseId(id)
  if (!postId) return
  router.push({ path: '/ceramics/community/publish', query: { postId } })
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

const goPublish = () => {
  router.push('/ceramics/community/publish')
}

const goAdminLogin = () => {
  router.push({
    path: '/ceramics/user-login',
    query: { redirect: '/ceramics/admin/community' }
  })
}

const goSquare = () => {
  router.push('/ceramics/community')
}

onMounted(async () => {
  await loadCurrentUser()
  await loadPosts()
})
</script>

<style scoped>
.admin-community {
  display: grid;
  gap: 12px;
}

.panel {
  border: 1px solid var(--ym-border);
  border-radius: 8px 22px 8px 22px;
  background: rgba(255, 255, 240, 0.9);
  padding: 16px;
}

.panel-head h3 {
  margin: 6px 0 8px;
}

.eyebrow {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.16em;
  color: var(--ym-text-muted);
}

.compact {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
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

.toolbar-actions,
.panel-actions,
.card-actions,
.pager {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pager {
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.primary-btn,
.ghost-btn,
.danger-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 8px 14px;
  cursor: pointer;
}

.primary-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.48);
  background: rgba(var(--ym-accent-rgb), 0.16);
  color: var(--ym-text);
}

.ghost-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.88);
  color: var(--ym-text);
}

.danger-btn {
  border: 1px solid rgba(184, 72, 72, 0.45);
  background: rgba(184, 72, 72, 0.1);
  color: #b84848;
}

.panel-tip {
  color: var(--ym-text-muted);
}

.post-list {
  display: grid;
  gap: 12px;
}

.post-card {
  display: grid;
  grid-template-columns: 220px 1fr;
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.84);
}

.post-cover {
  width: 100%;
  height: 100%;
  min-height: 180px;
  object-fit: cover;
}

.post-body {
  padding: 12px 14px;
  display: grid;
  gap: 8px;
}

.post-body h4 {
  margin: 0;
  font-size: 1.02rem;
}

.summary {
  margin: 0;
  line-height: 1.8;
  color: var(--ym-text-secondary);
}

.meta-row,
.meta-strip {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--ym-text-muted);
  font-size: 0.8rem;
}

.tag-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-row span {
  font-size: 0.74rem;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--ym-border);
  color: var(--ym-text-muted);
}

.tag-row .category {
  border-color: rgba(var(--ym-accent-rgb), 0.38);
  color: var(--ym-accent);
}

@media (max-width: 960px) {
  .filter-panel {
    grid-template-columns: 1fr 1fr;
  }

  .post-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .compact {
    display: grid;
  }
}
</style>
