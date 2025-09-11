<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import type { AiidaPermission, PermissionTypes } from '@/types'
import StatusTag from './StatusTag.vue'
import cronstrue from 'cronstrue'
import Button from '@/components/Button.vue'
import RevokeIcon from '@/assets/icons/RevokeIcon.svg'
import { usePermissionDialog } from '@/composables/permission-dialog'
import { useConfirmDialog } from '@/composables/confirm-dialog'
import { revokePermission } from '@/api'
import { fetchPermissions } from '@/stores/permissions'
const { confirm } = useConfirmDialog()

const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: PermissionTypes
}>()

const { updatePermission } = usePermissionDialog()

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
      'Revoke Permission',
      'Are you sure you want to revoke this permission? This action cannot be undone.',
      'Revoke',
    )
  ) {
    const promise = revokePermission(permission.permissionId)
    promise.then(() => fetchPermissions())
  }
}
</script>

<template>
  <dl class="permission-details">
    <div class="column">
      <div class="permission-field">
        <dt>Service</dt>
        <dd>{{ permission.serviceName }}</dd>
      </div>
      <div class="permission-field status">
        <dt>Status</dt>
        <StatusTag
          :status-type="status !== 'Complete' ? 'healthy' : 'unhealthy'"
          class="status-tag"
          >{{ STATUS[permission.status].title }}</StatusTag
        >
      </div>
      <div class="permission-field">
        <dt>Creation Date</dt>
        <dd>
          {{ dateTimeFormat.format(new Date(permission.startTime)) }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>EDDIE Application</dt>
        <dd>
          {{ permission.eddieId }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>Start</dt>
        <dd>
          {{ permission.grantTime ? dateTimeFormat.format(new Date(permission.grantTime)) : 'N/A' }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>End</dt>
        <dd>{{ dateTimeFormat.format(new Date(permission.expirationTime)) }}</dd>
      </div>
      <div class="permission-field graph" v-if="permission.unimplemented">
        <dt>Data Package Graph</dt>
        <dd class="graph-data">PLACEHOLDER</dd>
      </div>
    </div>
    <div class="column">
      <div class="permission-field">
        <dt>Transmission Schedule</dt>
        <dd>{{ cronstrue.toString(permission.dataNeed.transmissionSchedule) }}</dd>
      </div>
      <div class="permission-field">
        <dt>Schemas</dt>
        <div class="column schema">
          <dd v-for="schema in permission.dataNeed.schemas" :key="schema">
            {{ schema }}
          </dd>
        </div>
      </div>
      <div class="permission-field">
        <dt>Asset</dt>
        <dd>
          {{ permission.dataNeed.asset }}
        </dd>
      </div>
      <div class="permission-field schemas">
        <dt>OBIS-Codes</dt>
        <div class="column schema">
          <dd v-for="tag in permission.dataNeed.dataTags" :key="tag">
            {{ tag }}
          </dd>
        </div>
      </div>
      <div class="permission-field" v-if="permission.dataSource">
        <dt>Data Source</dt>
        <dd>{{ permission.dataSource?.name ?? 'undefined' }}</dd>
      </div>
      <div class="permission-field" v-if="permission.unimplemented">
        <dt>Target IP Adress, Port</dt>
        <dd>PLACEHOLDER</dd>
      </div>
      <div class="permission-field" v-if="permission.unimplemented">
        <dt>Last Data Package sent</dt>
        <dd>PLACEHOLDER</dd>
      </div>
      <Button
        button-style="error"
        class="update-button"
        v-if="status === 'Active'"
        @click="handleRevoke"
      >
        <RevokeIcon /> Revoke
      </Button>
      <Button
        v-if="status === 'Pending'"
        @click="updatePermission(permission)"
        class="update-button"
      >
        Accept
      </Button>
    </div>
  </dl>
</template>

<style scoped>
.column {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);

  &:first-child {
    margin-bottom: var(--spacing-sm);
  }
}

.permission-field {
  display: flex;

  justify-content: space-between;
  border: 1px solid var(--eddie-grey-light);
  color: var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-sm);
  border-radius: 0.5rem;
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

  dt {
    padding: var(--spacing-sm) var(--spacing-sm);
    border: 1px solid var(--eddie-grey-light);
    width: 100%;
    border-radius: 0.5rem;
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

@media screen and (min-width: 1024px) {
  .permission-details {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-lg);
  }
  .permission-field {
    align-items: center;
    &:not(.status) {
      flex-direction: row;
    }
  }
  .schema {
    /**field gap + field padding + border */
    gap: calc(var(--spacing-xlg) + var(--spacing-sm) * 2 + 2px);
  }
  .update-button {
    width: fit-content;
    justify-content: flex-start;
  }
}
</style>
