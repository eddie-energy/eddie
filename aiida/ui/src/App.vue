<script setup lang="ts">
import { RouterView } from 'vue-router'
import PermissionDialog from '@/components/PermissionDialog.vue'
import Header from '@/components/Header.vue'
</script>

<template>
  <PermissionDialog />

  <Header />

  <RouterView />
</template>

<style scoped></style>
