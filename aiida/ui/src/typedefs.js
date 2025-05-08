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
 * @typedef AiidaDataSource
 * @prop {string} id
 * @prop {string} dataSourceType
 * @prop {string} asset
 * @prop {string} name
 * @prop {boolean} enabled
 * @prop {number} simulationPeriod
 * @prop {AiidaMqttSettings} mqttSettings
 */
