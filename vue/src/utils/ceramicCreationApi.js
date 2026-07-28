const TOKEN_KEY = 'yc_token'

const CERAMIC_MODEL_ENDPOINT = import.meta.env.VITE_CERAMIC_MODEL_API || '/api/ceramic-creation/model'
const PROMPT_OPTIMIZE_ENDPOINT = import.meta.env.VITE_CERAMIC_PROMPT_API || '/api/ceramic-creation/prompt/optimize'
const STUDY_RESERVATION_ENDPOINT = import.meta.env.VITE_STUDY_RESERVATION_API || ''

const buildApiUrl = (path) => {
  const baseUrl = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
  const normalizedPath = `/${String(path || '').replace(/^\/+/, '')}`
  return `${baseUrl}${normalizedPath}`
}

const readPayload = async (response, fallbackMessage) => {
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch (error) {
    throw new Error(text || fallbackMessage)
  }

  if (!response.ok) {
    throw new Error(payload?.msg || `${fallbackMessage}：HTTP ${response.status}`)
  }
  if (payload?.code && payload.code !== 200) {
    throw new Error(payload?.msg || fallbackMessage)
  }
  return payload?.data || payload
}

const authHeaders = () => {
  const token = localStorage.getItem(TOKEN_KEY)
  return token ? { Authorization: `Bearer ${token}` } : {}
}

const formUrlHeaders = () => ({
  'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
  ...authHeaders()
})

const formDataHeaders = () => ({
  ...authHeaders()
})

const requestBackend = async (url, options, fallbackMessage) => {
  try {
    return await fetch(url, options)
  } catch (error) {
    throw new Error(`${fallbackMessage}：暂时无法连接服务，请稍后重试`)
  }
}

export const optimizeCeramicPrompt = async ({ message, style, vessel }) => {
  const response = await requestBackend(buildApiUrl(PROMPT_OPTIMIZE_ENDPOINT), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...authHeaders()
    },
    body: JSON.stringify({ message, style, vessel })
  }, '提示词优化失败')
  return await readPayload(response, '提示词优化失败')
}

export const generateCeramicModel = async ({ prompt, style, vessel, imageFile }) => {
  if (!CERAMIC_MODEL_ENDPOINT) {
    return {
      id: `local-${Date.now()}`,
      title: `${style || '柴烧'}${vessel || '陶瓷作品'}`,
      prompt,
      status: 'ready',
      modelUrl: '',
      coverUrl: imageFile ? URL.createObjectURL(imageFile) : '/vcg-flambe-vase-museum.webp',
      createdAt: new Date().toISOString()
    }
  }

  const body = new FormData()
  body.append('prompt', prompt)
  body.append('style', style || '')
  body.append('vessel', vessel || '')
  if (imageFile) body.append('image', imageFile)

  const response = await requestBackend(buildApiUrl(CERAMIC_MODEL_ENDPOINT), {
    method: 'POST',
    headers: formDataHeaders(),
    body
  }, 'AI 3D 模型生成失败')
  return await readPayload(response, 'AI 3D 模型生成失败')
}

export const queryCeramicModel = async (taskId) => {
  if (!CERAMIC_MODEL_ENDPOINT || String(taskId || '').startsWith('local-')) {
    return { taskId, status: 'ready' }
  }

  const response = await fetch(buildApiUrl(`${CERAMIC_MODEL_ENDPOINT}/${encodeURIComponent(taskId)}`), {
    method: 'GET',
    headers: authHeaders()
  })
  return await readPayload(response, 'AI 3D 模型查询失败')
}

export const saveCeramicModel = async (taskId, payload = {}) => {
  const body = new URLSearchParams()
  body.set('title', payload.title || '')
  body.set('prompt', payload.prompt || '')
  body.set('style', payload.style || '')
  body.set('vessel', payload.vessel || '')
  const response = await fetch(buildApiUrl(`${CERAMIC_MODEL_ENDPOINT}/${encodeURIComponent(taskId)}/save`), {
    method: 'POST',
    headers: formUrlHeaders(),
    body: body.toString()
  })
  return await readPayload(response, '作品永久保存失败')
}

export const getCeramicSession = async () => {
  const response = await fetch(buildApiUrl('/api/ceramic-creation/session'), {
    method: 'GET',
    headers: authHeaders()
  })
  return await readPayload(response, '创作进度加载失败')
}

export const listCeramicWorks = async (scope = 'all', limit = 20) => {
  const query = new URLSearchParams({
    scope: String(scope || 'all'),
    limit: String(limit)
  })
  const response = await fetch(buildApiUrl(`/api/ceramic-creation/works?${query.toString()}`), {
    method: 'GET',
    headers: authHeaders()
  })
  return await readPayload(response, '作品加载失败')
}

export const getCeramicWork = async (id) => {
  const response = await fetch(buildApiUrl(`/api/ceramic-creation/works/${encodeURIComponent(id)}`), {
    method: 'GET',
    headers: authHeaders()
  })
  return await readPayload(response, 'AI 3D 作品详情加载失败')
}

export const submitStudyReservation = async (form) => {
  if (!STUDY_RESERVATION_ENDPOINT) {
    return {
      id: `reservation-${Date.now()}`,
      status: 'draft',
      message: '预约信息已暂存，团队将在服务开放后与您联系。'
    }
  }

  const response = await fetch(buildApiUrl(STUDY_RESERVATION_ENDPOINT), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      ...authHeaders()
    },
    body: JSON.stringify(form)
  })
  return await readPayload(response, '研学预约提交失败')
}
