<!-- SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { STATUS_PRESETS } from '@/constants'
const { status } = defineProps<{ status: string }>()
</script>

<template>
  <span
    :style="{
      color: `var(--chip-text-${STATUS_PRESETS[status].color ?? 'neutral'})`,
      background: `var(--chip-background-${STATUS_PRESETS[status].color ?? 'neutral'})`
    }"
  >
    <i class="pi" :class="[STATUS_PRESETS[status].icon ?? 'pi-info-circle']"></i>
    {{ STATUS_PRESETS[status].text ?? status }}
  </span>
</template>

<style scoped>
span {
  display: inline-flex;
  align-items: center;
  height: 1.5rem;
  padding: 0 0.5rem;
  gap: 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
  text-wrap: nowrap;
}

i {
  font-size: 0.75rem;
}
</style>
