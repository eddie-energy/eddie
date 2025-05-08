<script setup>
import { getPermissions } from '@/api.js'
import PermissionCard from '@/components/PermissionCard.vue'

const permissions = await getPermissions()
</script>

<template>
  <article v-for="permission in permissions">
    <PermissionCard :permission="permission" />
  </article>
</template>

<style scoped>
article + article {
  margin-top: 1rem;
}
</style>
