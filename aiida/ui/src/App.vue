<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <h1>
      <img alt="AIIDA" src="@/assets/logo.svg" height="64" />
    </h1>

    <UserProfile />

    <nav>
      <RouterLink to="/">Dashboard</RouterLink>
    </nav>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped></style>
