import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import { apiRequest, toError } from '../api/client'
import type { MatchList, MatchType } from '../api/types'
import { MatchCard } from '../components/MatchCard'
import { Notice } from '../components/Notice'

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

  useEffect(() => {
    let cancelled = false
    // Gọi cả hai nhóm ngay để hai tab đều có total (kế hoạch bước 10).
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
  }, [])

  if (error) {
    return <Notice kind="error">{error}</Notice>
  }
  if (!data) {
    return <p>Đang tìm người phù hợp…</p>
  }

  // TK 4.5: chưa có kỹ năng muốn học thì không có gì để ghép (TC181).
  if (data.TWO_WAY.needsLearnSkills) {
    return (
      <>
        <h1>Gợi ý ghép cặp</h1>
        <Notice kind="info">
          Bạn chưa khai báo kỹ năng muốn học nên chưa có gợi ý. Hãy thêm ít nhất một kỹ năng muốn học.
        </Notice>
        <p><Link to="/skills">Sang trang Kỹ năng của tôi</Link></p>
      </>
    )
  }

  const list = data[active]
  return (
    <>
      <h1>Gợi ý ghép cặp</h1>
      <div className="tabs" role="tablist">
        {TABS.map(({ type, label }) => (
          <button key={type} type="button" role="tab" aria-selected={type === active}
                  className={type === active ? 'tab active' : 'tab'} onClick={() => setActive(type)}>
            {label} ({data[type].total})
          </button>
        ))}
      </div>

      {list.items.length === 0
        ? <p className="muted">Chưa có ai phù hợp ở nhóm này.</p>
        : list.items.map((match) => <MatchCard key={match.userId} match={match} />)}
      {list.total > list.items.length && (
        <p className="muted small">Đang hiện {list.items.length} trên {list.total} người phù hợp nhất.</p>
      )}
    </>
  )
}
