import { RotateCw } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Notice } from './Notice'

/** Tải dữ liệu thất bại: hiện "detail" của lỗi và nút "Thử lại" (cách làm từ tuần 3). */
export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-start gap-3">
      <Notice kind="error">{message}</Notice>
      <Button variant="outline" onClick={onRetry}>
        <RotateCw aria-hidden="true" />
        Thử lại
      </Button>
    </div>
  )
}
