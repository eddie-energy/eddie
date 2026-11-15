<!-- SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import type {
  AiidaPendingPermission,
  AiidaPermission,
  InboundMessageFormat,
  PermissionTypes,
} from '@/types'
import StatusTag from './StatusTag.vue'
import cronstrue from 'cronstrue/i18n'
import Button from '@/components/Button.vue'
import RevokeIcon from '@/assets/icons/RevokeIcon.svg'
import PenIcon from '@/assets/icons/PenIcon.svg'
import { useConfirmDialog } from '@/composables/confirm-dialog'
import { useLastMessageRefresh } from '@/composables/last-message-refresh'
import {
  deleteConnectionLimitMonitoring,
  revokePermission,
  updateConnectionLimitMonitoring,
  updateInboundMessageFormat,
} from '@/api'
import { dataSources, fetchDataSources } from '@/stores/dataSources'
import {
  fetchPermissions,
  isAcceptedPermission,
  isMonitorablePermission,
  permissions,
} from '@/stores/permissions'
import { computed, onMounted, ref, watch } from 'vue'
import EyeIcon from '@/assets/icons/EyeIcon.svg'
import MessageDownloadButton from '@/components/MessageDownloadButton.vue'
import { useI18n } from 'vue-i18n'
import CustomSelect from './CustomSelect.vue'
import ConnectionMonitorIcon from '@/assets/icons/ConnectionMonitorIcon.svg'

const { t, locale } = useI18n()
const { confirm } = useConfirmDialog()
const { permission, status } = defineProps<{
  /** Pending permissions are not accepted yet and only previewed before accepting or rejecting them. */
  permission: AiidaPermission | AiidaPendingPermission
  status?: PermissionTypes
}>()
/** Only accepted permissions can be acted on, pending permissions are not persisted yet. */
const acceptedPermission = computed(() =>
  isAcceptedPermission(permission) ? permission : undefined,
)
const dataSource = computed(() => acceptedPermission.value?.dataSource)
/** Pending permissions do not have their own schedule yet, they use the schedule of the data need. */
const transmissionSchedule = computed(
  () =>
    acceptedPermission.value?.effectiveTransmissionSchedule ??
    permission.dataNeed.transmissionSchedule,
)
const emit = defineEmits<{
  configureInboundProvisioning: [permission: AiidaPermission]
  resetInboundServerPassword: [permission: AiidaPermission]
  resetInboundRestApiKey: [permission: AiidaPermission]
  /** The monitoring data source selected for a pending permission, which is assigned once it is accepted. */
  selectMonitoringDataSource: [dataSourceId: string | undefined]
}>()

const selectedInboundMessageFormat = ref(permission.inboundMessageFormat)

const inboundMessageFormatOptions: { label: string; value: InboundMessageFormat }[] = [
  {
    label: t('permissions.inboundMessageFormats.CIM_1_12'),
    value: 'CIM_1_12',
  },
  {
    label: t('permissions.inboundMessageFormats.OPENADR_3_1'),
    value: 'OPENADR_3_1',
  },
]

const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  minute: '2-digit',
  hour: '2-digit',
  second: '2-digit',
})

// AIIDA derives the start and end of a permission from whole days in UTC, so they are shown in UTC as well
const permissionPeriodFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  minute: '2-digit',
  hour: '2-digit',
  second: '2-digit',
  timeZone: 'UTC',
  timeZoneName: 'short',
})

watch(
  () => permission.inboundMessageFormat,
  (inboundMessageFormat) => {
    selectedInboundMessageFormat.value = inboundMessageFormat
  },
)

const handleRevoke = async () => {
  if (
    await confirm(
      t('permissions.revokeTitle'),
      t('permissions.revokeDescription'),
      t('revokeButton'),
      t('cancelButton'),
    )
  ) {
    const promise = revokePermission(permission.permissionId)
    promise.then(() => fetchPermissions())
  }
}

