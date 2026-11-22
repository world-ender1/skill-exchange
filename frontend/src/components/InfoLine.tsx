import type { ReactNode } from 'react'

/** Một dòng thông tin có icon và nhãn đậm, dùng ở thẻ yêu cầu (S08) và phiên (S09); cùng kiểu dòng lý do của S05. */
export function InfoLine({ icon, label, children }: { icon: ReactNode; label: string; children: ReactNode }) {
  return (
    <p className="flex items-start gap-2 text-sm break-words">
      <span className="mt-0.5 text-muted-foreground [&_svg]:size-4" aria-hidden="true">{icon}</span>
      <span className="min-w-0">
        <span className="font-medium">{label}:</span> {children}
      </span>
    </p>
  )
}

/**
 * Địa điểm/link do người gửi yêu cầu tự nhập (S08, S09). Chỉ thành link mở tab mới khi bắt đầu bằng http:// hoặc
 * https://, để chuỗi "javascript:..." không thành link chạy được; còn lại hiện như chữ thường (địa chỉ phòng học).
 */
export function LinkOrText({ value }: { value: string }) {
  if (!/^https?:\/\//i.test(value)) {
    return value
  }
  return (
    // noopener: trang được mở không điều khiển ngược được tab này qua window.opener.
    <a href={value} target="_blank" rel="noopener noreferrer"
       className="break-all text-primary underline-offset-4 hover:underline">{value}</a>
  )
}
