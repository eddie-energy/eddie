<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import { RouterView } from 'vue-router'
import Header from '@/components/Header.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import AlertToastList from './components/AlertToastList.vue'
import { onMounted } from 'vue'
import { checkClipboard } from './stores/clipboardValue'
import ScrollToTopButton from './components/ScrollToTopButton.vue'

onMounted(() => {
  window.addEventListener('focus', checkClipboard)
})
</script>

<template>
  <ConfirmDialog />
  <AlertToastList />
  <Header />

  <RouterView v-slot="{ Component }">
    <Transition mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
  <ScrollToTopButton />
</template>
