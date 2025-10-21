<script setup lang="ts">
import GlobeIcon from '@/assets/icons/GlobeIcon.svg'
import { useI18n } from 'vue-i18n'
const { locale, availableLocales } = useI18n()
</script>

<template>
  <div class="wrapper" tabindex="0">
    <div class="lang-switch text-normal" :data-text="locale">
      {{ locale.toUpperCase() }}
      <GlobeIcon />
    </div>
    <div class="dropdown">
      <button
        v-for="loc in availableLocales"
        :key="loc"
        @click="locale = loc"
        class="locale text-normal"
        :class="{ current: loc === locale }"
      >
        {{ loc.toUpperCase() }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  position: relative;
}
.wrapper:hover > .dropdown,
.wrapper:focus-within > .dropdown {
  display: flex;
}
.lang-switch {
  color: var(--eddie-primary);
  display: inline-flex;
  gap: var(--spacing-sm);
  align-items: center;
  cursor: pointer;
  svg {
    width: 1rem;
    height: 1rem;
  }
}
.locale {
  cursor: pointer;
  transition:
    color 0.3s ease,
    font-weight 0.3s ease,
    text-decoration 0.3s ease;
  &:hover,
  &:focus {
    color: var(--eddie-primary);
    font-weight: bold;
    text-decoration: underline;
  }
}
.dropdown {
  display: none;
  position: absolute;
  margin-top: var(--spacing-sm);
  top: 100%;
  right: 0;
  flex-direction: column;
  gap: var(--spacing-sm);
  background-color: var(--light);
  padding: var(--spacing-md);
  border-radius: var(--border-radius);
}
.current {
  color: var(--eddie-primary);
  font-weight: var(--font-weight-bold);
  cursor: unset;
  &:hover,
  &:focus {
    text-decoration: none;
  }
}

@media screen and (min-width: 1024px) {
  .dropdown {
    margin-top: unset;
  }
  .lang-switch {
    margin-bottom: var(--spacing-sm);
    border-bottom: 1px solid transparent;
  }
}
</style>
