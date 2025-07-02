<script setup>
import { getDataSources } from '@/api.js'
import DataSourceCard from '@/components/DataSourceCard.vue'

const { edit } = defineProps(['edit'])

const dataSources = await getDataSources()
</script>

<template>
  <article v-for="dataSource in dataSources">
    <DataSourceCard :data-source="dataSource" @edit="$emit('edit', dataSource)" />
  </article>
</template>

<style scoped>
article {
  margin-top: 1rem;
}
</style>
