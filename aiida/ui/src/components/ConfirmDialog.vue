<script setup lang="ts">
import ModalDialog from './ModalDialog.vue'
import Button from './Button.vue'
import { onMounted, ref } from 'vue'
import { useConfirmDialog } from '@/composables/confirm-dialog'

const modal = ref<HTMLDialogElement>()
const {
  titleRef,
  descriptionRef,
  cancelLabelRef,
  confirmLabelRef,
  confirmModalRef,
  onConfirm,
  onCancel,
} = useConfirmDialog()

onMounted(() => {
  confirmModalRef.value = modal.value
})
</script>

<template>
  <ModalDialog :title="titleRef" ref="modal" @close="onCancel">
    <p class="description">
      {{ descriptionRef }}
    </p>
    <div class="button-pair">
      <Button button-style="secondary" @click="modal?.close()">
        {{ cancelLabelRef }}
      </Button>
      <Button button-style="error" @click="onConfirm">
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
