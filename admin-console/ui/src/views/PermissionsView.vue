<script lang="ts" setup>
import PermissionsTable from '@/components/PermissionsTable.vue'
</script>

<template>
  <h1>Permissions</h1>

  <Suspense>
    <PermissionsTable />

    <template #fallback>
      <p>Fetching permissions...</p>
    </template>
  </Suspense>
</template>
