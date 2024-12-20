import { DATA_NEEDS_API_URL, PERMISSIONS_API_URL, REGION_CONNECTOR_API_URL } from '@/config'

export type StatusMessage = {
  permissionId: string
  regionConnectorId: string
  dataNeedId: string
  country: string
  dso: string
  startDate: string
  status: string
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

export async function getPermissions(): Promise<StatusMessage[]> {
  return await fetch(PERMISSIONS_API_URL).then((res) => res.json())
}

export async function getDataNeeds(): Promise<DataNeed[]> {
  return await fetch(DATA_NEEDS_API_URL).then((res) => res.json())
}

export async function getRegionConnectors(): Promise<RegionConnectorMetadata[]> {
  return await fetch(REGION_CONNECTOR_API_URL).then((res) => res.json())
}
