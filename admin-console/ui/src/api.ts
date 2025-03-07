import {
  CSRF_HEADER,
  CSRF_TOKEN,
  DATA_NEEDS_API_URL,
  PERMISSIONS_API_URL,
  REGION_CONNECTOR_API_URL,
  REGION_CONNECTOR_HEALTH_API_URL,
  REGION_CONNECTORS_SUPPORTED_DATA_NEEDS_API_URL,
  REGION_CONNECTORS_SUPPORTED_FEATURES_API_URL,
  TERMINATION_API_URL
} from '@/config'

export type PageModel<Type> = {
  content: Type[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: 1
  }
}

export type StatusMessage = {
  permissionId: string
  regionConnectorId: string
  dataNeedId: string
  country: string
  dso: string
  startDate: string
  status: string
  cimStatus: string
  parsedStartDate: string
}

export type RegionConnectorMetadata = {
  id: string
  timeZone: string
  countryCodes: string[]
  coveredMeteringPoints: number
  earliestStart: string
  latestEnd: string
  supportedGranularities: string[]
}

export type DataNeed = {
  type: 'account' | 'validated' | 'aiida'
  id: string
  name: string
  description: string
  purpose: string
  policyLink: string
  enabled: boolean
}

export enum HealthStatus {
  UP = 'UP',
  UNKNOWN = 'UNKNOWN',
  OUT_OF_SERVICE = 'OUT_OF_SERVICE',
  DOWN = 'DOWN',
  DISABLED = 'DISABLED'
}

export type RegionConnectorHealth = {
  status: HealthStatus
  components?: {
    [key: string]: {
      status: HealthStatus
      details?: string
    }
  }
}

export type RegionConnectorSupportedFeatures = {
  regionConnectorId: string
  supportsRawDataMessages: boolean
  supportsTermination: boolean
  supportsAccountingPointMarketDocuments: boolean
  supportsPermissionMarketDocuments: boolean
  supportsValidatedHistoricalDataMarketDocuments: boolean
  supportsRetransmissionRequests: boolean
  supportsConnectionsStatusMessages: boolean
}

export type RegionConnectorSupportedDataNeeds = {
  regionConnectorId: string
  dataNeeds: string[]
}

export async function getPermissions(): Promise<StatusMessage[]> {
  return await fetch(PERMISSIONS_API_URL).then((res) => res.json())
}

export async function getPermissionsPaginated(page: number, size: number): Promise<PageModel<StatusMessage>> {
  return await fetch(`${PERMISSIONS_API_URL}?page=${page}&size=${size}`).then((res) => res.json())
}

export async function getStatusMessages(permissionId: string): Promise<StatusMessage[]> {
  return await fetch(`${PERMISSIONS_API_URL}/${permissionId}`).then((res) => res.json())
}

export async function terminatePermission(permissionId: string) {
  const response = await fetch(`${TERMINATION_API_URL}/${permissionId}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      [CSRF_HEADER]: CSRF_TOKEN
    }
  })

  if (!response.ok) {
    throw new Error('Failed to terminate permission')
  }
}

export async function getDataNeeds(): Promise<DataNeed[]> {
  return await fetch(DATA_NEEDS_API_URL).then((res) => res.json())
}

export async function getRegionConnectors(): Promise<RegionConnectorMetadata[]> {
  return await fetch(REGION_CONNECTOR_API_URL).then((res) => res.json())
}

export async function getRegionConnectorHealth(
  regionConnectorId: string
): Promise<RegionConnectorHealth> {
  return await fetch(
    `${REGION_CONNECTOR_HEALTH_API_URL}/region-connector-${regionConnectorId}`
  ).then((res) => (res.status === 404 ? { status: HealthStatus.UNKNOWN } : res.json()))
}

export async function getRegionConnectorsSupportedFeatures(): Promise<
  RegionConnectorSupportedFeatures[]
> {
  return await fetch(REGION_CONNECTORS_SUPPORTED_FEATURES_API_URL).then((res) => res.json())
}

export async function getRegionConnectorsSupportedDataNeeds(): Promise<
  RegionConnectorSupportedDataNeeds[]
> {
  return await fetch(REGION_CONNECTORS_SUPPORTED_DATA_NEEDS_API_URL).then((res) => res.json())
}
