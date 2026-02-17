<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  text: string
}>()

const isVisible = ref(false)

function show() {
  isVisible.value = true
}

function hide() {
  isVisible.value = false
}
</script>

<template>
  <div class="tooltip-wrapper" @mouseenter="show" @mouseleave="hide">
    <slot />

    <transition name="fade">
      <div v-if="isVisible" class="tooltip">
        {{ text }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.tooltip-wrapper {
  position: relative;
  display: inline-block;
  cursor: default;
  overflow: visible;
}

.tooltip {
  position: absolute;
  bottom: 125%;

  left: 50%;
  transform: translateX(-50%);

  background-color: var(--eddie-grey-medium);
  color: white;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 0.75rem;

  white-space: normal;
  max-width: min(260px, 90vw);
  width: max-content;

  text-align: center;
  z-index: 1000;
}

.tooltip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 5px;
  border-style: solid;
  border-color: var(--eddie-grey-medium) transparent transparent transparent;
}
</style>
