// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaPendingPermission, AiidaPermissionRequestsDTO } from '@/types'
import { ref } from 'vue'

const permission = ref<AiidaPendingPermission>()
const aiidaCode = ref<AiidaPermissionRequestsDTO>()
const open = ref<boolean>(false)
let _resolve: (value: boolean) => void

export function usePermissionDialog() {
  /**
   * Opens the dialog to accept or reject a pending permission. The AIIDA code is required, as pending permissions are
   * not persisted and have to be accepted or rejected with the handshake information of the AIIDA code.
   */
  async function decidePendingPermission(
    target: AiidaPendingPermission,
    code: AiidaPermissionRequestsDTO,
  ) {
    permission.value = target
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
