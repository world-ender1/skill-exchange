import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Loader2, Lock } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { FieldLegend, FieldSet } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { ApiError, apiRequest, toError } from '../api/client'
import type { AvailabilitySlot, LearningMode, MyProfile } from '../api/types'
import { PERIODS, slotKey } from '../availability'
import { AvailabilityGrid } from '../components/AvailabilityGrid'
import { Field } from '../components/Field'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'

/** S03 – Hồ sơ của tôi: thông tin cá nhân, liên hệ riêng tư, hình thức học, lưới lịch rảnh 7×3. */
export function ProfilePage() {
  const [profile, setProfile] = useState<MyProfile | null>(null)
  const [slots, setSlots] = useState<AvailabilitySlot[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  // Mở trang thì tải hồ sơ và lịch rảnh song song. reloadKey đổi khi bấm "Thử lại".
  useEffect(() => {
    let cancelled = false // trang bị đóng trước khi API trả về thì bỏ qua kết quả
    Promise.all([
      apiRequest<MyProfile>('/api/me'),
      apiRequest<AvailabilitySlot[]>('/api/me/availability'),
    ])
      .then(([me, availability]) => {
        if (!cancelled) {
          setProfile(me)
          setSlots(availability)
          setLoadError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setLoadError(toError(e).message)
        }
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  if (loadError) {
    return <LoadError message={loadError} onRetry={() => { setLoadError(null); setReloadKey((k) => k + 1) }} />
  }
  if (!profile || !slots) {
    return <PageSkeleton label="Đang tải hồ sơ" />
  }

  return (
    <div className="flex flex-col gap-6">
      <h1>Hồ sơ của tôi</h1>
      {profile.status === 'PENDING_VERIFICATION' && (
        // D29: frontend dùng status để nhắc xác thực email.
        <Notice kind="info">
          Email <strong>{profile.email}</strong> chưa được xác thực. Bạn vẫn điền được hồ sơ, nhưng cần xác thực
          để dùng gợi ý ghép cặp. <TextLink to="/verify-email">Gửi lại link xác thực</TextLink>
        </Notice>
      )}
      {/* key: đổi người dùng/dữ liệu thì React tạo lại form với giá trị mới. */}
      <ProfileForm key={profile.id} initial={profile} onSaved={setProfile} />
      <AvailabilityCard initial={slots} />
      <p className="text-sm text-muted-foreground">
        Tiếp theo: <TextLink to="/skills">khai báo kỹ năng dạy và muốn học</TextLink>.
      </p>
    </div>
  )
}

// ---------- Thông tin cá nhân ----------

function ProfileForm({ initial, onSaved }: { initial: MyProfile; onSaved: (profile: MyProfile) => void }) {
  // Ô nhập luôn giữ chuỗi; null từ backend hiện thành ô trống, ô trống gửi lên thành null (D15).
  const [fullName, setFullName] = useState(initial.fullName)
  const [bio, setBio] = useState(initial.bio ?? '')
  const [learningMode, setLearningMode] = useState<LearningMode | ''>(initial.learningMode ?? '')
  const [phone, setPhone] = useState(initial.phone ?? '')
  const [contactInfo, setContactInfo] = useState(initial.contactInfo ?? '')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setSaved(false)
    setError(null)
    try {
      const result = await apiRequest<MyProfile>('/api/me', {
        method: 'PUT',
        body: { fullName, bio, learningMode: learningMode || null, phone, contactInfo },
      })
      onSaved(result)
      setSaved(true)
    } catch (e) {
      setError(toError(e))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Thông tin cá nhân</CardTitle>
      </CardHeader>
      <CardContent>
        <form id="profile-form" onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-2">
          {error && <div className="sm:col-span-2"><Notice kind="error">{error.message}</Notice></div>}
          {saved && <div className="sm:col-span-2"><Notice kind="success">Đã lưu hồ sơ.</Notice></div>}

          <Field label="Email trường" hint="Không đổi được">
            <Input value={initial.email} disabled />
          </Field>
          <Field label="Họ tên" error={error?.fieldErrors.fullName}>
            <Input maxLength={100} value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </Field>
          <Field label="Giới thiệu" error={error?.fieldErrors.bio} hint="Tối đa 1000 ký tự" className="sm:col-span-2">
            <Textarea rows={3} maxLength={1000} value={bio} onChange={(e) => setBio(e.target.value)} />
          </Field>
          <Field label="Hình thức học" error={error?.fieldErrors.learningMode}>
            <NativeSelect className="w-full" value={learningMode}
                          onChange={(e) => setLearningMode(e.target.value as LearningMode | '')}>
              <NativeSelectOption value="">Chưa khai báo</NativeSelectOption>
              <NativeSelectOption value="ONLINE">Trực tuyến</NativeSelectOption>
              <NativeSelectOption value="OFFLINE">Gặp trực tiếp</NativeSelectOption>
              <NativeSelectOption value="ANY">Cách nào cũng được</NativeSelectOption>
            </NativeSelect>
          </Field>

          {/* Nhóm thông tin riêng tư (NFR-02): gom lại, có icon khóa để người dùng thấy ngay phần này không công khai. */}
          <FieldSet className="rounded-lg border p-4 sm:col-span-2">
            <FieldLegend className="flex items-center gap-2">
              <Lock className="size-4 text-muted-foreground" aria-hidden="true" />
              Liên hệ riêng tư
            </FieldLegend>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Số điện thoại" error={error?.fieldErrors.phone}
                     hint="Riêng tư: chỉ người đã nhận trao đổi với bạn mới thấy">
                <Input type="tel" maxLength={20} value={phone} onChange={(e) => setPhone(e.target.value)} />
              </Field>
              <Field label="Liên hệ khác" error={error?.fieldErrors.contactInfo}
                     hint="Ví dụ: Zalo, Facebook. Riêng tư như số điện thoại">
                <Textarea rows={2} maxLength={500} value={contactInfo} onChange={(e) => setContactInfo(e.target.value)} />
              </Field>
            </div>
          </FieldSet>
        </form>
      </CardContent>
      <CardFooter className="border-t">
        {/* form="profile-form": nút nằm ngoài thẻ <form> (ở chân thẻ) vẫn gửi đúng form này. */}
        <Button type="submit" form="profile-form" disabled={saving}>
          {saving && <Loader2 className="animate-spin" aria-hidden="true" />}
          {saving ? 'Đang lưu…' : 'Lưu hồ sơ'}
        </Button>
      </CardFooter>
    </Card>
  )
}

// ---------- Lịch rảnh ----------

function AvailabilityCard({ initial }: { initial: AvailabilitySlot[] }) {
  // Set các ô đang được tick, khóa dạng "2-EVENING".
  const [selected, setSelected] = useState(() => new Set(initial.map((s) => slotKey(s.dayOfWeek, s.period))))
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function toggle(key: string) {
    // State phải được thay bằng object MỚI (không sửa Set cũ), React mới biết để vẽ lại.
    setSelected((current) => {
      const next = new Set(current)
      if (next.has(key)) {
        next.delete(key)
      } else {
        next.add(key)
      }
      return next
    })
    setSaved(false)
  }

  async function save() {
    setSaving(true)
    setSaved(false)
    setError(null)
    const body: AvailabilitySlot[] = []
    for (let dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
      for (const { value } of PERIODS) {
        if (selected.has(slotKey(dayOfWeek, value))) {
          body.push({ dayOfWeek, period: value })
        }
      }
    }
    try {
      // PUT thay cả lịch: gửi đúng các ô đang tick; không tick ô nào = "chưa khai báo lịch".
      const result = await apiRequest<AvailabilitySlot[]>('/api/me/availability', { method: 'PUT', body })
      setSelected(new Set(result.map((s) => slotKey(s.dayOfWeek, s.period))))
      setSaved(true)
    } catch (e) {
      setError(toError(e).message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Lịch rảnh</CardTitle>
        <CardDescription>Tick các buổi bạn thường rảnh để được gợi ý người có lịch trùng.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && <Notice kind="error">{error}</Notice>}
        {saved && <Notice kind="success">Đã lưu lịch rảnh.</Notice>}
        <AvailabilityGrid selected={selected} onToggle={toggle} />
      </CardContent>
      <CardFooter className="flex flex-wrap items-center gap-3 border-t">
        <Button onClick={save} disabled={saving}>
          {saving && <Loader2 className="animate-spin" aria-hidden="true" />}
          {saving ? 'Đang lưu…' : 'Lưu lịch rảnh'}
        </Button>
        <span className="text-sm text-muted-foreground">Đã chọn {selected.size} buổi</span>
      </CardFooter>
    </Card>
  )
}
