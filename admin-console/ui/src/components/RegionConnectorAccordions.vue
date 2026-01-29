<!--
SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script lang="ts" setup>
import {
  getRegionConnectorHealth,
  getRegionConnectors,
  getRegionConnectorsSupportedDataNeeds,
  getRegionConnectorsSupportedFeatures,
  HealthStatus,
  type RegionConnectorMetadata,
  type RegionConnectorSupportedFeatures
} from '@/api'
import { REGION_CONNECTORS } from '@/constants'
import { countryFlag, formatCountry } from '@/util/countries'

import { Accordion, AccordionContent, AccordionHeader, AccordionPanel, Panel } from 'primevue'
import { computed, onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'

const regionConnectors = ref<RegionConnectorMetadata[]>([])
const regionConnectorHealth = ref<Map<string, HealthStatus>>(new Map())
const regionConnectorSupportedFeatures = ref<Map<string, string>>(new Map())
const regionConnectorSupportedDataNeeds = ref<Map<string, string[]>>(new Map())

const disabledRegionConnectors = computed(() =>
  regionConnectors.value.filter(({ id }) => !REGION_CONNECTORS.includes(id)).map(({ id }) => id)
)

const SUPPORTED_FEATURES: {
  [K in Exclude<keyof RegionConnectorSupportedFeatures, 'regionConnectorId'>]: string
} & {
  [key: string]: string
} = {
  supportsConnectionsStatusMessages: 'Connections Status Messages',
  supportsRawDataMessages: 'Raw Data Messages',
  supportsTermination: 'Termination v0.82',
  supportsAccountingPointMarketDocuments: 'Accounting Point Market Documents v0.82',
  supportsPermissionMarketDocuments: 'Permission Market Documents v0.82',
  supportsValidatedHistoricalDataMarketDocuments:
    'Validated Historical Data Market Documents v0.82',
  supportsRetransmissionRequests: 'Retransmission Requests v0.91.08',
  supportsValidatedHistoricalDataMarketDocumentsV1_04:
    'Validated Historical Data Market Documents v1.04',
  supportsNearRealTimeDataMarketDocumentsV1_04: 'Near Real Time Data Market Documents v1.04'
}

const SUPPORTED_DATA_NEEDS: { [key: string]: string } = {
  ValidatedHistoricalDataDataNeed: 'Validated Historical Data',
  AccountingPointDataNeed: 'Accounting Point'
}

onMounted(async () => {
  regionConnectors.value = await getRegionConnectors()
  regionConnectorSupportedFeatures.value = (await getRegionConnectorsSupportedFeatures()).reduce(
    (map, { regionConnectorId, ...features }) =>
      map.set(
        regionConnectorId,
        Object.entries(features)
          .filter(([, value]) => value)
          .map(([key]) => key)
      ),
    new Map()
  )
  regionConnectorSupportedDataNeeds.value = (await getRegionConnectorsSupportedDataNeeds()).reduce(
    (map, { dataNeeds, regionConnectorId }) => map.set(regionConnectorId, dataNeeds),
    new Map()
  )
  for (const { id } of regionConnectors.value) {
    const health = await getRegionConnectorHealth(id)
    regionConnectorHealth.value.set(id, health?.status || HealthStatus.UNKNOWN)
  }
})
</script>

<template>
  <main class="outer">
    <div class="accordion-grid">
      <Accordion value="0">
        <AccordionPanel value="0">
          <AccordionHeader>Enabled ({{ regionConnectors.length }})</AccordionHeader>
          <AccordionContent>
            <Panel
              class="region-connector-panel"
              v-for="{ countryCodes, id, timeZone } in regionConnectors"
              :key="id"
              :value="id"
              toggleable
              collapsed
            >
              <template #header>
                <div class="flex items-center gap-2">
                  <span class="font-bold">{{ countryFlag(countryCodes[0]) }}</span>
                  <span>{{ id }}</span>

                  <HealthIcon :health="regionConnectorHealth.get(id) || HealthStatus.UNKNOWN" />
                </div>
              </template>

              <dl style="display: grid; grid-template-columns: auto 1fr; column-gap: 2rem">
                <dt>Country:</dt>
                <dd>{{ formatCountry(countryCodes[0]) }}</dd>
                <dt>Timezone:</dt>
                <dd>{{ timeZone }}</dd>
                <dt>Status:</dt>
                <dd>{{ regionConnectorHealth.get(id) }}</dd>
              </dl>

              <Panel class="panel-data-needs" toggleable collapsed header="Supported DataNeeds">
                <ul>
                  <li v-for="supportedDataNeed in regionConnectorSupportedDataNeeds.get(id)">
                    {{ SUPPORTED_DATA_NEEDS[supportedDataNeed] }}
                  </li>
                </ul>
              </Panel>
              <Panel class="panel-features" toggleable collapsed header="Supported Features">
                <ul>
                  <li v-for="feature in regionConnectorSupportedFeatures.get(id)" :key="feature">
                    {{ SUPPORTED_FEATURES[feature] }}
                  </li>
                </ul>
              </Panel>
            </Panel>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>
      <Accordion value="1">
        <AccordionPanel value="1">
          <AccordionHeader>Disabled ({{ disabledRegionConnectors.length }})</AccordionHeader>
          <AccordionContent>
            <Panel
              class="region-connector-panel"
              v-for="regionConnector in disabledRegionConnectors"
              :key="regionConnector"
              :value="regionConnector"
              collapsed
              :header="regionConnector"
            >
            </Panel>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>
    </div>
  </main>
</template>

<style scoped>
.accordion-grid {
  display: grid;
  grid-template-columns: 1fr;
  height: 100%;
  gap: 1rem;

  @media only screen and (min-width: 1280px) {
    grid-template-columns: 1fr 1fr;
  }
}

.region-connector-panel {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}

span {
  margin-right: 0.5rem;
}

.panel-data-needs {
  margin: 1rem 0;
}
</style>
