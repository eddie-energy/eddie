<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { Popover } from 'primevue'

import type { PermissionStatus } from '@/types'

defineProps<{
  status: PermissionStatus
  datetime: string
  message?: string
}>()

const popover = useTemplateRef('popover')

const presets: Record<PermissionStatus, { color: string; icon: string }> = {
  // Request creation states
  CREATED: { color: 'info', icon: 'pi-plus' },
  VALIDATED: { color: 'success', icon: 'pi-thumbs-up' },
  SENT_TO_PERMISSION_ADMINISTRATOR: { color: 'help', icon: 'pi-send' },
  // Successful completion states
  ACCEPTED: { color: 'success', icon: 'pi-check' },
  FULFILLED: { color: 'success', icon: 'pi-file-import' },
  // Expected terminal states that did not result in data
  REJECTED: { color: 'warn', icon: 'pi-ban' },
  REVOKED: { color: 'warn', icon: 'pi-times' },
  TERMINATED: { color: 'warn', icon: 'pi-stop-circle' },
  TIMED_OUT: { color: 'warn', icon: 'pi-clock' },
  // Terminal states the EP might want to investigate
  MALFORMED: { color: 'danger', icon: 'pi-exclamation-circle' },
  UNABLE_TO_SEND: { color: 'danger', icon: 'pi-exclamation-triangle' },
  INVALID: { color: 'danger', icon: 'pi-exclamation-triangle' },
  UNFULFILLABLE: { color: 'danger', icon: 'pi-ban' },
  // Termination states
  REQUIRES_EXTERNAL_TERMINATION: { color: 'warn', icon: 'pi-clock' },
  EXTERNALLY_TERMINATED: { color: 'success', icon: 'pi-times' },
  FAILED_TO_TERMINATE: { color: 'danger', icon: 'pi-exclamation-triangle' }
}
</script>

<template>
  <div class="card">
    <i
      class="pi icon"
      :class="[presets[status].icon ?? 'pi-question-circle']"
      :style="`background: var(--${presets[status].color ?? 'help'})`"
    ></i>
    <div>
      <b>{{ status }}</b>
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

  button {
    border: unset;
    background: unset;
    color: currentColor;
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
