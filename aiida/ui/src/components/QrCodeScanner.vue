<script setup lang="ts">
import { BrowserQRCodeReader } from '@zxing/browser'
import { ref, useTemplateRef } from 'vue'

const emit = defineEmits(['result'])

const codeReader = new BrowserQRCodeReader()
const errorMessage = ref('')
const loading = ref(true)

const dialog = useTemplateRef('dialog')
const video = useTemplateRef<HTMLVideoElement>('video')

async function startScanning() {
  errorMessage.value = ''

  dialog.value.show()

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment' },
    })
    const result = await codeReader.decodeOnceFromStream(stream, video.value as HTMLVideoElement)
    emit('result', result.getText())

    dialog.value.hide()
  } catch (error: any) {
    console.error(error)

    errorMessage.value = error.message ?? error
  }
}

async function stopScanning() {
  dialog.value.hide()
}
</script>

<template>
  <sl-button @click="startScanning" circle>
    <sl-icon name="camera" label="Camera"></sl-icon>
  </sl-button>

  <sl-dialog ref="dialog" id="dialog" label="Scan AIIDA code">
    <video ref="video" @play="loading = false" :hidden="loading"></video>

    <template v-if="loading && !errorMessage">
      Waiting for camera...
      <sl-spinner></sl-spinner>
    </template>

    <template v-if="errorMessage">
      <sl-alert variant="danger" open>
        <sl-icon slot="icon" name="exclamation-triangle"></sl-icon>
        {{ errorMessage }}
      </sl-alert>
    </template>

    <div id="error"></div>

    <sl-button @close="stopScanning" slot="footer" outline>Close</sl-button>
  </sl-dialog>
</template>

<style scoped>
video {
  width: 100%;
}
</style>
