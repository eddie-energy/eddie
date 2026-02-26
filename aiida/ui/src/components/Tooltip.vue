<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { nextTick, ref } from 'vue'

const tooltipRef = ref<HTMLElement | null>(null)

async function show() {
  isVisible.value = true
  await nextTick()

  const el = tooltipRef.value
  if (!el) return

  const rect = el.getBoundingClientRect()

  if (rect.left < 0) {
    el.style.left = '0'
    el.style.transform = 'translateX(0)'
  }

  if (rect.right > window.innerWidth) {
    el.style.left = '100%'
    el.style.transform = 'translateX(-100%)'
  }
}

defineProps<{
  text: string
  enabled: boolean
}>()

const isVisible = ref(false)

function hide() {
  isVisible.value = false
}
</script>

<template>
  <div class="tooltip-wrapper" @mouseenter="show" @mouseleave="hide">
    <slot />

    <transition name="fade">
      <div v-if="isVisible && enabled" ref="tooltipRef" class="tooltip">
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
  color: var(--light);
  padding: 0.375rem 0.625rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;

  white-space: normal;
  min-width: max-content;
  max-width: min(16.25rem, 90vw);
  width: max-content;

  box-sizing: border-box;

  text-align: center;
  z-index: 1000;
}
</style>
