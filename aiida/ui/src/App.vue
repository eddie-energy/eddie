<script setup lang="ts">
import { RouterView } from 'vue-router'
import Header from '@/components/Header.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import AlertToastList from './components/AlertToastList.vue'
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
</template>
