<script setup lang="ts">
import { RouterView } from 'vue-router'
import SidebarNavigation from '@/components/SidebarNavigation.vue'
import { ConfirmDialog, Toast } from 'primevue'
</script>

<template>
  <Toast></Toast>
  <ConfirmDialog></ConfirmDialog>

  <div class="wrapper">
    <SidebarNavigation />

    <div>
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  height: 100%;
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: 100%;
  padding: 2rem;
  gap: 2rem;
}
</style>
