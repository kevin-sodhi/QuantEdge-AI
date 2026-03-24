import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // POST /api/backtest → Python FastAPI (port 8000)
      '/api': {
        target: 'http://localhost:8000',
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
