<script lang="ts" setup>
import {
  getRegionConnectorHealth,
  getRegionConnectors,
  getRegionConnectorsSupportedDataNeeds,
  getRegionConnectorsSupportedFeatures,
  type RegionConnectorMetadata,
  type RegionConnectorSupportedDataNeeds,
  type RegionConnectorSupportedFeatures,
} from "@/api";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";
import Panel from "primevue/panel";
import { onMounted, ref } from "vue";

const COUNTRY_NAMES = new Intl.DisplayNames(["en"], { type: "region" });

const regionConnectors = ref<RegionConnectorMetadata[]>([]);
const regionConnectorHealth = ref<Map<string, string>>(new Map());
const regionConnectorSupportedFeatures = ref<RegionConnectorSupportedFeatures[]>([]);
const regionConnectorSupportedDataNeeds = ref<RegionConnectorSupportedDataNeeds[]>([]);

onMounted(async () => {
  regionConnectors.value = await getRegionConnectors();
  regionConnectorSupportedFeatures.value = await getRegionConnectorsSupportedFeatures();
  regionConnectorSupportedDataNeeds.value = await getRegionConnectorsSupportedDataNeeds();
  for (const { id } of regionConnectors.value) {
    regionConnectorHealth.value.set(id, (await getRegionConnectorHealth(id))?.status || "UNKNOWN");
  }
});

function formatCountry(country: string) {
  try {
    return COUNTRY_NAMES.of(country);
  } catch {
    return country;
  }
}

function countryFlag(countryCode: string) {
  // check if result is in right range
  if (countryCode.length !== 2) {
    return "";
  }
  return [...countryCode]
    .map((char) => String.fromCodePoint(127397 + char.charCodeAt(0)))
    .reduce((a, b) => `${a}${b}`);
}

function getDataNeedsByRegionConnectorId(regionConnectorId: string) {
  const region = regionConnectorSupportedDataNeeds.value.find(
    (region) => region.regionConnectorId === regionConnectorId,
  );
  return region ? region.dataNeeds : "NONE";
}

function getSupportedFeaturesByRegionConnectorId(regionConnectorId: string) {
  const region = regionConnectorSupportedFeatures.value.find(
    (region) => region.regionConnectorId === regionConnectorId,
  );
  console.log(region);
  return region;
}
</script>

<template>
  <main class="outer">
    <h1>Region Connectors</h1>
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
                  <span>{{ regionConnector.id }}</span>
                  <span class="font-bold"
                  >[{{ countryFlag(regionConnector.countryCodes[0]) }}]</span
                  >
                  <span class="font-bold"
                  >[{{ regionConnectorHealth.get(regionConnector.id) || "UNKNOWN" }}]</span
                  >
                </div>
              </template>
              <p class="m-0">Country: {{ formatCountry(regionConnector.countryCodes[0]) }}</p>
              <p class="m-0">Timezone: {{ regionConnector.timeZone }}</p>
              <p class="m-0">
                Status: {{ regionConnectorHealth.get(regionConnector.id) || "UNKNOWN" }}
              </p>
              <p class="m-0">
                Supported DataNeeds:
                <ul>
                  <li v-for="feature in getDataNeedsByRegionConnectorId(regionConnector.id)"
                      :key="feature.regionConnectorId"
                  >
                    {{ feature }}
                  </li>
                </ul>
              </p>
              <p>Supported Features:</p>
              <p>{{ getSupportedFeaturesByRegionConnectorId(regionConnector.id) }}
              </p>
            </Panel>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>
      <Accordion value="1">
        <AccordionPanel value="1">
          <AccordionHeader>Disabled</AccordionHeader>
          <AccordionContent>
            <p class="m-0">
              Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor
              incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
              exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure
              dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
              Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt
              mollit anim id est laborum.
            </p>
          </AccordionContent>
        </AccordionPanel>
      </Accordion>
    </div>
  </main>
</template>

<style scoped>
.accordion-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  height: 100%;
  gap: 1rem;
}

.region-connector-panel {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}
</style>
