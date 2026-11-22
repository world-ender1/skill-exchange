import { useEffect, useId, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import {
  ArrowLeft,
  BookOpen,
  CalendarClock,
  Check,
  CheckCheck,
  Flag,
  GraduationCap,
  Loader2,
  MapPin,
  MessageCircle,
  MonitorSmartphone,
  Phone,
  Play,
  Timer,
  UserRound,
  X,
} from 'lucide-react'
import { useParams } from 'react-router'

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
import { Separator } from '@/components/ui/separator'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'
import { apiRequest, toError } from '../api/client'
import type { ReportResponse, SessionDetail, SessionStatus } from '../api/types'
import { Field } from '../components/Field'
import { InfoLine, LinkOrText } from '../components/InfoLine'
import { LoadError } from '../components/LoadError'
import { Notice } from '../components/Notice'
import { PageSkeleton } from '../components/PageSkeleton'
import { TextLink } from '../components/TextLink'
import { formatDateTime, formatMode } from '../format'
import {
  PROGRESS_STEPS,
  SESSION_STATUS_BADGE,
  SESSION_STATUS_LABELS,
  canStartAt,
  progressIndex,
  sessionActions,
  startOpensAt,
  viewerSkills,
} from '../sessionView'

/** Kết quả của một lần bấm nút, hiện ở đầu trang. */
type Feedback = { kind: 'success' | 'error'; text: string }

/**
 * Báo kết quả một thao tác lên trang. updated: SessionDetail mới do API trả (start, complete, cancel; D93), có thì
 * vẽ luôn; không có (báo cáo, hoặc lỗi) thì tải lại phiên.
 */
type OnFinished = (feedback: Feedback, updated?: SessionDetail) => void

/**
 * S09 – Chi tiết phiên (D92–D95; wireframe TK 6.3). Chỉ hai người tham gia xem được: người khác nhận 403, phiên không
 * có thì 404, {id} không phải số thì 400; cả ba hiện detail kèm "Thử lại".
 */
export function SessionDetailPage() {
  const { id = '' } = useParams()
  // key: sang phiên khác thì React tạo lại toàn bộ state (phiên, thông báo), không giữ lại của phiên trước.
  return <SessionDetailContent key={id} id={id} />
}

function SessionDetailContent({ id }: { id: string }) {
  const [session, setSession] = useState<SessionDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [feedback, setFeedback] = useState<Feedback | null>(null)

  useEffect(() => {
    let cancelled = false
    apiRequest<SessionDetail>(`/api/sessions/${encodeURIComponent(id)}`)
      .then((data) => {
        if (!cancelled) {
          setSession(data)
          setLoadError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) setLoadError(toError(e).message)
      })
    return () => {
      cancelled = true
    }
  }, [id, reloadKey])

  const finish: OnFinished = (next, updated) => {
    setFeedback(next)
    if (updated) {
      setSession(updated) // D93: response đã là phiên mới, không cần gọi lại GET
    } else {
      setReloadKey((k) => k + 1) // phiên cũ vẫn hiện tới khi có bản mới, trang không nháy
    }
  }

  let content: ReactNode
  if (loadError) {
    content = <LoadError message={loadError} onRetry={() => { setLoadError(null); setReloadKey((k) => k + 1) }} />
  } else if (!session) {
    content = <PageSkeleton label="Đang tải phiên" />
  } else {
    content = <SessionPanel session={session} onFinished={finish} />
  }

  return (
    <div className="flex flex-col gap-6">
      <TextLink to="/sessions" className="inline-flex w-fit items-center gap-1 text-sm">
        <ArrowLeft className="size-4" aria-hidden="true" />
        Phiên của tôi
      </TextLink>
      {feedback && <Notice kind={feedback.kind}>{feedback.text}</Notice>}
      {content}
    </div>
  )
}

// ---------- Nội dung phiên ----------

function SessionPanel({ session, onFinished }: { session: SessionDetail; onFinished: OnFinished }) {
  const [busy, setBusy] = useState(false)
  // Giờ hiện tại để khóa/mở nút "Bắt đầu". Đọc đồng hồ lúc vẽ thì mỗi lần vẽ ra một giá trị khác (luật purity),
  // nên giữ trong state và cập nhật mỗi 30 giây: để trang mở thì nút tự mở khi tới mốc, không cần F5.
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 30_000)
    return () => clearInterval(timer)
  }, [])
  const ids = useId() // tiền tố id: nối chữ "Mở lúc" với nút, tiêu đề với vùng liên hệ
  const startHintId = `${ids}-start-hint`
  const contactTitleId = `${ids}-contact`

  const { partner } = session
  const { youLearn, youTeach } = viewerSkills(session)
  const actions = sessionActions(session)
  const startOpen = canStartAt(session.startAt, now)
  const hasButtons = actions.start || actions.complete || actions.confirm || actions.cancel || actions.report
  const guide = guideText(session)

  /** Bắt đầu, Hoàn thành, Xác nhận hoàn thành: POST không có body, bấm là gửi ngay. */
  async function post(action: 'start' | 'complete') {
    setBusy(true)
    try {
      const updated = await apiRequest<SessionDetail>(`/api/sessions/${session.id}/${action}`, { method: 'POST' })
      const text = action === 'start'
        ? 'Đã bắt đầu phiên.'
        : updated.status === 'COMPLETED' ? 'Phiên đã hoàn thành.' : 'Đã báo hoàn thành.'
      onFinished({ kind: 'success', text }, updated)
    } catch (e) {
      // 409 khi bên kia vừa đổi trạng thái (vừa hủy, vừa báo cáo...): detail nói lý do, phiên tải lại cho thấy kết quả.
      onFinished({ kind: 'error', text: toError(e).message })
    } finally {
      setBusy(false)
    }
  }

  const cancelSession = (reason: string) =>
    apiRequest<SessionDetail>(`/api/sessions/${session.id}/cancel`, { method: 'POST', body: { reason } })

  // reports chỉ trả báo cáo vừa tạo (D94), không trả phiên: trả null để trang tải lại phiên.
  const sendReport = async (reason: string) => {
    await apiRequest<ReportResponse>(`/api/sessions/${session.id}/reports`, { method: 'POST', body: { reason } })
    return null
  }

  return (
    <Card>
      <CardHeader className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-1">
          <h1 className="text-2xl">Phiên #{session.id}</h1>
          <p className="break-words text-muted-foreground">
            {session.receiverLearnSkill
              ? `${session.senderLearnSkill.name} ↔ ${session.receiverLearnSkill.name}`
              : session.senderLearnSkill.name}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline">{session.type === 'TWO_WAY' ? 'Hai chiều' : 'Một chiều'}</Badge>
          <Badge variant={SESSION_STATUS_BADGE[session.status]}>{SESSION_STATUS_LABELS[session.status]}</Badge>
        </div>
      </CardHeader>

      <CardContent className="flex flex-col gap-5">
        {session.status === 'CANCELLED' ? <CancelledBox session={session} /> : <SessionProgress status={session.status} />}

        <div className="flex flex-col gap-2">
          <InfoLine icon={<UserRound />} label="Học cùng">
            <TextLink to={`/users/${partner.id}`}>{partner.fullName}</TextLink>
          </InfoLine>
          {youLearn && <InfoLine icon={<BookOpen />} label="Bạn học">{youLearn}</InfoLine>}
          {youTeach && <InfoLine icon={<GraduationCap />} label="Bạn dạy">{youTeach}</InfoLine>}
          <InfoLine icon={<CalendarClock />} label="Thời điểm">
            {formatDateTime(session.startAt)} ({session.durationMinutes} phút)
          </InfoLine>
          <InfoLine icon={<MonitorSmartphone />} label="Hình thức">{formatMode(session.mode)}</InfoLine>
          {session.locationOrLink && (
            <InfoLine icon={<MapPin />} label="Địa điểm/link"><LinkOrText value={session.locationOrLink} /></InfoLine>
          )}
          {session.startedAt && (
            <InfoLine icon={<Timer />} label="Bắt đầu lúc">{formatDateTime(session.startedAt)}</InfoLine>
          )}
          {session.completedAt && (
            <InfoLine icon={<CheckCheck />} label="Hoàn thành lúc">{formatDateTime(session.completedAt)}</InfoLine>
          )}
        </div>

        <Separator />

        {/* NFR-02: liên hệ chỉ có ở chi tiết phiên, cho hai người tham gia, ở mọi trạng thái (D92). */}
        <section className="flex flex-col gap-2" aria-labelledby={contactTitleId}>
          <h2 id={contactTitleId} className="text-base break-words">Liên hệ {partner.fullName}</h2>
          <InfoLine icon={<Phone />} label="Điện thoại">{partner.phone ?? <NotDeclared />}</InfoLine>
          <InfoLine icon={<MessageCircle />} label="Liên hệ khác">{partner.contactInfo ?? <NotDeclared />}</InfoLine>
        </section>

        <p className="text-xs text-muted-foreground">Tạo lúc {formatDateTime(session.createdAt)}</p>
      </CardContent>

      {(guide || session.openReport || hasButtons) && (
        <CardFooter className="flex flex-col items-start gap-3 border-t">
          {/* Báo cáo mở thay cho dòng hướng dẫn: không có lý do vì lý do chỉ dành cho admin (D92). */}
          {session.openReport ? (
            <Notice kind="info">
              {session.openReport.reportedByMe ? 'Bạn' : partner.fullName} đã gửi báo cáo sự cố lúc{' '}
              {formatDateTime(session.openReport.createdAt)}, phiên đang chờ quản trị viên xử lý.
            </Notice>
          ) : (
            guide && <p>{guide}</p>
          )}

          {/* Nút theo bảng 2.3 (sessionActions); backend vẫn kiểm lại mọi quyền và luật (spec 6.3). */}
          {hasButtons && (
            <div className="flex flex-wrap items-center gap-2">
              {actions.start && (
                <>
                  <Button size="sm" onClick={() => post('start')} disabled={busy || !startOpen}
                          aria-describedby={startOpen ? undefined : startHintId}>
                    <BusyIcon busy={busy} icon={<Play aria-hidden="true" />} />
                    Bắt đầu
                  </Button>
                  {!startOpen && (
                    <span id={startHintId} className="text-sm text-muted-foreground">
                      Mở lúc {formatDateTime(startOpensAt(session.startAt).toISOString())}
                    </span>
                  )}
                </>
              )}
              {actions.complete && (
                <Button size="sm" onClick={() => post('complete')} disabled={busy}>
                  <BusyIcon busy={busy} icon={<Check aria-hidden="true" />} />
                  Hoàn thành
                </Button>
              )}
              {actions.confirm && (
                <Button size="sm" onClick={() => post('complete')} disabled={busy}>
                  <BusyIcon busy={busy} icon={<CheckCheck aria-hidden="true" />} />
                  Xác nhận hoàn thành
                </Button>
              )}
              {actions.cancel && (
                <ReasonDialog
                  triggerIcon={<X aria-hidden="true" />} triggerLabel="Hủy phiên" disabled={busy}
                  title="Hủy phiên"
                  description={session.status === 'IN_PROGRESS'
                    ? 'Phiên đã bắt đầu: hủy lúc này được tính vào tỉ lệ hoàn thành của cả hai bên.'
                    : 'Phiên chưa bắt đầu: hủy lúc này không tính vào tỉ lệ hoàn thành.'}
                  submitLabel="Hủy phiên" submitVariant="destructive" successText="Đã hủy phiên."
                  send={cancelSession} onFinished={onFinished} />
              )}
              {actions.report && (
                <ReasonDialog
                  triggerIcon={<Flag aria-hidden="true" />} triggerLabel="Báo cáo sự cố" disabled={busy}
                  title="Báo cáo sự cố"
                  description={`Dùng khi bạn và ${partner.fullName} không thống nhất buổi học đã diễn ra. `
                    + 'Chỉ quản trị viên đọc lý do và chốt phiên.'}
                  submitLabel="Gửi báo cáo" submitVariant="default" successText="Đã gửi báo cáo sự cố."
                  send={sendReport} onFinished={onFinished} />
              )}
            </div>
          )}
        </CardFooter>
      )}
    </Card>
  )
}

