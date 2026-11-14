import { BrowserRouter, Navigate, Route, Routes, useOutletContext } from 'react-router'

import type { Role } from './api/types'
import { RequireAdmin } from './auth/RequireAdmin'
import { RequireAuth } from './auth/RequireAuth'
import { RequireStudent } from './auth/RequireStudent'
import { Layout } from './components/Layout'
import { AdminSkillsPage } from './pages/AdminSkillsPage'
import { LoginPage } from './pages/LoginPage'
import { MatchesPage } from './pages/MatchesPage'
import { ProfilePage } from './pages/ProfilePage'
import { SearchPage } from './pages/SearchPage'
import { SkillsPage } from './pages/SkillsPage'
import { UserProfilePage } from './pages/UserProfilePage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'

/** Bản đồ đường dẫn → màn hình (kế hoạch tuần 3 bước 10, tuần 4 bước 10, tuần 5 bước 10). */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        {/* D5: link trong thư trỏ tới ${FRONTEND_URL}/verify-email?token=...; trang công khai. */}
        <Route path="/verify-email" element={<VerifyEmailPage />} />

        {/* Các trang cần đăng nhập: RequireAuth kiểm vé, Layout đọc vai trò từ /api/me và vẽ menu. */}
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route element={<RequireStudent />}>
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/skills" element={<SkillsPage />} />
              <Route path="/matches" element={<MatchesPage />} />
              <Route path="/users/:id" element={<UserProfilePage />} />
              <Route path="/search" element={<SearchPage />} />
            </Route>

            <Route element={<RequireAdmin />}>
              <Route path="/admin/skills" element={<AdminSkillsPage />} />
            </Route>

            {/* "/", đường dẫn lạ (kể cả /admin/<chưa có>) và ngay sau đăng nhập: về trang chính theo vai trò. */}
            <Route path="*" element={<RoleHome />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

/**
 * Trang chính theo vai trò (D76): ADMIN vào A01, STUDENT vào S03.
 * Nằm trong Layout nên vai trò đã được đọc từ /api/me; chưa đăng nhập thì RequireAuth đã chuyển về /login.
 */
function RoleHome() {
  const role = useOutletContext<Role>()
  return <Navigate to={role === 'ADMIN' ? '/admin/skills' : '/profile'} replace />
}
