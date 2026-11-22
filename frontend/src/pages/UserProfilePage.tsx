import { useEffect, useState } from 'react'
import { MonitorSmartphone, Send } from 'lucide-react'
import { Link, useParams } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { apiRequest, toError } from '../api/client'
import type { MyProfile, PublicProfile } from '../api/types'
import { slotKey } from '../availability'
import { AvailabilityGrid } from '../components/AvailabilityGrid'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { UserAvatar } from '../components/UserAvatar'
import { formatLevel, formatMode } from '../format'

/**
 * S06 – Hồ sơ người khác (TK 4.3, 6.1). Chỉ thông tin công khai: API không trả email, số điện thoại,
 * liên hệ (NFR-02), nên trang này không có gì riêng tư để "lỡ" hiện ra.
 * Nút "Gửi yêu cầu" ở thẻ đầu trang mở S07 (D95, tuần 6 bước 9); phần đánh giá ở tuần 7.
 */
export function UserProfilePage() {
  // useParams: đọc phần ":id" của đường dẫn /users/:id.
  const { id } = useParams()
  const [result, setResult] = useState<
    { id: string; profile?: PublicProfile; myId?: number; error?: string } | null
  >(null)

  useEffect(() => {
    let cancelled = false
    // encodeURIComponent: id lấy từ URL người dùng gõ được, không ghép thô vào đường dẫn API.
    // /api/me gọi song song để biết đây có phải hồ sơ của chính mình (ẩn nút "Gửi yêu cầu", D95):
    // trang trong RequireStudent không nhận được thông tin người dùng từ Layout.
    Promise.all([
      apiRequest<PublicProfile>(`/api/users/${encodeURIComponent(id ?? '')}`),
      apiRequest<MyProfile>('/api/me'),
    ])
      .then(([profile, me]) => {
        if (!cancelled) setResult({ id: id ?? '', profile, myId: me.id })
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
    return <PageSkeleton label="Đang tải hồ sơ" />
  }
  if (result.error || !result.profile) {
    return <Notice kind="error">{result.error}</Notice>
  }

  const profile = result.profile
  const slots = new Set(profile.availability.map((s) => slotKey(s.dayOfWeek, s.period)))

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center gap-4">
            <UserAvatar fullName={profile.fullName} className="size-14 text-lg" />
            <div className="flex min-w-0 flex-1 flex-col gap-1">
              <h1 className="break-words">{profile.fullName}</h1>
              <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
                <MonitorSmartphone className="size-4" aria-hidden="true" />
                Hình thức học: {formatMode(profile.learningMode)}
              </span>
            </div>
            {/* D95: luôn hiện, trừ hồ sơ của chính mình. Đã có yêu cầu đang chờ thì S07 hiện lỗi 409 khi gửi.
                Không truyền ?type: mở từ S06 thì loại mặc định là ONE_WAY. */}
            {profile.id !== result.myId && (
              <Button asChild>
                <Link to={`/users/${profile.id}/request`}>
                  <Send aria-hidden="true" />
                  Gửi yêu cầu
                </Link>
              </Button>
            )}
          </div>
          {profile.bio
            ? <p className="break-words whitespace-pre-line">{profile.bio}</p>
            : <p className="text-muted-foreground">Chưa có giới thiệu.</p>}
        </CardContent>
      </Card>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Có thể dạy</CardTitle>
            <CardDescription>Trình độ do người dùng tự khai báo.</CardDescription>
          </CardHeader>
          <CardContent>
            {profile.teachSkills.length === 0
              ? <p className="text-muted-foreground">Chưa khai báo.</p>
              : (
                <ul className="flex flex-col divide-y">
                  {profile.teachSkills.map((skill) => (
                    <SkillLine key={skill.skillId} name={skill.skillName} category={skill.categoryName}
                               badge={formatLevel(skill.level)} />
                  ))}
                </ul>
              )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Muốn học</CardTitle>
          </CardHeader>
          <CardContent>
            {profile.learnSkills.length === 0
              ? <p className="text-muted-foreground">Chưa khai báo.</p>
              : (
                <ul className="flex flex-col divide-y">
                  {profile.learnSkills.map((skill) => (
                    <SkillLine key={skill.skillId} name={skill.skillName} category={skill.categoryName}
                               badge={skill.targetLevel === null
                                 ? 'chưa đặt mục tiêu'
                                 : `mục tiêu ${formatLevel(skill.targetLevel)}`} />
                  ))}
                </ul>
              )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Lịch rảnh</CardTitle>
        </CardHeader>
        <CardContent>
          {profile.availability.length === 0
            ? <p className="text-muted-foreground">Chưa khai báo.</p>
            // D84: cùng lưới với S03, ở chế độ chỉ đọc (không truyền onToggle).
            : <AvailabilityGrid selected={slots} />}
        </CardContent>
      </Card>
    </div>
  )
}

function SkillLine({ name, category, badge }: { name: string; category: string; badge: string }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-2 py-2 first:pt-0 last:pb-0">
      <span>
        <span className="font-medium">{name}</span> <span className="text-sm text-muted-foreground">({category})</span>
      </span>
      <Badge variant="secondary">{badge}</Badge>
    </li>
  )
}
