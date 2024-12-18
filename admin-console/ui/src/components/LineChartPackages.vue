<script setup lang="ts">
import Chart from 'primevue/chart'
import { onMounted, ref } from 'vue'

onMounted(() => {
  chartData.value = setChartData()
  chartOptions.value = setChartOptions()
})

const chartData = ref()
const chartOptions = ref()

const setChartData = () => {
  return {
    labels: ['January', 'February', 'March', 'April', 'May', 'June', 'July'],
    datasets: [
      {
        label: 'First Dataset',
        data: [65, 59, 80, 81, 56, 55, 40],
        fill: false,
        tension: 0.4
      },
      {
        label: 'Second Dataset',
        data: [28, 48, 40, 19, 86, 27, 90],
        fill: false,
        tension: 0.4
      }
    ]
  }
}

const setChartOptions = () => {
  return {
    maintainAspectRatio: false
  }
}
</script>

<template>
  <div class="dataPackages-chart-container">
    <Chart class="dataPackages-chart" type="line" :data="chartData" :options="chartOptions" />
  </div>
</template>

<style scoped>
.dataPackages-chart {
  width: 100%;
  height: 100%;
}

.dataPackages-chart-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>