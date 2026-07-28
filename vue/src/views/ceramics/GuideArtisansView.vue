<template>
  <CeramicsPageShell
    kicker="GUIDE"
    title="柴烧导览"
    description="围绕“可看、可学、可理解”组织导览内容，聚焦非遗匠人与工作站实践（基于本地传承人资料重制）。"
    :sub-nav-items="guideNavItems"
  >
    <GuideArtisanCards />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import GuideArtisanCards from '@/components/guide/GuideArtisanCards.vue'
import { guideNavItems } from './navs'
</script>
