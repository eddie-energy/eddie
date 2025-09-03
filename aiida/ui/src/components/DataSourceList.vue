<script setup lang="ts">
import { deleteDataSource, regenerateDataSourceSecrets } from '@/api'
import DataSourceCard from '@/components/DataSourceCard.vue'
import MqttPasswordDialog from '@/components/MqttPasswordDialog.vue'
import { onMounted, ref } from 'vue'
import { dataSources, fetchDataSources } from '@/stores/dataSources'

const dialogOpen = ref(false)
const dialogPassword = ref('')

function handleDelete(id: string) {
  confirm('This action will remove the given data source.') &&
    deleteDataSource(id).then(() => fetchDataSources())
}

async function handleReset(id: string) {
  const { plaintextPassword } = await regenerateDataSourceSecrets(id)
  dialogPassword.value = plaintextPassword
  dialogOpen.value = true
}

async function hideResetDialog() {
  dialogOpen.value = false
  dialogPassword.value = ''
}

onMounted(() => {
  fetchDataSources()
})
</script>

<template>
  <MqttPasswordDialog :open="dialogOpen" :password="dialogPassword" @hide="hideResetDialog" />
  <div class="layout">
    <DataSourceCard
      v-for="dataSource in dataSources"
      :key="JSON.stringify(dataSource)"
      :data-source="dataSource"
      @edit="$emit('edit', dataSource)"
      @delete="handleDelete(dataSource.id)"
      @reset="handleReset(dataSource.id)"
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