/** Dòng hướng dẫn ở chân thẻ theo trạng thái và vai trò (kế hoạch bước 11). Phiên đã hủy có hộp riêng. */
function guideText(session: SessionDetail): string | null {
  const name = session.partner.fullName
  switch (session.status) {
    case 'SCHEDULED':
      return 'Một trong hai bạn bấm Bắt đầu khi buổi học diễn ra.'
    case 'IN_PROGRESS':
      return 'Học xong thì một trong hai bạn bấm Hoàn thành, bên kia xác nhận.'
    case 'WAITING_CONFIRMATION':
      return session.myCompleted
        ? `Bạn đã bấm Hoàn thành, đang chờ ${name} xác nhận.`
        : `${name} đã bấm Hoàn thành. Bạn xác nhận buổi học đã diễn ra?` // câu của wireframe S09
    case 'COMPLETED':
      return 'Phiên đã hoàn thành.'
    case 'CANCELLED':
      return null
  }
}

function NotDeclared() {
  return <span className="text-muted-foreground">Chưa khai báo</span>
}

/** Đang gửi thì icon của nút đổi thành vòng xoay. icon đã có aria-hidden như mọi icon trang trí khác. */
function BusyIcon({ busy, icon }: { busy: boolean; icon: ReactNode }) {
  return busy ? <Loader2 className="animate-spin" aria-hidden="true" /> : icon
}

