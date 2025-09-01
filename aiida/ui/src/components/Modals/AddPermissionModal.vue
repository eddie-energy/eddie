<script setup lang="ts">
import Button from '@/components/Button.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import ScanQrCodeIcon from '@/assets/icons/ScanQrCodeIcon.svg'
import { ref } from 'vue'
import { addPermission } from '@/api'
import { fetchPermissions } from '@/stores/permissions'
import QrCodeScanner from '../QrCodeScanner.vue'
import { usePermissionDialog } from '@/composables/permission-dialog'
import type { AiidaPermissionRequest } from '@/types'

const { updatePermission } = usePermissionDialog()
const permissionModalRef = ref<HTMLDialogElement>()
const qrCodeModalRef = ref<HTMLDialogElement>()
const qrCodeIsOpen = ref(false)
const aiidaCode = ref('')
const aiidaCodeError = ref('')

const showModal = () => {
  aiidaCode.value = ''
  aiidaCodeError.value = ''
  permissionModalRef.value?.showModal()
}
const toggleQrCodeScanner = (open: boolean) => {
  if (open) {
    qrCodeIsOpen.value = true
    qrCodeModalRef.value?.showModal()
  } else {
    qrCodeIsOpen.value = false
    qrCodeModalRef.value?.close()
  }
}

const parseAiidaCode = (aiidaCode: string) => {
  if (!aiidaCode?.trim()) {
    throw new Error('Please fill out this field')
  }

  try {
    return JSON.parse(window.atob(aiidaCode))
  } catch (error: any) {
    if (error?.name === 'InvalidCharacterError') {
      console.debug('The AIIDA code does not appear to be encoded in Base64 format.', error)
      throw new Error('The AIIDA code should only contain letters, numbers, and "=" characters.')
    }
    if (error instanceof SyntaxError) {
      console.debug('The decoded AIIDA code could not be parsed into JSON format.', error)
    }

    throw new Error('The input does not appear to be a valid AIIDA code.')
  }
}

const executePermissionRequest = async (permissionRequest: AiidaPermissionRequest) => {
  delete permissionRequest.bearerToken
  try {
    const permission = await addPermission(permissionRequest)
    fetchPermissions()
    updatePermission(permission)
  } catch {
    //catch handled by notify from api methods
  }
  permissionModalRef.value?.close()
}

const handleAddPermission = async () => {
  try {
    const permissionRequest = parseAiidaCode(aiidaCode.value)
    executePermissionRequest(permissionRequest)
  } catch (error: any) {
    aiidaCodeError.value = error?.message ?? error?.toString() ?? 'An unknown error occurred.'
  }
}

const handleValidQrCode = (permissionRequest: AiidaPermissionRequest) => {
  toggleQrCodeScanner(false)
  executePermissionRequest(permissionRequest)
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog title="Add new Permission" ref="permissionModalRef">
    <form class="permission-form bottom-margin">
      <label for="code">New Permission *</label>
      <input
        type="text"
        id="code"
        placeholder="AIIDA Code"
        required
        class="code-input text-normal"
        v-model="aiidaCode"
      />
    </form>
    <Button class="bottom-margin" @click="toggleQrCodeScanner(true)">
      <ScanQrCodeIcon />
      Scan AIIDA Code
    </Button>
    <div class="bottom-margin" v-if="aiidaCodeError">
      <p class="heading-3">Error</p>
      {{ aiidaCodeError }}
    </div>
    <div class="two-item-pair">
      <Button button-style="error-secondary" @click="permissionModalRef?.close()">Cancel</Button>
      <Button @click="handleAddPermission">Add</Button>
    </div>
    <ModalDialog title="Scan QR Code" ref="qrCodeModalRef" @close="qrCodeIsOpen = false">
      <QrCodeScanner :open="qrCodeIsOpen" @valid="handleValidQrCode" />
      <div class="two-item-pair">
        <Button button-style="error-secondary" @click="toggleQrCodeScanner(false)">Cancel</Button>
      </div>
    </ModalDialog>
  </ModalDialog>
</template>

<style scoped>
.bottom-margin {
  margin-bottom: var(--spacing-xxl);
}

.two-item-pair {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.permission-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.code-input {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--eddie-grey-medium);
  border-radius: 0.5rem;
}
</style>
