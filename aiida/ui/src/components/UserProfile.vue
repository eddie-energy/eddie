<script setup lang="ts">
import { keycloak } from '@/keycloak'
import { ref, useTemplateRef } from 'vue'
import { getApplicationInformation } from '@/api'

const firstName = ref('')
const lastName = ref('')
const username = ref('')
const aiidaId = ref('')

const drawer = useTemplateRef('drawer')

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
  <a @click="drawer.show()" data-text="Account" class="link-with-bold-hover user-profile-link">
    <span class="user-profile-link">Account <AccountIcon /></span>
  </a>

  <sl-drawer ref="drawer" no-header>
    <div class="header">
      <div class="profile">
        <sl-avatar :initials="firstName[0] + lastName[0]"></sl-avatar>
        <div>
          <strong>{{ firstName }} {{ lastName }}</strong>
          <br />
          <span>{{ username }}</span>
        </div>
      </div>

      <sl-icon-button name="x-lg" label="close" @click="drawer.hide()"></sl-icon-button>
    </div>

    <div class="actions">
      <sl-button variant="primary" outline @click="keycloak.accountManagement()">
        <template v-slot:prefix>
          <sl-icon name="person-fill"></sl-icon>
        </template>
        Account settings
      </sl-button>

      <sl-button variant="danger" outline @click="keycloak.logout()">
        <template v-slot:prefix>
          <sl-icon name="box-arrow-in-right"></sl-icon>
        </template>
        Logout
      </sl-button>
    </div>

    <small>
      <strong>AIIDA ID: </strong>
      <span>{{ aiidaId }}</span>
    </small>
  </sl-drawer>
</template>

<style scoped>
.user-profile-link::after {
  padding-right: 2em;
}

.header {
  display: flex;
  justify-content: space-between;
}

.profile {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.profile div {
  line-height: 1.25;
}

.actions {
  display: grid;
  gap: 0.5rem;
}

sl-drawer::part(body) {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 2rem;
}

small {
  align-self: end;
}

button {
  all: unset;
  position: absolute;
  top: 2rem;
  right: 2rem;
  cursor: pointer;
}
</style>
