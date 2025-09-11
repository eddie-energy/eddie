import { ref } from 'vue'
import { getDataSourceImage, getDataSources } from '@/api.js'
import type { AiidaDataSource } from '@/types'

export const dataSources = ref<AiidaDataSource[]>([])
export const dataSourceImages = ref<Record<string, string | undefined>>({})

export async function fetchDataSources() {
  dataSources.value = await getDataSources()
  dataSources.value.map(async (datasource) => {
    try {
      dataSourceImages.value[datasource.id] = URL.createObjectURL(
        await getDataSourceImage(datasource.id),
      )
    } catch {
      dataSourceImages.value[datasource.id] = undefined
    }
  })
}
