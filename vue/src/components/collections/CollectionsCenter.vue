<template>
  <div class="collections-center">
    <section v-if="showCatalog" class="panel">
      <header class="panel-head">
        <p class="eyebrow">COLLECTION CENTER</p>
        <h3>数字藏品列表</h3>
        <p>面向公众展示当前上架藏品；数字藏品仅支持通过兑换码获取，已获取藏品会在“我的藏品”长期保留。</p>
      </header>

      <div class="list-tools">
        <label class="tool-field tool-search">
          <span>关键词</span>
          <input
            v-model.trim="filters.keyword"
            type="text"
            placeholder="按名称 / 系列 / 描述搜索"
          />
        </label>
        <label class="tool-field">
          <span>系列</span>
          <select v-model="filters.series">
            <option value="">全部系列</option>
            <option v-for="series in seriesList" :key="series.id" :value="String(series.id)">{{ series.name }}</option>
          </select>
        </label>
        <label class="tool-field">
          <span>稀有度</span>
          <select v-model="filters.rarity">
            <option value="">全部稀有度</option>
            <option v-for="rarity in rarityOptions" :key="rarity" :value="rarity">{{ rarity }}</option>
          </select>
        </label>
        <label class="tool-field">
          <span>获取状态</span>
          <select v-model="filters.status">
            <option value="all">全部状态</option>
            <option value="owned">仅看已获取</option>
            <option value="unowned">仅看待兑换</option>
          </select>
        </label>
        <label class="tool-field">
          <span>排序方式</span>
          <select v-model="filters.sort">
            <option value="latest">按上新时间</option>
            <option value="rarity">按稀有度</option>
            <option value="name">按名称</option>
          </select>
        </label>
        <button type="button" class="secondary-btn reset-btn" @click="resetFilters">重置筛选</button>
      </div>

      <div class="stats-row">
        <article class="stat-chip">
          <strong>{{ onShelfItems.length }}</strong>
          <span>上架藏品</span>
        </article>
        <article class="stat-chip">
          <strong>{{ seriesList.length }}</strong>
          <span>系列数量</span>
        </article>
        <article class="stat-chip">
          <strong>{{ ownedInView }}</strong>
          <span>当前已获取</span>
        </article>
        <article class="stat-chip">
          <strong>{{ activeFilterCount }}</strong>
          <span>启用筛选</span>
        </article>
      </div>

      <p v-if="loading" class="list-summary">数据加载中...</p>
      <p v-else class="list-summary">当前展示 {{ filteredItems.length }} 件，已获取 {{ ownedInView }} 件。</p>

      <div class="list-grid">
        <article
          v-for="(item, index) in filteredItems"
          :key="item.id"
          :class="['item-card', rarityClass(item), cardShapeClass(index)]"
        >
          <div class="item-cover">
            <img :src="resolveCoverUrl(item)" :alt="item.name" loading="lazy" @error="handleCoverError($event, item)" />
            <div class="cover-overlay">
              <span class="badge">{{ item.rarity }}</span>
              <span class="item-code">#{{ item.itemCode || item.id }}</span>
            </div>
          </div>
          <div class="item-body">
            <div class="item-network-row">
              <span class="network-tag">YAOCHAIN · NFT</span>
              <span class="mint-tag">{{ mintedLabel(item) }}</span>
            </div>
            <div class="item-row">
              <h4>{{ item.name }}</h4>
              <span class="meta-series">{{ item.series }}</span>
            </div>
            <p class="desc">{{ itemDisplayDescription(item) }}</p>
            <p class="item-story">藏品叙事：{{ item.series }} · {{ item.rarity }}，面向非遗数字传播长期沉淀。</p>
            <div class="state-row">
              <span :class="['state-pill', isOwned(item.id) ? 'owned' : 'unowned']">
                {{ isOwned(item.id) ? '已获取' : '待兑换' }}
              </span>
              <span class="state-pill shelf">上架中</span>
            </div>
            <div class="action-row">
              <span class="redeem-tip-inline">{{ isOwned(item.id) ? '已通过兑换码获取' : '仅支持兑换码获取' }}</span>
              <button type="button" class="secondary-btn detail-btn" @click="openItemDetail(item.id)">
                查看详情与3D
              </button>
            </div>
          </div>
        </article>
      </div>

      <p v-if="onShelfItems.length === 0" class="empty">暂无上架藏品。</p>
      <p v-else-if="filteredItems.length === 0" class="empty">未匹配到符合筛选条件的藏品。</p>
    </section>

    <section v-if="showMine" class="panel">
      <header class="panel-head">
        <p class="eyebrow">MY COLLECTIONS</p>
        <h3>我的数字藏品</h3>
      </header>

      <div v-if="isLoggedIn" class="progress-board">
        <article class="progress-card overall">
          <p class="progress-title">总获取进度</p>
          <h4>{{ completionStats.owned }} / {{ completionStats.total }}</h4>
          <div class="progress-track">
            <span class="progress-bar" :style="{ width: `${completionStats.percent}%` }"></span>
          </div>
          <p class="progress-desc">完成率 {{ completionStats.percent }}%</p>
        </article>
        <article class="progress-card">
          <p class="progress-title">系列完成度</p>
          <div class="series-list">
            <div v-for="item in seriesProgress" :key="item.series" class="series-row">
              <span>{{ item.series }}</span>
              <span>{{ item.owned }}/{{ item.total }} ({{ item.percent }}%)</span>
            </div>
          </div>
        </article>
        <article class="progress-card">
          <p class="progress-title">成就徽章</p>
          <div class="badge-wall">
            <div
              v-for="badge in earnedBadges"
              :key="badge.id"
              :class="['badge-chip', badge.unlocked ? 'unlocked' : 'locked']"
            >
              <strong>{{ badge.title }}</strong>
              <span>{{ badge.description }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-if="isLoggedIn" class="owned-grid">
        <article v-for="item in userOwnedItems" :key="item.id" class="owned-card">
          <img :src="resolveCoverUrl(item)" :alt="item.name" loading="lazy" @error="handleCoverError($event, item)" />
          <div>
            <h4>{{ item.name }}</h4>
            <p>{{ item.series }} · {{ item.rarity }}</p>
            <p :class="['shelf-state', item.onShelf ? 'up' : 'down']">
              {{ item.onShelf ? '上架中' : '已下架（已拥有不受影响）' }}
            </p>
            <button type="button" class="secondary-btn detail-btn" @click="openItemDetail(item.id)">
              查看详情与3D
            </button>
          </div>
        </article>
      </div>
      <p v-if="isLoggedIn && userOwnedItems.length === 0" class="empty">你还没有通过兑换码获取任何藏品。</p>
      <p v-if="!isLoggedIn" class="empty">
        登录后可查看个人数字藏品。
        <router-link to="/user-login" class="empty-link">去登录</router-link>
      </p>
    </section>

    <section v-if="showRedeem" class="panel">
      <header class="panel-head">
        <p class="eyebrow">REDEEM</p>
        <h3>兑换码领取数字藏品</h3>
        <p>用于线下非遗活动发放兑换码，兑换后直接进入用户收藏库。</p>
      </header>

      <form v-if="isAdmin" class="redeem-form" @submit.prevent="submitRedeem">
        <input v-model="redeemCode" type="text" placeholder="请输入兑换码，例如 YMXC-2026-001" />
        <button type="submit" class="action-btn">立即兑换</button>
      </form>

      <section v-if="!isAdmin" class="redeem-empty-state">
        <div class="redeem-empty-copy">
          <strong>还没有兑换码？</strong>
          <p>数字藏品仅通过线下导览、研学活动、公开展陈和专题传播活动发放，不开放自由领取。</p>
        </div>
        <form class="redeem-form redeem-entry-form" @submit.prevent="submitRedeem">
          <input v-model="redeemCode" type="text" placeholder="请输入兑换码，例如 YMXC-2026-001" />
          <button type="submit" class="action-btn">立即兑换</button>
        </form>
        <p class="redeem-entry-tip">
          已经拿到兑换码可直接输入领取；没有兑换码可按下方方式参与线下活动后获取。
        </p>
        <div class="redeem-empty-grid">
          <article class="redeem-empty-card">
            <strong>线下活动领取</strong>
            <p>参加非遗导览、柴烧体验或校园展示活动，现场完成签到后领取兑换码。</p>
          </article>
          <article class="redeem-empty-card">
            <strong>联系活动方补发</strong>
            <p>若你已经参加活动但未拿到兑换码，可联系组织方或管理员核验后补发。</p>
          </article>
          <article class="redeem-empty-card">
            <strong>先登录再领取</strong>
            <p>建议先完成登录，后续拿到兑换码后可直接入库到你的个人藏品页。</p>
          </article>
        </div>
        <div class="redeem-empty-actions">
          <router-link class="secondary-btn redeem-link-btn" to="/ceramics/about/visit">
            查看线下参与方式
          </router-link>
          <router-link v-if="!isLoggedIn" class="action-btn redeem-link-btn" to="/ceramics/user-login">
            去登录账号
          </router-link>
        </div>
      </section>

      <div v-if="isAdmin" class="admin-issue">
        <h4>管理员：生成兑换码</h4>
        <div class="issue-row">
          <input v-model="issueForm.code" type="text" placeholder="兑换码" />
          <select v-model="issueForm.itemId">
            <option disabled value="">选择藏品</option>
            <option v-for="item in catalog" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
          <button type="button" class="secondary-btn" @click="submitIssueCode">生成兑换码</button>
        </div>
        <div class="issue-row issue-row-2">
          <input v-model="issueForm.issuedChannel" type="text" placeholder="发放渠道（默认：线下非遗活动）" />
          <input v-model="issueForm.expireAt" type="text" placeholder="过期时间 yyyy-MM-dd HH:mm:ss（可空）" />
        </div>

        <div class="redeem-admin-board">
          <header class="redeem-admin-head">
            <h5>兑换码管理</h5>
            <p>查看当前兑换码状态，未使用的兑换码可直接作废。</p>
          </header>
          <div v-if="redeemCodeRecords.length === 0" class="empty redeem-empty">暂无兑换码记录。</div>
          <div v-else class="redeem-code-list">
            <article v-for="record in redeemCodeRecords" :key="record.id" class="redeem-code-card">
              <div class="redeem-code-main">
                <strong>{{ record.code }}</strong>
                <span :class="['redeem-status', redeemStatusClass(record.status)]">{{ record.statusLabel || '未知' }}</span>
              </div>
              <p>藏品：{{ record.itemName || `ID ${record.itemId}` }}</p>
              <p>渠道：{{ record.issuedChannel || '线下非遗活动' }}</p>
              <p>过期：{{ formatDateTime(record.expireAt) }}</p>
              <p>使用人：{{ record.usedByName || '--' }}</p>
              <p>使用时间：{{ formatDateTime(record.usedAt) }}</p>
              <div class="redeem-code-actions">
                <button
                  type="button"
                  class="secondary-btn danger-btn"
                  :disabled="Number(record.status) !== 1"
                  @click="invalidateCode(record)"
                >
                  作废兑换码
                </button>
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section v-if="showAdminGate" class="panel admin-gate">
      <header class="panel-head">
        <p class="eyebrow">ADMIN ACCESS</p>
        <h3>{{ isLoggedIn ? '当前账号没有后台权限' : '请先登录管理员账号' }}</h3>
        <p>
          {{
            isLoggedIn
              ? '该页面只对管理员开放。请切换到管理员账号后再进入藏品管理。'
              : '你已进入后台管理路由，但当前还未登录，所以管理面板不会显示。'
          }}
        </p>
      </header>

      <div class="admin-gate-actions">
        <button type="button" class="action-btn" @click="goAdminLogin">
          {{ isLoggedIn ? '切换管理员登录' : '去登录' }}
        </button>
        <button type="button" class="secondary-btn" @click="goHome">返回首页</button>
      </div>
    </section>

    <section v-if="showAdminPanel" class="panel admin-panel">
      <header class="panel-head">
        <p class="eyebrow">ADMIN</p>
        <h3>藏品增删改查管理</h3>
        <p>支持上新、编辑、删除、上下架与 GLB 上传；已被用户兑换获取的藏品仅允许下架，不允许删除。</p>
      </header>

      <form class="new-item-form" @submit.prevent="submitNewItem">
        <div class="admin-form-shell">
          <section class="admin-form-main admin-section">
            <header class="admin-section-head">
              <h4>基础信息</h4>
              <p>先完成藏品主信息，再补充资源地址和说明文案。</p>
            </header>

            <div class="admin-fields">
              <label class="field-card">
                <span>藏品编码</span>
                <input v-model="newItem.itemCode" type="text" placeholder="如 C1001 / C2026A" />
              </label>

              <label class="field-card">
                <span>藏品名称</span>
                <input v-model="newItem.name" type="text" placeholder="输入对外展示名称" />
              </label>

              <label class="field-card">
                <span>所属系列</span>
                <select v-model="newItem.seriesId">
                  <option disabled value="">选择系列</option>
                  <option v-for="series in seriesList" :key="series.id" :value="series.id">{{ series.name }}</option>
                </select>
              </label>

              <label class="field-card">
                <span>稀有度</span>
                <select v-model="newItem.rarityLevel">
                  <option :value="1">基础款 (Lv.1)</option>
                  <option :value="2">传承款 (Lv.2)</option>
                  <option :value="3">典藏款 (Lv.3)</option>
                  <option :value="4">限藏款 (Lv.4)</option>
                  <option :value="5">臻藏款 (Lv.5)</option>
                </select>
              </label>

              <label class="field-card field-full">
                <span>封面地址</span>
                <input v-model="newItem.coverUrl" type="text" placeholder="上传成功后会自动回填 OSS 地址，也可手动粘贴" />
              </label>

              <label class="field-card field-full">
                <span>GLB 地址</span>
                <input v-model="newItem.modelPath" type="text" placeholder="上传成功后会自动回填 GLB 绝对地址" />
              </label>

              <label class="field-card field-full">
                <span>藏品描述</span>
                <textarea v-model="newItem.description" rows="4" placeholder="补充器型特点、展示场景、活动定位等说明"></textarea>
              </label>
            </div>
          </section>

          <aside class="admin-form-side admin-section">
            <header class="admin-section-head">
              <h4>资源上传</h4>
              <p>封面与模型资源分开处理，优先保证封面可预览、模型可打开。</p>
            </header>

            <div class="upload-card">
              <div class="upload-copy">
                <strong>封面图片</strong>
                <span>支持常见图片格式，建议按 4:3 出图。</span>
              </div>
              <div class="upload-row">
                <input type="file" accept="image/*" @change="onCoverSelect" />
                <button type="button" class="secondary-btn upload-btn" :disabled="!selectedCoverFile || coverUploading" @click="submitCoverUpload">
                  {{ coverUploading ? '封面上传中...' : '上传封面到 OSS' }}
                </button>
              </div>
            </div>

            <div class="upload-card">
              <div class="upload-copy">
                <strong>GLB 模型</strong>
                <span>用于详情页 3D 预览，上传后自动回填模型地址。</span>
              </div>
              <div class="upload-row">
                <input type="file" accept=".glb" @change="onGlbSelect" />
                <button type="button" class="secondary-btn upload-btn" :disabled="!selectedGlbFile || glbUploading" @click="submitGlbUpload">
                  {{ glbUploading ? 'GLB 上传中...' : '上传 GLB 到 OSS' }}
                </button>
              </div>
            </div>

            <div v-if="adminPreviewCoverUrl" class="cover-preview">
              <img
                :src="adminPreviewCoverUrl"
                alt="藏品封面预览"
                @error="handleCoverError($event, { coverUrl: '', modelPath: newItem.modelPath, itemCode: newItem.itemCode })"
              />
              <span>当前封面预览</span>
            </div>
            <div v-else class="cover-preview cover-preview-empty">
              <span>上传或填写封面地址后，可在此预览藏品封面。</span>
            </div>
          </aside>
        </div>

        <div class="form-actions">
          <button type="submit" class="action-btn primary-submit">{{ editingItemId ? '保存修改' : '上新藏品' }}</button>
          <button v-if="editingItemId" type="button" class="secondary-btn" @click="resetAdminForm">取消编辑</button>
        </div>
      </form>

      <div class="manage-list">
        <article v-for="item in managementItems" :key="item.id" class="manage-item">
          <div class="manage-summary">
            <div class="manage-meta">
              <h4>{{ item.name }}</h4>
              <p>{{ item.series }} · {{ item.rarity }}</p>
              <p class="model-path">{{ modelAssetLabel(item) }}</p>
            </div>
            <div class="manage-actions">
              <button type="button" class="secondary-btn" @click="beginEditItem(item)">编辑</button>
              <button type="button" class="secondary-btn" @click="toggleShelf(item.id)">
                {{ item.onShelf ? '下架' : '上架' }}
              </button>
              <button type="button" class="secondary-btn danger-btn" @click="removeItem(item)">删除</button>
            </div>
          </div>

          <div class="item-redeem-panel">
            <div class="item-redeem-head">
              <div>
                <strong>兑换码管理</strong>
                <span>每个藏品固定维护一条兑换码，只保留生效和失效两种管理动作。</span>
              </div>
              <span class="item-redeem-count">{{ getItemRedeemCode(item.id)?.statusLabel || '未配置' }}</span>
            </div>

            <div class="item-redeem-inline">
              <input
                v-model.trim="getRedeemDraft(item.id).code"
                type="text"
                :placeholder="`${item.itemCode || 'ITEM'}-001`"
              />
              <input
                v-model.trim="getRedeemDraft(item.id).issuedChannel"
                type="text"
                placeholder="发放渠道，如线下展陈 / 答辩展示"
              />
              <input
                v-model.trim="getRedeemDraft(item.id).expireAt"
                type="text"
                placeholder="过期时间 yyyy-MM-dd HH:mm:ss，可空"
              />
              <span
                :class="[
                  'item-redeem-status-inline',
                  getItemRedeemCode(item.id) ? redeemStatusClass(getItemRedeemCode(item.id).status) : 'empty'
                ]"
              >
                {{ getItemRedeemCode(item.id)?.statusLabel || '未配置' }}
              </span>
              <button type="button" class="secondary-btn" @click="submitItemRedeemCode(item)">
                {{ getItemRedeemCode(item.id) ? '保存兑换码' : '创建兑换码' }}
              </button>
              <div v-if="getItemRedeemCode(item.id)" class="item-redeem-actions-inline">
                <button
                  v-if="Number(getItemRedeemCode(item.id).status) === 1"
                  type="button"
                  class="secondary-btn danger-btn"
                  @click="invalidateCode(getItemRedeemCode(item.id))"
                >
                  设为失效
                </button>
                <button
                  v-else-if="Number(getItemRedeemCode(item.id).status) === 0"
                  type="button"
                  class="secondary-btn success-btn"
                  @click="activateCode(getItemRedeemCode(item.id))"
                >
                  重新生效
                </button>
                <span v-else class="item-redeem-note">当前状态不可切换</span>
              </div>
            </div>

            <div v-if="getItemRedeemCode(item.id)" class="item-redeem-meta">
              <span>使用人：{{ getItemRedeemCode(item.id).usedByName || '--' }}</span>
              <span>更新时间：{{ formatDateTime(getItemRedeemCode(item.id).updatedAt) }}</span>
            </div>
            <p v-else class="item-redeem-empty">当前藏品尚未配置兑换码，可填写后保存启用。</p>
          </div>
        </article>
      </div>
      <p v-if="managementItems.length === 0" class="empty">暂无可管理藏品。</p>
    </section>

    <p v-if="feedback.message" :class="['feedback', feedback.ok ? 'success' : 'error']">{{ feedback.message }}</p>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCollectiblesCenter } from '@/composables/useCollectiblesCenter'
