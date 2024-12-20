<script setup lang="ts">
import EddieIcon from '@/components/EddieIcon.vue'
import {
  BriefcaseIcon,
  CheckBadgeIcon,
  GlobeEuropeAfricaIcon,
  HomeIcon
} from '@heroicons/vue/24/solid'
</script>

<template>
  <aside>
    <RouterLink to="/">
      <EddieIcon class="logo" />
    </RouterLink>

    <nav>
      <RouterLink to="/">
        <HomeIcon />
        Dashboard
      </RouterLink>
      <RouterLink to="/permissions">
        <CheckBadgeIcon />
        Permissions
      </RouterLink>
      <RouterLink to="/region-connectors">
        <GlobeEuropeAfricaIcon />
        Region Connectors
      </RouterLink>
      <RouterLink to="/data-needs">
        <BriefcaseIcon />
        Data Needs
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.logo {
  margin-left: 0.5rem;
  margin-bottom: 1rem;
  color: var(--color-text);
}

nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

nav a {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--font-size-lg);
  padding: 0.5rem;
  text-decoration: none;
  transition: 0.4s;
  color: var(--color-text);
  border-radius: 0.25rem;
}

nav a:hover,
nav a.router-link-exact-active {
  background-color: var(--color-accent-bg);
  color: var(--color-accent);
}

nav a svg {
  height: 1.5em;
}
</style>
