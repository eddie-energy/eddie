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
  <h2>Data sources</h2>

  <Suspense>
    <DataSourceDialog :open :dataSource @hide="open = false" />
  </Suspense>

  <sl-button variant="primary" @click="add">Add Data Source</sl-button>

  <DataSourceList @edit="edit" />
</template>
