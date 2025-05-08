<script setup>
import PermissionList from '@/components/PermissionList.vue'
import DataSourceList from '@/components/DataSourceList.vue'
</script>

<template>
  <h2>Add new permission</h2>

  <div class="add-permission">
    <sl-input placeholder="AIIDA code" required></sl-input>
    <sl-button type="submit" variant="primary">Add</sl-button>
  </div>

  <h2>Permissions</h2>

  <Suspense>
    <PermissionList />

    <template #fallback>
      <p>Fetching permissions...</p>
    </template>
  </Suspense>

  <h2>Data sources</h2>

  <Suspense>
    <DataSourceList />

    <template #fallback>
      <p>Fetching data sources...</p>
    </template>
  </Suspense>
</template>

<style scoped>
h2 {
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
}

.add-permission {
  display: flex;
  gap: 0.75rem;
}
</style>