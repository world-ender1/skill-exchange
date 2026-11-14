import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'

import { ApiError, apiRequest, toError } from '../api/client'
import type { AdminSkill, AdminSkillRelation, SkillCategory } from '../api/types'
import { Field } from '../components/Field'
import { Notice } from '../components/Notice'

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
    return (
      <>
        <Notice kind="error">{loadError}</Notice>
        <p><button type="button" onClick={() => { setLoadError(null); void reload() }}>Thử lại</button></p>
      </>
    )
  }
  if (!catalog) {
    return <p>Đang tải danh mục kỹ năng…</p>
  }

  return (
    <>
      <h1>Danh mục kỹ năng</h1>
      <Notice kind="info">
        Kỹ năng không xóa được, chỉ ẩn. Kỹ năng đã ẩn không còn trong danh sách chọn của sinh viên,
        nhưng người đang có vẫn giữ và vẫn được tính khi ghép cặp.
      </Notice>

      <section className="card">
        <h2>Thêm mới</h2>
        <AddSkillForm categories={catalog.categories} onChanged={reload} />
        <hr className="divider" />
        <AddCategoryForm onChanged={reload} />
      </section>

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
    </>
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
    <form onSubmit={handleSubmit} noValidate>
      <h3>Thêm kỹ năng</h3>
      {error && <Notice kind="error">{error.message}</Notice>}
      {added && <Notice kind="success">Đã thêm kỹ năng “{added}”.</Notice>}
      <Field label="Tên kỹ năng" error={error?.fieldErrors.name}>
        <input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field label="Nhóm" error={error?.fieldErrors.categoryId}>
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Chọn nhóm…</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </Field>
      <div className="actions">
        <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang thêm…' : 'Thêm kỹ năng'}</button>
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
    <form onSubmit={handleSubmit} noValidate>
      <h3>Thêm nhóm</h3>
      {error && <Notice kind="error">{error.message}</Notice>}
      {added && <Notice kind="success">Đã thêm nhóm “{added}”.</Notice>}
      <Field label="Tên nhóm" error={error?.fieldErrors.name}>
        <input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <div className="actions">
        <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang thêm…' : 'Thêm nhóm'}</button>
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
    <section className="card">
      {editing ? (
        <form onSubmit={handleSubmit} noValidate className="edit-form">
          {error && <Notice kind="error">{error.message}</Notice>}
          <Field label="Tên nhóm" error={error?.fieldErrors.name}>
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <div className="actions">
            <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu'}</button>
            <button type="button" onClick={() => setEditing(false)} disabled={busy}>Hủy</button>
          </div>
        </form>
      ) : (
        <div className="card-head">
          <h2>{category.name} <span className="muted small">({skills.length})</span></h2>
          <button type="button" onClick={startEdit}>Đổi tên nhóm</button>
        </div>
      )}

      {skills.length === 0 ? (
        <p className="muted">Chưa có kỹ năng nào.</p>
      ) : (
        <ul className="admin-rows">
          {skills.map((skill) => (
            <SkillRow key={skill.id} skill={skill} categories={categories} onChanged={onChanged} />
          ))}
        </ul>
      )}
    </section>
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
      <li>
        <form onSubmit={handleSubmit} noValidate className="edit-form">
          {error && <Notice kind="error">{error.message}</Notice>}
          <Field label="Tên kỹ năng" error={error?.fieldErrors.name}>
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Nhóm" error={error?.fieldErrors.categoryId}
                 hint="Kỹ năng đang có quan hệ liên quan thì phải gỡ quan hệ trước khi đổi nhóm">
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
          <div className="actions">
            <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu'}</button>
            <button type="button" onClick={() => setEditing(false)} disabled={busy}>Hủy</button>
          </div>
        </form>
      </li>
    )
  }

  return (
    <li>
      <div className={skill.isActive ? 'admin-row' : 'admin-row is-hidden'}>
        <span className="skill-name">
          {skill.name}
          {!skill.isActive && <> <span className="badge badge-hidden">Đã ẩn</span></>}
        </span>
        <button type="button" onClick={startEdit} disabled={busy}>Sửa</button>
        <button type="button" onClick={toggleActive} disabled={busy}>
          {skill.isActive ? 'Ẩn' : 'Hiện'}
        </button>
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
    <section className="card">
      <h2>Kỹ năng liên quan</h2>
      <p className="muted small">
        Hai kỹ năng cùng nhóm được đánh dấu liên quan thì người muốn học kỹ năng này vẫn được gợi ý người dạy kỹ năng kia.
      </p>

      {catalog.relations.length === 0 ? (
        <p className="muted">Chưa có cặp nào.</p>
      ) : (
        <ul className="admin-rows relation-list">
          {catalog.relations.map((relation) => (
            <RelationRow key={`${relation.skillAId}-${relation.skillBId}`} relation={relation} onChanged={onChanged} />
          ))}
        </ul>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <h3>Thêm cặp</h3>
        {error && <Notice kind="error">{error.message}</Notice>}
        <Field label="Kỹ năng thứ nhất" error={error?.fieldErrors.skillAId}>
          <SkillSelect value={skillAId} onChange={setSkillAId} catalog={catalog} />
        </Field>
        <Field label="Kỹ năng thứ hai" error={error?.fieldErrors.skillBId}>
          <SkillSelect value={skillBId} onChange={setSkillBId} catalog={catalog} />
        </Field>
        <div className="actions">
          <button type="submit" className="primary" disabled={busy}>{busy ? 'Đang thêm…' : 'Thêm cặp'}</button>
        </div>
      </form>
    </section>
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
    <li>
      <div className="admin-row">
        <span className="skill-name">{relation.skillAName} – {relation.skillBName}</span>
        <button type="button" onClick={remove} disabled={busy}>{busy ? 'Đang xóa…' : 'Xóa'}</button>
      </div>
      {error && <Notice kind="error">{error.message}</Notice>}
    </li>
  )
}

type SkillSelectProps = {
  value: string
  onChange: (value: string) => void
  catalog: Catalog
}

/**
 * Ô chọn kỹ năng gom theo nhóm, gồm cả kỹ năng đã ẩn (D75: vẫn tạo được quan hệ với kỹ năng bị ẩn).
 * Không lọc theo nhóm của ô kia: "hai kỹ năng phải cùng nhóm" do backend kiểm (D76).
 */
function SkillSelect({ value, onChange, catalog }: SkillSelectProps) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Chọn kỹ năng…</option>
      {catalog.categories.map((category) => (
        <optgroup key={category.id} label={category.name}>
          {catalog.skills.filter((s) => s.categoryId === category.id).map((s) => (
            <option key={s.id} value={s.id}>{s.name}{s.isActive ? '' : ' (đã ẩn)'}</option>
          ))}
        </optgroup>
      ))}
    </select>
  )
}
