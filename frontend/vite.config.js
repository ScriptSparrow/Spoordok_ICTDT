import { defineConfig } from 'vite';
import cesium from 'vite-plugin-cesium';

export default defineConfig({
  plugins: [cesium()],
  server : {
    host: '0.0.0.0',
    port: 5173
  }
});
