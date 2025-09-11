<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import type { AiidaPermission, PermissionTypes } from '@/types'
import PermissionsNavIcon from '@/assets/icons/PermissionsNavIcon.svg'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import StatusTag from './StatusTag.vue'
import { ref } from 'vue'
import PermissionDetails from './PermissionDetails.vue'

const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: PermissionTypes
}>()

const isOpen = ref(false)
</script>

<template>
  <li class="permission" :class="{ 'is-open': isOpen }">
    <header class="permission-header" @click="isOpen = !isOpen">
      <div class="icon-wrapper">
        <PermissionsNavIcon class="icon" />
      </div>
      <h2 class="heading-5 title">{{ permission.serviceName }}</h2>
      <p v-if="permission.unimplemented" class="small-data-graph">Placeholder</p>
      <time class="non-essential">{{
        new Date(permission.startTime).toLocaleDateString(undefined, {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
        })
      }}</time>
      <time v-if="permission.grantTime" class="non-essential">
        {{ new Date(permission.grantTime).toLocaleTimeString() }}
      </time>
      <StatusTag :status-type="status !== 'Complete' ? 'healthy' : 'unhealthy'" minimal-on-mobile>
        {{ STATUS[permission.status].title }}
      </StatusTag>
      <button class="chevron" aria-label="Open Permission Details">
        <ChevronDownIcon />
      </button>
    </header>
    <Transition name="details">
      <PermissionDetails v-if="isOpen" class="permission-details" :permission :status />
    </Transition>
  </li>
</template>

<style scoped>
.permission {
  padding: var(--spacing-sm) var(--spacing-md);
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
      & > :not(button, h2, .icon-wrapper) {
        opacity: 0;
        visibility: hidden;
      }
    }
  }
}

.icon-wrapper {
  padding: 8.5px 6px;
  border: 1px solid var(--eddie-grey-light);
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--eddie-primary);
  border-radius: 0.5rem;
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

.title {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.non-essential {
  display: none;
}
.status {
  display: none;
}

@media screen and (min-width: 640px) {
  .status {
    display: flex;
  }

  .title {
    max-width: 50%;
  }
}

@media screen and (min-width: 1024px) {
  .permission {
    padding: var(--spacing-lg) var(--spacing-xlg);
  }
  .non-essential {
    display: block;
  }
}
</style>
