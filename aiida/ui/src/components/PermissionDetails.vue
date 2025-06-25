<script setup>
import cronstrue from 'cronstrue'
import STATUS from '@/constants/permission-status.js'

/** @type {{ permission: AiidaPermission }} */
const props = defineProps(['permission'])
const { permission } = props

const {
  dataNeed: { asset, dataTags, schemas, transmissionSchedule },
  dataSource,
  eddieId,
  expirationTime,
  permissionId,
  serviceName,
  startTime,
  status,
} = permission
</script>

<template>
  <dl class="details-list">
    <dt>Service</dt>
    <dd>{{ serviceName }}</dd>

    <dt>Status</dt>
    <dd>
      <sl-tooltip :content="STATUS[status].description">
        <sl-badge
          :variant="
            STATUS[status].isActive
              ? 'success'
              : STATUS[status].isOpen
                ? 'primary'
                : STATUS[status].isError
                  ? 'danger'
                  : 'neutral'
          "
        >
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

    <template v-if="dataSource">
      <dt>Data Source</dt>
      <dd>{{ dataSource.name }} ({{ dataSource.id }})</dd>
    </template>
  </dl>
</template>

<style scoped></style>
