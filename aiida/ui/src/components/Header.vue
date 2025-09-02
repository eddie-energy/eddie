<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import UserProfile from '@/components/UserProfile.vue'
import Logo from '@/assets/logo.svg'
import { BASE_URL } from '@/api.js'
import PermissionsNavIcon from '@/assets/icons/PermissionsNavIcon.svg'
import DataSourceIcon from '@/assets/icons/DataSourceIcon.svg'
import DataSinkIcon from '@/assets/icons/DataSinkIcon.svg'
import AccountIcon from '@/assets/icons/AccountIcon.svg'
import { useTemplateRef } from 'vue'

const userProfile = useTemplateRef('userProfile')

const paths = [
  ['/', 'Permissions', PermissionsNavIcon],
  ['/data-sources', 'Data Sources', DataSourceIcon],
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
        v-for="[path, name, icon] in paths"
        class="link-with-bold-hover nav-link"
        :key="path"
      >
        <component :is="icon" class="icon" />
        {{ name }}
      </RouterLink>
      <a
        data-text="Services"
        :href="`${BASE_URL}/installer`"
        target="_blank"
        class="link-with-bold-hover nav-link"
      >
        <DataSinkIcon class="icon" />
        Data Sinks
      </a>
      <a
        @click="userProfile?.drawer.show()"
        data-text="Account"
        class="link-with-bold-hover nav-link"
      >
        <span class="user-profile-link"> <AccountIcon /> Account</span>
      </a>
      <UserProfile ref="userProfile" />
    </nav>
  </header>
</template>

<style scoped>
.logo {
  height: 3rem;
}

.header-nav {
  max-height: var(--mobile-header-height);
  position: fixed;
  bottom: 0;
  left: 0;
  display: grid;
  align-items: center;
  grid-template-columns: repeat(4, 1fr);
  width: 100%;
  color: var(--light);
  background-color: var(--eddie-primary);
  z-index: 1;
}

.nav-link {
  display: flex;
  flex-direction: column;
  padding: var(--spacing-md) var(--spacing-sm);
  font-weight: 500;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);

  height: 100%;
}

.user-profile-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-direction: column;
}

@media screen and (min-width: 1024px) {
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .nav-link {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--spacing-sm);
    padding: unset;
    cursor: pointer;
  }
  .icon {
    display: none;
  }
  .user-profile-link {
    flex-direction: row-reverse;
  }
  .header-nav {
    position: unset;
    background-color: unset;
    justify-content: end;
    display: flex;
    gap: 2.5rem;
    flex-wrap: wrap;
    color: var(--eddie-primary);
  }
}
</style>