import { normalizeMediaUrl } from '@/utils/collectiblesApi'

const props = defineProps({
  viewMode: {
    type: String,
    default: 'all'
  }
})

const {
  loading,
  catalog,
  adminCatalog,
  redeemCodeRecords,
  seriesList,
  onShelfItems,
  isLoggedIn,
  isAdmin,
  userOwnedItems,
  completionStats,
  seriesProgress,
  earnedBadges,
  isOwned,
  redeemByCode,
  refreshAll,
  uploadGlbModel,
  uploadCoverImage,
  upsertCatalogItem,
  toggleShelfStatus,
  deleteCatalogItem,
  issueRedeemCode,
  invalidateRedeemCode,
  activateRedeemCode
} = useCollectiblesCenter({
  preferAdminCatalog: props.viewMode === 'admin'
})

const feedback = reactive({
  ok: true,
  message: ''
})

const setFeedback = (ok, message) => {
  feedback.ok = ok
  feedback.message = message
}

const redeemCode = ref('')

const issueForm = reactive({
  code: '',
  itemId: '',
  issuedChannel: '',
  expireAt: ''
})

const redeemDrafts = reactive({})

const newItem = reactive({
  itemCode: '',
  name: '',
  seriesId: '',
  rarityLevel: 1,
  coverUrl: '',
  modelPath: '',
  description: ''
})

