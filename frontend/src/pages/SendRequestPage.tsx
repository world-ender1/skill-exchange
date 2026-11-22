import { useEffect, useId, useState } from 'react'
import type { FormEvent } from 'react'
import { Loader2, Send } from 'lucide-react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter } from '@/components/ui/card'
import {
  Field as FieldRoot,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldLabel,
  FieldLegend,
  FieldSet,
  FieldTitle,
} from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { ApiError, apiRequest, toError } from '../api/client'
import type {
  AvailabilitySlot,
  CreateRequestBody,
  LearnSkill,
  MeetingMode,
  PublicProfile,
  RequestItem,
  RequestType,
} from '../api/types'
import { Field } from '../components/Field'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'
import { formatMode, formatSlot } from '../format'
import { commonSlots, localDateString, nextSlotStart, toInstant } from '../requestTime'

type PageData = {
  receiver: PublicProfile
  myLearnSkills: LearnSkill[]
  mySlots: AvailabilitySlot[]
}

/**
 * S07 – Gửi yêu cầu (D95, Q7, Q8; wireframe TK 6.3). Mở từ nút "Gửi yêu cầu" ở S05 (thẻ ứng viên) và S06.
 *
 * Giao diện chỉ giúp điền đúng: mọi luật (kỹ năng có trong danh sách, Sim > 0, thời điểm ở tương lai, gửi trùng...)
 * do backend kiểm và trả lỗi theo ô hoặc detail (D89, D24). Vì vậy ô kỹ năng không tự lọc theo Sim.
 */
export function SendRequestPage() {
  // useParams đọc ":id" của /users/:id/request; useSearchParams đọc "?type=TWO_WAY" do thẻ S05 truyền.
  const { id = '' } = useParams()
  const [searchParams] = useSearchParams()
  const initialType: RequestType = searchParams.get('type') === 'TWO_WAY' ? 'TWO_WAY' : 'ONE_WAY'

  const [result, setResult] = useState<{ id: string; data?: PageData; error?: string } | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    // Ba API độc lập nên gọi song song. Người nhận không có, bị khóa, là admin... thì 404 (D89 bước 5).
    Promise.all([
      apiRequest<PublicProfile>(`/api/users/${encodeURIComponent(id)}`),
      apiRequest<LearnSkill[]>('/api/me/learn-skills'),
      apiRequest<AvailabilitySlot[]>('/api/me/availability'),
    ])
      .then(([receiver, myLearnSkills, mySlots]) => {
        if (!cancelled) setResult({ id, data: { receiver, myLearnSkills, mySlots } })
      })
      .catch((e: unknown) => {
        if (!cancelled) setResult({ id, error: toError(e).message })
      })
    return () => {
      cancelled = true
    }
  }, [id, reloadKey])

  // Đổi sang người nhận khác thì kết quả cũ (id khác) coi như chưa tải, như S06.
  if (!result || result.id !== id) {
    return <PageSkeleton label="Đang tải" />
  }
  if (result.error || !result.data) {
    return (
      <LoadError message={result.error ?? ''} onRetry={() => { setResult(null); setReloadKey((k) => k + 1) }} />
    )
  }

  const { receiver } = result.data
  return (
    <div className="flex flex-col gap-6">
      <h1 className="break-words">Gửi yêu cầu trao đổi tới {receiver.fullName}</h1>
      {/* key: mở S07 của người khác thì React tạo lại form trống, không giữ lựa chọn của người trước. */}
      <RequestForm key={receiver.id} data={result.data} initialType={initialType} />
    </div>
  )
}

const TYPE_OPTIONS: { value: RequestType; title: string; description: string }[] = [
  { value: 'TWO_WAY', title: 'Hai chiều', description: 'Bạn học một kỹ năng của họ và dạy lại họ một kỹ năng.' },
  { value: 'ONE_WAY', title: 'Một chiều', description: 'Bạn chỉ học, không phải dạy lại.' },
]

const MODES: MeetingMode[] = ['ONLINE', 'OFFLINE']

