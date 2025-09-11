<script setup lang="ts">
import ModalDialog from '@/components/ModalDialog.vue'
import { ref, useTemplateRef } from 'vue'
import Button from '../Button.vue'
import CopyIcon from '@/assets/icons/CopyIcon.svg'
import CheckmarkIcon from '@/assets/icons/CheckmarkIcon.svg'
import EyeIcon from '@/assets/icons/EyeIcon.svg'
import CrossedOutEyeIcon from '@/assets/icons/CrossedOutEyeIcon.svg'

const modal = useTemplateRef<HTMLDialogElement>('modal')
const pass = ref<string | undefined>()
const copied = ref(false)
const show = ref(false)
const title = ref('')

const showModal = (password?: string, isNew?: boolean) => {
  title.value = isNew ? 'Copy Password' : 'Reset Password'
  pass.value = password
  modal.value?.showModal()
}

const handleCopy = () => {
  copied.value = true
  navigator.clipboard.writeText(pass.value ?? '')
}

const handleShow = () => {
  show.value = !show.value
}

const closeModal = () => {
  copied.value = false
  show.value = false
}

const checkClipboard = async () => {
  if ((await navigator.clipboard.readText()) != pass.value) {
    copied.value = false
  }
}

defineExpose({ showModal })
</script>

<template>
  <ModalDialog :title ref="modal" @close="closeModal" @focus="checkClipboard">
    <p class="text-limit text-normalc">
      Make sure to copy the password now. You will not be able to view it again.
    </p>
    <div class="input-field text-limit">
      <input readonly :type="show ? 'text' : 'password'" :value="pass" />
      <div class="actions">
        <button
          class="copy-button"
          :class="{ copied }"
          @click="handleCopy"
          :disabled="copied"
          aria-label="Copy MQTT password"
        >
          <Transition mode="out-in">
            <component :is="copied ? CheckmarkIcon : CopyIcon" />
          </Transition>
        </button>
        <button
          class="show-button"
          @click="handleShow"
          :aria-label="show ? 'Hide MQTT password' : 'Show MQTT password'"
        >
          <Transition mode="out-in">
            <component :is="show ? EyeIcon : CrossedOutEyeIcon" />
          </Transition>
        </button>
      </div>
    </div>
    <Button class="close-button" button-style="error" @click="modal?.close()">Close</Button>
  </ModalDialog>
</template>

<style scoped>
.text-limit {
  width: 100%;
  margin-bottom: var(--spacing-xxl);
}

.actions {
  display: flex;
  gap: var(--spacing-sm);
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

.show-button {
  color: var(--eddie-grey-medium);
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
