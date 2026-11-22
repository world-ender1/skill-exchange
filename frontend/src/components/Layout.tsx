import { useEffect, useState } from 'react'
import { BookOpen, Inbox, LogOut, Search, Sparkles, Tags, User } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'

import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
  useSidebar,
} from '@/components/ui/sidebar'
import { apiRequest, toError } from '../api/client'
import type { MyProfile, Role } from '../api/types'
import { clearToken } from '../auth/token'
import { AppLogo } from './AppLogo'
import { LoadError } from './LoadError'
import { PageSkeleton } from './PageSkeleton'

type NavItem = { to: string; label: string; icon: LucideIcon }

/** D81: mục menu và icon theo vai trò. Không có số yêu cầu đang chờ (D86). Phiên thêm ở bước 11 khi có S09. */
const STUDENT_NAV: NavItem[] = [
  { to: '/profile', label: 'Hồ sơ', icon: User },
  { to: '/skills', label: 'Kỹ năng', icon: BookOpen },
  { to: '/matches', label: 'Gợi ý', icon: Sparkles },
  { to: '/search', label: 'Tìm kiếm', icon: Search },
  { to: '/requests', label: 'Yêu cầu', icon: Inbox },
]
const ADMIN_NAV: NavItem[] = [
  { to: '/admin/skills', label: 'Danh mục kỹ năng', icon: Tags },
]

/**
 * Khung chung của các trang sau khi đăng nhập: menu + nội dung trang.
 *
 * Vai trò đọc từ /api/me ở đây, một lần mỗi lần đăng nhập hoặc F5 (D76): Layout là route cha nên
 * chuyển giữa các trang con không dựng lại Layout, không gọi lại API. Vai trò được đưa xuống trang con
 * bằng <Outlet context>, RequireAdmin/RequireStudent/RoleHome đọc lại bằng useOutletContext<Role>().
 *
 * D81: từ 768 px menu là thanh bên; hẹp hơn thì thanh trên cùng có nút mở menu dạng Sheet
 * (component Sidebar của shadcn/ui tự đổi cách hiện theo độ rộng màn hình).
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
    <SidebarProvider>
      <AppSidebar role={role} onLogout={logout} />
      <SidebarInset>
        {/* Chỉ hiện dưới 768 px: tên ứng dụng và nút mở menu. */}
        <header className="flex h-14 items-center gap-2 border-b bg-card px-4 md:hidden">
          <SidebarTrigger aria-label="Mở menu" />
          <AppLogo />
        </header>
        {/* SidebarInset đã là <main>, nên phần nội dung dùng <div>. */}
        <div className="mx-auto w-full max-w-5xl flex-1 px-4 py-6 md:px-8">
          {loadError ? (
            <LoadError message={loadError} onRetry={() => { setLoadError(null); setReloadKey((k) => k + 1) }} />
          ) : role === null ? (
            <PageSkeleton />
          ) : (
            // Chưa biết vai trò thì chưa vẽ trang con: RequireAdmin/RequireStudent cần vai trò để quyết định.
            <Outlet context={role} />
          )}
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}

function AppSidebar({ role, onLogout }: { role: Role | null; onLogout: () => void }) {
  const { isMobile, setOpenMobile } = useSidebar()
  const { pathname } = useLocation()
  // Đang tải vai trò thì menu chỉ có tên và "Đăng xuất". Menu chỉ để dễ dùng; quyền vẫn do backend kiểm.
  const items = role === 'ADMIN' ? ADMIN_NAV : role === 'STUDENT' ? STUDENT_NAV : []

  // Trên màn hẹp menu là Sheet phủ lên trang: chọn xong một mục thì đóng lại (TC304).
  function closeOnMobile() {
    if (isMobile) {
      setOpenMobile(false)
    }
  }

  return (
    <Sidebar>
      <SidebarHeader className="px-4 py-3">
        <AppLogo />
      </SidebarHeader>
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <SidebarMenu>
              {items.map(({ to, label, icon: Icon }) => (
                <SidebarMenuItem key={to}>
                  {/* asChild: SidebarMenuButton không tự vẽ <button> mà "mặc" kiểu của nó lên NavLink (thẻ <a>). */}
                  <SidebarMenuButton asChild isActive={pathname === to || pathname.startsWith(`${to}/`)}>
                    <NavLink to={to} onClick={closeOnMobile}>
                      <Icon aria-hidden="true" />
                      <span>{label}</span>
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
      <SidebarFooter>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton onClick={onLogout}>
              <LogOut aria-hidden="true" />
              <span>Đăng xuất</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
    </Sidebar>
  )
}
