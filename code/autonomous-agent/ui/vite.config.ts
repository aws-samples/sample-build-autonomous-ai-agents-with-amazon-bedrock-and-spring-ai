import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development, proxy API calls to the Spring backend on :8080 so the
// browser talks to a single origin (no CORS needed in dev).
const backend = process.env.BACKEND_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/invocations': { target: backend, changeOrigin: true },
      '/actuator': { target: backend, changeOrigin: true },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
