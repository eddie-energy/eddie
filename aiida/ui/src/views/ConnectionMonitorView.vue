<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Chart from 'chart.js/auto'
import type { ChartDataset, ChartOptions, Plugin, TooltipItem } from 'chart.js'
import 'chartjs-adapter-date-fns'
import { getConnectionLimits, getConnectionLimitViolations, getConnectionMeasurements } from '@/api'
import { fetchPermissions, isFcaPermission, monitorablePermissions } from '@/stores/permissions'
import { bucketMeasurements, type MeasurementBucket } from '@/utils/measurementBuckets'
import CustomSelect from '@/components/CustomSelect.vue'
import Button from '@/components/Button.vue'
import type { ConnectionLimit, ConnectionLimitViolation, MeasurementPoint } from '@/types'
import ContextBadge from '@/components/ContextBadge.vue'

const DAY_IN_MS = 24 * 60 * 60 * 1000
const VIOLATION_COLOR = '#b3261e'
const VIOLATION_FILL = 'rgba(179, 38, 30, 0.18)'
const MEASUREMENT_COLOR = '#4e4e4e'
const MEASUREMENT_SPREAD_COLOR = 'rgba(78, 78, 78, 0.45)'
const NOW_COLOR = 'rgba(78, 78, 78, 0.7)'
// Above this many readings the series collapses into an unreadable band of points, so it is summarized.
const MAX_RAW_MEASUREMENTS = 600
const MAX_BUCKETS = 300

const nowMarker: Plugin<'line'> = {
  id: 'nowMarker',
  afterDatasetsDraw(chart) {
    const { ctx, chartArea, scales } = chart
    const xScale = scales.x
    if (!xScale) return

    const x = xScale.getPixelForValue(Date.now())
    if (x < chartArea.left || x > chartArea.right) return

    ctx.save()
    ctx.strokeStyle = NOW_COLOR
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(x, chartArea.top)
    ctx.lineTo(x, chartArea.bottom)
    ctx.stroke()
    ctx.restore()
  },
}

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const timeFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

const permissionId = ref('')
// Defaults to one day ago and one day ahead, so upcoming limits are visible as well.
const now = Date.now()
const from = ref(new Date(now - DAY_IN_MS))
const to = ref(new Date(now + DAY_IN_MS))
const fromInput = ref(toLocalInput(from.value))
const toInput = ref(toLocalInput(to.value))
const limits = ref<ConnectionLimit[]>([])
const violations = ref<ConnectionLimitViolation[]>([])
const measurements = ref<MeasurementPoint[]>([])
const loading = ref(false)

// Dense series are summarized, so they do not collapse into an unreadable band of points.
const measurementBuckets = computed(() =>
  measurements.value.length > MAX_RAW_MEASUREMENTS
    ? bucketMeasurements(measurements.value, MAX_BUCKETS)
    : [],
)

// Draws the spread of each bucket as a vertical line from its lowest to its highest power.
const measurementSpread: Plugin<'line'> = {
  id: 'measurementSpread',
  beforeDatasetsDraw(chart) {
    const buckets = measurementBuckets.value
    if (buckets.length === 0) return

    const { ctx, chartArea } = chart
    const xScale = chart.scales.x
    const yScale = chart.scales.y
    if (!xScale || !yScale) return

    ctx.save()
    ctx.beginPath()
    ctx.rect(chartArea.left, chartArea.top, chartArea.width, chartArea.height)
    ctx.clip()
    ctx.strokeStyle = MEASUREMENT_SPREAD_COLOR
    ctx.lineWidth = 1
    ctx.beginPath()
    for (const bucket of buckets) {
      const x = xScale.getPixelForValue(Date.parse(bucket.center))
      ctx.moveTo(x, yScale.getPixelForValue(bucket.highestPowerKw))
      ctx.lineTo(x, yScale.getPixelForValue(bucket.lowestPowerKw))
    }
    ctx.stroke()
    ctx.restore()
  },
}

