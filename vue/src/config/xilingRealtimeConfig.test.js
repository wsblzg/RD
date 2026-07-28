import assert from 'node:assert/strict'
import test from 'node:test'
import { DIALECT_PRESETS } from './dialectConfig.js'
import {
  buildXilingRealtimeUrl,
  isTrustedXilingOrigin,
  resolveXilingAccessToken,
  resolveXilingConfig
} from './xilingRealtimeConfig.js'

test('missing token disables remote digital human', () => {
  const config = resolveXilingConfig({})

  assert.equal(config.configured, false)
  assert.equal(buildXilingRealtimeUrl(config, DIALECT_PRESETS[0]), '')
})

test('configured digital human URL contains required params', () => {
  const config = resolveXilingConfig({
    VITE_XILING_STATIC_TOKEN: 'test-token',
    VITE_XILING_FIGURE_ID: '353922',
    VITE_XILING_BACKGROUND_IMAGE_URL: 'https://example.com/bg.png'
  })
  const url = new URL(buildXilingRealtimeUrl(config, DIALECT_PRESETS[0]))

  assert.equal(url.origin, 'https://open.xiling.baidu.com')
  assert.equal(url.searchParams.get('token'), 'test-token')
  assert.equal(url.searchParams.get('figureId'), '353922')
  assert.equal(url.searchParams.get('ttsPer'), 'CAP_4193')
  assert.equal(url.searchParams.get('showDebugger'), 'false')
})

test('six dialect presets include Cantonese dynamic-token configuration', () => {
  assert.equal(DIALECT_PRESETS.length, 6)
  assert.deepEqual(
    DIALECT_PRESETS.map((dialect) => dialect.key),
    ['mandarin', 'beijing', 'qingdao', 'henan', 'cantonese', 'guangpu']
  )

  const cantonese = DIALECT_PRESETS.find((dialect) => dialect.key === 'cantonese')
  assert.equal(cantonese.voiceId, 'LITE_lengdan_xiongzhang')
  assert.equal(cantonese.ttsLan, 'Chinese,Yue')
  assert.equal(cantonese.useDynamicToken, true)
})

test('Cantonese requests a backend token and sends cp-ttsLan to Xiling', async () => {
  const config = resolveXilingConfig({
    VITE_API_BASE_URL: 'https://api.example.test',
    VITE_XILING_STATIC_TOKEN: 'static-token',
    VITE_XILING_TOKEN_ENDPOINT: '/api/ai/xiling/token'
  })
  const cantonese = DIALECT_PRESETS.find((dialect) => dialect.key === 'cantonese')
  let requestedUrl = ''
  const fetchImpl = async (url) => {
    requestedUrl = url
    return {
      ok: true,
      text: async () =>
        JSON.stringify({
          code: 200,
          msg: '请求成功',
          data: 'dynamic-token'
        })
    }
  }

  const token = await resolveXilingAccessToken(config, cantonese, fetchImpl)
  const url = new URL(buildXilingRealtimeUrl(config, cantonese, token))

  assert.equal(requestedUrl, 'https://api.example.test/api/ai/xiling/token?expireHours=24')
  assert.equal(token, 'dynamic-token')
  assert.equal(url.searchParams.get('token'), 'dynamic-token')
  assert.equal(url.searchParams.get('cp-ttsLan'), 'Chinese,Yue')
  assert.equal(url.searchParams.get('ttsLan'), null)
})

test('only the xiling origin is trusted', () => {
  assert.equal(isTrustedXilingOrigin('https://open.xiling.baidu.com'), true)
  assert.equal(isTrustedXilingOrigin('https://example.com'), false)
})
