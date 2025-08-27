<script setup lang="ts">
import StatusDotIcon from '@/assets/icons/StatusDotIcon.svg'
const { statusType = 'healthy' } = defineProps<{
  statusType?: 'healthy' | 'unhealthy'
}>()
</script>

<template>
  <div class="status-tag text-xsmall" :class="statusType">
    <StatusDotIcon />
    <slot />
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
  border-radius: 1rem;

  &.unhealthy {
    color: var(--eddie-red-medium);
    background-color: #ffeaeb;
    border-color: var(--eddie-red-medium);
  }

  @media screen and (min-width: 1024px) {
    padding: var(--spacing-sm) var(--spacing-md);
  }
}
</style>
