export function createJsonBlobUrl(data: unknown): string {
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json;charset=utf-8',
  })
  return URL.createObjectURL(blob)
}

export function revokeUrl(url: string) {
  URL.revokeObjectURL(url)
}
