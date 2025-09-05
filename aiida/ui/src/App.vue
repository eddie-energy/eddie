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

<style scoped>
.v-enter-active,
.v-leave-active {
  transition: opacity 0.5s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

@media screen and (min-width: 1024px) {
  .alert-list {
    width: 100%;
    right: unset;
    max-width: var(--max-content-width);
    margin-bottom: unset;
    margin: 3em auto;
    padding: var(--spacing-xxl);
  }
}
</style>
