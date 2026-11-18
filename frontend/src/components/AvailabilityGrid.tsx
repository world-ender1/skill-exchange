import { Check } from 'lucide-react'

import { Checkbox } from '@/components/ui/checkbox'
import { cn } from '@/lib/utils'
import type { Period } from '../api/types'
import { DAYS, PERIODS, SHORT_DAYS, slotKey } from '../availability'

type AvailabilityGridProps = {
  /** Các ô đang được chọn (khóa của slotKey). */
  selected: ReadonlySet<string>
  /** Không truyền = chỉ đọc (S06): ô không tick được. */
  onToggle?: (key: string) => void
}

/**
 * Lưới lịch rảnh 7 ngày × 3 buổi (D84), dùng ở S03 (sửa được) và S06 (chỉ đọc).
 *
 * Vẽ hai bảng: bảng ngang (7 cột ngày) hiện từ 640 px, bảng dọc (7 hàng ngày) hiện dưới 640 px để màn
 * 360 px không phải cuộn ngang. Bảng đang ẩn có display: none nên trình duyệt bỏ qua hoàn toàn: không
 * nhận Tab, trình đọc màn hình không đọc, nên mỗi ô vẫn chỉ có một ô chọn thật.
 */
export function AvailabilityGrid({ selected, onToggle }: AvailabilityGridProps) {
  const cell = (dayIndex: number, period: Period, periodLabel: string) => (
    <SlotCell
      key={slotKey(dayIndex + 1, period)}
      label={`${periodLabel} ${DAYS[dayIndex]}`}
      checked={selected.has(slotKey(dayIndex + 1, period))}
      onToggle={onToggle && (() => onToggle(slotKey(dayIndex + 1, period)))}
    />
  )

  return (
    <>
      <table className="hidden w-full table-fixed border-separate border-spacing-1 sm:table">
        <thead>
          <tr>
            <td className="w-14" />
            {SHORT_DAYS.map((day, index) => (
              <th key={day} scope="col" title={DAYS[index]} className="pb-1 text-xs font-medium text-muted-foreground">
                {day}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {PERIODS.map(({ value, label }) => (
            <tr key={value}>
              <th scope="row" className="pr-2 text-left text-sm font-medium">{label}</th>
              {DAYS.map((_, dayIndex) => <td key={dayIndex}>{cell(dayIndex, value, label)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>

      <table className="w-full table-fixed border-separate border-spacing-1 sm:hidden">
        <thead>
          <tr>
            <td className="w-24" />
            {PERIODS.map(({ value, label }) => (
              <th key={value} scope="col" className="pb-1 text-xs font-medium text-muted-foreground">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {DAYS.map((day, dayIndex) => (
            <tr key={day}>
              <th scope="row" className="pr-2 text-left text-sm font-medium">{day}</th>
              {PERIODS.map(({ value, label }) => <td key={value}>{cell(dayIndex, value, label)}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

type SlotCellProps = {
  /** Ví dụ "Sáng Thứ Hai": aria-label của ô, giữ như lưới cũ (D84). */
  label: string
  checked: boolean
  onToggle?: () => void
}

function SlotCell({ label, checked, onToggle }: SlotCellProps) {
  const box = cn(
    'flex h-11 min-w-11 items-center justify-center rounded-md border transition-colors',
    checked ? 'border-primary bg-primary/10 text-primary' : 'border-border bg-card',
  )

  if (!onToggle) {
    // Chỉ đọc: không có ô chọn, chỉ có dấu tick và một câu ẩn cho trình đọc màn hình.
    return (
      <div className={box}>
        {checked && <Check className="size-4" aria-hidden="true" />}
        <span className="sr-only">{`${label}: ${checked ? 'rảnh' : 'không rảnh'}`}</span>
      </div>
    )
  }

  // <label> bọc ô chọn: bấm vào bất kỳ đâu trong ô vuông 44 px cũng tick được, không phải nhắm vào ô 16 px.
  return (
    <label className={cn(box, 'cursor-pointer hover:border-primary')}>
      <Checkbox aria-label={label} checked={checked} onCheckedChange={onToggle} />
    </label>
  )
}
