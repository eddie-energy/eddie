<script setup lang="ts">
import type { HealthStatus } from '@/api'

const HEALTH_INDICATOR_COLORS = {
  UP: 'var(--p-green-500)',
  UNKNOWN: 'var(--p-yellow-400)',
  OUT_OF_SERVICE: 'var(--p-orange-500)',
  DOWN: 'var(--p-red-500)',
  DISABLED: 'var(--p-gray-500)'
}

defineProps<{ health: HealthStatus }>()
</script>

<template>
  <i
    class="pi pi-circle-fill"
    :style="{ color: HEALTH_INDICATOR_COLORS[health] }"
    v-tooltip.top="health"
  ></i>
</template>
