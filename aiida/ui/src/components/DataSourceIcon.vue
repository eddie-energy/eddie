<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import ElectricityIcon from '@/assets/icons/ElectricityIcon.svg'
import MeterIcon from '@/assets/icons/MeterIcon.svg'
import WaterIcon from '@/assets/icons/WaterIcon.svg'
import HeatIcon from '@/assets/icons/HeatIcon.svg'
import type { AiidaDataSourceIcon } from '@/types'

const { icon } = defineProps<{ icon: AiidaDataSourceIcon }>()
</script>

<template>
  <span class="icon">
    <ElectricityIcon v-if="icon === 'ELECTRICITY'" class="elec-icon" />
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

  .elec-icon {
    width: 0.85rem;
  }
}
</style>
