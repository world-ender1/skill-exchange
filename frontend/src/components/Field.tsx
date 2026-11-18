import { cloneElement, isValidElement, useId } from 'react'
import type { ReactNode } from 'react'

import {
  Field as FieldRoot,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '@/components/ui/field'

type FieldProps = {
  label: string
  /** Lỗi của ô này lấy từ errors[] của backend (D24); không có thì không hiện gì. */
  error?: string
  hint?: string
  /** Đúng một ô nhập (input, select, textarea...). */
  children: ReactNode
}

/** Thuộc tính Field gắn thêm vào ô nhập con. */
type ControlProps = {
  id?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

/**
 * Một ô nhập có nhãn, gợi ý và lỗi ngay bên dưới (D85: tham số giữ như cũ, bên trong dùng Field của shadcn/ui).
 *
 * Bản cũ bọc ô nhập trong <label> nên nhãn tự nối với ô. Field của shadcn/ui đặt nhãn cạnh ô, nên phải nối
 * bằng id: useId() sinh id, cloneElement gắn id đó (và aria-invalid, aria-describedby để trình đọc màn hình
 * đọc lỗi khi vào ô) vào ô nhập con. Nhờ vậy các trang vẫn viết <Field label="..."><input .../></Field> như cũ.
 */
export function Field({ label, error, hint, children }: FieldProps) {
  const generatedId = useId()
  const message = error ?? hint
  const messageId = `${generatedId}-message`

  let controlId = generatedId
  let control = children
  if (isValidElement<ControlProps>(children)) {
    controlId = children.props.id ?? generatedId
    control = cloneElement(children, {
      id: controlId,
      'aria-invalid': error ? true : undefined,
      'aria-describedby': message ? messageId : undefined,
    })
  }

  return (
    <FieldRoot data-invalid={error ? true : undefined}>
      <FieldLabel htmlFor={controlId}>{label}</FieldLabel>
      {control}
      {hint && !error && <FieldDescription id={messageId}>{hint}</FieldDescription>}
      {error && <FieldError id={messageId}>{error}</FieldError>}
    </FieldRoot>
  )
}
