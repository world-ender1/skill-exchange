// Hằng số và khóa ô của lưới lịch rảnh 7 × 3 (spec 4.6), dùng chung cho AvailabilityGrid và S03.
import type { Period } from './api/types'

/** dayOfWeek 1..7 = Thứ Hai..Chủ nhật. */
export const DAYS = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật']

/** Tên ngắn, cùng cách ghi "Tối T3" của formatSlot. */
export const SHORT_DAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

export const PERIODS: { value: Period; label: string }[] = [
  { value: 'MORNING', label: 'Sáng' },
  { value: 'AFTERNOON', label: 'Chiều' },
  { value: 'EVENING', label: 'Tối' },
]

/** Khóa của một ô, ví dụ "2-EVENING" (Tối Thứ Ba). */
export const slotKey = (dayOfWeek: number, period: Period) => `${dayOfWeek}-${period}`
