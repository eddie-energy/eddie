// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

export type AiidaDataNeed = {
  dataNeedId: string
  name: string
  purpose: string
  policyLink: string
  transmissionSchedule: string
  schemas: string[]
  asset: string
  dataTags: string[]
  type: 'outbound-aiida' | 'inbound-aiida'
}

export type AiidaDataSourceIcon = 'ELECTRICITY' | 'HEAT' | 'METER' | 'WATER' | ''

export type AiidaDataSource = {
  id: string
  type: string
  name: string
  countryCode: string
  enabled: boolean
  icon: AiidaDataSourceIcon
  asset: string
  meterId?: string
  operatorId?: string
  //DatasourceType = INBOUND
  accessCode?: string
  //DatasourceType = MQTT
  internalHost?: string
  externalHost?: string
  topic?: string
  username?: string
  //DatasourceType = MODBUS
  ipAddress?: string
  vendorId?: string
  modelId?: string
  deviceId?: string
  //DatasourceType = SIMULATION
  pollingInterval?: number
  //DatasourceType = SINAPSI_ALFA
  activationKey?: string
}

export type AiidaPermission = {
  permissionId: string
  eddieId: string
  status: string
  serviceName: string
  startTime: string
  expirationTime: string
  grantTime?: string
  dataNeed: AiidaDataNeed
  dataSource?: AiidaDataSource
  mqttStreamingConfig?: AiidaPermissionStreamingConfig
  userId: string
  unimplemented: {
    packageGraph: any
    targetIP: any
    lastPackageSent: any
  }
}

export type AiidaPermissionStreamingConfig = {
  dataTopic: string
  serverUri: string
}

export type AiidaDataSourceType = {
  identifier: string
  name: string
}

export type AiidaApplicationInformation = {
  aiidaId: string
}

export type AiidaPermissionRequestsDTO = {
  eddieId: string
  permissionIds: string[]
  handshakeUrl: string
  accessToken: string
}

export type AiidaDataSourceHealthStatus = {
  status: string
  details?: {
    [key: string]: string
  }
}

export type PermissionTypes = 'Active' | 'Pending' | 'Complete'
export type ToastTypes = 'info' | 'success' | 'warning' | 'danger'
export type StatusTypes = 'healthy' | 'partially-healthy' | 'unhealthy' | 'unknown'
