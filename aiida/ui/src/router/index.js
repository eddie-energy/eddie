import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import DataSourceView from '@/views/DataSourceView.vue'
import ServiceView from '@/views/ServiceView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/data-sources',
      name: 'data-sources',
      component: DataSourceView,
    },
    {
      path: '/services',
      name: 'services',
      component: ServiceView,
    },
  ],
})

export default router
