import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development the React app calls /api/... and Vite forwards it to Spring Boot,
// so the browser sees one origin (the refresh cookie then works without CORS settings).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.API_URL || 'http://localhost:8080', changeOrigin: false } },
  },
})
