<script setup lang="ts">
import PermissionList from '@/components/PermissionList.vue'
import AiidaCodeInput from '@/components/AiidaCodeInput.vue'
</script>

<template>
  <main>
    <h1 class="heading-2">Permissions</h1>

    <h2>Add new permission</h2>

    <AiidaCodeInput />

    <PermissionList />
  </main>
</template>
