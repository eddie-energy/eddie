<script setup lang="ts">
import Button from '@/components/Button.vue'
import ModalDialog from '@/components/ModalDialog.vue'
import ScanQrCodeIcon from '@/assets/icons/ScanQrCodeIcon.svg'
import { ref } from 'vue'
import { addPermission } from '@/api'
import { fetchPermissions } from '@/stores/permissions'
import QrCodeScanner from '@/components/QrCodeScanner.vue'
import { usePermissionDialog } from '@/composables/permission-dialog'
import type { AiidaPermissionRequest } from '@/types'

const { updatePermission } = usePermissionDialog()
const permissionModal = ref<HTMLDialogElement>()
const qrCodeModal = ref<HTMLDialogElement>()
const qrCodeIsOpen = ref(false)
const aiidaCode = ref('')
const aiidaCodeError = ref('')
const loading = ref(false)

const showModal = () => {
  aiidaCode.value = ''
  aiidaCodeError.value = ''
  permissionModal.value?.showModal()
}

const toggleQrCodeModal = (open: boolean) => {
  if (open) {
    qrCodeIsOpen.value = true
    qrCodeModal.value?.showModal()
  } else {
    qrCodeIsOpen.value = false
    qrCodeModal.value?.close()
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
  loading.value = true
  try {
    const permission = await addPermission(permissionRequest)
    fetchPermissions()
    updatePermission(permission)
  } catch {
    //catch handled by notify from api functions
  }
  loading.value = false
  permissionModal.value?.close()
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
  toggleQrCodeModal(false)
  executePermissionRequest(permissionRequest)
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog title="Add new Permission" ref="permissionModal" :class="{ 'is-loading': loading }">
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
    <Button class="bottom-margin hide-on-load" @click="toggleQrCodeModal(true)">
      <ScanQrCodeIcon />
      Scan AIIDA Code
    </Button>
    <div class="bottom-margin" v-if="aiidaCodeError">
      <p class="heading-3">Error</p>
      {{ aiidaCodeError }}
    </div>
    <div class="action-buttons">
      <Button button-style="error-secondary" @click="permissionModal?.close()">Cancel</Button>
      <Button @click="handleAddPermission" class="hide-on-load">Add</Button>
    </div>
    <ModalDialog title="Scan QR Code" ref="qrCodeModal" @close="qrCodeIsOpen = false">
      <QrCodeScanner :open="qrCodeIsOpen" @valid="handleValidQrCode" />
      <Button button-style="error-secondary" @click="toggleQrCodeModal(false)">Cancel</Button>
    </ModalDialog>
    <div v-if="loading" class="loading-indicator"></div>
  </ModalDialog>
</template>

<style scoped>
.is-loading {
  .permission-form,
  .hide-on-load {
    opacity: 0;
  }
}
.bottom-margin {
  margin-bottom: var(--spacing-xxl);
}

.action-buttons {
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
  border-radius: var(--border-radius);
}
</style>
