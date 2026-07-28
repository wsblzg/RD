<template>
  <div ref="host" class="playcaptcha-host"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import React from 'react'
import { createRoot } from 'react-dom/client'
import { ClawCaptcha, TOY_META } from 'playcaptcha'
import 'playcaptcha/clawcaptcha.css'

const TOY_LABELS = {
  duck: '小鸭',
  bear: '小熊',
  panda: '熊猫',
  bunny: '兔子',
  dino: '恐龙',
  penguin: '企鹅',
  fox: '狐狸',
  frog: '青蛙',
  whale: '鲸鱼',
  cat: '小猫',
  puppy: '小狗',
  unicorn: '独角兽'
}

Object.entries(TOY_LABELS).forEach(([id, label]) => {
  if (TOY_META[id]) {
    TOY_META[id].label = label
  }
})

const props = defineProps({
  resetKey: {
    type: [Number, String],
    default: 0
  },
  target: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['verified'])
const host = ref(null)
let root = null
let observer = null
let startedAt = 0

const TEXT_MAP = new Map([
  ['Verified', '验证通过'],
  ['You’re human. Nice catch.', '验证完成，正在继续登录。'],
  ['Move', '移动'],
  ['Grab', '抓取'],
  ['Drop', '投放'],
  ['Nice catch!', '抓对了！'],
  ['Hmm, wrong toy', '抓错了'],
  ['Release!', '松开！'],
  ['Drop here', '投到这里'],
  ['Joystick or ← → to move · Space to grab & drop', '拖动摇杆或按← →移动，按空格抓取/投放']
])

const localizeCaptcha = () => {
  if (!host.value) return
  host.value.querySelectorAll('*').forEach((node) => {
    node.childNodes.forEach((child) => {
      if (child.nodeType !== Node.TEXT_NODE) return
      const text = child.textContent.trim()
      if (TEXT_MAP.has(text)) {
        child.textContent = child.textContent.replace(text, TEXT_MAP.get(text))
      }
    })
  })
}

const renderCaptcha = () => {
  if (!host.value) return
  if (!root) {
    root = createRoot(host.value)
  }
  startedAt = Date.now()
  root.render(React.createElement(ClawCaptcha, {
    key: props.resetKey,
    target: props.target || undefined,
    title: '抓取指定陶偶完成验证',
    assetBase: '/toys/',
    onVerify: () => emit('verified', {
      target: props.target,
      elapsedMs: Date.now() - startedAt
    })
  }))
  localizeCaptcha()
}

onMounted(() => {
  renderCaptcha()
  observer = new MutationObserver(localizeCaptcha)
  observer.observe(host.value, { childList: true, subtree: true, characterData: true })
})
watch(() => [props.resetKey, props.target], renderCaptcha)

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (root) {
    root.unmount()
    root = null
  }
})
</script>

<style scoped>
.playcaptcha-host {
  --clawcap-bg: rgba(255, 250, 242, 0.94);
  --clawcap-ink: #493421;
  --clawcap-muted: #8d6a50;
  --clawcap-accent: #9f4a31;
  --clawcap-action: #b5442e;
  border-radius: 12px;
  overflow: hidden;
}

.playcaptcha-host :deep(.clawcap-help) {
  display: none;
}
</style>
