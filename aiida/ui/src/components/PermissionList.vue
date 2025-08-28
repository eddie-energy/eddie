<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import { onMounted, ref, watch } from 'vue'
import { fetchPermissions, permissions } from '@/stores/permissions'
import type { AiidaPermission } from '@/types'
import Button from '@/components/Button.vue'
import CompleteIcon from '@/assets/icons/CompleteIcon.svg'
import ActiveIcon from '@/assets/icons/ActiveIcon.svg'
import PendingIcon from '@/assets/icons/PendingIcon.svg'
import PermissionDropdown from './PermissionDropdown.vue'

onMounted(async () => {
  await fetchPermissions()
})

const activePermissions = ref<AiidaPermission[]>(
  permissions.value.filter((p) => STATUS[p.status].isActive),
)

const selectedTab = ref('Active')

const tabs = [
  {
    name: 'Active',
    icon: ActiveIcon,
  },
  {
    name: 'Pending',
    icon: PendingIcon,
  },
  {
    name: 'Complete',
    icon: CompleteIcon,
  },
]

watch([selectedTab, permissions], () => {
  if (selectedTab.value === 'Active') {
    activePermissions.value = permissions.value.filter((p) => STATUS[p.status].isActive)
  } else if (selectedTab.value === 'Pending') {
    activePermissions.value = permissions.value.filter((p) => STATUS[p.status].isOpen)
  } else {
    activePermissions.value = permissions.value.filter(
      (p) => !STATUS[p.status].isOpen && !STATUS[p.status].isActive,
    )
  }
})
</script>

<template>
  <div class="permission-list-wrapper">
    <div class="permission-tabs">
      <Button
        v-for="tab in tabs"
        button-style="secondary"
        :key="tab.name"
        @click="selectedTab = tab.name"
        :class="{ active: selectedTab === tab.name }"
      >
        <component :is="tab.icon" /> {{ tab.name }}</Button
      >
    </div>
    <TransitionGroup tag="ul" name="permissions" class="permission-list">
      <PermissionDropdown
        v-for="permission in activePermissions"
        :key="permission.permissionId"
        :permission="permission"
        :status="selectedTab === 'Complete' ? 'unhealthy' : 'healthy'"
      />
    </TransitionGroup>
  </div>
</template>

<style scoped>
.permission-list-wrapper {
  display: grid;
  grid-template-columns: 1fr 3fr;
  gap: var(--spacing-xlg);
}
.permission-tabs > * {
  width: 100%;
  margin-bottom: var(--spacing-md);
}
.active {
  pointer-events: none;
  background-color: var(--eddie-primary);
  color: var(--light);
}
.permission-list {
  position: relative;
}

.permissions-move,
.permissions-enter-active,
.permissions-leave-active {
  transition:
    transform 0.5s ease,
    opacity 0.5s ease;
}

.permissions-enter-from,
.permissions-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.permissions-leave-active {
  position: absolute;
  width: 100%;
}
</style>
