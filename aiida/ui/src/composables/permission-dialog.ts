// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type {
  AiidaPendingPermission,
  AiidaPermissionPreview,
  AiidaPermissionRequestsDTO,
} from '@/types'
import { ref } from 'vue'

const permission = ref<AiidaPermissionPreview>()
const aiidaCode = ref<AiidaPermissionRequestsDTO>()
const open = ref<boolean>(false)
let _resolve: (value: boolean) => void

/**
 * Pending permissions are not persisted yet. To reuse the permission details view, they are shown as a permission
 * without a status.
 */
function toPermissionPreview(pending: AiidaPendingPermission): AiidaPermissionPreview {
  return {
    ...pending,
    transmissionEnabled: true,
    userId: '',
    unimplemented: { packageGraph: undefined, targetIP: undefined },
  }
}

export function usePermissionDialog() {
  /**
   * Opens the dialog to accept or reject a pending permission. The AIIDA code is required, as pending permissions are
   * not persisted and have to be accepted or rejected with the handshake information of the AIIDA code.
   */
  async function decidePendingPermission(
    target: AiidaPendingPermission,
    code: AiidaPermissionRequestsDTO,
  ) {
    permission.value = toPermissionPreview(target)
    aiidaCode.value = code
    open.value = true
    return new Promise<boolean>((resolve) => {
      _resolve = resolve
    })
  }

  function resolveDialog() {
    _resolve(true)
  }

  return {
    decidePendingPermission,
    resolveDialog,
    permission,
    aiidaCode,
    open,
  }
}
