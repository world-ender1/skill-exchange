// Cách hiển thị dùng chung cho S05, S06, S10 (kế hoạch tuần 4, bước 10).
import type { AvailabilitySlot, LearningMode, Period } from './api/types'

/**
 * D50: "Mức phù hợp" = score × 100, giữ 1 chữ số thập phân, bỏ ".0" khi chẵn.
 * 0.78 → "78%", 0.797 → "79.7%". score đã được backend làm tròn 3 chữ số; ở đây chỉ đổi đơn vị.
 *
 * Không viết String(score * 100): 0.57 * 100 trong số thực máy tính ra 56.99999999999999.
 * toFixed(1) làm tròn về "57.0", rồi Number(...) bỏ ".0" thừa → "57%".
 */
export function formatPercent(score: number): string {
  return `${Number((score * 100).toFixed(1))}%`
}

const SHORT_DAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'] // dayOfWeek 1..7

const PERIOD_LABELS: Record<Period, string> = {
  MORNING: 'Sáng',
  AFTERNOON: 'Chiều',
  EVENING: 'Tối',
}

/** { dayOfWeek: 3, period: 'EVENING' } → "Tối T4" (cách ghi của wireframe S05). */
export function formatSlot(slot: AvailabilitySlot): string {
  return `${PERIOD_LABELS[slot.period]} ${SHORT_DAYS[slot.dayOfWeek - 1]}`
}

/** Cùng nhãn với ô chọn ở S03. */
export function formatMode(mode: LearningMode | null): string {
  switch (mode) {
    case 'ONLINE':
      return 'Trực tuyến'
    case 'OFFLINE':
      return 'Gặp trực tiếp'
    case 'ANY':
      return 'Cách nào cũng được'
    default:
      return 'Chưa khai báo'
  }
}

const LEVEL_LABELS = ['Mới bắt đầu', 'Cơ bản', 'Khá', 'Thành thạo'] // thang 1–4 của spec 4.6

/** 3 → "3 – Khá" (như S04). */
export function formatLevel(level: number): string {
  return `${level} – ${LEVEL_LABELS[level - 1] ?? ''}`
}
