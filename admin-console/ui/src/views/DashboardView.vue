<!--
SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script lang="ts" setup>
import {
  getDataNeeds,
  getPermissions,
  getRegionConnectorHealth,
  getRegionConnectors,
  HealthStatus,
  type RegionConnectorMetadata,
  type StatusMessage
} from '@/api'
import LineChartPermissions from '@/components/LineChartPermissions.vue'
import { computed, onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'
import {
  ACTIVE_PERMISSION_STATES,
  FAILED_PERMISSION_STATES,
  GRANTED_PERMISSION_STATES
} from '@/constants'
import type { AnyDataNeed, TimeFramedDataNeed } from '@/types'
import DashboardCard from '@/components/DashboardCard.vue'
import { countryFlag, formatCountry } from '@/util/countries'
import Chart from 'primevue/chart'

const permissions = ref<StatusMessage[]>([])
const dataNeeds = ref<AnyDataNeed[]>([])
const regionConnectors = ref<RegionConnectorMetadata[]>([])
const permissionCountPerRegionConnector = computed(() => getPermissionCountPerRegionConnector())
const regionConnectorHealth = ref<Map<string, HealthStatus>>(new Map())

const permissionStates = computed(() => countPermissionStates())
const dataNeedStates = computed(() => countDataNeedStates())

function countPermissionStates() {
  const states = { granted: 0, active: 0, failed: 0 }

  for (const permission of permissions.value) {
    if (GRANTED_PERMISSION_STATES.includes(permission.status)) {
      states.granted++
    }

    if (ACTIVE_PERMISSION_STATES.includes(permission.status)) {
      states.active++
    }

    if (FAILED_PERMISSION_STATES.includes(permission.status)) {
      states.failed++
    }
  }

  return states
}

function countDataNeedStates() {
  const states = { disabled: 0, active: 0, expired: 0 }

  for (const dataNeed of dataNeeds.value) {
    if (!dataNeed.enabled) {
      states.disabled++
    }

    if (
      permissions.value.some(
        ({ dataNeedId, status }) =>
          dataNeedId === dataNeed.id && ACTIVE_PERMISSION_STATES.includes(status)
      )
    ) {
      states.active++
    }

    const timeFramed = dataNeed as TimeFramedDataNeed
    if (
      timeFramed.duration && // check if it actually is a timeframed data need
      timeFramed.duration.type === 'absoluteDuration' &&
      new Date(timeFramed.duration.end) < new Date()
    ) {
      states.expired++
    }
  }

  return states
}

onMounted(async () => {
  permissions.value = await getPermissions()
  dataNeeds.value = await getDataNeeds()
  regionConnectors.value = await getRegionConnectors()
  for (const { id } of regionConnectors.value) {
    const health = await getRegionConnectorHealth(id)
    regionConnectorHealth.value.set(id, health?.status || HealthStatus.UNKNOWN)
  }
})

function getPermissionCountPerRegionConnector() {
  const count: Record<string, number> = {}

  for (const { regionConnectorId } of permissions.value) {
    count[regionConnectorId] = (count[regionConnectorId] || 0) + 1
  }

  return count
}
</script>

<template>
  <h1>Dashboard</h1>
  <p>
    The space for creating, enabling, and deleting data needs that define what data can be exchanged
    through the EDDIE Framework.
  </p>

  <div class="layout">
    <section class="panel">
      <header>
        <i class="pi pi-check-circle"></i>
        <h2>Permissions <span>Timeline</span></h2>
        <RouterLink to="/permissions">
          <i class="pi pi-chevron-right"></i>
        </RouterLink>
      </header>

      <!-- TODO: Calculate with SQL queries for performance -->
      <div class="cards">
        <DashboardCard
          :count="permissions.length"
          info="Number of permissions and permission requests in all states."
          text="Total Permissions"
          color="info"
        >
          <i class="pi pi-plus-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="permissionStates.granted"
          info="Number of permissions that have been accepted or fulfilled."
          text="Granted Permissions"
          color="success"
        >
          <i class="pi pi-check-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="permissionStates.active"
          info="Number of permissions that are in the accepted state and should be sending data."
          text="Active Permissions"
          color="help"
        >
          <i class="pi pi-play-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="permissionStates.failed"
          info="Number of permissions that are in a failure state."
          text="Failed Permissions"
          color="danger"
        >
          <i class="pi pi-times-circle"></i>
        </DashboardCard>
      </div>

      <LineChartPermissions :permissions></LineChartPermissions>
    </section>

    <section class="panel">
      <header>
        <i class="pi pi-briefcase"></i>
        <h2>Data Needs <span>Most Popular</span></h2>
        <RouterLink to="/data-needs">
          <i class="pi pi-chevron-right"></i>
        </RouterLink>
      </header>

      <div class="cards">
        <DashboardCard
          :count="dataNeeds.length"
          info="Total number of data needs."
          text="Total Data Needs"
          color="info"
        >
          <i class="pi pi-plus-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="dataNeedStates.disabled"
          info="Data needs that have been disabled."
          text="Disabled Data Needs"
          color="warn"
        >
          <i class="pi pi-pause-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="dataNeedStates.active"
          info="Data needs having permissions that are in the accepted state and should be sending data."
          text="Active Data Needs"
          color="success"
        >
          <i class="pi pi-play-circle"></i>
        </DashboardCard>
        <DashboardCard
          :count="dataNeedStates.expired"
          info="Data needs with absolute durations with their end date in the past."
          text="Expired Data Needs"
          color="danger"
        >
          <i class="pi pi-times-circle"></i>
        </DashboardCard>
      </div>

      <div class="data-needs">
        <div v-for="dataNeed in dataNeeds" :key="dataNeed.id" class="data-need">
          <div class="data-need-headline">
            <h3>
              {{ dataNeed.name }}
            </h3>
            <svg width="6" height="6" fill="currentColor">
              <circle r="3" cx="3" cy="3"></circle>
            </svg>
            <span>
              {{ dataNeed.type }}
            </span>
          </div>
          <span>{{ dataNeed.description }}</span>
          <div class="data-need-stats">
            <div class="data-need-stat">
              <i class="pi pi-check-circle" style="background: var(--success)"></i>
              <span>Granted Permissions</span>
              <b>{{
                permissions.filter(
                  (permission) =>
                    permission.dataNeedId === dataNeed.id &&
                    GRANTED_PERMISSION_STATES.includes(permission.status)
                ).length
              }}</b>
            </div>
            <div class="data-need-stat">
              <i class="pi pi-play-circle" style="background: var(--help)"></i>
              <span>Active Permissions</span>
              <b>{{
                permissions.filter(
                  (permission) =>
                    permission.dataNeedId === dataNeed.id &&
                    ACTIVE_PERMISSION_STATES.includes(permission.status)
                ).length
              }}</b>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel bottom">
      <div>
        <header>
          <i class="pi pi-globe"></i>
          <h2>Region Connectors</h2>
          <RouterLink to="/region-connectors">
            <i class="pi pi-chevron-right"></i>
          </RouterLink>
        </header>

        <div class="table-scroll-wrapper">
          <table>
            <thead>
              <tr>
                <th scope="col">Region Connector</th>
                <th scope="col">Total Permissions</th>
                <th scope="col">Permissions Granted</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="{ id, countryCodes } in regionConnectors" :key="id">
                <th scope="row">
                  <span>
                    {{ countryFlag(countryCodes[0]) }}
                  </span>
                  <span>
                    <b>{{ id }}</b>
                    <br />
                    <small>{{ formatCountry(countryCodes[0]) }}</small>
                  </span>
                </th>
                <td>
                  {{ permissionCountPerRegionConnector[id] || 0 }}
                </td>
                <td>
                  {{ permissionCountPerRegionConnector[id] || 0 }}
                </td>
                <td>
                  <HealthIcon :health="regionConnectorHealth.get(id) || HealthStatus.UNKNOWN" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <h3>Permission Distribution</h3>

        <Chart
          style="height: 28rem"
          type="doughnut"
          :data="{
            labels: Object.keys(permissionCountPerRegionConnector),
            datasets: [{ data: Object.values(permissionCountPerRegionConnector) }]
          }"
          :canvasProps="{
            role: 'img',
            'aria-label': 'Pie chart describing the number of permissions per region connector.'
          }"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.layout {
  margin-top: 1.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 2.5rem 1.25rem;

  > * {
    flex: 1;
  }
}

