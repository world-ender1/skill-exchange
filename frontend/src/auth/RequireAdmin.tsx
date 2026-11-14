import { Navigate, Outlet, useOutletContext } from 'react-router'

import type { Role } from '../api/types'

/**
 * Bọc các trang của admin (/admin/*): sinh viên bị chuyển về /profile (D76, TC263).
 * Vai trò lấy từ Layout (đã đọc /api/me). Đây chỉ là tiện lợi: sinh viên tự gọi /api/admin/** vẫn nhận 403.
 */
export function RequireAdmin() {
  const role = useOutletContext<Role>()
  if (role !== 'ADMIN') {
    return <Navigate to="/profile" replace />
  }
  return <Outlet />
}
