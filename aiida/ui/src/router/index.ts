// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { createRouter, createWebHistory } from 'vue-router'
import PermissionView from '@/views/PermissionView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/:pathMatch(.*)*',
      name: 'home',
      component: PermissionView,
    },
    {
      path: '/data-sources',
      name: 'data-sources',
      component: () => import('../views/DataSourceView.vue'),
    },
    {
      path: '/account',
      name: 'account',
      component: () => import('../views/AccountView.vue'),
    },
    {
      path: '/connection-monitor',
      name: 'connection-monitor',
      component: () => import('../views/ConnectionMonitorView.vue'),
    },
  ],
})

export default router
