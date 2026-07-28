import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'

const headerUrl = new URL('../../components/NewHeaderNavigation.vue', import.meta.url)
const headerSource = readFileSync(headerUrl, 'utf8')
const routerUrl = new URL('../../router/index.js', import.meta.url)
const routerSource = readFileSync(routerUrl, 'utf8')
const hubUrl = new URL('../../components/intelligence/IntelligenceHub.vue', import.meta.url)
const hubSource = readFileSync(hubUrl, 'utf8')
const avatarUrl = new URL('../../components/xiling-realtime/XilingRealtimeAvatar.vue', import.meta.url)
const avatarSource = existsSync(avatarUrl) ? readFileSync(avatarUrl, 'utf8') : ''
const dialectConfigUrl = new URL('../../config/dialectConfig.js', import.meta.url)
const dialectConfigSource = readFileSync(dialectConfigUrl, 'utf8')
const pageUrl = new URL('./IntelligenceQaView.vue', import.meta.url)
const pageSource = readFileSync(pageUrl, 'utf8')

test('QA page preserves the site shell and mounts both conversation surfaces', () => {
  assert.match(pageSource, /<NewHeaderNavigation\s*\/>/)
  assert.match(pageSource, /<SiteFooter\s*\/>/)
  assert.match(pageSource, /<h1>柴烧知识问答<\/h1>/)
  assert.match(
    pageSource,
    /循着窑火与陶土的脉络，在一问一答之间，读懂柴烧的技艺、器物与传承故事。/
  )
  assert.doesNotMatch(pageSource, /class="qa-sub-nav"/)
  assert.match(
    pageSource,
    /import XilingRealtimeAvatar from '@\/components\/xiling-realtime\/XilingRealtimeAvatar\.vue'/
  )
  assert.match(pageSource, /<div class="digital-qa-stage">/)
})

test('intelligence entry defaults to the knowledge-qa page', () => {
  assert.match(
    headerSource,
    /\{\s*label:\s*'智鉴中枢',\s*to:\s*buildPath\('\/intelligence\/qa'\),\s*group:\s*'\/intelligence'\s*\}/
  )
  assert.match(
    routerSource,
    /path:\s*'\/intelligence',[\s\S]*redirect:\s*withCeramics\('\/intelligence\/qa'\)/
  )
})

test('main navigation uses the requested names and order', () => {
  const navItemsSource =
    headerSource.match(/const navItems = \[([\s\S]*?)\n\]/)?.[1] || ''
  const labels = [...navItemsSource.matchAll(/label:\s*'([^']+)'/g)]
    .map((match) => match[1])

  assert.deepEqual(labels, [
    '首页',
    '柴烧导览',
    '智鉴中枢',
    '数字藏品馆',
    '文创商城',
    '窑火造场',
    '社区广场'
  ])
  assert.match(
    navItemsSource,
    /\{\s*label:\s*'窑火造场',\s*to:\s*buildPath\('\/ai-creation'\),\s*group:\s*'\/ai-creation'\s*\}/
  )
})

test('QA page connects completed RAG answers and digital-human quick questions', () => {
  assert.match(pageSource, /const hubRef = ref\(null\)/)
  assert.match(pageSource, /const avatarRef = ref\(null\)/)
  assert.match(
    pageSource,
    /<IntelligenceHub[\s\S]*ref="hubRef"[\s\S]*@answer-complete="handleAnswerComplete"/
  )
  assert.match(
    pageSource,
    /<XilingRealtimeAvatar[\s\S]*ref="avatarRef"[\s\S]*@quick-question="handleQuickQuestion"/
  )
  assert.match(
    pageSource,
    /const handleAnswerComplete = \(answer\) => \{[\s\S]*avatarRef\.value\?\.speak\(answer\)/
  )
  assert.match(
    pageSource,
    /const handleQuickQuestion = \(question\) => \{[\s\S]*hubRef\.value\?\.askQuestion\(question\)/
  )
})

test('QA page gives the digital human more visual weight while keeping equal-height columns', () => {
  assert.match(
    pageSource,
    /\.qa-page-main\s*\{[\s\S]*width:\s*min\(1600px,\s*calc\(100vw\s*-\s*48px\)\)/
  )
  assert.match(
    pageSource,
    /\.digital-qa-stage\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*0\.84fr\)\s+minmax\(500px,\s*0\.76fr\)/
  )
  assert.match(
    pageSource,
    /\.digital-qa-stage\s*\{[\s\S]*align-items:\s*stretch[\s\S]*gap:\s*24px[\s\S]*height:\s*clamp\(760px,\s*calc\(100vh\s*-\s*190px\),\s*940px\)/
  )
  assert.match(
    pageSource,
    /\.digital-human-column\s*\{[^}]*grid-column:\s*2;?[^}]*grid-row:\s*1[^}]*height:\s*100%/
  )
  assert.match(
    pageSource,
    /\.conversation-column\s*\{[^}]*grid-column:\s*1;?[^}]*grid-row:\s*1[^}]*height:\s*100%/
  )
  assert.match(
    pageSource,
    /\.digital-human-column\s+:deep\(\.xiling-panel\)\s*\{[^}]*height:\s*100%[^}]*max-width:\s*none[^}]*border-color:\s*rgba\(var\(--ym-accent-rgb\),\s*0\.32\)/
  )
})

