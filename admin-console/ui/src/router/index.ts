import { createMemoryHistory, createRouter } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'

const router = createRouter({
  history: createMemoryHistory(),
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
