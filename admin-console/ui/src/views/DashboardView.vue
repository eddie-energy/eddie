<script lang="ts" setup>
import {
  type DataNeed,
  getDataNeeds,
  getPermissions,
  getRegionConnectorHealth,
  getRegionConnectors,
  type RegionConnectorMetadata,
  type StatusMessage
} from '@/api'
import LineChartPermissions from '@/components/LineChartPermissions.vue'
import DoughnutChartRegions from '@/components/DoughnutChartRegions.vue'
import LineChartPackages from '@/components/LineChartPackages.vue'
import { onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'

type PermissionsPerRegionConnector = { id: string; count: number }

const permissions = ref<StatusMessage[]>([])
const dataNeeds = ref<DataNeed[]>([])
const regionConnectors = ref<RegionConnectorMetadata[]>([])
const permissionCountPerRegionConnector = ref<PermissionsPerRegionConnector[]>([])
const regionConnectorHealth = ref<Map<string, string>>(new Map())

onMounted(async () => {
  permissions.value = await getPermissions()
  permissionCountPerRegionConnector.value = await getPermissionCountPerRegionConnector()
  dataNeeds.value = await getDataNeeds()
  regionConnectors.value = await getRegionConnectors()
  for (const { id } of regionConnectors.value) {
    regionConnectorHealth.value.set(id, (await getRegionConnectorHealth(id))?.status || 'DISABLED')
  }
})

async function getPermissionCountPerRegionConnector() {
  const permissions = await getPermissions()

  let permissionsPerRegionConnector: { [key: string]: number } = {}
  for (const { regionConnectorId } of permissions) {
    permissionsPerRegionConnector[regionConnectorId] =
      (permissionsPerRegionConnector[regionConnectorId] || 0) + 1
  }

  return Object.entries(permissionsPerRegionConnector).map(([id, count]) => ({ id, count }))
}
</script>

<template>
  <main class="outer">
    <div class="row--top">
      <section class="card card--top">
        <header class="card__item card__item--header">
          <span class="card__item-highlighted">{{ permissions.length }}</span>
          <h2>Permissions <span>active</span></h2>
        </header>
        <div class="card__item card__item--addition">
          <span>
            <b>{{ permissions.length }}</b> total
          </span>
          <span>
            <b>{{ permissions.length }}</b> permissions granted
          </span>
        </div>
        <div class="card__item card__item--addition">
          <span><b>0</b> completed</span>
          <span><b>0</b> failed recently</span>
        </div>
        <div>
          <LineChartPermissions :permissions="permissions"></LineChartPermissions>
        </div>
      </section>

      <section class="card card--top">
        <header class="card__item card__item--header">
          <span class="card__item-highlighted">1470</span>
          <h2>Data Packages <span>per minute</span></h2>
        </header>
        <div class="card__item card__item--addition">
          <span><b>22,003</b> total data packages</span>
        </div>
        <div>
          <LineChartPackages></LineChartPackages>
        </div>
      </section>

      <section class="card card--top">
        <header class="card__item card__item--header">
          <span class="card__item-highlighted">{{ dataNeeds.length }}</span>
          <h2>Data Needs <span>enabled</span></h2>
        </header>
        <div class="card__item card__item--addition">
          <span>
            <b>{{ dataNeeds.length }}</b> total
          </span>
          <span><b>2</b> disabled</span>
          <span><b>1</b> expired</span>
        </div>

        <div class="card__item card__item--list">
          <div v-for="dataNeed in dataNeeds" :key="dataNeed.id" class="item">
            <h3>
              <b>{{ dataNeed.name }}</b>
            </h3>
            <div class="card__item card__item--addition">
              <span><b>16</b> active permissions</span>
              <span><b>18</b> granted permissions</span>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="bottom">
      <div class="card card--bottom">
        <header class="card__item card__item--header">
          <span class="card__item-highlighted">{{ permissionCountPerRegionConnector.length }}</span>
          <h2>Region Connectors <span>enabled</span></h2>
        </header>
        <div class="card__item card__item--addition">
          <span><b>2</b> disabled</span>
          <span><b>1</b> failed to start</span>
        </div>
        <div class="card__item card__item--list">
          <div
            v-for="regionConnector in permissionCountPerRegionConnector"
            :key="regionConnector.id"
            class="item"
          >
            <div class="card__item card__item--addition">
              <h3>
                <b>{{ regionConnector.id }}</b>
              </h3>

              <span>
                {{ regionConnectorHealth.get(regionConnector.id) }}&nbsp;
                <HealthIcon :health="regionConnectorHealth.get(regionConnector.id)" />
              </span>
            </div>
            <div class="card__item card__item--addition">
              <span>
                <b>{{ regionConnector.count }}</b> total permissions
              </span>
              <span>
                <b>{{ regionConnector.count }}</b> granted permissions
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="card card--bottom">
        <img
          title="Blank SVG Europe Map"
          alt="SVG Europe Map Using Robinson Projection"
          src="/src/assets/europe.svg"
          class="card__map"
        />
      </div>
      <div class="card card--bottom">
        <DoughnutChartRegions
          :permission-count-per-region-connector="permissionCountPerRegionConnector"
        ></DoughnutChartRegions>
      </div>
    </section>
  </main>
</template>

<style scoped>
.outer {
  display: grid;
  height: 100%;
  gap: 1rem;
}

.row--top {
  display: grid;
  gap: 1rem;
}

.bottom {
  display: grid;
  border: 1px solid var(--p-surface-700);
  border-radius: 0.25rem;
  gap: 1rem;
}

.card--top {
  border: 1px solid var(--p-surface-700);
  border-radius: 0.25rem;
  padding: 1rem;
  overflow: auto;
}

.card--bottom {
  padding: 1rem;
  overflow: auto;
}

.card__item--header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.card__item--addition {
  display: flex;
  justify-content: space-between;
}

.card__item-highlighted {
  font-size: 2.125rem;
  color: var(--p-primary-color);
  font-weight: bold;
}

.card__item--list {
  gap: 0.5rem;
  margin-top: 1rem;
}

.item {
  border: 2px dashed var(--p-surface-700);
  border-radius: 0.4rem;
  padding: 0.5rem;
  max-width: 100%;
  margin-bottom: 0.5rem;
}

.item h3 {
  word-break: break-word;
}

h2 span {
  display: block;
  font-size: 1rem;
  line-height: 1;
}

.card__map {
  display: block;
  max-width: 100%;
  height: auto;
}

@media only screen and (min-width: 1280px) {
  .outer {
    grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  }

  .row--top {
    grid-template-columns: 1fr 1fr 1fr;
  }

  .bottom {
    grid-template-columns: 1fr 1fr 1fr;
  }
}
</style>
