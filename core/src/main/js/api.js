const CORE_URL =
  import.meta.env.VITE_CORE_URL ?? import.meta.url.split("/lib/")[0];

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
 * Fetches the data need calculations for the given data need IDs.
 * @param {string} dataNeedId - The IDs of the data needs to fetch calculations for in a comma separated list.
 * @returns {Promise<Array<string>>} - A map of region connector IDs to their respective data need calculation.
 */
export function getSupportedRegionConnectorsFor(dataNeedId) {
  return fetchJson(
    `/api/region-connectors/data-needs?data-need-id=${dataNeedId}`
  );
}

/**
 * Fetches the metadata for all region connectors.
 * @returns {Promise<RegionConnectorMetadata[]>} - Metadata for all region connectors.
 */
export function getRegionConnectorMetadata() {
  return fetchJson("/api/region-connectors-metadata");
}

/**
 * Fetches all the metadata for all permission administrators.
 * @returns {Promise<PermissionAdministrator[]>} - Metadata for all permission administrators.
 */
export function getPermissionAdministrators() {
  return fetchJson("/european-masterdata/api/permission-administrators");
}
