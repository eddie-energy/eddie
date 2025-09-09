<script setup lang="ts">
import { deleteDataSource, regenerateDataSourceSecrets, toggleDataSource } from '@/api'
import DataSourceCard from '@/components/DataSourceCard.vue'
import { onMounted, useTemplateRef } from 'vue'
import { dataSources, fetchDataSources } from '@/stores/dataSources'
import type { AiidaDataSource } from '@/types'
import { useConfirmDialog } from '@/composables/confirm-dialog'
import MqttPasswordModal from './Modals/MqttPasswordModal.vue'
import { notify } from '@/util/toast'

const { confirm } = useConfirmDialog()
const modal = useTemplateRef('passModal')
const emit = defineEmits(['edit'])

async function handleDelete(id: string) {
  if (
    await confirm(
      'Delete Data Source',
      'Are you sure you want to delete this data source? This action cannot be undone.',
      'Delete',
    )
  ) {
    deleteDataSource(id).then(() => fetchDataSources())
  }
}

async function handleReset(id: string) {
  const { plaintextPassword } = await regenerateDataSourceSecrets(id)
  notify('Successfully reset MQTT password', 'success')
  modal.value?.showModal(plaintextPassword)
}

const handleEnableToggle = (datasource: AiidaDataSource) => {
  toggleDataSource(datasource.id, datasource).then(() => fetchDataSources())
}

onMounted(() => {
  fetchDataSources()
})
</script>

<template>
  <MqttPasswordModal ref="passModal" />
  <div class="layout">
    <DataSourceCard
      v-for="dataSource in dataSources"
      :key="JSON.stringify(dataSource)"
      :data-source
      @edit="emit('edit', dataSource)"
      @delete="handleDelete(dataSource.id)"
      @reset="handleReset(dataSource.id)"
      @enableToggle="handleEnableToggle(dataSource)"
    />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  gap: 1rem;

  @media (min-width: 1024px) {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
