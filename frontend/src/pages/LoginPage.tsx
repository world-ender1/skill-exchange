import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'

import { ApiError, apiRequest, toError } from '../api/client'
import { getToken, setToken } from '../auth/token'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'

type Tab = 'login' | 'register'

/** S01 – Đăng nhập / Đăng ký (D25: đăng ký có ô Họ tên). */
export function LoginPage() {
  const [tab, setTab] = useState<Tab>('login')
  // Thông báo sau khi đăng ký xong; hiện trên tab Đăng nhập.
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

  // Đã có vé thì không cần ở trang này nữa.
  if (getToken()) {
    return <Navigate to="/profile" replace />
  }

  function handleRegistered(email: string) {
    setRegisteredEmail(email)
    setTab('login')
  }

  return (
    <main className="auth-page">
      <h1 className="brand">Skill Exchange</h1>
      <div className="card">
        <div className="tabs" role="tablist">
          <button type="button" role="tab" aria-selected={tab === 'login'}
                  className={tab === 'login' ? 'tab active' : 'tab'} onClick={() => setTab('login')}>
            Đăng nhập
          </button>
          <button type="button" role="tab" aria-selected={tab === 'register'}
                  className={tab === 'register' ? 'tab active' : 'tab'} onClick={() => setTab('register')}>
            Đăng ký
          </button>
        </div>

        {tab === 'login'
          ? <LoginForm registeredEmail={registeredEmail} />
          : <RegisterForm onRegistered={handleRegistered} />}
      </div>
    </main>
  )
}

function LoginForm({ registeredEmail }: { registeredEmail: string | null }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState(registeredEmail ?? '')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault() // không cho trình duyệt tải lại trang khi submit form
    setSubmitting(true)
    setError(null)
    try {
      const result = await apiRequest<{ accessToken: string }>('/api/auth/login', {
        method: 'POST',
        body: { email, password },
      })
      setToken(result.accessToken)
      navigate('/profile', { replace: true })
    } catch (e) {
      setError(toError(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {registeredEmail && (
        <Notice kind="success">
          Đăng ký thành công. Mở email <strong>{registeredEmail}</strong> và bấm link xác thực.
          Bạn vẫn có thể đăng nhập ngay để điền hồ sơ.
        </Notice>
      )}
      {error && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email}>
        <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Field label="Mật khẩu" error={error?.fieldErrors.password}>
        <input type="password" autoComplete="current-password" value={password}
               onChange={(e) => setPassword(e.target.value)} />
      </Field>

      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
      </button>
      <p className="muted small">
        Chưa nhận được thư xác thực? <Link to="/verify-email">Gửi lại link</Link>
      </p>
    </form>
  )
}

function RegisterForm({ onRegistered }: { onRegistered: (email: string) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const result = await apiRequest<{ email: string }>('/api/auth/register', {
        method: 'POST',
        body: { email, password, fullName },
      })
      onRegistered(result.email)
    } catch (e) {
      setError(toError(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      {error && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email} hint="Chỉ nhận email do trường cấp">
        <input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Field label="Họ tên" error={error?.fieldErrors.fullName}>
        <input autoComplete="name" maxLength={100} value={fullName} onChange={(e) => setFullName(e.target.value)} />
      </Field>
      <Field label="Mật khẩu" error={error?.fieldErrors.password} hint="8–72 ký tự">
        <input type="password" autoComplete="new-password" value={password}
               onChange={(e) => setPassword(e.target.value)} />
      </Field>

      <button type="submit" className="primary" disabled={submitting}>
        {submitting ? 'Đang đăng ký…' : 'Đăng ký'}
      </button>
    </form>
  )
}
