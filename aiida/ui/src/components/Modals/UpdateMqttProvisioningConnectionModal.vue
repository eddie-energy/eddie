<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script lang="ts" setup>
import { computed, ref, useTemplateRef } from 'vue'
import ModalDialog from '@/components/ModalDialog.vue'
import Button from '@/components/Button.vue'
import CopyButton from '@/components/CopyButton.vue'
import CustomSelect from '@/components/CustomSelect.vue'
import type {
  AiidaPermission,
  InboundProvisioningType,
  ProvisioningConnectionDto,
  ProvisioningTypePatchDto,
} from '@/types'
import { patchInboundProvisioning } from '@/api'
import { fetchPermissions } from '@/stores/permissions'
import { useI18n } from 'vue-i18n'
import {
  fetchInboundProvisioningTypes,
  inboundProvisioningTypes,
} from '@/stores/provisioningTypes.ts'

const { t } = useI18n()
const modalRef = useTemplateRef('modal')
const permission = ref<AiidaPermission>()
const inboundProvisioningType = ref<InboundProvisioningType>('REST_API_TOKEN')
const loading = ref(false)
const error = ref('')
const provisioningResult = ref<ProvisioningConnectionDto>()

const host = ref('')
const username = ref('')
const password = ref('')
const topic = ref('')

const inboundProvisioningTypeOptions = computed(() =>
  inboundProvisioningTypes.value.provisioningTypes.map((type) => ({
    label: t(`permissions.mqttProvisioning.types.${type}`),
    value: type,
  })),
)

const requiresMqttClientDetails = computed(() => inboundProvisioningType.value === 'MQTT_CLIENT')
const credentialsText = computed(() => {
  if (!provisioningResult.value) {
    return ''
  }

  return [
    `MQTT_HOST=${provisioningResult.value.host}`,
    `MQTT_USERNAME=${provisioningResult.value.username}`,
    `MQTT_PASSWORD=${provisioningResult.value.password}`,
    `MQTT_TOPIC=${provisioningResult.value.topic}`,
  ].join('\n')
})

const clearSensitiveState = () => {
  provisioningResult.value = undefined
  password.value = ''
}

const closeModal = () => {
  modalRef.value?.close()
}

const showModal = async (targetPermission: AiidaPermission) => {
  clearSensitiveState()
  permission.value = targetPermission
  error.value = ''
  inboundProvisioningType.value = targetPermission.dataSource?.provisioningType ?? 'REST_BEARER'

  if (!inboundProvisioningTypes.value.provisioningTypes.length) {
    await fetchInboundProvisioningTypes()
  }

  const connection = targetPermission.dataSource?.mqttProvisioningConfig?.connection

  host.value = connection?.externalHost ?? ''
  username.value = connection?.username ?? ''
  topic.value = ''

  modalRef.value?.showModal()
}

