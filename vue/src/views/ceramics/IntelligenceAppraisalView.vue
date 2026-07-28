<template>
  <CeramicsPageShell
    kicker="INTELLIGENCE"
    title="智鉴中枢"
    description="聚焦“识别-解释-问答”闭环，呈现 AI 辅助鉴赏。"
    :sub-nav-items="intelligenceNavItems"
  >
    <IntelligenceHub view-mode="appraisal" />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import IntelligenceHub from '@/components/intelligence/IntelligenceHub.vue'
import { intelligenceNavItems } from './navs'
</script>
