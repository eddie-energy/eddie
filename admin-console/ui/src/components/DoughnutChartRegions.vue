<script setup lang="ts">
import Chart from 'primevue/chart'
import { ref, watch } from 'vue'

const props = defineProps<{ permissionCountPerRegionConnector: { id: string; count: number }[] }>()

const chartData = ref()
const chartOptions = ref()
watch(props, () => {
  const labels = props.permissionCountPerRegionConnector.map(({ id }) => id)
  const data = props.permissionCountPerRegionConnector.map(({ count }) => count)

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