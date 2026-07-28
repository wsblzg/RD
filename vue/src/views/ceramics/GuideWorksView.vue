<template>
  <CeramicsPageShell
    kicker="GUIDE"
    title="柴烧导览"
    description="围绕“可看、可学、可理解”组织导览内容，聚焦马坝风采（作品展示）。"
    :sub-nav-items="guideNavItems"
  >
    <GuideWorksGallery />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import GuideWorksGallery from '@/components/guide/GuideWorksGallery.vue'
import { guideNavItems } from './navs'
</script>
