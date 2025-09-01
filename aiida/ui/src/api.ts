import { keycloak } from './keycloak'
import type { AiidaDataSource, AiidaPermission, AiidaPermissionRequest } from './types'
import { notify } from './util/toast'

export const BASE_URL = THYMELEAF_AIIDA_PUBLIC_URL ?? import.meta.env.VITE_AIIDA_PUBLIC_URL

const FALLBACK_ERROR_MESSAGES = {
  400: 'This action was declined as invalid. Please check your input.',
  401: 'Authorization failed. Please log in again.',
  403: 'Access was denied. The action might not be available anymore. Please try and reload this page.',
  404: 'The target resource was not found. It might have been removed. Please try and reload this page.',
  500: 'Something went wrong. Please try again.',
}

/**
 * Wrapper for {@link window.fetch} preconfigured with base url, content-type, authorization, and error response handling.
 *
 * @param path {string}
 * @param init {RequestInit?}
 * @returns {Promise<any>}
 * @throws {{message: string, cause: Response}} Error message and response as cause for HTTP status codes outside the 2xx range.
 */
async function fetch(path: string, init?: RequestInit): Promise<any> {
  try {
    await keycloak.updateToken(5)
  } catch (error) {
    notify('Failed to update authentication token. Please reload this page.', 'danger')
    throw error
  }

  const response = await window
    .fetch(BASE_URL + path, {
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        'Content-Type': 'application/json',
      },
      ...init,
    })
    .catch((error: unknown) => {
      notify('Network error. Please check your connection.', 'danger')
      throw error
    })

  if (!response.ok) {
    const message =
      (await parseErrorResponse(response)) ??
      FALLBACK_ERROR_MESSAGES[response.status as keyof typeof FALLBACK_ERROR_MESSAGES] ??
      'An unexpected error occurred. Please try again.'
    notify(message, 'danger')
    throw new Error(message)
  }

  if (response.headers.get('content-type')?.includes('application/json')) {
    return response.json()
  }

  return response.text()
}

/**
 * Helper for parsing error messages from response objects.
 * TODO: Simplify by enforcing a single format for all error messages on the backend.
 *
 * @param {Response} response
 * @returns {Promise<string>}
 */
async function parseErrorResponse(response: Response): Promise<string> {
  // Check if response is JSON
  if (!response.headers.get('content-type')?.includes('application/json')) {
    return response.text()
  }

  const json = await response.json()

  // EDDIE-style error messages
  if (json.errors && Array.isArray(json.errors) && json.errors.length > 0) {
    return json.errors.map(({ message }: { message: string }) => message).join(' ')
  }

  // AIIDA-style error messages
  if (json.message) {
    return json.message
  }

  // Fallback for other formats
  return JSON.stringify(json)
}

/** @returns {Promise<AiidaPermission[]>} */
export function getPermissions() {
  return fetch('/permissions')
}

/** @returns {Promise<AiidaDataSource[]>} */
export function getDataSources() {
  return fetch('/datasources/outbound')
}

/** @returns {Promise<AiidaDataSourceType[]>} */
export function getDataSourceTypes() {
  return fetch('/datasources/outbound/types')
}

/** @returns {Promise<{assets: string[]}>} */
export function getAssetTypes() {
  return fetch('/datasources/assets')
}

/** @returns {Promise<{id: string, name: string}[]>} */
export function getModbusVendors() {
  return fetch('/datasources/modbus/vendors')
}

/** @returns {Promise<{id: string, name: string, vendorId: string}[]>} */
export function getModbusModels(
  vendorId: string,
): Promise<{ id: string; name: string; vendorId: string }[]> {
  return fetch(`/datasources/modbus/vendors/${vendorId}/models`)
}

/** @returns {Promise<{id: string, name: string, modelId: string}[]>} */
export function getModbusDevices(
  modelId: string,
): Promise<{ id: string; name: string; modelId: string }[]> {
  return fetch(`/datasources/modbus/models/${modelId}/devices`)
}

/** @returns {Promise<AiidaApplicationInformation>} */
export function getApplicationInformation() {
  return fetch('/application-information')
}

/**
 * Create permission from a permission request.
 *
 * @param permission {AiidaPermissionRequest}
 * @returns {Promise<AiidaPermission>}
 */
export function addPermission(permission: AiidaPermissionRequest): Promise<AiidaPermission> {
  return fetch('/permissions', {
    method: 'POST',
    body: JSON.stringify(permission),
  })
}

export async function rejectPermission(permissionId: string): Promise<any> {
  const result = await fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REJECT',
    }),
  })
  notify('Permission request rejected.', 'success')
  return result
}

/**
 * Accepts an existing permission request and assigns a data source.
 * @param {string} permissionId Id of the permission to update.
 * @param {string} dataSourceId Data source to use to provide data for this permission.
 * @returns {Promise<void>}
 */
export async function acceptPermission(permissionId: string, dataSourceId: string): Promise<void> {
  await fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'ACCEPT',
      dataSourceId,
    }),
  })
  notify('Permission request accepted.', 'success')
}

export async function revokePermission(permissionId: string): Promise<void> {
  await fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REVOKE',
    }),
  })
  notify('The permission for this service was revoked.', 'success')
}

/**
 * Create a new data source.
 *
 * @param {Omit<AiidaDataSource, id>} dataSource
 */
export async function addDataSource(
  dataSource: Omit<AiidaDataSource, 'id'>,
): Promise<AiidaDataSource> {
  const result = await fetch(`/datasources`, {
    method: 'POST',
    body: JSON.stringify(dataSource),
  })
  notify('Data source created.', 'success')
  return result
}

/**
 * Updates the data source with the given id with the provided contents.
 *
 * @param {string} dataSourceId
 * @param {Omit<AiidaDataSource, id>} dataSource
 */
export async function saveDataSource(
  dataSourceId: string,
  dataSource: Omit<AiidaDataSource, 'id'>,
): Promise<void> {
  await fetch(`/datasources/${dataSourceId}`, {
    method: 'PATCH',
    body: JSON.stringify(dataSource),
  })
  notify('Changes to this data source have been saved.', 'success')
}

export async function deleteDataSource(dataSourceId: string): Promise<void> {
  await fetch(`/datasources/${dataSourceId}`, {
    method: 'DELETE',
  })
  notify('Data source deleted successfully.', 'success')
}

/**
 * Generates a new password for the MQTT data source of the given id.
 *
 * @param dataSourceId
 * @returns {Promise<{ plaintextPassword: string }>}
 */
export function regenerateDataSourceSecrets(
  dataSourceId: string,
): Promise<{ plaintextPassword: string }> {
  return fetch(`/datasources/${dataSourceId}/regenerate-secrets`, {
    method: 'POST',
  })
}
