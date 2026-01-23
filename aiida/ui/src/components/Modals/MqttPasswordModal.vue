<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'
import { ref, useTemplateRef } from 'vue'
import Button from '../Button.vue'

import EyeIcon from '@/assets/icons/EyeIcon.svg'
import CrossedOutEyeIcon from '@/assets/icons/CrossedOutEyeIcon.svg'
import CopyButton from '../CopyButton.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const modal = useTemplateRef<HTMLDialogElement>('modal')
const pass = ref<string | undefined>()
const showPassword = ref(false)
const title = ref('')

const showModal = (password?: string, isNew?: boolean) => {
  title.value = isNew ? t('datasources.modal.mqttCopyTitle') : t('datasources.modal.mqttResetTitle')
  pass.value = password
  modal.value?.showModal()
}

const handleShow = () => {
  showPassword.value = !showPassword.value
}

const closeModal = () => {
  showPassword.value = false
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog :title ref="modal" @close="closeModal">
    <p class="text-limit text-normal">
      {{ t('datasources.modal.mqttPasswordLabel') }}
    </p>
    <form class="input-field text-limit">
      <input
        readonly
        autocomplete="off"
        :type="showPassword ? 'text' : 'password'"
        :value="pass"
        name="password"
      />
      <div class="actions">
        <CopyButton :copy-text="pass" :aria-label="t('permissions.copyMqttPassword')" />
        <button
          class="showPassword-button"
          @click="handleShow"
          :aria-label="
            showPassword ? t('permissions.hideMqttPassword') : t('permissions.showMqttPassword')
          "
          type="button"
        >
          <Transition mode="out-in">
            <component :is="showPassword ? EyeIcon : CrossedOutEyeIcon" />
          </Transition>
        </button>
      </div>
    </form>
    <Button class="close-button" button-style="error" @click="modal?.close()">{{
      t('closeButton')
    }}</Button>
  </ModalDialog>
</template>

<style scoped>
.text-limit {
  width: 100%;
  margin-bottom: var(--spacing-xxl);
}

.actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
  position: absolute;
  right: var(--spacing-md);
  top: 50%;
  transform: translateY(-50%);

  button {
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

.copy-button {
  padding: 0;
  color: var(--eddie-primary);
}

button.copied {
  cursor: unset;
  color: var(--eddie-green);
}

.showPassword-button {
  color: var(--eddie-grey-medium);
  padding: unset;
  svg {
    height: 1rem;
    width: 1rem;
  }
}

.input-field {
  position: relative;

  input {
    border: 1px solid var(--eddie-grey-medium);
    padding: var(--spacing-sm) var(--spacing-md);
    border-radius: var(--border-radius);
    width: 100%;
  }
}

.close-button {
  margin-left: auto;
  width: 100%;
  justify-content: center;
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.2s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}

@media screen and (min-width: 640px) {
  .close-button {
    width: fit-content;
    justify-content: flex-start;
  }
  .text-limit {
    max-width: 80%;
  }
}
</style>
