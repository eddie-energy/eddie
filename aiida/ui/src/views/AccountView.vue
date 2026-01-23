<!--
SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
SPDX-License-Identifier: Apache-2.0
-->

<script setup lang="ts">
import { keycloak } from '@/keycloak'
import { ref } from 'vue'
import { getApplicationInformation } from '@/api'
import Button from '@/components/Button.vue'
import AccountIcon from '@/assets/icons/AccountIcon.svg'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const firstName = ref('')
const lastName = ref('')
const username = ref('')
const aiidaId = ref('')
const userAvatar = ref('')

keycloak.loadUserProfile().then((user) => {
  firstName.value = user.firstName ?? ''
  lastName.value = user.lastName ?? ''
  username.value = user.username ?? ''
})

getApplicationInformation().then((data) => {
  aiidaId.value = data.aiidaId
})
</script>

<template>
  <main class="account-view">
    <h1 class="heading-2 title">{{ t('account.title') }}</h1>
    <div class="user-profile">
      <div class="profile-header">
        <img v-if="userAvatar" :src="userAvatar" />
        <div v-else class="account-icon">
          <AccountIcon />
        </div>
        <div>
          <h2 class="heading-3">{{ username }}</h2>
          <p>{{ firstName }} {{ lastName }}</p>
        </div>
      </div>
      <dl class="user-info">
        <div class="info-field">
          <dt>AIIDA ID</dt>
          <dd>{{ aiidaId }}</dd>
        </div>
      </dl>
      <div class="user-buttons">
        <Button
          button-style="secondary"
          @click="keycloak.accountManagement()"
          class="button settings-button"
        >
          {{ t('account.settings') }}
        </Button>
        <Button button-style="error-secondary" @click="keycloak.logout()" class="button">
          {{ t('account.logout') }}
        </Button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.title {
  margin-bottom: var(--spacing-xxl);
}
.user-profile {
  min-height: 20vh;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  background: linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0.9) 100%);
  border: 1px solid var(--eddie-primary);
  padding: var(--spacing-md);
  border-radius: var(--border-radius);
}
.profile-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.account-icon {
  color: var(--eddie-primary);
  display: flex;
  padding: var(--spacing-sm);
  border-radius: var(--border-radius);
  border: 1px solid var(--eddie-grey-light);
}

.info-field {
  display: flex;
  justify-content: space-between;
  flex-direction: column;

  border: 1px solid var(--eddie-grey-medium);
  color: var(--eddie-grey-medium);
  padding: var(--spacing-sm) var(--spacing-sm);
  border-radius: var(--border-radius);
  font-size: 1rem;
  line-height: 1.5;
  word-break: break-all;
  gap: 0.25rem;

  dd {
    line-height: 1;
    color: var(--eddie-grey-medium);
    font-weight: 600;
  }
}
.user-buttons {
  margin-top: auto;
}
.settings-button {
  margin-bottom: var(--spacing-lg);
}
.button {
  width: 100%;
  justify-content: center;
}

@media screen and (min-width: 1024px) {
  .account-view {
    height: fit-content;
    min-height: 40vh;
  }
  .user-profile {
    background: unset;
    border: unset;
    padding: unset;
  }
  .user-buttons {
    display: flex;
    width: 100%;
    justify-content: space-between;
  }
  .button {
    width: fit-content;
    justify-content: flex-start;
  }
  .info-field {
    flex-direction: row;
    align-items: center;
  }
}
</style>
