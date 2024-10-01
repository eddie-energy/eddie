<script setup lang="ts">
import { RouterView } from 'vue-router'
import SidebarNavigation from '@/components/SidebarNavigation.vue'
</script>

<template>
  <div class="wrapper">
    <SidebarNavigation />

    <div>
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  display: grid;
  grid-template-columns: auto 1fr;
  padding: 2rem;
  gap: 2rem;
}
</style>
