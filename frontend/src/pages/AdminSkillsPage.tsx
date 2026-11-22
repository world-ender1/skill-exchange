import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Loader2, Pencil, Plus, Trash2 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOptGroup, NativeSelectOption } from '@/components/ui/native-select'
import { Separator } from '@/components/ui/separator'
import { Switch } from '@/components/ui/switch'
import { ApiError, apiRequest, toError } from '../api/client'
import type { AdminSkill, AdminSkillRelation, SkillCategory } from '../api/types'
import { Field } from '../components/Field'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'

type Catalog = {
  categories: SkillCategory[]
  skills: AdminSkill[]
  relations: AdminSkillRelation[]
}

/** Một hàm "báo đã đổi": trang tải lại danh mục rồi mới trả về. */
type OnChanged = () => Promise<void>

async function fetchCatalog(): Promise<Catalog> {
  const [categories, skills, relations] = await Promise.all([
    apiRequest<SkillCategory[]>('/api/skill-categories'), // D73: danh sách nhóm dùng API có sẵn (D40)
    apiRequest<AdminSkill[]>('/api/admin/skills'),
    apiRequest<AdminSkillRelation[]>('/api/admin/skill-relations'),
  ])
  return { categories, skills, relations }
}

/**
 * A01 – Quản trị danh mục kỹ năng (D76, TK 6.1): thêm kỹ năng và nhóm; mỗi nhóm một thẻ có kỹ năng của nhóm
 * (sửa tên/nhóm, ẩn/hiện); cuối trang là các cặp kỹ năng liên quan.
 *
 * Giao diện không kiểm lặp quy tắc của D74 (trùng tên, cùng nhóm, đổi nhóm khi còn quan hệ...):
 * gửi thẳng lên backend và hiện "detail" / lỗi theo ô mà backend trả về.
 */
export function AdminSkillsPage() {
  const [catalog, setCatalog] = useState<Catalog | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchCatalog()
      .then((loaded) => {
        if (!cancelled) {
          setCatalog(loaded)
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
  }, [])

  /**
   * Tải lại cả ba danh sách sau mỗi thao tác thành công, thay vì tự chèn kết quả vào danh sách đang có:
   * thứ tự là ORDER BY name của PostgreSQL (⚠3 của bộ ca), sắp lại ở trình duyệt có thể ra thứ tự khác.
   */
  async function reload() {
    try {
      setCatalog(await fetchCatalog())
    } catch (e) {
      setLoadError(toError(e).message)
    }
  }

  if (loadError) {
    return <LoadError message={loadError} onRetry={() => { setLoadError(null); void reload() }} />
  }
  if (!catalog) {
    return <PageSkeleton label="Đang tải danh mục kỹ năng" />
  }

  return (
    <div className="flex flex-col gap-6">
      <h1>Danh mục kỹ năng</h1>
      <Notice kind="info">
        Kỹ năng không xóa được, chỉ ẩn. Kỹ năng đã ẩn không còn trong danh sách chọn của sinh viên,
        nhưng người đang có vẫn giữ và vẫn được tính khi ghép cặp.
      </Notice>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg"><h2 className="text-lg">Thêm mới</h2></CardTitle>
        </CardHeader>
        {/* Từ 768 px hai form nằm cạnh nhau; hẹp hơn thì xếp chồng, ngăn bằng đường kẻ. */}
        <CardContent className="grid gap-6 md:grid-cols-[1fr_auto_1fr]">
          <AddSkillForm categories={catalog.categories} onChanged={reload} />
          <Separator orientation="vertical" className="hidden md:block" />
          <Separator className="md:hidden" />
          <AddCategoryForm onChanged={reload} />
        </CardContent>
      </Card>

      {catalog.categories.map((category) => (
        <CategoryCard
          key={category.id}
          category={category}
          // Gom theo categoryId để nhóm chưa có kỹ năng nào vẫn hiện (giữ thứ tự của GET /api/admin/skills).
          skills={catalog.skills.filter((s) => s.categoryId === category.id)}
          categories={catalog.categories}
          onChanged={reload}
        />
      ))}

      <RelationsCard catalog={catalog} onChanged={reload} />
    </div>
  )
}

/** Nút gửi của các form trên trang: vòng quay khi đang gửi, chữ đổi theo trạng thái. */
function SubmitButton({ busy, busyText, children }: { busy: boolean; busyText: string; children: ReactNode }) {
  return (
    <Button type="submit" disabled={busy}>
      {busy && <Loader2 className="animate-spin" aria-hidden="true" />}
      {busy ? busyText : children}
    </Button>
  )
}

// ---------- Trạng thái chung của một thao tác ghi ----------

/**
 * Hook tự viết: gom "đang gửi" + "lỗi" + cách chạy một lệnh ghi (POST/PUT/DELETE), dùng cho mọi form ở trang này.
 * run() trả true khi thành công. Thành công thì chờ trang tải lại danh mục rồi mới tắt "đang gửi",
 * để nút không bấm lại được trong lúc danh sách chưa cập nhật.
 */
function useAction(onChanged: OnChanged) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  async function run(send: () => Promise<unknown>): Promise<boolean> {
    setBusy(true)
    setError(null)
    try {
      await send()
    } catch (e) {
      setError(toError(e))
      setBusy(false)
      return false
    }
    await onChanged()
    setBusy(false)
    return true
  }

  return { busy, error, run }
}

