// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'
import { getPermissions } from '@/api.js'
import type { AiidaPermission } from '@/types'

export const permissions = ref<AiidaPermission[]>([])

export async function fetchPermissions() {
  permissions.value = await getPermissions()
}