test('QA page stacks the conversation first below 1240px and unlocks height below 720px', () => {
  assert.match(pageSource, /@media \(max-width:\s*1240px\)/)
  assert.match(
    pageSource,
    /@media \(max-width:\s*1240px\)[\s\S]*\.digital-qa-stage\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/
  )
  assert.match(
    pageSource,
    /@media \(max-width:\s*1240px\)[\s\S]*\.conversation-column\s*\{[^}]*grid-row:\s*1[^}]*order:\s*1/
  )
  assert.match(
    pageSource,
    /@media \(max-width:\s*1240px\)[\s\S]*\.digital-human-column\s*\{[^}]*grid-row:\s*2[^}]*order:\s*2/
  )
  assert.match(pageSource, /@media \(max-width:\s*720px\)/)
  assert.match(
    pageSource,
    /@media \(max-width:\s*720px\)[\s\S]*\.digital-qa-stage\s*\{[\s\S]*height:\s*auto/
  )
  assert.match(
    pageSource,
    /@media \(max-width:\s*720px\)[\s\S]*\.qa-standalone-page\s*\{[\s\S]*overflow-x:\s*hidden/
  )
})

test('QA-only hub puts the consultation sidebar left of the conversation and keeps both columns scrollable', () => {
  assert.match(
    hubSource,
    /问窑火、辨落灰、读器型，也听传承故事。若典藏未有确切记载，我们会坦然说明，不让猜测替代答案。/
  )
  assert.match(
    hubSource,
    /\.qa-page\s*\{[\s\S]*grid-template-columns:\s*minmax\(240px,\s*260px\)\s+minmax\(0,\s*1fr\)/
  )
  assert.match(
    hubSource,
    /\.qa-main-panel\s*\{[^}]*grid-column:\s*2;?[^}]*grid-row:\s*1/
  )
  assert.match(
    hubSource,
    /\.qa-sidebar\s*\{[^}]*grid-column:\s*1;?[^}]*grid-row:\s*1[^}]*height:\s*100%[^}]*overflow-y:\s*auto/
  )
  assert.match(
    hubSource,
    /\.qa-page\.qa-only\s*\{[\s\S]*height:\s*100%[\s\S]*min-height:\s*0[\s\S]*overflow:\s*hidden/
  )
  assert.match(
    hubSource,
    /\.qa-message-board\s*\{[\s\S]*min-height:\s*0[\s\S]*overflow-y:\s*auto/
  )
  assert.match(
    hubSource,
    /@media \(max-width:\s*720px\)[\s\S]*\.qa-ask-form\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+auto/
  )
  assert.match(
    hubSource,
    /@media \(max-width:\s*720px\)[\s\S]*\.send-btn\s*\{[\s\S]*width:\s*auto/
  )
})

test('left consultation sidebar keeps the neutral 7129ad4 visual style', () => {
  assert.match(
    hubSource,
    /\.qa-sidebar\s*\{[^}]*grid-column:\s*1;?[^}]*background:\s*#f3f3f3;[^}]*padding:\s*18px;/
  )
  assert.match(
    hubSource,
    /\.consultant-card\s*\{[^}]*background:\s*#ffffff;[^}]*border-radius:\s*16px;[^}]*padding:\s*14px;/
  )
  assert.match(
    hubSource,
    /\.topic-item\s*\{[^}]*border:\s*1px solid rgba\(225,\s*140,\s*62,\s*0\.28\);[^}]*background:\s*#efbd86;/
  )
  assert.doesNotMatch(
    hubSource,
    /\.qa-sidebar\s*\{[^}]*radial-gradient/
  )
})

