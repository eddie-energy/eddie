const CORE_URL = import.meta.env.VITE_CORE_URL ?? new URL(import.meta.url).href.split("/lib/eddie-components.js")[0];

function fetchJson(path) {
  return fetch(CORE_URL + path).then((response) => {
    if (!response.ok) {
      throw new Error(
        `Fetch to ${path} returned invalid status code ${response.status}`
      );
    }
    return response.json();
  });
}

/**
 * Fetches the data need attributes for the given data need ID.
 * @param {string} dataNeedId - The ID of the data need to fetch attributes for.
 * @returns {Promise<DataNeedAttributes>} - The attributes of the data need.
 */
export function getDataNeedAttributes(dataNeedId) {
  return fetchJson(`/data-needs/api/${dataNeedId}`);
}

/**
 * Fetches the data need calculations for the given data need ID.
 * @param dataNeedId - The ID of the data need to fetch calculations for.
 * @returns {Promise<Map<string, DataNeedCalculation>>} - A map of region connector IDs to their respective data need calculation.
 */
export function getDataNeedCalculations(dataNeedId) {
  return fetchJson(`/api/region-connectors/data-needs/${dataNeedId}`);
}

/**
 * Fetches the supported region connectors for the given data need ID.
 * @param dataNeedId - The ID of the data need to fetch supported region connectors for.
 * @returns {Promise<string[]>} - A list of region connector IDs that support the given data need.
 */
export function getSupportedRegionConnectors(dataNeedId) {
  return getDataNeedCalculations(dataNeedId).then((calculations) =>
    Object.keys(calculations).filter(
      (regionConnectorId) => calculations[regionConnectorId].supportsDataNeed
    )
  );
}

/**
 * Fetches the metadata for all region connectors.
 * @returns {Promise<RegionConnectorMetadata[]>} - Metadata for all region connectors.
 */
export function getRegionConnectorMetadata() {
  return fetchJson("/api/region-connectors-metadata");
}