type Point = {
  x: string
  y: number | null
  violation?: ConnectionLimitViolation
  bucket?: MeasurementBucket
}

const minLimitPoints = ref<Point[]>([])
const maxLimitPoints = ref<Point[]>([])
const minViolationPoints = ref<Point[]>([])
const maxViolationPoints = ref<Point[]>([])

const datasets = computed<ChartDataset<'line', Point[]>[]>(() => [
  {
    label: t('connectionMonitor.minLimit'),
    data: minLimitPoints.value,
    borderColor: '#017aa0',
    borderDash: [6, 6],
    borderWidth: 2,
    pointRadius: 0,
    fill: 1,
    backgroundColor: 'rgba(1, 122, 160, 0.12)',
  },
  {
    label: t('connectionMonitor.maxLimit'),
    data: maxLimitPoints.value,
    borderColor: '#b3261e',
    borderDash: [6, 6],
    borderWidth: 2,
    pointRadius: 0,
    fill: false,
  },
  {
    label: t('connectionMonitor.violations.belowMinimum'),
    data: minViolationPoints.value,
    borderColor: VIOLATION_COLOR,
    backgroundColor: VIOLATION_FILL,
    borderWidth: 4,
    pointRadius: 0,
    pointHoverRadius: 4,
    pointHoverBackgroundColor: VIOLATION_COLOR,
    fill: 0,
  },
  {
    label: t('connectionMonitor.violations.aboveMaximum'),
    data: maxViolationPoints.value,
    borderColor: VIOLATION_COLOR,
    backgroundColor: VIOLATION_FILL,
    borderWidth: 4,
    pointRadius: 0,
    pointHoverRadius: 4,
    pointHoverBackgroundColor: VIOLATION_COLOR,
    fill: 1,
  },
  {
    label: t('connectionMonitor.measuredPower'),
    data: measurementBuckets.value.length
      ? measurementBuckets.value.map((bucket) => ({
          x: bucket.center,
          y: bucket.averagePowerKw,
          bucket,
        }))
      : measurements.value.map(({ powerKw, timestamp }) => ({
          x: timestamp,
          y: powerKw,
        })),
    // A bucket summarizes an interval, so its average is connected. Single readings are not.
    showLine: measurementBuckets.value.length > 0,
    borderColor: MEASUREMENT_COLOR,
    backgroundColor: MEASUREMENT_COLOR,
    borderWidth: 1,
    pointRadius: measurementBuckets.value.length ? 0 : 1.5,
    pointHoverRadius: 4,
    pointHitRadius: 8,
    fill: false,
  },
])

watch(limits, () => {
  minLimitPoints.value = []
  maxLimitPoints.value = []
  for (const limit of limits.value) {
    minLimitPoints.value.push(
      { x: limit.intervalStart, y: limit.minLimitKw },
      { x: limit.intervalEnd, y: limit.minLimitKw },
    )

    maxLimitPoints.value.push(
      { x: limit.intervalStart, y: limit.maxLimitKw },
      { x: limit.intervalEnd, y: limit.maxLimitKw },
    )
  }
})

watch(violations, () => {
  minViolationPoints.value = []
  maxViolationPoints.value = []
  // An ongoing violation reaches up to now, not to the end of the shown time frame.
  const ongoingEnd = new Date().toISOString()
  for (const violation of violations.value) {
    const start = violation.startedAt
    const end = violation.endedAt ?? ongoingEnd

    // A violation can exceed the maximum and fall below the minimum, so both are drawn independently.
    // The null point ends each segment, so two violations are never connected.
    if (violation.maxLimitKw != null && violation.highestPowerKw > violation.maxLimitKw) {
      maxViolationPoints.value.push(
        { x: start, y: violation.highestPowerKw, violation },
        { x: end, y: violation.highestPowerKw, violation },
        { x: end, y: null },
      )
    }
    if (violation.minLimitKw != null && violation.lowestPowerKw < violation.minLimitKw) {
      minViolationPoints.value.push(
        { x: start, y: violation.lowestPowerKw, violation },
        { x: end, y: violation.lowestPowerKw, violation },
        { x: end, y: null },
      )
    }
  }
})

