<script setup lang="ts">
import ModalDialog from './ModalDialog.vue'
import Button from './Button.vue'
import { ref, watch } from 'vue'
import { useConfirmDialog } from '@/composables/confirm-dialog'

const { titleRef, descriptionRef, cancelLabelRef, confirmLabelRef, open, onConfirm, onCancel } =
  useConfirmDialog()

const modal = ref<HTMLDialogElement>()

watch([open], () => {
  if (open.value) {
    modal.value?.showModal()
  }
})

function handleUserInput(value: boolean) {
  if (value) {
    onConfirm()
  } else {
    onCancel()
  }
  modal.value?.close()
}
</script>

<template>
  <ModalDialog :title="titleRef" ref="modal" @close="handleUserInput(false)">
    <p class="description">
      {{ descriptionRef }}
    </p>
    <div class="button-pair">
      <Button button-style="secondary" @click="modal?.close()">
        {{ cancelLabelRef }}
      </Button>
      <Button button-style="error" @click="handleUserInput(true)">
        {{ confirmLabelRef }}
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
