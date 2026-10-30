<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Chart from 'chart.js/auto'
import type { ChartDataset, ChartOptions } from 'chart.js'
import { getConnectionLimits } from '@/api'
import { fetchPermissions, isFcaPermission, monitorablePermissions } from '@/stores/permissions'
import CustomSelect from '@/components/CustomSelect.vue'
import Button from '@/components/Button.vue'
import type { ConnectionLimit } from '@/types'
import ContextBadge from '@/components/ContextBadge.vue'

const HOUR_IN_MS = 60 * 60 * 1000
const DAY_IN_MS = 24 * HOUR_IN_MS

const { t, locale } = useI18n()
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
const loading = ref(false)

const chartCanvas = useTemplateRef<HTMLCanvasElement>('chartCanvas')
let chartInstance: Chart | undefined

const selectedPermission = computed(() =>
  monitorablePermissions.value.find((p) => p.permissionId === permissionId.value),
)

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

function limitPoints(value: (limit: ConnectionLimit) => number | null) {
  return limits.value.flatMap((limit) => [
    { x: Date.parse(limit.intervalStart), y: value(limit) },
    { x: Date.parse(limit.intervalEnd), y: value(limit) },
  ])
}

const tickStep = computed(() => {
  const span = to.value.getTime() - from.value.getTime()
  if (span <= 2 * DAY_IN_MS) return HOUR_IN_MS
  if (span <= 14 * DAY_IN_MS) return 6 * HOUR_IN_MS
  return DAY_IN_MS
})

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
])

const chartOptions = computed<ChartOptions<'line'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  parsing: false,
  interaction: { mode: 'nearest', intersect: false },
  scales: {
    x: {
      type: 'linear',
      min: Math.floor(from.value.getTime() / tickStep.value) * tickStep.value,
      max: to.value.getTime(),
      ticks: {
        stepSize: tickStep.value,
        autoSkip: true,
        maxRotation: 0,
        callback: (value) => timeFormat.format(new Date(Number(value))),
      },
    },
    y: { title: { display: true, text: 'kW' } },
  },
  plugins: {
    legend: { position: 'bottom' },
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

const showEmptyMessage = computed(() => !loading.value && limits.value.length === 0)

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
  if (!permissionId.value) return
  loading.value = true
  try {
    limits.value = await getConnectionLimits(
      permissionId.value,
      from.value.toISOString(),
      to.value.toISOString(),
    )
  } catch {
    limits.value = []
  } finally {
    loading.value = false
  }
  await nextTick()
  renderChart()
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
            <div v-else-if="showEmptyMessage" class="chart-overlay">
              {{ t('connectionMonitor.noData') }}
            </div>
          </div>
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
