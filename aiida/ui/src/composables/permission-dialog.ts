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
