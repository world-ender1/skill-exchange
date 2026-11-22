import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { CircleCheck, CircleX, Loader2, Mail } from 'lucide-react'
import { useSearchParams } from 'react-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ApiError, apiRequest, toError } from '../api/client'
import { getToken } from '../auth/token'
import { AuthShell } from '../components/AuthShell'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'
import { TextLink } from '../components/TextLink'

type VerifyState = 'verifying' | 'success' | 'failed' | 'no-token'

/** Icon và màu theo trạng thái (kế hoạch tuần 6, bước 2). Màu đi kèm chữ, không dùng màu một mình để báo kết quả. */
const STATE_ICON = {
  verifying: <Loader2 className="size-5 animate-spin text-muted-foreground" aria-hidden="true" />,
  success: <CircleCheck className="size-5 text-success" aria-hidden="true" />,
  failed: <CircleX className="size-5 text-destructive" aria-hidden="true" />,
  'no-token': <Mail className="size-5 text-muted-foreground" aria-hidden="true" />,
}

/**
 * S02 – Xác thực email. Mở từ link trong thư: /verify-email?token=... (D5).
 * Có token: gọi POST /api/auth/verify-email một lần. Không có token hoặc thất bại: cho gửi lại link.
 */
export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [state, setState] = useState<VerifyState>(token ? 'verifying' : 'no-token')
  const [failure, setFailure] = useState<string | null>(null)

  // Token chỉ dùng được MỘT lần. Ở chế độ dev, React StrictMode chạy useEffect hai lần:
  // lần hai sẽ nhận 400 và làm màn hình báo thất bại dù đã thành công. Ghi nhớ token đã gửi để không gửi lại.
  const sentToken = useRef<string | null>(null)

  // useEffect: chạy SAU khi trang được vẽ, dùng cho việc "phụ" như gọi API khi trang mở.
  useEffect(() => {
    if (!token || sentToken.current === token) {
      return
    }
    sentToken.current = token
    apiRequest<void>('/api/auth/verify-email', { method: 'POST', body: { token } })
      .then(() => setState('success'))
      .catch((e: unknown) => {
        setFailure(toError(e).message)
        setState('failed')
      })
  }, [token])

  return (
    <AuthShell>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            {STATE_ICON[state]}
            {/* Tiêu đề cấp 2 như bản trước tuần 6 (h1 là tên ứng dụng trong AuthShell). */}
            <h2 className="text-lg">Xác thực email</h2>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {state === 'verifying' && <p className="text-muted-foreground">Đang xác thực…</p>}

          {state === 'success' && (
            <>
              <Notice kind="success">Xác thực email thành công. Tài khoản của bạn đã được kích hoạt.</Notice>
              <p>
                {getToken()
                  ? <TextLink to="/profile">Về hồ sơ của tôi</TextLink>
                  : <TextLink to="/login">Đăng nhập</TextLink>}
              </p>
            </>
          )}

          {state === 'failed' && <Notice kind="error">{failure}</Notice>}

          {(state === 'failed' || state === 'no-token') && <ResendForm />}
        </CardContent>
      </Card>
    </AuthShell>
  )
}

/** Gửi lại link (D7): email đúng định dạng thì luôn báo đã gửi, không tiết lộ email có tồn tại hay không. */
function ResendForm() {
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setSent(false)
    try {
      await apiRequest<void>('/api/auth/resend-verification', { method: 'POST', body: { email } })
      setSent(true)
    } catch (e) {
      setError(toError(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Nhập email đã đăng ký để nhận link xác thực mới. Mỗi lần gửi cách nhau ít nhất 60 giây.
      </p>
      {sent && (
        <Notice kind="success">
          Nếu email này đang chờ xác thực, một link mới đã được gửi. Hãy kiểm tra hộp thư.
        </Notice>
      )}
      {error && !error.fieldErrors.email && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email}>
        <Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <div className="flex flex-wrap items-center gap-3">
        <Button type="submit" disabled={submitting}>
          {submitting && <Loader2 className="animate-spin" aria-hidden="true" />}
          {submitting ? 'Đang gửi…' : 'Gửi lại link'}
        </Button>
        <TextLink to="/login" className="text-sm">Về đăng nhập</TextLink>
      </div>
    </form>
  )
}
