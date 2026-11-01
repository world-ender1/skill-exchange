import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'

import { ApiError, apiRequest, toError } from '../api/client'
import type { AvailabilitySlot, LearningMode, MyProfile, Period } from '../api/types'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'

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
    return (
      <>
        <Notice kind="error">{loadError}</Notice>
        <p><button type="button" onClick={() => { setLoadError(null); setReloadKey((k) => k + 1) }}>Thử lại</button></p>
      </>
    )
  }
  if (!profile || !slots) {
    return <p>Đang tải hồ sơ…</p>
  }

  return (
    <>
      <h1>Hồ sơ của tôi</h1>
      {profile.status === 'PENDING_VERIFICATION' && (
        // D29: frontend dùng status để nhắc xác thực email.
        <Notice kind="info">
          Email <strong>{profile.email}</strong> chưa được xác thực. Bạn vẫn điền được hồ sơ, nhưng cần xác thực
          để dùng gợi ý ghép cặp. <Link to="/verify-email">Gửi lại link xác thực</Link>
        </Notice>
      )}
      {/* key: đổi người dùng/dữ liệu thì React tạo lại form với giá trị mới. */}
      <ProfileForm key={profile.id} initial={profile} onSaved={setProfile} />
      <AvailabilityGrid initial={slots} />
      <p className="muted">Tiếp theo: <Link to="/skills">khai báo kỹ năng dạy và muốn học</Link>.</p>
    </>
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
    <section className="card">
      <h2>Thông tin cá nhân</h2>
      <form onSubmit={handleSubmit} noValidate>
        {error && <Notice kind="error">{error.message}</Notice>}
        {saved && <Notice kind="success">Đã lưu hồ sơ.</Notice>}

        <Field label="Email trường" hint="Không đổi được">
          <input value={initial.email} disabled />
        </Field>
        <Field label="Họ tên" error={error?.fieldErrors.fullName}>
          <input maxLength={100} value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        <Field label="Giới thiệu" error={error?.fieldErrors.bio} hint="Tối đa 1000 ký tự">
          <textarea rows={3} maxLength={1000} value={bio} onChange={(e) => setBio(e.target.value)} />
        </Field>
        <Field label="Hình thức học" error={error?.fieldErrors.learningMode}>
          <select value={learningMode} onChange={(e) => setLearningMode(e.target.value as LearningMode | '')}>
            <option value="">Chưa khai báo</option>
            <option value="ONLINE">Trực tuyến</option>
            <option value="OFFLINE">Gặp trực tiếp</option>
            <option value="ANY">Cách nào cũng được</option>
          </select>
        </Field>
        <Field label="Số điện thoại" error={error?.fieldErrors.phone}
               hint="Riêng tư: chỉ người đã nhận trao đổi với bạn mới thấy">
          <input type="tel" maxLength={20} value={phone} onChange={(e) => setPhone(e.target.value)} />
        </Field>
        <Field label="Liên hệ khác" error={error?.fieldErrors.contactInfo} hint="Ví dụ: Zalo, Facebook. Riêng tư như số điện thoại">
          <textarea rows={2} maxLength={500} value={contactInfo} onChange={(e) => setContactInfo(e.target.value)} />
        </Field>

        <div className="actions">
          <button type="submit" className="primary" disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu hồ sơ'}</button>
        </div>
      </form>
    </section>
  )
}

// ---------- Lịch rảnh ----------

const DAYS = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật'] // dayOfWeek 1..7
const PERIODS: { value: Period; label: string }[] = [
  { value: 'MORNING', label: 'Sáng' },
  { value: 'AFTERNOON', label: 'Chiều' },
  { value: 'EVENING', label: 'Tối' },
]

const slotKey = (dayOfWeek: number, period: Period) => `${dayOfWeek}-${period}`

function AvailabilityGrid({ initial }: { initial: AvailabilitySlot[] }) {
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
    DAYS.forEach((_, index) => {
      for (const { value } of PERIODS) {
        if (selected.has(slotKey(index + 1, value))) {
          body.push({ dayOfWeek: index + 1, period: value })
        }
      }
    })
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
    <section className="card">
      <h2>Lịch rảnh</h2>
      <p className="muted small">Tick các buổi bạn thường rảnh để được gợi ý người có lịch trùng.</p>
      {error && <Notice kind="error">{error}</Notice>}
      {saved && <Notice kind="success">Đã lưu lịch rảnh.</Notice>}

      <div className="grid-scroll">
        <table className="availability">
          <thead>
            <tr>
              <th scope="col"></th>
              {DAYS.map((day) => <th key={day} scope="col">{day}</th>)}
            </tr>
          </thead>
          <tbody>
            {PERIODS.map(({ value, label }) => (
              <tr key={value}>
                <th scope="row">{label}</th>
                {DAYS.map((day, index) => {
                  const key = slotKey(index + 1, value)
                  return (
                    <td key={key} className={selected.has(key) ? 'on' : undefined}>
                      <input type="checkbox" aria-label={`${label} ${day}`}
                             checked={selected.has(key)} onChange={() => toggle(key)} />
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="actions">
        <button type="button" className="primary" onClick={save} disabled={saving}>
          {saving ? 'Đang lưu…' : 'Lưu lịch rảnh'}
        </button>
        <span className="muted small">Đã chọn {selected.size} buổi</span>
      </div>
    </section>
  )
}
