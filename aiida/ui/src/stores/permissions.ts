// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { computed, ref } from 'vue'
import { getPermissions } from '@/api.js'
import STATUS from '@/constants/permission-status'
import type { AiidaPermission } from '@/types'
import { connect as reconnectLastMessageStream } from '@/stores/lastMessageStream'

export const permissions = ref<AiidaPermission[]>([])

export function isMonitorablePermission(permission: AiidaPermission): boolean {
  return (
    STATUS[permission.status]?.isActive === true &&
    permission.dataNeed?.type === 'inbound-aiida' &&
    permission.dataNeed.schemas.includes('MIN-MAX-ENVELOPE-CIM-V1-12')
  )
}

export function isFcaPermission(permission: AiidaPermission): boolean {
  return permission.dataNeed.contexts?.includes('FLEXIBLE-CONNECTION-AGREEMENT') ?? false
}

export const monitorablePermissions = computed(() =>
  permissions.value.filter(isMonitorablePermission),
)

export async function fetchPermissions() {
  permissions.value = await getPermissions()
  reconnectLastMessageStream()
}
