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
        <span class="text">{{ text }}</span>
        <br />
        <span class="count">{{ count }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  font-size: 0.75rem;
  background: var(--card-background);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 0.5rem 1rem 0.75rem;
  width: 10rem;

  .tooltip {
    display: flex;
    justify-content: end;
  }

  .content {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 0.625rem;
  }

  .icon {
    text-align: center;
    height: 1.5rem;
    width: 1.5rem;
    border-radius: 50%;
    padding: 0.25rem;
    color: white;
  }

  .count {
    font-size: 1.25rem;
    font-weight: 500;
  }
}
</style>
