<!-- SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script lang="ts" setup>
import {
  getRegionConnectorHealth,
  getRegionConnectors,
  getRegionConnectorsSupportedDataNeeds,
  getRegionConnectorsSupportedFeatures,
  HealthStatus,
  type RegionConnectorFeature,
  type RegionConnectorMetadata
} from '@/api'
import { countryFlag, formatCountry } from '@/util/countries'

import { Button, Panel } from 'primevue'
import { computed, onMounted, ref } from 'vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatDuration } from '@/util/duration'
import {
  DATA_NEEDS,
  DATA_NEEDS_DEFAULT_LINK,
  FEATURES,
  FEATURES_DEFAULT_LINK,
  REGION_CONNECTORS
} from '@/constants'

const regionConnectors = ref<RegionConnectorMetadata[]>([])
const regionConnectorHealth = ref<Record<string, HealthStatus>>({})
const supportedFeatures = ref<Record<string, string[]>>({})
const unsupportedFeatures = ref<Record<string, string[]>>({})
const supportedDataNeeds = ref<Record<string, string[]>>({})

const openPanels = ref(new Set<string>())

const disabledRegionConnectors = computed(() =>
  REGION_CONNECTORS.filter((id) => !regionConnectors.value.some((rc) => rc.id === id))
)

onMounted(async () => {
  regionConnectors.value = await getRegionConnectors()

  for (const { regionConnectorId, ...features } of await getRegionConnectorsSupportedFeatures()) {
    const supported: string[] = []
    const unsupported: string[] = []

    for (const [key, value] of Object.entries(features)) {
      if (value) {
        supported.push(key)
      } else {
        unsupported.push(key)
      }
    }

    supportedFeatures.value[regionConnectorId] = supported
    unsupportedFeatures.value[regionConnectorId] = unsupported
  }

  for (const { regionConnectorId, dataNeeds } of await getRegionConnectorsSupportedDataNeeds()) {
    supportedDataNeeds.value[regionConnectorId] = dataNeeds
  }

  for (const { id } of regionConnectors.value) {
    const health = await getRegionConnectorHealth(id)
    regionConnectorHealth.value[id] = health?.status || HealthStatus.UNKNOWN
  }
})
</script>

<template>
  <h1>Region Connectors</h1>
  <p>Monitor and manage the regional endpoints that connect your data exchange infrastructure.</p>

  <Panel
    class="region-connector-panel"
    v-for="{
      id,
      countryCodes,
      coveredMeteringPoints,
      earliestStart,
      latestEnd,
      timeZone
    } in regionConnectors"
    :key="id"
    :value="id"
    toggleable
    :collapsed="!openPanels.has(id)"
    :pt:header:onClick="() => openPanels.delete(id) || openPanels.add(id)"
    pt:content:style="border-top: var(--panel-border)"
  >
    <template #header>
      <header>
        <span class="flag">{{ countryFlag(countryCodes[0]) }}</span>

        <div class="content">
          <h2>{{ id }}</h2>
          <span>{{ formatCountry(countryCodes[0]) }}</span>
        </div>

        <StatusTag :status="regionConnectorHealth[id] || HealthStatus.UNKNOWN" />
      </header>
    </template>

    <template #toggleicon="toggleIconProps">
      <i v-if="toggleIconProps.collapsed" class="pi pi-chevron-down"></i>
      <i v-if="!toggleIconProps.collapsed" class="pi pi-chevron-up"></i>
    </template>

    <h3>Region Connector Metadata</h3>

    <dl>
      <dt>Country</dt>
      <dd>{{ formatCountry(countryCodes[0]) }}</dd>
      <dt>Timezone</dt>
      <dd>{{ timeZone }}</dd>
      <dt>Status</dt>
      <dd>
        <StatusTag :status="regionConnectorHealth[id] || HealthStatus.UNKNOWN" />
      </dd>
      <dt>ID</dt>
      <dd>{{ id }}</dd>
      <dt>Covered Metering Points</dt>
      <dd>{{ coveredMeteringPoints }}</dd>
      <dt>Earliest Start</dt>
      <dd>{{ formatDuration(earliestStart) }}</dd>
      <dt>Latest End</dt>
      <dd>{{ formatDuration(latestEnd) }}</dd>
    </dl>

    <h3>Data Needs</h3>

    <div class="features">
      <div class="feature-list">
        <h4>Supported</h4>
        <ul>
          <li v-for="supportedDataNeed in supportedDataNeeds[id]" :key="supportedDataNeed">
            <a :href="DATA_NEEDS[supportedDataNeed]?.link ?? DATA_NEEDS_DEFAULT_LINK">
              <i class="pi pi-check-circle"></i>
              {{ DATA_NEEDS[supportedDataNeed]?.text ?? supportedDataNeed }}
              <i class="pi pi-external-link"></i>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <h3>Features</h3>

    <div class="features">
      <div class="feature-list">
        <h4>Supported</h4>
        <ul>
          <li v-for="feature in supportedFeatures[id]" :key="feature">
            <a :href="FEATURES[feature as RegionConnectorFeature]?.link ?? FEATURES_DEFAULT_LINK">
              <i class="pi pi-check-circle"></i>
              {{ FEATURES[feature as RegionConnectorFeature]?.text ?? feature }}
              <i class="pi pi-external-link"></i>
            </a>
          </li>
        </ul>
      </div>

      <div class="feature-list feature-list--unsupported">
        <h4>Not Supported</h4>
        <ul>
          <li v-for="feature in unsupportedFeatures[id]" :key="feature">
            <a :href="FEATURES[feature as RegionConnectorFeature]?.link ?? FEATURES_DEFAULT_LINK">
              <i class="pi pi-minus-circle"></i>
              {{ FEATURES[feature as RegionConnectorFeature]?.text ?? feature }}
              <i class="pi pi-external-link"></i>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <template #footer>
      <Button
        class="documentation-link"
        as="a"
        :href="`https://architecture.eddie.energy/framework/1-running/region-connectors/region-connector-${id}.html`"
        target="_blank"
        rounded
      >
        <i class="pi pi-link"></i>
        <b>Open Documentation</b>
        <i>https://architecture.eddie.energy/framework</i>
      </Button>
    </template>
  </Panel>

  <template v-if="disabledRegionConnectors.length > 0">
    <h2 class="disabled-headline">Disabled</h2>
    <p>The following region connectors are currently disabled.</p>
    <Panel
      class="region-connector-panel"
      v-for="regionConnector in disabledRegionConnectors"
      :key="regionConnector"
      :value="regionConnector"
      :header="regionConnector"
      collapsed
    />
  </template>
