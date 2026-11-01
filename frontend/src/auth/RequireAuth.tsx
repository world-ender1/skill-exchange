import { Navigate, Outlet } from 'react-router'

import { getToken } from './token'

/**
 * Bọc các trang cần đăng nhập: chưa có vé thì chuyển về /login (TC74).
 * Đây chỉ là tiện lợi cho người dùng. Người cố tình gọi API không có vé vẫn bị backend trả 401.
 */
export function RequireAuth() {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }
  // Outlet: chỗ trang con (ví dụ /profile) được vẽ ra.
  return <Outlet />
}
