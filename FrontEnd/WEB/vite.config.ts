import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // catch every /api/* call and send it to your nginx at port 80
      '/api': {
        target: 'http://127.0.0.1:8085',
        changeOrigin: true,
        secure: false,
      },
    },
  },
})
