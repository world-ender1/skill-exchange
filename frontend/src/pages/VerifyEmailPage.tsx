import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'

import { ApiError, apiRequest, toError } from '../api/client'
import { getToken } from '../auth/token'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'

type VerifyState = 'verifying' | 'success' | 'failed' | 'no-token'

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
    <main className="auth-page">
      <h1 className="brand">Skill Exchange</h1>
      <div className="card">
        <h2>Xác thực email</h2>

        {state === 'verifying' && <p>Đang xác thực…</p>}

        {state === 'success' && (
          <>
            <Notice kind="success">Xác thực email thành công. Tài khoản của bạn đã được kích hoạt.</Notice>
            <p>
              {getToken() ? <Link to="/profile">Về hồ sơ của tôi</Link> : <Link to="/login">Đăng nhập</Link>}
            </p>
          </>
        )}

        {state === 'failed' && <Notice kind="error">{failure}</Notice>}

        {(state === 'failed' || state === 'no-token') && <ResendForm />}
      </div>
    </main>
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
    <form onSubmit={handleSubmit} noValidate>
      <p className="muted small">Nhập email đã đăng ký để nhận link xác thực mới. Mỗi lần gửi cách nhau ít nhất 60 giây.</p>
      {sent && (
        <Notice kind="success">
          Nếu email này đang chờ xác thực, một link mới đã được gửi. Hãy kiểm tra hộp thư.
        </Notice>
      )}
      {error && !error.fieldErrors.email && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email}>
        <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <div className="actions">
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Đang gửi…' : 'Gửi lại link'}
        </button>
        <Link to="/login">Về đăng nhập</Link>
      </div>
    </form>
  )
}
