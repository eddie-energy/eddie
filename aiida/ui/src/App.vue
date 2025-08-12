<script setup>
import { RouterLink, RouterView, useRoute } from 'vue-router'
import UserProfile from '@/components/UserProfile.vue'
import PermissionDialog from '@/components/PermissionDialog.vue'
import { BASE_URL } from '@/api.js'

const paths = [
  ['/', 'Permissions', 'key'],
  ['/data-sources', 'Data Sources', 'outlet'],
]

const route = useRoute()

function isActive(path) {
  return route.matched.some((matched) => matched.path === path)
}
</script>

<template>
  <PermissionDialog />

  <header>
    <h1>
      <RouterLink to="/">
        <img alt="AIIDA" src="@/assets/logo.svg" height="64" />
      </RouterLink>
    </h1>

    <UserProfile />

    <nav>
      <RouterLink :to="path" v-for="[path, name, icon] in paths">
        <sl-button :variant="isActive(path) ? 'primary' : 'default'">
          <sl-icon slot="prefix" :name="icon"></sl-icon>
          {{ name }}
        </sl-button>
      </RouterLink>
      <sl-button :href="BASE_URL + '/installer'">
        <sl-icon slot="prefix" name="boxes"></sl-icon>
        Services
      </sl-button>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
nav {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

img {
  max-width: 100%;
}
</style>
