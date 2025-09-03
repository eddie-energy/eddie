import type { ToastTypes } from '@/types'
import { createVNode, render } from 'vue'
import AlertToast from '@/components/AlertToast.vue'

export function notify(message: string, severity?: ToastTypes, duration?: number, canClose?: boolean) {
    const vnode = createVNode(AlertToast, {
        severity,
        message,
        duration,
        canClose
    })
    const container = document.createElement('div')
    document.querySelector('.alert-list')?.appendChild(container)
    render(vnode, container)
}
export function info(message: string, duration: number, canClose?: boolean) {
    notify(message, "info", duration, canClose)
}
export function warn(message: string, duration?: number, canClose?: boolean) {
    notify(message, "warning", duration, canClose)
}
export function danger(message: string, duration?: number, canClose?: boolean) {
    notify(message, "danger", duration, canClose)
}
export function success(message: string, duration?: number, canClose?: boolean) {
    notify(message, "success", duration, canClose)
}