<script lang="ts" setup>
import {
  getRegionConnectorHealth,
  getRegionConnectors,
  getRegionConnectorsSupportedDataNeeds,
  getRegionConnectorsSupportedFeatures,
  type RegionConnectorMetadata,
  type RegionConnectorSupportedDataNeeds,
  type RegionConnectorSupportedFeatures
} from '@/api'
import { allRegionConnectors } from '@/constants/region-connectors'
import { countryFlag, formatCountry } from '@/util/countries'

import { Accordion, AccordionContent, AccordionHeader, AccordionPanel, Panel } from 'primevue'
import { onMounted, ref } from 'vue'
import HealthIcon from '@/components/HealthIcon.vue'

const regionConnectors = ref<RegionConnectorMetadata[]>([])
const regionConnectorHealth = ref<Map<string, string>>(new Map())
const regionConnectorSupportedFeatures = ref<RegionConnectorSupportedFeatures[]>([])
const regionConnectorSupportedDataNeeds = ref<RegionConnectorSupportedDataNeeds[]>([])

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
  regionConnectorSupportedFeatures.value = await getRegionConnectorsSupportedFeatures()
  regionConnectorSupportedDataNeeds.value = await getRegionConnectorsSupportedDataNeeds()
  for (const { id } of regionConnectors.value) {
    regionConnectorHealth.value.set(id, (await getRegionConnectorHealth(id))?.status || 'UNKNOWN')
  }
})

function getDataNeedsByRegionConnectorId(regionConnectorId: string) {
  const regionConnector = regionConnectorSupportedDataNeeds.value.find(
    (region) => region.regionConnectorId === regionConnectorId
  )
  return regionConnector?.dataNeeds
}

function getSupportedFeaturesByRegionConnectorId(regionConnectorId: string) {
  const regionConnector = regionConnectorSupportedFeatures.value.find(
    (region) => region.regionConnectorId === regionConnectorId
  )
  return regionConnector
    ? Object.keys(regionConnector).filter(
        (key) => key != 'regionConnectorId' && regionConnector[key]
      )
    : []
}

function getDisabledRegionConnectors() {
  return regionConnectors.value.filter(({ id }) => !allRegionConnectors.includes(id))
}
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
                <dd>{{ regionConnectorHealth.get(id) || 'UNKNOWN' }}</dd>
              </dl>

              <Panel class="panel-data-needs" toggleable collapsed header="Supported DataNeeds">
                <ul>
                  <li v-for="supportedDataNeed in getDataNeedsByRegionConnectorId(id)">
                    {{ SUPPORTED_DATA_NEEDS[supportedDataNeed] }}
                  </li>
                </ul>
              </Panel>
              <Panel class="panel-features" toggleable collapsed header="Supported Features">
                <ul>
                  <li v-for="supportedFeature in getSupportedFeaturesByRegionConnectorId(id)">
                    {{ SUPPORTED_FEATURES[supportedFeature] }}
                  </li>
                </ul>
              </Panel>
            </Panel>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>
      <Accordion value="1">
        <AccordionPanel value="1">
          <AccordionHeader>Disabled ({{ getDisabledRegionConnectors().length }})</AccordionHeader>
          <AccordionContent>
            <Panel
              class="region-connector-panel"
              v-for="regionConnector in getDisabledRegionConnectors()"
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
