import { createRouter, createWebHistory } from 'vue-router'
import PermissionView from '@/views/PermissionView.vue'

const router = createRouter({
  history: createWebHistory(THYMELEAF_AIIDA_PUBLIC_URL ? '/vue/' : undefined),
  routes: [
    {
      path: '/',
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
      path: '/:pathMatch(.*)*',
      component: PermissionView,
    },
  ],
})

export default router
