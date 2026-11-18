import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 代理后端接口与上传文件，解决开发期跨域
    // 可用环境变量 BACKEND_PORT 指定后端端口（默认 8080）
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.BACKEND_PORT || 8080}`,
        changeOrigin: true,
      },
      '/upload': {
        target: `http://localhost:${process.env.BACKEND_PORT || 8080}`,
        changeOrigin: true,
      },
    },
  },
})
