<script setup lang="ts">
import Chart from 'chart.js/auto'
import { getPermissions } from "@/api";

async function getPermissionData() {
  const permissions = await getPermissions();

  const regionConnectors = permissions.map((x) => x.dso);

  const count = regionConnectors.reduce((acc, value) => {
    acc[value] = (acc[value] || 0) + 1;
    return acc;
  }, {});

  regionConnectorLabel = Object.keys(count);
  regionConnectorCount = Object.values(count);
}

var regionConnectorLabel = [];
var regionConnectorCount = [];

(async function() {
  await getPermissionData();

  new Chart(
    document.getElementById('doughnutChartPermissions'),
    {
      type: 'doughnut',
      data: {
        labels: regionConnectorLabel,
        datasets: [
          {
            data: regionConnectorCount,
          }
        ]
      },
      options: {
        maintainAspectRatio: false,
      }
    }
  )
})();

</script>

<template>
  <canvas id="doughnutChartPermissions"></canvas>
</template>

<style scoped>
canvas {
  width: 100% !important;
  height: 100% !important;
}
</style>