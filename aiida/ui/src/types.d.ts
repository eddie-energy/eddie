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
  dataSourceType: string
  asset: string
  name: string
  countryCode: string
  enabled: boolean
  icon: AiidaDataSourceIcon
  accessCode?: string
  mqttInternalHost?: string
  mqttExternalHost?: string
  mqttSubscribeTopic?: string
  mqttUsername?: string
  //DatasourceType = MODBUS
  modbusIp?: string
  modbusVendor?: string
  modbusModel?: string
  modbusDevice?: string
  //DatasourceType = SIMULATION
  simulationPeriod?: number
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
  userId: string
  unimplemented: {
    packageGraph: any
    targetIP: any
    lastPackageSent: any
  }
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
  bearerToken: string
}

export type PermissionTypes = 'Active' | 'Pending' | 'Complete'
export type ToastTypes = 'info' | 'success' | 'warning' | 'danger'