const handleSubmit = async () => {
  if (!permission.value) {
    return
  }

  loading.value = true
  error.value = ''

  try {
    const payload: ProvisioningTypePatchDto =
      inboundProvisioningType.value === 'MQTT_CLIENT'
        ? {
            type: inboundProvisioningType.value,
            host: host.value,
            username: username.value,
            password: password.value,
            topic: topic.value,
          }
        : { type: inboundProvisioningType.value }
    const result = await patchInboundProvisioning(permission.value.permissionId, payload)

    if (inboundProvisioningType.value === 'MQTT_SERVER') {
      provisioningResult.value = result
    }

    await fetchPermissions()
    if (inboundProvisioningType.value !== 'MQTT_SERVER') {
      closeModal()
    }
  } catch (e: any) {
    error.value = e?.message ?? t('errors.unexpectedError')
  } finally {
    loading.value = false
  }
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog
    ref="modal"
    :class="{ 'is-loading': loading }"
    :title="t('permissions.mqttProvisioning.title')"
    @close="clearSensitiveState"
  >
    <section v-if="provisioningResult" class="mqtt-credentials">
      <h3 class="heading-3">
        {{ t('permissions.mqttProvisioning.credentialsTitle') }}
      </h3>
      <p class="credential-warning" role="alert">
        {{ t('permissions.mqttProvisioning.credentialsWarning') }}
      </p>

      <dl class="credential-list">
        <div class="credential-row">
          <dt>{{ t('permissions.mqttProvisioning.host') }}</dt>
          <dd>
            <code>{{ provisioningResult.host }}</code>
            <CopyButton
              :aria-label="t('permissions.mqttProvisioning.copyHost')"
              :copy-text="provisioningResult.host"
            />
          </dd>
        </div>
        <div class="credential-row">
          <dt>{{ t('permissions.mqttProvisioning.username') }}</dt>
          <dd>
            <code>{{ provisioningResult.username }}</code>
            <CopyButton
              :aria-label="t('permissions.mqttProvisioning.copyUsername')"
              :copy-text="provisioningResult.username"
            />
          </dd>
        </div>
        <div class="credential-row">
          <dt>{{ t('permissions.mqttProvisioning.password') }}</dt>
          <dd>
            <code>{{ provisioningResult.password }}</code>
            <CopyButton
              :aria-label="t('permissions.mqttProvisioning.copyPassword')"
              :copy-text="provisioningResult.password"
            />
          </dd>
        </div>
        <div class="credential-row">
          <dt>{{ t('permissions.mqttProvisioning.topic') }}</dt>
          <dd>
            <code>{{ provisioningResult.topic }}</code>
            <CopyButton
              :aria-label="t('permissions.mqttProvisioning.copyTopic')"
              :copy-text="provisioningResult.topic"
            />
          </dd>
        </div>
      </dl>

      <div class="copy-all">
        <span>{{ t('permissions.mqttProvisioning.copyAll') }}</span>
        <CopyButton
          :aria-label="t('permissions.mqttProvisioning.copyAll')"
          :copy-text="credentialsText"
        />
      </div>

      <div class="credentials-actions">
        <Button @click="closeModal">
          {{ t('permissions.mqttProvisioning.credentialsSaved') }}
        </Button>
      </div>
    </section>

    <form v-else class="mqtt-form" @submit.prevent="handleSubmit">
      <label id="inboundProvisioningTypeLabel">
        {{ t('permissions.mqttProvisioning.mode') }}*
      </label>
      <CustomSelect
        v-model="inboundProvisioningType"
        :options="inboundProvisioningTypeOptions"
        :placeholder="t('permissions.mqttProvisioning.modePlaceholder')"
        aria-labelledby="inboundProvisioningTypeLabel"
      />

      <template v-if="requiresMqttClientDetails">
        <label for="mqttHost">{{ t('permissions.mqttProvisioning.host') }}*</label>
        <input id="mqttHost" v-model="host" class="text-normal" required type="text" />

        <label for="mqttUsername">{{ t('permissions.mqttProvisioning.username') }}*</label>
        <input id="mqttUsername" v-model="username" class="text-normal" required type="text" />

        <label for="mqttPassword">{{ t('permissions.mqttProvisioning.password') }}*</label>
        <input id="mqttPassword" v-model="password" class="text-normal" required type="password" />

        <label for="mqttTopic">{{ t('permissions.mqttProvisioning.topic') }}*</label>
        <input id="mqttTopic" v-model="topic" class="text-normal" required type="text" />
      </template>

      <p v-if="error" class="heading-3 error-text">{{ error }}</p>

      <div class="action-buttons">
        <Button button-style="error-secondary" type="button" @click="closeModal">
          {{ t('cancelButton') }}
        </Button>
        <Button :disabled="loading">
          {{ t('saveButton') }}
        </Button>
      </div>
    </form>

    <div v-if="loading" class="loading-indicator"></div>
  </ModalDialog>
</template>

<style scoped>
.is-loading {
  .mqtt-form,
  .mqtt-credentials {
    opacity: 0;
  }
}

.mqtt-form,
.mqtt-credentials {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

input {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--eddie-grey-medium);
  border-radius: var(--border-radius);
}

.error-text {
  margin-top: var(--spacing-md);
  color: var(--eddie-red-medium);
}

.credential-warning {
  margin: var(--spacing-sm) 0 var(--spacing-lg);
  padding: var(--spacing-md);
  color: var(--eddie-red-dark);
  background: var(--eddie-red-background);
  border-radius: var(--border-radius);
}

.credential-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.credential-row {
  display: grid;
  gap: var(--spacing-xs);
}

.credential-row dt {
  font-weight: 600;
}

.credential-row dd,
.copy-all {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
  min-width: 0;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--eddie-secondary);
  border-radius: var(--border-radius);
}

.credential-row code {
  overflow-wrap: anywhere;
}

.copy-all {
  margin-top: var(--spacing-md);
  font-weight: 600;
}

.credentials-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-xl);
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-xxl);
}
</style>
