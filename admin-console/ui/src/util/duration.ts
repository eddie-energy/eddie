import { parse } from 'tinyduration'

export function formatDuration(durationStr: string) {
  const duration = parse(durationStr)
  const parts = new Array<string>()

  delete duration.negative

  for (const [key, value] of Object.entries(duration)) {
    if (value) {
      parts.push(
        value > 0 ? 'plus' : 'minus',
        String(Math.abs(value)),
        Math.abs(value) === 1 ? key.slice(0, -1) : key
      )
    }
  }

  const start = parts.shift()
  const result = parts.join(' ')

  switch (start) {
    case 'plus':
      return result + ' in the future'
    case 'minus':
      return result + ' in the past'
    default:
      return 'Now'
  }
}
