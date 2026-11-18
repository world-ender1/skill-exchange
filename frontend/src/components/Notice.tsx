import type { ReactNode } from 'react'
import { CircleAlert, CircleCheck, Info } from 'lucide-react'

import { Alert, AlertDescription } from '@/components/ui/alert'

type NoticeProps = {
  kind: 'error' | 'success' | 'info'
  children: ReactNode
}

const VARIANT = { error: 'destructive', success: 'success', info: 'default' } as const
const ICON = { error: CircleAlert, success: CircleCheck, info: Info } as const

/**
 * Hộp thông báo chung cho cả form (lỗi từ "detail", báo thành công...).
 * D85: hiện ngay trong thẻ vừa thao tác bằng Alert của shadcn/ui, không dùng toast; tham số giữ như cũ
 * nên các trang gọi Notice không phải sửa.
 */
export function Notice({ kind, children }: NoticeProps) {
  const Icon = ICON[kind]
  return (
    // Alert của shadcn/ui luôn đặt role="alert"; ghi đè để chỉ lỗi mới được đọc ngay (role="alert"),
    // thông báo thường dùng role="status" cho trình đọc màn hình đọc khi rảnh.
    <Alert variant={VARIANT[kind]} role={kind === 'error' ? 'alert' : 'status'}>
      <Icon aria-hidden="true" />
      <AlertDescription>{children}</AlertDescription>
    </Alert>
  )
}
