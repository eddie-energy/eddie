<script setup lang="ts">
import STATUS from '@/constants/permission-status'
import { computed, onMounted, ref, watch } from 'vue'
import { fetchPermissions, permissions } from '@/stores/permissions'
import type { AiidaPermission, PermissionTypes } from '@/types'
import Button from '@/components/Button.vue'
import CompleteIcon from '@/assets/icons/CompleteIcon.svg'
import ActiveIcon from '@/assets/icons/ActiveIcon.svg'
import PendingIcon from '@/assets/icons/PendingIcon.svg'
import PermissionDropdown from './PermissionDropdown.vue'
import UpdatePermissionModal from './Modals/UpdatePermissionModal.vue'

const activePermissions = ref<AiidaPermission[]>(
  permissions.value.filter((p) => STATUS[p.status].isActive),
)
const selectedTab = ref<PermissionTypes>('Active')
const showMore = ref(false)

onMounted(async () => {
  await fetchPermissions()
})

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
const initialPermissionsCount = 6

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

const slicedPermissions = computed(() => {
  if (showMore.value) {
    return activePermissions.value
  }
  return activePermissions.value.slice(0, initialPermissionsCount)
})

const handleTabClick = (tab: 'Active' | 'Pending' | 'Complete') => {
  showMore.value = false
  selectedTab.value = tab
}

const refetchPermissions = async () => {
  await fetchPermissions()
}
</script>

<template>
  <div class="permission-list-wrapper">
    <UpdatePermissionModal @update="refetchPermissions" />
    <div class="permission-tabs">
      <Button
        v-for="tab in tabs"
        button-style="secondary"
        :key="tab.name"
        @click="handleTabClick(tab.name as 'Active' | 'Pending' | 'Complete')"
        :class="{ active: selectedTab === tab.name }"
      >
        <component :is="tab.icon" /> {{ tab.name }}
      </Button>
    </div>
    <TransitionGroup tag="ul" name="permissions" class="permission-list">
      <PermissionDropdown
        v-for="permission in slicedPermissions"
        :key="permission.permissionId"
        :permission
        :status="selectedTab"
      />
      <Button
        v-if="activePermissions.length > initialPermissionsCount"
        button-style="secondary"
        @click="showMore = !showMore"
        class="show-more-button"
      >
        {{ showMore ? 'Show Less Permissions' : 'Load More Permissions' }}
      </Button>
    </TransitionGroup>
  </div>
</template>

<style scoped>
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
  transform: translateY(30px);
}

.permissions-leave-active {
  position: absolute;
  width: 100%;
}

.permission-list-wrapper {
  display: grid;
  grid-template-columns: 1fr 4fr;
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
  height: 75vh;
  max-height: 75vh;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-color: var(--eddie-primary) var(--light);
  scrollbar-gutter: stable;
}

.show-more-button {
  grid-column: span 2;
  width: fit-content;
  justify-self: end;
}
</style>
