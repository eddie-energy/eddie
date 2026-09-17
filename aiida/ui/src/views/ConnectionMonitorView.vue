<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Chart from 'chart.js/auto'
import type { ChartDataset, ChartOptions } from 'chart.js'
import { getConnectionLimits, getConnectionMeasurements } from '@/api'
import { fetchPermissions, isFcaPermission, monitorablePermissions } from '@/stores/permissions'
import CustomSelect from '@/components/CustomSelect.vue'
import Button from '@/components/Button.vue'
import type { ConnectionLimit, MeasurementPoint } from '@/types'

const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

type TimeRange = '24h' | '7d' | 'custom'
const rangeOptions: TimeRange[] = ['24h', '7d', 'custom']

const timeFormat = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

const permissionId = ref('')
const range = ref<TimeRange>('24h')
const customFrom = ref('')
const customTo = ref('')
const from = ref<Date>()
const to = ref<Date>()
const limits = ref<ConnectionLimit[]>([])
const measurements = ref<MeasurementPoint[]>([])
const loading = ref(false)

const chartCanvas = useTemplateRef<HTMLCanvasElement>('chartCanvas')
let chartInstance: Chart | undefined

const sortedMonitorable = computed(() =>
  [...monitorablePermissions.value].sort(
    (a, b) => Number(isFcaPermission(b)) - Number(isFcaPermission(a)),
  ),
)

const selectedPermission = computed(() =>
  monitorablePermissions.value.find((p) => p.permissionId === permissionId.value),
)

const permissionOptions = computed(() =>
  sortedMonitorable.value.map((permission) => ({
    label: [permission.displayName, isFcaPermission(permission) ? 'FCA' : null, permission.meterId]
      .filter((part): part is string => !!part)
      .join(' · '),
    value: permission.permissionId,
  })),
)

const tickStep = computed(() => {
  if (!from.value || !to.value) return HOUR
  const span = to.value.getTime() - from.value.getTime()
  if (span <= 2 * DAY) return HOUR
  if (span <= 14 * DAY) return 6 * HOUR
  return DAY
})

function limitPoints(value: (limit: ConnectionLimit) => number | null) {
  return limits.value.flatMap((limit) => [
    { x: Date.parse(limit.intervalStart), y: value(limit) },
    { x: Date.parse(limit.intervalEnd), y: value(limit) },
  ])
}

const datasets = computed<ChartDataset<'line', { x: number; y: number | null }[]>[]>(() => [
  {
    label: t('connectionMonitor.maxLimit'),
    data: limitPoints((limit) => limit.maxLimitKw),
    borderColor: '#b3261e',
    borderDash: [6, 6],
    borderWidth: 2,
    pointRadius: 0,
    fill: false,
  },
  {
    label: t('connectionMonitor.minLimit'),
    data: limitPoints((limit) => limit.minLimitKw),
    borderColor: '#017aa0',
    borderDash: [6, 6],
    borderWidth: 2,
    pointRadius: 0,
    fill: '-1',
    backgroundColor: 'rgba(1, 122, 160, 0.12)',
  },
  {
    // Lower bound of the measured band; only defines the fill area and is filtered from the legend.
    label: '',
    data: measurements.value.map((point) => ({
      x: Date.parse(point.timestamp),
      y: point.minPowerKw,
    })),
    borderColor: '#4e4e4e',
    borderWidth: 1,
    pointRadius: 0,
    fill: false,
  },
  {
    label: t('connectionMonitor.measuredPower'),
    data: measurements.value.map((point) => ({
      x: Date.parse(point.timestamp),
      y: point.maxPowerKw,
    })),
    borderColor: '#4e4e4e',
    backgroundColor: 'rgba(78, 78, 78, 0.18)',
    borderWidth: 1,
    pointRadius: 0,
    fill: '-1',
  },
])

const chartOptions = computed<ChartOptions<'line'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  parsing: false,
  interaction: { mode: 'nearest', intersect: false },
  scales: {
    x: {
      type: 'linear',
      min: from.value
        ? Math.floor(from.value.getTime() / tickStep.value) * tickStep.value
        : undefined,
      max: to.value?.getTime(),
      ticks: {
        stepSize: tickStep.value,
        autoSkip: true,
        maxRotation: 0,
        callback: (value) => timeFormat.format(new Date(Number(value))),
      },
    },
    y: { title: { display: true, text: t('connectionMonitor.kw') } },
  },
  plugins: {
    legend: {
      position: 'bottom',
      labels: { filter: (item) => item.text !== '' },
    },
    tooltip: {
      callbacks: {
        title: (items) => {
          const timestamp = items[0]?.parsed.x
          return timestamp == null ? '' : timeFormat.format(new Date(timestamp))
        },
      },
    },
  },
}))

const showEmptyMessage = computed(
  () => !loading.value && limits.value.length === 0 && measurements.value.length === 0,
)

