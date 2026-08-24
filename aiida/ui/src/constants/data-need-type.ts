// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { AiidaDataNeed } from '@/types'

const DATA_NEED_TYPE = {
  INBOUND: 'inbound-aiida',
  OUTBOUND: 'outbound-aiida',
} as const satisfies Record<string, AiidaDataNeed['type']>

export default DATA_NEED_TYPE
