<!-- SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script lang="ts" setup>
import type { AiidaPermission } from '@/types'
import PenIcon from '@/assets/icons/PenIcon.svg'
import { nextTick, ref, useTemplateRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirmDialog } from '@/composables/confirm-dialog'
import { updateDisplayName } from '@/api'
import { fetchPermissions } from '@/stores/permissions'

const { permission } = defineProps<{
  permission: AiidaPermission
}>()
const isEditing = defineModel<boolean>('editing', { default: false })

const { t } = useI18n()
const { confirm } = useConfirmDialog()

const displayNameInput = ref(permission.displayName)
const displayNameInputEl = useTemplateRef('displayNameInputEl')

watch(
  () => permission.displayName,
  (displayName) => {
    displayNameInput.value = displayName
  },
)

const startEditingDisplayName = async () => {
  isEditing.value = true
  await nextTick()
  displayNameInputEl.value?.focus()
}

const saveDisplayName = async () => {
  const newDisplayName = displayNameInput.value.trim()
  if (!newDisplayName || newDisplayName === permission.displayName) {
    displayNameInput.value = permission.displayName
    isEditing.value = false
    return
  }

  if (
    await confirm(
      t('permissions.updateDisplayNameTitle'),
      t('permissions.updateDisplayNameDescription', { displayName: newDisplayName }),
      t('editButton'),
      t('cancelButton'),
      'primary',
    )
  ) {
    try {
      await updateDisplayName(permission.permissionId, newDisplayName)
      fetchPermissions()
    } catch {
      displayNameInput.value = permission.displayName
    }
  } else {
    displayNameInput.value = permission.displayName
  }
  isEditing.value = false
}

const cancelEditingDisplayName = () => {
  displayNameInput.value = permission.displayName
  isEditing.value = false
}
</script>

<template>
  <template v-if="!isEditing">
    <h2 class="heading-5 title">{{ permission.displayName }}</h2>
    <button
      :aria-label="t('permissions.dropdown.editDisplayName')"
      class="edit-name-button"
      @click.stop="startEditingDisplayName"
    >
      <PenIcon />
    </button>
  </template>
  <input
    v-else
    ref="displayNameInputEl"
    v-model="displayNameInput"
    class="title-input"
    type="text"
    @blur="saveDisplayName"
    @click.stop
    @keyup.enter="($event.target as HTMLInputElement).blur()"
    @keyup.esc="cancelEditingDisplayName"
  />
</template>

<style scoped>
.title {
  overflow: hidden;
  text-overflow: ellipsis;
}

.edit-name-button {
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  color: var(--eddie-grey-medium);
  transition: color 0.3s ease-in-out;

  &:hover {
    color: var(--eddie-primary);
  }
}

.title-input {
  border: 1px solid var(--eddie-grey-medium);
  border-radius: var(--border-radius);
  background-color: var(--light);
  color: var(--dark);
  padding: var(--spacing-sm) var(--spacing-md);
  min-width: 0;
  width: 100%;

  &:focus {
    outline: none;
    border-color: var(--eddie-primary);
  }
}
</style>
