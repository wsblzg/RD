<template>
  <CeramicsPageShell
    kicker="ADMIN"
    title="后台管理"
    description="管理员专用入口：用于藏品上新、上下架、GLB 上传与兑换码发放。"
    :sub-nav-items="adminNavItems"
    :alt="true"
  >
    <CollectionsCenter view-mode="admin" />
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import CollectionsCenter from '@/components/collections/CollectionsCenter.vue'
import { adminNavItems } from './adminNavItems'
</script>
