<script lang="ts" setup>
import RegionConnectorAccordions from '@/components/RegionConnectorAccordions.vue'
</script>

<template>
  <h1 tabindex="0">Region Connectors</h1>
  <Suspense>
    <RegionConnectorAccordions />
    <template #fallback>
      <p>Fetching region connectors...</p>
    </template>
  </Suspense>
</template>
