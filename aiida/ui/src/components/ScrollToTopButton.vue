<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from './Button.vue'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import { useWindowScroll } from '@vueuse/core'
import { useI18n } from 'vue-i18n'

const { y } = useWindowScroll()
const { t } = useI18n()
const show = ref(false)

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(y, () => {
  show.value = false
  if (y.value >= window.screen.height / 2) {
    show.value = true
  }
})
</script>

<template>
  <Transition mode="in-out">
    <Button class="scroll-button" @click="scrollToTop" v-if="show" :aria-label="t('scrollToTop')">
      <ChevronDownIcon class="chevron" />
    </Button>
  </Transition>
</template>

<style scoped>
.scroll-button {
  position: fixed;
  right: 0;
  bottom: 0;
  padding: var(--spacing-md);
  border-radius: 50%;
  aspect-ratio: 1;
  margin: calc(var(--mobile-header-height) + var(--spacing-md)) var(--content-padding);
}
.chevron {
  transform: rotate(180deg);
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.5s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

@media screen and (min-width: 1024px) {
  .scroll-button {
    display: none;
  }
}
</style>
