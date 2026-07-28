<template>
  <CeramicsPageShell
    kicker="GUIDE"
    title="柴烧导览"
    description="围绕“可看、可学、可理解”组织导览内容，聚焦柴烧秘境（3D窑炉导览）。"
    :sub-nav-items="guideNavItems"
  >
    <GuideKiln3DPanel />
  </CeramicsPageShell>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { guideNavItems } from './navs'

const GuideKiln3DPanel = defineAsyncComponent(() => import('@/components/guide/GuideKiln3DPanel.vue'))
</script>
