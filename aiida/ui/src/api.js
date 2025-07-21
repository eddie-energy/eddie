import { keycloak } from '@/keycloak.js'

export const BASE_URL =
  THYMELEAF_AIIDA_PUBLIC_URL ?? import.meta.env.VITE_AIIDA_PUBLIC_URL ?? 'http://localhost:8080'

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

/** @returns {Promise<AiidaPermission[]>} */
export function getPermissions() {
  return fetchJson('/permissions')
}

/** @returns {Promise<AiidaDataSource[]>} */
export function getDataSources() {
  return fetchJson('/datasources')
}

/** @returns {Promise<AiidaDataSourceType[]>} */
export function getDataSourceTypes() {
  return fetchJson('/datasources/types')
}

/** @returns {Promise<{assets: string[]}>} */
export function getAssetTypes() {
  return fetchJson('/datasources/assets')
}

/** @returns {Promise<{id: string, name: string}[]>} */
export function getModbusVendors() {
  return fetchJson('/datasources/modbus/vendors')
}

/** @returns {Promise<{id: string, name: string, vendorId: string}[]>} */
export function getModbusModels(vendorId) {
  return fetchJson(`/datasources/modbus/vendors/${vendorId}/models`)
}

/** @returns {Promise<{id: string, name: string, modelId: string}[]>} */
export function getModbusDevices(modelId) {
  return fetchJson(`/datasources/modbus/models/${modelId}/devices`)
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

export function rejectPermission(permissionId) {
  return fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REJECT',
    }),
  })
}

export function acceptPermission(permissionId, dataSourceId) {
  return fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'ACCEPT',
      dataSourceId,
    }),
  })
}

export function revokePermission(permissionId) {
  return fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REVOKE',
    }),
  })
}

/**
 * Create a new data source.
 *
 * @param {Omit<AiidaDataSource, id>} dataSource
 */
export function addDataSource(dataSource) {
  return fetch(`/datasources`, {
    method: 'POST',
    body: JSON.stringify(dataSource),
  })
}

/**
 * Updates the data source with the given id with the provided contents.
 *
 * @param {string} dataSourceId
 * @param {Omit<AiidaDataSource, id>} dataSource
 */
export function saveDataSource(dataSourceId, dataSource) {
  return fetch(`/datasources/${dataSourceId}`, {
    method: 'PATCH',
    body: JSON.stringify(dataSource),
  })
}

export function deleteDataSource(dataSourceId) {
  return fetch(`/datasources/${dataSourceId}`, {
    method: 'DELETE',
  })
}

/**
 * Generates a new password for the MQTT data source of the given id.
 *
 * @param dataSourceId
 * @returns {Promise<{ plaintextPassword: string }>}
 */
export function regenerateDataSourceSecrets(dataSourceId) {
  return fetch(`/datasources/${dataSourceId}/regenerate-secrets`, {
    method: 'POST',
  }).then((response) => {
    return response.ok
      ? response.json()
      : response.text().then((error) => {
          throw new Error(`Failed to regenerate secrets: ${error}`)
        })
  })
}
