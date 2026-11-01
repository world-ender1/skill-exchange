import { NavLink, Outlet, useNavigate } from 'react-router'

import { clearToken } from '../auth/token'

/** Khung chung của các trang sau khi đăng nhập: thanh điều hướng + nội dung trang. */
export function Layout() {
  const navigate = useNavigate()

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
          <NavLink to="/profile">Hồ sơ</NavLink>
          <NavLink to="/skills">Kỹ năng</NavLink>
        </nav>
        <button type="button" className="link-button" onClick={logout}>Đăng xuất</button>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
