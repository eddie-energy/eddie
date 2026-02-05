// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView
    },
    {
      path: '/permissions',
      name: 'permissions',
      component: () => import('../views/PermissionsView.vue')
    },
    {
      path: '/region-connectors',
      name: 'region-connectors',
      component: () => import('../views/RegionConnectorsView.vue')
    },
    {
      path: '/data-needs',
      name: 'data-needs',
      component: () => import('../views/DataNeedsView.vue')
    }
  ]
})

export default router
