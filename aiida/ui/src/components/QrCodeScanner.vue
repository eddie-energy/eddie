<script setup lang="ts">
import { ref } from 'vue'
import { QrcodeStream, type DetectedBarcode } from 'vue-qrcode-reader'
import type { AiidaPermissionRequest } from '@/types'

const { open } = defineProps<{ open?: boolean }>()
const loading = ref(true)
const codeError = ref('')
const paused = ref(false)
const notValid = ref(false)

const emit = defineEmits<{
  (e: 'valid', permission: AiidaPermissionRequest): void
}>()

function parseAiidaCode(aiidaCode: string) {
  try {
    return JSON.parse(window.atob(aiidaCode))
  } catch {
    throw new Error('The input does not appear to be a valid AIIDA code.')
  }
}

function paintOutline(detectedCodes: DetectedBarcode[], ctx: CanvasRenderingContext2D) {
  for (const detectedCode of detectedCodes) {
    const [firstPoint, ...otherPoints] = detectedCode.cornerPoints

    ctx.strokeStyle = 'red'

    ctx.beginPath()
    ctx.moveTo(firstPoint.x, firstPoint.y)
    for (const { x, y } of otherPoints) {
      ctx.lineTo(x, y)
    }
    ctx.lineTo(firstPoint.x, firstPoint.y)
    ctx.closePath()
    ctx.stroke()
  }
}

const onError = () => {
  codeError.value = 'Unable to access the camera.'
}

const onDetect = async (detectedCodes: DetectedBarcode[]) => {
  if (!detectedCodes.length) return
  const firstDetectedCode = detectedCodes[0]
  paused.value = true
  try {
    const permissionRequest = parseAiidaCode(firstDetectedCode.rawValue)
    paused.value = false
    loading.value = true
    codeError.value = ''
    emit('valid', permissionRequest)
  } catch (error: any) {
    codeError.value = error ?? 'An unknown error occurred.'
    notValid.value = true
    paused.value = false
  }
}
</script>

<template>
  <div v-if="open" class="qr-scanner-wrapper">
    <QrcodeStream
      :track="paintOutline"
      :paused
      @error="onError"
      @detect="onDetect"
      @camera-off="loading = true"
    >
      <div class="loading-indicator" v-if="!codeError"></div>
      <p v-if="notValid" class="invalid">Not a valid AIIDA Code</p>
    </QrcodeStream>
    <div class="error" v-if="codeError">
      <p class="heading-3">Error</p>
      <p>{{ codeError }}</p>
    </div>
  </div>
</template>

<style scoped>
.error {
  max-width: 80%;
}

.qr-scanner-wrapper {
  margin-bottom: var(--spacing-xxl);
  z-index: 1;
}

.invalid {
  position: absolute;
  top: 10%;
  left: 50%;
  transform: translate(-50%);
  background-color: rgba(255, 255, 255, 0.8);
  padding: 0.5rem;
  font-weight: 600;
  color: var(--eddie-red-medium);
}
</style>
