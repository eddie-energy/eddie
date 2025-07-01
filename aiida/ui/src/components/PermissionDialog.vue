<script setup>
import { onMounted, ref } from 'vue'
import { acceptPermission, getDataSources, rejectPermission } from '@/api.js'
import PermissionDetails from '@/components/PermissionDetails.vue'
import { usePermissionDialog } from '@/composables/permission-dialog.js'

const { permission, open } = usePermissionDialog()

/** @type {Ref<AiidaDataSource[]>} */
const dataSources = ref([])

const selectedDataSource = ref('')

onMounted(() => {
  getDataSources().then((result) => {
    dataSources.value = result
    selectedDataSource.value = dataSources.value[0]?.id
  })
})

function confirm() {
  acceptPermission(permission.value.permissionId, selectedDataSource.value)
  hide()
}

function reject() {
  rejectPermission(permission.value.permissionId)
  hide()
}

function hide() {
  open.value = false
}
</script>

<template>
  <sl-dialog
    label="Accept or reject permission"
    :open="open"
    v-if="permission"
    @sl-hide="$event.target === $event.currentTarget && hide()"
  >
    <PermissionDetails :permission />

    <sl-select label="Data Source" :value="selectedDataSource">
      <sl-option v-for="{ name, id } in dataSources" :value="id">{{ name }} ({{ id }})</sl-option>
    </sl-select>

    <p class="text">
      <em>{{ permission.serviceName }}</em> requests permission to retrieve the near-realtime data
      for the given time frame and OBIS-codes. Please confirm the request is correct before granting
      permission.
    </p>

    <sl-button slot="footer" variant="primary" @click="confirm">Confirm</sl-button>
    <sl-button slot="footer" variant="danger" @click="reject">Reject</sl-button>
    <sl-button slot="footer" outline @click="hide">Close</sl-button>
  </sl-dialog>
</template>

<style scoped></style>
