<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import StatusDotIcon from '@/assets/icons/StatusDotIcon.svg'
import type { StatusTypes } from '@/types'

const { statusType = 'healthy', minimalOnMobile } = defineProps<{
  statusType?: StatusTypes
  minimalOnMobile?: boolean
}>()
</script>

<template>
  <div class="status-tag text-xsmall" :class="[statusType, minimalOnMobile && 'minimal']">
    <StatusDotIcon class="dot" />
    <span>
      <slot />
    </span>
  </div>
</template>

<style scoped>
.status-tag {
  --status-color: var(--eddie-green);

  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  background-color: #fafff2;
  border: 1px solid var(--status-color);
  color: var(--status-color);
  width: fit-content;
  height: fit-content;
  border-radius: 1rem;
  text-wrap: nowrap;

  &.unhealthy,
  &.partially-healthy {
    --status-color: var(--eddie-red-medium);
    background-color: var(--eddie-red-background);
  }

  &.unknown {
    --status-color: var(--eddie-grey-medium);
    background-color: var(--eddie-grey-light);
  }

  &.minimal {
    padding: unset;
    > span {
      display: none;
    }
  }

  .dot {
    width: 0.5rem;
  }

  @media screen and (min-width: 640px) {
    &.minimal {
      padding: var(--spacing-xs) var(--spacing-md);
      > span {
        display: inline;
      }
    }
  }

  @media screen and (min-width: 1024px) {
    padding: var(--spacing-sm) var(--spacing-md);
  }
}
</style>
