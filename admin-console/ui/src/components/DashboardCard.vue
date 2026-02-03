<script setup lang="ts">
defineProps<{
  text: string
  count: number | string
  info: string
  color: 'success' | 'info' | 'warn' | 'help' | 'danger'
}>()
</script>

<template>
  <div class="card">
    <span class="tooltip" v-tooltip="info">🛈</span>
    <div class="content">
      <div class="icon" :style="`background: var(--${color})`">
        <slot></slot>
      </div>
      <div>
        <span>{{ text }}</span>
        <span class="count">{{ count }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  background: var(--card-background);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 0.5rem 1rem 0.75rem;
  width: 10rem;

  .tooltip {
    display: flex;
    justify-content: end;
    cursor: default;
  }

  .content {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 0.625rem;
  }

  .icon {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 1.5rem;
    width: 1.5rem;
    border-radius: 50%;
    color: white;
  }

  .count {
    display: block;
    margin-top: 0.25rem;
    font-size: 1.25rem;
    font-weight: 500;
  }
}
</style>
