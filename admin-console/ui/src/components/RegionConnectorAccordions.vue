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
import { HEALTH_INDICATOR } from '@/constants/health-indicator'
import { allRegionConnectors } from '@/constants/region-connectors'

import { Accordion, AccordionContent, AccordionHeader, AccordionPanel, Panel } from 'primevue'
import { onMounted, ref } from 'vue'

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

const regionConnectors = ref<RegionConnectorMetadata[]>([])
const regionConnectorHealth = ref<Map<string, string>>(new Map())
const regionConnectorSupportedFeatures = ref<RegionConnectorSupportedFeatures[]>([])
const regionConnectorSupportedDataNeeds = ref<RegionConnectorSupportedDataNeeds[]>([])

onMounted(async () => {
  regionConnectors.value = await getRegionConnectors()
  regionConnectorSupportedFeatures.value = await getRegionConnectorsSupportedFeatures()
  regionConnectorSupportedDataNeeds.value = await getRegionConnectorsSupportedDataNeeds()
  for (const { id } of regionConnectors.value) {
    regionConnectorHealth.value.set(id, (await getRegionConnectorHealth(id))?.status || 'UNKNOWN')
  }
})

function formatCountry(country: string) {
  try {
    return COUNTRY_NAMES.of(country)
  } catch {
    return country
  }
}

function countryFlag(countryCode: string) {
  // check if result is in right range
  if (countryCode.length !== 2) {
    return ''
  }
  return [...countryCode]
    .map((char) => String.fromCodePoint(127397 + char.charCodeAt(0)))
    .reduce((a, b) => `${a}${b}`)
}

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
  return allRegionConnectors.filter(
    (id) => !regionConnectors.value.find((regionConnector) => regionConnector.id === id)
  )
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
              v-for="regionConnector in regionConnectors"
              :key="regionConnector.id"
              :value="regionConnector.id"
              toggleable
              collapsed
            >
              <template #header>
                <div class="flex items-center gap-2">
                  <span class="font-bold">{{ countryFlag(regionConnector.countryCodes[0]) }}</span>
                  <span>{{ regionConnector.id }}</span>
                  <span
                    v-tooltip.top="regionConnectorHealth.get(regionConnector.id) || 'UNKNOWN'"
                    class="font-bold"
                    >{{
                      HEALTH_INDICATOR[
                        (regionConnectorHealth.get(
                          regionConnector.id
                        ) as keyof typeof HEALTH_INDICATOR) || 'UNKNOWN'
                      ]
                    }}</span
                  >
                </div>
              </template>
              <p class="m-0">Country: {{ formatCountry(regionConnector.countryCodes[0]) }}</p>
              <p class="m-0">Timezone: {{ regionConnector.timeZone }}</p>
              <p class="m-0">
                Status: {{ regionConnectorHealth.get(regionConnector.id) || 'UNKNOWN' }}
              </p>
              <Panel class="panel-data-needs" toggleable collapsed header="Supported DataNeeds">
                <ul>
                  <li
                    v-for="supportedDataNeed in getDataNeedsByRegionConnectorId(regionConnector.id)"
                  >
                    {{ supportedDataNeed }}
                  </li>
                </ul>
              </Panel>
              <Panel class="panel-features" toggleable collapsed header="Supported Features">
                <ul>
                  <li
                    v-for="supportedFeature in getSupportedFeaturesByRegionConnectorId(
                      regionConnector.id
                    )"
                  >
                    {{ supportedFeature }}
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
