import assert from 'node:assert/strict'
import {
  canViewFullModel,
  getProductSection,
  parseModelPreviewImages,
  resolveLockedModelUrl
} from './shopProductModel.js'

assert.equal(getProductSection({ productType: '3D_MODEL' }), 'model')
assert.equal(getProductSection({ productCode: 'GLB-001' }), 'model')
assert.equal(getProductSection({ tags: ['文创', '3d模型'] }), 'model')
assert.equal(getProductSection({ name: '柴烧杯垫' }), 'physical')

assert.deepEqual(
  parseModelPreviewImages({
    coverUrl: '/cover.webp',
    previewImageUrls: ['/front.webp', '/side.webp', '/top.webp']
  }),
  ['/front.webp']
)
assert.deepEqual(
  parseModelPreviewImages({
    coverUrl: '/cover.webp',
    detailContent: 'preview:/front.webp,/side.webp,/top.webp'
  }),
  ['/front.webp']
)
assert.deepEqual(
  parseModelPreviewImages({
    coverUrl: '/cover.webp'
  }),
  ['/cover.webp']
)

assert.equal(canViewFullModel({ productType: '3D_MODEL', purchased: true }), true)
assert.equal(canViewFullModel({ productType: '3D_MODEL', purchased: false }), false)
assert.equal(canViewFullModel({ productType: 'PHYSICAL', purchased: false }), false)

assert.equal(resolveLockedModelUrl({ modelUrl: '/model.glb', purchased: true }), '/model.glb')
assert.equal(resolveLockedModelUrl({ modelUrl: '/model.glb', purchased: false }), '')
