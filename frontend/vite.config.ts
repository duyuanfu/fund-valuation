import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            // ECharts 单独切分，仅详情页进入时加载
            if (id.includes('echarts')) {
              return 'echarts'
            }
            // Ant Design 图标库极其庞大，单独拆包，避免阻塞 UI 组件渲染
            if (id.includes('@ant-design/icons')) {
              return 'antd-icons'
            }
            // Ant Design 核心 UI 单独打包
            if (id.includes('antd')) {
              return 'antd-core'
            }
            // 拖拽核心库单独打包
            if (id.includes('@dnd-kit')) {
              return 'dnd-kit'
            }
            // 最小化 React 核心运行时
            if (id.includes('react') || id.includes('react-dom') || id.includes('react-router-dom')) {
              return 'react-framework'
            }
            return 'vendor-misc'
          }
        },
      },
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    allowedHosts: ['daydayfund.dpdns.org', 'trythis.pw'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
