<template>
  <div class="publish-page">
    <NewHeaderNavigation />

    <main class="publish-main">
      <header class="page-head">
        <div class="head-title">
          <p class="kicker">COMMUNITY · PUBLISH</p>
          <h1>
            <Edit class="head-icon" />
            <span>{{ isEditMode ? '编辑文章' : '发布文章' }}</span>
          </h1>
          <p>分享你的窑场观察、工艺心得与活动纪实，让更多人看见非遗现场。</p>
        </div>
        <div class="head-actions">
          <button type="button" class="btn-ghost" @click="togglePreview">
            <View class="btn-icon" />
            <span>预览</span>
          </button>
          <button type="button" class="btn-muted" :disabled="publishing" @click="saveDraft">
            <Document class="btn-icon" />
            <span>保存草稿</span>
          </button>
        </div>
      </header>

      <section class="form-card" aria-label="发布表单">
        <h2>基本信息</h2>

        <label class="field-block">
          <span>文章标题 *</span>
          <input
            v-model.trim="form.title"
            type="text"
            maxlength="120"
            placeholder="请输入文章标题..."
          />
          <em>{{ titleCount }}/120</em>
        </label>

        <label class="field-block">
          <span>文章摘要</span>
          <textarea
            v-model.trim="form.summary"
            maxlength="280"
            rows="4"
            placeholder="请输入文章摘要（可选）..."
          ></textarea>
          <em>{{ summaryCount }}/280</em>
        </label>

        <div class="field-row">
          <label class="field-block">
            <span>分类 *</span>
            <select v-model="form.category">
              <option value="">请选择分类</option>
              <option v-for="item in categoryOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="field-block">
            <span>标签</span>
            <input
              v-model.trim="form.tagsInput"
              type="text"
              maxlength="80"
              placeholder="输入标签，使用空格或逗号分隔"
            />
          </label>
        </div>
      </section>

      <section class="form-card">
        <h2>关联 AI 作品</h2>
        <p class="field-hint">可选：选择一件已永久保存的 3D 作品，文章详情页会同步展示。</p>
        <label class="field-block">
          <span>关联作品</span>
          <select v-model="form.aiWorkId">
            <option :value="null">不关联作品</option>
            <option v-for="item in aiWorks" :key="item.id" :value="item.id">
              {{ item.title }} · {{ item.modelFormat || 'glb' }}
            </option>
          </select>
        </label>
      </section>

      <section class="form-card">
        <h2>文章内容 *</h2>
        <div class="editor-tools">
          <button type="button" class="tool-btn" @click="applyEditorCommand('bold')"><b>B</b></button>
          <button type="button" class="tool-btn" @click="applyEditorCommand('italic')"><i>I</i></button>
          <button type="button" class="tool-btn" @click="applyEditorCommand('insertUnorderedList')">列表</button>
          <button type="button" class="tool-btn" @click="applyEditorCommand('formatBlock', 'h3')">H3</button>
          <button type="button" class="tool-btn" @click="insertEditorLink">链接</button>
        </div>
        <div
          ref="editorRef"
          class="rich-editor"
          contenteditable="true"
          data-placeholder="请输入正文内容..."
          @input="onEditorInput"
        ></div>
        <p class="editor-foot">
          <span>支持基础富文本格式，适合发布工艺复盘、活动记录、知识分享。</span>
          <span>{{ contentCount }} 字符</span>
        </p>
      </section>

      <section class="form-card">
        <h2>文章图片</h2>
        <input
          ref="fileInputRef"
          class="hidden-file"
          type="file"
          accept="image/*"
          multiple
          @change="onSelectImages"
        />
        <div
          class="upload-box"
          @click="triggerFilePick"
          @dragover.prevent
          @drop.prevent="onDropFiles"
        >
          <p class="upload-icon">
            <UploadFilled />
          </p>
          <p class="upload-title">点击或拖拽上传图片</p>
          <p class="upload-desc">
            {{ uploading ? '图片上传中...' : '支持 JPG、PNG、WEBP，单图不超过 8MB，最多 9 张' }}
          </p>
        </div>

        <div v-if="form.imageUrls.length" class="upload-preview">
          <figure v-for="(url, idx) in form.imageUrls" :key="url" class="preview-item">
            <img :src="resolvePreviewImage(url)" :alt="`预览图${idx + 1}`" />
            <button type="button" @click="removeImage(idx)">移除</button>
          </figure>
        </div>
      </section>

      <section class="submit-row">
        <button type="button" class="btn-muted" @click="goBackHome">首页</button>
        <button type="button" class="btn-primary" :disabled="publishing" @click="submitPost">
          {{ publishing ? (isEditMode ? '保存中...' : '发布中...') : (isEditMode ? '保存修改' : '发布文章') }}
        </button>
        <span v-if="message.text" :class="['submit-message', message.type]">{{ message.text }}</span>
      </section>
    </main>

    <div v-if="showPreview" class="preview-backdrop" @click="showPreview = false"></div>
    <section v-if="showPreview" class="preview-panel" aria-label="预览">
      <header>
        <h3>{{ previewTitle }}</h3>
        <button type="button" class="btn-muted" @click="showPreview = false">关闭</button>
      </header>
      <p v-if="previewSummary" class="preview-summary">{{ previewSummary }}</p>
      <p class="preview-meta">
        <span>{{ form.category || '未选择分类' }}</span>
        <span v-if="previewTags.length">标签：{{ previewTags.join(' / ') }}</span>
      </p>
      <div class="preview-content" v-html="previewContent"></div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NewHeaderNavigation from '@/components/NewHeaderNavigation.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { collectiblesAuthAPI, communityAPI, normalizeMediaUrl, normalizeMediaUrls } from '@/utils/collectiblesApi'
