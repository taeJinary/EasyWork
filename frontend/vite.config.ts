/// <reference types="vitest" />
import { defineConfig } from 'vitest/config'
import { loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, path.resolve(__dirname, '..'), '');

  return {
    envDir: '..',
    define: {
      __OAUTH_GOOGLE_CLIENT_ID__: JSON.stringify(env.OAUTH_GOOGLE_CLIENT_ID ?? ''),
      __OAUTH_GOOGLE_REDIRECT_URI__: JSON.stringify(env.OAUTH_GOOGLE_REDIRECT_URI ?? ''),
      __OAUTH_NAVER_CLIENT_ID__: JSON.stringify(env.OAUTH_NAVER_CLIENT_ID ?? ''),
      __OAUTH_NAVER_REDIRECT_URI__: JSON.stringify(env.OAUTH_NAVER_REDIRECT_URI ?? ''),
    },
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      css: false,
    },
  };
})

