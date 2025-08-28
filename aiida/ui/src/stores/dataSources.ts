import { ref } from 'vue'
import { getDataSources } from '@/api.js'
import type { AiidaDataSource } from '@/types'

export const dataSources = ref<AiidaDataSource[]>([])

export async function fetchDataSources() {
  dataSources.value = await getDataSources()
}