const chartCanvas = useTemplateRef<HTMLCanvasElement>('chartCanvas')
let chartInstance: Chart<'line', Point[]> | undefined

const selectedPermission = computed(() =>
  monitorablePermissions.value.find((p) => p.permissionId === permissionId.value),
)

const newestViolationsFirst = computed(() => [...violations.value].reverse())

const permissionOptions = computed(() =>
  monitorablePermissions.value.map((permission) => ({
    label: permission.displayName,
    value: permission.permissionId,
  })),
)

function toLocalInput(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function applyRange() {
  const fromDate = new Date(fromInput.value)
  const toDate = new Date(toInput.value)
  if (Number.isNaN(fromDate.getTime()) || Number.isNaN(toDate.getTime())) {
    return
  }
  from.value = fromDate
  to.value = toDate
}

const chartOptions = computed<ChartOptions<'line'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'nearest', intersect: false },
  scales: {
    x: {
      type: 'time',
      min: from.value.getTime(),
      max: to.value.getTime(),
      ticks: { autoSkip: true, maxRotation: 0 },
      time: {
        displayFormats: {
          millisecond: 'HH:mm:ss',
          second: 'HH:mm:ss',
          minute: 'HH:mm',
          hour: 'dd.MM, HH:mm',
          day: 'dd.MM',
          week: 'dd.MM',
          month: 'MM.yyyy',
          quarter: 'MM.yyyy',
          year: 'yyyy',
        },
      },
    },
    y: { title: { display: true, text: 'kW' } },
  },
  plugins: {
    legend: { position: 'bottom' },
    tooltip: {
      callbacks: {
        title: tooltipTitle,
        label: tooltipLine,
      },
    },
  },
}))

function limitText({ minLimitKw, maxLimitKw }: ConnectionLimitViolation) {
  const bounds = [
    minLimitKw == null ? null : `≥ ${minLimitKw}`,
    maxLimitKw == null ? null : `≤ ${maxLimitKw}`,
  ]
  return `${bounds.filter(Boolean).join(' · ')} kW`
}

function violationOf(raw: unknown): ConnectionLimitViolation | undefined {
  return (raw as Point | undefined)?.violation
}

function bucketOf(raw: unknown): MeasurementBucket | undefined {
  return (raw as Point | undefined)?.bucket
}

function tooltipTitle(items: TooltipItem<'line'>[]): string {
  const violation = violationOf(items[0]?.raw)
  if (violation) {
    const start = timeFormat.format(new Date(violation.startedAt))
    const end = violation.endedAt
      ? timeFormat.format(new Date(violation.endedAt))
      : t('connectionMonitor.violations.ongoing')
    return `${start} – ${end}`
  }
  const bucket = bucketOf(items[0]?.raw)
  if (bucket) {
    return `${timeFormat.format(new Date(bucket.start))} – ${timeFormat.format(new Date(bucket.end))}`
  }
  const timestamp = items[0]?.parsed.x
  return timestamp == null ? '' : timeFormat.format(new Date(timestamp))
}

function tooltipLine(item: TooltipItem<'line'>): string | string[] {
  const violation = violationOf(item.raw)
  if (violation) {
    return [
      `${t('connectionMonitor.violations.limits')}: ${limitText(violation)}`,
      `${t('connectionMonitor.violations.startPower')}: ${violation.startPowerKw} kW`,
      `${t('connectionMonitor.violations.lowestPower')}: ${violation.lowestPowerKw} kW`,
      `${t('connectionMonitor.violations.highestPower')}: ${violation.highestPowerKw} kW`,
    ]
  }
  const bucket = bucketOf(item.raw)
  if (bucket) {
    return [
      `${t('connectionMonitor.measurement.lowest')}: ${bucket.lowestPowerKw} kW`,
      `${t('connectionMonitor.measurement.highest')}: ${bucket.highestPowerKw} kW`,
      `${t('connectionMonitor.measurement.average')}: ${bucket.averagePowerKw} kW`,
    ]
  }
  return `${item.dataset.label ?? ''}: ${item.parsed.y} kW`
}

