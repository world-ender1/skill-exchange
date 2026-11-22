import { useEffect, useState } from 'react'
import { Loader2, Plus, X } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { ApiError, apiRequest, toError } from '../api/client'
import type { LearnSkill, Skill, TeachSkill } from '../api/types'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { SkillCombobox } from '../components/SkillCombobox'

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
    return <LoadError message={loadError} onRetry={() => { setLoadError(null); setReloadKey((k) => k + 1) }} />
  }
  if (!data) {
    return <PageSkeleton label="Đang tải kỹ năng" />
  }

  // Kỹ năng đã LƯU ở danh sách kia: không cho chọn ở danh sách này (D19). Backend vẫn kiểm lại.
  const savedTeachIds = new Set(data.teach.map((s) => s.skillId))
  const savedLearnIds = new Set(data.learn.map((s) => s.skillId))

  return (
    <div className="flex flex-col gap-6">
      <h1>Kỹ năng của tôi</h1>
      <Notice kind="info">
        Một kỹ năng không thể vừa dạy vừa muốn học. Muốn chuyển một kỹ năng từ "muốn học" sang "dạy",
        hãy bỏ nó khỏi danh sách muốn học và <strong>lưu</strong> trước, rồi mới thêm vào danh sách dạy.
      </Notice>

      {/* Từ 1024 px hai danh sách nằm cạnh nhau; hẹp hơn thì xếp chồng. */}
      <div className="grid items-start gap-6 lg:grid-cols-2">
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
      </div>
    </div>
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
  const full = rows.length >= MAX_SKILLS

  /** Lý do một kỹ năng không chọn được, hiện mờ trong ô chọn như <option disabled> cũ. */
  function disabledReason(skill: Skill): string | null {
    if (otherListIds.has(skill.id)) return `đang ở danh sách ${otherListName}`
    if (chosenIds.has(skill.id)) return 'đã có trong danh sách'
    return null
  }

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

  /** Lỗi của dòng thứ index, ví dụ errors[].field = "[1].skillId" (chốt ở bước 9 tuần 3). */
  function rowError(index: number): string | undefined {
    return error?.fieldErrors[`[${index}].skillId`] ?? error?.fieldErrors[`[${index}].${levelField}`]
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg"><h2 className="text-lg">{title}</h2></CardTitle>
        <CardAction>
          <Badge variant="secondary" className="tabular-nums">{rows.length}/{MAX_SKILLS}</Badge>
        </CardAction>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && <Notice kind="error">{error.message}</Notice>}
        {saved && <Notice kind="success">Đã lưu.</Notice>}

        {rows.length === 0 ? (
          <p className="text-muted-foreground">Chưa có kỹ năng nào.</p>
        ) : (
          <ul className="flex flex-col divide-y">
            {rows.map((row, index) => (
              <li key={row.skillId} className="flex flex-col gap-1 py-2 first:pt-0 last:pb-0">
                <div className="flex items-center gap-2">
                  <span className="flex min-w-0 flex-1 flex-wrap items-center gap-2 font-medium">
                    <span className="break-words">{row.skillName}</span>
                    {/* D20: kỹ năng bị admin ẩn vẫn được giữ, nhưng nói rõ cho người dùng. */}
                    {!row.isActive && <Badge variant="outline">đã ngừng</Badge>}
                  </span>
                  <NativeSelect className="w-36 shrink-0" aria-label={`${levelLabel} ${row.skillName}`}
                                value={row.level ?? ''}
                                onChange={(e) => updateLevel(index, e.target.value === '' ? null : Number(e.target.value))}>
                    {!levelRequired && <NativeSelectOption value="">Không đặt mục tiêu</NativeSelectOption>}
                    {LEVELS.map((l) => <NativeSelectOption key={l.value} value={l.value}>{l.label}</NativeSelectOption>)}
                  </NativeSelect>
                  <Button variant="ghost" size="icon" aria-label={`Bỏ ${row.skillName}`} onClick={() => removeRow(index)}>
                    <X aria-hidden="true" />
                  </Button>
                </div>
                {rowError(index) && <p className="text-sm text-destructive">{rowError(index)}</p>}
              </li>
            ))}
          </ul>
        )}

        <div className="flex gap-2">
          <div className="min-w-0 flex-1">
            <SkillCombobox
              skills={catalog}
              value={newSkillId}
              onChange={setNewSkillId}
              ariaLabel="Chọn kỹ năng để thêm"
              placeholder={full ? 'Đã đủ 10 kỹ năng' : 'Chọn kỹ năng…'}
              disabledReason={disabledReason}
              disabled={full}
            />
          </div>
          <Button variant="outline" onClick={addRow} disabled={!newSkillId}>
            <Plus aria-hidden="true" />
            Thêm
          </Button>
        </div>
      </CardContent>
      <CardFooter className="border-t">
        <Button onClick={save} disabled={saving}>
          {saving && <Loader2 className="animate-spin" aria-hidden="true" />}
          {saving ? 'Đang lưu…' : 'Lưu danh sách'}
        </Button>
      </CardFooter>
    </Card>
  )
}