test('QA-only hub resets to one explicit column below 980px with chat before sidebar', () => {
  assert.match(
    hubSource,
    /@media \(max-width:\s*980px\)[\s\S]*\.qa-page\s*\{[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/
  )
  assert.match(
    hubSource,
    /@media \(max-width:\s*980px\)[\s\S]*\.qa-main-panel\s*\{[^}]*grid-column:\s*1;?[^}]*grid-row:\s*1[^}]*order:\s*1/
  )
  assert.match(
    hubSource,
    /@media \(max-width:\s*980px\)[\s\S]*\.qa-sidebar\s*\{[^}]*grid-column:\s*1;?[^}]*grid-row:\s*2[^}]*order:\s*2/
  )
})

test('IntelligenceHub exposes completed answers for the digital human', () => {
  assert.match(hubSource, /defineEmits\(\['answer-complete'\]\)/)
  assert.match(hubSource, /emit\('answer-complete', finalAnswer\)/)
  assert.match(hubSource, /defineExpose\(\{[\s\S]*askQuestion/)
})

test('IntelligenceHub emits only after the typewriter fully drains', () => {
  assert.match(hubSource, /const waitTypewriterDrain = async \(messageId\) =>/)
  assert.match(
    hubSource,
    /await waitTypewriterDrain\(assistantMessage\.id\)[\s\S]*emit\('answer-complete', finalAnswer\)/
  )
})

test('IntelligenceHub keeps the existing RAG and local retrieval fallback chain', () => {
  assert.match(hubSource, /await askRagQuestion\(normalized\)/)
  assert.match(hubSource, /retrieveKnowledge\(normalized,\s*\{\s*limit:\s*4\s*\}\)/)
  assert.match(hubSource, /buildGroundedAnswerFromChunks/)
})

test('digital human uses environment configuration without embedded credentials', () => {
  const forbiddenCredentialMarkers = [
    ['XILING', 'APP', 'KEY'].join('_'),
    ['Hmac', 'SHA256'].join(''),
    ['API', 'SECRET'].join('_'),
    ['XILING', 'STATIC', 'TOKEN'].join('_')
  ]

  assert.match(
    avatarSource,
    /resolveXilingConfig\(import\.meta\.env\)/
  )
  assert.match(avatarSource, /buildXilingRealtimeUrl/)
  assert.match(avatarSource, /isTrustedXilingOrigin/)
  forbiddenCredentialMarkers.forEach((marker) => {
    assert.equal(avatarSource.includes(marker), false)
  })
})

test('digital human header uses the project logo instead of the text seal', () => {
  assert.match(
    avatarSource,
    /<img\s+src="\/logo\.webp"\s+alt=""\s+class="xiling-brand-logo"\s*\/>/
  )
  assert.doesNotMatch(avatarSource, /class="xiling-seal"[^>]*>智</)
})

test('consultant and digital-human state marks use the project logo', () => {
  assert.match(
    hubSource,
    /<div class="consultant-badge">\s*<img\s+src="\/logo\.webp"\s+alt=""\s+class="consultant-logo"\s*\/>\s*<\/div>/
  )
  assert.doesNotMatch(hubSource, /<div class="consultant-badge">问<\/div>/)
  assert.match(
    avatarSource,
    /<div class="xiling-orbit-mark" aria-hidden="true">\s*<img\s+src="\/logo\.webp"\s+alt=""\s+class="xiling-orbit-logo"\s*\/>\s*<\/div>/
  )
  assert.doesNotMatch(avatarSource, /class="xiling-orbit-mark"[^>]*>(未|歇)</)
})

test('digital human auto-starts when configured and uses logo for the start mark', () => {
  assert.match(
    avatarSource,
    /onMounted\(\(\) => \{[\s\S]*if \(config\.configured\) \{[\s\S]*startDigitalHuman\(\)/
  )
  assert.match(
    avatarSource,
    /<div[\s\S]*class="xiling-overlay xiling-start"[\s\S]*<img\s+src="\/logo\.webp"\s+alt=""\s+class="xiling-orbit-logo"\s*\/>/
  )
  assert.doesNotMatch(
    avatarSource,
    /<div[\s\S]*class="xiling-overlay xiling-start"[\s\S]*<span class="xiling-orbit-mark" aria-hidden="true">陶<\/span>/
  )
})

test('digital human only creates the iframe after configuration and user gesture', () => {
  assert.match(avatarSource, /v-if="config\.configured && hasUserGesture"/)
  assert.match(avatarSource, /数字人服务尚未配置/)
  assert.match(avatarSource, /function startDigitalHuman\(\)/)
})

test('digital human startup copy uses the updated literary wording', () => {
  assert.match(
    avatarSource,
    /窑火已候，答案一到，讲解员便会循着柴烧文脉徐徐开口；初次连线时，请给它几秒与火色同频。/
  )
})

test('digital human keeps only the newest pending answer and flushes it when ready', () => {
  assert.match(avatarSource, /const pendingSpeech = ref\(''\)/)
  assert.match(avatarSource, /pendingSpeech\.value = normalized/)
  assert.match(
    avatarSource,
    /videoReady\.value && wsConnected\.value[\s\S]*pendingSpeech\.value/
  )
  assert.match(avatarSource, /function flushPendingSpeech\(\)/)
})

test('digital human exposes playback and connection controls', () => {
  assert.match(
    avatarSource,
    /defineExpose\(\{[\s\S]*speak[\s\S]*interrupt[\s\S]*reconnect[\s\S]*replayWelcome/
  )
  assert.match(avatarSource, /VITE_XILING_IDLE_TIMEOUT_SECONDS/)
  assert.match(avatarSource, /:\s*90/)
  assert.match(avatarSource, /function toggleMute\(\)/)
  assert.match(avatarSource, /DIALECT_PRESETS/)
})

test('digital human emits quick questions without answering them locally', () => {
  assert.match(avatarSource, /defineEmits\(\['quick-question'\]\)/)
  assert.match(avatarSource, /emit\('quick-question', question\)/)
  assert.doesNotMatch(avatarSource, /dialectKnowledgeBase|askRagQuestion|buildGroundedAnswer/)
})

test('digital human accepts trusted messages and cleans up its lifecycle', () => {
  assert.match(
    avatarSource,
    /if \(!isTrustedXilingOrigin\(event\.origin\)\) return/
  )
  assert.match(avatarSource, /window\.addEventListener\('message', onMessage\)/)
  assert.match(avatarSource, /window\.removeEventListener\('message', onMessage\)/)
  assert.match(avatarSource, /onUnmounted\(\(\) => \{[\s\S]*clearIdleTimer\(\)/)
})

test('idle timer resets do not cancel the mobile audio wake-up', () => {
  const clearIdleTimerSource =
    avatarSource.match(/function clearIdleTimer\(\) \{[\s\S]*?\n\}/)?.[0] || ''

  assert.match(avatarSource, /function clearAudioWakeTimer\(\)/)
  assert.doesNotMatch(clearIdleTimerSource, /audioWakeTimer/)
  assert.match(
    avatarSource,
    /onUnmounted\(\(\) => \{[\s\S]*clearIdleTimer\(\)[\s\S]*clearAudioWakeTimer\(\)/
  )
})

test('replay welcome always uses the welcome text', () => {
  assert.match(
    avatarSource,
    /function replayWelcome\(\) \{\s*speak\(WELCOME_TEXT\)\s*\}/
  )
})

test('digital human panel supports fixed-height parents and mobile layout', () => {
  assert.match(
    avatarSource,
    /\.xiling-panel\s*\{[\s\S]*width:\s*100%[\s\S]*max-width:\s*420px[\s\S]*height:\s*auto/
  )
  assert.match(avatarSource, /\.xiling-viewport\s*\{[\s\S]*min-height:\s*0/)
  assert.match(
    avatarSource,
    /\.xiling-iframe\s*\{[\s\S]*position:\s*absolute;[\s\S]*inset:\s*-6%\s+-8%;[\s\S]*width:\s*116%;[\s\S]*height:\s*112%;/
  )
  assert.match(avatarSource, /@media \(max-width: 720px\)/)
  assert.match(avatarSource, /var\(--ym-bg/)
  assert.match(avatarSource, /var\(--ym-accent/)
})

test('digital human keeps all six dialect buttons visible in the compact sidebar', () => {
  assert.match(dialectConfigSource, /key:\s*'cantonese'/)
  assert.match(dialectConfigSource, /label:\s*'粤语'/)
  assert.match(dialectConfigSource, /voiceId:\s*'LITE_lengdan_xiongzhang'/)
  assert.match(dialectConfigSource, /ttsLan:\s*'Chinese,Yue'/)
  assert.match(
    avatarSource,
    /\.xiling-dialect-list\s*\{[\s\S]*flex-wrap:\s*wrap[\s\S]*overflow:\s*visible/
  )
  assert.match(
    avatarSource,
    /\.xiling-viewport\s*\{[\s\S]*aspect-ratio:\s*9\s*\/\s*16[\s\S]*max-height:\s*560px/
  )
})