import { listCeramicWorks } from '@/utils/ceramicCreationApi'
import { Document, Edit, UploadFilled, View } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'

const DRAFT_KEY_BASE = 'yc_community_publish_draft_v2'

const route = useRoute()
const router = useRouter()
const editorRef = ref(null)
const fileInputRef = ref(null)
const uploading = ref(false)
const publishing = ref(false)
const initializing = ref(false)
const showPreview = ref(false)
const message = reactive({
  type: 'ok',
  text: ''
})

const categoryOptions = ['活动纪实', '工艺分享', '传承故事', '研学记录', '作品解读']

const form = reactive({
  title: '',
  summary: '',
  category: '',
  tagsInput: '',
  contentHtml: '',
  imageUrls: [],
  aiWorkId: null
})

const titleCount = computed(() => form.title.length)
const summaryCount = computed(() => form.summary.length)
const contentCount = computed(() => stripHtml(form.contentHtml).length)
const previewTitle = computed(() => form.title || '未命名文章')
const previewSummary = computed(() => form.summary)
const editPostId = computed(() => parsePostId(route.query.postId))
const isEditMode = computed(() => editPostId.value !== null)
const currentDraftKey = computed(() =>
  isEditMode.value ? `${DRAFT_KEY_BASE}_edit_${editPostId.value}` : `${DRAFT_KEY_BASE}_create`
)
const previewContent = computed(() => {
  if (!form.contentHtml) {
    return '<p style="color:#888;">暂无正文内容</p>'
  }
  return DOMPurify.sanitize(form.contentHtml)
})
const previewTags = computed(() => parseTags(form.tagsInput))
const aiWorks = ref([])

const setMessage = (type, text) => {
  message.type = type
  message.text = text
}

