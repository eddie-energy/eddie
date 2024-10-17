<script setup>
import { getDataNeeds, getPermissions, getRegionConnectors } from '@/api'
import LineChartPermissions from '@/components/LineChartPermissions.vue'
import DoughnutChartRegions from '@/components/DoughnutChartRegions.vue'
import LineChartPackages from '@/components/LineChartPackages.vue'
import { onMounted, ref } from 'vue'

const permissions = ref([])
const dataNeeds = ref([])
const regionConnectors = ref([])
const regionConnectorsFromPermissions = ref([])

onMounted(async () => {
  permissions.value = await getPermissions()
  regionConnectorsFromPermissions.value = await getRegionConnectorsFromPermissions()
  dataNeeds.value = await getDataNeeds()
  regionConnectors.value = await getRegionConnectors()
})

async function getRegionConnectorsFromPermissions() {
  const permissions = await getPermissions()

  const regionConnectors = permissions.map((x) => x.regionConnectorId)

  const count = regionConnectors.reduce((acc, value) => {
    acc[value] = (acc[value] || 0) + 1
    return acc
  }, {})
  return Object.entries(count).map(([name, count]) => ({ name, count }))
}

getRegionConnector()

async function getRegionConnector() {
  await getRegionConnectorsFromPermissions()
}
</script>

<template>
  <main>
    <div class="row row--top">
      <div class="card card--top">
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
          <LineChartPermissions></LineChartPermissions>
        </div>
      </div>

      <div class="card card--top">
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
      </div>

      <div class="card card--top">
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
      </div>
    </div>

    <div class="row row--bottom">
      <div class="card card--bottom">
        <header class="card__item card__item--header">
          <span class="card__item-highlighted">{{ regionConnectorsFromPermissions.length }}</span>
          <h2>Region Connectors <span>enabled</span></h2>
        </header>
        <div class="card__item card__item--addition">
          <span><b>2</b> disabled</span>
          <span><b>1</b> failed to start</span>
        </div>
        <div class="card__item card__item--list">
          <div
            v-for="regionConnector in regionConnectorsFromPermissions"
            :key="regionConnector.name"
            class="item"
          >
            <h3>
              <b>{{ regionConnector.name }}</b>
            </h3>
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
        <DoughnutChartRegions></DoughnutChartRegions>
      </div>
    </div>
  </main>
</template>

<style scoped>
.main {
  height: 100%;
}

.row {
  display: flex;
  gap: 1rem;
}

.row > * {
  width: calc(100% / 3);
  overflow: auto;
}

.row--top {
  margin-bottom: 1rem;
  max-height: calc(100vh / 2);
}

.row--bottom {
  height: 44vh;
  max-height: 42vh;
  justify-content: space-between;
  border: 1px solid var(--color-border);
  border-radius: 0.25rem;
}

.card--top {
  border: 1px solid var(--color-border);
  border-radius: 0.25rem;
  padding: 1rem;
  overflow: auto;
}

.card--bottom {
  padding: 1rem;
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
  color: var(--color-accent);
  font-weight: bold;
}

.card__item--list {
  gap: 0.5rem;
  margin-top: 1rem;
}

.item {
  border: 2px dashed var(--color-border);
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
</style>
