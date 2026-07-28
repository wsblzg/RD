<template>
  <CeramicsPageShell
    kicker="COMMUNITY ARTICLE"
    title="文章详情"
    description="查看社区文章全文、配图与作者信息，支持作者或管理员继续编辑与删除。"
    :alt="true"
  >
    <div class="community-detail">
      <section v-if="loading" class="detail-panel detail-state">
        <p>文章加载中...</p>
      </section>

      <section v-else-if="errorMessage" class="detail-panel detail-state">
        <p>{{ errorMessage }}</p>
        <div class="state-actions">
          <button type="button" class="ghost-btn" @click="goBackToSquare">返回社区广场</button>
        </div>
      </section>

      <template v-else-if="post">
        <section class="detail-panel detail-hero">
          <div class="hero-copy">
            <p class="detail-kicker">{{ post.category || '未分类' }}</p>
            <h2>{{ post.title }}</h2>
            <p class="detail-summary" v-if="post.summary">{{ post.summary }}</p>
            <div class="meta-row">
              <span>{{ post.authorName || '社区用户' }}</span>
              <span>{{ formatDate(post.createdAt) }}</span>
              <span>{{ (post.imageUrls || []).length }} 张配图</span>
            </div>
            <div v-if="post.tags?.length" class="tag-row">
              <span v-for="tag in post.tags" :key="`${post.id}-${tag}`">#{{ tag }}</span>
            </div>
          </div>
          <img
            v-if="post.coverImage"
            :src="resolveMedia(post.coverImage)"
            :alt="post.title"
            class="hero-cover"
            loading="eager"
          />
        </section>

        <section class="detail-panel">
          <div class="top-actions">
            <button type="button" class="ghost-btn" @click="goBackToSquare">返回社区广场</button>
            <button v-if="canManage(post)" type="button" class="ghost-btn" @click="editPost(post.id)">编辑文章</button>
            <button v-if="canManage(post)" type="button" class="danger-btn" @click="removePost(post.id)">删除文章</button>
          </div>

          <div v-if="post.aiWorkId" class="ai-work-card">
            <div class="ai-work-cover">
              <img
                v-if="linkedWork?.coverUrl"
                :src="resolveLinkedWorkCover(linkedWork)"
                :alt="linkedWork.title"
                loading="lazy"
                @error="handleLinkedWorkCoverError($event, linkedWork)"
              />
              <span v-else>AI</span>
            </div>
            <div class="ai-work-copy">
              <p class="ai-work-kicker">AI 作品资产</p>
              <strong>{{ linkedWork?.title || '关联作品加载中...' }}</strong>
              <p>{{ linkedWork?.prompt || `${linkedWork?.style || ''} ${linkedWork?.vessel || ''}` || '作者将这篇文章挂载到一个 AI 3D 作品。' }}</p>
              <div class="ai-work-meta">
                <span>{{ linkedWork?.modelFormat || 'glb' }}</span>
                <span>{{ linkedWork?.style || 'AI 生成' }}</span>
                <span>{{ linkedWork?.vessel || '陶瓷作品' }}</span>
              </div>
            </div>
            <button v-if="linkedWork?.id" type="button" class="ghost-btn" @click="goAiWork(linkedWork.id)">查看作品</button>
          </div>

          <article class="detail-content prose" v-html="detailHtml"></article>

          <div v-if="post.imageUrls?.length" class="detail-gallery">
            <img
              v-for="(url, index) in post.imageUrls"
              :key="`${url}-${index}`"
              :src="resolveMedia(url)"
              :alt="`${post.title}-配图-${index + 1}`"
              loading="lazy"
            />
          </div>
        </section>
      </template>
    </div>
  </CeramicsPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DOMPurify from 'dompurify'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { communityAPI, normalizeMediaUrl, normalizeMediaUrls } from '@/utils/collectiblesApi'
import { getCeramicWork } from '@/utils/ceramicCreationApi'
import { resolveGeneratedCover } from '@/utils/ceramicGenerationSchedule'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const post = ref(null)
const linkedWork = ref(null)
const errorMessage = ref('')

const currentUser = computed(() => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
})

const detailHtml = computed(() => DOMPurify.sanitize(String(post.value?.contentHtml || '')))

const parseId = (value) => {
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : null
}

const normalizePost = (data = {}) => ({
  ...data,
  coverImage: normalizeMediaUrl(data.coverImage),
  imageUrls: normalizeMediaUrls(data.imageUrls)
})

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

const resolveMedia = (url) => normalizeMediaUrl(url)
const resolveLinkedWorkCover = (work) => normalizeMediaUrl(resolveGeneratedCover(work))
const handleLinkedWorkCoverError = (event, work) => {
  const fallbackUrl = normalizeMediaUrl(work?.coverUrl) || '/青花梅瓶.webp'
  if (event.currentTarget?.src && !event.currentTarget.src.endsWith(fallbackUrl)) {
    event.currentTarget.src = fallbackUrl
  }
}

