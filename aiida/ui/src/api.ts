import useToast from './composables/useToast'
import { keycloak } from './keycloak'
import type {
  AiidaApplicationInformation,
  AiidaDataSource,
  AiidaDataSourceType,
  AiidaPermission,
  AiidaPermissionRequest,
} from './types'
const { danger, success } = useToast()

export const BASE_URL = THYMELEAF_AIIDA_PUBLIC_URL ?? import.meta.env.VITE_AIIDA_PUBLIC_URL

const FALLBACK_ERROR_MESSAGES = {
  400: 'This action was declined as invalid. Please check your input.',
  401: 'Authorization failed. Please log in again.',
  403: 'Access was denied. The action might not be available anymore. Please try and reload this page.',
  404: 'The target resource was not found. It might have been removed. Please try and reload this page.',
  500: 'Something went wrong. Please try again.',
}

async function fetch(path: string, init?: RequestInit): Promise<any> {
  try {
    await keycloak.updateToken(5)
  } catch (error) {
    danger('Failed to update authentication token. Please reload this page.')
    throw error
  }
  const isImagesEndpoint = path.startsWith('/datasources/images')

  const response = await window
    .fetch(BASE_URL + path, {
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        ...(!isImagesEndpoint ? { 'Content-Type': 'application/json' } : {}),
      },
      ...init,
    })
    .catch((error: unknown) => {
      danger('Network error. Please check your connection.')
      throw error
    })

  if (!response.ok) {
    const message =
      (await parseErrorResponse(response)) ??
      FALLBACK_ERROR_MESSAGES[response.status as keyof typeof FALLBACK_ERROR_MESSAGES] ??
      'An unexpected error occurred. Please try again.'
    if (!(isImagesEndpoint && response.status == 404)) {
      danger(message, response.status == 404 ? 5000 : 0, true)
    }
    throw new Error(message)
  }

  if (response.headers.get('content-type')?.includes('application/json')) {
    return response.json()
  }
  if (isImagesEndpoint) {
    return response.blob()
  }

  return response.text()
}

/**
 * Helper for parsing error messages from response objects.
 * TODO: Simplify by enforcing a single format for all error messages on the backend.
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

export function getPermissions(): Promise<AiidaPermission[]> {
  return fetch('/permissions')
}

export function getDataSources(): Promise<AiidaDataSource[]> {
  return fetch('/datasources/outbound')
}

export function getDataSourceTypes(): Promise<AiidaDataSourceType[]> {
  return fetch('/datasources/outbound/types')
}

export function getAssetTypes(): Promise<{ assets: string[] }> {
  return fetch('/datasources/assets')
}

export function getIconTypes(): Promise<{ icons: string[] }> {
  return fetch('/datasources/icons')
}

export function getModbusVendors(): Promise<{ id: string; name: string }[]> {
  return fetch('/datasources/modbus/vendors')
}

export function getModbusModels(
  vendorId: string,
): Promise<{ id: string; name: string; vendorId: string }[]> {
  return fetch(`/datasources/modbus/vendors/${vendorId}/models`)
}

export function getModbusDevices(
  modelId: string,
): Promise<{ id: string; name: string; modelId: string }[]> {
  return fetch(`/datasources/modbus/models/${modelId}/devices`)
}

export function getApplicationInformation(): Promise<AiidaApplicationInformation> {
  return fetch('/application-information')
}

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
  success('Permission request rejected.')
  return result
}

export async function acceptPermission(permissionId: string, dataSourceId: string): Promise<void> {
  await fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'ACCEPT',
      dataSourceId,
    }),
  })
  success('Permission request accepted.')
}

export async function revokePermission(permissionId: string): Promise<void> {
  await fetch(`/permissions/${permissionId}`, {
    method: 'PATCH',
    body: JSON.stringify({
      operation: 'REVOKE',
    }),
  })
  success('The permission for this service was revoked.')
}

export async function addDataSource(dataSource: Omit<AiidaDataSource, 'id'>): Promise<{
  dataSourceId: string
  plaintextPassword: string
}> {
  const result = await fetch(`/datasources`, {
    method: 'POST',
    body: JSON.stringify(dataSource),
  })
  success('Data source created.')
  return result
}

export async function saveDataSource(
  dataSourceId: string,
  dataSource: Omit<AiidaDataSource, 'id'>,
): Promise<void> {
  await fetch(`/datasources/${dataSourceId}`, {
    method: 'PATCH',
    body: JSON.stringify(dataSource),
  })
  success('Changes to this data source have been saved.')
}

export async function toggleDataSource(
  dataSourceId: string,
  dataSource: Omit<AiidaDataSource, 'id'>,
): Promise<void> {
  const enabled = !dataSource.enabled
  await fetch(`/datasources/${dataSourceId}/enabled`, {
    method: 'PATCH',
    body: JSON.stringify(enabled),
  })
  success(`${dataSource.name} has been ${enabled ? 'enabled' : 'disabled'} `)
}

export async function deleteDataSource(dataSourceId: string): Promise<void> {
  await fetch(`/datasources/${dataSourceId}`, {
    method: 'DELETE',
  })
  success('Data source deleted successfully.')
}

export function regenerateDataSourceSecrets(
  dataSourceId: string,
): Promise<{ plaintextPassword: string }> {
  return fetch(`/datasources/${dataSourceId}/regenerate-secrets`, {
    method: 'POST',
  })
}

export async function addDataSourceImage(dataSourceId: string, imageFile: File): Promise<void> {
  const data = new FormData()
  data.append('file', imageFile as Blob)
  await fetch(`/datasources/images/${dataSourceId}`, {
    method: 'POST',
    body: data,
  })
}
export async function getDataSourceImage(dataSourceId: string): Promise<Blob> {
  return fetch(`/datasources/images/${dataSourceId}`, {
    method: 'GET',
  })
}

export async function getLatestDataSourceMessage(id: string) {
  return fetch(`/messages/data-source/${id}/latest`, {
    method: 'GET',
  })
}

export async function getLatestOutboundPermissionMessage(id: string) {
  return fetch(`/messages/permission/${id}/outbound/latest`, {
    method: 'GET',
  })
}

export async function getLatestInboundPermissionMessage(id: string) {
  return fetch(`/messages/permission/${id}/inbound/latest`, {
    method: 'GET',
  })
}
