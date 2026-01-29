<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import type { AiidaPermission, PermissionTypes } from '@/types'
import PermissionIcon from '@/assets/icons/PermissionIcon.svg'
import ChevronDownIcon from '@/assets/icons/ChevronDownIcon.svg'
import StatusTag from './StatusTag.vue'
import { ref } from 'vue'
import PermissionDetails from './PermissionDetails.vue'
import { useI18n } from 'vue-i18n'

const { permission, status } = defineProps<{
  permission: AiidaPermission
  status?: PermissionTypes
}>()
const { t } = useI18n()

const isOpen = ref(false)
</script>

<template>
  <li class="permission" :class="{ 'is-open': isOpen }">
    <header class="permission-header" @click="isOpen = !isOpen">
      <div class="header-title">
        <PermissionIcon class="icon" />
        <h2 class="heading-5 title">{{ permission.serviceName }}</h2>
      </div>
      <p v-if="permission.unimplemented" class="small-data-graph">Placeholder</p>
      <div class="non-essential">
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
      </div>
      <StatusTag :status-type="status !== 'Complete' ? 'healthy' : 'unhealthy'" minimal-on-mobile>
        {{ t(permission.status) }}
      </StatusTag>
      <button class="chevron" :aria-label="t('permissions.dropdown.openDetails')">
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
  border-radius: var(--border-radius);

  &.is-open {
    .chevron {
      transform: rotate(180deg);
    }
    .permission-header {
      margin-bottom: var(--spacing-lg);
      & > :not(button, .header-title) {
        opacity: 0;
        visibility: hidden;
      }
    }
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-xlg);
  justify-self: start;

  width: 100%;

  .title {
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.permission-header {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr)) auto auto;
  align-items: center;
  justify-items: center;
  width: 100%;
  gap: var(--spacing-xlg);
  cursor: pointer;
  transition: margin-bottom 0.3s ease-out;
  > * {
    transition: opacity 0.5s ease-in;
  }
}

.icon {
  min-width: var(--spacing-xxl);
  min-height: var(--spacing-xxl);
}

.chevron {
  cursor: pointer;
  justify-self: end;
  align-self: flex-end;
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

  .permission-header {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  }
}

@media screen and (min-width: 1024px) {
  .permission {
    padding: var(--spacing-lg) var(--spacing-xlg);
  }
  .permission-header {
    grid-template-columns: minmax(0, 1.5fr) repeat(2, minmax(0, 1fr)) auto;
  }

  .non-essential {
    display: flex;
    gap: var(--spacing-sm);
  }
}
</style>
