<script setup>
import { ref } from 'vue'

const isVisible = ref(false)
</script>

<template>
  <span :class="{ hidden: !isVisible }">
    <slot />
  </span>
  <sl-icon
    @click="isVisible = !isVisible"
    :name="isVisible ? 'eye-slash' : 'eye'"
    label="Toggle password"
  ></sl-icon>
</template>

<style scoped>
.hidden {
  -webkit-text-security: disc;
}

span {
  margin-right: 1ch;
}
</style>
