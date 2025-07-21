<script setup>
import { deleteDataSource, regenerateDataSourceSecrets } from '@/api.js'
import DataSourceCard from '@/components/DataSourceCard.vue'
import MqttPasswordDialog from '@/components/MqttPasswordDialog.vue'
import { onMounted, ref, watch } from 'vue'
import { dataSources, fetchDataSources } from '@/stores/dataSources.js'

const dialogOpen = ref(false)
const dialogPassword = ref('')

function handleDelete(id) {
  confirm('This action will remove the given data source.') &&
    deleteDataSource(id).then(() => fetchDataSources())
}

async function handleReset(id) {
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

watch(dataSources, () => {
  console.log('watch', dataSources)
})
</script>

<template>
  <MqttPasswordDialog :open="dialogOpen" :password="dialogPassword" @hide="hideResetDialog" />

  <article v-for="dataSource in dataSources" :key="JSON.stringify(dataSource)">
    <DataSourceCard
      :data-source="dataSource"
      @edit="$emit('edit', dataSource)"
      @delete="handleDelete(dataSource.id)"
      @reset="handleReset(dataSource.id)"
    />
  </article>
</template>

<style scoped>
article {
  margin-top: 1rem;
}
</style>
