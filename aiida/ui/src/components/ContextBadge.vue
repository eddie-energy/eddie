<!-- SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at> -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<template>
  <span class="badge">
    <slot />
  </span>
</template>

<style scoped>
.badge {
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--eddie-primary);
  border-radius: var(--border-radius);
  color: var(--light);
  font-weight: var(--font-weight-bold);
  font-size: 0.75rem;
  line-height: 1;
}
</style>
