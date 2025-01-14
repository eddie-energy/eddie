import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
const { EDDIE_PUBLIC_URL, EDDIE_MANAGEMENT_URL } = process.env;

export default defineConfig({
  define: {
    EDDIE_PUBLIC_URL: JSON.stringify(EDDIE_PUBLIC_URL ?? "http://localhost:8080"),
    EDDIE_MANAGEMENT_URL: JSON.stringify(EDDIE_MANAGEMENT_URL ?? "http://localhost:9090"),
  },
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
