const MODEL_TYPE_VALUES = ['3D_MODEL', 'MODEL_3D', 'DIGITAL_MODEL', 'GLB_MODEL']
const MODEL_KEYWORDS = ['3d模型', '3d model', 'glb', 'gltf', '模型商品', '数字模型']

const normalizeText = (value) => String(value || '').trim()

const normalizeToken = (value) => normalizeText(value).toLowerCase()

const splitUrls = (value) => normalizeText(value)
  .split(/[,，\n|]/)
  .map((item) => item.trim())
  .filter(Boolean)

const findMetaValue = (source, keys) => {
  const text = normalizeText(source)
  if (!text) return ''
  const escapedKeys = keys.map((key) => key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')
  const match = text.match(new RegExp(`(?:^|\\n)\\s*(?:${escapedKeys})\\s*[:：]\\s*([^\\n]+)`, 'i'))
  return match?.[1]?.trim() || ''
}

export const getProductSection = (product = {}) => {
  const explicitType = normalizeText(product.productType || product.type || product.category)
  if (MODEL_TYPE_VALUES.includes(explicitType.toUpperCase())) {
    return 'model'
  }

  const tagText = Array.isArray(product.tags) ? product.tags.join(' ') : normalizeText(product.tags)
  const combined = [
    product.productCode,
    product.name,
    product.subtitle,
    product.detailContent,
    product.modelUrl,
    product.fullModelUrl,
    tagText
  ].map(normalizeToken).join(' ')

  return MODEL_KEYWORDS.some((keyword) => combined.includes(keyword)) ? 'model' : 'physical'
}

export const isModelProduct = (product = {}) => getProductSection(product) === 'model'

export const parseModelPreviewImages = (product = {}) => {
  const direct = Array.isArray(product.previewImageUrls) ? product.previewImageUrls : []
  const metaImages = splitUrls(findMetaValue(product.detailContent, ['front', 'frontView', 'preview', 'previewImages', 'modelPreviewImages', 'threeViewImages', '三视图']))
  const images = [...direct, ...metaImages]
    .map((item) => normalizeText(item))
    .filter(Boolean)

  if (images.length > 0) {
    return Array.from(new Set(images)).slice(0, 1)
  }

  const cover = normalizeText(product.coverUrl)
  return cover ? [cover] : []
}

export const getModelUrlFromProduct = (product = {}) => {
  return normalizeText(
    product.modelUrl ||
    product.fullModelUrl ||
    findMetaValue(product.detailContent, ['modelUrl', 'fullModelUrl', 'glbUrl', '模型地址'])
  )
}

export const canViewFullModel = (product = {}) => {
  return isModelProduct(product) && Boolean(product.purchased || product.hasPurchased || product.unlocked)
}

export const resolveLockedModelUrl = (product = {}) => {
  if (!canViewFullModel(product)) return ''
  return getModelUrlFromProduct(product)
}

export const buildModelProductView = (product = {}) => ({
  ...product,
  productSection: getProductSection(product),
  isModelProduct: isModelProduct(product),
  previewImageUrls: parseModelPreviewImages(product),
  modelUrl: resolveLockedModelUrl(product),
  lockedModelUrl: getModelUrlFromProduct(product),
  canViewFullModel: canViewFullModel(product)
})
