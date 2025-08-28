import { ref } from 'vue'
import { getPermissions } from '@/api.js'
import type { AiidaPermission } from '@/types'

export const permissions = ref<AiidaPermission[]>([])

export async function fetchPermissions() {
  permissions.value = await getPermissions()
}
