<script setup lang="ts">
import { ref, onBeforeUnmount, computed, useTemplateRef, nextTick } from 'vue'
import Button from '@/components/Button.vue'
import { createJsonBlobUrl, revokeUrl } from '@/utils/files'
import type { AiidaDataSource, AiidaPermission } from '@/types'
import {
  getLatestDataSourceMessage,
  getLatestInboundPermissionMessage,
  getLatestOutboundPermissionMessage,
} from '@/api'

const { data, buttonStyle = 'primary' } = defineProps<{
  data: AiidaDataSource | AiidaPermission
  buttonStyle?: 'primary' | 'secondary' | 'error' | 'error-secondary'
}>()

const isPreparing = ref(false)
const latestUrl = ref<string>('')
const buttonRef = useTemplateRef('button')

onBeforeUnmount(() => {
  if (latestUrl.value) revokeUrl(latestUrl.value)
  latestUrl.value = ''
})

const filename = computed(() => {
  if ('id' in data) {
    return `datasource_${data.id}_latest.json`
  } else {
    return `permission_${data.permissionId}_latest.json`
  }
})
const handleClick = async () => {
  if (isPreparing.value || latestUrl.value) return

  isPreparing.value = true
  try {
    let latestMessage: unknown
    if ('id' in data) {
      latestMessage = await getLatestDataSourceMessage(data.id)
    } else {
      latestMessage =
        data.dataNeed.type === 'inbound-aiida'
          ? await getLatestInboundPermissionMessage(data.permissionId)
          : await getLatestOutboundPermissionMessage(data.permissionId)
    }
    latestUrl.value = createJsonBlobUrl(latestMessage)
    await nextTick()
    buttonRef.value?.$el.click()

    setTimeout(() => {
      revokeUrl(latestUrl.value)
      latestUrl.value = ''
    }, 2000)
  } catch (err) {
    console.error(err)
  } finally {
    isPreparing.value = false
  }
}
</script>

<template>
  <Button
    :href="latestUrl || undefined"
    ref="button"
    :button-style
    :download="filename"
    @click="handleClick"
    is-link
    :rel="'noopener'"
  >
    <slot>Show Latest Message</slot>
  </Button>
</template>
