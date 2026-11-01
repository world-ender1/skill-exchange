import { BrowserRouter, Navigate, Route, Routes } from 'react-router'

import { RequireAuth } from './auth/RequireAuth'
import { getToken } from './auth/token'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'

/** Bản đồ đường dẫn → màn hình (xem "Chốt ở bước này" của bước 10). */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Các trang cần đăng nhập: RequireAuth kiểm vé, Layout vẽ thanh điều hướng. */}
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route path="/profile" element={<p>Hồ sơ của tôi: làm ở màn hình S03.</p>} />
          </Route>
        </Route>

        <Route path="*" element={<HomeRedirect />} />
      </Routes>
    </BrowserRouter>
  )
}

/** "/" và đường dẫn lạ: đã đăng nhập thì vào hồ sơ, chưa thì về đăng nhập. */
function HomeRedirect() {
  return <Navigate to={getToken() ? '/profile' : '/login'} replace />
}