function toLocalInput(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function applyRange() {
  if (range.value === 'custom') {
    const customFromDate = customFrom.value ? new Date(customFrom.value) : undefined
    const customToDate = customTo.value ? new Date(customTo.value) : undefined
    if (
      customFromDate &&
      customToDate &&
      !Number.isNaN(customFromDate.getTime()) &&
      !Number.isNaN(customToDate.getTime())
    ) {
      from.value = customFromDate
      to.value = customToDate
    }
    return
  }
  const now = new Date()
  const days = range.value === '24h' ? 1 : 7
  from.value = new Date(now.getTime() - days * DAY)
  to.value = now
}

function selectRange(value: TimeRange) {
  range.value = value
  if (value === 'custom') {
    customFrom.value = toLocalInput(from.value ?? new Date(Date.now() - DAY))
    customTo.value = toLocalInput(to.value ?? new Date())
  }
  applyRange()
}

function renderChart() {
  const canvasElement = chartCanvas.value
  if (!canvasElement) return
  if (!chartInstance) {
    chartInstance = new Chart(canvasElement, {
      type: 'line',
      data: { datasets: datasets.value },
      options: chartOptions.value,
    })
  } else {
    chartInstance.data.datasets = datasets.value
    chartInstance.options = chartOptions.value
    chartInstance.update()
  }
}

async function reload() {
  if (!permissionId.value || !from.value || !to.value) return
  loading.value = true
  try {
    const fromIso = from.value.toISOString()
    const toIso = to.value.toISOString()
    const [limitResult, measurementResult] = await Promise.all([
      getConnectionLimits(permissionId.value, fromIso, toIso),
      getConnectionMeasurements(permissionId.value, fromIso, toIso),
    ])
    limits.value = limitResult
    measurements.value = measurementResult
    await nextTick()
    renderChart()
  } catch {
    limits.value = []
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
      : (sortedMonitorable.value[0]?.permissionId ?? '')
  selectRange(range.value)
})

watch([permissionId, from, to], () => {
  if (permissionId.value && from.value && to.value) {
    void reload()
  }
})

watch(permissionId, (id) => {
  void router.replace({ query: id ? { permission: id } : {} })
})

watch(selectedPermission, (permission) => {
  if (!permission) {
    chartInstance?.destroy()
    chartInstance = undefined
  }
})

watch(locale, () => {
  if (chartInstance) {
    chartInstance.data.datasets = datasets.value
    chartInstance.options = chartOptions.value
    chartInstance.update()
  }
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
            <label>{{ t('connectionMonitor.timeRangeLabel') }}</label>
            <div class="range-tabs">
              <button
                v-for="option in rangeOptions"
                :key="option"
                type="button"
                class="range-tab"
                :class="{ active: range === option }"
                @click="selectRange(option)"
              >
                {{ t(`connectionMonitor.ranges.${option}`) }}
              </button>
            </div>
          </div>
          <div v-if="range === 'custom'" class="control custom-range">
            <label>
              {{ t('connectionMonitor.from') }}
              <input v-model="customFrom" type="datetime-local" />
            </label>
            <label>
              {{ t('connectionMonitor.to') }}
              <input v-model="customTo" type="datetime-local" />
            </label>
            <Button button-style="secondary" @click="applyRange">
              {{ t('connectionMonitor.apply') }}
            </Button>
          </div>
        </div>

        <div v-if="selectedPermission" class="chart-card">
          <h2 class="chart-title">
            {{ selectedPermission.displayName }}
            <span v-if="isFcaPermission(selectedPermission)">· FCA</span>
            <span v-if="selectedPermission.meterId">· {{ selectedPermission.meterId }}</span>
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
            <div v-else-if="showEmptyMessage" class="chart-overlay">
              {{ t('connectionMonitor.noData') }}
            </div>
          </div>
          <small v-if="measurements.length > 0" class="retention-note">
            {{ t('connectionMonitor.retentionNote') }}
          </small>
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

.range-tabs {
  display: flex;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
}

.range-tab {
  padding: var(--spacing-sm) var(--spacing-lg);
  border: 1px solid var(--eddie-primary);
  border-radius: 2rem;
  background-color: var(--light);
  color: var(--eddie-primary);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.3s ease-in-out,
    color 0.3s ease-in-out;

  &.active {
    background-color: var(--eddie-primary);
    color: var(--light);
  }
}

.custom-range {
  flex-direction: row;
  flex-wrap: wrap;
  align-items: flex-end;

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
  margin-bottom: var(--spacing-md);
  font-size: 1.125rem;
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

.retention-note {
  display: block;
  margin-top: var(--spacing-md);
  color: var(--eddie-grey-medium);
}

.empty-state {
  padding: var(--spacing-lg);
  color: var(--eddie-grey-medium);
}

@media screen and (min-width: 1024px) {
  .controls {
    flex-direction: row;
    align-items: flex-end;
    gap: var(--spacing-xxl);
  }
}
</style>