const filters = reactive({
  keyword: '',
  series: '',
  rarity: '',
  status: 'all',
  sort: 'latest'
})

const rarityRank = {
  基础款: 1,
  传承款: 2,
  典藏款: 3,
  限藏款: 4,
  臻藏款: 5
}

const rarityOptions = computed(() => [...new Set(onShelfItems.value.map(item => item.rarity))])
const selectedGlbFile = ref(null)
const selectedCoverFile = ref(null)
const glbUploading = ref(false)
const coverUploading = ref(false)
const editingItemId = ref(null)
const router = useRouter()
const DEFAULT_COVER = '/vcg-flambe-vase-museum.webp'
const coverFallbackByCode = {
  C1963514: '/vcg-flambe-vase-museum.webp',
  CERAMICVASE01: '/vcg-kiln-vessels-row.webp',
  C1930728: '/vcg-olive-vase-closeup.webp'
}
const coverFallbackByModelPath = [
  {
    keywords: ['9d3545802a8249dc9221b54e5f3c145b', '1963.514_vase'],
    cover: '/vcg-flambe-vase-museum.webp'
  },
  {
    keywords: ['55733fb87b7e4099951f69d9daf95d83', 'ceramic_vase'],
    cover: '/vcg-kiln-vessels-row.webp'
  },
  {
    keywords: ['1b874dc1832042d88a814692b7da6d8a', '1930.728_vase_with_trophy-heads_and_warriors'],
    cover: '/vcg-olive-vase-closeup.webp'
  }
]
const normalizedMode = computed(() => {
  const validModes = ['all', 'catalog', 'mine', 'admin']
  return validModes.includes(props.viewMode) ? props.viewMode : 'all'
})
const showCatalog = computed(() => ['all', 'catalog'].includes(normalizedMode.value))
const showMine = computed(() => ['all', 'mine'].includes(normalizedMode.value))
const showRedeem = computed(() => ['all', 'mine', 'catalog'].includes(normalizedMode.value))
const showAdminPanel = computed(() => isAdmin.value && ['all', 'admin'].includes(normalizedMode.value))
const showAdminGate = computed(() => normalizedMode.value === 'admin' && !showAdminPanel.value)
const managementItems = computed(() => (props.viewMode === 'admin' ? adminCatalog.value : (adminCatalog.value.length ? adminCatalog.value : catalog.value)))

