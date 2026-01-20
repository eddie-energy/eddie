<script setup lang="ts">
import Chart from 'primevue/chart'
import { computed } from 'vue'
import type { StatusMessage } from '@/api'

const { permissions } = defineProps<{
  permissions: StatusMessage[]
}>()

const chartData = computed(() => {
  const dates: Record<string, number> = {}

  for (let permission of permissions) {
    const date = permission.startDate.substring(0, permission.startDate.indexOf('T'))
    dates[date] = (dates[date] || 0) + 1
  }

  return {
    labels: Object.keys(dates),
    datasets: [
      {
        label: 'Permissions per day',
        data: Object.values(dates),
        tension: 0.25
      }
    ]
  }
})
</script>

<template>
  <Chart
    type="line"
    :data="chartData"
    :options="{
      maintainAspectRatio: false,
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }"
    :canvasProps="{
      role: 'img',
      'aria-label': 'Line chart describing the number of permissions added over time.'
    }"
  />
</template>
