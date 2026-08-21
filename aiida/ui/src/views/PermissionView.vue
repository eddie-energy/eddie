<!-- SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<script setup lang="ts">
import PermissionList from '@/components/PermissionList.vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import AddPermissionModal from '@/components/Modals/AddPermissionModal.vue'
import MqttPasswordModal from '@/components/Modals/MqttPasswordModal.vue'
import UpdateMqttProvisioningConnectionModal from '@/components/Modals/UpdateMqttProvisioningConnectionModal.vue'
import type { AiidaPermission } from '@/types'
import { ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { regenerateInboundServerPassword } from '@/api'
import useToast from '@/composables/useToast'

const permissionModalRef = ref<HTMLDialogElement>()
const inboundProvisioningModal = useTemplateRef<
  InstanceType<typeof UpdateMqttProvisioningConnectionModal>
>('inboundProvisioningModal')
const inboundServerPasswordModal = useTemplateRef<InstanceType<typeof MqttPasswordModal>>(
  'inboundServerPasswordModal',
)
const { t } = useI18n()
const { success } = useToast()
const showAddPermissionModal = () => {
  permissionModalRef.value?.showModal()
}

const configureInboundProvisioning = (permission: AiidaPermission) => {
  inboundProvisioningModal.value?.showModal(permission)
}

const resetInboundServerPassword = async (permission: AiidaPermission) => {
  const { password } = await regenerateInboundServerPassword(permission.permissionId)
  success('datasources.modal.mqttResetSuccess')
  inboundServerPasswordModal.value?.showModal(password)
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
    <PermissionList
      @configure-inbound-provisioning="configureInboundProvisioning"
      @reset-inbound-server-password="resetInboundServerPassword"
    />
    <AddPermissionModal ref="permissionModalRef" />
    <UpdateMqttProvisioningConnectionModal ref="inboundProvisioningModal" />
    <MqttPasswordModal ref="inboundServerPasswordModal" />
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