function RequestForm({ data, initialType }: { data: PageData; initialType: RequestType }) {
  const { receiver, myLearnSkills, mySlots } = data
  const navigate = useNavigate()
  const location = useLocation()
  const ids = useId() // tiền tố id cho các nút radio, để nhãn nối đúng nút

  // Ô chọn và ô nhập luôn giữ chuỗi ("" = chưa chọn); đổi sang số hoặc null lúc gửi.
  const [type, setType] = useState<RequestType>(initialType)
  const [senderLearnSkillId, setSenderLearnSkillId] = useState('')
  const [receiverLearnSkillId, setReceiverLearnSkillId] = useState('')
  const [date, setDate] = useState('')
  const [time, setTime] = useState('')
  const [duration, setDuration] = useState('90') // mặc định theo wireframe S07
  const [mode, setMode] = useState<MeetingMode>('ONLINE')
  const [locationOrLink, setLocationOrLink] = useState('')
  const [message, setMessage] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)
  // "Hôm nay" cho thuộc tính min của ô ngày: đọc đồng hồ một lần lúc mở form. Hàm truyền vào useState chỉ
  // chạy ở lần vẽ đầu; gọi new Date() thẳng trong phần vẽ thì mỗi lần vẽ lại ra một giá trị khác (luật purity).
  const [today] = useState(() => localDateString(new Date()))

  const sharedSlots = commonSlots(mySlots, receiver.availability)
  const startAtError = error?.fieldErrors.proposedStartAt
  const startAtMessageId = `${ids}-start-message`

  /** Bấm chip "Tối T3": điền ngày gần nhất có thứ đó mà 19:00 còn ở tương lai so với now (Q7). Sửa được. */
  function pickSlot(slot: AvailabilitySlot, now: Date) {
    const start = nextSlotStart(slot, now)
    setDate(start.date)
    setTime(start.time)
  }

  function cancel() {
    // key "default": S07 được mở thẳng bằng đường dẫn, không có trang trước trong ứng dụng để quay lại.
    if (location.key === 'default') {
      navigate(`/users/${receiver.id}`)
    } else {
      navigate(-1)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSending(true)
    setError(null)
    const minutes = Number(duration)
    const body: CreateRequestBody = {
      receiverId: receiver.id,
      type,
      senderLearnSkillId: toIdOrNull(senderLearnSkillId),
      // ONE_WAY không gửi kỹ năng chiều ngược, kể cả khi trước đó đã chọn ở chế độ hai chiều (D89 bước 2).
      receiverLearnSkillId: type === 'TWO_WAY' ? toIdOrNull(receiverLearnSkillId) : null,
      proposedStartAt: toInstant(date, time),
      // Ô trống hay số lẻ (1.5) gửi null để backend báo "Thời lượng từ 15 đến 480 phút" dưới ô.
      durationMinutes: duration !== '' && Number.isInteger(minutes) ? minutes : null,
      mode,
      locationOrLink,
      message,
    }
    try {
      await apiRequest<RequestItem>('/api/requests', { method: 'POST', body })
      // D95: sang S08, tab "Đã gửi". Trang rời đi nên không cần tắt trạng thái "đang gửi".
      navigate('/requests?box=sent')
    } catch (e) {
      setError(toError(e))
      setSending(false)
    }
  }

  return (
    <Card>
      <CardContent>
        <form id="request-form" onSubmit={handleSubmit} noValidate className="grid gap-5 sm:grid-cols-2">
          {/* detail của mọi lỗi: 409 gửi trùng, 400 gửi cho chính mình, "Dữ liệu không hợp lệ" kèm lỗi theo ô. */}
          {error && <div className="sm:col-span-2"><Notice kind="error">{error.message}</Notice></div>}

          {/* Loại: hai thẻ chọn, mỗi thẻ là một nhãn bọc nút radio nên bấm vào đâu trên thẻ cũng chọn được. */}
          <FieldSet className="gap-3 sm:col-span-2">
            <FieldLegend variant="label">Loại yêu cầu</FieldLegend>
            <RadioGroup value={type} onValueChange={(value) => setType(value as RequestType)}
                        className="grid gap-3 sm:grid-cols-2">
              {TYPE_OPTIONS.map((option) => (
                <FieldLabel key={option.value} htmlFor={`${ids}-type-${option.value}`}>
                  <FieldRoot orientation="horizontal">
                    <FieldContent>
                      <FieldTitle>{option.title}</FieldTitle>
                      <FieldDescription>{option.description}</FieldDescription>
                    </FieldContent>
                    <RadioGroupItem value={option.value} id={`${ids}-type-${option.value}`} />
                  </FieldRoot>
                </FieldLabel>
              ))}
            </RadioGroup>
          </FieldSet>

          {/* Q8: "Bạn muốn học" lấy danh sách học của tôi; Sim > 0 với kỹ năng dạy của người nhận do backend kiểm. */}
          <Field label="Bạn muốn học" error={error?.fieldErrors.senderLearnSkillId}
                 hint={myLearnSkills.length === 0
                   ? <>Bạn chưa khai báo kỹ năng muốn học. <TextLink to="/skills">Khai báo ở trang Kỹ năng</TextLink></>
                   : undefined}>
            <NativeSelect className="w-full" value={senderLearnSkillId}
                          onChange={(e) => setSenderLearnSkillId(e.target.value)}>
              <NativeSelectOption value="">Chọn kỹ năng</NativeSelectOption>
              {myLearnSkills.map((skill) => (
                <NativeSelectOption key={skill.skillId} value={skill.skillId}>{skill.skillName}</NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>

          {/* Chỉ yêu cầu hai chiều có ô này; lấy danh sách muốn học trong hồ sơ công khai của người nhận (Q8). */}
          {type === 'TWO_WAY' && (
            <Field label="Bạn sẽ dạy" error={error?.fieldErrors.receiverLearnSkillId}
                   hint={receiver.learnSkills.length === 0
                     ? 'Người này chưa khai báo kỹ năng muốn học, hãy gửi yêu cầu một chiều'
                     : `Kỹ năng ${receiver.fullName} muốn học`}>
              <NativeSelect className="w-full" value={receiverLearnSkillId}
                            onChange={(e) => setReceiverLearnSkillId(e.target.value)}>
                <NativeSelectOption value="">Chọn kỹ năng</NativeSelectOption>
                {receiver.learnSkills.map((skill) => (
                  <NativeSelectOption key={skill.skillId} value={skill.skillId}>{skill.skillName}</NativeSelectOption>
                ))}
              </NativeSelect>
            </Field>
          )}

          {/* Thời điểm: hai ô của trình duyệt (không thêm thư viện lịch) cùng chung một lỗi proposedStartAt. */}
          <FieldSet className="gap-2 sm:col-span-2" data-invalid={startAtError ? true : undefined}>
            <FieldLegend variant="label">Thời điểm</FieldLegend>
            <div className="flex flex-wrap gap-2">
              <Input type="date" aria-label="Ngày" className="w-auto min-w-0 flex-1 basis-40"
                     min={today} value={date} onChange={(e) => setDate(e.target.value)}
                     aria-invalid={startAtError ? true : undefined}
                     aria-describedby={startAtError ? startAtMessageId : undefined} />
              <Input type="time" aria-label="Giờ" className="w-32" value={time}
                     onChange={(e) => setTime(e.target.value)}
                     aria-invalid={startAtError ? true : undefined}
                     aria-describedby={startAtError ? startAtMessageId : undefined} />
            </div>
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <span className="text-muted-foreground">Gợi ý khung trùng:</span>
              {sharedSlots.length === 0
                ? <span className="text-muted-foreground">Không có buổi rảnh trùng</span>
                : sharedSlots.map((slot) => (
                  <Button key={`${slot.dayOfWeek}-${slot.period}`} type="button" variant="outline" size="sm"
                          onClick={() => pickSlot(slot, new Date())}>
                    {formatSlot(slot)}
                  </Button>
                ))}
            </div>
            {startAtError && <FieldError id={startAtMessageId}>{startAtError}</FieldError>}
          </FieldSet>

          <Field label="Thời lượng (phút)" error={error?.fieldErrors.durationMinutes} hint="Từ 15 đến 480 phút">
            <Input type="number" inputMode="numeric" min={15} max={480} step={15} value={duration}
                   onChange={(e) => setDuration(e.target.value)} />
          </Field>

          {/* Chỉ ONLINE, OFFLINE: một buổi học phải chốt một cách gặp (D89). */}
          <FieldSet className="gap-3">
            <FieldLegend variant="label">Hình thức</FieldLegend>
            <RadioGroup value={mode} onValueChange={(value) => setMode(value as MeetingMode)}
                        className="flex flex-wrap gap-x-6 gap-y-3">
              {MODES.map((value) => (
                <FieldRoot key={value} orientation="horizontal" className="w-auto">
                  <RadioGroupItem value={value} id={`${ids}-mode-${value}`} />
                  <FieldLabel htmlFor={`${ids}-mode-${value}`} className="font-normal">{formatMode(value)}</FieldLabel>
                </FieldRoot>
              ))}
            </RadioGroup>
          </FieldSet>

          {/* Q1, D91: người nhận chỉ thấy địa điểm/link sau khi chấp nhận; người gửi luôn xem lại được. */}
          <Field label="Địa điểm hoặc link" error={error?.fieldErrors.locationOrLink}
                 hint="Người nhận chỉ thấy sau khi chấp nhận" className="sm:col-span-2">
            <Input maxLength={500} value={locationOrLink} onChange={(e) => setLocationOrLink(e.target.value)}
                   placeholder={mode === 'ONLINE' ? 'https://meet.google.com/...' : 'Ví dụ: Thư viện trường, tầng 3'} />
          </Field>

          <Field label="Lời nhắn" error={error?.fieldErrors.message} hint="Không bắt buộc, tối đa 1000 ký tự"
                 className="sm:col-span-2">
            <Textarea rows={3} maxLength={1000} value={message} onChange={(e) => setMessage(e.target.value)} />
          </Field>
        </form>
      </CardContent>
      <CardFooter className="flex flex-wrap justify-end gap-2 border-t">
        <Button type="button" variant="outline" onClick={cancel}>Hủy</Button>
        {/* form="request-form": nút ở chân thẻ, ngoài thẻ <form>, vẫn gửi đúng form này (như S03). */}
        <Button type="submit" form="request-form" disabled={sending}>
          {sending ? <Loader2 className="animate-spin" aria-hidden="true" /> : <Send aria-hidden="true" />}
          {sending ? 'Đang gửi…' : 'Gửi yêu cầu'}
        </Button>
      </CardFooter>
    </Card>
  )
}

/** "" (chưa chọn) → null để backend báo "Chưa chọn kỹ năng..." dưới ô; còn lại là id số. */
function toIdOrNull(value: string): number | null {
  return value === '' ? null : Number(value)
}
