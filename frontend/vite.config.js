import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El backend (SecurityConfig) todavía no expone CORS para el origen de Vite, así que en
// desarrollo se evita el problema por completo con este proxy: el navegador solo habla con
// :5173, y Vite reenvía /api al backend real. En producción, servir front y back tras el mismo
// origen (o agregar CORS en el backend) resuelve lo mismo.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
