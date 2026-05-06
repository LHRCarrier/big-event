import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,//把/api添加到目标服务器的请求头中,让目标服务器知道这个请求是从哪里来的
        rewrite: (path) => path.replace(/^\/api/, '') //把/api替换成空字符串,也就是去掉/api前缀
      }
    }
  }
})