function syncChart() {
  const canvas = chartCanvas.value
  if (!canvas) return
  // Vue replaces the canvas when the permission changes, so a chart on another canvas is stale.
  if (chartInstance?.canvas !== canvas) {
    chartInstance?.destroy()
    chartInstance = new Chart(canvas, {
      type: 'line',
      data: { datasets: datasets.value },
      options: chartOptions.value,
      plugins: [measurementSpread, nowMarker],
    })
    return
  }
  chartInstance.data.datasets = datasets.value
  chartInstance.options = chartOptions.value
  chartInstance.update()
}

watch([datasets, chartOptions], syncChart)

async function reload() {
  if (!permissionId.value) return
  loading.value = true
  try {
    const fromIso = from.value.toISOString()
    const toIso = to.value.toISOString()
    const [limitResult, violationResult, measurementResult] = await Promise.all([
      getConnectionLimits(permissionId.value, fromIso, toIso),
      getConnectionLimitViolations(permissionId.value, fromIso, toIso),
      getConnectionMeasurements(permissionId.value, fromIso, toIso),
    ])
    limits.value = limitResult
    violations.value = violationResult
    measurements.value = measurementResult
  } catch {
    limits.value = []
    violations.value = []
    measurements.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchPermissions()
  const queryPermission = route.query.permission
  permissionId.value =
    typeof queryPermission === 'string' &&
    monitorablePermissions.value.some((p) => p.permissionId === queryPermission)
      ? queryPermission
      : (monitorablePermissions.value[0]?.permissionId ?? '')
})

watch([permissionId, from, to], () => {
  if (permissionId.value) {
    void reload()
  }
})

watch(permissionId, (id) => {
  void router.replace({ query: id ? { permission: id } : {} })
})

onBeforeUnmount(() => {
  chartInstance?.destroy()
  chartInstance = undefined
})
</script>