// ---------- Thanh tiến trình và hộp "đã hủy" ----------

/**
 * Bốn bước Đã lên lịch → Đang diễn ra → Chờ xác nhận → Hoàn thành (TK 5.2). Danh sách có thứ tự (<ol>) để trình
 * đọc màn hình đọc "1 trên 4..."; bước hiện tại có aria-current="step".
 */
function SessionProgress({ status }: { status: SessionStatus }) {
  const current = progressIndex(status)
  if (current === null) {
    return null
  }
  const finished = status === 'COMPLETED'

  return (
    <ol aria-label="Tiến trình phiên" className="grid grid-cols-4">
      {PROGRESS_STEPS.map((step, index) => {
        const done = index < current || finished
        const isCurrent = index === current
        return (
          <li key={step} aria-current={isCurrent ? 'step' : undefined}
              className="relative flex flex-col items-center gap-1.5 px-1 text-center">
            {/* Đoạn nối từ bước trước tới bước này: tô màu khi đã tới bước này. */}
            {index > 0 && (
              <span aria-hidden="true"
                    className={cn('absolute top-4 right-1/2 h-0.5 w-full -translate-y-1/2',
                      index <= current ? 'bg-primary' : 'bg-border')} />
            )}
            <span className={cn(
              'relative flex size-8 items-center justify-center rounded-full border-2 text-xs font-medium tabular-nums',
              done && 'border-primary bg-primary text-primary-foreground',
              !done && isCurrent && 'border-primary bg-card text-primary',
              !done && !isCurrent && 'border-border bg-card text-muted-foreground',
            )}>
              {done ? <Check className="size-4" aria-hidden="true" /> : index + 1}
            </span>
            <span className={cn('text-xs', done || isCurrent ? 'font-medium' : 'text-muted-foreground')}>
              {SESSION_STATUS_LABELS[step]}
              {done && <span className="sr-only"> (đã qua)</span>}
            </span>
          </li>
        )
      })}
    </ol>
  )
}

