import { useEffect, useState } from 'react'
import { useParams } from 'react-router'

import { apiRequest, toError } from '../api/client'
import type { PublicProfile } from '../api/types'
import { Notice } from '../components/Notice'
import { formatLevel, formatMode, formatSlot } from '../format'

/**
 * S06 – Hồ sơ người khác (TK 4.3, 6.1). Chỉ thông tin công khai: API không trả email, số điện thoại,
 * liên hệ (NFR-02), nên trang này không có gì riêng tư để "lỡ" hiện ra.
 * Chưa có nút "Gửi yêu cầu" (tuần 6) và phần đánh giá (tuần 7), theo D31.
 */
export function UserProfilePage() {
  // useParams: đọc phần ":id" của đường dẫn /users/:id.
  const { id } = useParams()
  const [result, setResult] = useState<{ id: string; profile?: PublicProfile; error?: string } | null>(null)

  useEffect(() => {
    let cancelled = false
    // encodeURIComponent: id lấy từ URL người dùng gõ được, không ghép thô vào đường dẫn API.
    apiRequest<PublicProfile>(`/api/users/${encodeURIComponent(id ?? '')}`)
      .then((profile) => {
        if (!cancelled) setResult({ id: id ?? '', profile })
      })
      .catch((e: unknown) => {
        // 404 (không có, bị khóa, admin...) và 400 (/users/abc) đều hiện detail của backend.
        if (!cancelled) setResult({ id: id ?? '', error: toError(e).message })
      })
    return () => {
      cancelled = true
    }
  }, [id])

  // Khi đổi sang hồ sơ khác, kết quả cũ (id khác) coi như chưa tải.
  if (!result || result.id !== (id ?? '')) {
    return <p>Đang tải hồ sơ…</p>
  }
  if (result.error || !result.profile) {
    return <Notice kind="error">{result.error}</Notice>
  }

  const profile = result.profile
  return (
    <>
      <h1>{profile.fullName}</h1>
      <section className="card">
        {profile.bio ? <p>{profile.bio}</p> : <p className="muted">Chưa có giới thiệu.</p>}
        <p className="small">Hình thức học: {formatMode(profile.learningMode)}</p>
      </section>

      <section className="card">
        <h2>Có thể dạy</h2>
        <p className="muted small">Trình độ do người dùng tự khai báo.</p>
        {profile.teachSkills.length === 0
          ? <p className="muted">Chưa khai báo.</p>
          : (
            <ul className="plain-list">
              {profile.teachSkills.map((skill) => (
                <li key={skill.skillId}>
                  {skill.skillName} <span className="muted">({skill.categoryName})</span> – {formatLevel(skill.level)}
                </li>
              ))}
            </ul>
          )}
      </section>

      <section className="card">
        <h2>Muốn học</h2>
        {profile.learnSkills.length === 0
          ? <p className="muted">Chưa khai báo.</p>
          : (
            <ul className="plain-list">
              {profile.learnSkills.map((skill) => (
                <li key={skill.skillId}>
                  {skill.skillName} <span className="muted">({skill.categoryName})</span> –{' '}
                  {skill.targetLevel === null ? 'chưa đặt mục tiêu' : `mục tiêu ${formatLevel(skill.targetLevel)}`}
                </li>
              ))}
            </ul>
          )}
      </section>

      <section className="card">
        <h2>Lịch rảnh</h2>
        {profile.availability.length === 0
          ? <p className="muted">Chưa khai báo.</p>
          : <p>{profile.availability.map(formatSlot).join(', ')}</p>}
      </section>
    </>
  )
}
