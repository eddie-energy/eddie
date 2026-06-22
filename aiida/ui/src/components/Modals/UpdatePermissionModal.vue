<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'

import Button from '@/components/Button.vue'
import { computed, ref, watch } from 'vue'
import PermissionDetails from '@/components/PermissionDetails.vue'
import { acceptPermission, getActiveInboundPermissions, rejectPermission } from '@/api'
import { usePermissionDialog } from '@/composables/permission-dialog'
import CustomSelect from '../CustomSelect.vue'
import { dataSources, fetchDataSources } from '@/stores/dataSources'
import type { AiidaPermission, AiidaSchema } from '@/types'
import { useI18n } from 'vue-i18n'

const inboundSchemas: Set<AiidaSchema> = new Set(['MIN-MAX-ENVELOPE-CIM-V1-12', 'OPAQUE'])

const { permission, open, resolveDialog } = usePermissionDialog()
const { t } = useI18n()
const modal = ref<HTMLDialogElement>()
const loading = ref(false)
const selectedDataSource = ref<string>('')
const inboundPermissions = ref<AiidaPermission[]>([])
const emit = defineEmits(['update'])

watch([open], async () => {
  if (open.value) {
    selectedDataSource.value = ''
    modal.value?.showModal()

    await fetchDataSources()
    inboundPermissions.value = await getActiveInboundPermissions()
  }
})

const handleInput = async (confirm: boolean) => {
  loading.value = true
  if (confirm) {
    await acceptPermission(permission.value!.permissionId, selectedDataSource.value)
  } else {
    await rejectPermission(permission.value!.permissionId)
  }
  modal.value?.close()
  emit('update')
}

const handleModalClose = () => {
  open.value = false
  loading.value = false
  resolveDialog()
}

const dataSourceOptions = computed(() => {
  const requestedSchemas = permission.value?.dataNeed.schemas ?? []
  const matches: { label: string; value: string }[] = []

  // Request includes inbound schemas
  if (requestedSchemas.some((requestedSchema) => inboundSchemas.has(requestedSchema))) {
    for (const { dataSource } of inboundPermissions.value) {
      if (
        dataSource && // For TypeScript, even though it should always be set here
        requestedSchemas.some((requestedSchema) => dataSource.schemas?.includes(requestedSchema))
      ) {
        matches.push({ label: dataSource.name, value: dataSource.id })
      }
    }
  }

  // Request includes outbound schemas
  if (requestedSchemas.some((requestedSchema) => !inboundSchemas.has(requestedSchema))) {
    matches.push(
      ...dataSources.value.map(({ id, name }) => ({
        label: name,
        value: id,
      })),
    )
  }

  return matches
})
</script>

<template>
  <ModalDialog
    :title="t('permissions.modal.title')"
    ref="modal"
    @close="handleModalClose"
    class="modal"
  >
    <div v-if="!loading">
      <PermissionDetails v-if="permission" :permission />
      <form class="form" v-if="permission?.dataNeed.type === 'outbound-aiida'">
        <label class="heading-3" id="updatePermLabel">
          {{ t('permissions.modal.datasourceInputLabel') }}
        </label>
        <CustomSelect
          v-model="selectedDataSource"
          id="datasourceSelect"
          :options="dataSourceOptions"
          :placeholder="t('permissions.modal.datasourceInputPlaceholder')"
          aria-labelledby="updatePermLabel"
        />

        <p
          class="text-normal"
          v-if="!dataSourceOptions.length && permission?.dataNeed.type === 'outbound-aiida'"
        >
          {{ t('permissions.modal.datasourceEmpty') }}
        </p>
      </form>

      <div class="two-item-pair">
        <Button button-style="error-secondary" @click="handleInput(false)">
          {{ t('rejectButton') }}
        </Button>
        <Button
          @click="handleInput(true)"
          :disabled="!selectedDataSource && permission?.dataNeed.type === 'outbound-aiida'"
        >
          {{ t('acceptButton') }}
        </Button>
      </div>
    </div>
    <div v-if="loading" class="loading-indicator"></div>
  </ModalDialog>
</template>

<style scoped>
.modal {
  min-height: 50vh;
}
.is-loading {
  opacity: 0;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin: 2rem 0;
}
.two-item-pair {
  margin-top: var(--spacing-xxl);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
