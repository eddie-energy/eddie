<!--
SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import EddieIcon from '@/components/EddieIcon.vue'
</script>

<template>
  <aside>
    <header>
      <RouterLink to="/">
        <EddieIcon class="logo" />
        Admin Console
      </RouterLink>
    </header>

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
      <RouterLink to="/configuration">
        <span class="pi pi-cog"></span>
        Configuration
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
aside {
  padding: 1.75rem 1.25rem;
  background: var(--sidebar-background);
}

header a {
  color: var(--text-color);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
}

nav {
  margin-top: 4.5rem;
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
