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
import PermissionsNavIcon from '@/assets/icons/PermissionsNavIcon.svg'
import { selectedPermissionCategory } from '@/stores/selectedPermissionCategory'

const selectedTab = ref<PermissionTypes>('Active')
const showMore = ref(false)
const scrollTarget = ref()
const activePermissionCategory = ref<AiidaPermission[]>(
  permissions.value.filter((p) => p.dataNeed.type === selectedPermissionCategory.value),
)

const activePermissions = ref<AiidaPermission[]>(
  activePermissionCategory.value.filter((p) => STATUS[p.status].isActive),
)

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

watch([selectedTab, permissions, selectedPermissionCategory], () => {
  activePermissionCategory.value = permissions.value.filter(
    (p) => p.dataNeed.type === selectedPermissionCategory.value,
  )
  if (selectedTab.value === 'Active') {
    activePermissions.value = activePermissionCategory.value.filter(
      (p) => STATUS[p.status].isActive,
    )
  } else if (selectedTab.value === 'Pending') {
    activePermissions.value = activePermissionCategory.value.filter((p) => STATUS[p.status].isOpen)
  } else {
    activePermissions.value = activePermissionCategory.value.filter(
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

const handleShowMore = () => {
  showMore.value = !showMore.value
  if (!showMore.value) {
    window.scrollTo({
      top: (scrollTarget.value as HTMLElement).offsetTop - 100,
      behavior: 'smooth',
    })
  }
}

const handleCategoryChange = (category: string) => {
  selectedPermissionCategory.value = category
}
</script>

<template>
  <div>
    <UpdatePermissionModal @update="refetchPermissions" />
    <div class="category-tabs">
      <button
        @click="handleCategoryChange('outbound-aiida')"
        class="text-normal"
        :class="{ 'active-category': selectedPermissionCategory === 'outbound-aiida' }"
      >
        <PermissionsNavIcon class="rotate" /> Outbound Permissions
      </button>
      <button
        @click="handleCategoryChange('inbound-aiida')"
        :class="{ 'active-category': selectedPermissionCategory === 'inbound-aiida' }"
        class="text-normal"
      >
        <PermissionsNavIcon /> Inbound Permissions
      </button>
    </div>
    <div class="permission-list-wrapper">
      <div class="permission-tabs">
        <Button
          v-for="tab in tabs"
          button-style="secondary"
          :key="tab.name"
          @click="handleTabClick(tab.name as 'Active' | 'Pending' | 'Complete')"
          :class="{ active: selectedTab === tab.name }"
        >
          <component :is="tab.icon" class="icon" /> {{ tab.name }}
        </Button>
      </div>
      <TransitionGroup tag="ul" name="permissions" class="permission-list" ref="scrollTarget">
        <PermissionDropdown
          v-for="permission in slicedPermissions"
          :key="permission.permissionId"
          :permission
          :status="selectedTab"
        />
        <Button
          v-if="activePermissions.length > initialPermissionsCount"
          button-style="secondary"
          @click="handleShowMore"
          class="show-more-button"
        >
          {{ showMore ? 'Show Less Permissions' : 'Load More Permissions' }}
        </Button>
      </TransitionGroup>
    </div>
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

.icon {
  min-width: 1rem;
}

.permission-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xlg);
  border-radius: 0.5rem;
  margin-bottom: calc(var(--mobile-header-height) / 1.5);
}

.permission-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  border-radius: 0.5rem 0.5rem 0 0;
  overflow: hidden;
  height: fit-content;
  min-height: fit-content;

  > * {
    width: 100%;
    height: 100%;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border-radius: unset;
    border: unset;
    border-bottom: 2px solid transparent;
    transition: border-color 0.3s ease-in-out;

    &:hover {
      background-color: var(--light);
      color: var(--eddie-primary);
    }
  }

  .active {
    border-bottom-color: var(--eddie-primary);
  }
}

.permission-list {
  position: relative;
  max-height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-color: var(--eddie-primary) var(--light);
  scrollbar-gutter: stable;
}

.show-more-button {
  grid-column: span 2;
  width: 100%;
  justify-content: center;
  justify-self: end;
}

.category-tabs {
  display: none;
}

.rotate {
  transform: rotate(180deg);
}

@media screen and (min-width: 1024px) {
  .category-tabs {
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-column: span 2;
    background: #017aa026;

    border-radius: var(--border-radius) var(--border-radius) 0 0;

    button {
      display: flex;
      align-items: center;
      gap: var(--spacing-md);
      text-align: left;
      color: var(--eddie-primary);
      cursor: pointer;
      border-radius: var(--border-radius) var(--border-radius) 0 0;
      padding: var(--spacing-lg) var(--spacing-xxl);
      border: 1px solid transparent;
      border-bottom: 1px solid var(--eddie-primary);
      transition:
        background-color 0.3s ease-in-out,
        border-color 0.3s ease-in-out;

      &.active-category {
        border: 1px solid var(--eddie-primary);
        border-bottom-color: transparent;
        background-color: var(--light);
      }
    }
  }
  .permission-list-wrapper {
    padding: var(--spacing-lg) var(--spacing-xxl);
    display: grid;
    background-color: var(--light);
    border: 1px solid var(--eddie-primary);
    border-top: none;
    border-radius: 0 0 var(--border-radius) var(--border-radius);
    grid-template-columns: 1fr 6fr;
    grid-template-rows: min-content;
  }
  .permission-list {
    padding: var(--spacing-md) 0.75rem;
  }
  .permission-tabs {
    display: block;
    .active {
      pointer-events: none;
      background-color: var(--eddie-primary);
      color: var(--light);
    }
    > * {
      width: 100%;
      flex-direction: row;
      height: fit-content;
      border-radius: 2rem;
      border: 1px solid var(--eddie-primary);
      margin-bottom: var(--spacing-md);
    }
  }
  .show-more-button {
    width: fit-content;
    justify-content: flex-start;
  }
}
@media screen and (min-width: 1620px) {
  .permission-list-wrapper {
    grid-template-columns: 1fr 4fr;
  }
}
</style>
