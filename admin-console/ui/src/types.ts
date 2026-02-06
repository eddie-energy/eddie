// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

export type RegionConnectorFilter = {
  type: 'blocklist' | 'allowlist'
  regionConnectorIds: string[]
}

export type DataNeed = {
  id: string
  name: string
  description: string
  purpose: string
  policyLink: string
  createdAt: string
  enabled: boolean
  type: string
  regionConnectorFilter?: RegionConnectorFilter
}

export type TimeFramedDataNeed = DataNeed & {
  duration: DataNeedDuration
}

// Durations
export type AbsoluteDuration = {
  type: 'absoluteDuration'
  start: string // date
  end: string // date
}

export type RelativeDuration = {
  type: 'relativeDuration'
  start?: string // ISO8601 duration (e.g. -P3M)
  end?: string // ISO8601 duration (e.g. P1Y4M12D)
  stickyStartCalendarUnit?: 'WEEK' | 'MONTH' | 'YEAR'
}

export type DataNeedDuration = AbsoluteDuration | RelativeDuration

export type AccountingPointDataNeed = DataNeed & {
  type: 'account'
}

export type AiidaDataNeed = TimeFramedDataNeed & {
  transmissionSchedule: string // cron expression
  schemas: ('SMART-METER-P1-RAW' | 'SMART-METER-P1-CIM')[]
  asset:
    | 'CONNECTION-AGREEMENT-POINT'
    | 'CONTROLLABLE-UNIT'
    | 'DEDICATED-MEASUREMENT-DEVICE'
    | 'SUBMETER'
  dataTags?: string[]
}

export type InboundAiidaDataNeed = AiidaDataNeed & {
  type: 'inbound-aiida'
}

export type OutboundAiidaDataNeed = AiidaDataNeed & {
  type: 'outbound-aiida'
}

export type ValidatedHistoricalDataDataNeed = TimeFramedDataNeed & {
  type: 'validated'
  energyType: 'ELECTRICITY' | 'NATURAL_GAS' | 'HYDROGEN' | 'HEAT'
  minGranularity: 'PT5M' | 'PT10M' | 'PT15M' | 'PT30M' | 'PT1H' | 'P1D' | 'P1M' | 'P1Y'
  maxGranularity: 'PT5M' | 'PT10M' | 'PT15M' | 'PT30M' | 'PT1H' | 'P1D' | 'P1M' | 'P1Y'
}

export type AnyDataNeed =
  | AccountingPointDataNeed
  | InboundAiidaDataNeed
  | OutboundAiidaDataNeed
  | ValidatedHistoricalDataDataNeed

export type DataNeedType = AnyDataNeed['type']

export type PermissionStatus =
  | 'CREATED'
  | 'VALIDATED'
  | 'MALFORMED'
  | 'UNABLE_TO_SEND'
  | 'SENT_TO_PERMISSION_ADMINISTRATOR'
  | 'TIMED_OUT'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'INVALID'
  | 'REVOKED'
  | 'TERMINATED'
  | 'FULFILLED'
  | 'UNFULFILLABLE'
  | 'REQUIRES_EXTERNAL_TERMINATION'
  | 'FAILED_TO_TERMINATE'
  | 'EXTERNALLY_TERMINATED'