<template>
  <main>
    <header class="page-header">
      <h1 class="heading-2">{{ t('connectionMonitor.title') }}</h1>
    </header>

    <div v-if="monitorablePermissions.length === 0" class="empty-state">
      <p>{{ t('connectionMonitor.noMonitorablePermissions') }}</p>
    </div>

    <template v-else>
      <div>
        <div class="controls">
          <div class="control">
            <label>{{ t('connectionMonitor.permissionLabel') }}</label>
            <CustomSelect
              v-model="permissionId"
              :options="permissionOptions"
              :placeholder="t('connectionMonitor.permissionPlaceholder')"
            />
          </div>
          <div class="control">
            <div class="time-range">
              <label>
                {{ t('connectionMonitor.from') }}
                <input v-model="fromInput" type="datetime-local" />
              </label>
              <label>
                {{ t('connectionMonitor.to') }}
                <input v-model="toInput" type="datetime-local" />
              </label>
              <Button button-style="secondary" @click="applyRange">
                {{ t('connectionMonitor.apply') }}
              </Button>
            </div>
          </div>
        </div>

        <div v-if="selectedPermission" class="chart-card">
          <h2 class="chart-title">
            {{ selectedPermission.displayName }}
            <ContextBadge v-if="isFcaPermission(selectedPermission)">FCA</ContextBadge>
          </h2>
          <dl class="chart-header">
            <dd>Permission ID:</dd>
            <dt>{{ selectedPermission.permissionId }}</dt>
            <template v-if="selectedPermission.meterId">
              <dd>Meter ID:</dd>
              <dt>{{ selectedPermission.meterId }}</dt>
            </template>
          </dl>
          <div class="chart-container">
            <canvas ref="chartCanvas"></canvas>
            <div v-if="loading" class="chart-overlay">{{ t('connectionMonitor.loading') }}</div>
            <div
              v-else-if="
                limits.length === 0 && measurements.length === 0 && violations.length === 0
              "
              class="chart-overlay"
            >
              {{ t('connectionMonitor.noData') }}
            </div>
          </div>
          <small v-if="measurements.length > 0" class="retention-note">
            {{ t('connectionMonitor.retentionNote') }}
          </small>
        </div>

        <div v-if="selectedPermission" class="chart-card violations">
          <h2 class="chart-title">{{ t('connectionMonitor.violations.title') }}</h2>
          <div v-if="violations.length" class="violations-table-wrapper">
            <table class="violations-table">
              <thead>
                <tr>
                  <th scope="col">{{ t('connectionMonitor.violations.start') }}</th>
                  <th scope="col">{{ t('connectionMonitor.violations.end') }}</th>
                  <th scope="col">{{ t('connectionMonitor.violations.limits') }}</th>
                  <th scope="col">{{ t('connectionMonitor.violations.startPower') }}</th>
                  <th scope="col">{{ t('connectionMonitor.violations.lowestPower') }}</th>
                  <th scope="col">{{ t('connectionMonitor.violations.highestPower') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="violation in newestViolationsFirst" :key="violation.startedAt">
                  <td>{{ timeFormat.format(new Date(violation.startedAt)) }}</td>
                  <td>
                    {{
                      violation.endedAt
                        ? timeFormat.format(new Date(violation.endedAt))
                        : t('connectionMonitor.violations.ongoing')
                    }}
                  </td>
                  <td>{{ limitText(violation) }}</td>
                  <td>{{ violation.startPowerKw }} kW</td>
                  <td>{{ violation.lowestPowerKw }} kW</td>
                  <td>{{ violation.highestPowerKw }} kW</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else-if="!loading" class="violations-hint">
            {{
              selectedPermission.monitoringDataSourceId
                ? t('connectionMonitor.violations.none')
                : t('connectionMonitor.violations.notMonitored')
            }}
          </p>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.page-header {
  margin-bottom: var(--spacing-xxl);
}

.controls {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xxl);
}

.control {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);

  label {
    font-weight: var(--font-weight-semibold);
  }
}

.time-range {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--spacing-sm);

  label {
    display: flex;
    flex-direction: column;
    gap: var(--spacing-xs);
  }

  input {
    padding: var(--spacing-sm) var(--spacing-md);
    border: 1px solid var(--eddie-grey-medium);
    border-radius: var(--border-radius);
    font-size: 1rem;
  }
}

.chart-card {
  padding: var(--spacing-lg);
  border: 1px solid var(--eddie-grey-medium);
  border-radius: var(--border-radius);
  background-color: var(--light);
}

.chart-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
  font-weight: var(--font-weight-semibold);
}

.chart-header {
  display: grid;
  grid-template-columns: auto 1fr;
  column-gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.chart-container {
  position: relative;
  height: 380px;
}

.retention-note {
  display: block;
  margin-top: var(--spacing-md);
  color: var(--eddie-grey-medium);
}

.violations {
  margin-top: var(--spacing-xxl);
}

.violations-table-wrapper {
  overflow-x: auto;
}

.violations-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;

  th,
  td {
    padding: var(--spacing-sm) var(--spacing-md);
    white-space: nowrap;
  }

  th {
    font-weight: var(--font-weight-semibold);
    border-bottom: 1px solid var(--eddie-grey-medium);
  }

  tbody tr:nth-child(even) {
    background-color: var(--eddie-secondary);
  }
}

.violations-hint {
  color: var(--eddie-grey-medium);
}

.chart-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--eddie-grey-medium);
  background-color: rgb(255, 255, 255, 0.7);
  text-align: center;
  z-index: 1;
}

.empty-state {
  padding: var(--spacing-lg);
  color: var(--eddie-grey-medium);
}

@media screen and (min-width: 1024px) {
  .controls {
    flex-direction: row;
    align-items: flex-start;
    gap: var(--spacing-xxl);
  }
}
</style>