const handleInboundMessageFormatSelection = async (
  inboundMessageFormat: string | number | boolean | undefined,
) => {
  if (inboundMessageFormat && inboundMessageFormat !== permission.inboundMessageFormat) {
    if (
      await confirm(
        t('permissions.updateInboundMessageFormatTitle'),
        t('permissions.updateInboundMessageFormatDescription', {
          format: t(`permissions.inboundMessageFormats.${inboundMessageFormat}`),
        }),
        t('editButton'),
        t('cancelButton'),
      )
    ) {
      try {
        await updateInboundMessageFormat(
          permission.permissionId,
          inboundMessageFormat as InboundMessageFormat,
        )
      } catch {
        selectedInboundMessageFormat.value = permission.inboundMessageFormat
      }
    } else {
      selectedInboundMessageFormat.value = permission.inboundMessageFormat
    }
  }
}

const openInboundProvisioningModal = () => {
  if (acceptedPermission.value) {
    emit('configureInboundProvisioning', acceptedPermission.value)
  }
}

const isInboundMqttProvisioning = computed(
  () =>
    permission.dataNeed.type === 'inbound-aiida' &&
    (dataSource.value?.provisioningType === 'MQTT_SERVER' ||
      dataSource.value?.provisioningType === 'MQTT_CLIENT'),
)

const isInboundRestProvisioning = computed(
  () =>
    permission.dataNeed.type === 'inbound-aiida' &&
    (dataSource.value?.provisioningType === 'REST_BEARER' ||
      dataSource.value?.provisioningType === 'REST_API_TOKEN'),
)

const inboundMqttConnection = computed(() => dataSource.value?.mqttProvisioningConfig?.connection)
const inboundMqttServerUri = computed(
  () => inboundMqttConnection.value?.externalHost ?? inboundMqttConnection.value?.internalHost,
)
const inboundMqttTopic = computed(() => dataSource.value?.mqttProvisioningConfig?.topic)

const resetInboundRestApiKey = async () => {
  if (
    await confirm(
      t('permissions.restApiKey.resetTitle'),
      t('permissions.restApiKey.resetDescription'),
      t('permissions.restApiKey.resetButton'),
      t('cancelButton'),
    )
  ) {
    if (acceptedPermission.value) {
      emit('resetInboundRestApiKey', acceptedPermission.value)
    }
  }
}

const dataSourceDisplayName = computed(() => {
  const currentDataSource = dataSource.value
  if (!currentDataSource) return undefined
  const inboundPermission = permissions.value.find(
    (p) => p.dataNeed.type === 'inbound-aiida' && p.dataSource?.id === currentDataSource.id,
  )
  return inboundPermission?.displayName ?? currentDataSource.name
})

const NO_MONITORING_DATA_SOURCE = 'NONE'
const monitoringDataSourceId = ref<string>(
  acceptedPermission.value?.monitoringDataSourceId ?? NO_MONITORING_DATA_SOURCE,
)

const monitoringDataSourceOptions = computed(() => [
  { label: t('permissions.dropdown.monitoringDataSourceNone'), value: NO_MONITORING_DATA_SOURCE },
  ...dataSources.value.map(({ id, name }) => ({ label: name, value: id })),
])

const handleMonitoringDataSourceSelection = async (
  value: string | number | boolean | undefined,
) => {
  if (!acceptedPermission.value) {
    emit(
      'selectMonitoringDataSource',
      value === NO_MONITORING_DATA_SOURCE ? undefined : String(value),
    )
    return
  }

  try {
    if (value === NO_MONITORING_DATA_SOURCE) {
      await deleteConnectionLimitMonitoring(permission.permissionId)
    } else {
      await updateConnectionLimitMonitoring(permission.permissionId, String(value))
    }
    await fetchPermissions()
  } catch {
    monitoringDataSourceId.value =
      acceptedPermission.value?.monitoringDataSourceId ?? NO_MONITORING_DATA_SOURCE
  }
}

onMounted(async () => {
  if (permission.supportsConnectionLimits && !dataSources.value.length) {
    await fetchDataSources()
  }
})

const { lastMessageAt } = useLastMessageRefresh(
  () => permission,
  () => !!status,
)
</script>