/** Ô chọn chưa chọn gì gửi null: backend trả 400 theo trường (D74), giao diện không tự chặn. */
function toId(value: string): number | null {
  return value === '' ? null : Number(value)
}

// ---------- Thêm kỹ năng, thêm nhóm ----------

function AddSkillForm({ categories, onChanged }: { categories: SkillCategory[]; onChanged: OnChanged }) {
  const [name, setName] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [added, setAdded] = useState<string | null>(null)
  const { busy, error, run } = useAction(onChanged)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setAdded(null)
    const ok = await run(() =>
      apiRequest<AdminSkill>('/api/admin/skills', { method: 'POST', body: { name, categoryId: toId(categoryId) } }))
    if (ok) {
      setAdded(name.trim())
      setName('') // giữ nhóm đã chọn: thường thêm liền vài kỹ năng cùng nhóm
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <h3>Thêm kỹ năng</h3>
      {error && <Notice kind="error">{error.message}</Notice>}
      {added && <Notice kind="success">Đã thêm kỹ năng “{added}”.</Notice>}
      <Field label="Tên kỹ năng" error={error?.fieldErrors.name}>
        <Input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field label="Nhóm" error={error?.fieldErrors.categoryId}>
        <NativeSelect className="w-full" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <NativeSelectOption value="">Chọn nhóm…</NativeSelectOption>
          {categories.map((c) => <NativeSelectOption key={c.id} value={c.id}>{c.name}</NativeSelectOption>)}
        </NativeSelect>
      </Field>
      <div>
        <SubmitButton busy={busy} busyText="Đang thêm…"><Plus aria-hidden="true" />Thêm kỹ năng</SubmitButton>
      </div>
    </form>
  )
}

function AddCategoryForm({ onChanged }: { onChanged: OnChanged }) {
  const [name, setName] = useState('')
  const [added, setAdded] = useState<string | null>(null)
  const { busy, error, run } = useAction(onChanged)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setAdded(null)
    const ok = await run(() => apiRequest<SkillCategory>('/api/admin/skill-categories', { method: 'POST', body: { name } }))
    if (ok) {
      setAdded(name.trim())
      setName('')
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <h3>Thêm nhóm</h3>
      {error && <Notice kind="error">{error.message}</Notice>}
      {added && <Notice kind="success">Đã thêm nhóm “{added}”.</Notice>}
      <Field label="Tên nhóm" error={error?.fieldErrors.name}>
        <Input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <div>
        <SubmitButton busy={busy} busyText="Đang thêm…"><Plus aria-hidden="true" />Thêm nhóm</SubmitButton>
      </div>
    </form>
  )
}

// ---------- Một nhóm và các kỹ năng của nhóm ----------

type CategoryCardProps = {
  category: SkillCategory
  skills: AdminSkill[]
  categories: SkillCategory[]
  onChanged: OnChanged
}

