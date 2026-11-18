import type { ReactNode } from 'react'

import { AppLogo } from './AppLogo'

/** Khung chung của S01, S02 (chưa đăng nhập, không có menu): logo và một cột hẹp giữa màn hình. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-6 px-4 py-10">
      <div className="flex flex-col items-center gap-2 text-center">
        <AppLogo />
        <p className="text-sm text-muted-foreground">Ghép cặp trao đổi kỹ năng giữa sinh viên</p>
      </div>
      <div className="w-full max-w-sm">{children}</div>
    </main>
  )
}
