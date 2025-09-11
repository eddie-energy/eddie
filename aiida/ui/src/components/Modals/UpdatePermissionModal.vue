<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'
import type { AiidaDataSource } from '@/types'
import Button from '@/components/Button.vue'
import { ref, watch } from 'vue'
import PermissionDetails from '@/components/PermissionDetails.vue'
import { acceptPermission, getDataSources, rejectPermission } from '@/api'
import { usePermissionDialog } from '@/composables/permission-dialog'

const { permission, open } = usePermissionDialog()

const modal = ref<HTMLDialogElement>()
const loading = ref(false)
const dataSources = ref<AiidaDataSource[]>([])
const selectedDataSource = ref<string>('')
const emit = defineEmits(['update'])

watch([open], () => {
  if (open.value) {
    modal.value?.showModal()
    getDataSources().then((result) => {
      dataSources.value = result
      selectedDataSource.value = dataSources.value[0]?.id
    })
  }
})

const handleInput = async (confirm: boolean) => {
  loading.value = true
  if (confirm) {
    await acceptPermission(permission.value!.permissionId, selectedDataSource.value)
  } else {
    await rejectPermission(permission.value!.permissionId)
  }
  modal.value?.close()
  emit('update')
}

const handleModalClose = () => {
  open.value = false
  loading.value = false
}
</script>

<template>
  <ModalDialog title="Add new Permission" ref="modal" @close="handleModalClose">
    <div :class="{ 'is-loading': loading }">
      <PermissionDetails v-if="permission" :permission />
      <form class="form">
        <label class="heading-3" for="datasourceSelect">Assign Datasource</label>
        <select v-model="selectedDataSource" id="datasourceSelect">
          <option v-for="datasource in dataSources" :key="datasource.id" :value="datasource.id">
            {{ datasource.name }} {{ datasource.id }}
          </option>
        </select>
      </form>
      <div class="two-item-pair">
        <Button button-style="error-secondary" @click="handleInput(false)">Reject</Button>
        <Button @click="handleInput(true)">Accept</Button>
      </div>
    </div>
    <div v-if="loading" class="loading-indicator"></div>
  </ModalDialog>
</template>

<style scoped>
.is-loading {
  opacity: 0;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin: 2rem 0;
}
.two-item-pair {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
