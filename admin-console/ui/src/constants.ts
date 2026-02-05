// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { RegionConnectorFeature } from '@/api'

export const ENERGY_TYPES = ['ELECTRICITY', 'NATURAL_GAS', 'HYDROGEN', 'HEAT']

export const GRANULARITIES = ['PT5M', 'PT10M', 'PT15M', 'PT30M', 'PT1H', 'P1D', 'P1M', 'P1Y']

export const ASSETS = [
  'CONNECTION-AGREEMENT-POINT',
  'CONTROLLABLE-UNIT',
  'DEDICATED-MEASUREMENT-DEVICE',
  'SUBMETER'
]

export const SCHEMAS = ['SMART-METER-P1-RAW', 'SMART-METER-P1-CIM']

export const REGION_CONNECTORS = [
  'aiida',
  'at-eda',
  'be-fluvius',
  'cds',
  'dk-energinet',
  'es-datadis',
  'fi-fingrid',
  'fr-enedis',
  'nl-mijn-aansluiting',
  'sim',
  'us-green-button'
]

export const ACTIVE_PERMISSION_STATES = ['ACCEPTED']

export const GRANTED_PERMISSION_STATES = ['ACCEPTED', 'FULFILLED']

export const FAILED_PERMISSION_STATES = [
  'MALFORMED',
  'UNABLE_TO_SEND',
  'TIMED_OUT',
  'UNFULFILLABLE',
  'FAILED_TO_TERMINATE'
]

export const DATA_NEEDS_DEFAULT_LINK =
  'https://architecture.eddie.energy/framework/2-integrating/data-needs.html'
export const FEATURES_DEFAULT_LINK =
  'https://architecture.eddie.energy/framework/2-integrating/messages/messages.html'

export const FEATURES: Record<RegionConnectorFeature, { text: string; link: string }> = {
  supportsConnectionStatusMessages: {
    text: 'Connection Status Messages',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/agnostic.html#connection-status-messages'
  },
  supportsRawDataMessages: {
    text: 'Raw Data Messages',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/agnostic.html#raw-data-messages'
  },
  supportsTermination: {
    text: 'Termination v0.82',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/permission-market-documents.html#termination-documents'
  },
  supportsAccountingPointMarketDocuments: {
    text: 'Accounting Point Market Documents v0.82',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/accounting-point-data-market-documents.html'
  },
  supportsPermissionMarketDocuments: {
    text: 'Permission Market Documents v0.82',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/permission-market-documents.html'
  },
  supportsValidatedHistoricalDataMarketDocuments: {
    text: 'Validated Historical Data Market Documents v0.82',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/validated-historical-data-market-documents.html'
  },
  supportsRetransmissionRequests: {
    text: 'Retransmission Requests v0.91.08',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/redistribution-transaction-request-documents.html'
  },
  supportsValidatedHistoricalDataMarketDocumentsV1_04: {
    text: 'Validated Historical Data Market Documents v1.04',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/validated-historical-data-market-documents.html'
  },
  supportsNearRealTimeDataMarketDocuments: {
    text: 'Near Real Time Data Market Documents v1.04',
    link: 'https://architecture.eddie.energy/framework/2-integrating/messages/cim/near-real-time-data-market-documents.html'
  }
}

export const DATA_NEEDS: Record<string, { text: string; link: string }> = {
  ValidatedHistoricalDataDataNeed: {
    text: 'Validated Historical Data',
    link: 'https://architecture.eddie.energy/framework/2-integrating/data-needs.html#validatedhistoricaldatadataneed'
  },
  AccountingPointDataNeed: {
    text: 'Accounting Point',
    link: 'https://architecture.eddie.energy/framework/2-integrating/data-needs.html#accountingpointdataneed'
  },
  OutboundAiidaDataNeed: {
    text: 'AIIDA Outbound',
    link: 'https://architecture.eddie.energy/framework/2-integrating/data-needs.html#aiidadataneed'
  },
  InboundAiidaDataNeed: {
    text: 'AIIDA Inbound',
    link: 'https://architecture.eddie.energy/framework/2-integrating/data-needs.html#aiidadataneed'
  }
}
