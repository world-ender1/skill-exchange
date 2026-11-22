import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Loader2, MonitorSmartphone, Search, SearchX } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { apiRequest, toError } from '../api/client'
import type { Skill, UserSearchResult } from '../api/types'
import { Field } from '../components/Field'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { SkillCombobox } from '../components/SkillCombobox'
import { TextLink } from '../components/TextLink'
import { UserAvatar } from '../components/UserAvatar'
import { formatMode } from '../format'

/**
 * S10 – Tìm kiếm theo tên và/hoặc kỹ năng dạy (TK 4.3, 6.1; D45).
 * Luật "ít nhất một tham số", cắt khoảng trắng, độ dài tối đa đều do backend kiểm; trang chỉ hiện detail/lỗi ô.
 */
export function SearchPage() {
  const [skills, setSkills] = useState<Skill[]>([])
  const [q, setQ] = useState('')
  const [skillId, setSkillId] = useState('')
  const [results, setResults] = useState<UserSearchResult[] | null>(null)
  const [error, setError] = useState<{ detail: string; q?: string } | null>(null)
  const [searching, setSearching] = useState(false)
  // Lỗi tải danh sách kỹ năng (ô chọn) tách khỏi lỗi tìm kiếm: lỗi tải có nút "Thử lại" (kế hoạch tuần 6 bước 1),
  // lỗi tìm kiếm thì bấm "Tìm" lại.
  const [skillsError, setSkillsError] = useState<string | null>(null)
  const [skillsReloadKey, setSkillsReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    // /api/skills chỉ trả kỹ năng đang hoạt động (D40), nên ô chọn không có kỹ năng đã ẩn (TC183).
    apiRequest<Skill[]>('/api/skills')
      .then((list) => {
        if (!cancelled) setSkills(list)
      })
      .catch((e: unknown) => {
        if (!cancelled) setSkillsError(toError(e).message)
      })
    return () => {
      cancelled = true
    }
  }, [skillsReloadKey])

  async function search(event: FormEvent) {
    event.preventDefault()
    // URLSearchParams tự mã hóa ký tự đặc biệt: "%" thành "%25", khoảng trắng thành "+".
    const params = new URLSearchParams()
    if (q !== '') params.set('q', q)
    if (skillId !== '') params.set('skillId', skillId)

    setSearching(true)
    setError(null)
    try {
      setResults(await apiRequest<UserSearchResult[]>(`/api/users/search?${params}`))
    } catch (e) {
      const apiError = toError(e)
      setResults(null)
      setError({ detail: apiError.message, q: apiError.fieldErrors.q })
    } finally {
      setSearching(false)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <h1>Tìm kiếm</h1>
      <Card>
        <CardContent>
          {/* Từ 640 px ba phần nằm trên một hàng; hẹp hơn thì xếp dọc. */}
          <form onSubmit={search} className="grid items-start gap-4 sm:grid-cols-[1fr_1fr_auto]">
            <Field label="Tên" error={error?.q} hint="Có phân biệt dấu: “an” không tìm ra “Ân”">
              <Input type="search" value={q} onChange={(e) => setQ(e.target.value)} />
            </Field>
            <Field label="Kỹ năng người đó dạy">
              <SkillCombobox skills={skills} value={skillId} onChange={setSkillId}
                             placeholder="Bất kỳ" emptyOptionLabel="Bất kỳ" />
            </Field>
            {/* Nút thẳng hàng với ô nhập: chừa một dòng trống bằng chiều cao nhãn ở màn rộng. */}
            <Button type="submit" disabled={searching} className="sm:mt-7">
              {searching
                ? <Loader2 className="animate-spin" aria-hidden="true" />
                : <Search aria-hidden="true" />}
              {searching ? 'Đang tìm…' : 'Tìm'}
            </Button>
          </form>
        </CardContent>
      </Card>

      {skillsError && (
        <LoadError message={skillsError} onRetry={() => { setSkillsError(null); setSkillsReloadKey((k) => k + 1) }} />
      )}
      {error && !error.q && <Notice kind="error">{error.detail}</Notice>}

      {results && (results.length === 0
        ? (
          <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed px-4 py-10 text-center">
            <SearchX className="size-8 text-muted-foreground" aria-hidden="true" />
            <p className="font-medium">Không tìm thấy ai.</p>
            <p className="text-sm text-muted-foreground">Thử bớt một điều kiện, hoặc tìm theo kỹ năng khác.</p>
          </div>
        )
        : (
          <ul className="grid gap-4 sm:grid-cols-2">
            {results.map((user) => (
              // min-w-0: ô lưới mặc định không co nhỏ hơn từ dài nhất, nên tên 100 ký tự không có dấu cách
              // đẩy trang 360 px rộng ra (TC308); break-words của tiêu đề chỉ có tác dụng khi ô được co.
              <li key={user.id} className="min-w-0">
                <Card className="h-full">
                  <CardContent className="flex flex-col gap-3">
                    <div className="flex items-center gap-3">
                      <UserAvatar fullName={user.fullName} />
                      <h2 className="min-w-0 text-base break-words">
                        <TextLink to={`/users/${user.id}`}>{user.fullName}</TextLink>
                      </h2>
                    </div>
                    <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                      <MonitorSmartphone className="size-4" aria-hidden="true" />
                      Hình thức học: {formatMode(user.learningMode)}
                    </p>
                    <div className="flex flex-wrap items-center gap-1.5 text-sm">
                      <span className="font-medium">Dạy:</span>
                      {user.teachSkills.length === 0
                        ? <span className="text-muted-foreground">chưa khai báo</span>
                        : user.teachSkills.map((s) => (
                          <Badge key={s.skillName} variant="secondary">{s.skillName} ({s.level})</Badge>
                        ))}
                    </div>
                  </CardContent>
                </Card>
              </li>
            ))}
          </ul>
        ))}
      {results && results.length === 20 && (
        <p className="text-sm text-muted-foreground">Chỉ hiện 20 người đầu tiên; hãy thêm điều kiện để thu hẹp.</p>
      )}
    </div>
  )
}
