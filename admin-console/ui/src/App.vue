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

    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.wrapper {
  height: 100%;
  display: grid;
  padding: 1rem;
  gap: 1rem;
}

@media only screen and (min-width: 1280px) {
  .wrapper {
    grid-template-columns: auto 1fr;
  }

  main {
    overflow: auto;
  }
}
</style>
