<script setup lang="ts">
const { open, password } = defineProps(['open', 'password'])
const emit = defineEmits(['hide'])

function hide(event: Event) {
  // avoid conflict with hide event from Shoelace's select element
  if (event.target === event.currentTarget) {
    emit('hide')
  }
}
</script>

<template>
  <sl-dialog label="MQTT Password" :open="open || undefined" @sl-hide="hide">
    <p>Make sure to copy the password now. You will not be able to view it again.</p>
    <sl-input type="password" readonly password-toggle :value="password">
      <template v-slot:suffix>
        <sl-copy-button :value="password"></sl-copy-button>
      </template>
    </sl-input>

    <template v-slot:footer>
      <sl-button variant="danger" @click="hide">Close</sl-button>
    </template>
  </sl-dialog>
</template>
