import { useEffect, useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { apiRequest, toError } from '../api/client'
import type { MatchList, MatchType } from '../api/types'
import { MatchCard } from '../components/MatchCard'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'

const TABS: { type: MatchType; label: string }[] = [
  { type: 'TWO_WAY', label: 'Trao đổi hai chiều' },
  { type: 'ONE_WAY', label: 'Tìm người hướng dẫn' },
]

type Loaded = Record<MatchType, MatchList>

/** S05 – Gợi ý ghép cặp (TK 6.1, 6.3). */
export function MatchesPage() {
  const [data, setData] = useState<Loaded | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [active, setActive] = useState<MatchType>('TWO_WAY')
  // Tăng để tải lại sau lỗi (nút "Thử lại", kế hoạch tuần 6 bước 1).
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    // Gọi cả hai nhóm ngay để hai tab đều có total (kế hoạch tuần 4, bước 10).
    Promise.all([
      apiRequest<MatchList>('/api/matches?type=TWO_WAY'),
      apiRequest<MatchList>('/api/matches?type=ONE_WAY'),
    ])
      .then(([twoWay, oneWay]) => {
        if (!cancelled) {
          setData({ TWO_WAY: twoWay, ONE_WAY: oneWay })
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(toError(e).message)
        }
      })
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  if (error) {
    return <LoadError message={error} onRetry={() => { setError(null); setReloadKey((k) => k + 1) }} />
  }
  if (!data) {
    return <PageSkeleton label="Đang tìm người phù hợp" />
  }

  // TK 4.5: chưa có kỹ năng muốn học thì không có gì để ghép (TC181).
  if (data.TWO_WAY.needsLearnSkills) {
    return (
      <div className="flex flex-col gap-6">
        <h1>Gợi ý ghép cặp</h1>
        <Notice kind="info">
          Bạn chưa khai báo kỹ năng muốn học nên chưa có gợi ý. Hãy thêm ít nhất một kỹ năng muốn học.
        </Notice>
        <p><TextLink to="/skills">Sang trang Kỹ năng của tôi</TextLink></p>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <h1>Gợi ý ghép cặp</h1>
      {/* Tabs của Radix: role="tab", aria-selected, chuyển tab bằng phím mũi tên. */}
      <Tabs value={active} onValueChange={(value) => setActive(value as MatchType)}>
        {/* Hai nhãn dài kèm số đếm rộng hơn màn 360 px: cho xuống dòng thay vì cuộn ngang (NFR-08). */}
        <TabsList className="w-full group-data-horizontal/tabs:h-auto sm:w-fit">
          {TABS.map(({ type, label }) => (
            <TabsTrigger key={type} value={type} className="h-auto py-1 whitespace-normal">

              {label}
              <Badge variant="secondary" className="tabular-nums">{data[type].total}</Badge>
            </TabsTrigger>
          ))}
        </TabsList>
        {TABS.map(({ type }) => {
          const list = data[type]
          return (
            <TabsContent key={type} value={type} className="mt-4 flex flex-col gap-4">
              {list.items.length === 0
                ? <p className="text-muted-foreground">Chưa có ai phù hợp ở nhóm này.</p>
                : list.items.map((match) => <MatchCard key={match.userId} match={match} />)}
              {list.total > list.items.length && (
                <p className="text-sm text-muted-foreground">
                  Đang hiện {list.items.length} trên {list.total} người phù hợp nhất.
                </p>
              )}
            </TabsContent>
          )
        })}
      </Tabs>
    </div>
  )
}
