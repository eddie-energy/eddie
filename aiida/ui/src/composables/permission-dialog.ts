// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaPermission } from '@/types'
import { ref } from 'vue'

const permission = ref<AiidaPermission>()
const open = ref<boolean>(false)

export function usePermissionDialog() {
  function updatePermission(target: AiidaPermission) {
    permission.value = target
    open.value = true
  }

  return {
    updatePermission,
    permission,
    open,
  }
}
