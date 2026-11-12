// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import type { MeasurementPoint } from '@/types'

/** A time interval of measurements, summarized so dense series stay readable. */
export type MeasurementBucket = {
  /** ISO timestamp of the first instant of the interval. */
  start: string
  /** ISO timestamp after the last instant of the interval. */
  end: string
  /** ISO timestamp in the middle of the interval, used to place it on a time axis. */
  center: string
  lowestPowerKw: number
  highestPowerKw: number
  averagePowerKw: number
}

const SECOND = 1000
const MINUTE = 60 * SECOND
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

// Sizes a human would pick, so the shown intervals are round.
const BUCKET_SIZES = [
  5 * SECOND,
  10 * SECOND,
  15 * SECOND,
  30 * SECOND,
  MINUTE,
  2 * MINUTE,
  5 * MINUTE,
  10 * MINUTE,
  15 * MINUTE,
  30 * MINUTE,
  HOUR,
  2 * HOUR,
  3 * HOUR,
  6 * HOUR,
  12 * HOUR,
  DAY,
  2 * DAY,
  7 * DAY,
  14 * DAY,
  30 * DAY,
]

/**
 * Summarizes measurements into at most {@link maxBuckets} intervals of equal length.
 * The intervals are aligned to the epoch, so they do not shift when the time frame changes.
 * Expects the points to be ordered by timestamp.
 */
export function bucketMeasurements(
  points: MeasurementPoint[],
  maxBuckets: number,
): MeasurementBucket[] {
  if (points.length < 2) {
    return []
  }

  const span = Date.parse(points[points.length - 1].timestamp) - Date.parse(points[0].timestamp)
  const size = BUCKET_SIZES.find((candidate) => span / candidate <= maxBuckets) ?? 30 * DAY

  const buckets = new Map<number, { lowest: number; highest: number; sum: number; count: number }>()
  for (const { timestamp, powerKw } of points) {
    const start = Math.floor(Date.parse(timestamp) / size) * size
    const bucket = buckets.get(start)
    if (bucket === undefined) {
      buckets.set(start, { lowest: powerKw, highest: powerKw, sum: powerKw, count: 1 })
      continue
    }
    bucket.lowest = Math.min(bucket.lowest, powerKw)
    bucket.highest = Math.max(bucket.highest, powerKw)
    bucket.sum += powerKw
    bucket.count += 1
  }

  return [...buckets.entries()]
    .sort(([left], [right]) => left - right)
    .map(([start, { lowest, highest, sum, count }]) => ({
      start: new Date(start).toISOString(),
      end: new Date(start + size).toISOString(),
      center: new Date(start + size / 2).toISOString(),
      lowestPowerKw: lowest,
      highestPowerKw: highest,
      averagePowerKw: Math.round((sum / count) * 100) / 100,
    }))
}