/** Phiên đã hủy hiện riêng thay cho thanh tiến trình: ai hủy, lúc nào, lý do (D92: cancelledBy theo người xem). */
function CancelledBox({ session }: { session: SessionDetail }) {
  const by = session.cancelledBy === 'ME'
    ? 'Bạn'
    : session.cancelledBy === 'PARTNER'
      ? session.partner.fullName
      : session.cancelledBy === 'ADMIN' ? 'Quản trị viên' : null

  return (
    <div className="flex flex-col gap-1 rounded-lg bg-muted p-3 text-sm break-words">
      <p className="font-medium">Phiên đã hủy</p>
      {session.cancelledAt && (
        <p>{by ? `${by} đã hủy lúc` : 'Hủy lúc'} {formatDateTime(session.cancelledAt)}.</p>
      )}
      {session.cancelReason && <p><span className="font-medium">Lý do:</span> {session.cancelReason}</p>}
    </div>
  )
}

// ---------- Hộp thoại nhập lý do (hủy phiên, báo cáo sự cố) ----------

type ReasonDialogProps = {
  triggerIcon: ReactNode
  triggerLabel: string
  disabled: boolean
  title: string
  description: string
  submitLabel: string
  /** Hủy phiên là thao tác không quay lại được nên tô đỏ; gửi báo cáo thì không. */
  submitVariant: 'destructive' | 'default'
  successText: string
  /** Gửi lý do lên API; trả SessionDetail mới nếu API trả phiên (cancel), null nếu không (reports). */
  send: (reason: string) => Promise<SessionDetail | null>
  onFinished: OnFinished
}

