<script setup lang="ts">
import type { HealthStatus } from '@/api'

const colors: Record<HealthStatus, string> = {
  DISABLED: 'danger',
  DOWN: 'danger',
  OUT_OF_SERVICE: 'info',
  UNKNOWN: 'warn',
  UP: 'success'
}

const text: Record<HealthStatus, string> = {
  DISABLED: 'Disabled',
  DOWN: 'Down',
  OUT_OF_SERVICE: 'Unavailable',
  UNKNOWN: 'Unknown',
  UP: 'Running'
}

const icons: Record<HealthStatus, string> = {
  DISABLED: 'pi-stop-circle',
  DOWN: 'pi-exclamation-circle',
  OUT_OF_SERVICE: 'pi-times-circle',
  UNKNOWN: 'pi-question-circle',
  UP: 'pi-check-circle'
}

defineProps<{ health: HealthStatus }>()
</script>

<template>
  <span
    :style="{
      color: `var(--chip-text-${colors[health]})`,
      background: `var(--chip-background-${colors[health]})`
    }"
  >
    <i class="pi" :class="[icons[health]]"></i>
    {{ text[health] }}
  </span>
</template>

<style scoped>
span {
  display: inline-flex;
  gap: 0.5rem;
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
}

span,
i {
  font-size: 0.625rem;
}
</style>
