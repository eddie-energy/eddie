const COUNTRY_NAMES = new Intl.DisplayNames(['en'], { type: 'region' })

export function formatCountry(country: string) {
  try {
    return COUNTRY_NAMES.of(country)
  } catch {
    return country
  }
}

export function countryFlag(countryCode: string) {
  if (countryCode === 'aiida') {
    return '🤖'
  }
  // check if result is in right range
  if (countryCode.length !== 2) {
    return ''
  }
  return [...countryCode]
    .map((char) => String.fromCodePoint(127397 + char.charCodeAt(0)))
    .reduce((a, b) => `${a}${b}`)
}
