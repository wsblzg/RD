<template>
  <CeramicsPageShell
    kicker="GUIDE"
    title="柴烧导览"
    description="围绕“可看、可学、可理解”组织导览内容，聚焦制作步骤（工艺图解）。"
    :sub-nav-items="guideNavItems"
  >
    <GuideProcessFlow />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import GuideProcessFlow from '@/components/guide/GuideProcessFlow.vue'
import { guideNavItems } from './navs'
</script>
