<script setup lang="ts">
import DataSourceList from '@/components/DataSourceList.vue'
import { useTemplateRef } from 'vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import type { AiidaDataSource } from '@/types'
import DataSourceModal from '@/components/Modals/DataSourceModal.vue'
import MqttPasswordModal from '@/components/Modals/MqttPasswordModal.vue'
import { regenerateDataSourceSecrets } from '@/api'
import useToast from '@/composables/useToast'

const { success } = useToast()

const modal = useTemplateRef('modal')
const passModal = useTemplateRef('passModal')

const add = () => {
  modal.value?.showModal()
}

function edit(target: AiidaDataSource) {
  const dataSource = { ...target }
  modal.value?.showModal(dataSource)
}

async function reset(id: string) {
  const { plaintextPassword } = await regenerateDataSourceSecrets(id)
  success('Successfully reset MQTT password')
  passModal.value?.showModal(plaintextPassword)
}
</script>

<template>
  <main>
    <MqttPasswordModal ref="passModal" />
    <DataSourceModal ref="modal" @showMqtt="(pass) => passModal?.showModal(pass, true)" />
    <header class="header">
      <h1 class="heading-2">Data sources</h1>
      <Button @click="add">
        <PlusIcon />
        Add Data Source
      </Button>
    </header>
    <DataSourceList @edit="edit" @reset="reset" />
  </main>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 2rem;
  button {
    width: 100%;
    justify-content: center;
  }
}

@media screen and (min-width: 640px) {
  .header {
    flex-direction: row;
    button {
      width: fit-content;
      justify-content: flex-start;
    }
  }
}
</style>
