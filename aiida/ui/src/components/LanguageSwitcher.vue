<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import GlobeIcon from '@/assets/icons/GlobeIcon.svg'
import { useI18n } from 'vue-i18n'

const { locale, availableLocales } = useI18n()

const localizedLanguageNames = availableLocales.map((loc) => {
  return {
    loc,
    name: new Intl.DisplayNames([loc], { type: 'language' }).of(loc),
  }
})
</script>

<template>
  <div class="wrapper" tabindex="0">
    <div class="lang-switch text-normal" :data-text="locale">
      {{ new Intl.DisplayNames([locale], { type: 'language' }).of(locale) }}
      <GlobeIcon />
    </div>
    <div class="dropdown">
      <button
        v-for="loc in localizedLanguageNames"
        :data-text="loc.name"
        :key="loc.loc"
        @click="locale = loc.loc"
        class="locale text-normal"
        :class="{ current: loc.loc === locale }"
      >
        {{ loc.name }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  position: relative;
  text-transform: capitalize;
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
  display: flex;
  flex-direction: column;
  text-transform: capitalize;
  cursor: pointer;
  transition:
    color 0.3s ease,
    font-weight 0.3s ease,
    text-decoration 0.3s ease;
  &:after {
    content: attr(data-text);
    height: 0;
    visibility: hidden;
    overflow: hidden;
    user-select: none;
    pointer-events: none;
    font-weight: var(--font-weight-bold);
    @media speech {
      display: none;
    }
  }
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
  align-items: flex-start;
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
