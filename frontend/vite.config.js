import { defineConfig } from 'vite';
import { viteStaticCopy } from 'vite-plugin-static-copy';

// Vite dev server with proxy to Spring Boot backend on 8080
export default defineConfig({
  base: '/',
  plugins: [
    // Copy Cesium runtime assets so they are available under /Cesium at runtime
    viteStaticCopy({
      targets: [
        { src: 'node_modules/cesium/Build/Cesium/Assets', dest: 'Cesium' },
        { src: 'node_modules/cesium/Build/Cesium/Widgets', dest: 'Cesium' },
        { src: 'node_modules/cesium/Build/Cesium/Workers', dest: 'Cesium' },
        { src: 'node_modules/cesium/Build/Cesium/ThirdParty', dest: 'Cesium' },
      ],
      // During dev, copy to the served root; during build, they end up in dist/Cesium
      watch: true,
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});
