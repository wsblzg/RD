const FIRST_QUERY_DELAY_MS = 5 * 60 * 1000
export const FOLLOW_UP_INTERVAL_MS = 60 * 1000
export const MAX_FOLLOW_UP_QUERIES = 5
export const DEFAULT_GENERATION_COVER = '/青花梅瓶.webp'

const toDate = (value) => value instanceof Date ? value : new Date(value)

export const getFirstQueryAt = (submittedAt) => {
  const date = toDate(submittedAt)
  return new Date(date.getTime() + FIRST_QUERY_DELAY_MS)
}

export const getRemainingWaitSeconds = (submittedAt, now = new Date()) => {
  const remaining = getFirstQueryAt(submittedAt).getTime() - toDate(now).getTime()
  return Math.max(0, Math.ceil(remaining / 1000))
}

export const shouldQueryGeneration = (submittedAt, now = new Date()) => (
  getRemainingWaitSeconds(submittedAt, now) === 0
)

export const getNextQueryDelayMs = (submittedAt, now = new Date()) => (
  getRemainingWaitSeconds(submittedAt, now) * 1000
)

export const resolveGeneratedCover = (work, fallbackUrl = DEFAULT_GENERATION_COVER) => (
  work?.coverWebpUrl || work?.coverUrl || fallbackUrl
)

export const selectGenerationPreview = ({
  previousWork,
  selectedImagePreview,
  fallbackUrl = DEFAULT_GENERATION_COVER
}) => {
  if (previousWork?.modelUrl && ['glb', 'gltf'].includes(String(previousWork.modelFormat || '').toLowerCase())) {
    return {
      type: 'model',
      url: previousWork.modelUrl,
      modelFormat: String(previousWork.modelFormat || '').toLowerCase()
    }
  }
  return {
    type: 'image',
    url: selectedImagePreview || fallbackUrl,
    modelFormat: ''
  }
}
