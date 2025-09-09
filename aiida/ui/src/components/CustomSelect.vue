<script setup lang="ts">
const { options, placeholder } = defineProps<{
  options: { label?: string; value: string }[] | string[]
  placeholder: string
}>()
const model = defineModel()
</script>

<template>
  <select v-model="model" class="select">
    <option class="option" disabled value="">{{ placeholder }}</option>
    <template v-if="typeof options[0] === 'string'">
      <option class="option" v-for="option in options" :value="option" :key="String(option)">
        {{ option }}
      </option>
    </template>
    <template v-else>
      <option
        class="option"
        v-for="option in options as { label?: string; value: string }[]"
        :value="option.value"
        :key="option.value"
      >
        {{ option.label }}
      </option>
    </template>
  </select>
</template>

<style scoped>
.select {
  appearance: none;
  border: 1px solid var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-md);
  color: var(--eddie-grey-medium);
  background-color: var(--light);
  border-radius: var(--border-radius);
  cursor: pointer;
  background-image: url('@/assets/icons/ChevronDownIcon.svg');
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
}
</style>
