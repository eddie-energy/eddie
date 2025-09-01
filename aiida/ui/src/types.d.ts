import type ConfirmDialog from "./components/ConfirmDialog.vue"

export type AiidaDataNeed = {
  dataNeedId: string
  name: string
  purpose: string
  policyLink: string
  transmissionSchedule: string
  schemas: string[]
  asset: string
  dataTags: string[]
  type: string
}

export type AiidaMqttSettings = {
  internalHost: string
  externalHost: string
  subscribeTopic: string
  username: string
  password: string
}

export type AiidaModbusSettings = {
  modbusIp: string
  modbusVendor: string
  modbusModel: string
  modbusDevice: string
}

export type AiidaDataSource = {
  id: string
  dataSourceType: string
  asset: string
  name: string
  countryCode: string
  enabled: boolean
  simulationPeriod?: number
  mqttSettings?: AiidaMqttSettings
  modbusSettings?: AiidaModbusSettings
}

export type AiidaPermission = {
  permissionId: string
  eddieId: string
  status: string
  serviceName: string
  startTime: string
  expirationTime: string
  grantTime: string
  dataNeed: AiidaDataNeed
  dataSource: AiidaDataSource
  userId: string
}

export type AiidaDataSourceType = {
  identifier: string
  name: string
}

export type AiidaApplicationInformation = {
  aiidaId: string
}

export type AiidaPermissionRequest = {
  eddieId: string
  permissionId: string
  serviceName: string
  handshakeUrl: string
  accessToken: string
}