<script setup lang="ts">
import Chart from 'chart.js/auto';

defineOptions({
  async mounted() {
    const ctx = document.getElementById('lineChartDataPackages');
    new Chart(ctx, {
      type: 'line',
      data: {
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
            tension: 0.4,
          },
        ]
      },
      options: {
        maintainAspectRatio: false,
        aspectRatio: 0.6,
      }
    });
  }})
</script>

<template>
  <canvas id="lineChartDataPackages"></canvas>
</template>

<style scoped>

</style>