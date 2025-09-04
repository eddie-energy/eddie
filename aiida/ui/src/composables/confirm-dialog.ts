import { ref } from 'vue'

const titleRef = ref('')
const descriptionRef = ref('')
const cancelLabelRef = ref('')
const confirmLabelRef = ref('')
const open = ref<boolean | undefined>(false)
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
        open.value = true

        return new Promise<boolean>((resolve) => {
            _resolve = resolve
        })
    }

    function onConfirm() {
        open.value = false;
        _resolve(true)
    }

    function onCancel() {
        open.value = false;
        _resolve(false)
    }

    return {
        titleRef,
        descriptionRef,
        cancelLabelRef,
        confirmLabelRef,
        confirm,
        onConfirm,
        onCancel,
        open,
    }
}
