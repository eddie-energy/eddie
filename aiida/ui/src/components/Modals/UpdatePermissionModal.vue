<!-- SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'

import Button from '@/components/Button.vue'
import { computed, ref, watch } from 'vue'
import PermissionDetails from '@/components/PermissionDetails.vue'
import {
  acceptPermission,
  getActiveInboundPermissions,
  getDataSources,
  rejectPermission,
} from '@/api'
import { usePermissionDialog } from '@/composables/permission-dialog'
import CustomSelect from '../CustomSelect.vue'
import type { AiidaDataSource, AiidaPermission, AiidaSchema } from '@/types'
import { useI18n } from 'vue-i18n'

const inboundSchemas: Set<AiidaSchema> = new Set(['MIN-MAX-ENVELOPE-CIM-V1-12', 'OPAQUE'])

const { permission, open, resolveDialog } = usePermissionDialog()
const { t } = useI18n()
const modal = ref<HTMLDialogElement>()
const loading = ref(false)
const selectedDataSource = ref<string>('')
const displayName = ref<string>('')
const inboundPermissions = ref<AiidaPermission[]>([])
const outboundDataSources = ref<AiidaDataSource[]>([])
const emit = defineEmits(['update'])

watch([open], async () => {
  if (open.value) {
    selectedDataSource.value = ''
    displayName.value = permission.value?.dataNeed.name ?? ''
    modal.value?.showModal()

    outboundDataSources.value = await getDataSources(permission.value?.meterId)
    inboundPermissions.value = await getActiveInboundPermissions()

    if (dataSourceOptions.value.length === 1) {
      selectedDataSource.value = dataSourceOptions.value[0].value
    }
  }
})

const handleInput = async (confirm: boolean) => {
  loading.value = true
  if (confirm) {
    await acceptPermission(
      permission.value!.permissionId,
      selectedDataSource.value,
      displayName.value,
    )
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
  const permissionMeterId = permission.value?.meterId
  const matches: { label: string; value: string }[] = []

  // Request includes inbound schemas
  if (requestedSchemas.some((requestedSchema) => inboundSchemas.has(requestedSchema))) {
    for (const { dataSource, displayName } of inboundPermissions.value) {
      if (
        dataSource && // For TypeScript, even though it should always be set here
        requestedSchemas.some((requestedSchema) => dataSource.schemas?.includes(requestedSchema)) &&
        (!permissionMeterId || dataSource.meterId === permissionMeterId)
      ) {
        matches.push({ label: displayName, value: dataSource.id })
      }
    }
  }

  // Request includes outbound schemas
  if (requestedSchemas.some((requestedSchema) => !inboundSchemas.has(requestedSchema))) {
    matches.push(
      ...outboundDataSources.value.map(({ id, name }) => ({
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
      <form v-if="permission" class="form">
        <label id="displayNameLabel" class="heading-3" for="displayNameInput">
          {{ t('permissions.modal.displayNameInputLabel') }}
        </label>
        <input
          id="displayNameInput"
          v-model="displayName"
          :placeholder="t('permissions.modal.displayNameInputPlaceholder')"
          aria-labelledby="displayNameLabel"
          class="display-name-input"
          type="text"
        />
      </form>
      <form v-if="permission?.dataNeed.type === 'outbound-aiida'" class="form">
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
.display-name-input {
  border: 1px solid var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius);
  background-color: var(--light);
  color: var(--dark);
  font-size: 1rem;
  line-height: 1.5;
}
.two-item-pair {
  margin-top: var(--spacing-xxl);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
