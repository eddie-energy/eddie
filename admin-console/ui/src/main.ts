import { createApp } from 'vue'
import App from './App.vue'
import '@fontsource-variable/inter'
import 'modern-normalize/modern-normalize.css'

import './assets/main.css'

import router from './router'

import { ConfirmationService, ToastService, Tooltip } from 'primevue'
import PrimeVue from 'primevue/config'
import Aura from '@primevue/themes/aura'
import { definePreset, palette } from '@primevue/themes'

const app = createApp(App)

const EddiePreset = definePreset(Aura, {
  semantic: {
    primary: palette('#017aa0')
  }
})

app.use(router)
app.use(PrimeVue, {
  theme: {
    preset: EddiePreset
  }
})
app.use(ConfirmationService)
app.use(ToastService)

app.directive('tooltip', Tooltip)

app.mount('#app')
