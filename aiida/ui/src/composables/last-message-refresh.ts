// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaPermission, LatestSchemaRecord } from '@/types'
import { getLatestInboundPermissionMessage, getLatestOutboundPermissionMessage } from '@/api'
import { lastMessageByPermissionId } from '@/stores/lastMessageStream'
import { computed, ref, watch } from 'vue'

/**
 * Keeps `lastMessageAt` up to date with the permission's latest inbound/outbound
 * message.
 * An initial value is fetched via REST on permission switch.
 * The shared last-message SSE stream (`@/stores/lastMessageStream`) tracks further messages.
 * Only one SSE stream per user and not per permission, as HTTP/1.1 limits concurrent SSE streams to a maximum of 6.
 */
export function useLastMessageRefresh(
  getPermission: () => AiidaPermission,
  isEnabled: () => boolean,
) {
  const initialLastMessageAt = ref<Date | null>(null)

  const fetchInitialLastMessageTimestamp = async () => {
    const permission = getPermission()
    try {
      if (permission.dataNeed.type === 'inbound-aiida') {
        const record = await getLatestInboundPermissionMessage(permission.permissionId, true)
        initialLastMessageAt.value = new Date(record.timestamp)
      } else {
        const record = await getLatestOutboundPermissionMessage(permission.permissionId, true)
        const latestSentAt = record.messages.reduce(
          (latest: number, message: LatestSchemaRecord) =>
            Math.max(latest, new Date(message.sentAt).getTime()),
          0,
        )
        initialLastMessageAt.value = latestSentAt > 0 ? new Date(latestSentAt) : null
      }
    } catch {
      initialLastMessageAt.value = null
    }
  }

  watch(
    () => getPermission().permissionId,
    async () => {
      initialLastMessageAt.value = null
      if (isEnabled()) {
        await fetchInitialLastMessageTimestamp()
      }
    },
    { immediate: true },
  )

  const lastMessageAt = computed(
    () =>
      lastMessageByPermissionId.value.get(getPermission().permissionId) ??
      initialLastMessageAt.value,
  )

  return { lastMessageAt }
}
