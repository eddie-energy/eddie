<script setup>
import DataSourceList from '@/components/DataSourceList.vue'
import DataSourceDialog from '@/components/DataSourceDialog.vue'
import { ref } from 'vue'

const open = ref(false)
const dataSource = ref()

function add() {
  dataSource.value = undefined
  open.value = true
}

function edit(target) {
  dataSource.value = target
  open.value = true
}
</script>

<template>
  <main>
    <h1 class="heading-2">Data sources</h1>

    <Suspense>
      <DataSourceDialog :open :dataSource @hide="open = false" />
    </Suspense>

    <sl-button variant="primary" @click="add">Add Data Source</sl-button>

    <DataSourceList @edit="edit" />
  </main>
</template>
