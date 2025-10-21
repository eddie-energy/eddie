<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import Logo from '@/assets/logo.svg'
import PermissionsNavIcon from '@/assets/icons/PermissionsNavIcon.svg'
import DataSourceIcon from '@/assets/icons/DataSourceIcon.svg'
import AccountIcon from '@/assets/icons/AccountIcon.svg'
import { selectedPermissionCategory } from '@/stores/selectedPermissionCategory'

console.log(navigator.language)
</script>

<template>
  <header class="header">
    <RouterLink to="/">
      <Logo class="logo" />
    </RouterLink>

    <nav class="header-nav">
      <RouterLink
        :data-text="$t('header.permissions')"
        to="/"
        class="link-with-bold-hover nav-link"
        :class="{ 'not-selected': selectedPermissionCategory !== 'outbound-aiida' }"
        @click="selectedPermissionCategory = 'outbound-aiida'"
      >
        <PermissionsNavIcon class="icon outbound" />
        {{ $t('header.permissions') }}
      </RouterLink>
      <RouterLink
        :data-text="$t('header.inbound')"
        to="/?category=inbound-aiida"
        class="link-with-bold-hover nav-link inbound-link"
        @click="selectedPermissionCategory = 'inbound-aiida'"
        :class="{ 'not-selected': selectedPermissionCategory !== 'inbound-aiida' }"
      >
        <PermissionsNavIcon class="icon" />
        {{ $t('header.inbound') }}
      </RouterLink>
      <RouterLink
        :data-text="$t('header.data-sources')"
        to="/data-sources"
        class="link-with-bold-hover nav-link"
      >
        <DataSourceIcon class="icon" />
        {{ $t('header.data-sources') }}
      </RouterLink>
      <RouterLink
        to="/account"
        :data-text="$t('header.account') + 'aaa'"
        class="link-with-bold-hover nav-link"
      >
        <span class="user-profile-link"> <AccountIcon /> {{ $t('header.account') }}</span>
      </RouterLink>
    </nav>
    <div class="locale-changer">
      <select v-model="$i18n.locale">
        <option v-for="locale in $i18n.availableLocales" :key="`locale-${locale}`" :value="locale">
          {{ locale }}
        </option>
      </select>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.logo {
  height: 3rem;
  width: auto;
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

.outbound {
  transform: rotate(180deg);
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
  .icon,
  .inbound-link {
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
