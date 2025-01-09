<script setup lang="ts">
import EddieIcon from '@/components/EddieIcon.vue'
</script>

<template>
  <aside>
    <EddieIcon class="logo" />

    <nav>
      <RouterLink to="/">
        <span class="pi pi-home"></span>
        Dashboard
      </RouterLink>
      <RouterLink to="/permissions">
        <span class="pi pi-check-circle"></span>
        Permissions
      </RouterLink>
      <RouterLink to="/region-connectors">
        <span class="pi pi-globe"></span>
        Region Connectors
      </RouterLink>
      <RouterLink to="/data-needs">
        <span class="pi pi-briefcase"></span>
        Data Needs
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.logo {
  margin: 0.5rem;
}

nav {
  display: grid;
  gap: 0.5rem;
}

nav a {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  text-decoration: none;
  transition: 0.4s;
  border-radius: 0.25rem;
  white-space: nowrap;
  color: var(--p-text-color);
}

nav a:hover,
nav a.router-link-exact-active {
  background-color: var(--p-highlight-background);
  color: var(--p-highlight-color);
}
</style>
