<script setup lang="ts">
import PermissionList from '@/components/PermissionList.vue'
import AiidaCodeInput from '@/components/AiidaCodeInput.vue'
import Button from '@/components/Button.vue'
import PlusIcon from '@/assets/icons/PlusIcon.svg'

const addPermission = () => {
  console.log('addPermission')
}
</script>

<template>
  <main>
    <div class="title-pair">
      <h1 class="heading-2">Permissions</h1>
      <Button @click="addPermission"><PlusIcon />Add Permission</Button>
    </div>

    <PermissionList />

    <AiidaCodeInput :class="'input'" />
  </main>
</template>

<style scoped>
.title-pair {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-xxl);
}
</style>
