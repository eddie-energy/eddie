// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

export function createJsonBlobUrl(data: unknown): string {
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json;charset=utf-8',
  })
  return URL.createObjectURL(blob)
}

export function revokeUrl(url: string) {
  URL.revokeObjectURL(url)
}

export async function getSvgUrlIfExists(svg: string) {
  const filePath = `/svgs/${svg}`
  try {
    const response = await fetch(filePath, { method: 'HEAD' })
    if (response.ok) {
      return `${THYMELEAF_AIIDA_PUBLIC_URL ?? import.meta.env.VITE_AIIDA_PUBLIC_URL}${filePath}`
    }
  } catch {
    return ""
  }
  return ""
}