import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  // S'assurer que le Service Worker est copié dans le build
  publicDir: 'public',
  build: {
    rollupOptions: {
      input: {
        main: './index.html',
      }
    }
  }
})
