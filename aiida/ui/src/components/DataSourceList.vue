<script setup>
import { getDataSources, regenerateDataSourceSecrets } from '@/api.js'
import DataSourceCard from '@/components/DataSourceCard.vue'
import MqttPasswordDialog from '@/components/MqttPasswordDialog.vue'
import { ref } from 'vue'

const dataSources = await getDataSources()

const dialogOpen = ref(false)
const dialogPassword = ref('')

async function handleReset(id) {
  const { plaintextPassword } = await regenerateDataSourceSecrets(id)
  dialogPassword.value = plaintextPassword
  dialogOpen.value = true
}

async function hideResetDialog() {
  dialogOpen.value = false
  dialogPassword.value = ''
}
</script>

<template>
  <MqttPasswordDialog :open="dialogOpen" :password="dialogPassword" @hide="hideResetDialog" />

  <article v-for="dataSource in dataSources">
    <DataSourceCard
      :data-source="dataSource"
      @edit="$emit('edit', dataSource)"
      @reset="handleReset(dataSource.id)"
    />
  </article>
</template>

<style scoped>
article {
  margin-top: 1rem;
}
</style>
