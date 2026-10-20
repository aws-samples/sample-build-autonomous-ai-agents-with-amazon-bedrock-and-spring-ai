import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development, proxy API calls to the autonomous-agent backend so the browser
// talks to a single origin (no CORS needed in dev). Local port layout (see code/README.md):
//   autonomous-agent backend :8080, this UI :5173.
const backend = process.env.BACKEND_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/invocations': { target: backend, changeOrigin: true },
      '/actuator': { target: backend, changeOrigin: true },
    },
  },
  preview: {
    port: 5173,
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