.bottom {
  display: flex;
  flex-wrap: wrap;
  flex-basis: 100%;
  gap: 2.5rem;

  h3 {
    font-size: 1.5rem;
    font-weight: 300;
    margin-bottom: 1.5rem;
  }
}

.panel {
  padding: 1.25rem;
  border-radius: var(--panel-radius);
  border: var(--panel-border);
  background: var(--panel-background);
  box-shadow: var(--panel-shadow);

  header {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    margin-bottom: 1.5rem;

    > i {
      font-size: 1.5em;
    }

    span {
      font-weight: 300;
    }

    h2 {
      font-size: 1.5rem;
      font-weight: 500;
    }

    a {
      margin-left: 2.25rem;
      font-size: 0.875rem;
    }
  }
}

.cards {
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

.data-needs {
  max-height: 24rem;
  overflow: auto;
  scrollbar-gutter: stable;
  display: grid;
  gap: 1rem;
}

.data-need {
  background: var(--card-background);
  border: var(--card-border);
  border-radius: var(--card-radius);
  padding: 1rem;
}

.data-need-headline {
  display: grid;
  grid-template-columns: auto max-content minmax(max-content, 1fr);
  gap: 0.75rem;
  align-items: center;

  h3 {
    font-weight: 300;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.data-need-stats {
  margin-top: 0.625rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.data-need-stat {
  display: grid;
  grid-template-columns: max-content 1fr auto;
  align-items: center;
  background: var(--tile-background);
  border-radius: var(--tile-radius);
  padding: 0.5rem;
  min-width: 14rem;
  gap: 0.5rem;

  i {
    padding: 0.25rem;
    border-radius: 50%;
    color: white;
  }

  b {
    font-weight: 500;
  }
}

/* table's display value does not support scrolling */
.table-scroll-wrapper {
  position: relative;
  height: 100%;
  max-height: 28rem;
  overflow: auto;
  scrollbar-gutter: stable;
  display: grid;
}

table {
  border-collapse: separate;
  border-spacing: 0 0.75rem;
  padding: 0 1px; /* account for row outline */

  th {
    text-align: left;
    vertical-align: bottom;

    & + th {
      padding-left: 1rem;
    }
  }

  tbody {
    tr {
      background: var(--card-background);
      border-radius: var(--card-radius);
      outline: var(--card-border);
    }

    th,
    td {
      padding: 1rem;
    }

    th {
      display: flex;
      gap: 0.75rem;
      align-items: center;

      b {
        font-weight: 600;
        line-height: 1;
      }

      small {
        font-size: 0.875rem;
        line-height: 0.75rem;
      }
    }
  }
}

@media (width < 80rem) {
  .layout {
    gap: 1.25rem;
  }
}
</style>
