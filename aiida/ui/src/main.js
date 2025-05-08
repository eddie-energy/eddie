import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { keycloak } from '@/keycloak.js'

try {
  const authenticated = await keycloak.init({
    onLoad: 'login-required',
    checkLoginIframe: false,
  })

  if (authenticated) {
    console.log('User is authenticated')

    const app = createApp(App)

    app.use(router)

    app.mount('#app')
  } else {
    console.error('User is not authenticated')
    window.location.reload()
  }
} catch (error) {
  console.error('Failed to initialize Keycloak adapter:', error)
}
