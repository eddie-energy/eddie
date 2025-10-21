<script setup lang="ts">
import { computed, ref } from 'vue'
import { useDropZone } from '@vueuse/core'
import { useI18n } from 'vue-i18n'

const dropZoneRef = ref<HTMLDivElement>()
const imageFile = defineModel<File | null>()

const imageUploadInput = ref<HTMLInputElement>()
const allowedFileTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/svg+xml']
const { t } = useI18n()
const onImageDrop = (files: File[] | null) => {
  const file = files && files[0]
  if (file) {
    // Need to double check file type in drop zone for Safari
    if (!isAllowedFileType([file.type])) {
      console.error('Dropped file is not an image:', file)
      return
    }
    imageFile.value = file
  }
}

const isAllowedFileType = (types: readonly string[]): boolean => {
  return types.some((type) => allowedFileTypes.includes(type))
}

const { isOverDropZone } = useDropZone(dropZoneRef, {
  multiple: false,
  dataTypes: isAllowedFileType,
  onDrop: onImageDrop,
})

const handleImage = (event: Event) => {
  const file = Array.from((event.target as HTMLInputElement).files || [])[0] || null
  imageFile.value = file
}

const handleImageUploadKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    imageUploadInput.value?.click()
  }
}

const previewImage = computed(() => {
  if (imageFile.value) {
    return URL.createObjectURL((imageFile.value as Blob) ?? '')
  } else {
    return null
  }
})
</script>

<template>
  <div class="image-upload" ref="dropZoneRef">
    <div class="image-upload-inner" :class="{ 'is-over': isOverDropZone }">
      <p class="heading-3">{{ t('datasources.modal.uploadImageTitle') }}</p>
      <p>{{ t('datasources.modal.uploadImageExtra') }}</p>
      <img v-if="previewImage" :src="previewImage" alt="Uploaded Image" class="preview-image" />
      <label
        for="image"
        class="image-upload-label"
        tabindex="0"
        @keydown="handleImageUploadKeydown"
        >{{ t('datasources.modal.uploadImageButton') }}</label
      >
      <input
        ref="imageUploadInput"
        type="file"
        :accept="allowedFileTypes.join(',')"
        id="image"
        @change="handleImage"
        class="image-upload-input"
      />
    </div>
  </div>
</template>

<style scoped>
.image-upload {
  display: flex;
  border: 1px solid var(--dark);
  border-radius: var(--spacing-sm);
}

.image-upload-inner {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-height: 15rem;
  justify-content: center;
  align-items: center;
  text-align: center;
  margin: var(--spacing-md);
  border: 1px dashed var(--eddie-grey-medium);
  gap: 0.75rem;
  padding: var(--spacing-md) var(--spacing-sm);

  &.is-over {
    border: 1px solid var(--eddie-primary);
  }
}

.image-upload-input {
  display: none;
}

.image-upload-label {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1.5px solid var(--dark);
  border-radius: var(--spacing-xlg);
  cursor: pointer;
  font-weight: 600;
  width: fit-content;
  transition: background-color 0.3s ease-in-out;
  &:hover {
    background-color: var(--eddie-grey-light);
  }
}

.preview-image {
  aspect-ratio: 1 / 1;
  max-height: 100px;
}
</style>
