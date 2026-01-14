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
import DoughnutChartRegions from '@/components/DoughnutChartRegions.vue'
import { computed, onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'
import { REGION_CONNECTORS } from '@/constants'
import type { AnyDataNeed } from '@/types'
import DashboardCard from '@/components/DashboardCard.vue'
import { countryFlag, formatCountry } from '@/util/countries'

const permissions = ref<StatusMessage[]>([])
const dataNeeds = ref<AnyDataNeed[]>([])
const regionConnectors = ref<RegionConnectorMetadata[]>([])
const permissionCountPerRegionConnector = computed(() => getPermissionCountPerRegionConnector())
const regionConnectorHealth = ref<Map<string, HealthStatus>>(new Map())

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
  const permissionsPerRegionConnector: { [key: string]: number } = {}

  for (const id of REGION_CONNECTORS) {
    permissionsPerRegionConnector[id] = 0
  }

  for (const { regionConnectorId } of permissions.value) {
    permissionsPerRegionConnector[regionConnectorId]++
  }

  return permissionsPerRegionConnector
}
</script>

<template>
  <h1>Dashboard</h1>
  <p>
    The space for creating, enabling, and deleting data needs that define what data can be exchanged
    through the EDDIE Framework.
  </p>

  <div class="top">
    <section>
      <header>
        <i class="pi pi-check-circle"></i>
        <h2>Permissions <span>Timeline</span></h2>
      </header>

      <!-- TODO: Calculate with SQL queries for performance -->
      <div class="cards">
        <DashboardCard :count="permissions.length" info="TODO" text="Total Permissions" />
        <DashboardCard count="TODO" info="TODO" text="Granted Permissions" />
        <DashboardCard count="TODO" info="TODO" text="Active Permissions" />
        <DashboardCard count="TODO" info="TODO" text="Failed Permissions" />
      </div>

      <div>
        <LineChartPermissions :permissions></LineChartPermissions>
      </div>
    </section>

    <section>
      <header>
        <i class="pi pi-briefcase"></i>
        <h2>Data Needs <span>Most Popular</span></h2>
      </header>

      <div class="cards">
        <DashboardCard :count="dataNeeds.length" info="TODO" text="Total Data Needs" />
        <DashboardCard count="TODO" info="TODO" text="Disabled Data Needs" />
        <DashboardCard count="TODO" info="TODO" text="Active Data Needs" />
        <DashboardCard count="TODO" info="TODO" text="Expired Data Needs" />
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
              <i class="pi pi-play-circle"></i>
              &nbsp;
              <span>Active Permissions</span>
              &nbsp;
              <b>TODO</b>
            </div>
            <div class="data-need-stat">
              <i class="pi pi-check-circle"></i>
              &nbsp;
              <span>Granted Permissions</span>
              &nbsp;
              <b>TODO</b>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <section class="bottom">
    <div>
      <header>
        <i class="pi pi-globe"></i>
        <h2>Region Connectors</h2>
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
                {{ permissionCountPerRegionConnector[id] }}
              </td>
              <td>
                {{ permissionCountPerRegionConnector[id] }}
              </td>
              <td>
                {{ regionConnectorHealth.get(id) }}&nbsp;
                <HealthIcon :health="regionConnectorHealth.get(id) || HealthStatus.UNKNOWN" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div>
      <h3>Permission Distribution</h3>

      <DoughnutChartRegions
        :permission-count-per-region-connector="permissionCountPerRegionConnector"
      ></DoughnutChartRegions>
    </div>
  </section>
</template>

<style scoped>
section {
  display: grid;
  flex: 1;
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

    i {
      font-size: 1.5em;
    }

    span {
      font-weight: 300;
    }
  }
}

.cards {
  display: flex;
  gap: 1.25rem;
  margin-bottom: 1.75rem;
}

.top {
  max-height: 35rem;
  display: flex;
  gap: 1.25rem;
  margin-top: 1.5rem;
  margin-bottom: 2.5rem;
}

.bottom {
  display: flex;
  gap: 2.5rem;

  > * {
    display: grid;
    max-height: 32.5rem;
    flex-grow: 1;
  }
}

.data-needs {
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
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.data-need-stats {
  display: flex;
  gap: 1rem;
}

.data-need-stat {
  display: flex;
  background: var(--tile-background);
  border-radius: var(--tile-radius);
  padding: 0.5rem;
}

/* table's display value does not support scrolling */
.table-scroll-wrapper {
  position: relative;
  display: flex;
  height: 100%;
  overflow: auto;
  scrollbar-gutter: stable;

  table {
    width: 100%;
  }
}

table {
  border-collapse: separate;
  border-spacing: 0 0.75rem;
  padding: 0 1px; /* account for row outline */

  th {
    font-size: 0.75rem;
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
        font-size: 0.625rem;
        line-height: 1.2;
      }
    }
  }
}

@media (width < 80rem) {
  .top {
    max-height: unset;
    flex-direction: column;
    margin-bottom: 1.25rem;
  }

  .bottom {
    max-height: unset;
    flex-direction: column;
  }
}
</style>
