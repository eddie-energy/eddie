<script setup lang="ts">
import DataSourceList from '@/components/DataSourceList.vue'
import DataSourceDialog from '@/components/DataSourceDialog.vue'
import { ref } from 'vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import type { AiidaDataSource } from '@/types'

const open = ref(false)
const dataSource = ref()

function add() {
  dataSource.value = undefined
  open.value = true
}

function edit(target: AiidaDataSource) {
  dataSource.value = target
  open.value = true
}
</script>

<template>
  <main>
    <header>
      <h1 class="heading-2">Data sources</h1>

      <Button @click="add">
        <PlusIcon />
        Add Data Source
      </Button>
    </header>

    <Suspense>
      <DataSourceDialog :open :dataSource @hide="open = false" />
    </Suspense>

    <DataSourceList @edit="edit" />
  </main>
</template>

<style scoped>
header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 2rem;
}
</style>
