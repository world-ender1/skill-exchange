import { Navigate, Outlet, useOutletContext } from 'react-router'

import type { Role } from '../api/types'

/**
 * Bọc các trang của sinh viên (S03–S10): admin bị chuyển về /admin/skills,
 * đối xứng với RequireAdmin (D76, chốt ở kế hoạch tuần 5 bước 10).
 * Không có lớp này thì admin gõ /matches vẫn vào được trang, rồi thấy lỗi 403 của backend.
 */
export function RequireStudent() {
  const role = useOutletContext<Role>()
  if (role !== 'STUDENT') {
    return <Navigate to="/admin/skills" replace />
  }
  return <Outlet />
}
