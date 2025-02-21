<script setup lang="ts">
import Chart from 'primevue/chart'
import { ref, watch } from 'vue'

const props = defineProps<{ permissionCountPerRegionConnector: { [key: string]: number } }>()

const chartData = ref()
const chartOptions = ref()
watch(props, () => {
  const labels = Object.keys(props.permissionCountPerRegionConnector)
  const data = Object.values(props.permissionCountPerRegionConnector)

  chartData.value = {
    labels: labels,
    datasets: [
      {
        data: data
      }
    ]
  }
  chartOptions.value = {
    maintainAspectRatio: false
  }
})
</script>

<template>
  <div class="regionConnectorCount-chart-container">
    <Chart
      class="regionConnectorCount-chart"
      type="doughnut"
      :data="chartData"
      :options="chartOptions"
      :canvasProps="{
        role: 'img',
        'aria-label': 'Pie chart describing the number of permissions per region connector.'
      }"
    />
  </div>
</template>

<style scoped>
.regionConnectorCount-chart {
  width: 100%;
  height: 100%;
}

.regionConnectorCount-chart-container {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
