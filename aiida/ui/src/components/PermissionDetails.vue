<script setup lang="ts">
import type { AiidaPermission, PermissionTypes } from '@/types'
import StatusTag from './StatusTag.vue'
import cronstrue from 'cronstrue/i18n'
import Button from '@/components/Button.vue'
import RevokeIcon from '@/assets/icons/RevokeIcon.svg'
import { usePermissionDialog } from '@/composables/permission-dialog'
import { useConfirmDialog } from '@/composables/confirm-dialog'
import { BASE_URL, revokePermission } from '@/api'
import { fetchPermissions } from '@/stores/permissions'
import { ref, useTemplateRef } from 'vue'
import EyeIcon from '@/assets/icons/EyeIcon.svg'
import CrossedOutEyeIcon from '@/assets/icons/CrossedOutEyeIcon.svg'
import ToolTipIcon from '@/assets/icons/ToolTipIcon.svg'
import { onClickOutside } from '@vueuse/core'
import CopyButton from './CopyButton.vue'
import MessageDownloadButton from '@/components/MessageDownloadButton.vue'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const { confirm } = useConfirmDialog()
const target = useTemplateRef('target')
const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: PermissionTypes
}>()

const { updatePermission } = usePermissionDialog()
const show = ref(false)
const showToolTip = ref(false)

const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  minute: '2-digit',
  hour: '2-digit',
  second: '2-digit',
})

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

const showInboundApiKey = () => {
  show.value = !show.value
}

const generateStringFromLength = (length: number, char: string) => {
  return Array.from({ length }, () => char).join('')
}

onClickOutside(target, () => (showToolTip.value = false))
</script>