const stripHtml = (input) => String(input || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()

const parseTags = (input) =>
  String(input || '')
    .split(/[,\s，、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 8)

const parsePostId = (value) => {
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
}

const applyEditorCommand = (command, value = null) => {
  editorRef.value?.focus()
  document.execCommand(command, false, value)
  form.contentHtml = editorRef.value?.innerHTML || ''
}

const insertEditorLink = () => {
  const url = window.prompt('请输入链接地址（https://...）')
  if (!url) return
  applyEditorCommand('createLink', url.trim())
}

const onEditorInput = () => {
  form.contentHtml = editorRef.value?.innerHTML || ''
}

const triggerFilePick = () => {
  fileInputRef.value?.click()
}

const uploadFiles = async (files) => {
  if (!files.length) return
  if (!collectiblesAuthAPI.getToken()) {
    setMessage('error', '请先登录后再上传图片')
    router.push({ path: '/user-login', query: { redirect: route.fullPath } })
    return
  }
  uploading.value = true
  try {
    for (const file of files) {
      if (form.imageUrls.length >= 9) break
      const result = await communityAPI.uploadImage(file)
      const uploadedUrl = normalizeMediaUrl(result?.url)
      if (uploadedUrl) {
        form.imageUrls.push(uploadedUrl)
      }
    }
    setMessage('ok', '图片上传完成')
  } catch (error) {
    setMessage('error', error?.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

const onSelectImages = async (event) => {
  const files = Array.from(event?.target?.files || [])
  await uploadFiles(files)
  event.target.value = ''
}

const onDropFiles = async (event) => {
  const files = Array.from(event?.dataTransfer?.files || []).filter((file) => file.type.startsWith('image/'))
  await uploadFiles(files)
}

const removeImage = (index) => {
  form.imageUrls.splice(index, 1)
}

const resolvePreviewImage = (url) => normalizeMediaUrl(url)

const saveDraft = () => {
  const draft = {
    title: form.title,
    summary: form.summary,
    category: form.category,
    tagsInput: form.tagsInput,
    contentHtml: form.contentHtml,
    imageUrls: form.imageUrls,
    aiWorkId: form.aiWorkId,
    savedAt: new Date().toISOString()
  }
  localStorage.setItem(currentDraftKey.value, JSON.stringify(draft))
  setMessage('ok', '草稿已保存到本地')
}

const restoreDraft = () => {
  const raw = localStorage.getItem(currentDraftKey.value)
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    form.title = draft?.title || ''
    form.summary = draft?.summary || ''
    form.category = draft?.category || ''
    form.tagsInput = draft?.tagsInput || ''
    form.contentHtml = draft?.contentHtml || ''
    form.imageUrls = normalizeMediaUrls(Array.isArray(draft?.imageUrls) ? draft.imageUrls.slice(0, 9) : [])
    form.aiWorkId = draft?.aiWorkId || null
    if (editorRef.value) {
      editorRef.value.innerHTML = form.contentHtml
    }
    setMessage('ok', '已恢复上次草稿')
  } catch (error) {
    localStorage.removeItem(currentDraftKey.value)
  }
}

const resetForm = () => {
  form.title = ''
  form.summary = ''
  form.category = ''
  form.tagsInput = ''
  form.contentHtml = ''
  form.imageUrls = []
  form.aiWorkId = null
  if (editorRef.value) {
    editorRef.value.innerHTML = ''
  }
}

const submitPost = async () => {
  if (!collectiblesAuthAPI.getToken()) {
    setMessage('error', `请先登录后再${isEditMode.value ? '保存' : '发布'}文章`)
    router.push({ path: '/user-login', query: { redirect: route.fullPath } })
    return
  }
  if (!form.title.trim()) {
    setMessage('error', '请填写文章标题')
    return
  }
  if (!form.category) {
    setMessage('error', '请选择文章分类')
    return
  }
  if (!stripHtml(form.contentHtml)) {
    setMessage('error', '请填写文章正文')
    return
  }
  publishing.value = true
  try {
    const payload = {
      title: form.title.trim(),
      summary: form.summary.trim(),
      category: form.category,
      tags: parseTags(form.tagsInput),
      contentHtml: form.contentHtml,
      imageUrls: form.imageUrls,
      aiWorkId: form.aiWorkId
    }
    let result = null
    if (isEditMode.value) {
      result = await communityAPI.updatePost(editPostId.value, payload)
    } else {
      result = await communityAPI.createPost(payload)
    }
    localStorage.removeItem(currentDraftKey.value)
    resetForm()
    setMessage('ok', isEditMode.value ? '保存成功' : '发布成功')
    const postId = Number(result?.id || editPostId.value || 0)
    window.setTimeout(() => {
      if (postId > 0) {
        router.push(`/ceramics/community/post/${postId}`)
        return
      }
      router.push('/ceramics/community')
    }, 900)
  } catch (error) {
    setMessage('error', error?.message || (isEditMode.value ? '保存失败' : '发布失败'))
  } finally {
    publishing.value = false
  }
}

const goBackHome = () => {
  router.push('/ceramics/community')
}

const loadPostForEdit = async (postId) => {
  if (!postId) return
  const post = await communityAPI.getPostDetail(postId)
  form.title = post?.title || ''
  form.summary = post?.summary || ''
  form.category = post?.category || ''
  form.tagsInput = Array.isArray(post?.tags) ? post.tags.join(' ') : ''
  form.contentHtml = post?.contentHtml || ''
  form.imageUrls = normalizeMediaUrls(Array.isArray(post?.imageUrls) ? post.imageUrls.slice(0, 9) : [])
  form.aiWorkId = post?.aiWorkId || null
  if (editorRef.value) {
    editorRef.value.innerHTML = form.contentHtml
  }
}

const loadMyWorks = async () => {
  if (!collectiblesAuthAPI.getToken()) {
    aiWorks.value = []
    return
  }
  try {
    const works = await listCeramicWorks('permanent', 50)
    aiWorks.value = Array.isArray(works) ? works : []
  } catch (error) {
    aiWorks.value = []
  }
}

const initPage = async () => {
  if (initializing.value) return
  initializing.value = true
  try {
    if (isEditMode.value) {
      await loadPostForEdit(editPostId.value)
    } else {
      resetForm()
    }
    await loadMyWorks()
    restoreDraft()
    if (!isEditMode.value) {
      const requestedWorkId = Number(route.query.aiWorkId || 0)
      if (requestedWorkId > 0 && aiWorks.value.some((item) => Number(item.id) === requestedWorkId)) {
        form.aiWorkId = requestedWorkId
      }
    }
  } catch (error) {
    setMessage('error', error?.message || '加载文章失败')
    if (isEditMode.value) {
      router.replace('/ceramics/community')
    }
  } finally {
    initializing.value = false
  }
}

watch(
  () => route.query.postId,
  () => {
    initPage()
  }
)

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: #efefef;
  color: #2b2b2b;
}

.publish-main {
  width: min(1120px, 94vw);
  margin: 0 auto;
  padding: 44px 0 56px;
  display: grid;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  border-bottom: 2px solid rgba(var(--ym-accent-rgb), 0.36);
  padding-bottom: 14px;
}

.head-title h1 {
  margin: 6px 0 10px;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: clamp(2rem, 4vw, 2.8rem);
  line-height: 1.1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.head-title p:last-child {
  margin: 0;
  color: #5d646d;
  font-size: 1rem;
}

.head-icon {
  width: 1.2em;
  height: 1.2em;
  color: rgba(var(--ym-accent-rgb), 0.9);
}

.kicker {
  margin: 0;
  font-size: 0.75rem;
  color: #848b93;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn-primary,
.btn-ghost,
.btn-muted {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 10px 24px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: rgba(var(--ym-accent-rgb), 0.9);
  color: #fff8ee;
  border-color: rgba(var(--ym-accent-rgb), 0.82);
}

.btn-primary:hover {
  filter: saturate(1.08);
}

.btn-ghost {
  background: #f1b87a;
  color: #fffdf8;
}

.btn-ghost:hover {
  background: #eba962;
}

.btn-muted {
  background: #76808a;
  color: #f6f7f9;
}

.btn-muted:hover {
  background: #6a7480;
}

.btn-muted:disabled,
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-icon {
  width: 1.05em;
  height: 1.05em;
}

.form-card {
  background: #f6f6f6;
  border: 1px solid #e2e2e2;
  border-radius: 16px;
  padding: 22px 24px;
  display: grid;
  gap: 14px;
}

.form-card h2 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.8rem;
}

.field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block span {
  font-size: 1.03rem;
  font-weight: 600;
}

.field-block input,
.field-block textarea,
.field-block select {
  width: 100%;
  border: 1px solid #d4d5d8;
  border-radius: 12px;
  background: #fdfdfd;
  padding: 12px 14px;
  font-size: 1rem;
  color: #2b2b2b;
  font: inherit;
}

.field-block textarea {
  resize: vertical;
  min-height: 116px;
}

.field-block em {
  justify-self: end;
  font-style: normal;
  color: #8e949b;
  font-size: 0.9rem;
}

.editor-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tool-btn {
  border: 1px solid #d5d9de;
  border-radius: 10px;
  padding: 6px 12px;
  background: #f8f8f8;
  color: #445161;
  cursor: pointer;
  font-size: 0.9rem;
}

.tool-btn:hover {
  border-color: #b8c3cf;
}

.rich-editor {
  min-height: 360px;
  border: 1px solid #d7d9dd;
  border-radius: 12px;
  background: #ffffff;
  padding: 14px;
  line-height: 1.8;
  color: #2b2b2b;
  overflow-y: auto;
}

.rich-editor:empty::before {
  content: attr(data-placeholder);
  color: #9aa1a9;
}

.editor-foot {
  margin: 0;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #7a8188;
  font-size: 0.92rem;
}

.hidden-file {
  display: none;
}

.upload-box {
  border: 2px dashed #ebb078;
  border-radius: 14px;
  background: #faf7f2;
  min-height: 210px;
  display: grid;
  place-content: center;
  text-align: center;
  gap: 8px;
  cursor: pointer;
  padding: 14px;
}

.upload-icon {
  margin: 0;
  display: grid;
  place-items: center;
}

.upload-icon :deep(svg) {
  width: 44px;
  height: 44px;
  color: rgba(var(--ym-accent-rgb), 0.75);
}

.upload-title {
  margin: 0;
  font-size: 1.65rem;
  font-family: var(--ym-font-calligraphy-ma);
}

.upload-desc {
  margin: 0;
  color: #80868d;
}

.upload-preview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.preview-item {
  margin: 0;
  border: 1px solid #dedede;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.preview-item img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  display: block;
}

.preview-item button {
  width: 100%;
  border: 0;
  border-top: 1px solid #ececec;
  padding: 7px 10px;
  background: #f6f7f8;
  color: #b44a35;
  cursor: pointer;
}

.submit-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.submit-message {
  font-size: 0.94rem;
}

.submit-message.ok {
  color: #2f9a5f;
}

.submit-message.error {
  color: #c24747;
}

.preview-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.34);
  z-index: 30;
}

.preview-panel {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 32;
  width: min(860px, 92vw);
  max-height: 84vh;
  overflow-y: auto;
  background: #fefefe;
  border-radius: 16px;
  border: 1px solid #dfdfdf;
  padding: 18px 20px;
  display: grid;
  gap: 10px;
}

.preview-panel header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.preview-panel h3 {
  margin: 0;
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.5rem;
}

.preview-summary {
  margin: 0;
  color: #5d646c;
}

.preview-meta {
  margin: 0;
  display: flex;
  gap: 12px;
  color: #87909a;
  font-size: 0.92rem;
}

.preview-content {
  border-top: 1px solid #ececec;
  padding-top: 12px;
  line-height: 1.8;
}

@media (max-width: 920px) {
  .field-row {
    grid-template-columns: 1fr;
  }

  .upload-preview {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .publish-main {
    width: 95vw;
    padding: 30px 0 48px;
  }

  .page-head {
    flex-direction: column;
  }

  .head-actions {
    width: 100%;
  }

  .btn-ghost,
  .btn-muted {
    flex: 1;
    text-align: center;
  }

  .upload-preview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .submit-row {
    flex-wrap: wrap;
  }
}
</style>
