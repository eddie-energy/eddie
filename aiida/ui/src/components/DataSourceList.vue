<script setup>
import { getDataSources } from '@/api.js'
import DataSourceCard from '@/components/DataSourceCard.vue'
import DataSourceDialog from '@/components/DataSourceDialog.vue'
import { ref } from 'vue'

const dataSources = await getDataSources()

const isDialogOpen = ref(false)
const dialogDataSource = ref(undefined)

function add() {
  dialogDataSource.value = undefined
  isDialogOpen.value = true
}

function edit(dataSource) {
  dialogDataSource.value = dataSource
  isDialogOpen.value = true
}

function hide() {
  isDialogOpen.value = false
}
</script>

<template>
  <DataSourceDialog :open="isDialogOpen" :data-source="dialogDataSource" @hide="hide" />

  <sl-button variant="primary" @click="add">Add Data Source</sl-button>

  <article v-for="dataSource in dataSources">
    <DataSourceCard :data-source="dataSource" @edit="edit(dataSource)" />
  </article>
</template>

<style scoped>
article {
  margin-top: 1rem;
}
</style>