<template>
  <dl class="permission-details">
    <div class="column">
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.service') }}</dt>
        <dd>{{ permission.serviceName }}</dd>
      </div>
      <div class="permission-field status">
        <dt>{{ t('permissions.dropdown.status') }}</dt>
        <StatusTag
          :status-type="status !== 'Complete' ? 'healthy' : 'unhealthy'"
          class="status-tag"
          >{{ t(permission.status) }}</StatusTag
        >
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.creationDate') }}</dt>
        <dd>
          {{ dateTimeFormat.format(new Date(permission.startTime)) }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.eddieApplication') }}</dt>
        <dd>
          {{ permission.eddieId }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.start') }}</dt>
        <dd>
          {{ permission.grantTime ? dateTimeFormat.format(new Date(permission.grantTime)) : 'N/A' }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>{{ t('permissions.dropdown.end') }}</dt>
        <dd>{{ dateTimeFormat.format(new Date(permission.expirationTime)) }}</dd>
      </div>
      <div class="permission-field graph" v-if="permission.unimplemented">
        <dt>Data Package Graph</dt>
        <dd class="graph-data">PLACEHOLDER</dd>
      </div>
      <div class="permission-field schedule">
        <dt>{{ t('permissions.dropdown.transmissionSchedule') }}</dt>
        <dd>
          {{
            cronstrue.toString(permission.dataNeed.transmissionSchedule, {
              locale: locale,
            })
          }}
        </dd>
      </div>
    </div>
    <div class="column">
      <template v-if="permission.dataNeed.type !== 'inbound-aiida'">
        <div class="permission-field">
          <dt>{{ t('permissions.dropdown.schemas') }}</dt>
          <div class="column schema">
            <dd v-for="schema in permission.dataNeed.schemas" :key="schema">
              {{ schema }}
            </dd>
          </div>
        </div>
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
        <div class="permission-field" v-if="permission.dataSource">
          <dt>{{ t('permissions.dropdown.dataSource') }}</dt>
          <dd>{{ permission.dataSource?.name ?? 'undefined' }}</dd>
        </div>
        <div
          class="permission-field"
          v-if="permission.mqttStreamingConfig && permission.mqttStreamingConfig.serverUri"
        >
          <dt>Streaming Uri</dt>
          <dd>{{ permission.mqttStreamingConfig.serverUri }}</dd>
        </div>
        <div
          class="permission-field"
          v-if="permission.mqttStreamingConfig && permission.mqttStreamingConfig.dataTopic"
        >
          <dt>Streaming Topic</dt>
          <dd>{{ permission.mqttStreamingConfig.dataTopic }}</dd>
        </div>
      </template>
      <template v-if="permission.dataNeed.type === 'inbound-aiida' && permission.dataSource">
        <div
          class="permission-field access-code-field"
          v-if="permission.dataSource.accessCode"
          ref="target"
        >
          <dt>
            API Key
            <button
              @click="showToolTip = !showToolTip"
              aria-label="Toggle Access Code tooltip"
              class="tool-tip-button"
              :class="{ active: showToolTip }"
            >
              <ToolTipIcon />
            </button>
          </dt>
          <dd>
            {{
              show
                ? permission.dataSource.accessCode
                : generateStringFromLength(permission.dataSource.accessCode.length, '●')
            }}

            <CopyButton :copy-text="permission.dataSource.accessCode" />
            <button
              class="show-button"
              @click="showInboundApiKey"
              :aria-label="show ? 'Hide MQTT password' : 'Show MQTT password'"
            >
              <Transition mode="out-in">
                <component :is="show ? EyeIcon : CrossedOutEyeIcon" />
              </Transition>
            </button>
          </dd>
          <Transition>
            <div v-if="showToolTip" class="tool-tip">
              <p>{{ t('permissions.dropdown.inboundTooltip') }}</p>
              <ul>
                <li class="copy-link">
                  X-API-Key Header
                  <CopyButton
                    :copy-text="`curl ${BASE_URL}/inbound/latest/${permission.permissionId} \\  --header 'X-API-Key: ${permission.dataSource.accessCode}'`"
                  />
                </li>
                <li class="copy-link">
                  apiKey Query Parameter
                  <CopyButton
                    :copy-text="`curl ${BASE_URL}/inbound/latest/${permission.permissionId}?apiKey=${permission.dataSource.accessCode}`"
                  />
                </li>
              </ul>
            </div>
          </Transition>
        </div>
      </template>
      <div class="permission-field" v-if="permission.permissionId">
        <dt>{{ t('permissions.dropdown.permissionID') }}</dt>
        <dd>{{ permission.permissionId }}</dd>
      </div>
      <div class="permission-field" v-if="permission.unimplemented">
        <dt>Last Data Package sent</dt>
        <dd>PLACEHOLDER</dd>
      </div>
      <div v-if="status === 'Active'" class="actions-row">
        <Button button-style="error" class="action-btn" @click="handleRevoke">
          <RevokeIcon /> {{ t('revokeButton') }}
        </Button>
        <MessageDownloadButton :data="permission" class="action-btn">
          <EyeIcon /> {{ t('permissions.dropdown.downloadLatestMessageButton') }}
        </MessageDownloadButton>
      </div>
      <Button
        v-if="status === 'Pending'"
        @click="updatePermission(permission)"
        class="update-button"
      >
        {{ t('continueButton') }}
      </Button>
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
  word-break: break-all;
  gap: 0.25rem;
  &:not(.status) {
    flex-direction: column;
  }
  dd {
    line-height: 1;
    color: var(--eddie-grey-medium);
    font-weight: 600;
  }
}

.status {
  padding: unset;
  border: unset;
  gap: 0.5rem;
  align-items: center;

  dt {
    padding: var(--spacing-sm) var(--spacing-sm);
    border: 1px solid var(--eddie-grey-medium);
    width: 100%;
    border-radius: var(--border-radius);
  }

  .status-tag {
    min-width: fit-content;
  }
}

.schema {
  gap: var(--spacing-sm);
}

.graph {
  flex-direction: column;

  gap: var(--spacing-sm);
}

.update-button {
  width: 100%;
  justify-content: center;
  margin: auto 0 0 auto;
}

.actions-row {
  display: flex;
  flex-direction: column;

  gap: var(--spacing-sm);
  align-items: center;
  margin-top: auto;
  justify-content: space-between;
}
.actions-row .action-btn {
  width: 100%;
  justify-content: center;
}

.access-code-field {
  position: relative;
  word-break: keep-all;
  button {
    padding: unset;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  dd,
  dt {
    display: flex;
    gap: var(--spacing-sm);
    align-items: center;
  }
}

.tool-tip-button {
  transition: color 0.3s ease-in-out;
  &:hover,
  &.active {
    color: var(--eddie-primary);
  }
}

.tool-tip {
  position: absolute;
  box-shadow: 0px 2px 5px 0px #00000040;
  top: 100%;
  left: 0;
  width: 80%;
  padding: var(--spacing-sm);
  background-color: var(--light);
  border: 1px solid var(--eddie-primary);
  border-radius: var(--border-radius);
  color: var(--eddie-grey-medium);

  &::after {
    content: '';
    position: absolute;
    bottom: 100%;
    left: 10%;
    margin-left: -var(--spacing-xs);
    border-width: var(--spacing-xs);
    border-style: solid;
    border-color: var(--eddie-primary) transparent transparent transparent;
    transform: rotate(180deg);
  }
}

.copy-link {
  display: grid;
  grid-template-columns: 50% 50%;
  align-items: center;
  gap: var(--spacing-sm);
  &:first-child {
    margin: var(--spacing-sm) 0;
  }
  button {
    justify-content: flex-start;
  }
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
    &:not(.status, .schedule) {
      flex-direction: row;
    }
    &.schemas {
      align-items: flex-start;
    }
  }
  .schedule {
    align-items: flex-start;
  }
  .schema {
    /**field gap + field padding + border */
    gap: calc(var(--spacing-xlg) + var(--spacing-sm) * 2 + 2px);
  }
  .update-button {
    width: fit-content;
    justify-content: flex-start;
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
