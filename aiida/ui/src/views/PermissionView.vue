<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import PermissionList from '@/components/PermissionList.vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import AddPermissionModal from '@/components/Modals/AddPermissionModal.vue'
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

const permissionModalRef = ref<HTMLDialogElement>()
const { t } = useI18n()
const showAddPermissionModal = () => {
  permissionModalRef.value?.showModal()
}
</script>

<template>
  <main>
    <header class="two-item-pair bottom-margin">
      <h1 class="heading-2">{{ t('permissions.title') }}</h1>
      <Button @click="showAddPermissionModal" class="add-button">
        <PlusIcon />{{ t('permissions.addButton') }}
      </Button>
    </header>
    <PermissionList />
    <AddPermissionModal ref="permissionModalRef" />
  </main>
</template>

<style scoped>
.bottom-margin {
  margin-bottom: var(--spacing-xxl);
}

.two-item-pair {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-direction: column;
  gap: var(--spacing-md);
}
.add-button {
  width: 100%;
  justify-content: center;
}

@media screen and (min-width: 640px) {
  .two-item-pair {
    flex-direction: row;
    align-items: center;
  }
  .add-button {
    width: fit-content;
  }
}
</style>
