import { useState } from 'react'
import type { FormEvent } from 'react'
import { Loader2 } from 'lucide-react'
import { Navigate, useNavigate } from 'react-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ApiError, apiRequest, toError } from '../api/client'
import { getToken, setToken } from '../auth/token'
import { AuthShell } from '../components/AuthShell'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'
import { TextLink } from '../components/TextLink'

type Tab = 'login' | 'register'

/** S01 – Đăng nhập / Đăng ký (D25: đăng ký có ô Họ tên). Giao diện: thẻ giữa trang, hai tab (kế hoạch tuần 6, bước 2). */
export function LoginPage() {
  const [tab, setTab] = useState<Tab>('login')
  // Thông báo sau khi đăng ký xong; hiện trên tab Đăng nhập.
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

  // Đã có vé thì không cần ở trang này nữa. "/" tự chọn trang chính theo vai trò (RoleHome, D76).
  if (getToken()) {
    return <Navigate to="/" replace />
  }

  function handleRegistered(email: string) {
    setRegisteredEmail(email)
    setTab('login')
  }

  return (
    <AuthShell>
      <Card>
        <CardContent>
          {/* Tabs của Radix tự gắn role="tab", aria-selected và cho chuyển tab bằng phím mũi tên. */}
          <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
            <TabsList className="w-full">
              <TabsTrigger value="login">Đăng nhập</TabsTrigger>
              <TabsTrigger value="register">Đăng ký</TabsTrigger>
            </TabsList>
            <TabsContent value="login" className="pt-4">
              <LoginForm registeredEmail={registeredEmail} />
            </TabsContent>
            <TabsContent value="register" className="pt-4">
              <RegisterForm onRegistered={handleRegistered} />
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>
    </AuthShell>
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
      // D76: vai trò đọc từ /api/me. Layout gọi /api/me khi mở "/", rồi RoleHome đưa ADMIN vào
      // /admin/skills, STUDENT vào /profile. Không gọi /api/me ở đây để luật chọn trang chỉ nằm một chỗ.
      navigate('/', { replace: true })
    } catch (e) {
      setError(toError(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      {registeredEmail && (
        <Notice kind="success">
          Đăng ký thành công. Hãy mở hộp thư email trường và bấm link xác thực.
          Bạn vẫn có thể đăng nhập ngay để điền hồ sơ.
        </Notice>
      )}
      {error && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email}>
        <Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Field label="Mật khẩu" error={error?.fieldErrors.password}>
        <Input type="password" autoComplete="current-password" value={password}
               onChange={(e) => setPassword(e.target.value)} />
      </Field>

      <Button type="submit" disabled={submitting} className="w-full">
        {submitting && <Loader2 className="animate-spin" aria-hidden="true" />}
        {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
      </Button>
      <p className="text-center text-sm text-muted-foreground">
        Chưa nhận được thư xác thực? <TextLink to="/verify-email">Gửi lại link</TextLink>
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
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      {error && <Notice kind="error">{error.message}</Notice>}

      <Field label="Email trường" error={error?.fieldErrors.email} hint="Chỉ nhận email do trường cấp">
        <Input type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
      </Field>
      <Field label="Họ tên" error={error?.fieldErrors.fullName}>
        <Input autoComplete="name" maxLength={100} value={fullName} onChange={(e) => setFullName(e.target.value)} />
      </Field>
      <Field label="Mật khẩu" error={error?.fieldErrors.password} hint="8–72 ký tự">
        <Input type="password" autoComplete="new-password" value={password}
               onChange={(e) => setPassword(e.target.value)} />
      </Field>

      <Button type="submit" disabled={submitting} className="w-full">
        {submitting && <Loader2 className="animate-spin" aria-hidden="true" />}
        {submitting ? 'Đang đăng ký…' : 'Đăng ký'}
      </Button>
    </form>
  )
}
