// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

export function formatCountry(countryCode?: string) {
  if (!countryCode) {
    return 'Global'
  }

  try {
    return COUNTRY_NAMES.of(countryCode)
  } catch {
    if (countryCode.toLowerCase() === 'aiida' || countryCode === 'Unknown') {
      return 'AIIDA'
    }
    return countryCode
  }
}

export function countryFlag(countryCode?: string) {
  if (!countryCode) {
    return '🌍'
  }

  if (countryCode === 'aiida' || countryCode === 'Unknown') {
    return '🤖'
  }
  // check if result is in right range
  if (countryCode.length !== 2) {
    return ''
  }
  return [...countryCode]
    .map((char) => String.fromCodePoint(127397 + char.charCodeAt(0)))
    .reduce((a, b) => `${a}${b}`, '')
}