<template>
  <dl class="permission-details">
    <div class="column">
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.service') }}</dt>
        <dd>{{ permission.serviceName }}</dd>
      </div>
      <div v-if="acceptedPermission" class="permission-field permission-field--status">
        <dt>{{ t('permissions.dropdown.status') }}</dt>
        <dd>
          <StatusTag :status-type="status !== 'Complete' ? 'healthy' : 'unhealthy'">
            {{ t(acceptedPermission.connectionStatus ?? acceptedPermission.status) }}
          </StatusTag>
        </dd>
      </div>
      <div v-if="acceptedPermission?.grantTime" class="permission-field">
        <dt>{{ t('permissions.dropdown.creationDate') }}</dt>
        <dd>
          {{ dateTimeFormat.format(new Date(acceptedPermission.grantTime)) }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.eddieApplication') }}</dt>
        <dd>
          {{ permission.eddieId }}
        </dd>
      </div>
      <div class="permission-field" v-if="permission.meterId">
        <dt>{{ t('permissions.dropdown.meterId') }}</dt>
        <dd>{{ permission.meterId }}</dd>
      </div>
      <div class="permission-field" v-if="permission.minLimitKw != null">
        <dt>{{ t('permissions.dropdown.minLimitKw') }}</dt>
        <dd>{{ permission.minLimitKw }} kW</dd>
      </div>
      <div class="permission-field" v-if="permission.maxLimitKw != null">
        <dt>{{ t('permissions.dropdown.maxLimitKw') }}</dt>
        <dd>{{ permission.maxLimitKw }} kW</dd>
      </div>
      <template v-if="permission.supportsConnectionLimits">
        <div class="permission-field permission-field--select">
          <dt>{{ t('permissions.dropdown.monitoringDataSource') }}</dt>
          <dd>
            <CustomSelect
              v-model="monitoringDataSourceId"
              :options="monitoringDataSourceOptions"
              :placeholder="t('permissions.dropdown.monitoringDataSourcePlaceholder')"
              @update:model-value="handleMonitoringDataSourceSelection"
            />
          </dd>
        </div>
        <p class="monitoring-hint">
          {{ t('permissions.dropdown.monitoringDataSourceHint') }}
        </p>
      </template>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.start') }}</dt>
        <dd>{{ permissionPeriodFormat.format(new Date(permission.startTime)) }}</dd>
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.end') }}</dt>
        <dd>{{ permissionPeriodFormat.format(new Date(permission.expirationTime)) }}</dd>
      </div>
      <div v-if="transmissionSchedule" class="permission-field schedule">
        <dt>{{ t('permissions.dropdown.transmissionSchedule') }}</dt>
        <dd>
          {{
            cronstrue.toString(transmissionSchedule, {
              locale: locale,
            })
          }}
        </dd>
      </div>
      <div v-if="acceptedPermission" class="permission-field">
        <dt>{{ t('permissions.dropdown.transmissionEnabled') }}</dt>
        <dd>
          <StatusTag
            :status-type="acceptedPermission.transmissionEnabled ? 'healthy' : 'unhealthy'"
          >
            {{
              acceptedPermission.transmissionEnabled
                ? t('permissions.dropdown.transmissionOn')
                : t('permissions.dropdown.transmissionOff')
            }}
          </StatusTag>
        </dd>
      </div>
      <template v-if="isInboundMqttProvisioning">
        <div class="permission-field">
          <dt>{{ t('permissions.mqttProvisioning.mode') }}</dt>
          <dd>
            {{ t(`permissions.mqttProvisioning.types.${dataSource?.provisioningType}`) }}
          </dd>
        </div>
      </template>
      <template v-if="isInboundMqttProvisioning && inboundMqttConnection">
        <div v-if="inboundMqttServerUri" class="permission-field">
          <dt>{{ t('datasources.card.mqttServerUri') }}</dt>
          <dd>{{ inboundMqttServerUri }}</dd>
        </div>
        <div class="permission-field">
          <dt>{{ t('datasources.card.mqttUsername') }}</dt>
          <dd>{{ inboundMqttConnection.username }}</dd>
        </div>
        <div v-if="inboundMqttTopic" class="permission-field">
          <dt>{{ t('datasources.card.mqttTopic') }}</dt>
          <dd>{{ inboundMqttTopic }}</dd>
        </div>
      </template>
    </div>
    <div class="column">
      <template v-if="permission.dataNeed.type !== 'inbound-aiida'">
        <div class="permission-field">
          <dt>{{ t('permissions.dropdown.asset') }}</dt>
          <dd>
            {{ permission.dataNeed.asset }}
          </dd>
        </div>
        <div class="permission-field schemas">
          <dt>{{ t('permissions.dropdown.obisCodes') }}</dt>
          <div class="column schema">
            <dd v-for="tag in permission.dataNeed.dataTags" :key="tag">
              {{ tag }}
            </dd>
          </div>
        </div>
        <div v-if="dataSource" class="permission-field">
          <dt>{{ t('permissions.dropdown.dataSource') }}</dt>
          <dd>{{ dataSourceDisplayName ?? 'undefined' }}</dd>
        </div>
        <div v-if="acceptedPermission?.mqttStreamingConfig?.serverUri" class="permission-field">
          <dt>Streaming Uri</dt>
          <dd>{{ acceptedPermission.mqttStreamingConfig.serverUri }}</dd>
        </div>
        <div v-if="acceptedPermission?.mqttStreamingConfig?.dataTopic" class="permission-field">
          <dt>Streaming Topic</dt>
          <dd>{{ acceptedPermission.mqttStreamingConfig.dataTopic }}</dd>
        </div>
      </template>
      <template v-if="permission.dataNeed.type === 'inbound-aiida'">
        <div class="permission-field permission-field--select">
          <dt>{{ t('permissions.dropdown.inboundMessageFormat') }}</dt>
          <dd v-if="!acceptedPermission">
            {{
              t(
                `permissions.inboundMessageFormats.${permission.inboundMessageFormat ?? 'CIM_1_12'}`,
              )
            }}
          </dd>
          <dd v-else>
            <CustomSelect
              v-model="selectedInboundMessageFormat"
              :options="inboundMessageFormatOptions"
              :placeholder="
                t(`permissions.inboundMessageFormats.${permission.inboundMessageFormat}`)
              "
              @update:model-value="handleInboundMessageFormatSelection"
            />
          </dd>
        </div>
      </template>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.schemas') }}</dt>
        <div class="column schema">
          <dd v-for="schema in permission.dataNeed.schemas" :key="schema">
            {{ schema }}
          </dd>
        </div>
      </div>
      <div v-if="permission.dataNeed.allowedPermissionCommands?.length" class="permission-field">
        <dt>{{ t('permissions.dropdown.allowedPermissionCommands') }}</dt>
        <div class="column schema">
          <dd v-for="command in permission.dataNeed.allowedPermissionCommands" :key="command">
            {{ command }}
          </dd>
        </div>
      </div>
      <div v-if="permission.dataNeed.acknowledgementRequired" class="permission-field">
        <dt>{{ t('permissions.dropdown.acknowledgementRequired') }}</dt>
        <dd>{{ permission.dataNeed.acknowledgementRequired }}</dd>
      </div>
      <div class="permission-field" v-if="permission.permissionId">
        <dt>{{ t('permissions.dropdown.permissionID') }}</dt>
        <dd>{{ permission.permissionId }}</dd>
      </div>
      <div v-if="status" class="permission-field">
        <dt>
          {{
            permission.dataNeed.type === 'inbound-aiida'
              ? t('permissions.dropdown.lastMessageReceived')
              : t('permissions.dropdown.lastMessageSent')
          }}
        </dt>
        <dd>{{ lastMessageAt ? dateTimeFormat.format(lastMessageAt) : 'N/A' }}</dd>
      </div>
      <div v-if="status === 'Active' && acceptedPermission" class="permission-actions">
        <div
          v-if="isMonitorablePermission(acceptedPermission)"
          class="actions-row actions-row--end"
        >
          <Button
            :isLink="true"
            :href="`/connection-monitor?permission=${permission.permissionId}`"
          >
            <ConnectionMonitorIcon />
            {{ t('permissions.openConnectionMonitor') }}
          </Button>
        </div>
        <div
          v-if="permission.dataNeed.type === 'inbound-aiida'"
          class="actions-row actions-row--end"
        >
          <Button
            v-if="dataSource?.provisioningType === 'MQTT_SERVER'"
            button-style="secondary"
            class="action-btn"
            @click="emit('resetInboundServerPassword', acceptedPermission)"
          >
            {{ t('datasources.card.resetPasswordButton') }}
          </Button>
          <Button
            v-if="isInboundRestProvisioning"
            button-style="secondary"
            class="action-btn"
            @click="resetInboundRestApiKey"
          >
            {{ t('permissions.restApiKey.resetButton') }}
          </Button>
          <Button button-style="primary" class="action-btn" @click="openInboundProvisioningModal">
            <PenIcon /> {{ t('permissions.mqttProvisioning.openButton') }}
          </Button>
        </div>
        <div class="actions-row">
          <Button button-style="error" class="action-btn" @click="handleRevoke">
            <RevokeIcon /> {{ t('revokeButton') }}
          </Button>
          <MessageDownloadButton :data="acceptedPermission" class="action-btn">
            <EyeIcon /> {{ t('permissions.dropdown.downloadLatestMessageButton') }}
          </MessageDownloadButton>
        </div>
      </div>
    </div>
  </dl>
</template>

<style scoped>
.column {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}
.column:first-child {
  margin-bottom: var(--spacing-sm);
}

.permission-field {
  display: flex;
  justify-content: space-between;
  border: 1px solid var(--eddie-grey-medium);
  color: var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-sm);
  border-radius: var(--border-radius);
  font-size: 1rem;
  line-height: 1.5;
  gap: 0.25rem;
  flex-direction: column;

  dd {
    word-break: break-all;
    line-height: 1;
    font-size: 0.9rem;
    color: var(--eddie-grey-medium);
    font-weight: 600;
  }
}

