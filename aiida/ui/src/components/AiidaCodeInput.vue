<script setup lang="ts">
import { useTemplateRef } from 'vue'
import { addPermission } from '@/api'
import QrCodeScanner from '@/components/QrCodeScanner.vue'
import { usePermissionDialog } from '@/composables/permission-dialog'
import { fetchPermissions } from '@/stores/permissions'

const { updatePermission } = usePermissionDialog()

const aiidaCodeInput = useTemplateRef<HTMLInputElement>('aiidaCodeInput')

function parseAiidaCode(aiidaCode: string) {
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

async function handleAddPermission() {
  try {
    const permissionRequest = parseAiidaCode(aiidaCodeInput.value!.value)
    const permission = await addPermission(permissionRequest)
    fetchPermissions()
    updatePermission(permission)
  } catch (error: any) {
    const message = error?.message ?? error?.toString() ?? 'An unknown error occurred.'
    if (aiidaCodeInput.value) {
      aiidaCodeInput.value.setCustomValidity(message)
      aiidaCodeInput.value.reportValidity()
    }
  }
}

function handleQrCodeResult(result: string) {
  aiidaCodeInput.value!.value = result
  handleAddPermission()
}
</script>

<template>
  <div>
    <sl-input ref="aiidaCodeInput" placeholder="AIIDA code"></sl-input>
    <sl-button variant="primary" @click="handleAddPermission">Add</sl-button>
    <QrCodeScanner @result="handleQrCodeResult"></QrCodeScanner>
  </div>
</template>

<style scoped>
div {
  display: flex;
  gap: 0.75rem;
}
</style>