function ReasonDialog({ triggerIcon, triggerLabel, disabled, onFinished, ...formProps }: ReasonDialogProps) {
  const [open, setOpen] = useState(false)
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline" disabled={disabled}>
          {triggerIcon}
          {triggerLabel}
        </Button>
      </DialogTrigger>
      {/* DialogContent chỉ được vẽ khi mở, nên mỗi lần mở là một form trống mới (như hộp thoại từ chối của S08). */}
      <DialogContent>
        <ReasonForm {...formProps} onFinished={(feedback, updated) => {
          setOpen(false)
          onFinished(feedback, updated)
        }} />
      </DialogContent>
    </Dialog>
  )
}

type ReasonFormProps = Omit<ReasonDialogProps, 'triggerIcon' | 'triggerLabel' | 'disabled'>

/**
 * Lý do bắt buộc (D88) nhưng do backend kiểm: gửi rỗng thì lỗi reason "Chưa nhập lý do" hiện dưới ô, hộp thoại vẫn
 * mở. Lỗi khác (409 bên kia vừa đổi trạng thái...) thì đóng hộp thoại và báo ở đầu trang như các nút khác.
 */
function ReasonForm({ title, description, submitLabel, submitVariant, successText, send, onFinished }: ReasonFormProps) {
  const [reason, setReason] = useState('')
  const [sending, setSending] = useState(false)
  const [reasonError, setReasonError] = useState<string | undefined>()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSending(true)
    setReasonError(undefined)
    try {
      const updated = await send(reason)
      onFinished({ kind: 'success', text: successText }, updated ?? undefined)
    } catch (e) {
      const error = toError(e)
      if (error.fieldErrors.reason) {
        setReasonError(error.fieldErrors.reason)
        setSending(false)
      } else {
        onFinished({ kind: 'error', text: error.message })
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <DialogHeader>
        <DialogTitle>{title}</DialogTitle>
        <DialogDescription>{description}</DialogDescription>
      </DialogHeader>
      <Field label="Lý do" error={reasonError} hint="Bắt buộc, tối đa 1000 ký tự">
        <Textarea rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} />
      </Field>
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" variant="outline" disabled={sending}>Quay lại</Button>
        </DialogClose>
        <Button type="submit" variant={submitVariant} disabled={sending}>
          {sending && <Loader2 className="animate-spin" aria-hidden="true" />}
          {submitLabel}
        </Button>
      </DialogFooter>
    </form>
  )
}
