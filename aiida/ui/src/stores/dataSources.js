import { ref } from 'vue'
import { getDataSources } from '@/api.js'

/** @type {Ref<AiidaDataSource[]>} */
export const dataSources = ref([])

/**
 * Updates the data source store by fetching all data sources from the backend.
 * @returns {void}
 */
export async function fetchDataSources() {
  dataSources.value = await getDataSources()
}
