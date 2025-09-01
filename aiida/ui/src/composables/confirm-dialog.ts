import { ref } from 'vue'

const titleRef = ref('')
const descriptionRef = ref('')
const cancelLabelRef = ref('')
const confirmLabelRef = ref('')
const open = ref<boolean | undefined>(false)
const resolvePromise = ref<((value: PromiseLike<boolean> | boolean) => void) | null>(null)


export function useConfirmDialog() {
    async function confirm(title: string,
        description: string,
        cancelLabel?: string,
        confirmLabel?: string) {
        titleRef.value = title
        descriptionRef.value = description
        cancelLabelRef.value = cancelLabel ?? "Cancel"
        confirmLabelRef.value = confirmLabel ?? "Confirm"
        open.value = true

        return new Promise<boolean>((resolve) => {
            resolvePromise.value = resolve
        })
    }

    return {
        titleRef,
        descriptionRef,
        cancelLabelRef,
        confirmLabelRef,
        confirm,
        open,
        resolvePromise
    }
}
