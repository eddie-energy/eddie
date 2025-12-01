<script setup lang="ts">
import CopyIcon from '@/assets/icons/CopyIcon.svg'
import CheckmarkIcon from '@/assets/icons/CheckmarkIcon.svg'
import { computed } from 'vue'
import { clipboardValue } from '@/stores/clipboardValue'

const { copyText } = defineProps<{
  copyText?: string
}>()

const handleCopy = () => {
  navigator.clipboard.writeText(copyText ?? '')
  clipboardValue.value = copyText ?? ''
}

const isCopied = computed(() => {
  return copyText === clipboardValue.value
})
</script>

<template>
  <button
    class="copy-button"
    :disabled="isCopied"
    :class="{ isCopied }"
    @click="handleCopy"
    type="button"
  >
    <Transition mode="out-in">
      <component :is="isCopied ? CheckmarkIcon : CopyIcon" />
    </Transition>
  </button>
</template>

<style scoped>
.copy-button {
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  color: var(--eddie-primary);
}

button.isCopied {
  cursor: unset;
  color: var(--eddie-green);
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.2s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}
</style>
