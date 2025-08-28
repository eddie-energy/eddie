<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import type { AiidaPermission } from '@/types'
import PermissionIcon from '@/assets/icons/PermissionIcon.svg'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import StatusTag from './StatusTag.vue'
import { ref } from 'vue'
import PermissionDetails from './PermissionDetails.vue'

const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: 'healthy' | 'unhealthy'
}>()

const isOpen = ref(false)
</script>

<template>
  <li class="permission" :class="{ 'is-open': isOpen }">
    <div class="permission-header">
      <PermissionIcon />
      <h2 class="heading-5">{{ permission.serviceName }}</h2>
      <img src="@/assets/DummySmallGraph.png" />
      <time>{{
        new Date(permission.startTime).toLocaleDateString(undefined, {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
        })
      }}</time>
      <time v-if="permission.grantTime">
        {{ new Date(permission.grantTime).toLocaleTimeString() }}
      </time>
      <StatusTag :status-type="status">{{ STATUS[permission.status].title }}</StatusTag>
      <button class="chevron" @click="isOpen = !isOpen">
        <ChevronDownIcon />
      </button>
    </div>
    <Transition name="details">
      <PermissionDetails v-if="isOpen" class="permission-details" :permission :status />
    </Transition>
  </li>
</template>

<style scoped>
.permission {
  padding: var(--spacing-lg) var(--spacing-xlg);
  border: 1px solid var(--eddie-primary);
  margin-bottom: var(--spacing-md);
  background-color: var(--light);
  border-radius: 0.5rem;

  &.is-open {
    .chevron {
      transform: rotate(180deg);
    }
    .permission-header {
      margin-bottom: var(--spacing-lg);
      & > :not(button, h2, svg) {
        opacity: 0;
        visibility: hidden;
      }
    }
  }
}
.permission-header {
  display: flex;
  align-items: center;
  width: 100%;
  gap: var(--spacing-xlg);
  transition: margin-bottom 0.3s ease-out;
  > * {
    transition: opacity 0.5s ease-in;
  }
}

.chevron {
  cursor: pointer;
  justify-self: end;
  margin-left: auto;
  padding: 0.5rem;
  transition: transform 0.3s ease-in-out;
}

.details-enter-active,
.details-leave-active {
  transition: opacity 0.3s ease;
}

.details-enter-from,
.details-leave-to {
  opacity: 0;
}
</style>
