<script setup lang="ts">
import StatusDotIcon from '@/assets/icons/StatusDotIcon.svg'
const { statusType = 'healthy', minimalOnMobile } = defineProps<{
  statusType?: 'healthy' | 'unhealthy'
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
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  background-color: #fafff2;
  border: 1px solid var(--eddie-green);
  color: var(--eddie-green);
  width: fit-content;
  height: fit-content;
  border-radius: 1rem;
  text-wrap: nowrap;

  &.unhealthy {
    color: var(--eddie-red-medium);
    background-color: #ffeaeb;
    border-color: var(--eddie-red-medium);
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
