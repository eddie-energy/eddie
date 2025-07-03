<script setup>
import PermissionList from '@/components/PermissionList.vue'
import AiidaCodeInput from '@/components/AiidaCodeInput.vue'
</script>

<template>
  <h2>Add new permission</h2>

  <AiidaCodeInput />

  <h2>Permissions</h2>

  <Suspense>
    <PermissionList />

    <template #fallback>
      <p>Fetching permissions...</p>
    </template>
  </Suspense>
</template>
