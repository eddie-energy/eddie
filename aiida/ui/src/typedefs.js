/**
 * @typedef {import("vue").Ref} Ref
 */

/**
 * @typedef AiidaDataNeed
 * @prop {string} dataNeedId
 * @prop {string} name
 * @prop {string} purpose
 * @prop {string} policyLink
 * @prop {string} transmissionSchedule
 * @prop {string[]} schemas
 * @prop {string} asset
 * @prop {string[]} dataTags
 */

/**
 * @typedef AiidaPermission
 * @prop {string} permissionId
 * @prop {string} eddieId
 * @prop {string} status
 * @prop {string} serviceName
 * @prop {string} startTime
 * @prop {string} expirationTime
 * @prop {string} grantTime
 * @prop {AiidaDataNeed} dataNeed
 * @prop {AiidaDataSource} dataSource
 * @prop {string} userId
 */

/**
 * @typedef AiidaMqttSettings
 * @prop {string} internalHost
 * @prop {string} externalHost
 * @prop {string} subscribeTopic
 * @prop {string} username
 * @prop {string} password
 */

/**
 * @typedef AiidaModbusSettings
 * @prop {string} modbusIp
 * @prop {string} modbusVendor
 * @prop {string} modbusModel
 * @prop {string} modbusDevice
 */

/**
 * @typedef AiidaDataSource
 * @prop {string} id
 * @prop {string} dataSourceType
 * @prop {string} asset
 * @prop {string} name
 * @prop {string} countryCode
 * @prop {boolean} enabled
 * @prop [number] simulationPeriod
 * @prop [AiidaMqttSettings] mqttSettings
 * @prop [AiidaModbusSettings] modbusSettings
 */

/**
 * @typedef AiidaDataSourceType
 * @prop {string} identifier
 * @prop {string} name
 */

/**
 * @typedef AiidaApplicationInformation
 * @prop {string} aiidaId
 */

/**
 * @typedef AiidaPermissionRequest
 * @prop {string} eddieId
 * @prop {string} permissionId
 * @prop {string} serviceName
 * @prop {string} handshakeUrl
 * @prop {string} accessToken
 */
