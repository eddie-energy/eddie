/**
 * @typedef {Object} PermissionAdministrator
 * @property {string} country
 * @property {string} company
 * @property {string} companyId
 * @property {string} jumpOffUrl
 * @property {string} regionConnector
 */

/**
 * @typedef {"account" | "validated" | "genericAiida" | "smartMeterAiida"} DataNeedType
 */

/**
 * @typedef {Object} DataNeed
 * @property {DataNeedType} type - Type of the data need.
 * @property {string} id - The unique identifier of the data need.
 * @property {string} name - The name of the data need.
 * @property {string} description - The description of the data need for internal use. It is not displayed to the user.
 * @property {string} purpose - The purpose and description of this data need that will be displayed to the user in the popup.
 * @property {string} policyLink - The URL to a document describing the data policy.
 * @property {boolean} enabled - If data need is enabled.
 */

/**
 * @typedef {DataNeed} AccountingPointDataNeed
 */

/**
 * @typedef {"relative" | "absolute"} DurationType
 */

/**
 * @typedef {Object} DataNeedDuration
 * @property {DurationType} type - Type of the duration.
 */

/**
 * @typedef {"WEEK" | "MONTH" | "YEAR"} CalendarUnit
 */

/**
 * @typedef {DataNeedDuration} RelativeDuration
 * @property {string} start - The start of the data need relative to the current date as an ISO 8601 duration string.
 * @property {string} end - The end date of the data need relative to the current date as an ISO 8601 duration string.
 * @property {CalendarUnit} stickyStartCalendarUnit - The calendar unit to which the start date is sticky.
 */

/**
 * @typedef {DataNeedDuration} AbsoluteDuration
 * @property {string} start - The start date of the data need.
 * @property {string} end - The end date of the data need.
 */

/**
 * @typedef {DataNeed} TimeframedDataNeed
 * @property {DataNeedDuration} duration - The duration of the data need.
 */

/**
 * @typedef {"ELECTRICITY" | "NATURAL_GAS" | "HYDROGEN" | "HEAT"} EnergyType
 */

/**
 * @typedef {TimeframedDataNeed} ValidatedHistoricalDataDataNeed
 * @property {EnergyType} energyType - The type of energy for the data need.
 * @property {string} minGranularity - The minimum granularity of the data.
 * @property {string} maxGranularity - The maximum granularity of the data.
 */

/**
 * @typedef {DataNeed} AiidaDataNeed
 * @property {number} transmissionInterval - The interval at which data is transmitted.
 */

/**
 * @typedef {AiidaDataNeed} SmartMeterAiidaDataNeed
 */

/**
 * @typedef {AiidaDataNeed} GenericAiidaDataNeed
 * @property {number} transmissionInterval - The interval at which data is transmitted.
 * @property {Array<string>} dataTags - The tags associated with the data.
 */

/**
 * @typedef {ValidatedHistoricalDataDataNeed | SmartMeterAiidaDataNeed | GenericAiidaDataNeed | AccountingPointDataNeed} DataNeedAttributes
 */
