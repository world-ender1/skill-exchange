import type { ReactNode } from 'react'

type FieldProps = {
  label: string
  /** Lỗi của ô này lấy từ errors[] của backend (D24); không có thì không hiện gì. */
  error?: string
  hint?: string
  children: ReactNode
}

/** Một ô nhập có nhãn, gợi ý và lỗi ngay bên dưới. */
export function Field({ label, error, hint, children }: FieldProps) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}
