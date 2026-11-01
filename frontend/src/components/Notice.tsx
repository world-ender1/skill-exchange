import type { ReactNode } from 'react'

type NoticeProps = {
  kind: 'error' | 'success' | 'info'
  children: ReactNode
}

/** Hộp thông báo chung cho cả form (lỗi từ "detail", báo thành công...). */
export function Notice({ kind, children }: NoticeProps) {
  // role="alert" để trình đọc màn hình đọc ngay lỗi vừa xuất hiện.
  return (
    <div className={`notice notice-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
