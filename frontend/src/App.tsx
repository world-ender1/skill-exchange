import { BrowserRouter, Navigate, Route, Routes } from 'react-router'

import { RequireAuth } from './auth/RequireAuth'
import { getToken } from './auth/token'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { ProfilePage } from './pages/ProfilePage'
import { SkillsPage } from './pages/SkillsPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

/** Bản đồ đường dẫn → màn hình (xem "Chốt ở bước này" của bước 10). */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        {/* D5: link trong thư trỏ tới ${FRONTEND_URL}/verify-email?token=...; trang công khai. */}
        <Route path="/verify-email" element={<VerifyEmailPage />} />

        {/* Các trang cần đăng nhập: RequireAuth kiểm vé, Layout vẽ thanh điều hướng. */}
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/skills" element={<SkillsPage />} />
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
