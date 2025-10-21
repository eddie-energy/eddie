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
import { useI18n } from 'vue-i18n'

const { updatePermission } = usePermissionDialog()
const permissionModal = ref<HTMLDialogElement>()
const qrCodeModal = ref<HTMLDialogElement>()
const qrCodeIsOpen = ref(false)
const aiidaCode = ref('')
const aiidaCodeError = ref('')
const loading = ref(false)
const { t } = useI18n()

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
    throw new Error(t('permissions.modal.errorEmpty'))
  }
  try {
    return JSON.parse(window.atob(aiidaCode))
  } catch (error: any) {
    if (error?.name === 'InvalidCharacterError') {
      console.debug('The AIIDA code does not appear to be encoded in Base64 format.', error)
      throw new Error(t('permissions.modal.errorWrong'))
    }
    if (error instanceof SyntaxError) {
      console.debug('The decoded AIIDA code could not be parsed into JSON format.', error)
    }

    throw new Error(t('permissions.modal.errorInvalid'))
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
    aiidaCodeError.value = error?.message ?? error?.toString() ?? t('errors.unexpectedError')
  }
}

const handleValidQrCode = (permissionRequest: AiidaPermissionRequest) => {
  toggleQrCodeModal(false)
  executePermissionRequest(permissionRequest)
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog
    :title="t('permissions.modal.title')"
    ref="permissionModal"
    :class="{ 'is-loading': loading }"
  >
    <form class="permission-form bottom-margin">
      <label for="code">{{ t('permissions.modal.inputLabel') }}*</label>
      <input
        type="text"
        id="code"
        :placeholder="t('permissions.modal.inputPlaceholder')"
        required
        class="code-input text-normal"
        v-model="aiidaCode"
      />
    </form>
    <Button class="bottom-margin hide-on-load" @click="toggleQrCodeModal(true)">
      <ScanQrCodeIcon />
      {{ t('permissions.modal.qrButton') }}
    </Button>
    <div class="bottom-margin" v-if="aiidaCodeError">
      <p class="heading-3">{{ t('permissions.modal.errorTitle') }}</p>
      {{ aiidaCodeError }}
    </div>
    <div class="action-buttons">
      <Button button-style="error-secondary" @click="permissionModal?.close()">
        {{ t('cancelButton') }}
      </Button>
      <Button @click="handleAddPermission" class="hide-on-load"> {{ t('addButton') }}</Button>
    </div>
    <ModalDialog
      :title="t('permissions.modal.qrTitle')"
      ref="qrCodeModal"
      @close="qrCodeIsOpen = false"
    >
      <QrCodeScanner :open="qrCodeIsOpen" @valid="handleValidQrCode" />
      <Button button-style="error-secondary" @click="toggleQrCodeModal(false)">
        {{ t('cancelButton') }}</Button
      >
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
