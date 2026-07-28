import axios from 'axios'

const TOKEN_KEY = 'yc_token'
const USER_KEY = 'yc_user'
const OSS_HOST = 'wsnlzg.oss-cn-shenzhen.aliyuncs.com'
export const SHOP_PAYMENT_QR_URL = import.meta.env.VITE_SHOP_PAYMENT_QR_URL || '/picket.webp'
const BACKEND_MEDIA_PREFIXES = ['/photo-wall/', '/project-media/', '/avatar/', '/upload/', '/uploads/', '/file/', '/files/']
const STATIC_ASSET_EXT_RE = /\.(?:png|jpe?g|webp|gif|svg|avif|ico|bmp)$/i
const LEGACY_STATIC_MEDIA_MAP = {
  '/vcg-flambe-vase-museum.jpg': '/vcg-flambe-vase-museum.webp',
  '/vcg-kiln-vessels-row.jpg': '/vcg-kiln-vessels-row.webp',
  '/vcg-olive-vase-closeup.jpg': '/vcg-olive-vase-closeup.webp',
  '/vcg-kiln-glow.jpg': '/vcg-kiln-glow.webp',
  '/logo.jpg': '/logo.webp'
}

const trimEnvValue = (value) => String(value || '').trim()

const rewriteLegacyStaticMedia = (value) => {
  const normalized = `/${String(value || '').trim().replace(/^\/+/, '')}`
  const directHit = LEGACY_STATIC_MEDIA_MAP[normalized]
  if (directHit) {
    return directHit
  }
  const lowerHit = Object.entries(LEGACY_STATIC_MEDIA_MAP).find(([key]) => key.toLowerCase() === normalized.toLowerCase())
  return lowerHit ? lowerHit[1] : value
}

const resolveBackendOrigin = () => {
  const candidates = [
    trimEnvValue(import.meta.env.VITE_API_BASE_URL),
    trimEnvValue(import.meta.env.VITE_DEV_PROXY_TARGET)
  ].filter(Boolean)

  for (const candidate of candidates) {
    try {
      const base = typeof window !== 'undefined' ? window.location.origin : 'http://localhost'
      return new URL(candidate, base).origin
    } catch (error) {
      // 忽略非法配置，尝试下一个候选值
    }
  }
  return ''
}

const BACKEND_ORIGIN = resolveBackendOrigin()

const joinWithOrigin = (origin, path) => {
  const normalizedPath = `/${String(path || '').replace(/^\/+/, '')}`
  if (!origin) {
    return normalizedPath
  }
  try {
    return new URL(normalizedPath, origin).toString()
  } catch (error) {
    return normalizedPath
  }
}

const isBackendMediaPath = (value) => {
  const normalized = `/${String(value || '').replace(/^\/+/, '')}`
  return BACKEND_MEDIA_PREFIXES.some((prefix) => normalized.startsWith(prefix))
}

const normalizeFrontendStaticPath = (value) => `/${String(value || '').replace(/^\/+/, '')}`

export const normalizeMediaUrl = (rawUrl) => {
  const value = rewriteLegacyStaticMedia(rawUrl)
  if (!value) return ''
  if (/^(?:https?:|data:|blob:)/i.test(value)) return value
  if (/^\/\//.test(value)) {
    const protocol = typeof window !== 'undefined' ? window.location.protocol : 'https:'
    return `${protocol}${value}`
  }
  if (new RegExp(`^${OSS_HOST}/`, 'i').test(value)) {
    return `https://${value}`
  }
  if (value.startsWith('/')) {
    if (isBackendMediaPath(value)) {
      return joinWithOrigin(BACKEND_ORIGIN, value)
    }
    return value
  }
  if (isBackendMediaPath(value)) {
    return joinWithOrigin(BACKEND_ORIGIN, value)
  }
  if (STATIC_ASSET_EXT_RE.test(value)) {
    return normalizeFrontendStaticPath(value)
  }
  return joinWithOrigin(BACKEND_ORIGIN, value)
}

export const normalizeMediaUrls = (urls) => {
  if (!Array.isArray(urls)) return []
  return urls
    .map((item) => normalizeMediaUrl(item))
    .filter(Boolean)
}

const collectibleHttp = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

collectibleHttp.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

const unwrap = (response) => {
  const payload = response?.data
  if (!payload) {
    throw new Error('服务返回为空')
  }
  if (payload.code !== 200) {
    throw new Error(payload.msg || '请求失败')
  }
  return payload.data
}

const uploadConfig = {
  headers: {
    'Content-Type': 'multipart/form-data'
  },
  timeout: 120000
}

