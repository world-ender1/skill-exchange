import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { BookOpen, CalendarClock, ChevronRight, GraduationCap, MonitorSmartphone } from 'lucide-react'
import { Link } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { apiRequest, toError } from '../api/client'
import type { SessionItem, SessionStatus } from '../api/types'
import { Field } from '../components/Field'
import { InfoLine } from '../components/InfoLine'
import { LoadError } from '../components/LoadError'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'
import { UserAvatar } from '../components/UserAvatar'
import { formatDateTime, formatMode } from '../format'
import { SESSION_STATUS_BADGE, SESSION_STATUS_LABELS, viewerSkills } from '../sessionView'

/**
 * S09 – Phiên của tôi (D95, Q10). Danh sách để chọn phiên; địa điểm/link và liên hệ chỉ có ở trang chi tiết vì
 * API danh sách không trả (D92, NFR-02). Bộ lọc trạng thái gọi API với status như S08, không lọc ở giao diện.
 */
export function SessionsPage() {
  const [status, setStatus] = useState<SessionStatus | ''>('') // "" = Tất cả
  const query = status ? `?status=${status}` : ''

  // Ghi kèm query đã tải: đổi bộ lọc thì kết quả cũ (query khác) coi như chưa tải, hiện khung đang tải.
  const [result, setResult] = useState<{ query: string; items?: SessionItem[]; error?: string } | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    apiRequest<SessionItem[]>(`/api/sessions${query}`)
      .then((items) => {
        if (!cancelled) setResult({ query, items })
      })
      .catch((e: unknown) => {
        if (!cancelled) setResult({ query, error: toError(e).message })
      })
    return () => {
      cancelled = true
    }
  }, [query, reloadKey])

  let content: ReactNode
  if (!result || result.query !== query) {
    content = <PageSkeleton label="Đang tải phiên" />
  } else if (result.error || !result.items) {
    content = (
      <LoadError message={result.error ?? ''} onRetry={() => { setResult(null); setReloadKey((k) => k + 1) }} />
    )
  } else if (result.items.length === 0) {
    content = (
      <p className="text-muted-foreground">
        {status
          ? 'Không có phiên nào ở trạng thái này.'
          : 'Chưa có phiên nào. Phiên được tạo khi một yêu cầu được chấp nhận.'}
      </p>
    )
  } else {
    content = result.items.map((item) => <SessionCard key={item.id} item={item} />)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1>Phiên của tôi</h1>
        <Field label="Trạng thái" className="w-full sm:w-48">
          <NativeSelect className="w-full" value={status}
                        onChange={(e) => setStatus(e.target.value as SessionStatus | '')}>
            <NativeSelectOption value="">Tất cả</NativeSelectOption>
            {(Object.keys(SESSION_STATUS_LABELS) as SessionStatus[]).map((value) => (
              <NativeSelectOption key={value} value={value}>{SESSION_STATUS_LABELS[value]}</NativeSelectOption>
            ))}
          </NativeSelect>
        </Field>
      </div>
      <div className="flex flex-col gap-4">{content}</div>
    </div>
  )
}

function SessionCard({ item }: { item: SessionItem }) {
  const { youLearn, youTeach } = viewerSkills(item)

  return (
    <Card>
      <CardHeader className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <UserAvatar fullName={item.partner.fullName} />
          <div className="flex min-w-0 flex-col">
            <span className="text-sm text-muted-foreground">Học cùng</span>
            <TextLink to={`/users/${item.partner.id}`} className="break-words">{item.partner.fullName}</TextLink>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline">{item.type === 'TWO_WAY' ? 'Hai chiều' : 'Một chiều'}</Badge>
          <Badge variant={SESSION_STATUS_BADGE[item.status]}>{SESSION_STATUS_LABELS[item.status]}</Badge>
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-2">
        {youLearn && <InfoLine icon={<BookOpen />} label="Bạn học">{youLearn}</InfoLine>}
        {youTeach && <InfoLine icon={<GraduationCap />} label="Bạn dạy">{youTeach}</InfoLine>}
        <InfoLine icon={<CalendarClock />} label="Thời điểm">
          {formatDateTime(item.startAt)} ({item.durationMinutes} phút)
        </InfoLine>
        <InfoLine icon={<MonitorSmartphone />} label="Hình thức">{formatMode(item.mode)}</InfoLine>
      </CardContent>

      <CardFooter className="justify-end border-t">
        <Button asChild size="sm" variant="outline">
          <Link to={`/sessions/${item.id}`}>
            Xem chi tiết
            <ChevronRight aria-hidden="true" />
          </Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