const canManage = (currentPost) => {
  if (!currentPost || !currentUser.value?.id) return false
  if (currentUser.value.role === 'admin') return true
  return Number(currentPost.userId) === Number(currentUser.value.id)
}

const goBackToSquare = () => {
  router.push('/ceramics/community')
}

const goAiWork = (id) => {
  if (!id) return
  router.push({ path: '/ceramics/ai-creation', query: { workId: id } })
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
    router.replace('/ceramics/community')
  } catch (error) {
    window.alert(error?.message || '删除失败')
  }
}

const loadPost = async () => {
  const postId = parseId(route.params.id)
  if (!postId) {
    errorMessage.value = '文章编号无效'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await communityAPI.getPostDetail(postId)
    post.value = normalizePost(data)
    if (post.value?.aiWorkId) {
      try {
        const work = await getCeramicWork(post.value.aiWorkId)
        linkedWork.value = work || null
      } catch (error) {
        linkedWork.value = null
      }
    } else {
      linkedWork.value = null
    }
  } catch (error) {
    post.value = null
    errorMessage.value = error?.message || '文章加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPost()
})
</script>

<style scoped>
.community-detail {
  display: grid;
  gap: 14px;
}

.detail-panel {
  border: 1px solid var(--ym-border);
  border-radius: 10px 24px 10px 24px;
  background: rgba(255, 255, 240, 0.92);
  padding: 20px;
}

.detail-state {
  min-height: 220px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--ym-text-secondary);
}

.state-actions {
  margin-top: 10px;
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 420px);
  gap: 18px;
  align-items: stretch;
}

.detail-kicker {
  margin: 0;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  color: var(--ym-accent);
}

.hero-copy h2 {
  margin: 10px 0 12px;
  font-size: clamp(1.8rem, 3vw, 2.6rem);
  line-height: 1.22;
}

.detail-summary {
  margin: 0 0 14px;
  color: var(--ym-text-secondary);
  line-height: 1.8;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  color: var(--ym-text-muted);
  font-size: 0.9rem;
}

.tag-row {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-row span {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.25);
  background: rgba(var(--ym-accent-rgb), 0.08);
  color: var(--ym-accent);
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 0.82rem;
}

.hero-cover {
  width: 100%;
  height: 100%;
  min-height: 260px;
  border-radius: 10px 24px 10px 24px;
  object-fit: cover;
  border: 1px solid rgba(26, 26, 26, 0.08);
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.ai-work-card {
  margin-bottom: 18px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.28);
  border-radius: 10px 24px 10px 24px;
  background:
    linear-gradient(135deg, rgba(var(--ym-accent-rgb), 0.1), rgba(var(--ym-gold-rgb), 0.08)),
    rgba(255, 255, 255, 0.78);
  padding: 12px;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.ai-work-cover {
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  background: rgba(255, 252, 246, 0.9);
  display: grid;
  place-items: center;
  color: var(--ym-accent);
  font-family: var(--ym-font-seal);
}

.ai-work-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.ai-work-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.ai-work-copy strong,
.ai-work-copy p,
.ai-work-kicker {
  margin: 0;
}

.ai-work-kicker {
  font-size: 0.72rem;
  letter-spacing: 0.16em;
  color: var(--ym-accent);
}

.ai-work-copy strong {
  font-size: 1.05rem;
}

.ai-work-copy p {
  color: var(--ym-text-secondary);
  line-height: 1.65;
  font-size: 0.88rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ai-work-meta {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.ai-work-meta span {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.22);
  border-radius: 999px;
  padding: 3px 8px;
  color: var(--ym-text-muted);
  font-size: 0.74rem;
  background: rgba(255, 255, 255, 0.68);
}

.ghost-btn,
.danger-btn {
  border-radius: 6px 14px 6px 14px;
  padding: 8px 14px;
  cursor: pointer;
}

.ghost-btn {
  border: 1px solid var(--ym-border-strong);
  background: rgba(255, 255, 255, 0.84);
  color: var(--ym-text);
}

.danger-btn {
  border: 1px solid rgba(184, 72, 72, 0.45);
  background: rgba(184, 72, 72, 0.1);
  color: #b84848;
}

.detail-content {
  color: var(--ym-text);
  line-height: 1.95;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3) {
  line-height: 1.35;
  margin: 1.2em 0 0.6em;
}

.detail-content :deep(p) {
  margin: 0.8em 0;
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
  padding-left: 1.4em;
}

.detail-content :deep(img) {
  max-width: 100%;
  border-radius: 10px 18px 10px 18px;
}

.detail-gallery {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-gallery img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 10px 20px 10px 20px;
}

@media (max-width: 900px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .ai-work-card {
    grid-template-columns: 1fr;
  }

  .detail-gallery {
    grid-template-columns: 1fr;
  }
}
</style>
