// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'
import { getInboundProvisioningTypes } from '@/api.ts'

export const inboundProvisioningTypes = ref({
  provisioningTypes: [] as string[],
})

export async function fetchInboundProvisioningTypes() {
  inboundProvisioningTypes.value = await getInboundProvisioningTypes()
}
