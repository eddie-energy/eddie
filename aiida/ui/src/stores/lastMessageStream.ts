// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'
import { subscribeToLastMessageStream } from '@/api'

/**
 * One shared SSE stream for all permissions of one user.
 * HTTP/1.1 limits concurrent SSE streams to a maximum of 6.
 */
export const lastMessageByPermissionId = ref<Map<string, Date>>(new Map())

let abortController: AbortController | undefined

export function connect() {
  abortController?.abort()
  abortController = new AbortController()

  subscribeToLastMessageStream((event) => {
    lastMessageByPermissionId.value.set(event.permissionId, new Date(event.timestamp))
  }, abortController.signal)
}
