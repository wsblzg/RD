import assert from 'node:assert/strict'
import {
  FOLLOW_UP_INTERVAL_MS,
  MAX_FOLLOW_UP_QUERIES,
  getFirstQueryAt,
  getNextQueryDelayMs,
  getRemainingWaitSeconds,
  resolveGeneratedCover,
  shouldQueryGeneration,
  selectGenerationPreview
} from './ceramicGenerationSchedule.js'

const submittedAt = '2026-07-16T10:00:00Z'

assert.equal(getFirstQueryAt(submittedAt).toISOString(), '2026-07-16T10:05:00.000Z')
assert.equal(getRemainingWaitSeconds(submittedAt, new Date('2026-07-16T10:03:30Z')), 90)
assert.equal(shouldQueryGeneration(submittedAt, new Date('2026-07-16T10:04:59Z')), false)
assert.equal(shouldQueryGeneration(submittedAt, new Date('2026-07-16T10:05:00Z')), true)
assert.equal(FOLLOW_UP_INTERVAL_MS, 60 * 1000)
assert.equal(MAX_FOLLOW_UP_QUERIES, 5)
assert.equal(getNextQueryDelayMs(submittedAt, new Date('2026-07-16T10:03:30Z')), 90 * 1000)
assert.equal(getNextQueryDelayMs(submittedAt, new Date('2026-07-16T10:05:00Z')), 0)

assert.deepEqual(
  selectGenerationPreview({
    previousWork: { modelUrl: 'https://example.com/previous.glb', modelFormat: 'glb' },
    selectedImagePreview: 'blob:reference',
    fallbackUrl: '/青花梅瓶.webp'
  }),
  { type: 'model', url: 'https://example.com/previous.glb', modelFormat: 'glb' }
)

assert.deepEqual(
  selectGenerationPreview({
    previousWork: null,
    selectedImagePreview: 'blob:reference',
    fallbackUrl: '/青花梅瓶.webp'
  }),
  { type: 'image', url: 'blob:reference', modelFormat: '' }
)

assert.deepEqual(
  selectGenerationPreview({
    previousWork: null,
    selectedImagePreview: '',
    fallbackUrl: '/青花梅瓶.webp'
  }),
  { type: 'image', url: '/青花梅瓶.webp', modelFormat: '' }
)

assert.deepEqual(
  selectGenerationPreview({
    previousWork: null,
    selectedImagePreview: ''
  }),
  { type: 'image', url: '/青花梅瓶.webp', modelFormat: '' }
)

assert.equal(
  resolveGeneratedCover({
    coverWebpUrl: 'https://example.com/preview.webp',
    coverUrl: 'https://example.com/preview.png'
  }),
  'https://example.com/preview.webp'
)
assert.equal(resolveGeneratedCover({}, '/青花梅瓶.webp'), '/青花梅瓶.webp')
