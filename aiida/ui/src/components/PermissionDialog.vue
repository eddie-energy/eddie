<script setup>
import { onMounted, ref } from 'vue'
import { acceptPermission, getDataSources, rejectPermission } from '@/api.js'
import PermissionDetails from '@/components/PermissionDetails.vue'
import { usePermissionDialog } from '@/composables/permission-dialog.js'

const { permission, open } = usePermissionDialog()

/** @type {Ref<AiidaDataSource[]>} */
const dataSources = ref([])

/** @type {Ref<string>} */
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

function hide(event) {
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
    @sl-hide="$event.target === $event.currentTarget && hide()"
    v-if="permission"
  >
    <PermissionDetails :permission />

    <sl-select
      label="Data Source"
      :value="selectedDataSource"
      @sl-input="selectedDataSource = $event.target.value"
    >
      <sl-option v-for="{ name, id } in dataSources" :value="id">{{ name }} ({{ id }})</sl-option>
    </sl-select>

    <p class="text">
      <em>{{ permission.serviceName }}</em> requests permission to retrieve the near-realtime data
      for the given time frame and OBIS-codes. Please confirm the request is correct before granting
      permission.
    </p>

    <footer slot="footer">
      <sl-button variant="primary" @click="confirm">Confirm</sl-button>
      <sl-button variant="danger" @click="reject">Reject</sl-button>
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