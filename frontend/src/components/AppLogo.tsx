import { ArrowLeftRight } from 'lucide-react'

/** Biểu tượng và tên ứng dụng, dùng ở menu (Layout) và trang đăng nhập (AuthShell). */
export function AppLogo() {
  return (
    <span className="flex items-center gap-2">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <ArrowLeftRight className="size-4" aria-hidden="true" />
      </span>
      <span className="font-heading text-lg font-bold">Skill Exchange</span>
    </span>
  )
}
