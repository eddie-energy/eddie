<script setup>
import STATUS from '@/constants/permission-status.js'
import { revokePermission } from '@/api.js'
import PermissionDetails from '@/components/PermissionDetails.vue'
import { usePermissionDialog } from '@/composables/permission-dialog.js'
import { fetchPermissions } from '@/stores/permissions.js'

/** @type {{ permission: AiidaPermission }} */
const { permission } = defineProps(['permission'])
const { permissionId, serviceName, status } = permission
const { updatePermission } = usePermissionDialog()

function handleRevoke() {
  confirm(
    'This action will revoke the given permission. The eligible party will no longer be able to receive data for this entry.',
  ) && revokePermission(permissionId).then(() => fetchPermissions())
}

function handleUpdate() {
  updatePermission(permission)
}
</script>

<template>
  <sl-details>
    <span slot="summary">
      <strong>{{ serviceName }}</strong>
      <br />
      <small>{{ permissionId }}</small>
    </span>

    <PermissionDetails :permission />

    <br />
    <sl-button v-if="STATUS[status].isRevocable" @click="handleRevoke">Revoke</sl-button>
    <sl-button v-if="status === 'FETCHED_DETAILS'" @click="handleUpdate">Update</sl-button>
  </sl-details>
</template>

<style scoped></style>
