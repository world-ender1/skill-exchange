import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  BookOpen,
  CalendarClock,
  Check,
  GraduationCap,
  Loader2,
  MapPin,
  MessageSquare,
  MonitorSmartphone,
  NotebookPen,
  X,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { apiRequest, toError } from '../api/client'
import type { AcceptResponse, RequestItem, RequestStatus } from '../api/types'
import { Field } from '../components/Field'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'
import { UserAvatar } from '../components/UserAvatar'
import { formatDateTime, formatMode } from '../format'

type Box = 'received' | 'sent'

const BOXES: { value: Box; label: string }[] = [
  { value: 'received', label: 'Đã nhận' },
  { value: 'sent', label: 'Đã gửi' },
]

const STATUS_LABELS: Record<RequestStatus, string> = {
  PENDING: 'Đang chờ',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
}

/** Màu badge theo kế hoạch bước 10 (D79): chờ màu hổ phách, chấp nhận xanh lá, từ chối đỏ, hủy xám. */
const STATUS_BADGE = {
  PENDING: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'destructive',
  CANCELLED: 'secondary',
} as const satisfies Record<RequestStatus, string>

/** Kết quả của một lần bấm nút, hiện ở đầu danh sách. */
type Feedback = { kind: 'success' | 'error'; text: string }

/**
 * S08 – Hộp yêu cầu (D95, Q10; TK 6.1). Hai tab theo tham số ?box= trên URL, nên S07 gửi xong chuyển thẳng tới
 * tab "Đã gửi" được, và nút Back của trình duyệt quay về đúng tab. Không có trang chi tiết yêu cầu riêng.
 *
 * Nút chỉ hiện theo trạng thái và vai trò để dễ dùng; mọi luật (quá giờ, người gửi bị khóa, người kia vừa hủy...)
 * do backend kiểm. Gặp lỗi thì hiện detail và tải lại danh sách để thấy trạng thái mới (D90, D24).
 */
