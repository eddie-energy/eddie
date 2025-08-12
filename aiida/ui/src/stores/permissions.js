import { ref } from 'vue'
import { getPermissions } from '@/api.js'

/** @type {Ref<AiidaPermission[]>} */
export const permissions = ref([])

/**
 * Updates the permissions store by fetching all permissions from the backend.
 * @returns {void}
 */
export async function fetchPermissions() {
  permissions.value = await getPermissions()
}
