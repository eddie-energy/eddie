<script setup lang="ts">
import type { ToastTypes } from '@/types'
import ErrorToastIcon from '@/assets/icons/ErrorToastIcon.svg'
import InfoToastIcon from '@/assets/icons/InfoToastIcon.svg'
import WarningToastIcon from '@/assets/icons/WarningToastIcon.svg'
import SuccessToastIcon from '@/assets/icons/SuccessToastIcon.svg'
import CloseIcon from '@/assets/icons/CloseIcon.svg'
import useToast from '@/composables/useToast'
import { useI18n } from 'vue-i18n'

const { remove } = useToast()
const { t } = useI18n()

const {
  severity = 'info',
  message,
  duration = 5000,
  canClose = false,
  id,
} = defineProps<{
  severity?: ToastTypes
  message: string
  id: number
  duration?: number
  canClose?: boolean
}>()

const toastTypes: {
  [key: string]: {
    translation: string
    icon: string
  }
} = {
  info: {
    translation: 'toasts.info',
    icon: InfoToastIcon,
  },
  success: {
    translation: 'toasts.success',
    icon: SuccessToastIcon,
  },
  warning: {
    translation: 'toasts.warning',
    icon: WarningToastIcon,
  },
  danger: {
    translation: 'toasts.danger',
    icon: ErrorToastIcon,
  },
}

const progressBarDuration = `${duration}ms`
</script>

<template>
  <div class="toast" :class="[severity]" aria-live="polite" role="alert">
    <component :is="toastTypes[severity].icon" class="icon" />
    <div>
      <p class="toast-title text-normal">{{ t(toastTypes[severity].translation) }}</p>
      <p class="toast-message text-small">{{ message }}</p>
    </div>
    <button
      type="button"
      v-if="canClose"
      @click="remove(id)"
      aria-label="Close"
      class="close-button"
    >
      <CloseIcon />
    </button>

    <div class="toast-progress-bar" :class="{ hide: !duration }"></div>
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
  opacity: 1;
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
  min-width: 2.5rem;
  border-radius: var(--border-radius);
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
  animation: progress v-bind(progressBarDuration) linear;
}

.hide {
  display: none;
}

.close-button {
  cursor: pointer;
  margin-left: auto;
}

@keyframes progress {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}
</style>
