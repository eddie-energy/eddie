<script setup>
import { getDataSources } from '@/api.js'
import DataSourceCard from '@/components/DataSourceCard.vue'

const dataSources = await getDataSources()
</script>

<template>
  <article v-for="dataSource in dataSources">
    <DataSourceCard :data-source="dataSource" />
  </article>
</template>

<style scoped>
article + article {
  margin-top: 1rem;
}
</style>
