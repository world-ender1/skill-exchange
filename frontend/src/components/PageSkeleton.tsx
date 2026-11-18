import { Skeleton } from '@/components/ui/skeleton'

/**
 * Khung xám giữ chỗ trong lúc tải, thay chữ "Đang tải…" (kế hoạch tuần 6, bước 1).
 * aria-busy báo cho trình đọc màn hình biết vùng này đang tải.
 */
export function PageSkeleton({ label = 'Đang tải' }: { label?: string }) {
  return (
    <div aria-busy="true" aria-label={label} className="flex flex-col gap-4">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-40 w-full" />
      <Skeleton className="h-40 w-full" />
    </div>
  )
}
