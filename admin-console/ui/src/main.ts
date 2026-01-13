import { createApp } from 'vue'
import App from './App.vue'
import '@fontsource-variable/inter'
import 'modern-normalize/modern-normalize.css'

import './assets/main.css'

import router from './router'

import PrimeVue from 'primevue/config'
import { definePreset, palette } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

import { ConfirmationService, ToastService, Tooltip } from 'primevue'

const app = createApp(App)

const EddiePreset = definePreset(Aura, {
  semantic: {
    primary: palette('#017aa0'),
    colorScheme: {
      light: {
        content: {
          color: '#017AA0'
        },
        primary: {
          color: '#017AA0'
        }
      },
      dark: {
        content: {
          color: '#ffffff'
        }
      }
    }
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
