import { defineConfig } from 'vite';

// Static landing page. public/ (config.js, main.js, styles.css) is copied as-is.
export default defineConfig({
  server: { port: 3000, strictPort: true },
  preview: { port: 3000, strictPort: true },
  build: { outDir: 'dist' },
});
