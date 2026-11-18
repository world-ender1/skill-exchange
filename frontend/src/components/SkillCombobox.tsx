import { useState } from 'react'
import { ChevronsUpDown } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from '@/components/ui/command'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import type { Skill } from '../api/types'

type SkillComboboxProps = {
  /** Danh mục để chọn, đã xếp theo thứ tự backend trả (D40). */
  skills: Skill[]
  /** Mã kỹ năng đang chọn dạng chuỗi, '' = chưa chọn. */
  value: string
  onChange: (value: string) => void
  /** Tên của ô cho trình đọc màn hình, ví dụ "Chọn kỹ năng để thêm". */
  ariaLabel: string
  placeholder: string
  /** Lý do một kỹ năng không chọn được (đã có trong danh sách...), null = chọn được. */
  disabledReason?: (skill: Skill) => string | null
  /** Thêm một lựa chọn "không chọn gì" ở đầu, ví dụ "Bất kỳ" ở S10. */
  emptyOptionLabel?: string
  disabled?: boolean
}

/** Bỏ dấu tiếng Việt và viết thường: "Tiếng Anh" → "tieng anh", để gõ không dấu vẫn tìm ra. */
function normalize(text: string): string {
  return text
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

/**
 * Ô chọn kỹ năng gõ để lọc, gom theo nhóm (kế hoạch tuần 6, bước 2), thay <select> có <optgroup>.
 * Dùng ở S04 (thêm kỹ năng) và S10 (tìm theo kỹ năng).
 */
export function SkillCombobox(props: SkillComboboxProps) {
  const { skills, value, onChange, ariaLabel, placeholder, disabledReason, emptyOptionLabel, disabled } = props
  const [open, setOpen] = useState(false)

  const selected = skills.find((s) => String(s.id) === value)
  const buttonText = selected?.name ?? (value === '' && emptyOptionLabel ? emptyOptionLabel : placeholder)
  const categories = [...new Set(skills.map((s) => s.categoryName))]

  function choose(next: string) {
    onChange(next)
    setOpen(false)
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-label={ariaLabel}
          disabled={disabled}
          className="w-full justify-between font-normal"
        >
          <span className={selected ? 'truncate' : 'truncate text-muted-foreground'}>{buttonText}</span>
          <ChevronsUpDown className="opacity-60" aria-hidden="true" />
        </Button>
      </PopoverTrigger>
      {/* Rộng bằng nút bấm; tối thiểu 16rem cho tên dài. */}
      <PopoverContent className="w-(--radix-popover-trigger-width) min-w-64 p-0" align="start">
        <Command
          filter={(itemValue, search, keywords) =>
            normalize([itemValue, ...(keywords ?? [])].join(' ')).includes(normalize(search)) ? 1 : 0}
        >
          <CommandInput placeholder="Gõ để tìm kỹ năng…" />
          <CommandList>
            <CommandEmpty>Không có kỹ năng phù hợp.</CommandEmpty>
            {emptyOptionLabel && (
              <CommandGroup>
                <CommandItem value={emptyOptionLabel} onSelect={() => choose('')}>{emptyOptionLabel}</CommandItem>
              </CommandGroup>
            )}
            {categories.map((category) => (
              <CommandGroup key={category} heading={category}>
                {skills.filter((s) => s.categoryName === category).map((skill) => {
                  const reason = disabledReason?.(skill) ?? null
                  return (
                    // value là tên (duy nhất theo D74) để lọc; keywords thêm tên nhóm: gõ "ngoại ngữ" ra cả nhóm.
                    <CommandItem
                      key={skill.id}
                      value={skill.name}
                      keywords={[category]}
                      disabled={reason !== null}
                      onSelect={() => choose(String(skill.id))}
                    >
                      <span>{skill.name}</span>
                      {reason && <span className="text-xs text-muted-foreground">({reason})</span>}
                    </CommandItem>
                  )
                })}
              </CommandGroup>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