const filteredItems = computed(() => {
  const keyword = filters.keyword.toLowerCase()
  const items = onShelfItems.value.filter((item) => {
    const searchable = `${item.name} ${item.series} ${item.description}`.toLowerCase()
    const keywordMatch = !keyword || searchable.includes(keyword)
    const seriesMatch = !filters.series || String(item.seriesId) === filters.series
    const rarityMatch = !filters.rarity || item.rarity === filters.rarity
    const statusMatch = filters.status === 'all'
      || (filters.status === 'owned' && isOwned(item.id))
      || (filters.status === 'unowned' && !isOwned(item.id))

    return keywordMatch && seriesMatch && rarityMatch && statusMatch
  })

  return items.sort((a, b) => {
    if (filters.sort === 'name') {
      return a.name.localeCompare(b.name, 'zh-Hans-CN')
    }
    if (filters.sort === 'rarity') {
      const left = b.rarityLevel || rarityRank[b.rarity] || 0
      const right = a.rarityLevel || rarityRank[a.rarity] || 0
      return left - right
    }
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

const ownedInView = computed(() => filteredItems.value.filter(item => isOwned(item.id)).length)
const activeFilterCount = computed(() => {
  const filterFlags = [
    Boolean(filters.keyword),
    Boolean(filters.series),
    Boolean(filters.rarity),
    filters.status !== 'all',
    filters.sort !== 'latest'
  ]
  return filterFlags.filter(Boolean).length
})

const getModelPathNormalized = (item) => {
  return String(item?.modelPath || item?.modelUrl || '').toLowerCase()
}

const resolveCoverByModelPath = (item) => {
  const path = getModelPathNormalized(item)
  if (!path) return ''
  const matched = coverFallbackByModelPath.find((entry) => entry.keywords.some(keyword => path.includes(keyword)))
  return matched?.cover || ''
}

const resolveCoverUrl = (item) => {
  const raw = String(item?.coverUrl || '').trim()
  if (raw && !raw.toLowerCase().endsWith('.glb')) {
    return normalizeMediaUrl(raw)
  }
  const modelMatchedCover = resolveCoverByModelPath(item)
  if (modelMatchedCover) {
    return modelMatchedCover
  }
  if (!raw || raw.toLowerCase().endsWith('.glb')) {
    return coverFallbackByCode[item?.itemCode] || DEFAULT_COVER
  }
  return normalizeMediaUrl(raw)
}

const adminPreviewCoverUrl = computed(() => resolveCoverUrl({
  coverUrl: newItem.coverUrl,
  modelPath: newItem.modelPath,
  itemCode: newItem.itemCode
}))

const handleCoverError = (event, item) => {
  const target = event?.target
  if (!target) return

  const modelFallback = resolveCoverByModelPath(item)
  if (modelFallback && target.dataset.fallbackStage !== 'model') {
    target.dataset.fallbackStage = 'model'
    target.src = modelFallback
    return
  }

  const codeFallback = coverFallbackByCode[item?.itemCode]
  if (codeFallback && target.dataset.fallbackStage !== 'code') {
    target.dataset.fallbackStage = 'code'
    target.src = codeFallback
    return
  }

  if (target.dataset.fallbackStage !== 'default') {
    target.dataset.fallbackStage = 'default'
    target.src = DEFAULT_COVER
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.series = ''
  filters.rarity = ''
  filters.status = 'all'
  filters.sort = 'latest'
}

const rarityClass = (item) => {
  const rarity = String(item?.rarity || '')
  if (rarity.includes('限藏')) return 'rarity-legend'
  if (rarity.includes('典藏')) return 'rarity-epic'
  if (rarity.includes('传承')) return 'rarity-rare'
  return 'rarity-common'
}

const cardShapeClass = (index) => {
  const classes = ['shape-a', 'shape-b', 'shape-c']
  return classes[index % classes.length]
}

const mintedLabel = (item) => {
  const year = Number.parseInt(String(item?.createdAt || '').slice(0, 4), 10)
  return `Minted ${Number.isFinite(year) ? year : '2026'}`
}

const itemDisplayDescription = (item) => {
  const raw = String(item?.description || '').trim()
  if (!raw) return '该藏品正在持续补充叙事说明。'
  const cleaned = raw.replace(/https?:\/\/\S+/gi, '').replace(/\s{2,}/g, ' ').trim()
  if (!cleaned || /^wsnlzg\.oss-cn-shenzhen\.aliyuncs\.com\/.+/i.test(cleaned)) {
    return '该藏品聚焦柴烧器型、窑火肌理与非遗传播价值。'
  }
  return cleaned
}

const modelAssetLabel = (item) => {
  const hasModel = Boolean(String(item?.modelPath || item?.modelUrl || '').trim())
  return hasModel ? '模型资源：已配置（隐藏原始地址）' : '模型资源：待上传'
}

const formatDateTime = (value) => {
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

const formatDateTimeInput = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const redeemStatusClass = (status) => {
  if (Number(status) === 2) return 'used'
  if (Number(status) === 4) return 'expired'
  if (Number(status) === 0) return 'invalid'
  return 'ready'
}

const getItemRedeemCode = (itemId) => {
  const matched = redeemCodeRecords.value
    .filter(record => Number(record.itemId) === Number(itemId))
    .sort((left, right) => Number(right.id || 0) - Number(left.id || 0))
  return matched[0] || null
}

const getRedeemDraft = (itemId) => {
  const key = String(itemId)
  const current = getItemRedeemCode(itemId)
  if (!redeemDrafts[key]) {
    redeemDrafts[key] = {
      code: current?.code || '',
      issuedChannel: current?.issuedChannel || '',
      expireAt: current?.expireAt ? formatDateTimeInput(current.expireAt) : ''
    }
    return redeemDrafts[key]
  }
  if (current) {
    redeemDrafts[key].code = current.code || ''
    redeemDrafts[key].issuedChannel = current.issuedChannel || ''
    redeemDrafts[key].expireAt = current.expireAt ? formatDateTimeInput(current.expireAt) : ''
  }
  return redeemDrafts[key]
}

const openItemDetail = (itemId) => {
  if (!itemId) return
  router.push(`/collections/item/${itemId}`)
}

const goAdminLogin = () => {
  router.push({
    path: '/ceramics/user-login',
    query: { redirect: '/ceramics/admin/collectibles' }
  })
}

const goHome = () => {
  router.push('/ceramics/home')
}

const submitRedeem = async () => {
  const result = await redeemByCode(redeemCode.value)
  setFeedback(result.ok, result.message)
  if (result.ok) {
    redeemCode.value = ''
  }
}

const submitIssueCode = async () => {
  const result = await issueRedeemCode(issueForm)
  setFeedback(result.ok, result.message)
  if (result.ok) {
    issueForm.code = ''
    issueForm.itemId = ''
    issueForm.issuedChannel = ''
    issueForm.expireAt = ''
  }
}

const invalidateCode = async (record) => {
  if (!record?.id) return
  const ok = window.confirm(`确认作废兑换码「${record.code}」？未使用的兑换码作废后将无法再兑换。`)
  if (!ok) return
  const result = await invalidateRedeemCode(record.id)
  setFeedback(result.ok, result.message)
}

const activateCode = async (record) => {
  if (!record?.id) return
  const ok = window.confirm(`确认将兑换码「${record.code}」重新设为生效状态？`)
  if (!ok) return
  const result = await activateRedeemCode(record.id)
  setFeedback(result.ok, result.message)
}

const submitItemRedeemCode = async (item) => {
  if (!item?.id) return
  const draft = getRedeemDraft(item.id)
  const result = await issueRedeemCode({
    code: draft.code,
    itemId: item.id,
    issuedChannel: draft.issuedChannel,
    expireAt: draft.expireAt
  })
  setFeedback(result.ok, result.message)
  if (result.ok) {
    const current = getItemRedeemCode(item.id)
    draft.code = current?.code || draft.code
    draft.issuedChannel = current?.issuedChannel || ''
    draft.expireAt = current?.expireAt ? formatDateTimeInput(current.expireAt) : ''
  }
}

const saveCurrentItem = async ({ resetAfterSuccess = true, successMessage = '' } = {}) => {
  const result = await upsertCatalogItem({
    id: editingItemId.value || null,
    itemCode: newItem.itemCode,
    name: newItem.name,
    seriesId: newItem.seriesId,
    rarityLevel: newItem.rarityLevel,
    coverUrl: newItem.coverUrl,
    modelPath: newItem.modelPath,
    description: newItem.description,
    onShelf: true
  })
  const message = successMessage || result.message
  setFeedback(result.ok, message)
  if (result.ok && resetAfterSuccess) {
    resetAdminForm()
  }
  return result
}

const submitNewItem = async () => {
  await saveCurrentItem()
}

const beginEditItem = (item) => {
  if (!item) return
  editingItemId.value = item.id
  newItem.itemCode = item.itemCode || ''
  newItem.name = item.name || ''
  newItem.seriesId = item.seriesId || ''
  newItem.rarityLevel = Number(item.rarityLevel || 1)
  newItem.coverUrl = item.coverUrl || ''
  newItem.modelPath = item.modelPath || ''
  newItem.description = item.description || ''
  selectedGlbFile.value = null
  selectedCoverFile.value = null
}

const resetAdminForm = () => {
  editingItemId.value = null
  newItem.itemCode = ''
  newItem.name = ''
  newItem.seriesId = ''
  newItem.rarityLevel = 1
  newItem.coverUrl = ''
  newItem.modelPath = ''
  newItem.description = ''
  selectedGlbFile.value = null
  selectedCoverFile.value = null
}

const toggleShelf = async (itemId) => {
  const result = await toggleShelfStatus(itemId)
  setFeedback(result.ok, result.message)
}

const removeItem = async (item) => {
  if (!item?.id) return
  const ok = window.confirm(`确认删除藏品「${item.name || item.id}」？该操作不可恢复。`)
  if (!ok) return
  const result = await deleteCatalogItem(item.id)
  setFeedback(result.ok, result.message)
  if (result.ok && editingItemId.value === item.id) {
    resetAdminForm()
  }
}

const onGlbSelect = (event) => {
  const file = event?.target?.files?.[0]
  selectedGlbFile.value = file || null
}

const onCoverSelect = (event) => {
  const file = event?.target?.files?.[0]
  selectedCoverFile.value = file || null
}

const submitGlbUpload = async () => {
  if (!selectedGlbFile.value) {
    setFeedback(false, '请先选择 .glb 文件')
    return
  }
  glbUploading.value = true
  const result = await uploadGlbModel(selectedGlbFile.value)
  glbUploading.value = false
  setFeedback(result.ok, result.message)
  if (result.ok && result.data?.modelUrl) {
    newItem.modelPath = result.data.modelUrl
  }
}

const submitCoverUpload = async () => {
  if (!selectedCoverFile.value) {
    setFeedback(false, '请先选择封面图片')
    return
  }
  coverUploading.value = true
  const result = await uploadCoverImage(selectedCoverFile.value)
  coverUploading.value = false
  if (!result.ok || !result.data?.coverUrl) {
    setFeedback(false, result.message)
    return
  }

  newItem.coverUrl = result.data.coverUrl
  selectedCoverFile.value = null

  if (editingItemId.value) {
    const saveResult = await saveCurrentItem({
      resetAfterSuccess: false,
      successMessage: '封面上传并保存成功。'
    })
    if (!saveResult.ok) {
      setFeedback(false, `封面已上传，但保存修改失败：${saveResult.message}`)
    }
    return
  }

  setFeedback(true, '封面上传成功，请继续点击“上新藏品”或“保存修改”。')
}

onMounted(async () => {
  try {
    await refreshAll()
  } catch (error) {
    setFeedback(false, error?.message || '初始化数据失败')
  }
})
</script>

<style scoped>
.collections-center {
  --panel-bg: rgba(255, 250, 240, 0.9);
  --surface: var(--ym-surface);
  --surface-soft: rgba(255, 255, 255, 0.88);
  --text-primary: var(--ym-text);
  --text-secondary: var(--ym-text-secondary);
  --text-muted: var(--ym-text-muted);
  --border: var(--ym-border);
  --border-strong: var(--ym-border-strong);
  --accent: var(--ym-accent);
  --accent-rgb: var(--ym-accent-rgb);
  --support: var(--ym-support);
  --support-rgb: var(--ym-support-rgb);
  --success: var(--ym-success);
  --success-bg: var(--ym-success-bg);
  --danger: var(--ym-danger);
  --danger-bg: var(--ym-danger-bg);
  display: grid;
  gap: 14px;
}

.panel {
  border: 1px solid var(--border);
  border-radius: 6px 22px 6px 22px;
  padding: 24px;
  background:
    linear-gradient(160deg, rgba(255, 251, 242, 0.94), rgba(252, 244, 231, 0.82));
  color: var(--text-primary);
  box-shadow: 0 12px 28px rgba(51, 44, 35, 0.05);
}

.panel-head h3 {
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
  margin: 8px 0 8px;
}

.panel-head p {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 0.93rem;
}

.eyebrow {
  font-size: 0.76rem;
  color: var(--text-muted);
  letter-spacing: 0.16em;
}

.list-tools {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1.4fr repeat(4, minmax(0, 1fr)) auto;
  gap: 8px;
  align-items: end;
}

.tool-field {
  border: 1px solid var(--border);
  border-radius: 4px 12px 4px 12px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.78);
  display: grid;
  gap: 5px;
}

.tool-field span {
  font-size: 0.74rem;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}

.tool-field :deep(input),
.tool-field :deep(select) {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: rgba(255, 250, 240, 0.95);
  font-size: 0.88rem;
  color: var(--text-primary);
}

.reset-btn {
  height: 42px;
}

.stats-row {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-chip {
  border: 1px solid var(--border);
  border-radius: 3px 12px 3px 12px;
  padding: 10px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 250, 240, 0.7));
  display: grid;
  gap: 3px;
}

.stat-chip strong {
  font-family: var(--ym-font-display);
  color: var(--accent);
}

.stat-chip span {
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.list-summary {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.list-grid,
.owned-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.item-card,
.owned-card {
  border: 1px solid var(--border);
  border-radius: 4px 20px 4px 20px;
  overflow: hidden;
  background: var(--surface);
  transition: transform 0.24s ease, border-color 0.24s ease, box-shadow 0.24s ease;
  position: relative;
  transform-origin: center;
  transform-style: preserve-3d;
}

.item-card:hover,
.owned-card:hover {
  transform: translateY(-7px) rotateX(3deg) rotateY(-2deg);
  border-color: rgba(var(--accent-rgb), 0.34);
  box-shadow: 0 22px 36px rgba(43, 43, 43, 0.16);
}

.item-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(var(--accent-rgb), 0.16), transparent 28%),
    linear-gradient(315deg, rgba(var(--support-rgb), 0.12), transparent 34%);
  opacity: 0.45;
  pointer-events: none;
}

.item-card::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  width: 34px;
  height: 34px;
  clip-path: polygon(100% 0, 0 0, 100% 100%);
  background: rgba(62, 56, 47, 0.12);
  pointer-events: none;
}

.item-card.shape-a {
  border-radius: 4px 24px 10px 28px;
}

.item-card.shape-b {
  border-radius: 18px 4px 24px 8px;
}

.item-card.shape-c {
  border-radius: 8px 26px 4px 24px;
}

.item-cover {
  position: relative;
}

.item-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent 35%, rgba(255, 255, 255, 0.34) 50%, transparent 64%);
  transform: translateX(-100%);
  transition: transform 0.45s ease;
  pointer-events: none;
}

.item-card:hover .item-cover::after {
  transform: translateX(100%);
}

.item-card img,
.owned-card img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.item-card:hover img,
.owned-card:hover img {
  transform: scale(1.03);
}

.cover-overlay {
  position: absolute;
  inset: auto 8px 8px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-body,
.owned-card > div {
  padding: 14px;
  position: relative;
  z-index: 1;
}

.item-network-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.artifact-strip {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.artifact-strip span {
  border: 1px solid rgba(var(--support-rgb), 0.24);
  border-radius: 3px 10px 4px 12px;
  padding: 4px 6px;
  font-size: 0.72rem;
  letter-spacing: 0.02em;
  color: var(--text-secondary);
  background: rgba(var(--support-rgb), 0.08);
  text-align: center;
}

.network-tag,
.mint-tag {
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  border: 1px solid var(--border);
  padding: 2px 8px;
}

.network-tag {
  border-radius: 3px 8px 3px 8px;
  color: var(--ym-text-muted);
  background: rgba(255, 255, 255, 0.72);
}

.mint-tag {
  border-radius: 8px 3px 8px 3px;
  color: var(--support);
  border-color: rgba(var(--support-rgb), 0.38);
  background: rgba(var(--support-rgb), 0.12);
}

.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.badge {
  border: 1px solid rgba(var(--accent-rgb), 0.35);
  color: var(--accent);
  border-radius: 3px 9px 3px 9px;
  padding: 3px 8px;
  font-size: 0.75rem;
  background: rgba(var(--accent-rgb), 0.08);
}

.item-code {
  font-size: 0.74rem;
  border-radius: 10px 2px 10px 2px;
  padding: 2px 8px;
  color: #fff7ea;
  background: rgba(43, 43, 43, 0.72);
}

.meta-series {
  font-size: 0.76rem;
  border-radius: 2px 8px 2px 8px;
  padding: 2px 8px;
  border: 1px solid var(--border-strong);
  color: var(--text-muted);
  background: rgba(255, 250, 240, 0.92);
}

.meta,
.desc,
.model-path {
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 0.9rem;
}

.item-story {
  margin-top: 4px;
  font-size: 0.82rem;
  line-height: 1.65;
  color: var(--text-muted);
}

.model-match {
  margin-top: 6px;
  font-size: 0.8rem;
  color: #4f3a2d;
  line-height: 1.7;
  padding: 6px 8px;
  border-left: 2px solid rgba(var(--accent-rgb), 0.45);
  background: rgba(255, 248, 236, 0.86);
}

.model-path {
  font-size: 0.8rem;
  color: var(--text-muted);
  background: rgba(var(--support-rgb), 0.08);
  border: 1px dashed rgba(var(--support-rgb), 0.3);
  border-radius: 4px 10px 4px 10px;
  padding: 4px 6px;
}

.model-ready {
  margin-top: 6px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  background: rgba(var(--support-rgb), 0.08);
  border: 1px solid rgba(var(--support-rgb), 0.2);
  border-radius: 4px 10px 4px 10px;
  padding: 4px 6px;
}

.state-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.state-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 3px 10px 3px 10px;
  padding: 2px 8px;
  font-size: 0.76rem;
}

.state-pill.owned {
  background: rgba(var(--support-rgb), 0.13);
  border: 1px solid rgba(var(--support-rgb), 0.32);
  color: var(--support);
}

.state-pill.unowned {
  background: rgba(var(--accent-rgb), 0.1);
  border: 1px solid rgba(var(--accent-rgb), 0.3);
  color: var(--accent);
}

.state-pill.shelf {
  background: rgba(79, 73, 64, 0.08);
  border: 1px solid var(--border-strong);
  color: var(--text-secondary);
}

.action-btn,
.secondary-btn {
  border: 1px solid var(--border-strong);
  border-radius: 4px 10px 4px 10px;
  padding: 8px 10px;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.action-btn {
  background: rgba(var(--accent-rgb), 0.1);
  border-color: rgba(var(--accent-rgb), 0.38);
  color: var(--text-primary);
}

.action-btn.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary-btn {
  background: var(--surface);
  color: var(--text-primary);
}

.action-btn:not(.disabled):hover,
.secondary-btn:hover {
  border-color: rgba(var(--accent-rgb), 0.42);
  background: rgba(var(--accent-rgb), 0.08);
}

.detail-btn {
  margin-top: 0;
}

.action-row {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.redeem-tip-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border: 1px dashed rgba(var(--support-rgb), 0.36);
  border-radius: 10px;
  padding: 0 10px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  background: rgba(var(--support-rgb), 0.08);
  text-align: center;
}

.redeem-empty-state {
  margin-top: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 249, 239, 0.9));
  display: grid;
  gap: 12px;
}

.redeem-empty-copy {
  display: grid;
  gap: 4px;
}

.redeem-empty-copy strong {
  color: var(--text-primary);
  font-size: 1rem;
}

.redeem-empty-copy p,
.redeem-empty-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.redeem-entry-form {
  margin-top: 0;
}

.redeem-entry-tip {
  margin: -2px 0 0;
  color: var(--text-muted);
  font-size: 0.82rem;
  line-height: 1.7;
}

.redeem-empty-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.redeem-empty-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.88);
  display: grid;
  gap: 6px;
}

