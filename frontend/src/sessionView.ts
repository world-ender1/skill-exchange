// Luật hiển thị của S09 Phiên (D93, D94, bảng 2.3 của quyết định tuần 6). Hàm thuần, không đụng React hay API,
// để chạy thử được bằng Node như requestTime.ts. Chỉ import kiểu (import type) nên Node chạy thẳng file .ts này.
//
// Mọi thứ ở đây chỉ để giao diện dễ dùng (ẩn nút không bấm được); quyền và luật do backend kiểm lại (spec 6.3).
import type { OpenReport, SessionItem, SessionStatus } from './api/types'

export const SESSION_STATUS_LABELS: Record<SessionStatus, string> = {
  SCHEDULED: 'Đã lên lịch',
  IN_PROGRESS: 'Đang diễn ra',
  WAITING_CONFIRMATION: 'Chờ xác nhận',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
}

/** Màu badge (kế hoạch bước 11, D79): chờ xác nhận màu hổ phách, hoàn thành xanh lá, đã hủy xám như S08. */
export const SESSION_STATUS_BADGE = {
  SCHEDULED: 'outline',
  IN_PROGRESS: 'default',
  WAITING_CONFIRMATION: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'secondary',
} as const satisfies Record<SessionStatus, string>

/** Bốn bước của thanh tiến trình, theo thứ tự của TK 5.2. CANCELLED không nằm trên thanh mà hiện riêng. */
export const PROGRESS_STEPS: SessionStatus[] = ['SCHEDULED', 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'COMPLETED']

/** Vị trí bước hiện tại trên thanh (0..3); phiên đã hủy thì null. */
export function progressIndex(status: SessionStatus): number | null {
  const index = PROGRESS_STEPS.indexOf(status)
  return index === -1 ? null : index
}

const START_WINDOW_MINUTES = 15

/** Mốc mở nút "Bắt đầu": giờ hẹn trừ 15 phút (D93). */
export function startOpensAt(startAt: string): Date {
  return new Date(new Date(startAt).getTime() - START_WINDOW_MINUTES * 60_000)
}

/** Đã tới mốc thì bắt đầu được, đúng mốc cũng được; không có cận trên (D93: phiên trễ giờ vẫn bắt đầu được). */
export function canStartAt(startAt: string, now: Date): boolean {
  return now.getTime() >= startOpensAt(startAt).getTime()
}

/** Các nút của chi tiết phiên; true = hiện nút. "Bắt đầu" hiện cả khi chưa tới mốc, chỉ bị khóa (canStartAt). */
export type SessionActions = {
  start: boolean
  complete: boolean
  confirm: boolean
  cancel: boolean
  report: boolean
}

type ActionInput = {
  status: SessionStatus
  myCompleted: boolean
  openReport: OpenReport | null
}

/**
 * Nút nào hiện, theo bảng 2.3. Hai người tham gia làm được như nhau, trừ ô "Xác nhận hoàn thành": người đã bấm
 * hoàn thành thì không xác nhận lại được (D93).
 */
export function sessionActions({ status, myCompleted, openReport }: ActionInput): SessionActions {
  const actions: SessionActions = { start: false, complete: false, confirm: false, cancel: false, report: false }
  switch (status) {
    case 'SCHEDULED':
      actions.start = true
      actions.cancel = true // hủy được ở mọi thời điểm, kể cả sau giờ hẹn (R34)
      break
    case 'IN_PROGRESS':
      actions.complete = true
      actions.cancel = true
      break
    case 'WAITING_CONFIRMATION':
      // Có báo cáo mở thì chỉ admin chốt được (D93, D94): không còn nút nào cho cả hai bên.
      if (openReport === null) {
        actions.confirm = !myCompleted
        actions.report = true // D94: người đã bấm hoàn thành cũng gửi được báo cáo
      }
      break
    // COMPLETED, CANCELLED: trạng thái cuối, không có nút (đánh giá ở tuần 7).
  }
  return actions
}

type SkillInput = Pick<SessionItem, 'myRole' | 'senderLearnSkill' | 'receiverLearnSkill'>

/**
 * Kỹ năng theo góc nhìn người xem, như S08: người gửi yêu cầu học senderLearnSkill; người nhận dạy kỹ năng đó và
 * học receiverLearnSkill (chỉ có ở phiên hai chiều).
 */
export function viewerSkills({ myRole, senderLearnSkill, receiverLearnSkill }: SkillInput) {
  return myRole === 'SENDER'
    ? { youLearn: senderLearnSkill.name, youTeach: receiverLearnSkill?.name }
    : { youLearn: receiverLearnSkill?.name, youTeach: senderLearnSkill.name }
}
