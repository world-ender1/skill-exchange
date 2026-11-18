import { useState } from 'react'
import { BookOpen, CalendarDays, ChevronDown, GraduationCap, MonitorSmartphone, Star } from 'lucide-react'
import { Link } from 'react-router'
import type { ReactNode } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Progress } from '@/components/ui/progress'
import type { MatchItem, ScoreBreakdown, TheyLearnItem, YouLearnItem } from '../api/types'
import { formatMode, formatPercent, formatSlot } from '../format'
import { UserAvatar } from './UserAvatar'

/**
 * Một ứng viên trong S05 (wireframe TK 6.3). Mọi lý do hiển thị lấy thẳng từ response (NFR-09):
 * giao diện không tự tính lại điểm. Chưa có nút "Gửi yêu cầu" (bước 9 tuần 6).
 */
export function MatchCard({ match }: { match: MatchItem }) {
  const [showDetail, setShowDetail] = useState(false)

  return (
    <Card>
      <CardHeader className="flex flex-wrap items-start gap-x-4 gap-y-3">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <UserAvatar fullName={match.fullName} />
          <div className="flex min-w-0 flex-col gap-1">
            <h2 className="text-lg leading-snug break-words">{match.fullName}</h2>
            <div className="flex flex-wrap items-center gap-2">
              {match.reviewCount === 0 ? (
                <Badge variant="secondary">Mới</Badge>
              ) : (
                <span className="flex items-center gap-1 text-sm">
                  <Star className="size-4 fill-current text-warning" aria-hidden="true" />
                  {match.ratingAverage} ({match.reviewCount})
                </span>
              )}
              {match.hasPendingRequest && <Badge variant="outline">Đã gửi yêu cầu</Badge>}
            </div>
          </div>
        </div>
        {/* Mức phù hợp: số in nguyên văn theo D50; thanh chỉ để nhìn nên ẩn khỏi trình đọc màn hình. */}
        <div className="flex w-32 flex-col items-end gap-1">
          <span className="text-sm text-muted-foreground">Mức phù hợp</span>
          <span className="text-2xl font-bold text-primary tabular-nums">{formatPercent(match.score)}</span>
          <Progress value={match.score * 100} className="h-1.5" aria-hidden="true" />
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-2">
        {match.youLearn.map((item) => (
          <Reason key={`learn-${item.want}`} icon={<BookOpen />} label="Bạn học">{describeYouLearn(item)}</Reason>
        ))}
        {/* theyLearn chỉ có ở TWO_WAY (D49): vắng key thì không vẽ dòng nào. */}
        {match.theyLearn?.map((item) => (
          <Reason key={`teach-${item.want}`} icon={<GraduationCap />} label="Bạn dạy">{describeTheyLearn(item)}</Reason>
        ))}
        <Reason icon={<CalendarDays />} label="Lịch trùng">
          {match.commonSlots.length === 0 ? (
            'chưa có'
          ) : (
            <span className="inline-flex flex-wrap gap-1 align-middle">
              {match.commonSlots.map((slot) => (
                <Badge key={`${slot.dayOfWeek}-${slot.period}`} variant="outline">{formatSlot(slot)}</Badge>
              ))}
            </span>
          )}
        </Reason>
        <Reason icon={<MonitorSmartphone />} label="Hình thức">{formatMode(match.learningMode)}</Reason>

        <Collapsible open={showDetail} onOpenChange={setShowDetail}>
          <CollapsibleContent>
            <div className="mt-2 grid gap-4 sm:grid-cols-2">
              <ScoreTable title={match.backward ? 'Bạn học từ họ' : 'Điểm'} score={match.forward} />
              {match.backward && <ScoreTable title="Họ học từ bạn" score={match.backward} />}
            </div>
          </CollapsibleContent>
          {/* Hàng nút nằm trong Collapsible, ngay dưới phần chi tiết. */}
          <div className="mt-3 flex flex-wrap gap-2">
            {/* CollapsibleTrigger tự gắn aria-expanded, aria-controls cho nút. */}
            <CollapsibleTrigger asChild>
              <Button variant="outline" size="sm">
                <ChevronDown className={showDetail ? 'rotate-180 transition-transform' : 'transition-transform'}
                             aria-hidden="true" />
                {showDetail ? 'Ẩn chi tiết điểm' : 'Xem chi tiết điểm'}
              </Button>
            </CollapsibleTrigger>
            <Button asChild variant="ghost" size="sm">
              <Link to={`/users/${match.userId}`}>Xem hồ sơ</Link>
            </Button>
          </div>
        </Collapsible>
      </CardContent>
    </Card>
  )
}

/** Một dòng lý do có icon, ví dụ "Bạn học: Java (trùng, trình độ 3)". */
function Reason({ icon, label, children }: { icon: ReactNode; label: string; children: ReactNode }) {
  return (
    <p className="flex items-start gap-2 text-sm break-words">
      <span className="mt-0.5 text-muted-foreground [&_svg]:size-4" aria-hidden="true">{icon}</span>
      <span>
        <span className="font-medium">{label}:</span> {children}
      </span>
    </p>
  )
}

/** Mũi tên đi từ kỹ năng được dạy tới kỹ năng muốn học: "Java ← Kotlin" (kế hoạch tuần 4, bước 10). */
function describeYouLearn(item: YouLearnItem): string {
  return item.relation === 'EXACT'
    ? `${item.want} (trùng, trình độ ${item.theirLevel})`
    : `${item.want} ← ${item.teach} (kỹ năng liên quan, trình độ ${item.theirLevel})`
}

function describeTheyLearn(item: TheyLearnItem): string {
  return item.relation === 'EXACT'
    ? `${item.teach} (trùng, trình độ ${item.yourLevel})`
    : `${item.teach} → ${item.want} (kỹ năng liên quan, trình độ ${item.yourLevel})`
}

const COMPONENTS: { key: keyof ScoreBreakdown; label: string }[] = [
  { key: 'skillFit', label: 'Khớp kỹ năng' },
  { key: 'levelFit', label: 'Khớp trình độ' },
  { key: 'scheduleFit', label: 'Khớp lịch' },
  { key: 'modeFit', label: 'Khớp hình thức' },
  { key: 'reputation', label: 'Uy tín' },
]

/**
 * D và 5 thành phần, in nguyên số API trả để so với bảng tính tay (NFR-09).
 * Mọi thành phần nằm trong [0, 1] (spec 4.3) nên thanh dùng chung thang 0–1; thanh chỉ để nhìn, số mới là dữ liệu.
 */
function ScoreTable({ title, score }: { title: string; score: ScoreBreakdown }) {
  return (
    <table className="w-full text-sm">
      <caption className="mb-2 text-left font-medium">
        {title}: D = <span className="tabular-nums">{score.d}</span>
      </caption>
      <tbody>
        {COMPONENTS.map(({ key, label }) => (
          <tr key={key}>
            <th scope="row" className="w-32 py-1 pr-3 text-left font-normal text-muted-foreground">{label}</th>
            <td className="py-1"><Progress value={score[key] * 100} className="h-1.5" aria-hidden="true" /></td>
            <td className="w-16 py-1 pl-3 text-right tabular-nums">{score[key]}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
