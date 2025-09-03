<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import UserProfile from '@/components/UserProfile.vue'
import Logo from '@/assets/logo.svg'
import { BASE_URL } from '@/api.js'

const paths = [
  ['/', 'Permissions'],
  ['/data-sources', 'Data Sources'],
]
</script>

<template>
  <header class="header">
    <RouterLink to="/">
      <Logo class="logo" />
    </RouterLink>

    <nav class="header-nav">
      <RouterLink
        :data-text="name"
        :to="path"
        v-for="[path, name] in paths"
        class="link-with-bold-hover"
        :key="path"
      >
        {{ name }}
      </RouterLink>
      <a
        data-text="Services"
        :href="`${BASE_URL}/installer`"
        target="_blank"
        class="link-with-bold-hover"
      >
        Services
      </a>
      <UserProfile />
    </nav>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 2rem;
}

.logo {
  height: 3rem;
}

.header-nav {
  display: flex;
  gap: 2.5rem;
  flex-wrap: wrap;
  color: var(--eddie-primary);
}
</style>
