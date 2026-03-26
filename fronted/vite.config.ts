import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import monacoEditorPlugin from 'vite-plugin-monaco-editor'
import type { PluginOption } from 'vite'

const monacoPlugin =
  typeof monacoEditorPlugin === 'function'
    ? monacoEditorPlugin
    : (monacoEditorPlugin as { default: (options?: unknown) => PluginOption }).default

export default defineConfig({
  plugins: [vue(), monacoPlugin({}) as PluginOption],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
