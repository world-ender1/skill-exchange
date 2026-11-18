import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { cn } from '@/lib/utils'

/** "Nguyễn Văn An" → "NA": chữ đầu của từ đầu và từ cuối (họ và tên gọi). */
function initials(fullName: string): string {
  const words = fullName.trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return '?'
  const first = words[0][0]
  const last = words.length > 1 ? words[words.length - 1][0] : ''
  return (first + last).toLocaleUpperCase('vi')
}

/**
 * Avatar chữ cái (dự án chưa có ảnh đại diện). Chữ cái chỉ để nhìn: tên đầy đủ luôn hiện ngay bên cạnh,
 * nên avatar ẩn khỏi trình đọc màn hình để không đọc lặp "N A, Nguyễn Văn An".
 */
export function UserAvatar({ fullName, className }: { fullName: string; className?: string }) {
  return (
    <Avatar className={cn('size-10', className)} aria-hidden="true">
      <AvatarFallback className="bg-primary/10 font-medium text-primary">{initials(fullName)}</AvatarFallback>
    </Avatar>
  )
}