.redeem-empty-card strong {
  color: var(--text-primary);
  font-size: 0.92rem;
}

.redeem-empty-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.redeem-link-btn {
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.rarity-legend {
  border-color: rgba(190, 90, 42, 0.45);
}

.rarity-legend .badge {
  border-color: rgba(190, 90, 42, 0.55);
  color: #b5482e;
  background: rgba(190, 90, 42, 0.12);
}

.rarity-epic {
  border-color: rgba(138, 96, 52, 0.45);
}

.rarity-rare {
  border-color: rgba(98, 121, 104, 0.45);
}

.progress-board {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.progress-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface-soft);
  padding: 10px;
}

.progress-title {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.progress-card h4 {
  margin: 6px 0;
}

.progress-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: rgba(79, 73, 64, 0.14);
  overflow: hidden;
}

.progress-bar {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, rgba(var(--accent-rgb), 0.9), rgba(var(--accent-rgb), 0.36));
  transition: width 0.6s ease;
  border-radius: 999px;
}

.progress-desc {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 0.84rem;
}

.series-list {
  margin-top: 6px;
  display: grid;
  gap: 6px;
}

.series-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 0.84rem;
  color: var(--text-secondary);
}

.badge-wall {
  margin-top: 6px;
  display: grid;
  gap: 6px;
}

