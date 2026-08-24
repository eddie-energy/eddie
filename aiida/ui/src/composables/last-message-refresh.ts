// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaPermission, LatestSchemaRecord } from '@/types'
import {
  getLatestInboundPermissionMessage,
  getLatestOutboundPermissionMessage,
  getNextExpectedTransmission,
} from '@/api'
import DATA_NEED_TYPE from '@/constants/data-need-type'
import STATUS from '@/constants/permission-status'
import { computed, onBeforeUnmount, ref, watch } from 'vue'

// Fallback if no effective transmission schedule is present
const DEFAULT_REFRESH_DELAY_MS = 60_000
// Buffer for clock lag
const REFRESH_BUFFER_MS = 2_000
const MIN_REFRESH_DELAY_MS = 2_000

/**
 * Keeps `lastMessageAt` up to date with the permission's latest inbound/outbound
 * message
 * Polling is based on the transmission schedule expecting a value at those intervals.
 * Polling stops when a terminal status is reached.
 */
export function useLastMessageRefresh(
  getPermission: () => AiidaPermission,
  isEnabled: () => boolean,
) {
  const lastMessageAt = ref<Date | null>(null)
  let refreshTimer: ReturnType<typeof setTimeout> | undefined

  const isTerminalStatus = computed(() => {
    const status = STATUS[getPermission().status]
    return !status?.isActive && !status?.isOpen
  })

  const fetchLastMessageTimestamp = async () => {
    const permission = getPermission()
    try {
      if (permission.dataNeed.type === DATA_NEED_TYPE.INBOUND) {
        const record = await getLatestInboundPermissionMessage(permission.permissionId, true)
        lastMessageAt.value = new Date(record.timestamp)
      } else {
        const record = await getLatestOutboundPermissionMessage(permission.permissionId, true)
        const latestSentAt = record.messages.reduce(
          (latest: number, message: LatestSchemaRecord) =>
            Math.max(latest, new Date(message.sentAt).getTime()),
          0,
        )
        lastMessageAt.value = latestSentAt > 0 ? new Date(latestSentAt) : null
      }
    } catch {
      lastMessageAt.value = null
    }
  }

  const fetchNextExpectedTransmission = async (): Promise<Date | null> => {
    try {
      const { nextExpectedAt } = await getNextExpectedTransmission(getPermission().permissionId)
      return nextExpectedAt ? new Date(nextExpectedAt) : null
    } catch {
      return null
    }
  }

  const scheduleRefreshCycle = async () => {
    if (!isEnabled()) {
      return
    }

    if (isTerminalStatus.value) {
      await fetchLastMessageTimestamp()
      return
    }

    const [, nextExpectedAt] = await Promise.all([
      fetchLastMessageTimestamp(),
      fetchNextExpectedTransmission(),
    ])

    const delay = nextExpectedAt
      ? Math.max(nextExpectedAt.getTime() - Date.now() + REFRESH_BUFFER_MS, MIN_REFRESH_DELAY_MS)
      : DEFAULT_REFRESH_DELAY_MS

    refreshTimer = setTimeout(scheduleRefreshCycle, delay)
  }

  watch(
    () => getPermission().permissionId,
    () => {
      clearTimeout(refreshTimer)
      scheduleRefreshCycle()
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    clearTimeout(refreshTimer)
  })

  return { lastMessageAt }
}
