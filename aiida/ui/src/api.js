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

export function revokePermission(permissionId) {
  return fetch(`${PERMISSIONS_URL}/${permissionId}`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      operation: 'REVOKE',
    }),
  })
}

export function deleteDataSource(dataSourceId) {
  return fetch(`${DATA_SOURCES_URL}/${dataSourceId}`, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      'Content-Type': 'application/json',
    },
  })
}
