<script lang="ts" setup>
import RegionConnectorAccordions from '@/components/RegionConnectorAccordions.vue'
</script>

<template>
  <Suspense>
    <RegionConnectorAccordions />
    <template #fallback>
      <p>Fetching region connectors...</p>
    </template>
  </Suspense>
</template>
