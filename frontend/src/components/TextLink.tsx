import type { ComponentProps } from 'react'
import { Link } from 'react-router'

import { cn } from '@/lib/utils'

/** Link nằm trong câu chữ: màu chính, gạch chân khi rê chuột. Điều hướng trong app nên vẫn là Link của react-router. */
export function TextLink({ className, ...props }: ComponentProps<typeof Link>) {
  return (
    <Link
      data-slot="text-link"
      className={cn('font-medium text-primary underline-offset-4 hover:underline', className)}
      {...props}
    />
  )
}
