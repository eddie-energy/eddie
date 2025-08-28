<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import type { AiidaPermission } from '@/types'
import StatusTag from './StatusTag.vue'
import cronstrue from 'cronstrue'
import Button from '@/components/Button.vue'

const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: 'healthy' | 'unhealthy'
}>()

const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  minute: '2-digit',
  hour: '2-digit',
  second: '2-digit',
})
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
        <StatusTag :status-type="status" class="status-tag">{{
          STATUS[permission.status].title
        }}</StatusTag>
      </div>
      <div class="permission-field">
        <dt>Creation Date</dt>
        <dd>
          {{ dateTimeFormat.format(new Date(permission.startTime)) }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>EDDIE Appliaction</dt>
        <dd>
          {{ permission.eddieId }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>Start</dt>
        <dd v-if="permission.grantTime">
          {{ dateTimeFormat.format(new Date(permission.grantTime)) }}
        </dd>
      </div>
      <div class="permission-field">
        <dt>End</dt>
        <dd>{{ dateTimeFormat.format(new Date(permission.expirationTime)) }}</dd>
      </div>
      <div class="permission-field graph">
        <dt>Data Package Graph</dt>
        <img class="graph-data" src="/DummyGraph.png" />
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
      <div class="permission-field">
        <dt>Data Source</dt>
        <dd>{{ permission.dataSource?.name ?? 'undefined' }}</dd>
      </div>
      <div class="permission-field">
        <dt>Target IP Adress, Port</dt>
        <dd>PLACEHOLDER</dd>
      </div>
      <div class="permission-field">
        <dt>Last Data Package sent</dt>
        <dd>PLACEHOLDER</dd>
      </div>
      <Button class="update-button">Update</Button>
    </div>
  </dl>
</template>

<style scoped>
.permission-details {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-lg);
}

.column {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.permission-field {
  display: flex;
  justify-content: space-between;
  border: 1px solid var(--eddie-grey-light);
  color: var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-sm);
  border-radius: 0.5rem;
  font-size: 0.625rem;
  line-height: 1rem;

  @media screen and (min-width: 1024px) {
    font-size: 0.875rem;
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
  /**field gap + field padding + border */
  gap: calc(var(--spacing-sm) + var(--spacing-sm) * 2 + 2px);
}

.graph {
  flex-direction: column;
  gap: var(--spacing-sm);
}

.update-button {
  width: fit-content;
  margin: auto 0 0 auto;
}
</style>