function CategoryCard({ category, skills, categories, onChanged }: CategoryCardProps) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.name)
  const { busy, error, run } = useAction(onChanged)

  function startEdit() {
    setName(category.name) // lấy tên mới nhất, phòng khi nhóm vừa được đổi tên
    setEditing(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const ok = await run(() =>
      apiRequest<SkillCategory>(`/api/admin/skill-categories/${category.id}`, { method: 'PUT', body: { name } }))
    if (ok) {
      setEditing(false)
    }
  }

  return (
    <Card>
      {editing ? (
        <CardContent>
          <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
            {error && <Notice kind="error">{error.message}</Notice>}
            <Field label="Tên nhóm" error={error?.fieldErrors.name}>
              <Input value={name} onChange={(e) => setName(e.target.value)} />
            </Field>
            <div className="flex flex-wrap gap-2">
              <SubmitButton busy={busy} busyText="Đang lưu…">Lưu</SubmitButton>
              <Button type="button" variant="outline" onClick={() => setEditing(false)} disabled={busy}>Hủy</Button>
            </div>
          </form>
        </CardContent>
      ) : (
        <CardHeader>
          <CardTitle className="flex flex-wrap items-center gap-2 text-lg break-words">
            {/* Tên nhóm là tiêu đề cấp 2 của trang, như bản cũ. */}
            <h2 className="text-lg">{category.name}</h2>
            <Badge variant="secondary" className="tabular-nums">{skills.length}</Badge>
          </CardTitle>
          <CardAction>
            <Button variant="outline" size="sm" onClick={startEdit}>
              <Pencil aria-hidden="true" />
              Đổi tên nhóm
            </Button>
          </CardAction>
        </CardHeader>
      )}

      <CardContent>
        {skills.length === 0 ? (
          <p className="text-muted-foreground">Chưa có kỹ năng nào.</p>
        ) : (
          <ul className="flex flex-col divide-y">
            {skills.map((skill) => (
              <SkillRow key={skill.id} skill={skill} categories={categories} onChanged={onChanged} />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

type SkillRowProps = {
  skill: AdminSkill
  categories: SkillCategory[]
  onChanged: OnChanged
}

function SkillRow({ skill, categories, onChanged }: SkillRowProps) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(skill.name)
  const [categoryId, setCategoryId] = useState(String(skill.categoryId))
  const { busy, error, run } = useAction(onChanged)

  function startEdit() {
    setName(skill.name)
    setCategoryId(String(skill.categoryId))
    setEditing(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // PUT bắt buộc đủ ba trường (D73): giữ nguyên isActive khi chỉ sửa tên/nhóm.
    const ok = await run(() => apiRequest<AdminSkill>(`/api/admin/skills/${skill.id}`, {
      method: 'PUT',
      body: { name, categoryId: toId(categoryId), isActive: skill.isActive },
    }))
    if (ok) {
      setEditing(false)
    }
  }

  function toggleActive() {
    // Ẩn/hiện dùng chung PUT với sửa tên: gửi lại tên và nhóm đang lưu, chỉ đảo isActive.
    void run(() => apiRequest<AdminSkill>(`/api/admin/skills/${skill.id}`, {
      method: 'PUT',
      body: { name: skill.name, categoryId: skill.categoryId, isActive: !skill.isActive },
    }))
  }

  if (editing) {
    return (
      <li className="py-3">
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 rounded-lg border p-4">
          {error && <Notice kind="error">{error.message}</Notice>}
          <Field label="Tên kỹ năng" error={error?.fieldErrors.name}>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Nhóm" error={error?.fieldErrors.categoryId}
                 hint="Kỹ năng đang có quan hệ liên quan thì phải gỡ quan hệ trước khi đổi nhóm">
            <NativeSelect className="w-full" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              {categories.map((c) => <NativeSelectOption key={c.id} value={c.id}>{c.name}</NativeSelectOption>)}
            </NativeSelect>
          </Field>
          <div className="flex flex-wrap gap-2">
            <SubmitButton busy={busy} busyText="Đang lưu…">Lưu</SubmitButton>
            <Button type="button" variant="outline" onClick={() => setEditing(false)} disabled={busy}>Hủy</Button>
          </div>
        </form>
      </li>
    )
  }

  return (
    <li className="flex flex-col gap-2 py-2 first:pt-0 last:pb-0">
      {/* 360 px: các nút xuống dòng thay vì đẩy trang rộng ra (NFR-08, TC264). */}
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <span className="flex min-w-40 flex-1 flex-wrap items-center gap-2 break-words">
          <span className={skill.isActive ? 'font-medium' : 'font-medium text-muted-foreground'}>{skill.name}</span>
          {!skill.isActive && <Badge variant="outline">Đã ẩn</Badge>}
        </span>
        {/* Ẩn/hiện = công tắc "Đang dùng" (kế hoạch bước 3). Tên kỹ năng nằm trong aria-label để mỗi công tắc có tên riêng. */}
        <label className="flex items-center gap-2 text-sm">
          <Switch checked={skill.isActive} onCheckedChange={toggleActive} disabled={busy}
                  aria-label={`Đang dùng ${skill.name}`} />
          Đang dùng
        </label>
        <Button variant="ghost" size="sm" onClick={startEdit} disabled={busy} aria-label={`Sửa ${skill.name}`}>
          <Pencil aria-hidden="true" />
          Sửa
        </Button>
      </div>
      {error && <Notice kind="error">{error.message}</Notice>}
    </li>
  )
}

// ---------- Kỹ năng liên quan ----------

function RelationsCard({ catalog, onChanged }: { catalog: Catalog; onChanged: OnChanged }) {
  const [skillAId, setSkillAId] = useState('')
  const [skillBId, setSkillBId] = useState('')
  const { busy, error, run } = useAction(onChanged)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    // Gửi theo thứ tự bất kỳ; backend tự đổi về skillAId < skillBId (D73).
    const ok = await run(() => apiRequest<AdminSkillRelation>('/api/admin/skill-relations', {
      method: 'POST',
      body: { skillAId: toId(skillAId), skillBId: toId(skillBId) },
    }))
    if (ok) {
      setSkillAId('')
      setSkillBId('')
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">
          <h2 className="text-lg">Kỹ năng liên quan</h2>
        </CardTitle>
        <CardDescription>
          Hai kỹ năng cùng nhóm được đánh dấu liên quan thì người muốn học kỹ năng này vẫn được gợi ý người dạy kỹ năng kia.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        {catalog.relations.length === 0 ? (
          <p className="text-muted-foreground">Chưa có cặp nào.</p>
        ) : (
          <ul className="flex flex-col divide-y">
            {catalog.relations.map((relation) => (
              <RelationRow key={`${relation.skillAId}-${relation.skillBId}`} relation={relation} onChanged={onChanged} />
            ))}
          </ul>
        )}

        <Separator />

        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
          <h3>Thêm cặp</h3>
          {error && <Notice kind="error">{error.message}</Notice>}
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Kỹ năng thứ nhất" error={error?.fieldErrors.skillAId}>
              <SkillSelect value={skillAId} onChange={setSkillAId} catalog={catalog} />
            </Field>
            <Field label="Kỹ năng thứ hai" error={error?.fieldErrors.skillBId}>
              <SkillSelect value={skillBId} onChange={setSkillBId} catalog={catalog} />
            </Field>
          </div>
          <div>
            <SubmitButton busy={busy} busyText="Đang thêm…"><Plus aria-hidden="true" />Thêm cặp</SubmitButton>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

function RelationRow({ relation, onChanged }: { relation: AdminSkillRelation; onChanged: OnChanged }) {
  const { busy, error, run } = useAction(onChanged)

  function remove() {
    // Không hỏi xác nhận: xóa nhầm thì thêm lại được ngay (chốt bước 10).
    void run(() => apiRequest<void>(`/api/admin/skill-relations/${relation.skillAId}/${relation.skillBId}`, {
      method: 'DELETE',
    }))
  }

  return (
    <li className="flex flex-col gap-2 py-2 first:pt-0 last:pb-0">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
        <span className="min-w-40 flex-1 font-medium break-words">{relation.skillAName} – {relation.skillBName}</span>
        <Button variant="ghost" size="sm" onClick={remove} disabled={busy}
                aria-label={`Xóa cặp ${relation.skillAName} – ${relation.skillBName}`}>
          {busy ? <Loader2 className="animate-spin" aria-hidden="true" /> : <Trash2 aria-hidden="true" />}
          {busy ? 'Đang xóa…' : 'Xóa'}
        </Button>
      </div>
      {error && <Notice kind="error">{error.message}</Notice>}
    </li>
  )
}

type SkillSelectProps = {
  value: string
  onChange: (value: string) => void
  catalog: Catalog
  /** Do Field gắn vào để <label> và lỗi nối với ô chọn. */
  id?: string
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

/**
 * Ô chọn kỹ năng gom theo nhóm, gồm cả kỹ năng đã ẩn (D75: vẫn tạo được quan hệ với kỹ năng bị ẩn).
 * Không lọc theo nhóm của ô kia: "hai kỹ năng phải cùng nhóm" do backend kiểm (D76).
 */
function SkillSelect({ value, onChange, catalog, ...aria }: SkillSelectProps) {
  return (
    <NativeSelect className="w-full" value={value} onChange={(e) => onChange(e.target.value)} {...aria}>
      <NativeSelectOption value="">Chọn kỹ năng…</NativeSelectOption>
      {catalog.categories.map((category) => (
        <NativeSelectOptGroup key={category.id} label={category.name}>
          {catalog.skills.filter((s) => s.categoryId === category.id).map((s) => (
            <NativeSelectOption key={s.id} value={s.id}>{s.name}{s.isActive ? '' : ' (đã ẩn)'}</NativeSelectOption>
          ))}
        </NativeSelectOptGroup>
      ))}
    </NativeSelect>
  )
}
