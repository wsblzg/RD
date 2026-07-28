const XILING_REALTIME_URL = 'https://open.xiling.baidu.com/cloud/realtime'
const XILING_TRUSTED_ORIGIN = 'https://open.xiling.baidu.com'
const DEFAULT_FIGURE_ID = '353922'
const DEFAULT_TOKEN_ENDPOINT = '/api/ai/xiling/token'

const normalizeEnvValue = (value) => String(value || '').trim()

const buildApiUrl = (baseUrl, path) => {
  const normalizedBase = normalizeEnvValue(baseUrl).replace(/\/+$/, '')
  const normalizedPath = `/${String(path || '').replace(/^\/+/, '')}`
  return `${normalizedBase}${normalizedPath}`
}

export function resolveXilingConfig(env = {}) {
  const token = normalizeEnvValue(env.VITE_XILING_STATIC_TOKEN)
  const figureId = normalizeEnvValue(env.VITE_XILING_FIGURE_ID) || DEFAULT_FIGURE_ID
  const backgroundImageUrl = normalizeEnvValue(env.VITE_XILING_BACKGROUND_IMAGE_URL)
  const apiBaseUrl = normalizeEnvValue(env.VITE_API_BASE_URL)
  const tokenEndpoint =
    normalizeEnvValue(env.VITE_XILING_TOKEN_ENDPOINT) || DEFAULT_TOKEN_ENDPOINT

  return {
    token,
    figureId,
    backgroundImageUrl,
    apiBaseUrl,
    tokenEndpoint,
    configured: Boolean(token)
  }
}

export async function resolveXilingAccessToken(
  config,
  dialect,
  fetchImpl = globalThis.fetch
) {
  if (!dialect?.useDynamicToken) {
    return config?.token || ''
  }
  if (typeof fetchImpl !== 'function') {
    throw new Error('当前环境无法请求粤语动态令牌')
  }

  const endpoint = buildApiUrl(config?.apiBaseUrl, config?.tokenEndpoint)
  const separator = endpoint.includes('?') ? '&' : '?'
  const response = await fetchImpl(`${endpoint}${separator}expireHours=24`, {
    method: 'GET',
    credentials: 'same-origin'
  })
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch {
    throw new Error(text || '粤语动态令牌返回格式异常')
  }

  if (!response.ok || payload?.code !== 200 || !payload?.data) {
    throw new Error(payload?.msg || `粤语动态令牌请求失败：HTTP ${response.status}`)
  }
  return String(payload.data).trim()
}

export function buildXilingRealtimeUrl(config, dialect, tokenOverride = '') {
  if (!config?.configured) return ''

  const token = normalizeEnvValue(tokenOverride) || config.token
  const params = new URLSearchParams({
    token,
    figureId: config.figureId,
    initMode: 'noAudio',
    videoBg: '#F3F4FB',
    resolutionWidth: '1080',
    resolutionHeight: '1920',
    showDebugger: 'false',
    ttsPer: dialect?.voiceId || '',
    'cp-ttsSample': dialect?.ttsSample || '16000',
    'cp-preAlertSec': '120',
    'cp-positionV2': JSON.stringify({
      location: { top: 0, left: 0, width: 1080, height: 1920 }
    })
  })

  if (dialect?.ttsLan) {
    params.set('cp-ttsLan', dialect.ttsLan)
  }
  if (config.backgroundImageUrl) {
    params.set('backgroundImageUrl', config.backgroundImageUrl)
  }

  return `${XILING_REALTIME_URL}?${params.toString()}`
}

export function isTrustedXilingOrigin(origin) {
  return origin === XILING_TRUSTED_ORIGIN
}
