/**
 * @typedef {Object} PermissionAdministrator
 * @property {string} country - The lowercase country code of the permission administrator.
 * @property {string} company - The full legal name of the company.
 * @property {string} name - The name of the permission administrator to be displayed to the user.
 * @property {string} companyId - The unique identifier of the company.
 * @property {string} [jumpOffUrl] - Optional URL to the permission administrator's portal.
 * @property {string} regionConnector - The unique identifier of the region connector handling permissions for the permission administrator.
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
 * @typedef {"CONNECTION_AGREEMENT_POINT", "CONTROLLABLE_UNIT", "DEDICATED_MEASUREMENT_DEVICE", "SUBMETER"} Asset
 */

/**
 * @typedef {DataNeed} AiidaDataNeed
 * @property {string} transmissionSchedule - The schedule at which data is transmitted.
 * @property {Asset} asset - The asset type.
 */

/**
 * @typedef {AiidaDataNeed} SmartMeterAiidaDataNeed
 */

/**
 * @typedef {AiidaDataNeed} GenericAiidaDataNeed
 * @property {string} transmissionSchedule - The schedule at which data is transmitted.
 * @property {Array<string>} dataTags - The tags associated with the data.
 */

/**
 * @typedef {ValidatedHistoricalDataDataNeed | SmartMeterAiidaDataNeed | GenericAiidaDataNeed | AccountingPointDataNeed} DataNeedAttributes
 */

/**
 * @typedef {Object} RegionConnectorMetadata
 * @property {string} id - The unique identifier of the region connector.
 * @property {string} timeZone - Time zone of the region covered by the region connector.
 * @property {string[]} countryCodes - Country codes of the regions covered by the region connector in uppercase.
 * @property {number} coveredMeteringPoints - Number of metering points that are accessible through the region connector.
 * @property {string} earliestStart - The earliest possible start for permissions as a string such as P6Y3M1D.
 * @property {string} latestEnd - The latest possible end for permissions as a string such as P6Y3M1D.
 * @property {string[]} supportedGranularities - List of supported granularities.
 */

/**
 * @typedef {Object} DataNeedCalculation
 * @property {boolean} supportsDataNeed - Indicates if the data need is supported.
 * @property {string} [unsupportedDataNeedMessage] - Message explaining why the data need is not supported.
 * @property {string[]} [granularities] - List of granularities supported by the data need.
 * @property {DataNeedCalculationTimeframe} [permissionTimeframe] - The timeframe for which permission is granted.
 * @property {DataNeedCalculationTimeframe} [energyDataTimeframe] - The timeframe for which energy data is available.
 */

/**
 * @typedef {Object} DataNeedCalculationTimeframe
 * @property {string} start - The start of the timeframe.
 * @property {string} end - The end of the timeframe.
 */
