import { useEffect, useState } from 'react'

import { ApiError, apiRequest, toError } from '../api/client'
import type { LearnSkill, Skill, TeachSkill } from '../api/types'
import { Notice } from '../components/Notice'

/** Thang trình độ của spec mục 4.6. */
const LEVELS = [
  { value: 1, label: '1 – Mới bắt đầu' },
  { value: 2, label: '2 – Cơ bản' },
  { value: 3, label: '3 – Khá' },
  { value: 4, label: '4 – Thành thạo' },
]

const MAX_SKILLS = 10 // D21

/** Một dòng trong danh sách đang sửa (dùng chung cho dạy và học). */
type Row = {
  skillId: number
  skillName: string
  isActive: boolean
  level: number | null
}

type Loaded = {
  catalog: Skill[]
  teach: TeachSkill[]
  learn: LearnSkill[]
}

/** S04 – Kỹ năng của tôi: danh sách dạy (bắt buộc trình độ) và muốn học (mục tiêu tùy chọn). */
export function SkillsPage() {
  const [data, setData] = useState<Loaded | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    Promise.all([
      apiRequest<Skill[]>('/api/skills'),
      apiRequest<TeachSkill[]>('/api/me/teach-skills'),
      apiRequest<LearnSkill[]>('/api/me/learn-skills'),
    ])
      .then(([catalog, teach, learn]) => {
        if (!cancelled) {
          setData({ catalog, teach, learn })
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
  if (!data) {
    return <p>Đang tải kỹ năng…</p>
  }

  // Kỹ năng đã LƯU ở danh sách kia: không cho chọn ở danh sách này (D19). Backend vẫn kiểm lại.
  const savedTeachIds = new Set(data.teach.map((s) => s.skillId))
  const savedLearnIds = new Set(data.learn.map((s) => s.skillId))

  return (
    <>
      <h1>Kỹ năng của tôi</h1>
      <Notice kind="info">
        Một kỹ năng không thể vừa dạy vừa muốn học. Muốn chuyển một kỹ năng từ "muốn học" sang "dạy",
        hãy bỏ nó khỏi danh sách muốn học và <strong>lưu</strong> trước, rồi mới thêm vào danh sách dạy.
      </Notice>

      <SkillListEditor
        title="Kỹ năng tôi có thể dạy"
        path="/api/me/teach-skills"
        levelLabel="Trình độ"
        levelRequired
        catalog={data.catalog}
        initial={data.teach}
        otherListIds={savedLearnIds}
        otherListName="muốn học"
        onSaved={(rows) => setData((d) => d && { ...d, teach: rows as TeachSkill[] })}
      />
      <SkillListEditor
        title="Kỹ năng tôi muốn học"
        path="/api/me/learn-skills"
        levelLabel="Mục tiêu"
        levelRequired={false}
        catalog={data.catalog}
        initial={data.learn.map((s) => ({ ...s, level: s.targetLevel }))}
        otherListIds={savedTeachIds}
        otherListName="dạy"
        onSaved={(rows) => setData((d) => d && { ...d, learn: rows as LearnSkill[] })}
      />
    </>
  )
}

type EditorProps = {
  title: string
  path: '/api/me/teach-skills' | '/api/me/learn-skills'
  levelLabel: string
  /** Dạy: bắt buộc trình độ (field "level"). Học: mục tiêu có thể bỏ trống (field "targetLevel"). */
  levelRequired: boolean
  catalog: Skill[]
  initial: Row[]
  otherListIds: Set<number>
  otherListName: string
  onSaved: (saved: TeachSkill[] | LearnSkill[]) => void
}

function SkillListEditor(props: EditorProps) {
  const { title, path, levelLabel, levelRequired, catalog, initial, otherListIds, otherListName, onSaved } = props
  const levelField = levelRequired ? 'level' : 'targetLevel'

  const [rows, setRows] = useState<Row[]>(initial)
  const [newSkillId, setNewSkillId] = useState('')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const chosenIds = new Set(rows.map((r) => r.skillId))
  // Gộp danh mục theo nhóm để hiện trong <optgroup>.
  const categories = [...new Set(catalog.map((s) => s.categoryName))]

  function addRow() {
    const skill = catalog.find((s) => s.id === Number(newSkillId))
    if (!skill) {
      return
    }
    setRows([...rows, { skillId: skill.id, skillName: skill.name, isActive: true, level: levelRequired ? 1 : null }])
    setNewSkillId('')
    setSaved(false)
  }

  function updateLevel(index: number, level: number | null) {
    setRows(rows.map((row, i) => (i === index ? { ...row, level } : row)))
    setSaved(false)
  }

  function removeRow(index: number) {
    setRows(rows.filter((_, i) => i !== index))
    setSaved(false)
  }

  async function save() {
    setSaving(true)
    setSaved(false)
    setError(null)
    try {
      // PUT thay cả danh sách; thứ tự gửi lên cũng là thứ tự "[i]" trong errors của backend.
      const body = rows.map((row) => ({ skillId: row.skillId, [levelField]: row.level }))
      const result = await apiRequest<TeachSkill[] | LearnSkill[]>(path, { method: 'PUT', body })
      setRows(result.map((s) => ({ ...s, level: 'level' in s ? s.level : s.targetLevel })))
      onSaved(result)
      setSaved(true)
    } catch (e) {
      setError(toError(e))
    } finally {
      setSaving(false)
    }
  }

  /** Lỗi của dòng thứ index, ví dụ errors[].field = "[1].skillId" (chốt ở bước 9). */
  function rowError(index: number): string | undefined {
    return error?.fieldErrors[`[${index}].skillId`] ?? error?.fieldErrors[`[${index}].${levelField}`]
  }

  return (
    <section className="card">
      <h2>{title}</h2>
      {error && <Notice kind="error">{error.message}</Notice>}
      {saved && <Notice kind="success">Đã lưu.</Notice>}

      {rows.length === 0 ? (
        <p className="muted">Chưa có kỹ năng nào.</p>
      ) : (
        <ul className="skill-rows">
          {rows.map((row, index) => (
            <li key={row.skillId}>
              <div className="skill-row">
                <span className="skill-name">
                  {row.skillName}
                  {/* D20: kỹ năng bị admin ẩn vẫn được giữ, nhưng nói rõ cho người dùng. */}
                  {!row.isActive && <span className="muted small"> (đã ngừng)</span>}
                </span>
                <select aria-label={`${levelLabel} ${row.skillName}`} value={row.level ?? ''}
                        onChange={(e) => updateLevel(index, e.target.value === '' ? null : Number(e.target.value))}>
                  {!levelRequired && <option value="">Không đặt mục tiêu</option>}
                  {LEVELS.map((l) => <option key={l.value} value={l.value}>{l.label}</option>)}
                </select>
                <button type="button" onClick={() => removeRow(index)}>Bỏ</button>
              </div>
              {rowError(index) && <span className="field-error">{rowError(index)}</span>}
            </li>
          ))}
        </ul>
      )}

      <div className="actions">
        <select aria-label="Chọn kỹ năng để thêm" value={newSkillId} onChange={(e) => setNewSkillId(e.target.value)}
                disabled={rows.length >= MAX_SKILLS} className="grow">
          <option value="">{rows.length >= MAX_SKILLS ? 'Đã đủ 10 kỹ năng' : 'Chọn kỹ năng…'}</option>
          {categories.map((category) => (
            <optgroup key={category} label={category}>
              {catalog.filter((s) => s.categoryName === category).map((s) => (
                <option key={s.id} value={s.id} disabled={chosenIds.has(s.id) || otherListIds.has(s.id)}>
                  {s.name}{otherListIds.has(s.id) ? ` (đang ở danh sách ${otherListName})` : ''}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
        <button type="button" onClick={addRow} disabled={!newSkillId}>Thêm</button>
      </div>

      <div className="actions">
        <button type="button" className="primary" onClick={save} disabled={saving}>
          {saving ? 'Đang lưu…' : 'Lưu danh sách'}
        </button>
        <span className="muted small">{rows.length}/{MAX_SKILLS} kỹ năng</span>
      </div>
    </section>
  )
}
