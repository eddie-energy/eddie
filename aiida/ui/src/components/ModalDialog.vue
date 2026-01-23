<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import { ref } from 'vue'
import CloseIcon from '@/assets/icons/CloseIcon.svg'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const { title } = defineProps<{
  title: string
}>()

const modal = ref<HTMLDialogElement>()

const showModal = () => {
  modal.value?.showModal()
}
const close = () => {
  modal.value?.close()
}

defineExpose({ showModal, close })
</script>

<template>
  <dialog class="dialog" ref="modal" closedby="any">
    <div class="dialog-header">
      <p class="heading-2">{{ title }}</p>
      <button type="button" @click="close" :aria-label="t('closeButton')" class="close-button">
        <CloseIcon />
      </button>
    </div>
    <div class="dialog-body">
      <slot />
    </div>
  </dialog>
</template>

<style scoped>
.dialog {
  margin: auto;
  padding: var(--spacing-xlg);
  min-width: 25vw;
  border-radius: var(--border-radius);
  border: unset;
  overflow: auto;
  &::backdrop {
    background-color: var(--eddie-grey-medium);
    opacity: 0.5;
  }
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xxl);
  gap: var(--spacing-md);
}

.close-button {
  display: flex;
  cursor: pointer;
}

@media screen and (min-width: 1024px) {
  .dialog {
    overflow: auto;
  }
}
</style>
