import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { keycloak } from '@/keycloak.js'

keycloak
  .init({
    onLoad: 'login-required',
    checkLoginIframe: false,
  })
  .catch((error) => {
    console.error('Failed to initialize Keycloak adapter:', error)
  })
  .then((authenticated) => {
    if (authenticated) {
      console.log('User is authenticated')
      createApp(App).use(router).mount('#app')
    } else {
      console.error('User is not authenticated')
      window.location.reload()
    }
  })
