<script setup>
import { getPermissions } from '@/api.js'
import PermissionCard from '@/components/PermissionCard.vue'
import STATUS from '@/constants/permission-status.js'

const permissions = await getPermissions()

const groups = Object.groupBy(permissions, (permission) => {
  const status = STATUS[permission.status]
  if (status.isOpen) return 'open'
  if (status.isActive) return 'active'
  if (status.isError) return 'error'
  return 'complete'
})

const labels = [
  { key: 'open', label: 'Pending' },
  { key: 'active', label: 'Active' },
  { key: 'error', label: 'Failed' },
  { key: 'complete', label: 'Complete' },
]
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
