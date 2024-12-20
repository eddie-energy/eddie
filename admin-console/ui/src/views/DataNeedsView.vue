<script lang="ts" setup>
import { getDataNeeds } from '@/api.ts'
import { onMounted, ref } from 'vue'

const dataNeeds = ref([])

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