</template>

<style scoped>
.region-connector-panel {
  margin-top: 1rem;

  &:first-child {
    margin-top: 1.75rem;
  }
}

.disabled-headline {
  margin-top: 2rem;
  font-size: 1.5rem;
  font-weight: 500;
}

header {
  display: grid;
  grid-template-columns: auto minmax(auto, 12.5rem) auto;
  align-items: center;
  gap: 0.75rem;
  line-height: 0.75rem;

  .content {
    display: grid;
    gap: 0.5rem;

    h2 {
      font-weight: 600;
    }
  }
}

h3 {
  margin-top: 1.25rem;
  margin-bottom: 1rem;
  font-weight: 600;

  &:first-child {
    margin-top: 0;
  }
}

dl {
  background: var(--card-background);
  border: var(--card-border);
  border-radius: var(--table-radius);
  display: grid;
  grid-template-columns: 1fr 1fr;
  font-size: 0.875rem;
  line-height: 1;

  dd,
  dt {
    padding: var(--table-padding);
  }

  dd ~ dd,
  dt ~ dt {
    border-top: var(--card-border);
  }
}

.features {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  align-items: start;

  @media (width < 48rem) {
    grid-template-columns: 1fr;
  }
}

.feature-list {
  background: var(--chip-background-success);
  border-radius: 0.5rem;
  padding: 1rem;
  color: var(--chip-text-neutral);
  font-size: 0.875rem;

  li,
  li a {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-top: 0.5rem;
    line-height: 1.25rem;
  }

  i {
    font-size: 1em;
    color: var(--chip-text-success);
  }

  i + i {
    font-size: 0.75em;
    color: var(--chip-text-link);
  }

  a {
    color: var(--chip-text-neutral);
    text-decoration: none;
  }

  h4 {
    color: var(--chip-text-contrast);
    font-weight: 600;
  }
}

.feature-list--unsupported {
  background: var(--chip-background-danger);

  i {
    color: var(--chip-text-danger);
  }
}

.documentation-link {
  i:first-child {
    font-size: 0.75rem;
  }

  b {
    font-weight: 500;
  }

  i:last-child {
    font-size: 0.875rem;
    margin-left: 0.5rem;
  }
}
</style>
