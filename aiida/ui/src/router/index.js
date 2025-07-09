import { createRouter, createWebHistory } from 'vue-router'
import PermissionView from '@/views/PermissionView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
  ],
})

export default router
