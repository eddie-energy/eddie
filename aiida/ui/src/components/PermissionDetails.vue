<script setup lang="ts">
import cronstrue from 'cronstrue'
import PermissionStatusBadge from './PermissionStatusBadge.vue'
import type { AiidaPermission } from '@/types'

const props = defineProps<{
  permission: AiidaPermission
}>()

const { permission } = props

const {
  dataNeed: { asset, dataTags, schemas, transmissionSchedule, type },
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

    <dt>Type</dt>
    <dd>{{ type }}</dd>

    <dt>Status</dt>
    <dd>
      <PermissionStatusBadge :status />
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
      <template v-for="schema in schemas" :key="schema">
        <span>{{ schema }}</span>
        <br />
      </template>
    </dd>

    <dt>Asset</dt>
    <dd>{{ asset }}</dd>

    <template v-if="dataTags && dataTags.length">
      <dt>OBIS-Codes</dt>
      <dd>
        <template v-for="code in dataTags" :key="code">
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
