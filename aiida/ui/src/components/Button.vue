<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
const {
  buttonStyle,
  size,
  href,
  download,
  isLink = false,
  disabled = undefined,
} = defineProps<{
  buttonStyle?: 'primary' | 'secondary' | 'error' | 'error-secondary'
  size?: 'small' | 'normal' | 'medium'
  href?: string
  download?: string
  disabled?: boolean
  isLink?: boolean
}>()
</script>

<template>
  <component
    :is="isLink ? 'a' : 'button'"
    class="button"
    :class="[buttonStyle, size, { 'is-disabled': disabled }]"
    :href
    :download
    :disabled
  >
    <slot />
  </component>
</template>

<style scoped>
.button {
  --button-block-padding: var(--spacing-sm);
  --button-color: var(--light);
  --button-bg-color: var(--eddie-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--button-block-padding) var(--spacing-lg);
  transition:
    background-color 0.3s ease-in-out,
    color 0.3s ease-in-out;
  cursor: pointer;
  color: var(--button-color);
  background-color: var(--button-bg-color);
  border: 1px solid var(--button-bg-color);
  border-radius: 2rem;
  font-size: 1rem;
  font-weight: 600;
  width: fit-content;
  height: fit-content;
  text-decoration: none;
}

.button:hover {
  color: var(--button-bg-color);
  background-color: var(--button-color);
}

.button.is-disabled,
.button[disabled],
.button[aria-disabled='true'] {
  color: var(--eddie-grey-medium);
  border-color: var(--eddie-grey-medium);
  background-color: var(--eddie-secondary);
  cursor: not-allowed;
  pointer-events: none;
}

.secondary {
  --button-bg-color: var(--light);
  --button-color: var(--eddie-primary);
  border-color: var(--button-color);
}

.error {
  --button-bg-color: var(--eddie-red-medium);
  --button-color: var(--light);
}

.error-secondary {
  --button-bg-color: var(--light);
  --button-color: var(--eddie-red-medium);
  border-color: var(--button-color);
}

.small {
  --button-block-padding: var(--spacing-xs);
}
.medium {
  --button-block-padding: 0.75rem;
}
</style>
