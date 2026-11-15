// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { subscribeToConnectionStatusStream } from '@/api'
import { fetchPermissions, permissions } from '@/stores/permissions'

let abortController: AbortController | undefined

/**
 * Keeps the connection status of the permissions up to date, e.g. once the connection to the EDDIE framework is
 * established after accepting a permission.
 */
export function connect() {
  abortController?.abort()
  abortController = new AbortController()

  subscribeToConnectionStatusStream(({ permissionId, connectionStatus }) => {
    const permission = permissions.value.find((p) => p.permissionId === permissionId)
    if (permission) {
      permission.connectionStatus = connectionStatus
    }
    // Refetch, so a list request that was answered before the connection status changed cannot overwrite it
    void fetchPermissions()
  }, abortController.signal)
}
