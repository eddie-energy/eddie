<script setup>
import cronstrue from 'cronstrue'

import STATUS from '@/constants/permission-status.js'
import { revokePermission } from '@/api.js'

/** @type {{ permission: AiidaPermission }} */
const { permission } = defineProps(['permission'])

const {
  dataNeed: {
    asset,
    dataNeedId,
    dataTags,
    name,
    policyLink,
    purpose,
    schemas,
    transmissionSchedule,
  },
  eddieId,
  expirationTime,
  grantTime,
  permissionId,
  serviceName,
  startTime,
  status,
  userId,
} = permission

function handleRevoke() {
  confirm(
    'This action will revoke the given permission. The eligible party will no longer be able to receive data for this entry.',
  ) && revokePermission(permissionId)
}
</script>

<template>
  <sl-details>
    <span slot="summary">
      <strong>{{ serviceName }}</strong>
      <br />
      <small>{{ permissionId }}</small>
    </span>

    <dl class="details-list">
      <dt>Service</dt>
      <dd>{{ serviceName }}</dd>
      <dt>Status</dt>
      <dd>
        <sl-tooltip :content="STATUS[status].description">
          <sl-badge :variant="STATUS[status].isActive ? 'success' : 'danger'">
            {{ STATUS[status].title }}
          </sl-badge>
        </sl-tooltip>
      </dd>

      <dt>EDDIE Application</dt>
      <dd>{{ eddieId }}</dd>

      <dt>Permission ID</dt>
      <dd>{{ permissionId }}</dd>

      <dt>Start</dt>
      <dd>{{ new Date(startTime).toLocaleString() }}</dd>

      <dt>End</dt>
      <dd>{{ new Date(expirationTime).toLocaleString() }}</dd>

      <dt>Transmission Schedule</dt>
      <dd>{{ cronstrue.toString(transmissionSchedule) }}</dd>

      <dt>Schemas</dt>
      <dd>
        <template v-for="schema in schemas">
          <span>{{ schema }}</span>
          <br />
        </template>
      </dd>

      <dt>Asset</dt>
      <dd>{{ asset }}</dd>

      <template v-if="dataTags && dataTags.length">
        <dt>OBIS-Codes</dt>
        <dd>
          <template v-for="code in dataTags">
            <span>{{ code }}</span>
            <br />
          </template>
        </dd>
      </template>
    </dl>

    <br />
    <sl-button v-if="STATUS[status].isRevocable" @click="handleRevoke">Revoke</sl-button>
  </sl-details>
</template>

<style scoped></style>
