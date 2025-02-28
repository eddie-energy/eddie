<script setup lang="ts">
import Chart from 'primevue/chart'
import { ref, watch } from 'vue'

const props = defineProps<{
  permissions: {
    permissionId: string
    regionConnectorId: string
    dataNeedId: string
    country: string
    dso: string
    startDate: string
    status: string
    parsedStartDate: string
  }[]
}>()

const chartData = ref()
const chartOptions = ref()

watch(props, () => {
  const permissionsStartDates = props.permissions.map(({ startDate }) => startDate)

  const dateArray = permissionsStartDates.map((dateTime) => {
    const [year, month, day] = dateTime.split('T')[0].split('-')
    return `${day}.${month}.${year}`
  })

  let permissionsCountPerDate: { [key: string]: number } = {}
  for (const date of dateArray) {
    permissionsCountPerDate[date] = (permissionsCountPerDate[date] || 0) + 1
  }

  chartData.value = {
    labels: Object.keys(permissionsCountPerDate),
    datasets: [
      {
        label: 'Permissions per day',
        data: Object.values(permissionsCountPerDate),
        tension: 0.25
      }
    ]
  }

  chartOptions.value = {
    maintainAspectRatio: false,
    scales: {
      y: {
        beginAtZero: true
      }
    }
  }
})
</script>

<template>
  <div class="permissions-chart-container">
    <Chart
      class="permissions-chart"
      type="line"
      :data="chartData"
      :options="chartOptions"
      :canvasProps="{
        role: 'img',
        'aria-label': 'Line chart describing the number of permissions added over time.'
      }"
    />
  </div>
</template>

<style scoped>
.permissions-chart {
  width: 100%;
  height: 100%;
}

.permissions-chart-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