.permission-field--select,
.permission-field--status {
  padding: unset;
  border: unset;
  gap: 0.5rem;
  align-items: center;
  flex-direction: row;

  dt {
    padding: var(--spacing-sm) var(--spacing-sm);
    border: 1px solid var(--eddie-grey-medium);
    width: 100%;
    border-radius: var(--border-radius);
  }

  dd {
    font-weight: unset;
    min-width: max-content;
  }
}

.schema {
  gap: var(--spacing-sm);
}

.monitoring-hint {
  font-size: 0.85rem;
  line-height: 1.4;
  color: var(--eddie-grey-medium);
}

.permission-actions,
.actions-row {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.permission-actions {
  margin-top: auto;
}

.monitor-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-lg);
  border: 1px solid var(--eddie-primary);
  border-radius: 2rem;
  color: var(--eddie-primary);
  font-weight: 600;
  text-decoration: none;
  transition:
    background-color 0.3s ease-in-out,
    color 0.3s ease-in-out;

  &:hover {
    color: var(--light);
    background-color: var(--eddie-primary);
  }
}

.actions-row {
  align-items: center;
  justify-content: space-between;
}

.actions-row--end {
  align-items: flex-end;
  justify-content: flex-end;
}

.actions-row .action-btn {
  width: 100%;
  justify-content: center;
}

.actions-row--end .action-btn {
  width: fit-content;
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.2s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

@media screen and (min-width: 1024px) {
  .permission-details {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-lg);
  }
  .permission-field {
    align-items: center;
    &:not(.schedule) {
      flex-direction: row;
    }
    &.schemas {
      align-items: flex-start;
    }
    dd {
      text-align: right;
    }
  }
  .schedule {
    align-items: flex-start;
  }
  .schema {
    /**field gap + field padding + border */
    gap: calc(var(--spacing-xlg) + var(--spacing-sm) * 2 + 2px);
  }
  .actions-row .action-btn {
    width: 100%;
    justify-content: center;
  }
  .column:first-child {
    margin-bottom: unset;
  }
}

@media screen and (min-width: 1640px) {
  .actions-row {
    flex-direction: row;
    align-items: center;
    .action-btn {
      width: fit-content;
      justify-content: flex-start;
    }
  }
}
</style>
