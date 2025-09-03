<script setup lang="ts">
import type { ToastTypes } from '@/types'
import { onMounted, ref, useId } from 'vue'
import ErrorToastIcon from '@/assets/icons/ErrorToastIcon.svg'
import InfoToastIcon from '@/assets/icons/InfoToastIcon.svg'
import WarningToastIcon from '@/assets/icons/WarningToastIcon.svg'
import SuccessToastIcon from '@/assets/icons/SuccessToastIcon.svg'
import CloseIcon from '@/assets/icons/CloseIcon.svg'

let timeoutId: ReturnType<typeof setTimeout> | null = null

const {
  severity = 'info',
  message,
  duration = 5000,
  canClose = false,
} = defineProps<{
  severity?: ToastTypes
  message: string
  duration?: number
  canClose?: boolean
}>()
const id = useId()
const progress = ref(100)
const isLeaving = ref(false)

const selfdestruct = () => {
  const el = document.getElementById(id)
  if (el && el.parentNode) {
    el.parentNode.parentNode?.removeChild(el.parentNode)
  }
}

onMounted(() => {
  if (timeoutId) clearTimeout(timeoutId)
  timeoutId = setTimeout(selfdestruct, duration)
  const interval = 10
  const decrement = 100 / (duration / interval)
  const progressInterval = setInterval(() => {
    progress.value -= decrement
    if (progress.value <= 0) {
      progress.value = 0
      clearInterval(progressInterval)
    }
    if (progress.value <= 5 && !isLeaving.value) {
      isLeaving.value = true
    }
  }, interval)
})

const toastTypes: {
  [key: string]: {
    title: string
    icon: string
  }
} = {
  info: {
    title: 'Did you know?',
    icon: InfoToastIcon,
  },
  success: {
    title: 'Congratulations!',
    icon: SuccessToastIcon,
  },
  warning: {
    title: 'Warning!',
    icon: WarningToastIcon,
  },
  danger: {
    title: 'Something went wrong!',
    icon: ErrorToastIcon,
  },
}
</script>

<template>
  <div
    class="toast"
    :class="[severity, isLeaving && 'toast-leave']"
    aria-live="polite"
    role="alert"
    :id
  >
    <component :is="toastTypes[severity].icon" class="icon" />
    <div>
      <p class="toast-title text-normal">{{ toastTypes[severity].title }}</p>
      <p class="toast-message text-small">{{ message }}</p>
    </div>
    <button
      type="button"
      v-if="canClose"
      @click="selfdestruct"
      aria-label="Close"
      class="close-button"
    >
      <CloseIcon />
    </button>

    <div class="toast-progress-bar" :style="{ width: progress + '%' }"></div>
  </div>
</template>

<style scoped>
.toast {
  --severity-color: var(--eddie-blue);
  z-index: 10;
  position: relative;
  border-radius: 1rem;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  background-color: var(--light);
  width: fit-content;
  padding: var(--spacing-md) var(--spacing-xlg);
  box-shadow: 0px 3px 10px 0px #00000040;
  min-width: 20vw;
  width: 100%;
  opacity: 0;
  animation: fadeIn 0.4s ease;
  animation-fill-mode: forwards;
  overflow: hidden;
}

.success {
  --severity-color: var(--eddie-green);
}

.warning {
  --severity-color: var(--eddie-yellow);
}

.danger {
  --severity-color: var(--eddie-red-medium);
}

.icon {
  min-width: 40px;
  border-radius: 0.5rem;
  background: linear-gradient(322.21deg, var(--severity-color) -3.82%, #ffffff 96.51%);
}

.toast-title {
  font-weight: 600;
}

.toast-progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 5px;
  background-color: var(--severity-color);
}

.close-button {
  cursor: pointer;
  margin-left: auto;
}

.toast-leave {
  animation: fadeOut 0.4s ease forwards;
}

@keyframes fadeOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(16px);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
