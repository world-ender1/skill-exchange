// Ngày giờ của S07 Gửi yêu cầu (D95, Q7). Hàm thuần, không đụng React hay API, để chạy thử được bằng Node.
// Chỉ import kiểu (import type): Node bỏ được phần kiểu và chạy thẳng file .ts này.
import type { AvailabilitySlot, Period } from './api/types'

/** Giờ bắt đầu của buổi khi bấm một chip khung trùng (Q7), theo giờ máy người dùng. */
const PERIOD_START: Record<Period, { hour: number; minute: number }> = {
  MORNING: { hour: 8, minute: 0 },
  AFTERNOON: { hour: 14, minute: 0 },
  EVENING: { hour: 19, minute: 0 },
}

const PERIOD_ORDER: Period[] = ['MORNING', 'AFTERNOON', 'EVENING']

/** Các buổi cả hai cùng rảnh, xếp theo thứ (T2 → CN) rồi buổi (Sáng → Tối). */
export function commonSlots(mine: AvailabilitySlot[], theirs: AvailabilitySlot[]): AvailabilitySlot[] {
  const key = (slot: AvailabilitySlot) => `${slot.dayOfWeek}-${slot.period}`
  const theirKeys = new Set(theirs.map(key))
  return mine
    .filter((slot) => theirKeys.has(key(slot)))
    .sort((a, b) => a.dayOfWeek - b.dayOfWeek || PERIOD_ORDER.indexOf(a.period) - PERIOD_ORDER.indexOf(b.period))
}

/**
 * Ngày gần nhất có thứ của khung mà giờ bắt đầu buổi còn ở tương lai (Q7). Trả chuỗi để điền thẳng vào
 * ô date ("2026-11-24") và ô time ("19:00").
 * Ví dụ khung Tối T3: thứ Hai → thứ Ba ngày mai; thứ Ba 18:59 → hôm nay; thứ Ba 19:00 trở đi → thứ Ba tuần sau.
 */
export function nextSlotStart(slot: AvailabilitySlot, now: Date): { date: string; time: string } {
  const { hour, minute } = PERIOD_START[slot.period]
  // dayOfWeek của API: 1..7 = T2..CN. getDay() của JavaScript: 0..6 = CN..T7. 7 % 7 = 0 nên CN khớp.
  const targetDay = slot.dayOfWeek % 7
  const daysAhead = (targetDay - now.getDay() + 7) % 7
  // new Date(năm, tháng, ngày + n, ...) tự sang tháng, sang năm khi ngày vượt quá cuối tháng.
  const startAt = (days: number) =>
    new Date(now.getFullYear(), now.getMonth(), now.getDate() + days, hour, minute)

  let start = startAt(daysAhead)
  if (start <= now) {
    start = startAt(daysAhead + 7) // đúng thứ hôm nay nhưng giờ buổi đã qua (bằng cũng tính là qua)
  }
  return { date: localDateString(start), time: `${pad(hour)}:${pad(minute)}` }
}

/**
 * Ghép ô ngày và ô giờ (giờ máy người dùng) thành chuỗi ISO giờ UTC, dạng backend nhận (D88).
 * Thiếu một trong hai ô thì null, để backend báo "Chưa chọn thời điểm" dưới ô.
 */
export function toInstant(date: string, time: string): string | null {
  if (!date || !time) {
    return null
  }
  // "2026-11-24T19:00" không ghi múi giờ: JavaScript hiểu là giờ máy, toISOString() đổi sang UTC.
  const value = new Date(`${date}T${time}`)
  return Number.isNaN(value.getTime()) ? null : value.toISOString()
}

/** "YYYY-MM-DD" theo giờ máy (toISOString() thì theo UTC, có thể lệch sang hôm trước). */
export function localDateString(value: Date): string {
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}
