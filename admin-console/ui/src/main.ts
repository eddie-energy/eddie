// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
  components: {
    button: {
      root: {
        borderRadius: '0.5rem',
        paddingX: '1rem',
        label: {
          fontWeight: '500'
        }
      },
      colorScheme: {
        light: {
          root: {
            primary: {
              background: '#017aa0',
              color: '#ffffff',
              hoverColor: '#ffffff'
            },
            danger: {
              background: 'var(--danger)',
              color: '#ffffff',
              hoverColor: '#ffffff'
            }
          }
        },
        dark: {
          root: {
            primary: {
              color: '#ffffff',
              hoverColor: '#ffffff',
              activeColor: '#ffffff',
              borderColor: '#3865da',
              hoverBorderColor: '#2954c9',
              activeBorderColor: '#1a43b8',
              background: 'linear-gradient(90deg, #3865da 0%, #6e96fb 100%)',
              hoverBackground: 'linear-gradient(90deg, #2954c9 0%, #5d85ea 100%)',
              activeBackground: 'linear-gradient(90deg, #1a43b8 0%, #4c74d9 100%)'
            },
            danger: {
              color: '#ffffff',
              hoverColor: '#ffffff',
              activeColor: '#ffffff',
              borderColor: '#5f1010',
              hoverBorderColor: '#4e0808',
              activeBorderColor: '#3d0000',
              background: 'linear-gradient(90deg, #c42222 0%, #5f1010 100%)',
              hoverBackground: 'linear-gradient(90deg, #b31111 0%, #4e0808 100%)',
              activeBackground: 'linear-gradient(90deg, #a20000 0%, #3d0000 100%)'
            }
          }
        }
      }
    },
    panel: {
      toggleableHeader: {
        padding: '1.25rem 1.5rem'
      },
      content: {
        padding: '1.25rem 1.5rem'
      },
      footer: {
        padding: '0 1.5rem 1.25rem'
      }
    }
  },
  semantic: {
    primary: palette('#017aa0'),
    colorScheme: {
      light: {
        text: {
          color: '#017aa0'
        },
        content: {
          background: '#fafbfc',
          borderColor: '#017aa0',
          color: '#017aa0'
        },
        primary: {
          color: '#017aa0',
          contrastColor: '#ffffff'
        }
      },
      dark: {
        text: {
          color: '#ffffff'
        },
        content: {
          background: '#202022',
          borderColor: '#2e2e30',
          color: '#ffffff'
        },
        primary: {
          color: '#017aa0',
          contrastColor: '#ffffff'
        }
      }
    },
    content: {
      borderRadius: '0.5rem'
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
