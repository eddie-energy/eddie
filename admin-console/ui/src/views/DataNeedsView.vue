<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { type DataNeed, getDataNeeds } from '@/api'

const dataNeeds = ref<DataNeed[]>([])

onMounted(async () => {
  dataNeeds.value = await getDataNeeds()
})
</script>

<template>
  <h1>Data Needs</h1>

  <article v-for="dataNeed in dataNeeds" :key="dataNeed.id">
    <h2>{{ dataNeed.id }}</h2>
  </article>
</template>