export function RequestsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const box: Box = searchParams.get('box') === 'sent' ? 'sent' : 'received'
  const [status, setStatus] = useState<RequestStatus | ''>('') // "" = Tất cả
  const query = status ? `box=${box}&status=${status}` : `box=${box}`

  const [result, setResult] = useState<{ query: string; items?: RequestItem[]; error?: string } | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  useEffect(() => {
    let cancelled = false
    apiRequest<RequestItem[]>(`/api/requests?${query}`)
      .then((items) => {
        if (!cancelled) setResult({ query, items })
      })
      .catch((e: unknown) => {
        if (!cancelled) setResult({ query, error: toError(e).message })
      })
    return () => {
      cancelled = true
    }
  }, [query, reloadKey])

  function changeBox(value: string) {
    setFeedback(null)
    setSearchParams({ box: value })
  }

  function changeStatus(value: string) {
    setFeedback(null)
    setStatus(value as RequestStatus | '')
  }

  /** Sau mọi lần bấm nút, thành công hay lỗi: báo kết quả rồi tải lại để thấy trạng thái mới nhất. */
  function finish(next: Feedback) {
    setFeedback(next)
    setReloadKey((k) => k + 1)
  }

  /** Chấp nhận, hủy: POST không có body, bấm là gửi ngay (kế hoạch chỉ ghi hộp thoại cho "Từ chối"). */
  async function act(item: RequestItem, action: 'accept' | 'cancel') {
    setBusyId(item.id)
    setFeedback(null)
    try {
      if (action === 'accept') {
        await apiRequest<AcceptResponse>(`/api/requests/${item.id}/accept`, { method: 'POST' })
        finish({ kind: 'success', text: 'Đã chấp nhận yêu cầu, phiên đã được tạo.' })
      } else {
        await apiRequest<RequestItem>(`/api/requests/${item.id}/cancel`, { method: 'POST' })
        finish({ kind: 'success', text: 'Đã hủy yêu cầu.' })
      }
    } catch (e) {
      // 409 khi bên kia vừa đổi trạng thái (ví dụ người gửi vừa hủy): detail nói lý do, danh sách mới cho thấy kết quả.
      finish({ kind: 'error', text: toError(e).message })
    } finally {
      setBusyId(null)
    }
  }

  let content: ReactNode
  // Đổi tab hay bộ lọc (query khác) thì hiện khung đang tải; tải lại sau khi bấm nút thì giữ danh sách cũ tới khi
  // có danh sách mới, để trang không nháy.
  if (!result || result.query !== query) {
    content = <PageSkeleton label="Đang tải yêu cầu" />
  } else if (result.error || !result.items) {
    content = (
      <LoadError message={result.error ?? ''} onRetry={() => { setResult(null); setReloadKey((k) => k + 1) }} />
    )
  } else if (result.items.length === 0) {
    content = (
      <p className="text-muted-foreground">
        {status ? 'Không có yêu cầu nào ở trạng thái này.' : 'Chưa có yêu cầu nào.'}
      </p>
    )
  } else {
    content = result.items.map((item) => (
      <RequestCard key={item.id} item={item} busy={busyId === item.id}
                   onAccept={() => act(item, 'accept')} onCancel={() => act(item, 'cancel')}
                   onRejectFinished={finish} />
    ))
  }

  return (
    <div className="flex flex-col gap-6">
      <h1>Yêu cầu trao đổi</h1>
      {/* Tabs của Radix: role="tab", aria-selected; giá trị tab đọc từ URL nên không cần state riêng. */}
      <Tabs value={box} onValueChange={changeBox}>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <TabsList>
            {BOXES.map(({ value, label }) => <TabsTrigger key={value} value={value}>{label}</TabsTrigger>)}
          </TabsList>
          <Field label="Trạng thái" className="w-full sm:w-48">
            <NativeSelect className="w-full" value={status} onChange={(e) => changeStatus(e.target.value)}>
              <NativeSelectOption value="">Tất cả</NativeSelectOption>
              {(Object.keys(STATUS_LABELS) as RequestStatus[]).map((value) => (
                <NativeSelectOption key={value} value={value}>{STATUS_LABELS[value]}</NativeSelectOption>
              ))}
            </NativeSelect>
          </Field>
        </div>
        {/* Radix chỉ vẽ nội dung của tab đang chọn, nên hai TabsContent dùng chung một danh sách. */}
        {BOXES.map(({ value }) => (
          <TabsContent key={value} value={value} className="mt-4 flex flex-col gap-4">
            {feedback && <Notice kind={feedback.kind}>{feedback.text}</Notice>}
            {content}
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}

// ---------- Một thẻ yêu cầu ----------

type RequestCardProps = {
  item: RequestItem
  busy: boolean
  onAccept: () => void
  onCancel: () => void
  onRejectFinished: (feedback: Feedback) => void
}

function RequestCard({ item, busy, onAccept, onCancel, onRejectFinished }: RequestCardProps) {
  const [rejectOpen, setRejectOpen] = useState(false)
  const iAmSender = item.myRole === 'SENDER'
  const other = iAmSender ? item.receiver : item.sender
  // Ghi theo góc nhìn người xem: người gửi học senderLearnSkill; người nhận dạy kỹ năng đó và học receiverLearnSkill.
  const youLearn = iAmSender ? item.senderLearnSkill.name : item.receiverLearnSkill?.name
  const youTeach = iAmSender ? item.receiverLearnSkill?.name : item.senderLearnSkill.name
  const pending = item.status === 'PENDING'

  return (
    <Card>
      <CardHeader className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <UserAvatar fullName={other.fullName} />
          <div className="flex min-w-0 flex-col">
            <span className="text-sm text-muted-foreground">{iAmSender ? 'Tới' : 'Từ'}</span>
            <TextLink to={`/users/${other.id}`} className="break-words">{other.fullName}</TextLink>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline">{item.type === 'TWO_WAY' ? 'Hai chiều' : 'Một chiều'}</Badge>
          <Badge variant={STATUS_BADGE[item.status]}>{STATUS_LABELS[item.status]}</Badge>
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-2">
        {youLearn && <InfoLine icon={<BookOpen />} label="Bạn học">{youLearn}</InfoLine>}
        {youTeach && <InfoLine icon={<GraduationCap />} label="Bạn dạy">{youTeach}</InfoLine>}
        <InfoLine icon={<CalendarClock />} label="Thời điểm">
          {formatDateTime(item.proposedStartAt)} ({item.durationMinutes} phút)
        </InfoLine>
        <InfoLine icon={<MonitorSmartphone />} label="Hình thức">{formatMode(item.mode)}</InfoLine>
        <LocationLine item={item} />
        {item.message && <InfoLine icon={<MessageSquare />} label="Lời nhắn">{item.message}</InfoLine>}
        {item.responseNote && <InfoLine icon={<NotebookPen />} label="Ghi chú từ chối">{item.responseNote}</InfoLine>}
        <p className="text-xs text-muted-foreground">Gửi lúc {formatDateTime(item.createdAt)}</p>
      </CardContent>

      {/* Nút theo trạng thái và vai trò (TK 5.1); backend vẫn kiểm lại mọi quyền. */}
      {(pending || item.sessionId !== null) && (
        <CardFooter className="flex flex-wrap gap-2 border-t">
          {pending && !iAmSender && (
            <>
              <Button size="sm" onClick={onAccept} disabled={busy}>
                {busy ? <Loader2 className="animate-spin" aria-hidden="true" /> : <Check aria-hidden="true" />}
                Chấp nhận
              </Button>
              <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
                <DialogTrigger asChild>
                  <Button size="sm" variant="outline" disabled={busy}>
                    <X aria-hidden="true" />
                    Từ chối
                  </Button>
                </DialogTrigger>
                {/* DialogContent chỉ được vẽ khi mở, nên mỗi lần mở là một form trống mới. */}
                <DialogContent>
                  <RejectForm item={item} onFinished={(feedback) => {
                    setRejectOpen(false)
                    onRejectFinished(feedback)
                  }} />
                </DialogContent>
              </Dialog>
            </>
          )}
          {pending && iAmSender && (
            <Button size="sm" variant="outline" onClick={onCancel} disabled={busy}>
              {busy ? <Loader2 className="animate-spin" aria-hidden="true" /> : <X aria-hidden="true" />}
              Hủy yêu cầu
            </Button>
          )}
          {/* sessionId chỉ có khi ACCEPTED (D91): một yêu cầu được chấp nhận sinh đúng một phiên. */}
          {item.sessionId !== null && (
            <Button asChild size="sm">
              <Link to={`/sessions/${item.sessionId}`}>Xem phiên</Link>
            </Button>
          )}
        </CardFooter>
      )}
    </Card>
  )
}

/**
 * Địa điểm/link theo Q1 (D91): người gửi luôn thấy; người nhận chỉ thấy khi ACCEPTED, trước đó API trả null.
 * Chỉ biến thành link khi bắt đầu bằng http:// hoặc https://, để chuỗi "javascript:..." không thành link chạy được.
 */
function LocationLine({ item }: { item: RequestItem }) {
  const value = item.locationOrLink
  if (value === null) {
    if (item.myRole === 'RECEIVER' && item.status === 'PENDING') {
      return (
        <InfoLine icon={<MapPin />} label="Địa điểm/link">
          <span className="text-muted-foreground">Hiện sau khi bạn chấp nhận</span>
        </InfoLine>
      )
    }
    return null
  }
  return (
    <InfoLine icon={<MapPin />} label="Địa điểm/link">
      {/^https?:\/\//i.test(value)
        ? <a href={value} target="_blank" rel="noopener noreferrer"
             className="break-all text-primary underline-offset-4 hover:underline">{value}</a>
        : value}
    </InfoLine>
  )
}

/** Một dòng thông tin có icon, cùng kiểu với dòng lý do của thẻ S05. */
function InfoLine({ icon, label, children }: { icon: ReactNode; label: string; children: ReactNode }) {
  return (
    <p className="flex items-start gap-2 text-sm break-words">
      <span className="mt-0.5 text-muted-foreground [&_svg]:size-4" aria-hidden="true">{icon}</span>
      <span className="min-w-0">
        <span className="font-medium">{label}:</span> {children}
      </span>
    </p>
  )
}

// ---------- Hộp thoại từ chối ----------

/**
 * Ghi chú không bắt buộc (D90). Lỗi của ô note (dài hơn 1000 ký tự) hiện dưới ô, hộp thoại vẫn mở; lỗi khác
 * (409 người gửi vừa hủy...) thì đóng hộp thoại và báo ở đầu danh sách như các nút khác.
 */
function RejectForm({ item, onFinished }: { item: RequestItem; onFinished: (feedback: Feedback) => void }) {
  const [note, setNote] = useState('')
  const [sending, setSending] = useState(false)
  const [noteError, setNoteError] = useState<string | undefined>()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSending(true)
    setNoteError(undefined)
    try {
      await apiRequest<RequestItem>(`/api/requests/${item.id}/reject`, { method: 'POST', body: { note } })
      onFinished({ kind: 'success', text: 'Đã từ chối yêu cầu.' })
    } catch (e) {
      const error = toError(e)
      if (error.fieldErrors.note) {
        setNoteError(error.fieldErrors.note)
        setSending(false)
      } else {
        onFinished({ kind: 'error', text: error.message })
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <DialogHeader>
        <DialogTitle>Từ chối yêu cầu của {item.sender.fullName}</DialogTitle>
        <DialogDescription>Ghi chú không bắt buộc. Người gửi sẽ thấy ghi chú này.</DialogDescription>
      </DialogHeader>
      <Field label="Ghi chú" error={noteError} hint="Tối đa 1000 ký tự">
        <Textarea rows={3} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} />
      </Field>
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" variant="outline" disabled={sending}>Quay lại</Button>
        </DialogClose>
        <Button type="submit" variant="destructive" disabled={sending}>
          {sending && <Loader2 className="animate-spin" aria-hidden="true" />}
          Từ chối
        </Button>
      </DialogFooter>
    </form>
  )
}
