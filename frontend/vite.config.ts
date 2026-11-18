import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  // D78: Tailwind CSS 4 chạy qua plugin của Vite, không cần file tailwind.config.
  plugins: [react(), tailwindcss()],
  resolve: {
    // D83: "@/..." trỏ tới src/ (CLI shadcn sinh code import theo đường dẫn này).
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // D22: khi dev, request tới /api được Vite chuyển sang backend Spring ở cổng 8080.
    // Trình duyệt chỉ thấy một địa chỉ (localhost:5173) nên chưa cần cấu hình CORS.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
