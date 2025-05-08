<script setup>
import { keycloak } from '@/keycloak.js'
import { ref } from 'vue'

const firstName = ref('')
const lastName = ref('')
const username = ref('')

keycloak.loadUserProfile().then((user) => {
  firstName.value = user.firstName
  lastName.value = user.lastName
  username.value = user.username
})
</script>

<template>
  <div class="wrapper">
    <sl-avatar :initials="firstName[0] + lastName[0]"></sl-avatar>
    <div class="text">
      <strong>{{ firstName }} {{ lastName }}</strong>
      <br />
      <span>{{ username }}</span>
    </div>
  </div>
</template>

<style scoped>
.wrapper {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;

  @media (min-width: 600px) {
    position: absolute;
    right: 2rem;
    top: 2rem;
  }
}

.text {
  line-height: 1.25;
}
</style>
