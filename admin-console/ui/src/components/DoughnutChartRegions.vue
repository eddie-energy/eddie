<script setup lang="ts">
import Chart from 'primevue/chart'
import { ref, watch } from 'vue'

const props = defineProps<{ permissionCountPerRegionConnector: { [key: string]: number } }>()

const chartData = ref()
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
})
</script>

<template>
  <Chart
    class="chart"
    type="doughnut"
    :data="chartData"
    :canvasProps="{
      role: 'img',
      'aria-label': 'Pie chart describing the number of permissions per region connector.'
    }"
  />
</template>

<style scoped>
.chart {
  height: 30rem;
  max-width: 30rem;
}
</style>
