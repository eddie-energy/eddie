import type { ToastTypes } from '@/types'
import { createVNode, render } from 'vue'
import AlertToast from '@/components/AlertToast.vue'



export function notify(msg: string, serv?: ToastTypes, dur?: number, closeable?: boolean) {
    const vnode = createVNode(AlertToast, {
        severity: serv ?? "info",
        message: msg,
        duration: dur ?? 5000,
        canClose: closeable ?? false
    })
    const container = document.createElement('div')
    document.querySelector('.alert-list')?.appendChild(container)
    render(vnode, container)
}
export function info(msg: string, dur?: number, closeable?: boolean) {
    notify(msg, "info", dur, closeable)
}
export function warn(msg: string, dur?: number, closeable?: boolean) {
    notify(msg, "warning", dur, closeable)
}
export function danger(msg: string, dur?: number, closeable?: boolean) {
    notify(msg, "danger", dur, closeable)
}
export function success(msg: string, dur?: number, closeable?: boolean) {
    notify(msg, "success", dur, closeable)
}



