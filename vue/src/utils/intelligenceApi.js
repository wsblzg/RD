const APPRAISAL_REPORT_ENDPOINT = '/api/ai/chatbot'
const IMAGE_APPRAISAL_ENDPOINT = '/api/ai/generate-image-description'
const TOKEN_KEY = 'yc_token'

export const INTELLIGENCE_API_ENDPOINTS = {
  appraisalReport: APPRAISAL_REPORT_ENDPOINT,
  imageAppraisal: IMAGE_APPRAISAL_ENDPOINT
}

const buildApiUrl = (path) => {
  const baseUrl = String(import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
  const normalizedPath = `/${String(path || '').replace(/^\/+/, '')}`
  return `${baseUrl}${normalizedPath}`
}

const parseApiPayload = async (response) => {
  const text = await response.text()
  let payload = null
  try {
    payload = text ? JSON.parse(text) : null
  } catch (error) {
    throw new Error(text || '问答接口返回格式异常')
  }

  if (!response.ok) {
    throw new Error(payload?.msg || `问答接口请求失败：HTTP ${response.status}`)
  }
  if (!payload || payload.code !== 200) {
    throw new Error(payload?.msg || '问答接口请求失败')
  }
  if (!payload.data) {
    throw new Error('问答接口返回为空')
  }
  return payload.data
}

export const askRagQuestion = async (question, context = '') => {
  const normalizedQuestion = String(question || '').trim()
  if (!normalizedQuestion) {
    throw new Error('问题不能为空')
  }

  const body = new URLSearchParams()
  body.set('question', normalizedQuestion)
  if (context && String(context).trim()) {
    body.set('context', String(context).trim())
  }

  const headers = {
    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
  }
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(buildApiUrl(APPRAISAL_REPORT_ENDPOINT), {
    method: 'POST',
    headers,
    body
  })
  return await parseApiPayload(response)
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const normalizeReport = (file, partial = {}, source = 'mock') => {
  const now = new Date().toISOString()
  const defaultReport = {
    source,
    endpoint: APPRAISAL_REPORT_ENDPOINT,
    provider: source === 'api' ? 'AI 辅助鉴赏服务' : 'AI 辅助鉴赏服务（本地回退）',
    fileName: file?.name || 'unknown-file',
    confidence: 0.89,
    summary: '器物呈现典型木灰落釉痕迹，胎体收缩均匀，属于中高温柴烧稳定窑段。',
    features: [
      {
        label: '落灰层',
        value: '中等偏厚',
        evidence: '口沿与肩部出现连续灰釉沉积带。'
      },
      {
        label: '火痕方向',
        value: '单向偏右',
        evidence: '腹部流痕一致，推测处于主火道侧位。'
      },
      {
        label: '窑变色域',
        value: '灰青至琥珀过渡',
        evidence: '受热梯度清晰，存在缓慢降温迹象。'
      }
    ],
    generatedAt: now
  }

  const next = {
    ...defaultReport,
    ...partial
  }

  if (!Array.isArray(next.features) || next.features.length === 0) {
    next.features = defaultReport.features
  }

  next.features = next.features.map((item) => ({
    label: item?.label || '特征点',
    value: item?.value || '待补充',
    evidence: item?.evidence || '暂无说明'
  }))

  const numericConfidence = Number(next.confidence)
  next.confidence = Number.isFinite(numericConfidence)
    ? Math.max(0, Math.min(1, numericConfidence))
    : defaultReport.confidence

  if (!next.generatedAt) {
    next.generatedAt = now
  }

  return next
}

const extractJsonText = (rawText) => {
  if (!rawText) return ''
  const fenced = rawText.match(/```json\s*([\s\S]*?)\s*```/i)
  if (fenced?.[1]) return fenced[1].trim()

  const start = rawText.indexOf('{')
  const end = rawText.lastIndexOf('}')
  if (start >= 0 && end > start) {
    return rawText.slice(start, end + 1).trim()
  }
  return ''
}

const requestImageAppraisalReport = async (file) => {
  const body = new FormData()
  body.append('file', file)

  const headers = {}
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(buildApiUrl(IMAGE_APPRAISAL_ENDPOINT), {
    method: 'POST',
    headers,
    body
  })
  const raw = await parseApiPayload(response)
  const parsed = typeof raw === 'string'
    ? JSON.parse(extractJsonText(raw) || raw)
    : raw
  return normalizeReport(file, parsed, parsed?.source || 'api')
}

export async function identifyCeramicByImage(file) {
  if (!file) {
    return {
      ok: false,
      message: '请先上传待鉴赏图片。'
    }
  }

  // 统一鉴赏能力入口：后端接入 NVIDIA 视觉模型，真实读取图片像素。
  try {
    const report = await requestImageAppraisalReport(file)
    return {
      ok: true,
      message: '鉴赏完成（已通过在线服务生成报告）。',
      report
    }
  } catch (error) {
    await wait(600)
    const report = normalizeReport(file, {}, 'mock')
    return {
      ok: true,
      message: '在线服务不可用，已回退本地模拟报告。',
      report
    }
  }
}