.badge-chip {
  display: grid;
  gap: 4px;
  border-radius: 10px;
  padding: 8px;
  border: 1px solid var(--border);
}

.badge-chip span {
  font-size: 0.8rem;
}

.badge-chip.unlocked {
  background: rgba(var(--support-rgb), 0.13);
  border-color: rgba(var(--support-rgb), 0.34);
  color: var(--support);
}

.badge-chip.locked {
  background: rgba(79, 73, 64, 0.06);
  color: var(--text-muted);
}

.redeem-form {
  margin-top: 12px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.admin-issue {
  margin-top: 14px;
  border-top: 1px dashed var(--border-strong);
  padding-top: 10px;
}

.redeem-admin-board {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.redeem-admin-head h5 {
  margin: 0 0 4px;
}

.redeem-admin-head p,
.redeem-empty {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.redeem-code-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.redeem-code-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
  display: grid;
  gap: 6px;
}

.redeem-code-card p {
  margin: 0;
  font-size: 0.84rem;
  color: var(--text-secondary);
}

.redeem-code-main {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}

.redeem-code-main strong {
  font-size: 0.95rem;
  word-break: break-all;
}

.redeem-status {
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 0.76rem;
  border: 1px solid var(--border);
}

.redeem-status.ready {
  color: var(--support);
  border-color: rgba(var(--support-rgb), 0.35);
  background: rgba(var(--support-rgb), 0.12);
}

.redeem-status.used {
  color: var(--accent);
  border-color: rgba(var(--accent-rgb), 0.35);
  background: rgba(var(--accent-rgb), 0.12);
}

.redeem-status.expired,
.redeem-status.invalid {
  color: var(--text-muted);
  background: rgba(79, 73, 64, 0.08);
}

.redeem-code-actions {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
}

.issue-row,
.new-item-form {
  display: grid;
  gap: 8px;
  margin-top: 8px;
}

.issue-row {
  grid-template-columns: 1fr 1fr auto;
}

.new-item-form {
  grid-template-columns: 1fr;
}

.admin-form-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.admin-section {
  border: 1px solid rgba(44, 36, 28, 0.12);
  border-radius: 16px;
  background: rgba(255, 252, 246, 0.82);
  padding: 14px;
}

.admin-section-head h4 {
  margin: 0 0 4px;
  color: var(--text-primary);
  font-size: 1rem;
}

.admin-section-head p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.84rem;
  line-height: 1.7;
}

.admin-fields {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.field-card {
  display: grid;
  gap: 6px;
}

.field-card span {
  font-size: 0.8rem;
  color: var(--text-muted);
  letter-spacing: 0.02em;
}

.field-card input,
.field-card select,
.field-card textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-primary);
  padding: 12px 14px;
  font: inherit;
}

