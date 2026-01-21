<!--
SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

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
  display: grid;
  grid-template-columns: auto 1fr;
  height: 100%;
}

main {
  padding: 7.5rem 2.5rem 3rem;
  overflow: auto;
}

@media (width < 80rem) {
  .wrapper {
    grid-template-columns: auto;
  }

  main {
    padding: 3rem 2rem;
    overflow: initial;
  }
}
</style>
