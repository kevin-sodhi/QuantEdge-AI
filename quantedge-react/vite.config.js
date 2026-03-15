import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // POST /api/backtest → Node.js Express (port 3000, HTTPS)
      '/api': {
        target:  'https://localhost:3000',
        secure:  false,   // accept self-signed cert
        changeOrigin: true,
      },
      // GET /python/... → Python FastAPI (port 8000)
      '/python': {
        target:  'http://localhost:8000',
        rewrite: (path) => path.replace(/^\/python/, ''),
        changeOrigin: true,
      },
    },
  },
});
