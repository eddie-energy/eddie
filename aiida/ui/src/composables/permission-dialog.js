import { ref } from 'vue'

/** @type {Ref<AiidaPermission>} */
const permission = ref(undefined)
/** @type {Ref<boolean>} */
const open = ref(false)

export function usePermissionDialog() {
  /**
   * Opens the dialog to accept or reject the target permission.
   * @param {AiidaPermission} target
   */
  function updatePermission(target) {
    permission.value = target
    open.value = true
  }

  return {
    updatePermission,
    permission,
    open,
  }
}
