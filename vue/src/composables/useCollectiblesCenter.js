import { computed, ref } from 'vue'
import { adminCollectiblesAPI, collectiblesAPI, collectiblesAuthAPI } from '@/utils/collectiblesApi'

const DEFAULT_COVER = '/vcg-flambe-vase-museum.webp'

const RARITY_LABEL_MAP = {
  1: '基础款',
  2: '传承款',
  3: '典藏款',
  4: '限藏款',
  5: '臻藏款'
}

const BADGE_RULES = [
  {
    id: 'badge_first_collect',
    title: '初入窑门',
    description: '完成第一件藏品兑换。',
    check: ({ ownedCount }) => ownedCount >= 1
  },
  {
    id: 'badge_series_complete',
    title: '系列守藏者',
    description: '任意系列获取完成度达到 100%。',
    check: ({ seriesProgress }) => seriesProgress.some(item => item.completed)
  },
  {
    id: 'badge_master_collector',
    title: '窑火策展人',
    description: '累计获取 3 件及以上藏品。',
    check: ({ ownedCount }) => ownedCount >= 3
  }
]

const readStoredUser = () => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

const normalizeRarityLabel = (level) => RARITY_LABEL_MAP[level] || `Lv.${level || 1}`

const normalizeItem = (item = {}) => ({
  id: item.id,
  itemCode: item.itemCode || '',
  seriesId: item.seriesId,
  seriesCode: item.seriesCode || '',
  series: item.seriesName || '未分类',
  rarityLevel: Number(item.rarityLevel || 1),
  rarity: normalizeRarityLabel(Number(item.rarityLevel || 1)),
  name: item.name || '',
  coverUrl: item.coverUrl || DEFAULT_COVER,
  modelPath: item.modelUrl || '',
  description: item.description || '',
  onShelf: Number(item.isOnShelf) === 1,
  status: Number(item.status || 1),
  collected: Boolean(item.collected),
  acquiredAt: item.acquiredAt || null,
  createdAt: item.createdAt || null,
  updatedAt: item.updatedAt || null
})

