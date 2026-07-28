import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devProxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8006'
  const mediaProxyTarget = env.VITE_PROJECT_MEDIA_PROXY_TARGET || 'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com'

  return {
    base: env.VITE_APP_BASE || '/',
    plugins: [
      vue({
        template: {
          compilerOptions: {
            isCustomElement: (tag) => tag === 'model-viewer'
          }
        }
      }),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    server: {
      proxy: {
        '/api': {
          target: devProxyTarget,
          changeOrigin: true,
        },
        '/admin': {
          target: devProxyTarget,
          changeOrigin: true,
        },
        '/collectibles': {
          target: devProxyTarget,
          changeOrigin: true,
        },
        '/project-media': {
          target: mediaProxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})
