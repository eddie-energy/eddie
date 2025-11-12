<script setup lang="ts">
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import { computed, onMounted, ref, useTemplateRef, watch } from 'vue'

const { options, placeholder } = defineProps<{
  options: { label?: string; value: string }[] | string[]
  placeholder: string
}>()
const model = defineModel()
const show = ref(false)
const parentDiv = useTemplateRef('parent')

const boundingRect = ref()

watch([show], () => {
  if (show.value) {
    boundingRect.value = parentDiv.value?.getBoundingClientRect()
  }
})

const labelValueOptions = computed(() => {
  if (typeof options[0] === 'string') {
    return options.map((option) => ({
      label: option,
      value: option,
    }))
  } else {
    return options as { label?: string; value: string }[]
  }
})

const handleOptionClick = (value: string) => {
  show.value = false
  model.value = value
}

const handleBlur = (e: FocusEvent) => {
  if (
    !(e.target as Node).contains(e.relatedTarget as Node) &&
    !parentDiv.value?.contains(e.relatedTarget as Node)
  ) {
    show.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', () => {
    boundingRect.value = parentDiv.value?.getBoundingClientRect()
  })
})
</script>

<template>
  <div
    class="select"
    :class="{ 'is-open': show }"
    @click="show = !show"
    @keydown.space="show = !show"
    tabindex="0"
    @blur="handleBlur"
    ref="parent"
    role="listbox"
  >
    <div class="main-option" :class="{ placeholder: model === '' }">
      {{ labelValueOptions.find((option) => option.value === model)?.label ?? placeholder }}
      <ChevronDownIcon class="icon" />
    </div>
    <Transition>
      <div
        class="options"
        v-if="show"
        :style="{ width: `${parentDiv?.offsetWidth}px`, left: `${boundingRect?.left}px` }"
      >
        <div
          v-for="option in labelValueOptions as { label?: string; value: string }[]"
          class="option"
          :class="{ selected: model === option.value }"
          @click.stop="handleOptionClick(option.value)"
          :key="option.value"
          tabindex="0"
          @keydown.enter="handleOptionClick(option.value)"
          @blur="handleBlur"
          role="option"
          :aria-selected="model === option.value"
        >
          {{ option.label }}
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.select {
  position: relative;
  border: 1px solid var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-md);
  color: var(--dark);
  background-color: var(--light);
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1.5;
  transition:
    border-radius 0.3s ease-in-out,
    border-color 0.3s ease-in-out;

  &.is-open {
    border-bottom-color: transparent;
    border-radius: var(--border-radius) var(--border-radius) 0 0;
    .icon {
      transform: rotate(180deg);
    }
  }
}

.icon {
  transition: transform 0.3s ease-in-out;
}

.main-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  &.placeholder {
    color: var(--eddie-grey-medium);
  }
}
.options {
  position: fixed;
  z-index: 100;
  background-color: var(--light);
  border: 1px solid var(--eddie-grey-medium);
  border-top: unset;
  border-radius: 0 0 var(--border-radius) var(--border-radius);
  overflow: auto;
  max-height: 250px;
}
.option {
  border-left: 3px solid transparent;
  padding: var(--spacing-sm) var(--spacing-xlg);
  transition:
    border-color 0.3s ease-in-out,
    background-color 0.3s ease-in-out;
  &:hover,
  &:focus {
    background-color: var(--eddie-grey-light);
    border-color: var(--eddie-primary);
  }
  &.selected {
    background-color: var(--eddie-turquoise-light);
    border-color: var(--eddie-primary);
  }
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.3s ease;
}

.v-enter-from,
.v-leave-to {
  opacity: 0;
}
</style>