export const collectiblesAuthAPI = {
  register: (data) => collectibleHttp.post('/collectibles/auth/register', data).then(unwrap),

  createCaptchaChallenge: () => collectibleHttp.post('/collectibles/auth/captcha/challenge').then(unwrap),

  verifyCaptcha: (data) => collectibleHttp.post('/collectibles/auth/captcha/verify', data).then(unwrap),

  login: async (data) => {
    const result = await collectibleHttp.post('/collectibles/auth/login', data).then(unwrap)
    const token = result?.token
    const user = result?.user
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    }
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    }
    return result
  },

  me: () => collectibleHttp.get('/collectibles/auth/me').then(unwrap),

  logout: () => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },

  getToken: () => localStorage.getItem(TOKEN_KEY),

  setToken: (token) => {
    if (!token) return
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export const collectiblesAPI = {
  getSeries: () => collectibleHttp.get('/collectibles/series').then(unwrap),

  getItems: (params = {}) => collectibleHttp.get('/collectibles/items', { params }).then(unwrap),

  getItemDetail: (id) => collectibleHttp.get(`/collectibles/items/${id}`).then(unwrap),

  favorite: (id, source = 'detail_page') =>
    collectibleHttp.post(`/collectibles/items/${id}/favorite`, { source }).then(unwrap),

  unfavorite: (id) => collectibleHttp.delete(`/collectibles/items/${id}/favorite`).then(unwrap),

  getMyFavorites: (params = { page: 1, pageSize: 20 }) =>
    collectibleHttp.get('/collectibles/me/favorites', { params }).then(unwrap),

  redeem: (code) => collectibleHttp.post('/collectibles/redeem', { code }).then(unwrap),

  getGuideConfig: () => collectibleHttp.get('/collectibles/guide/config').then(unwrap)
}

export const adminCollectiblesAPI = {
  getItems: (params = {}) =>
    collectibleHttp.get('/admin/collectibles/items', { params }).then(unwrap),

  createItem: (data) =>
    collectibleHttp.post('/admin/collectibles/items', data).then(unwrap),

  updateItem: (id, data) =>
    collectibleHttp.put(`/admin/collectibles/items/${id}`, data).then(unwrap),

  deleteItem: (id) =>
    collectibleHttp.delete(`/admin/collectibles/items/${id}`).then(unwrap),

  updateShelf: (id, data) =>
    collectibleHttp.put(`/admin/collectibles/items/${id}/shelf`, data).then(unwrap),

  createRedeemCode: (data) =>
    collectibleHttp.post('/admin/collectibles/redeem-codes', data).then(unwrap),

  getRedeemCodes: (params = {}) =>
    collectibleHttp.get('/admin/collectibles/redeem-codes', { params }).then(unwrap),

  invalidateRedeemCode: (id) =>
    collectibleHttp.put(`/admin/collectibles/redeem-codes/${id}/invalidate`).then(unwrap),

  activateRedeemCode: (id) =>
    collectibleHttp.put(`/admin/collectibles/redeem-codes/${id}/activate`).then(unwrap),

  uploadGlb: async (file) => {
    if (!file) {
      throw new Error('请选择 glb 文件')
    }
    const formData = new FormData()
    formData.append('file', file)
    return collectibleHttp.post('/admin/collectibles/upload-glb', formData, uploadConfig).then(unwrap)
  },

  uploadCover: async (file) => {
    if (!file) {
      throw new Error('请选择封面图片')
    }
    const formData = new FormData()
    formData.append('file', file)
    return collectibleHttp.post('/admin/collectibles/upload-cover', formData, uploadConfig).then(unwrap)
  },

  saveGuideConfig: (data) =>
    collectibleHttp.put('/admin/collectibles/guide/config', data).then(unwrap)
}

export const communityAPI = {
  getLatestPosts: (limit = 5) =>
    collectibleHttp.get('/collectibles/community/posts/latest', { params: { limit } }).then(unwrap),

  getPosts: (params = { page: 1, pageSize: 10 }) =>
    collectibleHttp.get('/collectibles/community/posts', { params }).then(unwrap),

  getPostDetail: (id) =>
    collectibleHttp.get(`/collectibles/community/posts/${id}`).then(unwrap),

  getMyPosts: (limit = 20) =>
    collectibleHttp.get('/collectibles/community/posts/me', { params: { limit } }).then(unwrap),

  createPost: (data) =>
    collectibleHttp.post('/collectibles/community/posts', data).then(unwrap),

  updatePost: (id, data) =>
    collectibleHttp.put(`/collectibles/community/posts/${id}`, data).then(unwrap),

  deletePost: (id) =>
    collectibleHttp.delete(`/collectibles/community/posts/${id}`).then(unwrap),

  uploadImage: async (file) => {
    if (!file) {
      throw new Error('请选择图片文件')
    }
    const formData = new FormData()
    formData.append('file', file)
    return collectibleHttp.post('/collectibles/community/upload-image', formData, uploadConfig).then(unwrap)
  }
}

export const shopAPI = {
  getProducts: (params = { page: 1, pageSize: 12 }) =>
    collectibleHttp.get('/shop/products', { params }).then(unwrap),

  getPaymentConfig: () =>
    collectibleHttp.get('/shop/payment-config').then(unwrap),

  getProductDetail: (id) =>
    collectibleHttp.get(`/shop/products/${id}`).then(unwrap),

  getPurchasedModels: () =>
    collectibleHttp.get('/shop/models/purchased').then(unwrap),

  getCart: () =>
    collectibleHttp.get('/shop/cart').then(unwrap),

  addCartItem: (data) =>
    collectibleHttp.post('/shop/cart/items', data).then(unwrap),

  updateCartItem: (id, data) =>
    collectibleHttp.put(`/shop/cart/items/${id}`, data).then(unwrap),

  deleteCartItem: (id) =>
    collectibleHttp.delete(`/shop/cart/items/${id}`).then(unwrap),

  createOrder: (data) =>
    collectibleHttp.post('/shop/orders', data).then(unwrap),

  getMyOrders: (params = { page: 1, pageSize: 10 }) =>
    collectibleHttp.get('/shop/orders', { params }).then(unwrap),

  getOrderDetail: (id) =>
    collectibleHttp.get(`/shop/orders/${id}`).then(unwrap),

  markOrderPaid: (id) =>
    collectibleHttp.post(`/shop/orders/${id}/mark-paid`).then(unwrap),

  cancelOrder: (id) =>
    collectibleHttp.post(`/shop/orders/${id}/cancel`).then(unwrap)
}

export const pointsAPI = {
  getSummary: () =>
    collectibleHttp.get('/api/points/summary').then(unwrap),

  createRecharge: (data) =>
    collectibleHttp.post('/api/points/recharges', data).then(unwrap),

  getRecharges: (params = { page: 1, pageSize: 10 }) =>
    collectibleHttp.get('/api/points/recharges', { params }).then(unwrap),

  markRechargePaid: (id) =>
    collectibleHttp.post(`/api/points/recharges/${id}/mark-paid`).then(unwrap)
}

export const adminShopAPI = {
  getProducts: (params = {}) =>
    collectibleHttp.get('/admin/shop/products', { params }).then(unwrap),

  createProduct: (data) =>
    collectibleHttp.post('/admin/shop/products', data).then(unwrap),

  updateProduct: (id, data) =>
    collectibleHttp.put(`/admin/shop/products/${id}`, data).then(unwrap),

  updateShelf: (id, data) =>
    collectibleHttp.put(`/admin/shop/products/${id}/shelf`, data).then(unwrap),

  deleteProduct: (id) =>
    collectibleHttp.delete(`/admin/shop/products/${id}`).then(unwrap),

  uploadCover: async (file, options = {}) => {
    if (!file) {
      throw new Error('请选择商品封面图片')
    }
    const formData = new FormData()
    formData.append('file', file)
    return collectibleHttp.post('/admin/shop/upload-cover', formData, {
      ...uploadConfig,
      onUploadProgress: typeof options.onUploadProgress === 'function' ? options.onUploadProgress : undefined
    }).then(unwrap)
  },

  getOrders: (params = { page: 1, pageSize: 10 }) =>
    collectibleHttp.get('/admin/shop/orders', { params }).then(unwrap),

  getOrderDetail: (id) =>
    collectibleHttp.get(`/admin/shop/orders/${id}`).then(unwrap),

  reviewPayment: (id, data) =>
    collectibleHttp.post(`/admin/shop/orders/${id}/review-payment`, data).then(unwrap),

  shipOrder: (id, data) =>
    collectibleHttp.post(`/admin/shop/orders/${id}/ship`, data).then(unwrap)
}

export const adminPointsAPI = {
  getRecharges: (params = { page: 1, pageSize: 10 }) =>
    collectibleHttp.get('/admin/points/recharges', { params }).then(unwrap),

  reviewRecharge: (id, data) =>
    collectibleHttp.post(`/admin/points/recharges/${id}/review`, data).then(unwrap)
}

export const adminFeaturesAPI = {
  getFeatures: () => collectibleHttp.get('/admin/features').then(unwrap)
}

export default {
  collectiblesAuthAPI,
  collectiblesAPI,
  adminCollectiblesAPI,
  communityAPI,
  shopAPI,
  pointsAPI,
  adminShopAPI,
  adminPointsAPI,
  adminFeaturesAPI
}
