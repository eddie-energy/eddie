<script setup lang="ts">
//inspired by https://medium.com/nerd-for-tech/creating-a-customized-alert-and-confirm-function-modal-component-which-can-stop-function-execution-bb26914da78a
import ModalDialog from './ModalDialog.vue'
import Button from './Button.vue'
import { ref } from 'vue'

const { title } = defineProps<{
  title: string
  description: string
  cancelLabel: string
  confirmLabel: string
}>()

const modal = ref<HTMLDialogElement>()
let resolvePromise: (value: PromiseLike<boolean> | boolean) => void

const showModal = () => {
  modal.value?.showModal()

  return new Promise((resolve) => {
    resolvePromise = resolve
  })
}

function handleUserInput(value: boolean) {
  if (!resolvePromise) return
  resolvePromise(value)
  modal.value?.close()
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog :title ref="modal" @keydown.esc="handleUserInput(false)">
    <p class="description">
      {{ description }}
    </p>
    <div class="button-pair">
      <Button button-style="secondary" @click="handleUserInput(false)">{{ cancelLabel }}</Button>
      <Button button-style="error" @click="handleUserInput(true)">
        {{ confirmLabel }}
      </Button>
    </div>
  </ModalDialog>
</template>

<style scoped>
.description {
  margin-bottom: var(--spacing-xxl);
  max-width: 80%;
}
.button-pair {
  display: flex;
  justify-content: space-between;
}
</style>
