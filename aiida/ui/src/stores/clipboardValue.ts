import { ref } from 'vue'

export const clipboardValue = ref('')

export const checkClipboard = async () => {
  if (clipboardValue.value !== (await navigator.clipboard.readText())) {
    clipboardValue.value = ''
  }
}
