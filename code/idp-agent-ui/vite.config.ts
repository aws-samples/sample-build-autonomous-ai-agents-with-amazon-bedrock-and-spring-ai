import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// `process` is provided by Node (types come from @types/node via tsconfig.node.json).
// During development, proxy API calls to the IDP backend so the browser talks to a
// single origin (no CORS needed in dev). Local port layout (see code/README.md):
//   idp-agent backend :8081, this UI :5174.
const backend = process.env.BACKEND_URL ?? 'http://localhost:8081';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/invocations': { target: backend, changeOrigin: true },
      '/actuator': { target: backend, changeOrigin: true },
    },
  },
  preview: {
    port: 5174,
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
