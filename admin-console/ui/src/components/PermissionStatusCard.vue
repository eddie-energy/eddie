<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { Popover } from 'primevue'

import type { PermissionStatus } from '@/types'
import { STATUS_PRESETS } from '@/constants'

const { status, datetime, message } = defineProps<{
  status: PermissionStatus
  datetime: string
  message?: string
}>()

const popover = useTemplateRef('popover')
</script>

<template>
  <div class="card">
    <i
      class="pi icon"
      :class="[STATUS_PRESETS[status]?.icon ?? 'pi-question-circle']"
      :style="`background: var(--${STATUS_PRESETS[status]?.color ?? 'help'})`"
    ></i>
    <div>
      <b>{{ STATUS_PRESETS[status]?.text ?? status }}</b>
      <br />
      <span>{{ datetime }}</span>
    </div>
    <button v-if="message" @click="popover?.toggle">
      <i class="pi pi-comment"></i>
    </button>
  </div>
  <Popover ref="popover" v-if="message">
    {{ message }}
  </Popover>
</template>

<style scoped>
.card {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--card-background);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 1rem;
  font-size: 0.875rem;

  b {
    font-size: 1rem;
    font-weight: 500;
  }

  .icon {
    font-size: 0.75rem;
    padding: 0.375rem;
    border-radius: 50%;
    color: white;
  }

  .message {
    display: block;
    margin-top: 1rem;
    line-height: 1.25rem;
  }
}
</style>
