import { keycloak } from '@/keycloak.js'

const BASE_URL = 'http://localhost:8081'
const PERMISSIONS_URL = BASE_URL + '/permissions'
const DATA_SOURCES_URL = BASE_URL + '/datasources'

function fetchJson(url) {
  return fetch(url, {
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      'Content-Type': 'application/json',
    },
  }).then((response) => response.json())
}

/** @returns {Promise<AiidaPermission>} */
export function getPermissions() {
  return fetchJson(PERMISSIONS_URL)
}

/** @returns {Promise<AiidaDataSource>} */
export function getDataSources() {
  return fetchJson(DATA_SOURCES_URL)
}
