<script setup lang="ts">
import DataSourceList from '@/components/DataSourceList.vue'
import { useTemplateRef } from 'vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import type { AiidaDataSource } from '@/types'
import DataSourceModal from '@/components/Modals/DataSourceModal.vue'

const modal = useTemplateRef('modal')

const add = () => {
  modal.value?.showModal()
}

function edit(target: AiidaDataSource) {
  const dataSource = { ...target }
  modal.value?.showModal(dataSource)
}
</script>

<template>
  <main>
    <DataSourceModal ref="modal" />
    <header class="header">
      <h1 class="heading-2">Data sources</h1>

      <Button @click="add">
        <PlusIcon />
        Add Data Source
      </Button>
    </header>
    <DataSourceList @edit="edit" />
  </main>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 2rem;
}
</style>
