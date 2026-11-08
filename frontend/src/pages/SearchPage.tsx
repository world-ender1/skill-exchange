import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'

import { apiRequest, toError } from '../api/client'
import type { Skill, UserSearchResult } from '../api/types'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'
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

  useEffect(() => {
    // /api/skills chỉ trả kỹ năng đang hoạt động (D40), nên ô chọn không có kỹ năng đã ẩn (TC183).
    apiRequest<Skill[]>('/api/skills')
      .then(setSkills)
      .catch((e: unknown) => setError({ detail: toError(e).message }))
  }, [])

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

  const categories = [...new Set(skills.map((s) => s.categoryName))]

  return (
    <>
      <h1>Tìm kiếm</h1>
      <form className="card" onSubmit={search}>
        <Field label="Tên" error={error?.q} hint="Có phân biệt dấu: “an” không tìm ra “Ân”">
          <input type="search" value={q} onChange={(e) => setQ(e.target.value)} />
        </Field>
        <Field label="Kỹ năng người đó dạy">
          <select value={skillId} onChange={(e) => setSkillId(e.target.value)}>
            <option value="">Bất kỳ</option>
            {categories.map((category) => (
              <optgroup key={category} label={category}>
                {skills.filter((s) => s.categoryName === category).map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </optgroup>
            ))}
          </select>
        </Field>
        <div className="actions">
          <button type="submit" className="primary" disabled={searching}>
            {searching ? 'Đang tìm…' : 'Tìm'}
          </button>
        </div>
      </form>

      {error && !error.q && <Notice kind="error">{error.detail}</Notice>}

      {results && (results.length === 0
        ? <p className="muted">Không tìm thấy ai.</p>
        : (
          <ul className="result-list">
            {results.map((user) => (
              <li key={user.id} className="card">
                <h2 className="match-name"><Link to={`/users/${user.id}`}>{user.fullName}</Link></h2>
                <p className="small">Hình thức học: {formatMode(user.learningMode)}</p>
                <p className="small">
                  Dạy: {user.teachSkills.length === 0
                    ? 'chưa khai báo'
                    : user.teachSkills.map((s) => `${s.skillName} (${s.level})`).join(', ')}
                </p>
              </li>
            ))}
          </ul>
        ))}
      {results && results.length === 20 && (
        <p className="muted small">Chỉ hiện 20 người đầu tiên; hãy thêm điều kiện để thu hẹp.</p>
      )}
    </>
  )
}
