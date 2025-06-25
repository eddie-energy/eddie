<script setup>
import { RouterLink, RouterView } from 'vue-router'
import UserProfile from '@/components/UserProfile.vue'
import PermissionDialog from '@/components/PermissionDialog.vue'
</script>

<template>
  <PermissionDialog />

  <header>
    <h1>
      <img alt="AIIDA" src="@/assets/logo.svg" height="64" />
    </h1>

    <UserProfile />

    <nav>
      <RouterLink to="/">Dashboard</RouterLink>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped></style>
