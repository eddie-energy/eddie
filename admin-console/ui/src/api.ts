import { DATA_NEEDS_API_URL, PERMISSIONS_API_URL, REGION_CONNECTOR_API_URL } from '@/config'

export async function getPermissions() {
  return await fetch(PERMISSIONS_API_URL).then(res => res.json());
}

export async function getDataNeeds() {
  return await fetch(DATA_NEEDS_API_URL).then(res => res.json());
}

export async function getRegionConnectors() {
  return await fetch(REGION_CONNECTOR_API_URL).then(res => res.json());
}