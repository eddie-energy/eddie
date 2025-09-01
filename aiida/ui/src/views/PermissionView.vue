<script setup lang="ts">
import PermissionList from '@/components/PermissionList.vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'
import AddPermissionModal from '@/components/Modals/AddPermissionModal.vue'
import { ref } from 'vue'

const permissionModalRef = ref<HTMLDialogElement>()

const showAddPermissionModal = () => {
  permissionModalRef.value?.showModal()
}
</script>

<template>
  <main>
    <div class="two-item-pair bottom-margin">
      <h1 class="heading-2">Permissions</h1>
      <Button @click="showAddPermissionModal"><PlusIcon />Add Permission</Button>
    </div>

    <PermissionList />
    <AddPermissionModal ref="permissionModalRef" />
  </main>
</template>

<style scoped>
.bottom-margin {
  margin-bottom: var(--spacing-xxl);
}

.two-item-pair {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
