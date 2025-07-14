<script setup>
import PermissionList from '@/components/PermissionList.vue'
import AiidaCodeInput from '@/components/AiidaCodeInput.vue'
</script>

<template>
  <h2>Permissions</h2>

  <h3>Add new permission</h3>

  <AiidaCodeInput />

  <Suspense>
    <PermissionList />

    <template #fallback>
      <p>Fetching permissions...</p>
    </template>
  </Suspense>
</template>
