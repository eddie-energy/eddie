<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import type { HealthStatus } from '@/api'

const presets: Record<HealthStatus, { severity: string; text: string; icon: string }> = {
  DISABLED: { severity: 'danger', icon: 'pi-stop-circle', text: 'Disabled' },
  DOWN: { severity: 'danger', icon: 'pi-exclamation-circle', text: 'Down' },
  OUT_OF_SERVICE: { severity: 'info', icon: 'pi-times-circle', text: 'Unavailable' },
  UNKNOWN: { severity: 'warn', icon: 'pi-question-circle', text: 'Unknown' },
  UP: { severity: 'success', icon: 'pi-check-circle', text: 'Running' }
}

const { health } = defineProps<{ health: HealthStatus }>()
</script>

<template>
  <span
    :style="{
      color: `var(--chip-text-${presets[health].severity})`,
      background: `var(--chip-background-${presets[health].severity})`
    }"
  >
    <i class="pi" :class="[presets[health].icon]"></i>
    {{ presets[health].text }}
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
