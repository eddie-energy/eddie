// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