const toNumberOrNull = (value) => {
  if (value === null || value === undefined || value === '') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const normalizeMessage = (error, fallback) => {
  if (error?.response?.data?.msg) {
    return error.response.data.msg
  }
  if (error?.message) {
    return error.message
  }
  return fallback
}

export function useCollectiblesCenter(options = {}) {
  const preferAdminCatalog = Boolean(options?.preferAdminCatalog)
  const currentUser = ref(readStoredUser())
  const catalog = ref([])
  const adminCatalog = ref([])
  const redeemCodeRecords = ref([])
  const seriesList = ref([])
  const myCollections = ref([])
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(currentUser.value?.id))
  const isAdmin = computed(() => currentUser.value?.role === 'admin')

  const onShelfItems = computed(() => catalog.value.filter(item => item.onShelf))

  const userOwnedItems = computed(() => {
    const list = [...myCollections.value]
    return list.sort((a, b) => {
      const aTime = new Date(a.acquiredAt || a.createdAt || 0).getTime()
      const bTime = new Date(b.acquiredAt || b.createdAt || 0).getTime()
      return bTime - aTime
    })
  })

  const ownedIdSet = computed(() => new Set(userOwnedItems.value.map(item => item.id)))

  const completionStats = computed(() => {
    const total = catalog.value.length
    const owned = userOwnedItems.value.length
    const percent = total > 0 ? Math.round((owned / total) * 100) : 0
    return {
      total,
      owned,
      percent
    }
  })

  const seriesProgress = computed(() => {
    if (!isLoggedIn.value) return []
    const map = new Map()
    const owned = ownedIdSet.value

    catalog.value.forEach((item) => {
      const key = item.series || '未分类'
      const prev = map.get(key) || { series: key, total: 0, owned: 0 }
      prev.total += 1
      if (owned.has(item.id)) prev.owned += 1
      map.set(key, prev)
    })

    return Array.from(map.values()).map((item) => ({
      ...item,
      percent: item.total > 0 ? Math.round((item.owned / item.total) * 100) : 0,
      completed: item.total > 0 && item.total === item.owned
    }))
  })

  const earnedBadges = computed(() => {
    if (!isLoggedIn.value) return []
    const context = {
      ownedCount: userOwnedItems.value.length,
      seriesProgress: seriesProgress.value
    }
    return BADGE_RULES.map(rule => ({
      id: rule.id,
      title: rule.title,
      description: rule.description,
      unlocked: rule.check(context)
    }))
  })

  const isOwned = (itemId) => ownedIdSet.value.has(itemId)

  const syncCatalogOwnedState = () => {
    const ownedMap = new Map(userOwnedItems.value.map(item => [item.id, item.acquiredAt || null]))
    catalog.value = catalog.value.map(item => ({
      ...item,
      collected: ownedMap.has(item.id),
      acquiredAt: ownedMap.get(item.id) || item.acquiredAt || null
    }))
  }

  const loadSeries = async () => {
    const data = await collectiblesAPI.getSeries()
    seriesList.value = Array.isArray(data) ? data : []
  }

  const loadCatalog = async () => {
    const data = await collectiblesAPI.getItems({
      page: 1,
      pageSize: 100
    })
    const list = Array.isArray(data?.list) ? data.list : []
    catalog.value = list.map(normalizeItem)
  }

  const loadAdminCatalog = async () => {
    if (!isAdmin.value) {
      adminCatalog.value = []
      return
    }
    const data = await adminCollectiblesAPI.getItems()
    const list = Array.isArray(data) ? data : []
    adminCatalog.value = list.map(normalizeItem)
  }

  const loadRedeemCodes = async () => {
    if (!isAdmin.value) {
      redeemCodeRecords.value = []
      return
    }
    const data = await adminCollectiblesAPI.getRedeemCodes()
    redeemCodeRecords.value = Array.isArray(data) ? data : []
  }

  const loadMyCollections = async () => {
    if (!isLoggedIn.value) {
      myCollections.value = []
      syncCatalogOwnedState()
      return
    }
    const data = await collectiblesAPI.getMyFavorites({ page: 1, pageSize: 100 })
    const list = Array.isArray(data?.list) ? data.list : []
    myCollections.value = list.map(normalizeItem)
    syncCatalogOwnedState()
  }

  const loadCurrentUser = async () => {
    if (!collectiblesAuthAPI.getToken()) {
      currentUser.value = null
      localStorage.removeItem('yc_user')
      return
    }
    const user = await collectiblesAuthAPI.me()
    currentUser.value = user
    localStorage.setItem('yc_user', JSON.stringify(user))
  }

  const refreshAll = async () => {
    loading.value = true
    try {
      try {
        await loadCurrentUser()
      } catch (error) {
        collectiblesAuthAPI.logout()
        currentUser.value = null
      }
      const [seriesResult, catalogResult] = await Promise.allSettled([loadSeries(), loadCatalog()])
      if (seriesResult.status === 'rejected') {
        seriesList.value = []
      }
      if (catalogResult.status === 'rejected') {
        throw catalogResult.reason
      }
      if (preferAdminCatalog && isAdmin.value) {
        try {
          await Promise.all([loadAdminCatalog(), loadRedeemCodes()])
        } catch (error) {
          adminCatalog.value = []
          redeemCodeRecords.value = []
        }
      } else if (!isAdmin.value) {
        adminCatalog.value = []
        redeemCodeRecords.value = []
      }
      try {
        await loadMyCollections()
      } catch (error) {
        myCollections.value = []
        syncCatalogOwnedState()
      }
    } finally {
      loading.value = false
    }
  }

  const register = async ({ username, password, displayName }) => {
    try {
      await collectiblesAuthAPI.register({
        username: (username || '').trim(),
        password: password || '',
        displayName: (displayName || '').trim()
      })
      return { ok: true, message: '注册成功，请登录。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '注册失败') }
    }
  }

  const login = async ({ username, password, captchaToken }) => {
    try {
      const result = await collectiblesAuthAPI.login({
        username: (username || '').trim(),
        password: password || '',
        captchaToken: captchaToken || ''
      })
      currentUser.value = result.user
      const [catalogResult, seriesResult] = await Promise.allSettled([loadCatalog(), loadSeries()])
      if (catalogResult.status === 'rejected') {
        throw catalogResult.reason
      }
      if (seriesResult.status === 'rejected') {
        seriesList.value = []
      }
      if (preferAdminCatalog && currentUser.value?.role === 'admin') {
        try {
          await Promise.all([loadAdminCatalog(), loadRedeemCodes()])
        } catch (error) {
          adminCatalog.value = []
          redeemCodeRecords.value = []
        }
      } else {
        adminCatalog.value = []
        redeemCodeRecords.value = []
      }
      await loadMyCollections()
      return { ok: true, message: '登录成功。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '登录失败') }
    }
  }

  const logout = async () => {
    collectiblesAuthAPI.logout()
    currentUser.value = null
    myCollections.value = []
    adminCatalog.value = []
    redeemCodeRecords.value = []
    await loadCatalog()
    return { ok: true, message: '已退出登录。' }
  }

  const redeemByCode = async (rawCode) => {
    if (!isLoggedIn.value) {
      return { ok: false, message: '请先登录后再兑换。' }
    }
    try {
      await collectiblesAPI.redeem((rawCode || '').trim())
      await Promise.all([loadCatalog(), loadMyCollections()])
      return { ok: true, message: '兑换成功。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '兑换失败') }
    }
  }

  const issueRedeemCode = async ({ code, itemId, issuedChannel, expireAt }) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可生成兑换码。' }
    }
    try {
      await adminCollectiblesAPI.createRedeemCode({
        code: (code || '').trim(),
        itemId: toNumberOrNull(itemId),
        issuedChannel: (issuedChannel || '').trim() || '线下非遗活动',
        expireAt: (expireAt || '').trim() || null
      })
      await loadRedeemCodes()
      return { ok: true, message: '兑换码生成成功。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '兑换码生成失败') }
    }
  }

  const invalidateRedeemCode = async (id) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可操作。' }
    }
    try {
      await adminCollectiblesAPI.invalidateRedeemCode(id)
      await loadRedeemCodes()
      return { ok: true, message: '兑换码已作废。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '作废兑换码失败') }
    }
  }

  const activateRedeemCode = async (id) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可操作。' }
    }
    try {
      await adminCollectiblesAPI.activateRedeemCode(id)
      await loadRedeemCodes()
      return { ok: true, message: '兑换码已重新生效。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '兑换码生效失败') }
    }
  }

  const upsertCatalogItem = async ({
    id,
    itemCode,
    name,
    seriesId,
    rarityLevel,
    coverUrl,
    modelPath,
    description,
    onShelf = true
  }) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可操作。' }
    }

    const payload = {
      seriesId: toNumberOrNull(seriesId),
      name: (name || '').trim(),
      rarityLevel: toNumberOrNull(rarityLevel) || 1,
      coverUrl: (coverUrl || '').trim() || null,
      modelUrl: (modelPath || '').trim(),
      modelFormat: 'glb',
      description: (description || '').trim() || null,
      status: 1
    }

    if (!payload.seriesId || !payload.name || !payload.modelUrl) {
      return { ok: false, message: '请完整填写系列、名称和模型地址。' }
    }
    if (!id && !(itemCode || '').trim()) {
      return { ok: false, message: '上新时请输入藏品编码。' }
    }

    try {
      if (id) {
        await adminCollectiblesAPI.updateItem(id, payload)
      } else {
        await adminCollectiblesAPI.createItem({
          itemCode: (itemCode || '').trim(),
          ...payload,
          isOnShelf: onShelf ? 1 : 0
        })
      }
      const tasks = [loadCatalog(), loadSeries(), loadMyCollections()]
      if (preferAdminCatalog) {
        tasks.push(loadAdminCatalog(), loadRedeemCodes())
      }
      await Promise.all(tasks)
      return { ok: true, message: id ? '藏品更新成功。' : '藏品上新成功。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '藏品保存失败') }
    }
  }

  const toggleShelfStatus = async (itemId) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可操作。' }
    }
    const item = catalog.value.find(entry => entry.id === itemId) || adminCatalog.value.find(entry => entry.id === itemId)
    if (!item) {
      return { ok: false, message: '藏品不存在。' }
    }
    const next = item.onShelf ? 0 : 1
    try {
      await adminCollectiblesAPI.updateShelf(itemId, {
        isOnShelf: next,
        remark: next === 1 ? '前台重新上架' : '前台下架'
      })
      const tasks = [loadCatalog(), loadMyCollections()]
      if (preferAdminCatalog) {
        tasks.push(loadAdminCatalog())
      }
      await Promise.all(tasks)
      return { ok: true, message: next === 1 ? '藏品已上架。' : '藏品已下架。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '更新上下架失败') }
    }
  }

  const deleteCatalogItem = async (itemId) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可操作。' }
    }
    try {
      await adminCollectiblesAPI.deleteItem(itemId)
      const tasks = [loadCatalog(), loadMyCollections()]
      if (preferAdminCatalog) {
        tasks.push(loadAdminCatalog())
      }
      await Promise.all(tasks)
      return { ok: true, message: '藏品删除成功。' }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '删除藏品失败') }
    }
  }

  const uploadGlbModel = async (file) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可上传 glb。' }
    }
    try {
      const data = await adminCollectiblesAPI.uploadGlb(file)
      return { ok: true, message: 'GLB 上传成功。', data }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, 'GLB 上传失败') }
    }
  }

  const uploadCoverImage = async (file) => {
    if (!isAdmin.value) {
      return { ok: false, message: '仅管理员可上传封面。' }
    }
    try {
      const data = await adminCollectiblesAPI.uploadCover(file)
      return { ok: true, message: '封面上传成功。', data }
    } catch (error) {
      return { ok: false, message: normalizeMessage(error, '封面上传失败') }
    }
  }

  return {
    loading,
    catalog,
    adminCatalog,
    redeemCodeRecords,
    seriesList,
    onShelfItems,
    currentUser,
    isLoggedIn,
    isAdmin,
    userOwnedItems,
    completionStats,
    seriesProgress,
    earnedBadges,
    isOwned,
    refreshAll,
    register,
    login,
    logout,
    redeemByCode,
    issueRedeemCode,
    invalidateRedeemCode,
    activateRedeemCode,
    upsertCatalogItem,
    toggleShelfStatus,
    deleteCatalogItem,
    loadAdminCatalog,
    loadRedeemCodes,
    uploadGlbModel,
    uploadCoverImage
  }
}
