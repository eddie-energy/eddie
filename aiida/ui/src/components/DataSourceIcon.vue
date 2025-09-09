<script setup lang="ts">
import ElectricityIcon from '@/assets/icons/ElectricityIcon.svg'
import MeterIcon from '@/assets/icons/MeterIcon.svg'
import WaterIcon from '@/assets/icons/WaterIcon.svg'
import HeatIcon from '@/assets/icons/HeatIcon.svg'

const { icon } = defineProps<{ icon: 'ELECTRICITY' | 'HEAT' | 'METER' | 'WATER' }>()
</script>

<template>
  <span class="icon">
    <ElectricityIcon v-if="icon === 'ELECTRICITY'" />
    <HeatIcon v-if="icon === 'HEAT'" />
    <MeterIcon v-if="icon === 'METER'" />
    <WaterIcon v-if="icon === 'WATER'" />
  </span>
</template>

<style scoped>
.icon {
  height: 2rem;
  width: 2rem;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid var(--eddie-grey-light);
  border-radius: var(--border-radius);
}
</style>
