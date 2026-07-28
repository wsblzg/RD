<template>
  <CeramicsPageShell
    kicker="COLLECTIONS"
    title="数字藏品馆"
    description="以“可兑换、可沉淀、可持续返访”为目标，展示藏品总览。"
    :sub-nav-items="collectionsNavItems"
    :alt="true"
  >
    <CollectionsCenter view-mode="catalog" />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import CollectionsCenter from '@/components/collections/CollectionsCenter.vue'
import { collectionsNavItems } from './navs'
</script>
