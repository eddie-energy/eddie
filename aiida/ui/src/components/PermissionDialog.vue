<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { acceptPermission, getDataSources, rejectPermission } from '@/api'
import PermissionDetails from './PermissionDetails.vue'
import { usePermissionDialog } from '@/composables/permission-dialog'
import { fetchPermissions } from '@/stores/permissions'
import type { AiidaDataSource } from '@/types'

const { permission, open } = usePermissionDialog()

const dataSources = ref<AiidaDataSource[]>([])

const selectedDataSource = ref<string>('')

onMounted(() => {
  getDataSources().then((result) => {
    dataSources.value = result
    selectedDataSource.value = dataSources.value[0]?.id
  })
})

function confirm(permissionId: string) {
  acceptPermission(permissionId, selectedDataSource.value)
  open.value = false
  fetchPermissions()
}

function reject(permissionId: string) {
  rejectPermission(permissionId)
  open.value = false
  fetchPermissions()
}

function hide(event: Event) {
  // avoid conflict with hide event from Shoelace's select element
  if (event.target === event.currentTarget) {
    open.value = false
  }
}
</script>

<template>
  <sl-dialog
    label="Accept or reject permission"
    :open="open || undefined"
    @sl-hide="hide"
    v-if="permission"
  >
    <PermissionDetails :permission />

    <sl-select
      label="Data Source"
      :value="selectedDataSource"
      @sl-input="selectedDataSource = $event.target.value"
    >
      <sl-option v-for="{ name, id } in dataSources" :value="id" :key="id"
        >{{ name }} ({{ id }})</sl-option
      >
    </sl-select>

    <p class="text">
      <em>{{ permission.serviceName }}</em> requests permission to retrieve the near-realtime data
      for the given time frame and OBIS-codes. Please confirm the request is correct before granting
      permission.
    </p>

    <footer slot="footer">
      <sl-button variant="primary" @click="confirm(permission.permissionId)">Confirm</sl-button>
      <sl-button variant="danger" @click="reject(permission.permissionId)">Reject</sl-button>
      <sl-button outline @click="hide">Close</sl-button>
    </footer>
  </sl-dialog>
</template>

<style scoped>
footer {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: end;
}
</style>
