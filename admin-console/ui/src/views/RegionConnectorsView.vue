<script lang="ts" setup>
import { getRegionConnectors, type RegionConnectorMetadata } from '@/api'
import { onMounted, ref } from 'vue'

const regionConnectors = ref<RegionConnectorMetadata[]>([])

onMounted(async () => {
  regionConnectors.value = await getRegionConnectors()
})
</script>

<template>
  <h1>Region Connectors</h1>

  <article v-for="regionConnector in regionConnectors" :key="regionConnector.id">
    <h2>{{ regionConnector.id }}</h2>
  </article>
</template>
