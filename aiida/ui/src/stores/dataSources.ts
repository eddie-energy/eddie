// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { ref } from 'vue'
import { getDataSourceHealthStatus, getDataSourceImage, getDataSources } from '@/api.js'
import type { AiidaDataSource, AiidaDataSourceHealthStatus } from '@/types'

export const dataSources = ref<AiidaDataSource[]>([])
export const dataSourceImages = ref<Record<string, string | undefined>>({})
export const dataSourceHealthStatuses = ref<Record<string, AiidaDataSourceHealthStatus | undefined>>({})

export async function fetchDataSourcesFull() {
  await fetchDataSources()
  await fetchDataSourcesHealthStatus()
  await fetchDataSourceImages()
}

export async function fetchDataSources(){
  dataSources.value = await getDataSources()
}

export async function fetchDataSourcesHealthStatus() {
  for (const datasource of dataSources.value) {
    try {
      dataSourceHealthStatuses.value[datasource.id] = await getDataSourceHealthStatus(datasource.id)
    } catch {
      dataSourceHealthStatuses.value[datasource.id] = undefined
    }
  }
}

export async function fetchDataSourceImages() {
  for (const datasource of dataSources.value) {
    try {
      dataSourceImages.value[datasource.id] = URL.createObjectURL(
        await getDataSourceImage(datasource.id),
      )
    } catch {
      dataSourceImages.value[datasource.id] = undefined
    }
  }
}