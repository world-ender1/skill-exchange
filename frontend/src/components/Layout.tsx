import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'

import { apiRequest, toError } from '../api/client'
import type { MyProfile, Role } from '../api/types'
import { clearToken } from '../auth/token'
import { Notice } from './Notice'

/**
 * Khung chung của các trang sau khi đăng nhập: thanh điều hướng + nội dung trang.
 *
 * Vai trò đọc từ /api/me ở đây, một lần mỗi lần đăng nhập hoặc F5 (D76): Layout là route cha nên
 * chuyển giữa các trang con không dựng lại Layout, không gọi lại API. Vai trò được đưa xuống trang con
 * bằng <Outlet context>, RequireAdmin/RequireStudent/RoleHome đọc lại bằng useOutletContext<Role>().
 */
export function Layout() {
  const navigate = useNavigate()
  const [role, setRole] = useState<Role | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    apiRequest<MyProfile>('/api/me')
      .then((me) => {
        if (!cancelled) {
          setRole(me.role)
          setLoadError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setLoadError(toError(e).message)
        }
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  function logout() {
    // Đăng xuất = xóa vé khỏi trình duyệt. Server không lưu vé nên không có gì để "hủy" ở backend;
    // vé cũ vẫn hợp lệ tới khi hết hạn nếu ai đó đã chép được nó (hạn chế của JWT không có refresh token).
    clearToken()
    navigate('/login', { replace: true })
  }

  return (
    <>
      <header className="topbar">
        <span className="brand-small">Skill Exchange</span>
        <nav>
          {/* Menu chỉ để dễ dùng; quyền vẫn do backend kiểm (sinh viên gọi /api/admin/** nhận 403). */}
          {role === 'ADMIN' && <NavLink to="/admin/skills">Danh mục kỹ năng</NavLink>}
          {role === 'STUDENT' && (
            <>
              <NavLink to="/profile">Hồ sơ</NavLink>
              <NavLink to="/skills">Kỹ năng</NavLink>
              <NavLink to="/matches">Gợi ý</NavLink>
              <NavLink to="/search">Tìm kiếm</NavLink>
            </>
          )}
        </nav>
        <button type="button" className="link-button" onClick={logout}>Đăng xuất</button>
      </header>
      <main className="page">
        {loadError ? (
          <>
            <Notice kind="error">{loadError}</Notice>
            <p><button type="button" onClick={() => { setLoadError(null); setReloadKey((k) => k + 1) }}>Thử lại</button></p>
          </>
        ) : role === null ? (
          <p>Đang tải…</p>
        ) : (
          // Chưa biết vai trò thì chưa vẽ trang con: RequireAdmin/RequireStudent cần vai trò để quyết định.
          <Outlet context={role} />
        )}
      </main>
    </>
  )
}
