import type { ToastTypes } from '@/types'
import { reactive } from 'vue'

const toasts = reactive(
  new Map<
    number,
    { severity?: ToastTypes; message: string; duration?: number; canClose?: boolean }
  >(),
)

let nextId = 0

export default function useToast() {
  function notify(message: string, severity?: ToastTypes, duration?: number, canClose?: boolean) {
    const id = nextId++
    toasts.set(id, { severity, message, duration, canClose })
    if (!canClose) {
      setTimeout(() => remove(id), duration ?? 5000)
    }
  }
  function remove(id: number) {
    toasts.delete(id)
  }
  function info(message: string, duration: number, canClose?: boolean) {
    notify(message, 'info', duration, canClose)
  }
  function warn(message: string, duration?: number, canClose?: boolean) {
    notify(message, 'warning', duration, canClose)
  }
  function danger(message: string, duration?: number, canClose?: boolean) {
    notify(message, 'danger', duration, canClose)
  }
  function success(message: string, duration?: number, canClose?: boolean) {
    notify(message, 'success', duration, canClose)
  }

  return {
    toasts,
    notify,
    remove,
    info,
    warn,
    danger,
    success,
  }
}
