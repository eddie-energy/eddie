<!--
SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'
import Button from '@/components/Button.vue'
import CopyButton from '@/components/CopyButton.vue'
import { BASE_URL } from '@/api'
import type { AiidaPermission, InboundProvisioningType } from '@/types'
import { computed, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const modal = useTemplateRef<HTMLDialogElement>('modal')
const apiKey = ref('')
const permissionId = ref('')
const provisioningType = ref<InboundProvisioningType>()

const curlCommand = computed(() => {
  const endpoint = `${BASE_URL}/inbound/latest/${permissionId.value}`
  if (provisioningType.value === 'REST_API_TOKEN') {
    return `curl '${endpoint}?apiKey=${encodeURIComponent(apiKey.value)}'`
  }
  return `curl '${endpoint}' --header 'X-API-Key: ${apiKey.value}'`
})

const showModal = (newApiKey: string, permission: AiidaPermission) => {
  apiKey.value = newApiKey
  permissionId.value = permission.permissionId
  provisioningType.value = permission.dataSource?.provisioningType
  modal.value?.showModal()
}

const closeModal = () => {
  apiKey.value = ''
  permissionId.value = ''
  provisioningType.value = undefined
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog
    :title="t('permissions.restApiKey.credentialsTitle')"
    ref="modal"
    @close="closeModal"
  >
    <p class="warning text-normal">{{ t('permissions.restApiKey.credentialsWarning') }}</p>
    <div class="credential-field">
      <label for="rest-api-key">{{ t('permissions.restApiKey.apiKeyLabel') }}</label>
      <div class="input-field">
        <input id="rest-api-key" name="apiKey" readonly autocomplete="off" :value="apiKey" />
        <CopyButton :copy-text="apiKey" :aria-label="t('permissions.restApiKey.copyApiKey')" />
      </div>
    </div>
    <div class="credential-field">
      <label for="rest-api-request">{{ t('permissions.restApiKey.requestLabel') }}</label>
      <div class="input-field">
        <input id="rest-api-request" name="request" readonly :value="curlCommand" />
        <CopyButton
          :copy-text="curlCommand"
          :aria-label="t('permissions.restApiKey.copyRequest')"
        />
      </div>
    </div>
    <Button class="close-button" button-style="primary" @click="modal?.close()">
      {{ t('permissions.restApiKey.credentialsSaved') }}
    </Button>
  </ModalDialog>
</template>

<style scoped>
.warning,
.credential-field {
  margin-bottom: var(--spacing-lg);
}

.warning {
  max-width: 34rem;
}

.credential-field label {
  display: block;
  margin-bottom: var(--spacing-xs);
  font-weight: var(--font-weight-bold);
}

.input-field {
  position: relative;

  input {
    width: 100%;
    padding: var(--spacing-sm) calc(var(--spacing-xxl) + var(--spacing-md)) var(--spacing-sm)
      var(--spacing-md);
    border: 1px solid var(--eddie-grey-medium);
    border-radius: var(--border-radius);
  }

  .copy-button {
    position: absolute;
    right: var(--spacing-md);
    top: 50%;
    transform: translateY(-50%);
  }
}

.close-button {
  width: 100%;
  justify-content: center;
}

@media screen and (min-width: 640px) {
  .close-button {
    width: fit-content;
    margin-left: auto;
  }
}
</style>
