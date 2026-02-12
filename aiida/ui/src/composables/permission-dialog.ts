// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaPermission } from '@/types'
import { ref } from 'vue'

const permission = ref<AiidaPermission>()
const open = ref<boolean>(false)
let _resolve: (value: boolean) => void

export function usePermissionDialog() {
  async function updatePermission(target: AiidaPermission) {
    permission.value = target
    open.value = true
    return new Promise<boolean>((resolve) => {
      _resolve = resolve
    })
  }

  function resolveDialog() {
    _resolve(true)
  }

  return {
    updatePermission,
    resolveDialog,
    permission,
    open,
  }
}
