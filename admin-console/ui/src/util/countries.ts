// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

export function formatCountry(country: string) {
  try {
    return COUNTRY_NAMES.of(country)
  } catch {
    if (country.toLowerCase() === 'aiida' || country === 'Unknown') {
      return 'AIIDA'
    }
    return country
  }
}

export function countryFlag(countryCode: string) {
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
