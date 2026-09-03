// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'
import type { AiidaDataNeed } from '@/types'

export const selectedPermissionCategory = ref<AiidaDataNeed['type']>('outbound-aiida')
