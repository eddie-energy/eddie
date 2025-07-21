<script setup>
import PermissionCard from '@/components/PermissionCard.vue'
import STATUS from '@/constants/permission-status.js'
import { computed, onMounted } from 'vue'
import { fetchPermissions, permissions } from '@/stores/permissions.js'

onMounted(() => {
  fetchPermissions()
})

const labels = [
  { key: 'open', label: 'Pending' },
  { key: 'active', label: 'Active' },
  { key: 'error', label: 'Failed' },
  { key: 'complete', label: 'Complete' },
]

const groups = computed(() => {
  return Object.groupBy(permissions.value, (permission) => {
    const status = STATUS[permission.status]
    if (status.isOpen) return 'open'
    if (status.isActive) return 'active'
    if (status.isError) return 'error'
    return 'complete'
  })
})
</script>

<template>
  <template v-for="{ key, label } in labels" :key="key">
    <template v-if="groups[key]?.length">
      <h3>{{ label }}</h3>
      <article v-for="permission in groups[key]" :key="permission.id">
        <PermissionCard :permission="permission" />
      </article>
    </template>
  </template>
</template>

<style scoped>
article + article {
  margin-top: 1rem;
}
</style>
