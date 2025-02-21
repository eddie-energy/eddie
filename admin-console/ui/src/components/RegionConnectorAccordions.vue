<script lang="ts" setup>
import {
  getRegionConnectorHealth,
  getRegionConnectors,
  getRegionConnectorsSupportedDataNeeds,
  getRegionConnectorsSupportedFeatures,
  type RegionConnectorMetadata
} from '@/api'
import { allRegionConnectors } from '@/constants/region-connectors'
import { countryFlag, formatCountry } from '@/util/countries'

import { Accordion, AccordionContent, AccordionHeader, AccordionPanel, Panel } from 'primevue'
import { computed, onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'

const regionConnectors = ref<RegionConnectorMetadata[]>([])
const regionConnectorHealth = ref<Map<string, string>>(new Map())
const regionConnectorSupportedFeatures = ref<Map<string, string>>(new Map())
const regionConnectorSupportedDataNeeds = ref<Map<string, string[]>>(new Map())

const disabledRegionConnectors = computed(() =>
  regionConnectors.value.filter(({ id }) => !allRegionConnectors.includes(id))
)

const SUPPORTED_FEATURES = {
  supportsConnectionsStatusMessages: 'Connections Status Messages',
  supportsRawDataMessages: 'Raw Data Messages',
  supportsTermination: 'Termination',
  supportsAccountingPointMarketDocuments: 'Accounting Point Market Documents',
  supportsPermissionMarketDocuments: 'Permission Market Documents',
  supportsValidatedHistoricalDataMarketDocuments: 'Validated Historical Data Market Documents',
  supportsRetransmissionRequests: 'Retransmission Requests'
}

const SUPPORTED_DATA_NEEDS = {
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
    regionConnectorHealth.value.set(id, (await getRegionConnectorHealth(id))?.status || 'UNKNOWN')
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

                  <HealthIcon :health="regionConnectorHealth.get(id)" />
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
