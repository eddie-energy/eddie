<script setup lang="ts">
import Chart from 'chart.js/auto'
import { getPermissions } from "@/api";

async function getPermissionData() {
  const permissions = await getPermissions();

  const regionConnectorsDatesAndTime = permissions.map((x) => x.startDate);

  const dateArray = regionConnectorsDatesAndTime.map(dateTime => {
    const [year, month, day] = dateTime.split('T')[0].split('-');
    return `${day}.${month}.${year}`;
  });

  const count = dateArray.reduce((acc, value) => {
    acc[value] = (acc[value] || 0) + 1;
    return acc;
  }, {});

  dateLabel = Object.keys(count);
  permissionsPerDate = Object.values(count);

}

var dateLabel = [];
var permissionsPerDate = [];

(async function() {
  await getPermissionData();

  new Chart(
    document.getElementById('lineChartPermissions'),
    {
      type: 'line',
      data: {
        labels: dateLabel,
        datasets: [
          {
            label: 'Permissions per day',
            data: permissionsPerDate,
            tension: 0.25,
          }
        ]
      },
      options: {
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true
          }
        }
      }
    }
  )
})();

</script>

<template>
  <canvas id="lineChartPermissions"></canvas>
</template>

<style scoped>

</style>