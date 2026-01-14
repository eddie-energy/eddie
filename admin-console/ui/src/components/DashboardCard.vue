<script setup lang="ts">
defineProps<{
  text: string;
  count: number | string
  info: string
}>()
</script>

<template>
  <div class="card">
    <span class="tooltip" v-tooltip="info">🛈</span>
    <div class="content">
      <div>
        {{ text }}
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

  .tooltip {
    display: flex;
    justify-content: end;
  }

  .content {
    display: flex;
    gap: 0.625rem;
  }

  .count {
    font-size: 1.25rem;
    font-weight: 500;
  }
}
</style>
