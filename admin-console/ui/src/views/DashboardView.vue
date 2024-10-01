<script>
import { getDataNeeds, getPermissions, getRegionConnectors } from '@/api'

export default {
  data() {
    return {
      permissions: [],
      dataNeeds: [],
      regionConnectors: []
    }
  },
  async mounted() {
    this.permissions = await getPermissions()
    this.dataNeeds = await getDataNeeds()
    this.regionConnectors = await getRegionConnectors()
  }
}
</script>

<template>
  <main>
    <div class="top">
      <div class="card">
        <header>
          <span>{{ permissions.length }}</span>
          <h2>Permissions <span>active</span></h2>
        </header>
      </div>

      <div class="card">
        <header>
          <span>1470</span>
          <h2>Data Packages <span>per minute</span></h2>
        </header>
      </div>

      <div class="card">
        <header>
          <span>{{ dataNeeds.length }}</span>
          <h2>Data Needs <span>enabled</span></h2>
        </header>

        <div class="items">
          <div v-for="dataNeed in dataNeeds" :key="dataNeed.id" class="item">
            <h3>{{ dataNeed.name }}</h3>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <header>
        <span>{{ regionConnectors.length }}</span>
        <h2>Region Connectors <span>enabled</span></h2>
      </header>

      <div class="items">
        <div v-for="regionConnector in regionConnectors" :key="regionConnector.id" class="item">
          <h3>{{ regionConnector.id }}</h3>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.top {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.top > * {
  flex-grow: 0;
  flex-shrink: 1;
  width: calc(100% / 3);
}

.card {
  border: 1px solid var(--color-border);
  border-radius: 0.25rem;
  padding: 1rem;
}

.items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.item {
  border: 1px solid var(--color-border);
  border-radius: 0.25rem;
  padding: 0.5rem;
  max-width: 100%;
}

.item h3 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

header > span {
  font-size: 2.125rem;
  color: var(--color-accent);
  font-weight: bold;
}

h2 span {
  display: block;
  font-size: 1rem;
  line-height: 1;
}
</style>
