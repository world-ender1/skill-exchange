import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // D22: khi dev, request tới /api được Vite chuyển sang backend Spring ở cổng 8080.
    // Trình duyệt chỉ thấy một địa chỉ (localhost:5173) nên chưa cần cấu hình CORS.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