.field-card textarea {
  resize: vertical;
  min-height: 118px;
}

.field-card input::placeholder,
.field-card textarea::placeholder {
  color: var(--text-muted);
}

.field-full {
  grid-column: 1 / -1;
}

.admin-form-side {
  display: grid;
  gap: 12px;
}

.upload-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  padding: 12px;
  display: grid;
  gap: 10px;
}

.upload-copy {
  display: grid;
  gap: 4px;
}

.upload-copy strong {
  color: var(--text-primary);
  font-size: 0.92rem;
}

.upload-copy span {
  color: var(--text-muted);
  font-size: 0.8rem;
  line-height: 1.65;
}

.issue-row-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.upload-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  align-items: center;
}

.upload-row input[type='file'] {
  width: 100%;
  border: 1px dashed var(--border-strong);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-primary);
  padding: 10px 12px;
}

.upload-btn {
  width: 100%;
}

.cover-preview {
  display: grid;
  gap: 6px;
  align-items: start;
  border: 1px dashed var(--border);
  border-radius: 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.94);
}

.cover-preview img {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.08);
}

.cover-preview span {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.cover-preview-empty {
  min-height: 160px;
  place-items: center;
  text-align: center;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  align-items: center;
}

.primary-submit {
  min-width: 180px;
  font-weight: 600;
}

.manage-list {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.manage-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  background: var(--surface-soft);
  display: grid;
  gap: 12px;
}

.manage-summary {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: start;
}

.manage-meta {
  display: grid;
  gap: 4px;
}

.manage-meta h4,
.item-redeem-head strong {
  margin: 0;
}

.item-redeem-panel {
  border-top: 1px dashed var(--border);
  padding-top: 12px;
  display: grid;
  gap: 8px;
}

.item-redeem-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
}

