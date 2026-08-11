// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'

type ButtonStyle = 'primary' | 'secondary' | 'error' | 'error-secondary'

const titleRef = ref('')
const descriptionRef = ref('')
const cancelLabelRef = ref('')
const confirmLabelRef = ref('')
const confirmButtonStyleRef = ref<ButtonStyle>('error')
const confirmModalRef = ref<HTMLDialogElement>()
let _resolve: (value: boolean) => void

export function useConfirmDialog() {
  async function confirm(
    title: string,
    description: string,
    confirmLabel = 'Confirm',
    cancelLabel = 'Cancel',
    confirmButtonStyle: ButtonStyle = 'error',
  ) {
    titleRef.value = title
    descriptionRef.value = description
    cancelLabelRef.value = cancelLabel
    confirmLabelRef.value = confirmLabel
    confirmButtonStyleRef.value = confirmButtonStyle
    confirmModalRef.value?.showModal()

    return new Promise<boolean>((resolve) => {
      _resolve = resolve
    })
  }

  function onConfirm() {
    confirmModalRef.value?.close()
    _resolve(true)
  }

  function onCancel() {
    confirmModalRef.value?.close()
    _resolve(false)
  }

  return {
    titleRef,
    descriptionRef,
    cancelLabelRef,
    confirmLabelRef,
    confirmButtonStyleRef,
    confirmModalRef,
    confirm,
    onConfirm,
    onCancel,
  }
}
