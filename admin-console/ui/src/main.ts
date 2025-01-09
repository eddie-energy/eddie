import { createApp } from 'vue'
import App from './App.vue'
import '@fontsource/poppins'
import './assets/normalize.css'
import './assets/main.css'

import router from './router'

import { ConfirmationService, ToastService } from 'primevue'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { definePreset, palette } from '@primevue/themes'

const app = createApp(App)

app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: Aura
  }
})
app.use(ConfirmationService)
app.use(ToastService)

app.mount('#app')