.item-redeem-head > div {
  display: grid;
  gap: 4px;
}

.item-redeem-head span,
.item-redeem-count,
.item-redeem-note,
.item-redeem-empty {
  color: var(--text-muted);
  font-size: 0.82rem;
}

.item-redeem-inline {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) minmax(180px, 1fr) minmax(180px, 1fr) 108px auto auto;
  gap: 8px;
  align-items: center;
}

.item-redeem-inline input {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--text-primary);
  padding: 10px 12px;
  font: inherit;
}

.item-redeem-status-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border-radius: 10px;
  padding: 0 10px;
  font-size: 0.82rem;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-secondary);
}

.item-redeem-status-inline.ready {
  color: var(--support);
  border-color: rgba(var(--support-rgb), 0.34);
  background: rgba(var(--support-rgb), 0.1);
}

.item-redeem-status-inline.invalid {
  color: var(--danger);
  border-color: rgba(245, 159, 0, 0.35);
  background: rgba(245, 159, 0, 0.12);
}

.item-redeem-status-inline.used,
.item-redeem-status-inline.expired,
.item-redeem-status-inline.empty {
  color: var(--text-muted);
}

.item-redeem-actions-inline {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.item-redeem-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--text-muted);
  font-size: 0.8rem;
}

.manage-actions {
  display: grid;
  grid-auto-flow: column;
  gap: 8px;
}

.success-btn {
  border-color: rgba(79, 122, 88, 0.35);
  color: #2b6a38;
  background: rgba(79, 122, 88, 0.12);
}

.success-btn:hover {
  border-color: rgba(79, 122, 88, 0.55);
  background: rgba(79, 122, 88, 0.18);
}

.danger-btn {
  border-color: rgba(245, 159, 0, 0.55);
  color: #ffd8a8;
  background: rgba(245, 159, 0, 0.2);
}

.danger-btn:hover {
  border-color: rgba(245, 159, 0, 0.8);
  background: rgba(245, 159, 0, 0.3);
}

.shelf-state {
  font-size: 0.84rem;
  margin-top: 4px;
}

.shelf-state.up {
  color: var(--support);
}

.shelf-state.down {
  color: var(--danger);
}

.empty {
  margin-top: 10px;
  color: var(--text-muted);
}

.empty-link {
  margin-left: 6px;
  color: var(--accent);
  text-decoration: none;
  border-bottom: 1px solid rgba(var(--accent-rgb), 0.35);
}

.empty-link:hover {
  border-bottom-color: rgba(var(--accent-rgb), 0.7);
}

.feedback {
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.92rem;
}

.feedback.success {
  background: var(--success-bg);
  color: var(--success);
  border: 1px solid rgba(79, 122, 88, 0.35);
}

.feedback.error {
  background: var(--danger-bg);
  color: var(--danger);
  border: 1px solid rgba(154, 63, 48, 0.35);
}

.admin-gate {
  display: grid;
  gap: 14px;
}

.admin-gate-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

button:focus-visible,
input:focus-visible,
select:focus-visible,
textarea:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

.admin-panel {
  --panel-bg: rgba(255, 248, 238, 0.96);
  --surface: rgba(255, 255, 255, 0.98);
  --surface-soft: rgba(255, 252, 246, 0.96);
  --text-primary: #111111;
  --text-secondary: #1f1f1f;
  --text-muted: #3a3128;
  --border: rgba(44, 36, 28, 0.14);
  --border-strong: rgba(44, 36, 28, 0.24);
  --accent: #f76707;
  --accent-rgb: 247, 103, 7;
  --support: #22b8cf;
  --support-rgb: 34, 184, 207;
  --success: #22b8cf;
  --success-bg: rgba(34, 184, 207, 0.18);
  --danger: #f59f00;
  --danger-bg: rgba(245, 159, 0, 0.2);
}

@media (max-width: 1080px) {
  .list-tools,
  .stats-row,
  .list-grid,
  .progress-board,
  .owned-grid,
  .redeem-empty-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .artifact-strip,
  .action-row {
    grid-template-columns: 1fr;
  }

  .admin-form-shell,
  .admin-fields {
    grid-template-columns: 1fr;
  }

  .manage-summary,
  .item-redeem-inline,
  .redeem-empty-grid {
    grid-template-columns: 1fr;
  }

  .item-redeem-head,
  .manage-actions {
    display: grid;
  }

  .item-redeem-actions-inline {
    justify-content: flex-start;
  }
}

@media (max-width: 860px) {
  .list-tools,
  .stats-row,
  .issue-row,
  .issue-row-2,
  .upload-row,
  .list-grid,
  .progress-board,
  .owned-grid,
  .redeem-form,
  .manage-item,
  .redeem-code-list {
    grid-template-columns: 1fr;
  }

  .form-actions {
    justify-content: stretch;
  }

  .form-actions > * {
    width: 100%;
  }

  .manage-actions {
    grid-auto-flow: row;
  }
}
</style>
