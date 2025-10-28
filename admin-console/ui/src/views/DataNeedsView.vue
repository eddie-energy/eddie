<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { getDataNeeds } from '@/api'
import DataNeedForm from '@/components/DataNeedForm.vue'
import type { AnyDataNeed } from '@/types'
import { CSRF_HEADER, CSRF_TOKEN, DATA_NEEDS_API_URL } from '@/config'

const dataNeeds = ref<AnyDataNeed[]>([])

function refresh() {
  getDataNeeds().then((result) => {
    dataNeeds.value = result
  })
}

function deleteDataNeed(id: string) {
  if (
    confirm(
      'Are you sure you want to delete this data need? Related permissions and permission requests will be removed!'
    )
  ) {
    fetch(`${DATA_NEEDS_API_URL}/${id}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: {
        [CSRF_HEADER]: CSRF_TOKEN
      }
    })
      .catch((error) => {
        alert('Failed to delete data need.' + error.message)
      })
      .then(() => {
        refresh()
      })
  }
}

function toggleEnabled(id: string, current: boolean) {
  fetch(`${DATA_NEEDS_API_URL}/${id}`, {
    method: 'PATCH',
    credentials: 'include',
    headers: {
      [CSRF_HEADER]: CSRF_TOKEN,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ isEnabled: !current })
  })
    .catch((error: Error) => {
      alert('Failed to update data need.' + error.message)
    })
    .then(() => {
      refresh()
    })
}

onMounted(() => refresh())
</script>

<template>
  <h1>Data Needs</h1>

  <article v-for="dn in dataNeeds" :key="dn.id">
    <dl>
      <dt>Type</dt>
      <dd>{{ dn.type }}</dd>
      <dt>Id</dt>
      <dd>{{ dn.id }}</dd>
      <dt>Name</dt>
      <dd>{{ dn.name }}</dd>
      <dt>Description</dt>
      <dd>{{ dn.description }}</dd>
      <dt>Purpose</dt>
      <dd>{{ dn.purpose }}</dd>
      <dt>Policy Link</dt>
      <dd>{{ dn.policyLink }}</dd>
      <dt>Created at</dt>
      <dd>{{ dn.createdAt }}</dd>
      <dt>Enabled</dt>
      <dd>{{ dn.enabled }}</dd>

      <template v-if="dn.regionConnectorFilter">
        <dt v-if="dn.regionConnectorFilter.type === 'blocklist'">Blocked region connectors</dt>
        <dt v-if="dn.regionConnectorFilter.type === 'allowlist'">Allowed region connectors</dt>
        <dd>{{ dn.regionConnectorFilter.regionConnectorIds.join(', ') }}</dd>
      </template>

      <template
        v-if="
          dn.type === 'validated' || dn.type === 'inbound-aiida' || dn.type === 'outbound-aiida'
        "
      >
        <dt>Duration Type</dt>
        <dd>{{ dn.duration.type }}</dd>
        <dt v-if="dn.duration.start">Start</dt>
        <dd v-if="dn.duration.start">{{ dn.duration.start }}</dd>
        <dt v-if="dn.duration.end">End</dt>
        <dd v-if="dn.duration.end">{{ dn.duration.end }}</dd>

        <template v-if="dn.duration.type === 'relativeDuration'">
          <dt>Sticky Start Calendar Unit</dt>
          <dd>
            {{ dn.duration.stickyStartCalendarUnit }}
          </dd>
        </template>
      </template>

      <!-- Validated -->
      <template v-if="dn.type === 'validated'">
        <dt>Energy Type</dt>
        <dd>{{ dn.energyType }}</dd>
        <dt>Min Granularity</dt>
        <dd>{{ dn.minGranularity }}</dd>
        <dt>Max Granularity</dt>
        <dd>{{ dn.maxGranularity }}</dd>
      </template>

      <!-- AIIDA -->
      <template v-if="dn.type === 'inbound-aiida' || dn.type === 'outbound-aiida'">
        <dt>Asset</dt>
        <dd>{{ dn.asset }}</dd>
        <dt>Transmission Schedule</dt>
        <dd>{{ dn.transmissionSchedule }}</dd>
        <dt>Schemas</dt>
        <dd>{{ dn.schemas?.join(', ') }}</dd>
        <dt>Data Tags</dt>
        <dd>{{ dn.dataTags?.join(', ') }}</dd>
      </template>
    </dl>

    <button @click="toggleEnabled(dn.id, dn.enabled)">
      {{ dn.enabled ? 'Disable' : 'Enable' }}
    </button>
    <button @click="deleteDataNeed(dn.id)">Delete</button>
  </article>

  <DataNeedForm @created="refresh" />
</template>

<style scoped>
dl {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.5rem 1rem;
  margin-bottom: 1rem;
}

article {
  border: 1px solid var(--p-surface-700);
  padding: 1rem;
  margin-bottom: 1rem;
}

button + button {
  margin-left: 0.5rem;
}
</style>
