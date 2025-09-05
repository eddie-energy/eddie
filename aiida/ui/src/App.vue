<script setup lang="ts">
import { RouterView } from 'vue-router'
import Header from '@/components/Header.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
</script>

<template>
  <ConfirmDialog />

  <Header />

  <RouterView />
</template>

<style scoped></style>
