import { ref } from 'vue'

const titleRef = ref('')
const descriptionRef = ref('')
const cancelLabelRef = ref('')
const confirmLabelRef = ref('')
const confirmModalRef = ref<HTMLDialogElement>()
let _resolve: (value: boolean) => void

export function useConfirmDialog() {

    async function confirm(
        title: string,
        description: string,
        cancelLabel = "Cancel",
        confirmLabel = "Confirm"
    ) {
        titleRef.value = title
        descriptionRef.value = description
        cancelLabelRef.value = cancelLabel
        confirmLabelRef.value = confirmLabel
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
        confirmModalRef,
        confirm,
        onConfirm,
        onCancel,

    }
}
