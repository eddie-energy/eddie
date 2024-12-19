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

export type RegionConnectorHealth = {
  status: string
  components?: {
    [key: string]: {
      status: string
      details?: string
    }
  }
}

export type RegionConnectorSupportedFeatures = {
  regionConnectorId: string
  dataNeeds: string[]
}

export type RegionConnectorSupportedDataNeeds = {
  regionConnectorId: string
  dataNeeds: boolean[]
}

export async function getPermissions(): Promise<StatusMessage[]> {
  return await fetch(PERMISSIONS_API_URL).then((res) => res.json())
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
): Promise<RegionConnectorHealth | undefined> {
  return await fetch(
    `${REGION_CONNECTOR_HEALTH_API_URL}/region-connector-${regionConnectorId}`
  ).then((res) => (res.status === 404 ? undefined : res.json()))
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
