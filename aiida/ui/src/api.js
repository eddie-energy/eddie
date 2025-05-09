import { keycloak } from '@/keycloak.js'

const BASE_URL = 'http://localhost:8081'

/**
 * Wrapper for {@link window.fetch} preconfigured with base url, content-type and authorization.
 *
 * @param path {string}
 * @param init {RequestInit?}
 * @returns {Promise<Response>}
 */
function fetch(path, init) {
  return keycloak.updateToken(5).then(() =>
    window.fetch(BASE_URL + path, {
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        'Content-Type': 'application/json',
      },
      ...init,
    }),
  )
}

function fetchJson(path) {
  return fetch(path).then((response) => response.json())
}

/** @returns {Promise<AiidaPermission>} */
export function getPermissions() {
  return fetchJson('/permissions')
}

/** @returns {Promise<AiidaDataSource>} */
export function getDataSources() {
  return fetchJson('/datasources')
}

/** @returns {Promise<AiidaApplicationInformation>} */
export function getApplicationInformation() {
  return fetchJson('/application-information')
}

/**
 * Create permission from a permission request.
 *
 * @param permission {AiidaPermissionRequest}
 * @returns {Promise<AiidaPermission>}
 */
export function addPermission(permission) {
  return fetch('/permissions', {
    method: 'POST',
    body: JSON.stringify(permission),
  }).then((response) =>
    response.json().then((json) => {
      if (!response.ok) {
        throw new Error(`Failed to add permission: ${json.errors[0].message}`)
      }
      return json
    }),
  )
}

export function revokePermission(permissionId) {
  return fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REVOKE',
    }),
  })
}

export function deleteDataSource(dataSourceId) {
  return fetch(`/datasources/${dataSourceId}`, {
    method: 'DELETE',
  })
}
