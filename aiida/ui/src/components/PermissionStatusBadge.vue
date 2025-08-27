<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import type { PermissionStatus } from '@/types'

const props = defineProps<{ status: PermissionStatus }>()
const status = props.status
</script>

<template>
  <sl-tooltip :content="STATUS[status].description">
    <sl-badge
      :variant="
        STATUS[status].isActive
          ? 'success'
          : STATUS[status].isOpen
            ? 'primary'
            : STATUS[status].isError
              ? 'danger'
              : 'neutral'
      "
    >
      {{ STATUS[status].title }}
    </sl-badge>
  </sl-tooltip>
</template>
